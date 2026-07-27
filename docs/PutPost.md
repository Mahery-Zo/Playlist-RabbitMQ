# 📡 Guide complet : POST & PUT dans ce projet

> **Mots-clés généraux** : POST, PUT, axios, PrestaShop, webservice, XML, JSON, XMLBuilder, headers, Content-Type, application/xml, application/json, créer, mettre à jour, update, create, REST, CRUD, module custom, endpoint, secure_key, /api/orders, /api/carts, /api/stock_availables, order_state_change, stock_delta, stock_by_category, multipart

---

## 📑 Index

- [1. POST vs PUT — quand utiliser quoi](#1-post-vs-put--quand-utiliser-quoi)
- [2. Setup de base avec axios](#2-setup-de-base-avec-axios)
- [3. POST en XML vers PrestaShop (création)](#3-post-en-xml-vers-prestashop-création)
- [4. PUT en XML vers PrestaShop (mise à jour)](#4-put-en-xml-vers-prestashop-mise-à-jour)
- [5. POST en JSON vers endpoint custom](#5-post-en-json-vers-endpoint-custom)
- [6. Lire et extraire la réponse XML](#6-lire-et-extraire-la-réponse-xml)
- [7. Gestion d'erreur (PrestaShop + axios)](#7-gestion-derreur-prestashop--axios)
- [8. Upload de fichier (multipart/form-data)](#8-upload-de-fichier-multipartform-data)
- [9. Cas d'usage POST dans ce projet](#9-cas-dusage-post-dans-ce-projet)
- [10. Cas d'usage PUT dans ce projet](#10-cas-dusage-put-dans-ce-projet)
- [11. Côté Service — pattern complet](#11-côté-service--pattern-complet)
- [12. Côté View — appel depuis une vue](#12-côté-view--appel-depuis-une-vue)
- [13. Côté module PHP — recevoir un POST custom](#13-côté-module-php--recevoir-un-post-custom)
- [14. Pièges et limitations PrestaShop](#14-pièges-et-limitations-prestashop)
- [15. Anti-patterns à éviter](#15-anti-patterns-à-éviter)
- [16. Templates copy-paste](#16-templates-copy-paste)
- [17. Cheatsheet](#17-cheatsheet)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. POST vs PUT — quand utiliser quoi

**Mots-clés** : POST, PUT, différence, création, update, sémantique, REST

### Définitions REST

| | **POST** | **PUT** |
|---|---|---|
| Rôle | **Créer** une nouvelle ressource | **Mettre à jour** une ressource existante |
| URL | `/api/products` (collection) | `/api/products/42` (ressource précise) |
| Idempotence | ❌ Non (chaque POST crée un nouveau) | ✅ Oui (même PUT 2x = même résultat) |
| Code retour typique | 201 Created | 200 OK |

### En pratique dans ce projet

- **POST** :
  - Créer un cart, un order, un customer, une address, un produit
  - Appeler un endpoint custom qui exécute une action (`/api/stock_delta`, `/api/order_state_change`)
- **PUT** :
  - Modifier la quantité d'un stock
  - Modifier les infos d'un produit
  - Modifier un cart existant

⚠️ **Particularité PrestaShop** : les endpoints custom du projet (`/api/stock_delta`, `/api/order_state_change`) acceptent **POST** même pour des actions de mise à jour. C'est intentionnel pour faciliter l'envoi de body JSON.

---

## 2. Setup de base avec axios

**Mots-clés** : axios, config, headers, auth, baseURL

L'axios du projet est pré-configuré dans [src/config/axios.js](../NewApp/src/config/axios.js) :

```js
import axios from 'axios'
const API_KEY = import.meta.env.VITE_API_KEY

// L'auth est injectée automatiquement à chaque requête
axios.defaults.auth = { username: API_KEY, password: '' }

axios.interceptors.request.use((config) => {
  if (!config.auth) config.auth = { username: API_KEY, password: '' }
  return config
})

export default axios
```

→ Dans les services, on l'importe et l'utilise directement : **pas besoin de re-passer l'auth** à chaque appel.

```js
import axios from '../config/axios'
```

---

## 3. POST en XML vers PrestaShop (création)

**Mots-clés** : POST, création, XML, application/xml, XMLBuilder

PrestaShop attend du **XML** dans le body. On le construit avec `fast-xml-parser`.

### Structure générale

```js
import { XMLBuilder } from 'fast-xml-parser'

const builder = new XMLBuilder({
  ignoreAttributes: false,
  attributeNamePrefix: '@_',
  format: true
})

const xmlObj = {
  prestashop: {
    [resource]: {
      // ...champs de la ressource
    }
  }
}

const xmlContent = builder.build(xmlObj)

const response = await axios.post('/api/<resource>', xmlContent, {
  headers: { 'Content-Type': 'application/xml' }
})
```

### Exemple — création d'un cart ([CartService.js](../NewApp/src/services/CartService.js))

```js
async createCart(cartData) {
  const xml = this.buildCartXML(cartData)
  console.log("📤 Envoi du panier à PrestaShop:", xml)

  const response = await axios.post('/api/carts', xml, {
    headers: { 'Content-Type': 'application/xml' }
  })

  const parser = new XMLParser()
  const result = parser.parse(response.data)
  const cartId = result.prestashop?.cart?.id

  return { success: true, cart_id: cartId, data: result.prestashop?.cart }
}
```

### Exemple — création d'un order ([orderService.js](../NewApp/src/services/orderService.js))

```js
const xmlObj = {
  prestashop: {
    order: {
      id_address_delivery: orderData.id_address_delivery,
      id_address_invoice: orderData.id_address_invoice,
      id_cart: orderData.id_cart,
      id_currency: orderData.id_currency || 1,
      id_lang: orderData.id_lang || 1,
      id_customer: orderData.id_customer,
      id_carrier: orderData.id_carrier || 1,
      current_state: 11,
      module: orderData.module || 'ps_cashondelivery',
      payment: orderData.payment || 'Paiement à la livraison',
      secure_key: secureKey,    // 32 chars hex
      valid: 1,
      total_paid: totalPaid.toFixed(6),
      total_paid_real: totalPaid.toFixed(6),
      total_paid_tax_incl: totalPaid.toFixed(6),
      total_paid_tax_excl: totalProducts.toFixed(6),
      total_products: totalProducts.toFixed(6),
      total_products_wt: totalProductsWt.toFixed(6),
      total_shipping: totalShipping.toFixed(6),
      conversion_rate: '1.000000',
      // ... + autres champs requis
    }
  }
}

const xmlContent = builder.build(xmlObj)
const response = await axios.post('/api/orders', xmlContent, {
  headers: { 'Content-Type': 'application/xml' }
})
```

⚠️ **Champs obligatoires** pour `/api/orders` : `id_cart`, `id_customer`, `id_address_*`, `secure_key`, `module`, `payment`, et les totaux complets (`total_paid`, `total_paid_real`, `total_paid_tax_incl/excl`, `total_products`, etc. — voir l'XML complet dans orderService.js).

---

## 4. PUT en XML vers PrestaShop (mise à jour)

**Mots-clés** : PUT, update, mise à jour, /api/X/id

L'URL inclut l'ID de la ressource :

```js
await axios.put(`/api/<resource>/${id}`, xmlContent, {
  headers: { 'Content-Type': 'application/xml' }
})
```

### Exemple — mise à jour d'un stock ([importService.js](../NewApp/src/services/importService.js))

⚠️ Envoyer **un XML minimal** (PrestaShop refuse si tu renvoies les `xlink:href` lus en GET) :

```js
async function setStock(idProduct, idCombination, quantity) {
  const stocks = await getList('stock_availables', 'stock_available',
    `&filter[id_product]=${idProduct}&filter[id_product_attribute]=${idCombination}`
  )
  if (stocks.length === 0) return

  const stockId = txt(stocks[0].id)

  const updateXml = builder.build({
    prestashop: {
      stock_available: {
        id: stockId,
        id_product: idProduct,
        id_product_attribute: idCombination,
        id_shop: 1,
        id_shop_group: 0,
        quantity,
        depends_on_stock: 0,
        out_of_stock: 2,
        location: '',
      },
    },
  })

  await axios.put(`/api/stock_availables/${stockId}`, updateXml, {
    headers: { 'Content-Type': 'application/xml' }
  })
}
```

### Pattern get-then-update

```js
// 1. GET pour récupérer l'objet
const getRes = await axios.get(`/api/products/${id}`)
const product = parser.parse(getRes.data).prestashop.product

// 2. Modifier ce qu'on veut
product.price = '15.500000'

// 3. PUT pour sauvegarder
const updateXml = builder.build({ prestashop: { product } })
await axios.put(`/api/products/${id}`, updateXml, {
  headers: { 'Content-Type': 'application/xml' }
})
```

⚠️ Cette technique peut échouer si la réponse GET contient des attributs read-only (`xlink:href`). Solution : reconstruire le XML manuellement (cf. exemple `setStock` ci-dessus).

---

## 5. POST en JSON vers endpoint custom

**Mots-clés** : POST, JSON, application/json, custom, module

Pour les endpoints **custom** ajoutés par nos modules (`stock_delta`, `order_state_change`, `stock_by_category`), on envoie du **JSON** au lieu de XML — c'est plus naturel et léger.

### Exemple — `order_state_change`

```js
const res = await axios.post('/api/order_state_change', {
  id_order: 42,
  new_state: 11,
}, {
  headers: { 'Content-Type': 'application/json' }
})

console.log(res.data)
// { success: true, id_order: 42, new_state: 11, stock_updates: [...] }
```

### Exemple — `stock_delta`

```js
async updateProductStockDelta(productId, delta, combinationId = 0) {
  const res = await axios.post('/api/stock_delta',
    { id_product: productId, id_product_attribute: combinationId, delta },
    { headers: { 'Content-Type': 'application/json' } }
  )
  return res.data
}
```

**Avantages du JSON par rapport au XML** :
- Plus court à écrire
- Pas besoin de `XMLBuilder` / `XMLParser`
- `res.data` est directement un objet JS

---

## 6. Lire et extraire la réponse XML

**Mots-clés** : XMLParser, parse, response, #text, @_

PrestaShop renvoie du XML même pour les POST/PUT réussis (avec la ressource créée/modifiée).

```js
import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '@_'
})

const result = parser.parse(response.data)
const order = result.prestashop?.order

// Les champs peuvent être string ou objet avec #text
const id = order?.id?.['#text'] ?? order?.id
const ref = order?.reference?.['#text'] ?? order?.reference
```

### Helpers utilitaires (à mettre au niveau module)

```js
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

const id = txt(order.id)        // "42"
const total = num(order.total_paid)   // 15.5
```

---

## 7. Gestion d'erreur (PrestaShop + axios)

**Mots-clés** : error, try, catch, CDATA, message, status

### Pattern try / catch standard

```js
try {
  const res = await axios.post('/api/orders', xmlContent, {
    headers: { 'Content-Type': 'application/xml' }
  })
  // ... traiter le résultat
} catch (err) {
  console.error('❌ Erreur:', err)
  throw err
}
```

### Extraire le message d'erreur PrestaShop (XML CDATA)

PrestaShop renvoie ses erreurs dans une structure XML :

```xml
<prestashop>
  <errors>
    <error>
      <code>134</code>
      <message><![CDATA[id_lang is required]]></message>
    </error>
  </errors>
</prestashop>
```

Helper pour extraire :

```js
async function postXml(endpoint, xmlBody) {
  try {
    const res = await axios.post(endpoint, xmlBody, {
      headers: { 'Content-Type': 'application/xml' }
    })
    return parser.parse(res.data)
  } catch (err) {
    if (err.response?.data) {
      const match = String(err.response.data).match(
        /<message><!\[CDATA\[(.*?)\]\]><\/message>/s
      )
      if (match) {
        console.error(`📝 PrestaShop error sur ${endpoint}:`, match[1])
        err.message = `${endpoint}: ${match[1]}`
      }
    }
    throw err
  }
}
```

### Fallback en cas de réponse vide

PrestaShop peut retourner un XML vide (`<prestashop></prestashop>`) si un module tiers fait planter la cascade. La commande est créée mais on n'a pas l'ID dans la réponse. Solution : retrouver l'ID via un GET de fallback.

```js
const result = parser.parse(response.data)
let order = result?.prestashop?.order

if (!order) {
  // Fallback : retrouver via id_cart qu'on connaît
  const fallback = await axios.get(
    `/api/orders?filter[id_cart]=${orderData.id_cart}&display=full&sort=[id_DESC]&limit=1`
  )
  const fallbackData = parser.parse(fallback.data)
  let orders = fallbackData?.prestashop?.orders?.order
  if (Array.isArray(orders)) orders = orders[0]
  if (!orders) throw new Error('Commande créée mais introuvable')
  order = orders
}
```

---

## 8. Upload de fichier (multipart/form-data)

**Mots-clés** : multipart, FormData, file, image, upload, blob

Pour uploader un fichier (image, par exemple) :

```js
const formData = new FormData()
formData.append('image', blob, 'photo.png')

await axios.post(`/api/images/products/${productId}`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

Exemple complet d'import d'images depuis un ZIP ([importService.js](../NewApp/src/services/importService.js) — méthode `importImages`) :

```js
const JSZip = (await import('jszip')).default
const zip = await JSZip.loadAsync(zipFile)
const entries = Object.values(zip.files).filter(f => /\.(png|jpe?g)$/i.test(f.name))

for (const entry of entries) {
  const blob = await entry.async('blob')
  const formData = new FormData()
  formData.append('image', blob, entry.name)

  await axios.post(`/api/images/products/${productId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

---

## 9. Cas d'usage POST dans ce projet

**Mots-clés** : POST, exemples, projet, NewApp

| Endpoint | Body | Service | Utilisé dans |
|----------|------|---------|--------------|
| `POST /api/categories` | XML category | importService.getOrCreateCategory | Import file 1 |
| `POST /api/taxes` | XML tax | importService.getOrCreateTax | Import file 1 |
| `POST /api/tax_rule_groups` | XML group | importService.getOrCreateTaxRuleGroup | Import file 1 |
| `POST /api/tax_rules` | XML rule | importService.getOrCreateTaxRuleGroup | Import file 1 |
| `POST /api/products` | XML product | importService.createProduct | Import file 1 |
| `POST /api/product_options` | XML option | importService.getOrCreateProductOption | Import file 2 |
| `POST /api/product_option_values` | XML value | importService.getOrCreateProductOptionValue | Import file 2 |
| `POST /api/combinations` | XML combination | importService.createCombination | Import file 2 |
| `POST /api/customers` | XML customer | importService.getOrCreateCustomer | Import file 3 |
| `POST /api/addresses` | XML address | importService.getOrCreateAddress | Import file 3 |
| `POST /api/carts` | XML cart | CartService.createCart, importService | Add to cart, import |
| `POST /api/orders` | XML order | orderService.createOrder, importService | Checkout, import |
| `POST /api/order_state_change` | **JSON** | orderService.changeOrderState | OrderList boutons |
| `POST /api/stock_delta` | **JSON** | StockService.updateProductStockDelta | Dashboard / StockMgmt |
| `POST /api/images/products/X` | **FormData** | importService.importImages | Import images.zip |

---

## 10. Cas d'usage PUT dans ce projet

**Mots-clés** : PUT, exemples, projet, update

| Endpoint | Body | Service | Utilisé dans |
|----------|------|---------|--------------|
| `PUT /api/stock_availables/{id}` | XML stock | StockService.updateStock, importService.setStock | Stock management, import |
| `PUT /api/carts/{id}` | XML cart | CartService.updateCart | Cart sync |
| `PUT /api/products/{id}` | XML product | (rarement utilisé) | Produit edit |

Note : la majorité des mises à jour passent par **`/api/order_state_change`** (POST custom) au lieu de PUT direct, car le PUT direct ne déclenche pas les hooks PrestaShop nécessaires.

---

## 11. Côté Service — pattern complet

**Mots-clés** : service, pattern, code, complet

### Template pour POST XML (création)

```js
import axios from '../config/axios'
import { XMLParser, XMLBuilder } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: '@_', format: true })

const txt = (v) => v?.['#text'] ?? v ?? ''

export const MyService = {
  async createItem(data) {
    try {
      const xml = builder.build({
        prestashop: {
          item: {
            name: data.name,
            // ... autres champs
          }
        }
      })

      const res = await axios.post('/api/items', xml, {
        headers: { 'Content-Type': 'application/xml' }
      })

      const result = parser.parse(res.data)
      return txt(result.prestashop.item.id)
    } catch (err) {
      console.error('❌ Erreur création item:', err)
      // Extraire message PrestaShop si dispo
      if (err.response?.data) {
        const match = String(err.response.data).match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/s)
        if (match) err.message = match[1]
      }
      throw err
    }
  },
}
```

### Template pour POST JSON (custom endpoint)

```js
async customAction(payload) {
  try {
    const res = await axios.post('/api/my_custom_endpoint', payload, {
      headers: { 'Content-Type': 'application/json' }
    })
    return res.data
  } catch (err) {
    console.error('❌ Erreur custom:', err)
    // PrestaShop renvoie aussi du JSON sur erreur via les endpoints custom
    const msg = err.response?.data?.error || err.message
    throw new Error(msg)
  }
}
```

### Template pour PUT XML (update)

```js
async updateItem(id, fields) {
  try {
    const xml = builder.build({
      prestashop: {
        item: {
          id,                // OBLIGATOIRE pour PUT
          ...fields,         // champs à mettre à jour
        }
      }
    })

    await axios.put(`/api/items/${id}`, xml, {
      headers: { 'Content-Type': 'application/xml' }
    })

    return true
  } catch (err) {
    console.error(`❌ Erreur update item ${id}:`, err)
    throw err
  }
}
```

---

## 12. Côté View — appel depuis une vue

**Mots-clés** : view, appel, formulaire, validation, async

### Pattern complet — formulaire qui POST

```vue
<template>
  <form @submit.prevent="onSubmit">
    <input v-model="form.name" placeholder="Nom" required :disabled="loading" />
    <button type="submit" :disabled="!canSubmit">
      {{ loading ? 'Création...' : 'Créer' }}
    </button>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>
  </form>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { MyService } from '../services/MyService'

const form = reactive({ name: '' })
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const canSubmit = computed(() => !loading.value && form.name.length > 0)

const onSubmit = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const id = await MyService.createItem(form)
    successMessage.value = `✅ Créé avec ID ${id}`
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
```

### Pattern — bouton qui PUT/POST custom

Extrait de [OrderList.vue](../NewApp/src/components/OrderList.vue) :

```vue
<button
  v-if="parseInt(item.current_state) === 11"
  @click="onDeliver(item)"
  :disabled="updatingId === item.id"
>
  📦 Livrer
</button>

<script setup>
const updatingId = ref(null)

const onDeliver = async (item) => {
  if (!confirm(`Marquer ${item.reference} comme LIVRÉE ?`)) return

  updatingId.value = item.id
  try {
    await OrderService.changeOrderState(item.id, 5)
    await loadAll()   // recharger la liste
  } catch (err) {
    errorMessage.value = err.response?.data?.error || err.message
  } finally {
    updatingId.value = null
  }
}
</script>
```

**Bonnes pratiques** :
- `updatingId.value = item.id` pour désactiver le bouton uniquement de l'item en cours
- `confirm()` avant action irréversible
- Recharger la liste après succès (sinon état stale)

---

## 13. Côté module PHP — recevoir un POST custom

**Mots-clés** : PHP, module, webservice, manage, php://input

Quand on veut faire du custom (POST JSON qui exécute une action), on crée un module PrestaShop avec une classe `WebserviceSpecificManagement` :

```php
class WebserviceSpecificManagementMyAction implements WebserviceSpecificManagementInterface
{
    // ... interface boilerplate (getters/setters)

    public function manage()
    {
        header('Content-Type: application/json');

        // Vérifier la méthode
        if ($this->wsObject->method !== 'POST') {
            http_response_code(405);
            die(json_encode(['error' => 'Method not allowed']));
        }

        // Lire le body JSON
        $input = json_decode(file_get_contents('php://input'), true);

        $id = (int)($input['id'] ?? 0);
        if (!$id) {
            http_response_code(400);
            die(json_encode(['error' => 'id requis']));
        }

        // Logique métier
        // ...

        // Retourner JSON
        die(json_encode([
            'success' => true,
            'id' => $id,
            // ...
        ]));
    }

    public function getContent()
    {
        return $this->output;
    }
}
```

Et dans le module principal :

```php
public function hookAddWebserviceResources($params)
{
    require_once __DIR__ . '/classes/WebserviceSpecificManagementMyAction.php';
    return [
        'my_action' => [
            'description' => 'Mon action custom',
            'specific_management' => true,
            'forbidden_method' => ['GET', 'PUT', 'PATCH', 'DELETE'],
        ]
    ];
}
```

Exemples concrets dans le projet :
- [modules/stockdeltaapi/](../modules/stockdeltaapi/) — POST `/api/stock_delta` (JSON)
- [modules/orderstateapi/](../modules/orderstateapi/) — POST `/api/order_state_change` (JSON)

---

## 14. Pièges et limitations PrestaShop

**Mots-clés** : pièges, limitations, gotchas, problèmes, bugs

### 14.1 Réponse vide après POST

Si un module tiers (ex. `gamification`, `ps_emailalerts`, `ps_googleanalytics`) plante pendant la cascade de hooks, PrestaShop renvoie `<prestashop></prestashop>` vide alors que la ressource EST créée. → utiliser un **fallback GET** pour retrouver l'ID.

### 14.2 PUT avec xlink:href dans le body

Si on fait `GET` puis `PUT` directement, PrestaShop refuse à cause des attributs `xlink:href`. → reconstruire un XML **minimal** sans ces attributs.

### 14.3 `physical_quantity` et `reserved_quantity` non exposés

Le webservice `/api/stock_availables` n'expose **que** `quantity`. Pour modifier `physical_quantity` ou `reserved_quantity`, il faut passer par un endpoint custom (notre `stock_delta` ou `order_state_change`).

### 14.4 `SymfonyContainer::getInstance()` null en webservice

PrestaShop's `StockManager::saveMovement()` (Core) utilise le container Symfony, qui n'est pas disponible en contexte webservice. → faire un `Db::getInstance()->insert('stock_mvt', [...])` direct depuis le module.

### 14.5 `current_state` dans le XML d'order

Si on POST `/api/orders` avec `current_state: 11`, PrestaShop le respecte. Mais il **décrémente automatiquement** `quantity`, donc si notre code re-décrémente, on a un double comptage.

### 14.6 Hooks deprecated qui plantent

Le module `gamification` utilise un hook `newOrder` deprecated → PrestaShop convertit ce warning en HTTP 500. Solution : supprimer la liaison hook (`DELETE FROM ps_hook_module WHERE id_module = X`).

### 14.7 Soft-delete sur certaines ressources

`DELETE /api/taxes/X` ne supprime pas physiquement si la taxe est référencée par un `order_detail_tax`. Elle reste avec `deleted=1` invisible mais présente.

---

## 15. Anti-patterns à éviter

**Mots-clés** : anti-pattern, mauvais, éviter

### ❌ 1. Construire le XML à la main avec des templates littéraux

```js
// ❌ ÉVITER
const xml = `<?xml version="1.0"?><prestashop><cart><id_customer>${id}</id_customer></cart></prestashop>`
```

Problèmes : caractères spéciaux (`&`, `<`, etc.) cassent le XML, indentation imprévisible.

✅ Toujours utiliser `XMLBuilder` :

```js
const xml = builder.build({ prestashop: { cart: { id_customer: id } } })
```

### ❌ 2. Mélanger `fetch` et `axios`

Le projet utilise **axios** (avec auth pré-configurée). Utiliser `fetch` casse le pattern et oblige à re-passer l'auth manuellement.

### ❌ 3. Oublier `Content-Type`

Sans header `application/xml` pour PrestaShop, le body est ignoré et tu reçois une erreur cryptique.

### ❌ 4. Forcer `current_state` au POST + appeler order_state_change ensuite

PrestaShop décrémente `quantity` automatiquement quand on POST avec `current_state=11`. Si notre `order_state_change(11)` re-décrémente, on a un double effet.

→ Soit ne pas mettre `current_state` dans le XML, soit ne pas re-décrémenter dans notre endpoint custom.

### ❌ 5. Ne pas vérifier la réponse

```js
// ❌ Risqué : si la réponse est vide, ça plante
const orderId = result.prestashop.order.id
```

✅ Toujours vérifier :

```js
const order = result?.prestashop?.order
if (!order) throw new Error('Réponse vide')
const orderId = order.id?.['#text'] ?? order.id
```

### ❌ 6. Parser dans chaque fonction

```js
async myMethod() {
  const parser = new XMLParser({ ... })   // ❌ recréé à chaque appel
}
```

✅ Déclarer parser/builder au niveau module :

```js
const parser = new XMLParser({ ... })   // ✅ une fois
export const Service = { /* utilise parser */ }
```

### ❌ 7. Ne pas gérer le 404 séparément

```js
catch (err) {
  throw err   // ❌ traite 404 comme erreur fatale
}
```

✅ Pour les listes :

```js
catch (err) {
  if (err.response?.status === 404) return []
  throw err
}
```

---

## 16. Templates copy-paste

**Mots-clés** : template, snippet, copy-paste, boilerplate

### 16.1 Service complet — POST XML (création)

```js
import axios from '../config/axios'
import { XMLParser, XMLBuilder } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: '@_', format: true })
const txt = (v) => v?.['#text'] ?? v ?? ''

export const FooService = {
  async create(data) {
    try {
      const xml = builder.build({
        prestashop: {
          foo: {
            name: data.name,
            // ... champs
          }
        }
      })

      const res = await axios.post('/api/foos', xml, {
        headers: { 'Content-Type': 'application/xml' }
      })

      const result = parser.parse(res.data)
      const foo = result?.prestashop?.foo
      if (!foo) throw new Error('Réponse invalide')

      return { id: txt(foo.id), ...foo }
    } catch (err) {
      if (err.response?.data) {
        const match = String(err.response.data).match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/s)
        if (match) err.message = match[1]
      }
      throw err
    }
  }
}
```

### 16.2 Service complet — PUT XML (update)

```js
async update(id, fields) {
  const xml = builder.build({
    prestashop: {
      foo: {
        id,                  // OBLIGATOIRE
        ...fields,
      }
    }
  })

  await axios.put(`/api/foos/${id}`, xml, {
    headers: { 'Content-Type': 'application/xml' }
  })

  return true
}
```

### 16.3 Service complet — POST JSON (custom endpoint)

```js
async myCustomAction(payload) {
  const res = await axios.post('/api/my_endpoint', payload, {
    headers: { 'Content-Type': 'application/json' }
  })
  return res.data
}
```

### 16.4 Module PHP — POST custom (réception)

```php
// modules/myapi/myapi.php
class Myapi extends Module {
    public function __construct() {
        $this->name = 'myapi';
        $this->version = '1.0.0';
        $this->displayName = 'My API';
        parent::__construct();
        require_once __DIR__ . '/classes/WebserviceSpecificManagementMyResource.php';
    }

    public function install() {
        return parent::install() && $this->registerHook('addWebserviceResources');
    }

    public function hookAddWebserviceResources() {
        return [
            'my_resource' => [
                'description' => 'My custom resource',
                'specific_management' => true,
                'forbidden_method' => ['GET', 'PUT', 'PATCH', 'DELETE'],
            ]
        ];
    }
}
```

```php
// modules/myapi/classes/WebserviceSpecificManagementMyResource.php
class WebserviceSpecificManagementMyResource implements WebserviceSpecificManagementInterface {
    protected $objOutput;
    protected $wsObject;
    protected $output = '';

    public function setObjectOutput($obj) { $this->objOutput = $obj; return $this; }
    public function getObjectOutput() { return $this->objOutput; }
    public function setWsObject($obj) { $this->wsObject = $obj; return $this; }
    public function getWsObject() { return $this->wsObject; }

    public function manage() {
        header('Content-Type: application/json');

        if ($this->wsObject->method !== 'POST') {
            http_response_code(405);
            die(json_encode(['error' => 'POST only']));
        }

        $input = json_decode(file_get_contents('php://input'), true);
        // ... logique métier
        die(json_encode(['success' => true]));
    }

    public function getContent() { return $this->output; }
}
```

### 16.5 View — formulaire POST avec toast

```vue
<template>
  <form @submit.prevent="onSubmit">
    <input v-model="form.name" :disabled="loading" required />
    <button :disabled="loading">{{ loading ? '...' : 'Envoyer' }}</button>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </form>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { FooService } from '../services/FooService'

const form = reactive({ name: '' })
const loading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const onSubmit = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await FooService.create(form)
    successMessage.value = `✅ Créé : ${result.id}`
    setTimeout(() => { successMessage.value = '' }, 3000)
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
```

---

## 17. Cheatsheet

**Mots-clés** : cheatsheet, antisèche, résumé, rappel

```
╔══════════════════════════════════════════════════════════════╗
║              POST / PUT — RAPPELS RAPIDES                    ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  POST PrestaShop (création XML)                              ║
║    axios.post('/api/X', xml, {                               ║
║      headers: { 'Content-Type': 'application/xml' }          ║
║    })                                                        ║
║                                                              ║
║  PUT PrestaShop (update XML)                                 ║
║    axios.put(`/api/X/${id}`, xml, {                          ║
║      headers: { 'Content-Type': 'application/xml' }          ║
║    })                                                        ║
║    ⚠️ ID dans l'URL ET dans le XML                           ║
║    ⚠️ XML minimal, pas de xlink:href                         ║
║                                                              ║
║  POST custom (JSON)                                          ║
║    axios.post('/api/X', { ...obj }, {                        ║
║      headers: { 'Content-Type': 'application/json' }         ║
║    })                                                        ║
║                                                              ║
║  CONSTRUIRE XML                                              ║
║    builder.build({                                           ║
║      prestashop: {                                           ║
║        resource: { ...champs }                               ║
║      }                                                       ║
║    })                                                        ║
║                                                              ║
║  CHAMP MULTILINGUE                                           ║
║    name: { language: { '@_id': 1, '#text': 'Mon nom' } }     ║
║                                                              ║
║  ASSOCIATIONS                                                ║
║    associations: {                                           ║
║      categories: { category: { id: 3 } }                     ║
║    }                                                         ║
║                                                              ║
║  LIRE RÉPONSE                                                ║
║    const result = parser.parse(res.data)                     ║
║    const id = result?.prestashop?.resource?.id?.['#text']    ║
║                                                              ║
║  GESTION D'ERREUR                                            ║
║    catch (err) {                                             ║
║      const match = err.response?.data.match(                 ║
║        /<message><!\\[CDATA\\[(.*?)\\]\\]><\\/message>/s)    ║
║      if (match) err.message = match[1]                       ║
║      throw err                                               ║
║    }                                                         ║
║                                                              ║
║  FALLBACK SI RÉPONSE VIDE                                    ║
║    GET /api/resource?filter[X]=value                         ║
║      &display=full&sort=[id_DESC]&limit=1                    ║
║                                                              ║
║  UPLOAD FICHIER                                              ║
║    formData.append('image', blob, name)                      ║
║    axios.post(url, formData, {                               ║
║      headers: { 'Content-Type': 'multipart/form-data' }      ║
║    })                                                        ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Différence POST/PUT | `POST vs PUT` |
| Config axios | `Setup de base` |
| Créer une ressource | `POST en XML` |
| Mettre à jour | `PUT en XML` |
| Endpoint custom JSON | `POST en JSON` |
| Extraire la réponse | `Lire et extraire` |
| Gérer les erreurs | `Gestion d'erreur` ou `CDATA` |
| Upload de fichier | `multipart` ou `Upload` |
| Liste des POST du projet | `Cas d'usage POST` |
| Liste des PUT du projet | `Cas d'usage PUT` |
| Template service | `Côté Service` |
| Template view | `Côté View` |
| Créer un module PHP | `Côté module PHP` |
| Bugs PrestaShop | `Pièges et limitations` |
| Erreurs à éviter | `Anti-patterns` |
| Code copy-paste | `Templates copy-paste` |
| Rappel express | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |
| Pagination | Voir [`listePagination.md`](listePagination.md) |

---

## 🗂️ Mapping rapide endpoint ↔ fichier source

| Endpoint | Service / fichier |
|----------|------------------|
| `POST /api/categories, taxes, products, ...` | [importService.js](../NewApp/src/services/importService.js) |
| `POST /api/carts` | [CartService.js](../NewApp/src/services/CartService.js) |
| `POST /api/orders` | [orderService.js](../NewApp/src/services/orderService.js) |
| `POST /api/order_state_change` | OrderService.changeOrderState + [modules/orderstateapi/](../modules/orderstateapi/) |
| `POST /api/stock_delta` | StockService.updateProductStockDelta + [modules/stockdeltaapi/](../modules/stockdeltaapi/) |
| `POST /api/images/products/X` | importService.importImages |
| `PUT /api/stock_availables/X` | StockService.updateStock, importService.setStock |
| `PUT /api/carts/X` | CartService.updateCart |
