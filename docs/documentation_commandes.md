# Documentation : Logique d'importation et gestion des Commandes

Ce document détaille la logique complète de traitement des commandes (Fichier 3) entre l'application d'importation (Vue.js) et le backend PrestaShop. 

Il explique la mécanique de création, les appels API, ainsi que la gestion intelligente des dates et des mouvements de stock.

---

## 1. Vue d'ensemble du flux (Workflow)

Lorsqu'une ligne du "Fichier 3" (CSV des commandes) est lue par l'importateur, le script suit le flux logique suivant :

1. **Vérification du stock mémoire** : Avant toute action, le script vérifie si le stock local (dans le *stockTracker*) est suffisant pour honorer la commande.
2. **Création du panier (Cart)** : Une requête est envoyée à l'API PrestaShop pour générer un `id_cart`.
3. **Création de la commande (Order)** : À partir du panier, la commande est officiellement créée dans PrestaShop (par défaut à un état initial "Attente").
4. **Forçage de la date (PUT Order)** : La date de la commande est réécrite pour correspondre à celle du CSV.
5. **Changement d'état et Mouvements de stock** : L'état de la commande est mis à jour (ex: "Paiement accepté"). Cela déclenche la réservation du stock et la création du mouvement de stock (avec la date historique).

---

## 2. Détail des API et des codes associés

### A. La vérification du stock mémoire
Avant d'interroger PrestaShop, le script simule la consommation de stock via une variable locale (`stockTracker`) pour éviter de créer des commandes partielles si le stock CSV vient à manquer en cours d'import.

> [!NOTE]
> **Fichier concerné :** `NewApp/src/services/importService.js` (Lignes 863-892)

```javascript
// Si la commande consomme du stock (ex: Livré ou Payé)
if (consumesStock(row.etat)) {
  for (const it of items) {
    const key = `${it.idProduct}|${it.idCombination}`
    const available = stockTracker.get(key) ?? 0
    if (it.qty > available) {
      throw new Error(`Stock insuffisant...`)
    }
  }
}
```

### B. Création du Panier (`POST /api/carts`)
L'application génère un flux XML décrivant les produits et la quantité, puis demande à PrestaShop de créer un panier.

- **API utilisée :** Native de PrestaShop Web Services
- **Rôle :** Préparer le contenant (nécessaire avant toute commande)
- **Fichier :** `importService.js` (Vers la ligne 500 dans `createCartAndOrder`)

```javascript
const cartXml = builder.build({
  prestashop: {
    cart: {
      id_currency: 1,
      id_lang: 1,
      id_customer: idCustomer,
      // ... détails des produits (cart_row)
    }
  }
})
// Envoi du XML à PrestaShop
const cartResult = await postXml('/api/carts', cartXml)
const cartId = txt(cartResult.prestashop.cart.id)
```

### C. Création de la Commande (`POST /api/orders`)
Une fois le `cartId` obtenu, la commande est créée. 

- **API utilisée :** Native PrestaShop
- **Rôle :** Transformer le panier en commande ferme.

```javascript
// Dans importService.js (vers la ligne 540)
const orderXml = builder.build({
  prestashop: {
    order: {
      id_cart: cartId,
      id_currency: 1,
      id_lang: 1,
      id_customer: idCustomer,
      current_state: 1, // État temporaire (ex: Attente)
      module: 'ps_wirepayment',
      payment: 'Virement bancaire',
      date_add: `${date} 12:00:00`, // Tentative d'imposer la date dès la création
      // ... montants et totaux
    }
  }
})
const orderResult = await postXml('/api/orders', orderXml)
```

### D. Forçage de la date de création (`PUT /api/orders/{id}`)
PrestaShop ignore souvent la date passée lors de la création et force la date actuelle (NOW). Une deuxième requête (`PUT`) est donc nécessaire pour **écraser la date** afin de préserver l'historique de l'ancien système.

> [!TIP]
> C'est cette étape qui permet à la liste de vos commandes (ainsi qu'au calcul de CA de votre Dashboard) d'afficher les ventes sur les mois/années passés.

```javascript
// Dans importService.js (Ligne 575)
const getOrderRes = await axios.get(`/api/orders/${orderId}`);
const orderData = parser.parse(getOrderRes.data);

// Réécriture de la date d'ajout et de mise à jour
orderData.prestashop.order.date_add = `${date} 12:00:00`;
orderData.prestashop.order.date_upd = `${date} 12:00:00`;

// Suppression des champs générant des bugs (Error 500 chez PS)
delete orderData.prestashop.order.associations; 
delete orderData.prestashop.order.shipping_number; 

await axios.put(`/api/orders/${orderId}`, builder.build(orderData), /* headers */);
```

---

## 3. Gestion experte des statuts et mouvements de stock

C'est ici que se joue la logique complexe de l'évolution journalière du stock. Pour qu'une commande modifie le stock dans PrestaShop, il faut changer son statut (`current_state`).

### A. L'appel Front-End (`POST /api/order_state_change`)
Le script JS envoie une requête personnalisée en JSON (non pas en XML) pour forcer le statut (ex: `11` = Paiement accepté, ou `5` = Livré) tout en transmettant la date historique.

- **Fichier concerné :** `importService.js` (Ligne 595)

```javascript
await axios.post('/api/order_state_change', {
  id_order: parseInt(orderId),
  new_state: 11,
  date_add: `${date} 12:00:00`, // <-- CRUCIAL : Transmission de la date CSV
}, { headers: { 'Content-Type': 'application/json' } })
```

### B. Le Module Backend (`WebserviceSpecificManagementOrderStateChange.php`)
C'est un endpoint (module) PrestaShop sur mesure. Il intercepte cette requête JSON et traite la logique métier.

> [!IMPORTANT]
> **Fichier concerné :** `modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`

**1. Récupération des paramètres :**
```php
// Ligne 58 : On extrait la commande, le nouvel état, ET la date historique
$input     = json_decode(file_get_contents('php://input'), true);
$id_order  = (int)($input['id_order'] ?? 0);
$new_state = (int)($input['new_state'] ?? 0);
$this->date_add = $input['date_add'] ?? date('Y-m-d H:i:s'); // Date par défaut = NOW
```

**2. Réservation de stock (État `11` ou `2`) :**
Le module appelle le cœur de PrestaShop pour changer le statut. Cela va décrémenter automatiquement la quantité disponible. Le code ajoute aussi une "réservation" du stock physique.

**3. Mouvement de stock physique (État `5` - Livré) :**
C'est le moment précis où les produits quittent comptablement l'entrepôt.
Le code contourne une limitation de PrestaShop (en webservice) en **insérant manuellement le mouvement** dans la table `ps_stock_mvt`.

```php
// Ligne 208 : Écriture directe dans la table des mouvements
Db::getInstance()->insert('stock_mvt', [
    'id_stock'            => $id_stock,
    'id_employee'         => 0,
    'physical_quantity'   => $qty,
    'id_stock_mvt_reason' => $reason,
    'id_order'            => (int)$order->id,
    'sign'                => -1, // -1 signifie SORTIE de stock
    'date_add'            => $this->date_add, // <-- UTILISATION de la date historique
]);
```

> [!TIP]
> **Impact sur le Dashboard :**
> Puisque le script PHP utilise la propriété `$this->date_add` transmise par le Vue.js, le mouvement de stock est inscrit dans la base de données avec la date passée (celle du fichier 3). C'est ce qui garantit que l'historique d'évolution journalière du stock du tableau de bord soit exact.

---

## 4. Tableau Récapitulatif des Endpoints

| Endpoint API | Méthode | Format | Action principale | Modifie le stock ? |
|--------------|---------|--------|-------------------|--------------------|
| `/api/carts` | POST | XML | Crée le conteneur panier | Non |
| `/api/orders` | POST | XML | Crée la commande "vierge" | Non (stock théorique uniquement) |
| `/api/orders/{id}`| PUT | XML | Force la date `date_add` | Non |
| `/api/order_state_change` | POST | JSON | Change l'état de la commande et gère la logique de mouvement de stock selon le statut | **OUI** (Crée des `ps_stock_mvt` datés) |
