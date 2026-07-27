# 🧾 Guide complet : API `/api/order_slip` (Avoirs)

> Documentation complète de l'endpoint webservice PrestaShop **`/api/order_slip`** : ce que c'est, comment l'utiliser depuis le projet (axios + XML), avec **chaque exemple de code annoté** par son fichier et ses lignes de référence.

> **Mots-clés** : order_slip, avoir, credit slip, refund, remboursement, OrderSlip, webservice, /api/order_slip, order_slip_details

---

## 📑 Index

- [1. Qu'est-ce qu'un `order_slip` ?](#1-quest-ce-quun-order_slip-)
- [2. Schéma SQL (table `ps_order_slip`)](#2-schéma-sql-table-ps_order_slip)
- [3. Champs exposés par le webservice](#3-champs-exposés-par-le-webservice)
- [4. Association `order_slip_details` (virtual entity)](#4-association-order_slip_details-virtual-entity)
- [5. Authentification et configuration axios](#5-authentification-et-configuration-axios)
- [6. GET — Lister tous les avoirs](#6-get--lister-tous-les-avoirs)
- [7. GET — Récupérer un avoir par ID](#7-get--récupérer-un-avoir-par-id)
- [8. GET — Filtrer par `id_order` ou `id_customer`](#8-get--filtrer-par-id_order-ou-id_customer)
- [9. GET — Schéma blank (synopsis avant POST)](#9-get--schéma-blank-synopsis-avant-post)
- [10. POST — Créer un avoir](#10-post--créer-un-avoir)
- [11. PUT — Modifier un avoir](#11-put--modifier-un-avoir)
- [12. DELETE — Supprimer un avoir](#12-delete--supprimer-un-avoir)
- [13. Service réutilisable `OrderSlipService`](#13-service-réutilisable-orderslipservice)
- [14. Intégration côté Vue (composant exemple)](#14-intégration-côté-vue-composant-exemple)
- [15. Règles métier et pièges connus](#15-règles-métier-et-pièges-connus)
- [16. Cas d'erreur et codes HTTP](#16-cas-derreur-et-codes-http)
- [17. Récap des fichiers concernés](#17-récap-des-fichiers-concernés)

---

## 1. Qu'est-ce qu'un `order_slip` ?

**Mots-clés** : définition, avoir, remboursement, refund

Un **order_slip** (français : **avoir** / **bon de réduction de remboursement**) est un document généré quand un client se fait **rembourser tout ou partie d'une commande**.

C'est l'équivalent comptable d'une **facture négative** : il référence la commande d'origine (`id_order`) et liste les produits + montants à rembourser.

Types possibles (`order_slip_type`) :
- `0` = avoir **standard** (remboursement classique)
- `1` = avoir **avec montant** (remboursement par montant arbitraire)
- `2` = avoir **partiel** (montant choisi OU frais de port)

Référence côté code PrestaShop : [classes/order/OrderSlip.php:26-95](../classes/order/OrderSlip.php#L26-L95) — déclaration de la classe `OrderSlipCore` et de sa `$definition`.

---

## 2. Schéma SQL (table `ps_order_slip`)

**Mots-clés** : SQL, colonnes, schéma BDD

La définition exacte est dans [classes/order/OrderSlip.php:76-95](../classes/order/OrderSlip.php#L76-L95) :

```php
public static $definition = [
    'table' => 'order_slip',
    'primary' => 'id_order_slip',
    'fields' => [
        'id_customer'              => ['type' => self::TYPE_INT,   'required' => true],
        'id_order'                 => ['type' => self::TYPE_INT,   'required' => true],
        'conversion_rate'          => ['type' => self::TYPE_FLOAT, 'required' => true],
        'total_products_tax_excl'  => ['type' => self::TYPE_FLOAT, 'required' => true],
        'total_products_tax_incl'  => ['type' => self::TYPE_FLOAT, 'required' => true],
        'total_shipping_tax_excl'  => ['type' => self::TYPE_FLOAT, 'required' => true],
        'total_shipping_tax_incl'  => ['type' => self::TYPE_FLOAT, 'required' => true],
        'amount'                   => ['type' => self::TYPE_FLOAT],
        'shipping_cost'            => ['type' => self::TYPE_BOOL],
        'shipping_cost_amount'     => ['type' => self::TYPE_FLOAT],
        'partial'                  => ['type' => self::TYPE_INT],
        'date_add'                 => ['type' => self::TYPE_DATE],
        'date_upd'                 => ['type' => self::TYPE_DATE],
        'order_slip_type'          => ['type' => self::TYPE_INT],
    ],
];
```

**Note** : la clé primaire est `id_order_slip` (et non `id`) — gardez-le en tête quand vous parsez la réponse XML.

Une seconde table liée, `ps_order_slip_detail`, contient les **lignes produits** de l'avoir (un avoir peut couvrir plusieurs produits) — détaillée plus bas.

---

## 3. Champs exposés par le webservice

**Mots-clés** : webservice parameters, XML, xlink

Tout ce que vous pouvez lire/écrire via `/api/order_slip` est défini par `protected $webserviceParameters` dans [classes/order/OrderSlip.php:97-114](../classes/order/OrderSlip.php#L97-L114) :

```php
protected $webserviceParameters = [
    'objectNodeName'  => 'order_slip',
    'objectsNodeName' => 'order_slips',
    'fields' => [
        'id_customer' => ['xlink_resource' => 'customers'],
        'id_order'    => ['xlink_resource' => 'orders'],
    ],
    'associations' => [
        'order_slip_details' => [
            'resource'       => 'order_slip_detail',
            'setter'         => false,
            'virtual_entity' => true,
            'fields' => [
                'id'               => [],
                'id_order_detail'  => ['required' => true],
                'product_quantity' => ['required' => true],
                'amount_tax_excl'  => ['required' => true],
                'amount_tax_incl'  => ['required' => true],
            ],
        ],
    ],
];
```

**Conséquences pratiques :**
- Le nœud XML racine d'une ressource = `<order_slip>` ; la collection = `<order_slips>`.
- `id_customer` et `id_order` portent un attribut `xlink:href` pointant vers la ressource liée (utile pour suivre la relation depuis le XML).
- Les autres champs déclarés dans `$definition` sont automatiquement disponibles en lecture/écriture si compatibles.

---

## 4. Association `order_slip_details` (virtual entity)

**Mots-clés** : virtual_entity, lignes produit, getter, setter

C'est une **entité virtuelle** : elle n'a pas d'endpoint propre (pas de `/api/order_slip_details`), elle s'expose **uniquement à travers `order_slip`**.

Le getter est défini dans [classes/order/OrderSlip.php:530-538](../classes/order/OrderSlip.php#L530-L538) :

```php
public function getWsOrderSlipDetails()
{
    $query = 'SELECT id_order_slip as id, id_order_detail, product_quantity, amount_tax_excl, amount_tax_incl
    FROM `' . _DB_PREFIX_ . 'order_slip_detail`
    WHERE id_order_slip = ' . (int) $this->id;
    return Db::getInstance()->executeS($query);
}
```

> ⚠️ **`setter => false`** dans la config webservice (ligne 105) : vous **ne pouvez pas créer/modifier les lignes d'un avoir via le webservice**. Le webservice expose les détails en **lecture uniquement**.
>
> Pour créer un avoir avec ses lignes, il faut soit :
> 1. Passer par le BackOffice PrestaShop (interface admin "Avoirs"),
> 2. Utiliser un helper PHP custom qui appelle `OrderSlip::create($order, $product_list)` ([classes/order/OrderSlip.php:264-402](../classes/order/OrderSlip.php#L264-L402)),
> 3. Insérer directement en SQL dans `ps_order_slip` + `ps_order_slip_detail` (technique mais possible).

---

## 5. Authentification et configuration axios

**Mots-clés** : auth, API key, axios, .env

Le projet utilise une **clé API PrestaShop** en Basic Auth (username = clé, password = vide). Tout passe par l'instance axios configurée dans [NewApp/src/config/axios.js:1-21](../NewApp/src/config/axios.js#L1-L21) :

```js
import axios from 'axios'
import { API_CONFIG } from './api'

axios.defaults.auth = {
  username: API_CONFIG.apiKey,
  password: '',
}

axios.interceptors.request.use((config) => {
  if (!config.auth) {
    config.auth = { username: API_CONFIG.apiKey, password: '' }
  }
  return config
}, (error) => Promise.reject(error))

export default axios
```

La clé API et l'URL de base sont lues depuis le `.env` via [NewApp/src/config/api.js:1-12](../NewApp/src/config/api.js#L1-L12) :

```js
export const API_CONFIG = {
  prestashopUrl: import.meta.env.VITE_PRESTASHOP_URL,
  apiKey:        import.meta.env.VITE_API_KEY,
  phpHelpersUrl: import.meta.env.VITE_PHP_HELPERS_URL,
}
```

> Pré-requis BackOffice : la clé API doit avoir les **droits `GET / POST / PUT / DELETE` sur la ressource `order_slip`** dans `Paramètres avancés > Webservice`.

---

## 6. GET — Lister tous les avoirs

**Mots-clés** : GET, liste, collection, display=full

**Endpoint** : `GET /api/order_slips?display=full`

```js
// Inspiré du pattern utilisé dans NewApp/src/services/StockService.js:11-48
import axios from "../config/axios";
import { XMLParser } from "fast-xml-parser";

async function listOrderSlips() {
  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
  });

  const res = await axios.get("/api/order_slips?display=full");
  const json = parser.parse(res.data);

  let slips = json.prestashop.order_slips?.order_slip || [];
  if (!Array.isArray(slips)) slips = [slips]; // ⚠️ XML : 1 résultat = objet, pas tableau

  return slips.map(s => ({
    id:                       s.id,
    id_order:                 s.id_order?.["#text"] || s.id_order,
    id_customer:              s.id_customer?.["#text"] || s.id_customer,
    amount:                   parseFloat(s.amount || 0),
    total_products_tax_incl:  parseFloat(s.total_products_tax_incl || 0),
    total_shipping_tax_incl:  parseFloat(s.total_shipping_tax_incl || 0),
    partial:                  parseInt(s.partial || 0),
    order_slip_type:          parseInt(s.order_slip_type || 0),
    date_add:                 s.date_add,
  }));
}
```

**Pourquoi `display=full`** : sans ce paramètre, PrestaShop renvoie uniquement `id` + `<xlink:href>`. Avec `display=full` on récupère tous les champs.

**Pourquoi `if (!Array.isArray(stocks)) stocks = [stocks]`** : pattern présent dans [NewApp/src/services/StockService.js:27-29](../NewApp/src/services/StockService.js#L27-L29). Le parser XML retourne **un objet** si une seule entrée, **un tableau** si plusieurs.

---

## 7. GET — Récupérer un avoir par ID

**Mots-clés** : GET by id, détail, single resource

**Endpoint** : `GET /api/order_slips/{id_order_slip}`

```js
async function getOrderSlip(id) {
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "@_" });
  const res    = await axios.get(`/api/order_slips/${id}`);
  const json   = parser.parse(res.data);
  const slip   = json.prestashop.order_slip;

  // Les lignes produits (lecture seule)
  let details = slip.associations?.order_slip_details?.order_slip_detail || [];
  if (!Array.isArray(details)) details = [details];

  return {
    id:           slip.id,
    id_order:     slip.id_order?.["#text"] || slip.id_order,
    id_customer:  slip.id_customer?.["#text"] || slip.id_customer,
    amount:       parseFloat(slip.amount || 0),
    date_add:     slip.date_add,
    details: details.map(d => ({
      id_order_detail:  d.id_order_detail,
      product_quantity: parseInt(d.product_quantity || 0),
      amount_tax_excl:  parseFloat(d.amount_tax_excl || 0),
      amount_tax_incl:  parseFloat(d.amount_tax_incl || 0),
    })),
  };
}
```

Le pattern de parsing des associations imbriquées est calqué sur [NewApp/src/services/StockService.js:200-230](../NewApp/src/services/StockService.js#L200-L230) (lecture des `product_option_values` d'une combinaison).

---

## 8. GET — Filtrer par `id_order` ou `id_customer`

**Mots-clés** : filter, id_order, id_customer, query string

PrestaShop supporte le pattern `filter[<champ>]=<valeur>` (documenté en détail dans [docs/filter.md](filter.md)). Exemple pour obtenir tous les avoirs d'**une commande** :

```js
async function getSlipsByOrder(idOrder) {
  const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "@_" });
  const res    = await axios.get(`/api/order_slips?filter[id_order]=${idOrder}&display=full`);
  const json   = parser.parse(res.data);

  let slips = json.prestashop.order_slips?.order_slip || [];
  if (!Array.isArray(slips)) slips = [slips];
  return slips;
}

// Tous les avoirs d'un client
async function getSlipsByCustomer(idCustomer) {
  const res = await axios.get(
    `/api/order_slips?filter[id_customer]=${idCustomer}&display=full`
  );
  // ... même parsing
}
```

> ⚠️ Le helper PHP `OrderSlip::getOrdersSlip()` ([classes/order/OrderSlip.php:135-143](../classes/order/OrderSlip.php#L135-L143)) fait la même chose côté serveur — utile si vous écrivez un helper PHP custom plutôt que d'appeler le webservice.

---

## 9. GET — Schéma blank (synopsis avant POST)

**Mots-clés** : ?schema=blank, template, synopsis

Avant tout `POST`, PrestaShop attend un XML complet (tous les champs `required` doivent être présents). On récupère le squelette XML attendu :

```js
async function fetchOrderSlipBlank() {
  const res = await axios.get('/api/order_slips?schema=blank');
  return res.data; // XML à compléter
}
```

Le XML retourné ressemble à :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<prestashop xmlns:xlink="http://www.w3.org/1999/xlink">
  <order_slip>
    <id></id>
    <id_customer xlink:href="..."></id_customer>
    <id_order    xlink:href="..."></id_order>
    <conversion_rate></conversion_rate>
    <total_products_tax_excl></total_products_tax_excl>
    <total_products_tax_incl></total_products_tax_incl>
    <total_shipping_tax_excl></total_shipping_tax_excl>
    <total_shipping_tax_incl></total_shipping_tax_incl>
    <amount></amount>
    <shipping_cost></shipping_cost>
    <shipping_cost_amount></shipping_cost_amount>
    <partial></partial>
    <date_add></date_add>
    <date_upd></date_upd>
    <order_slip_type></order_slip_type>
  </order_slip>
</prestashop>
```

Voir [docs/PutPost.md](PutPost.md) pour le pattern général de `schema=blank` → modifier → POST.

---

## 10. POST — Créer un avoir

**Mots-clés** : POST, création, XMLBuilder, Content-Type application/xml

**Endpoint** : `POST /api/order_slips`

```js
import { XMLBuilder } from "fast-xml-parser";

async function createOrderSlip({ id_order, id_customer, amount, shipping_cost = false }) {
  const builder = new XMLBuilder({
    ignoreAttributes: false,
    attributeNamePrefix: "@_",
    format: true,
  });

  const xml = builder.build({
    prestashop: {
      order_slip: {
        id_customer,
        id_order,
        conversion_rate:         1,
        total_products_tax_excl: amount,
        total_products_tax_incl: amount,
        total_shipping_tax_excl: 0,
        total_shipping_tax_incl: 0,
        amount,
        shipping_cost:           shipping_cost ? 1 : 0,
        shipping_cost_amount:    0,
        partial:                 0,
        order_slip_type:         0,
      },
    },
  });

  const res = await axios.post('/api/order_slips', xml, {
    headers: { 'Content-Type': 'application/xml' },
  });
  return res.data; // XML de l'avoir créé (contient id_order_slip)
}
```

**Pattern XMLBuilder identique** à [NewApp/src/services/StockService.js:72-91](../NewApp/src/services/StockService.js#L72-L91) (méthode `updateStock`) et à [NewApp/src/services/orderService.js:386-411](../NewApp/src/services/orderService.js#L386-L411) (`updateOrderState`).

> ⚠️ **Limite webservice** : on **ne peut pas** envoyer les `order_slip_details` dans ce POST (à cause de `setter => false` à [classes/order/OrderSlip.php:105](../classes/order/OrderSlip.php#L105)). L'avoir est créé **sans lignes produit** — il sera vide tant que vous n'insérez pas les détails autrement.
>
> Pour un avoir **complet avec lignes produit**, utilisez plutôt la méthode PHP `OrderSlip::create()` ([classes/order/OrderSlip.php:264-402](../classes/order/OrderSlip.php#L264-L402)) via un helper PHP custom — voir section 13.

---

## 11. PUT — Modifier un avoir

**Mots-clés** : PUT, update, modification

**Endpoint** : `PUT /api/order_slips/{id}`

Pattern : on **GET** d'abord pour récupérer la ressource complète, on modifie le champ, on **PUT** :

```js
async function updateOrderSlipAmount(id, newAmount) {
  const parser  = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "@_" });
  const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: "@_", format: true });

  // 1. GET
  const getRes = await axios.get(`/api/order_slips/${id}`);
  const json   = parser.parse(getRes.data);
  const slip   = json.prestashop.order_slip;

  // 2. Modifier
  slip.amount = newAmount;
  slip.total_products_tax_incl = newAmount;

  // 3. Re-builder en XML
  const xml = builder.build({ prestashop: { order_slip: slip } });

  // 4. PUT
  await axios.put(`/api/order_slips/${id}`, xml, {
    headers: { 'Content-Type': 'application/xml' },
  });
}
```

Pattern **identique à `StockService.updateStock`** ([NewApp/src/services/StockService.js:56-99](../NewApp/src/services/StockService.js#L56-L99)) — c'est la convention dans tout le projet pour le PUT XML.

---

## 12. DELETE — Supprimer un avoir

**Mots-clés** : DELETE, suppression

**Endpoint** : `DELETE /api/order_slips/{id}`

```js
async function deleteOrderSlip(id) {
  await axios.delete(`/api/order_slips/${id}`);
  return true;
}
```

> ⚠️ **Effets de bord** : supprimer un avoir ne **rembourse pas automatiquement** le client (le webservice ne touche pas au paiement) et **ne ré-incrémente pas le stock**. C'est purement une suppression de l'enregistrement comptable. Réservez ce verbe à la correction d'erreur de saisie.

---

## 13. Service réutilisable `OrderSlipService`

**Mots-clés** : service, factorisation, code réutilisable

Voici un service complet à placer dans `NewApp/src/services/orderSlipService.js`, calqué sur la structure de [NewApp/src/services/StockService.js](../NewApp/src/services/StockService.js) :

```js
import axios from "../config/axios";
import { XMLParser, XMLBuilder } from "fast-xml-parser";

const parser  = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: "@_" });
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: "@_", format: true });

function unwrap(node) {
  // PrestaShop encode parfois les valeurs scalaires en { "#text": "..." } à cause des xlink
  if (node && typeof node === "object" && "#text" in node) return node["#text"];
  return node;
}

export const OrderSlipService = {
  async list() {
    const res  = await axios.get("/api/order_slips?display=full");
    const json = parser.parse(res.data);
    let slips  = json.prestashop.order_slips?.order_slip || [];
    if (!Array.isArray(slips)) slips = [slips];
    return slips.map(s => ({
      id:          s.id,
      id_order:    unwrap(s.id_order),
      id_customer: unwrap(s.id_customer),
      amount:      parseFloat(s.amount || 0),
      partial:     parseInt(s.partial || 0),
      date_add:    s.date_add,
    }));
  },

  async getById(id) {
    const res  = await axios.get(`/api/order_slips/${id}`);
    const json = parser.parse(res.data);
    const slip = json.prestashop.order_slip;

    let details = slip.associations?.order_slip_details?.order_slip_detail || [];
    if (!Array.isArray(details)) details = [details];

    return {
      id:          slip.id,
      id_order:    unwrap(slip.id_order),
      id_customer: unwrap(slip.id_customer),
      amount:      parseFloat(slip.amount || 0),
      partial:     parseInt(slip.partial || 0),
      date_add:    slip.date_add,
      details: details.map(d => ({
        id_order_detail:  d.id_order_detail,
        product_quantity: parseInt(d.product_quantity || 0),
        amount_tax_excl:  parseFloat(d.amount_tax_excl || 0),
        amount_tax_incl:  parseFloat(d.amount_tax_incl || 0),
      })),
    };
  },

  async getByOrder(idOrder) {
    const res  = await axios.get(`/api/order_slips?filter[id_order]=${idOrder}&display=full`);
    const json = parser.parse(res.data);
    let slips  = json.prestashop.order_slips?.order_slip || [];
    if (!Array.isArray(slips)) slips = [slips];
    return slips;
  },

  async create({ id_order, id_customer, amount, shipping_cost = false, partial = 0, order_slip_type = 0 }) {
    const xml = builder.build({
      prestashop: {
        order_slip: {
          id_customer,
          id_order,
          conversion_rate:         1,
          total_products_tax_excl: amount,
          total_products_tax_incl: amount,
          total_shipping_tax_excl: 0,
          total_shipping_tax_incl: 0,
          amount,
          shipping_cost:           shipping_cost ? 1 : 0,
          shipping_cost_amount:    0,
          partial,
          order_slip_type,
        },
      },
    });
    const res  = await axios.post("/api/order_slips", xml, {
      headers: { "Content-Type": "application/xml" },
    });
    const json = parser.parse(res.data);
    return { id: json.prestashop.order_slip.id, raw: res.data };
  },

  async update(id, patch) {
    const getRes = await axios.get(`/api/order_slips/${id}`);
    const json   = parser.parse(getRes.data);
    const slip   = json.prestashop.order_slip;
    Object.assign(slip, patch);
    const xml = builder.build({ prestashop: { order_slip: slip } });
    await axios.put(`/api/order_slips/${id}`, xml, {
      headers: { "Content-Type": "application/xml" },
    });
    return true;
  },

  async remove(id) {
    await axios.delete(`/api/order_slips/${id}`);
    return true;
  },
};
```

L'export `OrderSlipService` suit la convention exacte des autres services du projet (`StockService`, `OrderService`, `ImportService`).

---

## 14. Intégration côté Vue (composant exemple)

**Mots-clés** : Vue 3, composition API, script setup

Exemple minimal pour afficher les avoirs d'une commande dans un composant ; calqué sur [NewApp/src/components/OrderList.vue](../NewApp/src/components/OrderList.vue) :

```vue
<script setup>
import { ref, onMounted } from "vue";
import { OrderSlipService } from "@/services/orderSlipService";

const props = defineProps({ orderId: { type: Number, required: true } });
const slips = ref([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    slips.value = await OrderSlipService.getByOrder(props.orderId);
  } finally {
    loading.value = false;
  }
}

async function cancelSlip(id) {
  if (!confirm("Supprimer cet avoir ?")) return;
  await OrderSlipService.remove(id);
  await load();
}

onMounted(load);
</script>

<template>
  <div v-if="loading">Chargement…</div>
  <table v-else>
    <thead>
      <tr><th>ID</th><th>Montant</th><th>Date</th><th>Action</th></tr>
    </thead>
    <tbody>
      <tr v-for="s in slips" :key="s.id">
        <td>{{ s.id }}</td>
        <td>{{ s.amount }} €</td>
        <td>{{ s.date_add }}</td>
        <td><button @click="cancelSlip(s.id)">🗑️</button></td>
      </tr>
    </tbody>
  </table>
</template>
```

> Pour la **création** d'un avoir depuis le front avec ses lignes produit, **passer par un endpoint PHP custom** (voir section 15) — l'API PrestaShop seule ne le permet pas.

---

## 15. Règles métier et pièges connus

**Mots-clés** : pièges, business rules, limitations

| # | Règle / piège | Raison |
|---|---------------|--------|
| 1 | `order_slip_details` est en **lecture seule** via webservice | `setter => false` dans [classes/order/OrderSlip.php:105](../classes/order/OrderSlip.php#L105) |
| 2 | Créer un avoir via `POST /api/order_slips` génère un avoir **sans lignes produit** | Idem — pas de setter pour l'association |
| 3 | Le webservice **ne déclenche pas** le remboursement réel (PayPal / Stripe / etc.) | Le webservice manipule seulement la table BDD, pas les passerelles de paiement |
| 4 | Le webservice **ne décrémente pas** `product_quantity_refunded` côté `order_detail` | Cette colonne n'est mise à jour que par `OrderSlip::create()` PHP — voir [classes/order/OrderSlip.php:319-323](../classes/order/OrderSlip.php#L319-L323) |
| 5 | Le webservice **ne réincrémente pas le stock** | Le retour en stock est géré séparément par la logique d'annulation/retour — voir [docs/Annulation.md](Annulation.md) |
| 6 | `conversion_rate` est **requis** (pas de défaut) | Sinon HTTP 400 "missing field" |
| 7 | `id_customer` doit correspondre à `id_customer` de l'`Order` lié | Sinon incohérence comptable côté BackOffice (l'avoir n'apparaît pas dans le compte du bon client) |

**Pour un workflow complet de remboursement** (créer l'avoir **avec** ses lignes + rembourser le paiement + ré-incrémenter le stock), créez un helper PHP custom basé sur `OrderSlip::create()` ([classes/order/OrderSlip.php:264-402](../classes/order/OrderSlip.php#L264-L402)) — c'est la méthode utilisée par le BackOffice natif (`AdminOrdersController::doPartialRefund`).

Squelette du helper PHP :

```php
// helpers/create_order_slip.php (à placer dans la racine PrestaShop)
require dirname(__FILE__) . '/config/config.inc.php';

$order = new Order((int)$_POST['id_order']);
$product_list = [
    [
        'id_order_detail'      => 42,
        'quantity'             => 1,
        'unit_price'           => 19.99,
        'unit_price_tax_incl'  => 23.99,
        'unit_price_tax_excl'  => 19.99,
        'total_price_tax_incl' => 23.99,
        'total_price_tax_excl' => 19.99,
    ],
];

$ok = OrderSlip::create($order, $product_list, false, 0, false, true);
echo json_encode(['success' => (bool)$ok]);
```

Cette voie utilise [classes/order/OrderSlip.php:264](../classes/order/OrderSlip.php#L264) avec toute sa logique métier (calcul de taxes, addProductOrderSlip, mise à jour de `product_quantity_refunded`, etc.).

---

## 16. Cas d'erreur et codes HTTP

**Mots-clés** : erreurs, HTTP, 400, 401, 404

| Code HTTP | Cause typique | Comment résoudre |
|-----------|---------------|------------------|
| `401 Unauthorized` | Clé API absente ou invalide | Vérifier `VITE_API_KEY` dans `.env` et les droits `order_slip` dans BO > Paramètres avancés > Webservice |
| `400 Bad Request` — *"Field X is required"* | Champ obligatoire manquant dans le POST | Compléter le XML avec `conversion_rate`, `id_customer`, `id_order`, et les `total_*` (voir section 3) |
| `400 Bad Request` — *"Invalid format"* | Champ `partial` ou `shipping_cost` reçu comme string `"true"`/`"false"` | Envoyer `0` / `1` (entiers), pas des booléens stringifiés |
| `404 Not Found` sur `GET /{id}` | `id_order_slip` inexistant | Vérifier l'ID via `GET /api/order_slips?display=[id]` |
| `405 Method Not Allowed` | Verbe HTTP désactivé dans BO > Webservice | Activer GET/POST/PUT/DELETE pour la ressource `order_slip` |
| Réponse XML **vide** `<prestashop/>` | Hook d'un module qui plante silencieusement | Symptôme déjà rencontré sur `/api/orders` — voir le fallback GET dans [NewApp/src/services/orderService.js:473-590](../NewApp/src/services/orderService.js#L473-L590) ; même pattern applicable ici |

---

## 17. Récap des fichiers concernés

**Mots-clés** : récap, références

### Côté PrestaShop core
| Fichier | Rôle |
|---------|------|
| [classes/order/OrderSlip.php:26-95](../classes/order/OrderSlip.php#L26-L95) | Définition de la classe et schéma SQL |
| [classes/order/OrderSlip.php:97-114](../classes/order/OrderSlip.php#L97-L114) | Configuration webservice (champs exposés + association) |
| [classes/order/OrderSlip.php:116-133](../classes/order/OrderSlip.php#L116-L133) | `addSlipDetail()` — insertion des lignes |
| [classes/order/OrderSlip.php:135-143](../classes/order/OrderSlip.php#L135-L143) | `getOrdersSlip()` — liste par client/commande |
| [classes/order/OrderSlip.php:264-402](../classes/order/OrderSlip.php#L264-L402) | `OrderSlip::create()` — création complète avec lignes (voie PHP) |
| [classes/order/OrderSlip.php:419-437](../classes/order/OrderSlip.php#L419-L437) | `createPartialOrderSlip()` — avoir partiel |
| [classes/order/OrderSlip.php:530-538](../classes/order/OrderSlip.php#L530-L538) | `getWsOrderSlipDetails()` — getter webservice |
| [classes/order/Order.php:2098-2104](../classes/order/Order.php#L2098-L2104) | `Order::getOrderSlipsCollection()` — avoirs d'une commande |
| [classes/webservice/WebserviceRequest.php:316](../classes/webservice/WebserviceRequest.php#L316) | Inscription de la ressource `order_slip` dans le webservice |

### Côté NewApp (patterns à imiter pour le nouveau service)
| Fichier | Pattern à réutiliser |
|---------|----------------------|
| [NewApp/src/config/axios.js:1-21](../NewApp/src/config/axios.js#L1-L21) | Auth Basic auto-injectée |
| [NewApp/src/config/api.js:1-12](../NewApp/src/config/api.js#L1-L12) | URLs depuis `.env` |
| [NewApp/src/services/StockService.js:11-48](../NewApp/src/services/StockService.js#L11-L48) | GET + XMLParser + normalisation array |
| [NewApp/src/services/StockService.js:56-99](../NewApp/src/services/StockService.js#L56-L99) | PUT via GET → mutate → re-build XML |
| [NewApp/src/services/orderService.js:386-411](../NewApp/src/services/orderService.js#L386-L411) | POST XML pour créer un `order_history` |
| [NewApp/src/services/orderService.js:473-590](../NewApp/src/services/orderService.js#L473-L590) | Fallback GET si réponse vide |
| [NewApp/src/components/OrderList.vue](../NewApp/src/components/OrderList.vue) | Pattern d'affichage / refresh / actions |

### Documentation liée
| Fichier | Sujet |
|---------|-------|
| [docs/PutPost.md](PutPost.md) | Pattern général PUT / POST en XML |
| [docs/filter.md](filter.md) | Syntaxe `filter[champ]=valeur` |
| [docs/Service.md](Service.md) | Convention des services NewApp |
| [docs/Annulation.md](Annulation.md) | Workflow d'annulation (complémentaire au remboursement) |
| [docs/commande.md](commande.md) | Cycle de vie de la commande (état 11 → 5 → 6) |

---

> 💡 **TL;DR** : `/api/order_slip` est une ressource webservice **standard PrestaShop**, en lecture/écriture pour l'enveloppe mais en **lecture seule pour les lignes produit** (`order_slip_details`). Pour un avoir métier complet (avec lignes + remboursement), créez un helper PHP qui appelle `OrderSlip::create()`. Pour de la simple lecture (lister, afficher, supprimer), le webservice suffit avec les patterns axios/XMLParser/XMLBuilder déjà en place dans le projet.
