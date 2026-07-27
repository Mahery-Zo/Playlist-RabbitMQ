# Documentation Globale : Écosystème des Commandes et Stocks

Cette documentation couvre l'intégralité de la logique de gestion des commandes et de leurs impacts sur les stocks dans votre application Vue.js interconnectée avec PrestaShop. Elle sert de manuel de référence pour comprendre, utiliser et maintenir les appels API.

---

## 1. Architecture Générale

L'application communique avec PrestaShop de deux manières :
1. **API Native (Web Services PrestaShop)** : Utilisée pour la lecture (GET) et la création de base (POST) au format XML.
2. **API Personnalisées (Modules PHP sur-mesure)** : Utilisées pour contourner les limitations de PrestaShop (par exemple, gérer les stocks sans erreur 500, forcer les dates d'historique). Elles utilisent le format JSON, plus léger et moderne.

---

## 2. Mode d'emploi des APIs Natives (XML)

### A. Récupération des données (GET)
Ces appels sont principalement centralisés dans le fichier `NewApp/src/services/OrderService.js`.

* **`GET /api/orders`**
  * **Usage :** Lister toutes les commandes ou filtrer par client (`?filter[id_customer]=1`).
  * **Effet :** Retourne les données brutes de la commande (totaux, `current_state`, `id_cart`).
  * **Exemple de code :** `OrderService.js` (Ligne 24)
    ```javascript
    const res = await axios.get(`/api/orders?filter[id_customer]=${customerId}&display=full&sort=[id_DESC]`);
    ```

* **`GET /api/order_states/{id}` & `GET /api/carriers/{id}`**
  * **Usage :** Les API PrestaShop ne renvoient que des IDs. Il faut faire une 2ème requête pour obtenir le texte (ex: "Paiement accepté" ou "Colissimo").
  * **Exemple de code :** `OrderService.js` (Ligne 47)
    ```javascript
    const stateRes = await axios.get(`/api/order_states/${currentState}?display=full`);
    ```

### B. Création de Commandes (POST / PUT)
Créer une commande via l'API native est un processus en deux étapes obligatoires.

* **1. `POST /api/carts` (Création du Panier)**
  * **Usage :** On ne peut pas créer de commande sans panier. Ce Endpoint lie un client à une liste de produits (`cart_rows`).
  * **Mode d'emploi :** Fournir un objet XML avec `id_customer`, `id_currency`, et les produits (ID + ID combinaison + quantité).
  * **Exemple :** `OrderService.js` (Ligne 434) - Méthode `createCart()`

* **2. `POST /api/orders` (Génération de la commande)**
  * **Usage :** Transforme le panier en commande.
  * **Effet :** Crée la commande en base de données. **Attention :** Par défaut, cela ne déduit pas le stock physique (le statut initial "En attente" ne déclenche que des réservations selon la config).
  * **Exemple :** `importService.js` (Ligne 540) - Méthode `createCartAndOrder()`

* **3. `PUT /api/orders/{id}` (Modification d'une commande)**
  * **Usage :** Mettre à jour une commande existante. Dans notre cas, elle sert exclusivement à **forcer la date de création historique (`date_add`)** issue des anciens fichiers CSV.
  * **Attention :** Nécessite de supprimer les nœuds XML `associations` et `shipping_number` de la réponse GET avant le PUT, sinon PrestaShop renvoie une erreur 500 (Bug natif de l'API PrestaShop).

---

## 3. Mode d'emploi des APIs Personnalisées (JSON)

Ces APIs ont été développées spécifiquement (via des modules comme `orderstateapi` et `stockdeltaapi`) pour simplifier et optimiser les flux de votre application.

### A. Gestion des Statuts et Mouvements (`POST /api/order_state_change`)
Le endpoint le plus critique de l'application. Changer l'état d'une commande nativement en XML est lourd et buggé. Cette API personnalisée gère tout en une seule requête JSON.

* **Fichier Frontend :** `OrderService.js` (Ligne 375, méthode `changeOrderState`)
* **Fichier Backend :** `modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`

* **Paramètres JSON attendus :**
  ```json
  {
    "id_order": 123,
    "new_state": 5,
    "date_add": "2023-01-15 12:00:00" 
  }
  ```

* **Effets métiers (Très important) :**
  * Si `new_state` = 11 (Paiement accepté) : Change le statut de la commande et incrémente le stock "réservé" (`reserved_quantity++`).
  * Si `new_state` = 5 (Livré) : Change le statut, décrémente la quantité physique, et **insère une ligne de sortie de stock dans la table `ps_stock_mvt`**.
  * Si `date_add` est fourni, le mouvement de stock dans `ps_stock_mvt` adoptera cette date, ce qui permet au Dashboard d'afficher une évolution journalière fidèle au passé.

### B. Gestion directe des Stocks (`/api/stock_delta`)
L'API native `stock_availables` est très difficile à manier. `/api/stock_delta` a été créée pour manipuler les stocks directement.

* **Fichier Frontend :** `NewApp/src/services/StockService.js` (Lignes 112 et 156)
* **Fichier Backend :** `modules/stockdeltaapi/classes/WebserviceSpecificManagementStockDelta.php`

**1. Mode Initialisation (`POST` avec `set_to`)**
* **Usage :** Utilisé lors de l'import du Fichier 2 (Stocks initiaux).
* **Payload :** `{ "id_product": 1, "id_product_attribute": 0, "set_to": 150 }`
* **Effet :** Écrase la quantité actuelle pour la remplacer par 150. **Aucun mouvement n'est enregistré dans l'historique `ps_stock_mvt`**. 

**2. Mode Mouvement (`POST` avec `delta`)**
* **Usage :** Pour ajouter ou retirer du stock manuellement.
* **Payload :** `{ "id_product": 1, "id_product_attribute": 0, "delta": -5 }`
* **Effet :** Soustrait 5 au stock, et **insère une ligne dans l'historique `ps_stock_mvt`** datée d'aujourd'hui.

**3. Mode Lecture d'historique (`GET`)**
* **Usage :** Alimente le graphique "Evolution journalière du stock" dans votre Dashboard.
* **Effet :** Exécute une requête SQL directement sur la table `ps_stock_mvt`, regroupe les entrées/sorties par jour (`GROUP BY DATE(date_add)`) et renvoie un JSON propre et prêt à être affiché par Vue.js.

---

## 4. Résumé : Comment importer correctement ?

Si vous devez développer un nouveau script d'import, voici la checklist obligatoire :
1. Lire le CSV et parser les données.
2. Appeler `POST /api/carts` (XML).
3. Appeler `POST /api/orders` (XML) en utilisant l'ID du panier.
4. (Optionnel) Faire un `GET` puis un `PUT /api/orders/{id}` pour écraser la `date_add`.
5. Appeler **absolument** `POST /api/order_state_change` avec le statut final (ex: 11 puis 5) pour que PrestaShop génère les bons mouvements comptables dans l'historique des stocks. Sans cette étape, votre stock physique ne bougera pas.
