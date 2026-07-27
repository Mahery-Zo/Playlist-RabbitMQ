# 🛒 Guide complet : Processus de commande & gestion du stock

> Documentation exhaustive : tout le cycle de vie d'une commande, l'enchaînement des appels API, tous les états possibles, et l'impact sur les 3 colonnes de stock (`physical_quantity`, `reserved_quantity`, `quantity`).

> **Mots-clés généraux** : commande, order, cart, panier, état, state, paiement, livré, annulé, payment, delivered, cancelled, stock, physical, reserved, quantity, /api/orders, /api/carts, /api/order_state_change, /api/stock_delta, transitionToPaid, transitionToDelivered, transitionToCancelled, OrderHistory, changeIdOrderState, secure_key, current_state

---

## 📑 Index

- [1. Vue d'ensemble du cycle de vie d'une commande](#1-vue-densemble-du-cycle-de-vie-dune-commande)
- [2. Les 3 colonnes de stock — sémantique](#2-les-3-colonnes-de-stock--sémantique)
- [3. Les états de commande (`current_state`)](#3-les-états-de-commande-current_state)
- [4. Flow #1 — Ajout au panier (PrestaShop cart)](#4-flow-1--ajout-au-panier-prestashop-cart)
- [5. Flow #2 — Validation de commande (checkout)](#5-flow-2--validation-de-commande-checkout)
- [6. Flow #3 — Marquer comme "Livré"](#6-flow-3--marquer-comme-livré)
- [7. Flow #4 — Annulation d'une commande payée](#7-flow-4--annulation-dune-commande-payée)
- [8. Tableau récapitulatif des impacts sur le stock](#8-tableau-récapitulatif-des-impacts-sur-le-stock)
- [9. Cas particuliers : import de commandes (fichier 3)](#9-cas-particuliers--import-de-commandes-fichier-3)
- [10. Diagrammes de transition d'état](#10-diagrammes-de-transition-détat)
- [11. Architecture du module orderstateapi](#11-architecture-du-module-orderstateapi)
- [12. Création d'order — payload XML minimal](#12-création-dorder--payload-xml-minimal)
- [13. Cas d'erreur courants & contournements](#13-cas-derreur-courants--contournements)
- [14. Vérifications côté front (Vue) avant commande](#14-vérifications-côté-front-vue-avant-commande)
- [15. Cheatsheet rapide](#15-cheatsheet-rapide)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Vue d'ensemble du cycle de vie d'une commande

**Mots-clés** : cycle, lifecycle, processus

```
┌─────────────┐
│  Panier     │  cart créé (id_cart) sans commande associée
│  (cart)     │  → produits réservés en mémoire, BDD non engagée
└──────┬──────┘
       │ checkout
       ▼
┌─────────────┐
│ Paiement    │  current_state = 11
│ accepté     │  → reserved_quantity ↑   quantity ↓   physical_quantity =
└──────┬──────┘
       │
       │  Choix : (a) Livraison    (b) Annulation
       │
   ┌───┴───────────────────────┐
   ▼                           ▼
┌─────────────┐         ┌─────────────┐
│  Livré      │         │  Annulé     │
│  (5)        │         │  (6)        │
└──────┬──────┘         └──────┬──────┘
       │                       │
  res ↓                  res ↓
  qty =                  qty ↑   (par PrestaShop)
  phys ↓                 phys =
  + stock_mvt (-)        Aucun stock_mvt créé
```

**Règle métier** : seule une commande **payée** (état 11) peut être annulée (état 6) ou livrée (état 5).

---

## 2. Les 3 colonnes de stock — sémantique

**Mots-clés** : sémantique, colonnes, physical_quantity, reserved_quantity, quantity

Table `ps_stock_available` :

| Colonne | Signification | Modifiée par |
|---------|---------------|--------------|
| `physical_quantity` | Stock **physique réel** présent en magasin / entrepôt | Livraison uniquement (5) |
| `reserved_quantity` | Engagements (clients ayant payé mais pas encore livré) | Validation (11) +N, Livraison (5) -N, Annulation (6) -N |
| `quantity` | Stock **vendable / disponible** affiché en boutique | Validation (11) -N, Annulation (6) +N |

### Formule invariante (devrait toujours tenir)

```
physical_quantity = reserved_quantity + quantity + (commandes livrées passées)
```

En pratique, après les imports + actions de gestion :
- `physical_quantity` = ce qu'il reste vraiment dans le magasin
- `reserved_quantity` = les unités promises à des clients
- `quantity` = ce qu'on peut encore vendre = `physical_quantity − reserved_quantity` (idéalement)

### Exemple chronologique pour T_01/kely (stock initial = 10)

| Action | phys | reserved | qty | Commentaire |
|--------|:---:|:---:|:---:|---|
| Stock initial (file 2) | 10 | 0 | 10 | rien commandé |
| Validation Rajao ×2 | 10 | 2 | 8 | 2 unités réservées |
| Validation Rakoto ×1 | 10 | 3 | 7 | 3 unités réservées |
| Livraison Rakoto ×4 (= validation + livraison) | 6 | 3 | 3 | -4 phys, +0 net sur res, -4 qty |
| Annulation Rajao ×2 (= validation + annulation) | 6 | 3 | 3 | net rien (+2 res puis -2, -2 qty puis +2) |

---

## 3. Les états de commande (`current_state`)

**Mots-clés** : state, current_state, PS_OS_PAYMENT, configurer

PrestaShop fournit des constantes pour les états par défaut :

| ID | Constante | Libellé | `shipped` | `paid` | `logable` |
|:--:|-----------|---------|:---:|:---:|:---:|
| 2 | `PS_OS_PAYMENT` | Paiement accepté | 0 | 1 | 1 |
| 3 | `PS_OS_PREPARATION` | Préparation en cours | 0 | 1 | 1 |
| 4 | `PS_OS_SHIPPING` | Expédié | 1 | 1 | 1 |
| 5 | `PS_OS_DELIVERED` | Livré | 1 | 1 | 1 |
| 6 | `PS_OS_CANCELED` | Annulé | 0 | 0 | 0 |
| 7 | `PS_OS_REFUND` | Remboursé | 0 | 0 | 0 |
| 8 | `PS_OS_ERROR` | Erreur de paiement | 0 | 0 | 0 |
| 11 | `PS_OS_WS_PAYMENT` | Paiement à distance accepté | 0 | 1 | 1 |

Dans ce projet, on utilise principalement :
- **11** = Paiement accepté (cas standard du checkout)
- **5** = Livré (action manuelle dans OrderAdmin)
- **6** = Annulé (action manuelle dans OrderAdmin)

---

## 4. Flow #1 — Ajout au panier (PrestaShop cart)

**Mots-clés** : cart, panier, addToCart

### Côté Vue

L'utilisateur clique sur "Ajouter au panier" :

```js
// ProductDetail.vue → addToCart()
await cartStore.addToCart(
  product.value,
  quantity.value,
  selectedCombination.value?.id || null,
  selectedOptions.value
)
```

### Côté Store (cartStore.js)

1. Vérifier le stock via `StockService.getStock(productId, combinationId)`
2. Si suffisant, ajouter à `items` (state local) et `localStorage`
3. Appeler `syncWithPrestaShop()` qui :
   - Si `prestashopCartId` n'existe pas → `POST /api/carts` (création)
   - Sinon → `PUT /api/carts/{id}` (mise à jour)

### Côté serveur PrestaShop

```
POST /api/carts
Body XML :
<prestashop>
  <cart>
    <id_customer>26</id_customer>
    <id_address_delivery>30</id_address_delivery>
    <id_address_invoice>30</id_address_invoice>
    <id_currency>1</id_currency>
    <id_lang>1</id_lang>
    <id_shop>1</id_shop>
    <id_carrier>2</id_carrier>
    <associations>
      <cart_rows>
        <cart_row>
          <id_product>92</id_product>
          <id_product_attribute>108</id_product_attribute>
          <quantity>1</quantity>
          <id_address_delivery>30</id_address_delivery>
        </cart_row>
      </cart_rows>
    </associations>
  </cart>
</prestashop>
```

### Impact stock

**Aucun**. Un cart est juste une intention d'achat. Pas de modification sur `physical`, `reserved` ou `quantity`.

---

## 5. Flow #2 — Validation de commande (checkout)

**Mots-clés** : checkout, validation, paiement accepté, état 11

### Côté Vue (CheckoutView.vue)

```js
const validateOrder = async () => {
  // 1. Vérifier stock + customer + address
  await cartStore.validateStock()

  // 2. Sauvegarder le cart côté PrestaShop
  const cartResult = await cartStore.saveToPrestaShop(cartData)

  // 3. Créer la commande
  const orderData = {
    id_cart: cartResult.cart_id,
    id_customer: customer.id,
    id_address_delivery, id_address_invoice,
    id_carrier: 1,
    module: 'ps_cashondelivery',
    payment: 'Paiement à la livraison',
    total_paid: cartStore.totalPrice,
    // ...
  }
  const orderResult = await OrderService.createOrder(orderData)
}
```

### Côté Service (orderService.createOrder)

3 étapes :

1. **POST `/api/orders`** avec `current_state: 11` dans le XML
2. **Fallback** si la réponse est vide : `GET /api/orders?filter[id_cart]=X&sort=[id_DESC]&limit=1` pour retrouver l'ID
3. **POST `/api/order_state_change`** avec `{ id_order, new_state: 11 }` pour activer la logique métier :
   - `reserved_quantity += qty` (par item)
   - `quantity` est décrémenté automatiquement par PrestaShop (lors de la création de l'order)

### Côté module orderstateapi (`transitionToPaid`)

```php
private function transitionToPaid(Order $order, $new_state)
{
    // 1. Si l'état n'est pas déjà 11, le forcer
    if ((int)$order->current_state !== $new_state) {
        $history = new OrderHistory();
        $history->id_order = (int)$order->id;
        $history->id_order_state = $new_state;
        $history->changeIdOrderState($new_state, $order);
        $history->add();
    }

    // 2. Réserver le stock : reserved++ (pas de touche à physical_quantity)
    foreach ($order->getProducts() as $product) {
        $id_stock = StockAvailable::getStockAvailableIdByProductId(
            (int)$product['product_id'],
            (int)$product['product_attribute_id']
        );
        $stock = new StockAvailable($id_stock);
        $stock->reserved_quantity = (int)$stock->reserved_quantity + (int)$product['product_quantity'];
        $stock->quantity = max(0, (int)$stock->quantity - (int)$product['product_quantity']);
        $stock->update();
    }
}
```

### Impact stock (pour 1 unité commandée)

| Avant | Action | Après |
|-------|--------|-------|
| phys=10, res=0, qty=10 | Validation | phys=10, res=**1**, qty=**9** |

`physical_quantity` **NE BOUGE PAS** — le produit est encore physiquement en magasin, juste réservé.

---

## 6. Flow #3 — Marquer comme "Livré"

**Mots-clés** : livré, delivered, état 5, transitionToDelivered

### Déclencheur

Bouton "📦 Livrer" dans [OrderList.vue](../NewApp/src/components/OrderList.vue), visible pour les commandes en état 11 :

```vue
<button v-if="item.type === 'order' && parseInt(item.current_state) === 11"
        @click="onDeliver(item)">
  📦 Livrer
</button>
```

```js
const onDeliver = async (item) => {
  if (!confirm(`Marquer la commande ${item.reference} comme LIVRÉE ?`)) return
  await OrderService.changeOrderState(item.id, 5)
  await loadAll()
}
```

### Côté Service

```js
async changeOrderState(orderId, newState) {
  const res = await axios.post('/api/order_state_change', {
    id_order: orderId,
    new_state: newState,
  }, { headers: { 'Content-Type': 'application/json' } })
  return res.data
}
```

### Côté module orderstateapi (`transitionToDelivered`)

```php
private function transitionToDelivered(Order $order)
{
    // 1. Changement d'état via OrderHistory (déclenche les hooks)
    $history = new OrderHistory();
    $history->id_order = (int)$order->id;
    $history->id_order_state = 5;
    $history->changeIdOrderState(5, $order);
    $history->add();

    // 2. Diminution du stock physique + log dans stock_mvt
    foreach ($order->getProducts() as $product) {
        $id_stock = StockAvailable::getStockAvailableIdByProductId(...);
        $stock = new StockAvailable($id_stock);
        $stock->physical_quantity = max(0, (int)$stock->physical_quantity - $qty);
        $stock->reserved_quantity = max(0, (int)$stock->reserved_quantity - $qty);
        $stock->update();

        // Mouvement de stock (sortie)
        Db::getInstance()->insert('stock_mvt', [
            'id_stock' => $id_stock,
            'physical_quantity' => $qty,
            'sign' => -1,
            'id_order' => (int)$order->id,
            'id_stock_mvt_reason' => Configuration::get('PS_STOCK_CUSTOMER_ORDER_REASON'),
            // ...
        ]);
    }
}
```

### Impact stock (pour 1 unité)

| Avant | Action | Après |
|-------|--------|-------|
| phys=10, res=1, qty=9 | Livré | phys=**9**, res=**0**, qty=9 |

- `physical_quantity` **↓** (le produit sort réellement du stock)
- `reserved_quantity` **↓** (la promesse au client est honorée)
- `quantity` **inchangé** (déjà décrémenté lors de la validation)
- Une ligne `stock_mvt` créée (`sign=-1`) → visible dans le tableau "Evolution journalière"

---

## 7. Flow #4 — Annulation d'une commande payée

**Mots-clés** : annulé, cancelled, état 6, transitionToCancelled

### Déclencheur

Bouton "❌ Annuler" dans OrderList.vue, **visible uniquement** pour les commandes en état 11.

```vue
<button v-if="item.type === 'order' && parseInt(item.current_state) === 11"
        @click="onCancel(item)">
  ❌ Annuler
</button>
```

### Vérification métier

Le module refuse si l'état actuel n'est pas un état "payé" (2 ou 11) :

```php
if (!in_array($current_state, $paid_states, true)) {
    http_response_code(400);
    die(json_encode([
        'error' => 'Seule une commande PAYÉE peut être annulée',
        'current_state' => $current_state,
    ]));
}
```

### Côté module orderstateapi (`transitionToCancelled`)

```php
private function transitionToCancelled(Order $order)
{
    // 1. Changement d'état
    $history = new OrderHistory();
    $history->id_order = (int)$order->id;
    $history->id_order_state = 6;
    $history->changeIdOrderState(6, $order);
    $history->add();

    // 2. Libérer la réservation (quantity est ré-incrémenté par PrestaShop)
    // Aucun stock_mvt n'est créé : pas de mouvement physique réel.
    foreach ($order->getProducts() as $product) {
        $id_stock = StockAvailable::getStockAvailableIdByProductId(...);
        $stock = new StockAvailable($id_stock);
        $stock->reserved_quantity = max(0, (int)$stock->reserved_quantity - $qty);
        $stock->update();
    }
}
```

### Impact stock (pour 1 unité)

| Avant | Action | Après |
|-------|--------|-------|
| phys=10, res=1, qty=9 | Annulé | phys=10, res=**0**, qty=**10** |

- `physical_quantity` **inchangé** (jamais sorti du magasin)
- `reserved_quantity` **↓** (libération)
- `quantity` **↑** (par PrestaShop, retour vendable)
- **Aucun** `stock_mvt` créé (la marchandise n'a jamais quitté le magasin)

### ⚠️ Note importante

Si tu voulais annuler une commande **déjà livrée** (état 5), il faudrait alors créer un `stock_mvt sign=+1` (vrai retour client). Mais ce projet n'autorise pas ce cas (seules les commandes en état 11 peuvent être annulées).

---

## 8. Tableau récapitulatif des impacts sur le stock

**Mots-clés** : récap, table, impacts

Pour **1 unité commandée**, voici les variations exactes (`Δ` = variation, `=` = inchangé) :

| Transition | `physical_quantity` | `reserved_quantity` | `quantity` | `stock_mvt` créé |
|------------|:---:|:---:|:---:|:---:|
| (Cart → ) Validation 11 | = | **+1** | **−1** (auto PS) | Non |
| Validation 11 → Livré 5 | **−1** | **−1** | = | **Oui** (`sign=-1`) |
| Validation 11 → Annulé 6 | = | **−1** | **+1** (auto PS) | Non |

### Cycle complet

Une commande qui parcourt **Validation → Livré** sur 1 unité :
```
phys:  10 → 10 → 9   (−1 net)
res:   0  → 1  → 0   (0 net, transit)
qty:   10 → 9  → 9   (−1 net)
```

Une commande qui parcourt **Validation → Annulé** sur 1 unité :
```
phys:  10 → 10 → 10  (0 net ✅)
res:   0  → 1  → 0   (0 net ✅)
qty:   10 → 9  → 10  (0 net ✅)
```

→ L'annulation **restaure intégralement** l'état pré-commande.

---

## 9. Cas particuliers : import de commandes (fichier 3)

**Mots-clés** : import, fichier 3, états, vide, livré

Dans l'import, la colonne `etat` détermine le flow appliqué :

| `etat` CSV | Flow exécuté | Résultat final |
|------------|--------------|----------------|
| (vide) | Création du cart seulement | Cart créé, **pas d'order** |
| `paiement accepté` | Validation 11 | Order état 11 + réservation |
| `livré` | Validation 11 **puis** Livraison 5 | Order état 5 + sortie phys |
| `annulé` | Validation 11 **puis** Annulation 6 | Order état 6 + restauration |

### Code dans `createCartAndOrder` (importService.js)

```js
const e = String(etat ?? '').toLowerCase().trim()
let needsOrder = false
let extraState = null

if (e.includes('accept'))   { needsOrder = true; extraState = null }
else if (e.includes('livr')) { needsOrder = true; extraState = 5 }
else if (e.includes('annul')) { needsOrder = true; extraState = 6 }

// Création du cart, puis (si needsOrder) création de l'order
// puis appel à /api/order_state_change(11) — réservation
// puis (si extraState) appel à /api/order_state_change(extraState)
```

### Vérification de stock avant chaque commande

L'import maintient un `stockTracker` en mémoire pour vérifier que chaque commande payée/livrée ne dépasse pas le stock disponible. Sinon → ligne sautée avec message clair :

```
Stock insuffisant pour T_01/kely : 20 demandé(s), seulement 3 disponible(s)
```

---

## 10. Diagrammes de transition d'état

**Mots-clés** : diagramme, transition, machine à états

### Machine à états des commandes du projet

```
                  +────────────+
       paiement   │            │  livraison
          ─────►  │  Validé 11 │  ─────►   +─────────+
                  │            │           │  Livré 5 │
                  +────────────+           +─────────+
                       │
                       │ annulation
                       ▼
                  +────────────+
                  │  Annulé 6  │
                  +────────────+
```

Règles :
- Une commande arrive en **11** dès sa création (POST /api/orders + transitionToPaid)
- De **11**, on peut aller vers **5** (livraison) ou **6** (annulation)
- Les états **5** et **6** sont **terminaux** dans ce projet (pas de retour en arrière)

### États non utilisés mais possibles à ajouter

- 3 (Préparation) — étape intermédiaire avant livraison
- 4 (Expédié) — entre validation et livraison
- 7 (Remboursé) — variante d'annulation

---

## 11. Architecture du module orderstateapi

**Mots-clés** : module, orderstateapi, architecture

```
modules/orderstateapi/
├── orderstateapi.php
│     ├── install()                    → enregistre les permissions POST
│     └── hookAddWebserviceResources() → déclare la resource "order_state_change"
└── classes/
    └── WebserviceSpecificManagementOrderStateChange.php
          ├── manage()                  → entry point, dispatcher
          ├── transitionToPaid()        → état 11/2 (réservation)
          ├── transitionToDelivered()   → état 5 (livraison)
          └── transitionToCancelled()   → état 6 (annulation)
```

### Endpoint exposé

```
POST /api/order_state_change
Body JSON : { id_order: N, new_state: 11|5|6 }
```

### Réponses

**Succès** :
```json
{
  "success": true,
  "id_order": 42,
  "previous_state": 11,
  "new_state": 5,
  "stock_updates": [
    {
      "id_product": 112,
      "id_product_attribute": 133,
      "qty": 2,
      "physical_quantity": 8,
      "reserved_quantity": 0,
      "quantity": 9
    }
  ]
}
```

**Erreur** (annulation sur commande non payée) :
```json
{
  "error": "Seule une commande PAYÉE peut être annulée",
  "current_state": 8,
  "paid_states": [2, 11]
}
```

---

## 12. Création d'order — payload XML minimal

**Mots-clés** : XML, payload, créer, secure_key

Champs **obligatoires** pour `POST /api/orders` :

```xml
<prestashop>
  <order>
    <id_address_delivery>20</id_address_delivery>
    <id_address_invoice>20</id_address_invoice>
    <id_cart>49</id_cart>
    <id_currency>1</id_currency>
    <id_lang>1</id_lang>
    <id_customer>26</id_customer>
    <id_carrier>1</id_carrier>
    <current_state>11</current_state>
    <module>ps_cashondelivery</module>
    <payment>Paiement à la livraison</payment>
    <secure_key>89bd44f825e5ac35dd3f2eea75e7081f</secure_key>
    <valid>1</valid>

    <!-- Totaux : OBLIGATOIRES, format 6 décimales -->
    <total_paid>15.000000</total_paid>
    <total_paid_real>15.000000</total_paid_real>
    <total_paid_tax_incl>15.000000</total_paid_tax_incl>
    <total_paid_tax_excl>12.500000</total_paid_tax_excl>
    <total_products>12.500000</total_products>
    <total_products_wt>15.000000</total_products_wt>
    <total_shipping>0.000000</total_shipping>
    <total_shipping_tax_incl>0.000000</total_shipping_tax_incl>
    <total_shipping_tax_excl>0.000000</total_shipping_tax_excl>
    <total_discounts>0.000000</total_discounts>
    <total_discounts_tax_incl>0.000000</total_discounts_tax_incl>
    <total_discounts_tax_excl>0.000000</total_discounts_tax_excl>
    <total_wrapping>0.000000</total_wrapping>
    <total_wrapping_tax_incl>0.000000</total_wrapping_tax_incl>
    <total_wrapping_tax_excl>0.000000</total_wrapping_tax_excl>
    <conversion_rate>1.000000</conversion_rate>

    <date_add>2026-05-19 12:00:00</date_add>
  </order>
</prestashop>
```

### secure_key — format

32 caractères hexadécimaux (format MD5) :

```js
const secureKey = Array.from({ length: 32 }, () =>
  Math.floor(Math.random() * 16).toString(16)
).join('')
```

### ⚠️ Si tu oublies un champ

PrestaShop renvoie une erreur 400 avec un message du type :
```
total_paid_tax_incl is required
```

Le helper `postXml` extrait ce message via une regex sur `<message><![CDATA[...]]></message>`.

---

## 13. Cas d'erreur courants & contournements

**Mots-clés** : erreur, debug, troubleshoot

### 13.1 Réponse vide `<prestashop></prestashop>`

**Symptôme** : `result.prestashop.order` est undefined.
**Cause** : un module tiers (gamification, ps_emailalerts, ps_eventbus) plante pendant la cascade de hooks. La commande EST créée en BDD, mais la réponse a été vidée.

**Contournement** dans createOrder :
```js
let order = result?.prestashop?.order
if (!order) {
  // Retrouver via id_cart
  const fallback = await axios.get(
    `/api/orders?filter[id_cart]=${orderData.id_cart}&display=full&sort=[id_DESC]&limit=1`
  )
  order = parser.parse(fallback.data)?.prestashop?.orders?.order
}
```

### 13.2 Adresse référencée n'existe pas

**Symptôme** : `POST /api/orders` retourne 400.
**Cause** : `localStorage.selectedCustomer.addresses[0].id` pointe vers une adresse qui a été supprimée (après reset, par exemple).
**Fix** : refaire `CustomerService.getCustomerAddresses(customerId)` pour resync.

### 13.3 Commande créée en état 8 (erreur paiement) malgré `current_state=11`

**Cause** : `gamification` ou autre module utilise un hook deprecated qui throw → PrestaShop met l'état à 8.
**Fix** : `DELETE FROM ps_hook_module WHERE id_module = (SELECT id_module FROM ps_module WHERE name = 'gamification')`

### 13.4 `quantity` décrémenté 2 fois

**Cause** : PrestaShop décrémente automatiquement `quantity` lors du POST /api/orders avec `current_state=11`, et notre code re-décrémente.
**Fix** : `transitionToPaid` ne touche **que** `reserved_quantity` côté addition ; le décrément de `quantity` est laissé à PrestaShop.

### 13.5 `StockManager::saveMovement` ne crée pas de ligne dans `stock_mvt`

**Cause** : en contexte webservice, `SymfonyContainer::getInstance()` retourne null → `saveMovement` retourne false silencieusement.
**Fix** : `INSERT` direct via `Db::getInstance()->insert('stock_mvt', [...])`.

---

## 14. Vérifications côté front (Vue) avant commande

**Mots-clés** : validation, front, customer, address, stock

Avant d'autoriser le checkout, on vérifie côté Vue :

```js
// CheckoutView.validateOrder()
1. selectedCustomer existe                  → sinon "Sélectionner un client"
2. deliveryAddress existe                   → sinon "Adresse manquante"
3. cartStore.items.length > 0               → sinon "Panier vide"
4. cartStore.validateStock()                → re-fetch des stocks pour vérifier
5. Calcul du total                          → cartStore.totalPrice
6. POST cart + POST order
```

### Méthode `validateStock()` du cartStore

```js
const validateStock = async () => {
  const errors = []
  for (const item of items.value) {
    const available = await checkStock(item.product_id, item.combination_id)
    if (item.quantity > available) {
      errors.push(`${item.name}: seulement ${available} disponible(s)`)
    }
  }
  if (errors.length > 0) throw new Error(errors.join('\n'))
}
```

→ Cela empêche la création d'une commande si le stock vient juste de changer (concurrence).

---

## 15. Cheatsheet rapide

**Mots-clés** : cheatsheet, mémo, antisèche

```
╔══════════════════════════════════════════════════════════════╗
║          PROCESSUS DE COMMANDE — RAPPELS                     ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  ÉTATS UTILISÉS                                              ║
║    11  Paiement accepté (création standard)                  ║
║    5   Livré                                                 ║
║    6   Annulé (depuis 11 uniquement)                         ║
║                                                              ║
║  ENDPOINTS                                                   ║
║    POST /api/carts                  → créer cart             ║
║    POST /api/orders                 → créer order            ║
║    POST /api/order_state_change     → changer état (custom)  ║
║                                                              ║
║  PAYLOAD order_state_change                                  ║
║    { id_order, new_state }                                   ║
║                                                              ║
║  IMPACTS STOCK (par unité)                                   ║
║    Validation 11 :   res +1   qty -1   phys =                ║
║    Livraison 5  :    res -1   qty =    phys -1  +stock_mvt   ║
║    Annulation 6 :    res -1   qty +1   phys =                ║
║                                                              ║
║  RÈGLE MÉTIER                                                ║
║    Annulation autorisée UNIQUEMENT depuis état 11 ou 2       ║
║                                                              ║
║  FALLBACK RÉPONSE VIDE                                       ║
║    GET /api/orders?filter[id_cart]=X&sort=[id_DESC]&limit=1  ║
║                                                              ║
║  STOCK_MVT créé                                              ║
║    UNIQUEMENT pour Livraison (vrai mouvement physique)       ║
║                                                              ║
║  CHAMPS OBLIGATOIRES order                                   ║
║    id_cart, id_customer, id_address_*, secure_key (32 hex),  ║
║    valid=1, current_state, module, payment, tous les total_* ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Vue d'ensemble du cycle | `cycle de vie` |
| Sémantique des 3 colonnes | `3 colonnes de stock` |
| Liste des états | `états de commande` |
| Flow add to cart | `Flow #1` ou `Ajout au panier` |
| Flow validation | `Flow #2` ou `Validation` |
| Flow livraison | `Flow #3` ou `Livré` |
| Flow annulation | `Flow #4` ou `Annulation` |
| Tableau d'impacts | `récapitulatif des impacts` |
| Import fichier 3 | `import de commandes` |
| Diagramme état | `transition d'état` |
| Module orderstateapi | `architecture du module` |
| XML obligatoire | `payload XML minimal` |
| Réponse vide | `Cas d'erreur courants` |
| Validation front | `Vérifications côté front` |
| Rappel express | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |
| Pagination | Voir [`listePagination.md`](listePagination.md) |
| POST / PUT | Voir [`PutPost.md`](PutPost.md) |
| Variables / types | Voir [`variable.md`](variable.md) |
| Filtres | Voir [`filter.md`](filter.md) |
| Import / extension | Voir [`ImportExprt.md`](ImportExprt.md) |
