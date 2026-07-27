# ❌ Guide complet : Annulation d'une commande

> Tout le flux d'annulation décrit étape par étape, avec **chaque bout de code annoté** par son fichier et ses lignes, ainsi qu'une explication de ce qu'il fait dans le pipeline.

> **Mots-clés** : annulation, annulé, état 6, cancel, transitionToCancelled, order_state_change, OrderList, OrderHistory, paid_states, reserved_quantity, libération

---

## 📑 Index

- [1. Vue d'ensemble du flux d'annulation](#1-vue-densemble-du-flux-dannulation)
- [2. Règles métier appliquées](#2-règles-métier-appliquées)
- [3. Étape 1 — Déclencheur UI (OrderList.vue)](#3-étape-1--déclencheur-ui-orderlistvue)
- [4. Étape 2 — Confirmation utilisateur](#4-étape-2--confirmation-utilisateur)
- [5. Étape 3 — Appel du service orderService.changeOrderState](#5-étape-3--appel-du-service-orderservicechangeorderstate)
- [6. Étape 4 — Endpoint webservice `/api/order_state_change`](#6-étape-4--endpoint-webservice-apiorder_state_change)
- [7. Étape 5 — Vérification métier "commande payée"](#7-étape-5--vérification-métier-commande-payée)
- [8. Étape 6 — Méthode `transitionToCancelled`](#8-étape-6--méthode-transitiontocancelled)
- [9. Étape 7 — `OrderHistory::changeIdOrderState` (PrestaShop core)](#9-étape-7--orderhistorychangeidorderstate-prestashop-core)
- [10. Étape 8 — Libération de la réservation (StockAvailable)](#10-étape-8--libération-de-la-réservation-stockavailable)
- [11. Étape 9 — Retour JSON au client](#11-étape-9--retour-json-au-client)
- [12. Étape 10 — Refresh de la liste côté Vue](#12-étape-10--refresh-de-la-liste-côté-vue)
- [13. Annulation via import (fichier 3 — état "annulé")](#13-annulation-via-import-fichier-3--état-annulé)
- [14. Impact détaillé sur le stock](#14-impact-détaillé-sur-le-stock)
- [15. Cas d'erreur et messages associés](#15-cas-derreur-et-messages-associés)
- [16. Récap des fichiers concernés](#16-récap-des-fichiers-concernés)

---

## 1. Vue d'ensemble du flux d'annulation

**Mots-clés** : vue d'ensemble, pipeline, schéma

```
   ┌────────────────────────┐
   │ User clique "Annuler"  │
   │  (OrderList.vue:85)    │
   └──────────┬─────────────┘
              │ onCancel(item)
              ▼
   ┌────────────────────────┐
   │ confirm() utilisateur  │
   └──────────┬─────────────┘
              │ accepté
              ▼
   ┌────────────────────────┐
   │ OrderService.          │
   │  changeOrderState(id,6)│
   │ (orderService.js:375)  │
   └──────────┬─────────────┘
              │ POST JSON
              ▼
   ┌────────────────────────────────────┐
   │ /api/order_state_change            │
   │ Module orderstateapi               │
   │ WebserviceSpecificManagement       │
   │  OrderStateChange::manage()        │
   └──────────┬─────────────────────────┘
              │
              │ if (new_state === 6)
              ▼
   ┌────────────────────────────────────┐
   │ Vérif : current_state ∈ [2, 11] ?  │
   │ Si NON → HTTP 400                  │
   └──────────┬─────────────────────────┘
              │ OUI
              ▼
   ┌────────────────────────────────────┐
   │ transitionToCancelled($order)      │
   │  1. OrderHistory::changeIdOrderState(6, $order)
   │  2. $history->add()
   │  3. for each product:
   │       reserved_quantity -= qty
   │ (PrestaShop ré-incrémente qty auto)│
   └──────────┬─────────────────────────┘
              │ JSON success
              ▼
   ┌────────────────────────┐
   │ Vue : await loadAll()  │
   │ → rafraîchit le tableau│
   └────────────────────────┘
```

---

## 2. Règles métier appliquées

**Mots-clés** : règles métier, contraintes, validation

1. **Seules les commandes "payées" (état 2 ou 11) peuvent être annulées.**
   Tentative depuis un autre état → HTTP 400 avec message explicite.

2. **Aucun stock_mvt n'est créé.**
   Le produit n'a jamais quitté physiquement le magasin (commande non livrée) → aucune trace de mouvement physique.

3. **`physical_quantity` n'est pas touché.**
   Seul `reserved_quantity` est diminué (libération de la réservation).

4. **`quantity` est ré-incrémenté automatiquement par PrestaShop.**
   Via `OrderHistory::changeIdOrderState` interne — pas par notre code custom.

---

## 3. Étape 1 — Déclencheur UI (OrderList.vue)

**Mots-clés** : bouton, UI, déclencheur, click

📄 **Fichier** : [`NewApp/src/components/OrderList.vue`](../NewApp/src/components/OrderList.vue)
📍 **Lignes 82-91** : le bouton "❌ Annuler"

```vue
<!-- Bouton Annuler : visible si commande payée (11) -->
<button
  v-if="item.type === 'order' && parseInt(item.current_state) === 11"
  @click="onCancel(item)"
  class="btn-cancel"
  :disabled="updatingId === item.id"
  title="Annuler la commande"
>
  ❌ Annuler
</button>
```

**Explication ligne par ligne** :
- `v-if=...` (ligne 83) : le bouton n'apparaît **que** pour les items de type `order` ET dont `current_state` est égal à 11. Pour les paniers ou les commandes déjà livrées/annulées, il est masqué.
- `@click="onCancel(item)"` (ligne 84) : transmet l'objet `item` complet (incluant `id` et `reference`) au handler.
- `:disabled="updatingId === item.id"` (ligne 86) : pendant qu'une opération est en cours sur cette commande, désactive le bouton pour éviter le double-clic. `updatingId` est un `ref` qui mémorise l'ID en cours de traitement.

---

## 4. Étape 2 — Confirmation utilisateur

**Mots-clés** : confirm, validation utilisateur

📄 **Fichier** : [`NewApp/src/components/OrderList.vue`](../NewApp/src/components/OrderList.vue)
📍 **Lignes 207-210** : handler `onCancel`

```js
// Annuler une commande (état 6)
const onCancel = async (item) => {
  if (!confirm(`Annuler la commande ${item.reference} ?`)) return
  await callOrderStateChange(item, 6, 'annulée')
}
```

**Explication** :
- `confirm(...)` (ligne 209) bloque l'exécution avec une boîte de dialogue native. Si l'utilisateur clique "Annuler", la fonction retourne immédiatement sans rien faire.
- Sinon, on appelle `callOrderStateChange(item, 6, 'annulée')` — `6` est l'ID de l'état "Annulé" et `'annulée'` est juste le label affiché en message de succès après.

---

## 5. Étape 3 — Appel du service `orderService.changeOrderState`

**Mots-clés** : service, axios, JSON

📄 **Fichier** : [`NewApp/src/components/OrderList.vue`](../NewApp/src/components/OrderList.vue)
📍 **Lignes 212-228** : helper `callOrderStateChange`

```js
const callOrderStateChange = async (item, newState, label) => {
  successMessage.value = ''
  errorMessage.value = ''
  updatingId.value = item.id
  try {
    const result = await OrderService.changeOrderState(item.id, newState)
    console.log('✅ Résultat:', result)
    successMessage.value = `Commande ${item.reference} ${label} (stock mis à jour)`
    setTimeout(() => { successMessage.value = '' }, 3000)
    await loadAll()
  } catch (err) {
    const msg = err.response?.data?.error || err.message
    errorMessage.value = `Échec : ${msg}`
  } finally {
    updatingId.value = null
  }
}
```

**Explication** :
- `updatingId.value = item.id` : verrouille les boutons de l'item pendant l'opération.
- `OrderService.changeOrderState(item.id, 6)` : appelle le service avec l'ID de la commande et le nouvel état.
- `try / catch / finally` :
  - **try** : on récupère le résultat JSON et on met à jour les messages
  - **catch** : on extrait le message d'erreur PrestaShop si disponible (`err.response?.data?.error`)
  - **finally** : on libère `updatingId` quoi qu'il arrive

📄 **Fichier** : [`NewApp/src/services/orderService.js`](../NewApp/src/services/orderService.js)
📍 **Lignes 375-383** : méthode `changeOrderState`

```js
async changeOrderState(orderId, newState) {
  const res = await axios.post('/api/order_state_change', {
    id_order: orderId,
    new_state: newState,
  }, {
    headers: { 'Content-Type': 'application/json' }
  })
  return res.data
}
```

**Explication** :
- Simple wrapper autour de `axios.post`. Le body est un **JSON** (et pas du XML comme pour les ressources PrestaShop natives), car c'est un endpoint **custom** que nous avons ajouté via le module `orderstateapi`.
- `Content-Type: application/json` est obligatoire — sinon PrestaShop interprète le body autrement.
- L'authentification (clé API) est automatiquement injectée par l'intercepteur axios configuré dans [`config/axios.js`](../NewApp/src/config/axios.js).

---

## 6. Étape 4 — Endpoint webservice `/api/order_state_change`

**Mots-clés** : webservice, endpoint, manage, dispatcher

📄 **Fichier** : [`modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php)
📍 **Lignes 50-73** : début de `manage()` — entry point de la requête

```php
public function manage()
{
    header('Content-Type: application/json');

    if ($this->wsObject->method !== 'POST') {
        http_response_code(405);
        die(json_encode(['error' => 'Method not allowed — only POST is supported']));
    }

    $input     = json_decode(file_get_contents('php://input'), true);
    $id_order  = (int)($input['id_order'] ?? 0);
    $new_state = (int)($input['new_state'] ?? 0);

    if (!$id_order || !$new_state) {
        http_response_code(400);
        die(json_encode(['error' => 'id_order et new_state requis']));
    }

    // Vérifier que la commande existe
    $order = new Order($id_order);
    if (!Validate::isLoadedObject($order)) {
        http_response_code(404);
        die(json_encode(['error' => 'Commande ' . $id_order . ' introuvable']));
    }
    // ...
}
```

**Explication ligne par ligne** :
- `header('Content-Type: application/json')` : on signale que la réponse sera en JSON.
- Check HTTP method : si autre que POST → **405 Method Not Allowed**.
- `json_decode(file_get_contents('php://input'), true)` : lit le body brut de la requête et le parse en tableau PHP associatif.
- Cast `(int)` + `?? 0` : sécurité — si le champ est absent ou invalide, valeur par défaut = 0 → le check suivant le rejette.
- `new Order($id_order)` : charge la commande depuis la BDD via l'ObjectModel PrestaShop.
- `Validate::isLoadedObject($order)` : vérifie que le chargement a bien fonctionné. Si `id_order` n'existe pas → l'objet est "vide" → on renvoie **404**.

---

## 7. Étape 5 — Vérification métier "commande payée"

**Mots-clés** : règle, paid, payée, vérification

📄 **Fichier** : [`modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php)
📍 **Lignes 75-108** : `paid_states` + aiguillage par état + check

```php
$current_state = (int)$order->current_state;

// États "payés" considérés comme annulables
$paid_states = [
    (int)Configuration::get('PS_OS_PAYMENT'),     // 2 par défaut
    (int)Configuration::get('PS_OS_WS_PAYMENT'),  // 11 par défaut
];

// ...

} elseif ($new_state === 6) {
    // 6 = Annulé → règle métier : la commande doit être payée
    if (!in_array($current_state, $paid_states, true)) {
        http_response_code(400);
        die(json_encode([
            'error'         => 'Seule une commande PAYÉE peut être annulée',
            'current_state' => $current_state,
            'paid_states'   => $paid_states,
        ]));
    }

    $this->transitionToCancelled($order);
}
```

**Explication** :
- `$paid_states` est construit **dynamiquement** depuis la config PrestaShop. Les IDs `2` et `11` sont par défaut mais peuvent varier d'une instance à l'autre.
- `in_array($current_state, $paid_states, true)` : `true` en 3ᵉ paramètre force la comparaison **stricte** (`===` plutôt que `==`).
- Si la commande est dans un état non payé (ex. déjà annulée, déjà livrée, en erreur de paiement) → **400 Bad Request** avec un message clair et le contexte (`current_state` actuel + liste des états payés acceptés).
- Sinon, on passe à `transitionToCancelled($order)`.

---

## 8. Étape 6 — Méthode `transitionToCancelled`

**Mots-clés** : transitionToCancelled, logique métier

📄 **Fichier** : [`modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php)
📍 **Lignes 240-274** : méthode complète

```php
private function transitionToCancelled(Order $order)
{
    // 1. Changement d'état
    $history = new OrderHistory();
    $history->id_order       = (int)$order->id;
    $history->id_order_state = 6;
    $history->changeIdOrderState(6, $order);
    $history->add();

    // 2. Annulation = libérer la réservation uniquement.
    // Le quantity (dispo) est ré-incrémenté AUTOMATIQUEMENT par PrestaShop
    // lors du changement d'état (logique de cancel cascade), on n'y touche pas.
    // Aucun stock_mvt n'est créé : la commande n'a jamais été livrée physiquement,
    // donc pas de mouvement physique à tracer (juste une libération de réservation).
    foreach ($order->getProducts() as $product) {
        $id_product           = (int)$product['product_id'];
        $id_product_attribute = (int)$product['product_attribute_id'];
        $qty                  = (int)$product['product_quantity'];

        $id_stock = (int)StockAvailable::getStockAvailableIdByProductId(
            $id_product,
            $id_product_attribute
        );
        if (!$id_stock || $qty <= 0) continue;

        $stock = new StockAvailable($id_stock);
        $stock->reserved_quantity = max(0, (int)$stock->reserved_quantity - $qty);
        $stock->update();

        $this->stockUpdates[] = [
            'id_product'           => $id_product,
            'id_product_attribute' => $id_product_attribute,
            'qty'                  => $qty,
            'physical_quantity'    => (int)$stock->physical_quantity,
            'reserved_quantity'    => (int)$stock->reserved_quantity,
            'quantity'             => (int)$stock->quantity,
        ];
    }
}
```

**Explication** :

**Bloc 1 — Changement d'état (lignes 242-246)** :
- Instancie un nouvel `OrderHistory` (une "ligne" dans le journal des changements d'état)
- Renseigne l'ID de la commande et l'état cible
- Appelle `changeIdOrderState(6, $order)` qui fait tout le travail PrestaShop natif (voir étape 7)
- `$history->add()` persiste la ligne dans `ps_order_history`

**Bloc 2 — Boucle sur les produits commandés (lignes 252-273)** :
- `$order->getProducts()` retourne **chaque ligne** de la commande (un produit + qté)
- On extrait `id_product`, `id_product_attribute` (= combinaison) et `qty`
- `StockAvailable::getStockAvailableIdByProductId(...)` retourne l'ID interne de la ligne `ps_stock_available` correspondante
- `if (!$id_stock || $qty <= 0) continue` : skip défensif si stock introuvable ou quantité invalide

**Action critique (ligne 263)** :
```php
$stock->reserved_quantity = max(0, (int)$stock->reserved_quantity - $qty);
```
→ On diminue `reserved_quantity` (libération de la réservation). `max(0, ...)` empêche d'aller en négatif si des données incohérentes.

**`$stock->update()` (ligne 264)** : persiste la modification en BDD.

**`stockUpdates[]` (lignes 266-272)** : on accumule les changements pour les renvoyer au client dans la réponse JSON. Utile pour le front qui peut afficher les nouveaux soldes.

⚠️ **Note importante** : on **ne touche pas** à `physical_quantity` ni à `quantity`. `quantity` sera ré-incrémenté par PrestaShop (étape 7).

---

## 9. Étape 7 — `OrderHistory::changeIdOrderState` (PrestaShop core)

**Mots-clés** : OrderHistory, changeIdOrderState, hooks, cascade

📄 **Fichier** : [`classes/order/OrderHistory.php`](../classes/order/OrderHistory.php)
📍 **Ligne 83** : signature de la méthode

```php
public function changeIdOrderState($new_order_state, $id_order, $use_existing_payment = false)
```

Cette méthode native PrestaShop fait **beaucoup** de choses :

1. Crée une nouvelle ligne dans `ps_order_history`
2. Met à jour `ps_orders.current_state = 6`
3. Déclenche les hooks :
   - `actionOrderStatusUpdate`
   - `actionOrderStatusPostUpdate`
4. Pour les commandes annulées spécifiquement, si la commande avait un état `shipped=1` (ce qui n'est pas notre cas — on annule depuis 11), elle restaurerait le stock automatiquement.
5. Pour notre cas (état 11 → 6, tous deux avec `shipped=0`) : PrestaShop ne fait **rien** sur `physical_quantity`.
6. **`quantity` est ré-incrémenté** par le mécanisme interne de PrestaShop quand la commande est annulée (logique de "stock_available reverse on cancel").

📍 **Lignes 333-357** : extrait pertinent (gestion du stock dans `changeIdOrderState`)

```php
// Save movement if not advanced stock management and shipped changed
if ($new_os->shipped != $old_os->shipped && !Configuration::get('PS_ADVANCED_STOCK_MANAGEMENT')) {
    $product_quantity = (int)($product['product_quantity'] - $product['product_quantity_refunded'] - $product['product_quantity_return']);

    if ($product_quantity > 0) {
        (new StockManager())->saveMovement(
            (int)$product['product_id'],
            (int)$product['product_attribute_id'],
            (int)$product_quantity * ($new_os->shipped == 1 ? -1 : 1),
            // ...
        );
    }
}
```

**Pourquoi cela ne s'applique pas à notre annulation** :
- État 11 (Paiement accepté) : `shipped = 0`
- État 6 (Annulé) : `shipped = 0`
- Donc `$new_os->shipped != $old_os->shipped` est **false** → ce bloc ne s'exécute pas.

PrestaShop a d'autres mécanismes qui ré-incrémentent `quantity` lors d'une annulation, mais sans toucher `physical_quantity`.

---

## 10. Étape 8 — Libération de la réservation (StockAvailable)

**Mots-clés** : StockAvailable, reserved_quantity, libération

📄 **Fichier** : [`classes/stock/StockAvailable.php`](../classes/stock/StockAvailable.php)

C'est l'ObjectModel correspondant à la table `ps_stock_available`. Pas de méthode spécifique impliquée — on charge l'objet, modifie son `reserved_quantity`, puis on appelle `->update()`.

📄 **Fichier** : [`modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php)
📍 **Lignes 261-264** : action concrète

```php
$stock = new StockAvailable($id_stock);
$stock->reserved_quantity = max(0, (int)$stock->reserved_quantity - $qty);
$stock->update();
```

**Diagramme d'évolution** (pour 1 unité commandée annulée) :

```
                   AVANT          APRÈS
physical_quantity   10    →       10     (inchangé)
reserved_quantity    1    →        0     (libération)
quantity             9    →       10     (ré-incrémenté par PrestaShop)
```

---

## 11. Étape 9 — Retour JSON au client

**Mots-clés** : réponse, JSON, response

📄 **Fichier** : [`modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php)
📍 **Lignes 117-123** : retour final

```php
die(json_encode([
    'success'          => true,
    'id_order'         => $id_order,
    'previous_state'   => $current_state,
    'new_state'        => $new_state,
    'stock_updates'    => $this->stockUpdates,
]));
```

**Explication** :
- `die(...)` arrête immédiatement le script après l'envoi de la réponse.
- `stockUpdates` contient le détail des changements pour chaque produit (tels qu'accumulés à l'étape 6).
- Le statut HTTP par défaut est **200 OK**.

**Exemple de réponse** :
```json
{
  "success": true,
  "id_order": 42,
  "previous_state": 11,
  "new_state": 6,
  "stock_updates": [
    {
      "id_product": 112,
      "id_product_attribute": 133,
      "qty": 2,
      "physical_quantity": 10,
      "reserved_quantity": 0,
      "quantity": 10
    }
  ]
}
```

---

## 12. Étape 10 — Refresh de la liste côté Vue

**Mots-clés** : refresh, reload, UI update

📄 **Fichier** : [`NewApp/src/components/OrderList.vue`](../NewApp/src/components/OrderList.vue)
📍 **Lignes 217-220** : après succès, on recharge la liste

```js
const result = await OrderService.changeOrderState(item.id, newState)
console.log('✅ Résultat:', result)
successMessage.value = `Commande ${item.reference} ${label} (stock mis à jour)`
setTimeout(() => { successMessage.value = '' }, 3000)
await loadAll()
```

**Explication** :
- `successMessage.value` : affichage d'un toast vert pendant 3 secondes.
- `await loadAll()` : recharge toutes les commandes + paniers depuis l'API → le tableau affiche les valeurs à jour, et la commande annulée passe en état 6, ce qui :
  - Change la couleur de son badge dans le select
  - **Masque les boutons "Livrer" et "Annuler"** (qui sont conditionnés à `current_state === 11`)

---

## 13. Annulation via import (fichier 3 — état "annulé")

**Mots-clés** : import, fichier 3, état "annulé", flow batch

Le même flux peut être déclenché en masse via l'import du fichier 3 quand la colonne `etat` contient `annulé`.

📄 **Fichier** : [`NewApp/src/services/importService.js`](../NewApp/src/services/importService.js)
📍 **Lignes 482-486** : aiguillage selon `etat`

```js
const e = String(etat ?? '').toLowerCase().trim()
let needsOrder = false
let extraState = null
if (e.includes('accept'))   { needsOrder = true; extraState = null }
else if (e.includes('livr')) { needsOrder = true; extraState = 5 }
else if (e.includes('annul')) { needsOrder = true; extraState = 6 }
```

📍 **Lignes 663-690** (extrait) : enchaînement validation puis annulation

```js
// 1. Forcer l'état "Paiement à distance accepté" (11) ET réserver le stock
try {
  await axios.post('/api/order_state_change', {
    id_order: parseInt(orderId),
    new_state: 11,
  }, { headers: { 'Content-Type': 'application/json' } })
  console.log(`✅ Commande ${orderId} validée + stock réservé`)
} catch (err) { ... }

// 2. Si état "livré" ou "annulé", appliquer la transition supplémentaire
if (extraState) {
  try {
    await axios.post('/api/order_state_change', {
      id_order: parseInt(orderId),
      new_state: extraState,
    }, { headers: { 'Content-Type': 'application/json' } })
  } catch (err) { ... }
}
```

**Explication** :
- Toute commande créée passe **toujours** par l'état 11 d'abord (validation + réservation).
- Si la colonne `etat` du CSV est `annulé`, on enchaîne immédiatement avec un 2ᵉ appel `order_state_change(6)`.
- Au final, en BDD, l'historique de la commande contient :
  1. (création) → état 11
  2. (annulation) → état 6
- Le résultat net : `reserved_quantity` = 0, `quantity` = stock initial, `physical_quantity` = stock initial.

---

## 14. Impact détaillé sur le stock

**Mots-clés** : impact, stock, avant après

### Cas simple : commande de **1 unité** sur T_01/kely

**État initial** :
```
physical_quantity = 10
reserved_quantity = 0
quantity          = 10
```

**Après création de la commande (état 11)** :
```
physical_quantity = 10   (inchangé)
reserved_quantity = 1    (+1, réservation)
quantity          = 9    (-1, par PrestaShop)
```

**Après annulation (état 6)** :
```
physical_quantity = 10   (inchangé)
reserved_quantity = 0    (-1, libération)
quantity          = 10   (+1, par PrestaShop)
```

→ Retour à l'état initial. **Aucun mouvement dans `ps_stock_mvt`** n'est créé.

### Cas multi-produits

Une commande avec plusieurs lignes (ex. 2 kely + 1 ngoza) :
- La boucle `foreach ($order->getProducts() as $product)` traite chaque ligne séparément.
- Pour chacune : on diminue `reserved_quantity` du nombre commandé.
- Conséquence : si une ligne échoue (stock introuvable), les autres continuent à être traitées (skip défensif `continue`).

---

## 15. Cas d'erreur et messages associés

**Mots-clés** : erreur, HTTP, status, message

| Code HTTP | Message renvoyé | Cause | Référence code |
|:--:|---|---|---|
| 405 | `Method not allowed — only POST is supported` | Appel via GET/PUT/DELETE | [WebserviceSpecificManagementOrderStateChange.php:54-57](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php) |
| 400 | `id_order et new_state requis` | Body JSON sans `id_order` ou `new_state` | lignes 63-66 |
| 404 | `Commande N introuvable` | `id_order` n'existe pas en BDD | lignes 69-73 |
| 400 | `Seule une commande PAYÉE peut être annulée` | `current_state` ∉ {2, 11} lors d'une annulation | lignes 99-105 |
| 400 | `État non géré : N, supported: [2, 11, 5, 6]` | `new_state` autre que 2/5/6/11 | lignes 109-114 |

### Récupération côté Vue

📄 **Fichier** : [`NewApp/src/components/OrderList.vue`](../NewApp/src/components/OrderList.vue)
📍 **Lignes 222-225** : capture de l'erreur

```js
} catch (err) {
  console.error('❌ Erreur:', err)
  const msg = err.response?.data?.error || err.message
  errorMessage.value = `Échec : ${msg}`
}
```

→ Le message renvoyé par le serveur (`err.response.data.error`) est affiché dans un toast rouge à l'écran. Exemple : `"Échec : Seule une commande PAYÉE peut être annulée"`.

---

## 16. Récap des fichiers concernés

**Mots-clés** : récap, fichiers, mapping

| Fichier | Rôle | Lignes clés |
|---------|------|-------------|
| [NewApp/src/components/OrderList.vue](../NewApp/src/components/OrderList.vue) | UI bouton + handler + refresh | 82-91 (bouton), 207-210 (onCancel), 212-228 (handler), 217-220 (refresh) |
| [NewApp/src/services/orderService.js](../NewApp/src/services/orderService.js) | Wrapper axios POST JSON | 375-383 |
| [modules/orderstateapi/orderstateapi.php](../modules/orderstateapi/orderstateapi.php) | Déclaration module + permissions | hookAddWebserviceResources |
| [modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php](../modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php) | Logique métier annulation | 50-115 (manage), 99-108 (check métier), 240-274 (transitionToCancelled), 117-123 (réponse) |
| [classes/order/OrderHistory.php](../classes/order/OrderHistory.php) | `changeIdOrderState` natif PrestaShop | 83 (signature), 333-357 (gestion stock conditionnelle) |
| [classes/stock/StockAvailable.php](../classes/stock/StockAvailable.php) | ObjectModel sur ps_stock_available | (toute la classe) |
| [NewApp/src/services/importService.js](../NewApp/src/services/importService.js) | Annulation en masse via fichier 3 | 482-486 (aiguillage etat), 663-690 (double appel order_state_change) |

---

## 🔗 Documentation liée

- [`commande.md`](commande.md) — Vue d'ensemble du cycle de vie d'une commande (validation, livraison, annulation)
- [`PutPost.md`](PutPost.md) — Détails sur les appels POST custom (JSON) et les endpoints PrestaShop
- [`Service.md`](Service.md) — Patterns d'implémentation de services Vue ↔ PrestaShop
- [`View.md`](View.md) — Patterns Vue : confirmation, refresh, toast, bouton conditionnel
- [`variable.md`](variable.md) — `parseInt`, comparaisons strict (`===`), gestion des string venant du XML PrestaShop
