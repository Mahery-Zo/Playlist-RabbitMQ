# 📕 Guide des Services — Bonnes pratiques de ce projet

> **Mots-clés généraux** : Service, axios, fast-xml-parser, XMLParser, XMLBuilder, PrestaShop, webservice, API, REST, JSON, XML, CRUD, filter, display=full, async, await, Promise, cache, localStorage, fetch

---

## 📑 Index

- [1. Anatomie d'un Service](#1-anatomie-dun-service)
- [2. Parsing XML PrestaShop (XMLParser)](#2-parsing-xml-prestashop-xmlparser)
- [3. Construction XML (XMLBuilder)](#3-construction-xml-xmlbuilder)
- [4. Requêtes CRUD via axios](#4-requêtes-crud-via-axios)
- [5. Filtres webservice et display](#5-filtres-webservice-et-display)
- [6. Gestion d'erreurs](#6-gestion-derreurs)
- [7. Conversions HT / TTC / Prix](#7-conversions-ht--ttc--prix)
- [8. Cache en mémoire (Map)](#8-cache-en-mémoire-map)
- [9. Auth & sessions](#9-auth--sessions)
- [10. Images en base64](#10-images-en-base64)
- [11. CSV : lecture, parsing](#11-csv--lecture-parsing)
- [12. Progress callback (onProgress)](#12-progress-callback-onprogress)
- [13. Suppression en cascade ordonnée](#13-suppression-en-cascade-ordonnée)
- [14. Get-or-create idempotent](#14-get-or-create-idempotent)
- [15. Inventaire détaillé des services](#15-inventaire-détaillé-des-services)
- [16. Anti-patterns identifiés](#16-anti-patterns-identifiés)
- [17. Templates de code à copier-coller](#17-templates-de-code-à-copier-coller)
- [18. Cheatsheet rapide](#18-cheatsheet-rapide)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Anatomie d'un Service

**Mots-clés** : structure, fichier, exporter, module, organisation, ES6, import

Structure type d'un service dans ce projet :

```js
import axios from '../config/axios'              // axios pré-configuré avec auth
import { XMLParser, XMLBuilder } from 'fast-xml-parser'

// Parser & builder au niveau MODULE (1 seule instance)
const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: '@_', format: true })

// Constantes
const ID_LANG = 1
const ID_SHOP = 1

// Helpers internes
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

// Service exporté
export const MyService = {
  async getAll() { ... },
  async getById(id) { ... },
  async create(data) { ... },
  async update(id, data) { ... },
  async delete(id) { ... },
}
```

**Bonnes pratiques** :

- ✅ `parser` / `builder` au niveau **module**, pas dans chaque fonction (importService et ResetService font ça, les autres non — à uniformiser)
- ✅ Toujours utiliser **axios pré-configuré** (`config/axios`) pour récupérer l'auth automatiquement
- ✅ Export d'un **objet nommé** (`export const ProductService = { ... }`)
- ✅ Helpers `txt()` et `num()` pour extraire les valeurs des objets XML parsés

**Exemple** : [importService.js:1-9](../NewApp/src/services/importService.js)

---

## 2. Parsing XML PrestaShop (XMLParser)

**Mots-clés** : XMLParser, parser, parsing, response, "#text", "@_", attribut, fast-xml-parser

PrestaShop renvoie du XML par défaut. On le convertit en objet JS pour le manipuler.

### Setup

```js
import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({
  ignoreAttributes: false,       // garder les attributs (xlink:href)
  attributeNamePrefix: '@_'      // attributs préfixés par @_
})
```

### Parser une réponse

```js
const res = await axios.get('/api/products/42')
const data = parser.parse(res.data)
const product = data.prestashop.product
```

### Structure typique du résultat

```xml
<product>
  <id><![CDATA[42]]></id>
  <id_category xlink:href="...">  <![CDATA[3]]>  </id_category>
  <name>
    <language id="1" xlink:href="..."><![CDATA[T-shirt]]></language>
  </name>
</product>
```

→ devient :

```js
{
  id: { '#text': '42' },
  id_category: { '#text': '3', '@_xlink:href': '...' },
  name: {
    language: { '#text': 'T-shirt', '@_id': '1', '@_xlink:href': '...' }
  }
}
```

### Helpers d'extraction

Pour éviter de répéter `?.['#text']` partout :

```js
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

const id = txt(product.id)                          // "42"
const price = num(product.price)                    // 19.99
const name = txt(product.name?.language) || ''      // "T-shirt"
```

### Gérer "un seul élément" vs "tableau"

Quand un endpoint retourne **une** ressource, le parser donne un objet. Pour **plusieurs**, un tableau. Toujours normaliser :

```js
let items = data.prestashop?.products?.product || []
if (!Array.isArray(items)) items = [items]
```

**Exemple** : [CartService.js:188-192](../NewApp/src/services/CartService.js), [importService.js:82-93](../NewApp/src/services/importService.js)

---

## 3. Construction XML (XMLBuilder)

**Mots-clés** : XMLBuilder, builder, POST, PUT, créer, créer XML, body, payload

Quand on POST ou PUT vers PrestaShop, il faut envoyer du XML.

### Setup

```js
import { XMLBuilder } from 'fast-xml-parser'

const builder = new XMLBuilder({
  ignoreAttributes: false,
  attributeNamePrefix: '@_',
  format: true                  // indentation lisible (debug)
})
```

### Construire un XML simple

```js
const xml = builder.build({
  prestashop: {
    category: {
      active: 1,
      id_parent: 2,
      name: {
        language: { '@_id': 1, '#text': 'Akanjo' }
      }
    }
  }
})
```

Résultat :

```xml
<prestashop>
  <category>
    <active>1</active>
    <id_parent>2</id_parent>
    <name>
      <language id="1">Akanjo</language>
    </name>
  </category>
</prestashop>
```

### Champs multilingues

Tous les champs textuels de PrestaShop sont multilingues. **Toujours** envelopper avec `language[id]` :

```js
name: {
  language: { '@_id': ID_LANG, '#text': 'Mon produit' }
}
```

### Associations (liens vers d'autres ressources)

Pour lier un produit à une catégorie ou des options à une combinaison :

```js
{
  combination: {
    id_product: idProduct,
    price: '5.000000',
    associations: {
      product_option_values: {
        product_option_value: { id: optionValueId }
      }
    }
  }
}
```

### XML minimal pour PUT

Pour mettre à jour (PUT), envoie le **minimum** de champs (éviter les `xlink:href` et les champs read-only qui font planter PrestaShop) :

```js
// ❌ ÉVITER : reprendre tout le GET et juste modifier quantity
// → PrestaShop refuse à cause des xlink:href

// ✅ ENVOYER UN XML MINIMAL
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
    }
  }
})
```

**Exemple** : [importService.js:331-362](../NewApp/src/services/importService.js)

---

## 4. Requêtes CRUD via axios

**Mots-clés** : axios, GET, POST, PUT, DELETE, get, post, put, delete, headers

### Setup global

L'axios du projet est pré-configuré avec l'API Key :

```js
import axios from '../config/axios'
// L'auth est injectée automatiquement par l'intercepteur
```

### GET — liste

```js
const res = await axios.get('/api/products?display=full')
const data = parser.parse(res.data)
let products = data.prestashop?.products?.product || []
if (!Array.isArray(products)) products = [products]
```

### GET — un seul

```js
const res = await axios.get(`/api/products/${id}?display=full`)
const product = parser.parse(res.data).prestashop.product
```

### POST — création

```js
const xml = builder.build({ prestashop: { product: { ... } } })

const res = await axios.post('/api/products', xml, {
  headers: { 'Content-Type': 'application/xml' }
})

const id = parser.parse(res.data).prestashop.product.id
```

### PUT — mise à jour

```js
await axios.put(`/api/products/${id}`, xml, {
  headers: { 'Content-Type': 'application/xml' }
})
```

### DELETE — suppression

```js
await axios.delete(`/api/products/${id}`)
```

### POST JSON custom (endpoint custom)

```js
const res = await axios.post('/api/stock_delta',
  { id_product: 42, delta: 5 },
  { headers: { 'Content-Type': 'application/json' } }
)
return res.data    // déjà JSON, pas besoin de parser
```

**Exemples** : [CartService.js:16-20](../NewApp/src/services/CartService.js), [StockService.js:137-145](../NewApp/src/services/StockService.js)

---

## 5. Filtres webservice et display

**Mots-clés** : filter, display, sort, full, query, paramètres, recherche, encoding

### `display=full`

Sans rien, PrestaShop renvoie juste les IDs et liens. Avec `display=full`, tu obtiens tous les champs :

```js
axios.get('/api/products')           // → liste d'IDs seuls
axios.get('/api/products?display=full')   // → objets complets
```

### `display=[champ1,champ2]`

Pour économiser de la bande passante :

```js
axios.get('/api/orders?display=[id_cart]')
// → uniquement les id_cart de chaque order
```

### `filter[field]=value`

Filtre par champ exact :

```js
// Tous les paniers d'un client
axios.get(`/api/carts?filter[id_customer]=${customerId}&display=full`)

// Stock d'un produit spécifique
axios.get(`/api/stock_availables?filter[id_product]=${id}&filter[id_product_attribute]=${combId}`)

// Customer par email — encoder le caractère @
axios.get(`/api/customers?filter[email]=${encodeURIComponent(email)}`)
```

### `sort=[champ_DESC]`

```js
axios.get('/api/orders?sort=[id_DESC]&display=full')
```

### Combinaison

```js
const url = `/api/orders?filter[id_customer]=${id}&display=full&sort=[id_DESC]`
```

**Exemples** : [CartService.js:183](../NewApp/src/services/CartService.js), [orderService.js:24](../NewApp/src/services/orderService.js)

---

## 6. Gestion d'erreurs

**Mots-clés** : error, try, catch, finally, throw, console.error, PrestaShop, CDATA

### Pattern try / catch standard

```js
async getProduct(id) {
  try {
    const res = await axios.get(`/api/products/${id}`)
    return parser.parse(res.data).prestashop.product
  } catch (err) {
    console.error(`❌ Erreur produit ${id}:`, err)
    throw err
  }
}
```

### Extraire le message d'erreur PrestaShop

PrestaShop renvoie ses erreurs dans une structure XML avec `<message><![CDATA[...]]></message>`. Pour avoir un message clair :

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

**Avantage** : au lieu de "Request failed with status 400", tu obtiens "id_lang is required".

### Gestion gracieuse (continue sur erreur)

Pour des opérations où une erreur sur une ligne ne doit pas arrêter le reste :

```js
const results = { success: 0, errors: [] }

for (const row of rows) {
  try {
    await processRow(row)
    results.success++
  } catch (err) {
    console.error('❌ Erreur ligne:', err)
    results.errors.push({ row, error: err.message })
  }
}

return results
```

### 404 → tableau vide (pas une erreur)

```js
async function getList(resource) {
  try {
    const res = await axios.get(`/api/${resource}`)
    return parser.parse(res.data)
  } catch (err) {
    if (err.response?.status === 404) return []
    throw err
  }
}
```

**Exemples** : [importService.js:63-80](../NewApp/src/services/importService.js), [ResetService.js:14-25](../NewApp/src/services/ResetService.js)

---

## 7. Conversions HT / TTC / Prix

**Mots-clés** : prix, HT, TTC, taxe, taxRate, conversion, parseFloat, toFixed

### Concept

PrestaShop stocke les prix produit en **HT** (`product.price`). Le prix **TTC** est calculé à l'affichage.

### HT → TTC

```js
const priceTTC = priceHT * (1 + taxRate / 100)
```

### TTC → HT

```js
const priceHT = priceTTC / (1 + taxRate / 100)
```

### Format pour PrestaShop

PrestaShop exige les prix avec **6 décimales** dans le XML :

```js
{
  price: priceHT.toFixed(6),                    // "11.196429"
  wholesale_price: wholesalePrice.toFixed(6),
}
```

### Conversion nombres français

CSV provenant d'Excel français utilise `,` comme séparateur décimal et `%` :

```js
const parseFrenchNumber = (s) => parseFloat(String(s ?? '').replace(',', '.')) || 0
const parseFrenchPercent = (s) =>
  parseFloat(String(s ?? '').replace(',', '.').replace('%', '')) || 0

parseFrenchNumber('12,5')      // → 12.5
parseFrenchPercent('11,65%')   // → 11.65
```

### Delta combinaison

Le `price` d'une **combinaison** n'est PAS le prix de vente. C'est un **delta** (positif ou négatif) qui s'ajoute au prix de base :

```js
// Pour la combinaison "XL" qui coûte 5€ de plus
const deltaTTC = priceComboTTC - priceProductTTC
const deltaHT = deltaTTC / (1 + taxRate / 100)
// → on stocke deltaHT dans combination.price
```

**Exemples** : [importService.js:548-562](../NewApp/src/services/importService.js), [ProductService.js:33-51](../NewApp/src/services/ProductService.js)

---

## 8. Cache en mémoire (Map)

**Mots-clés** : cache, Map, mémoire, doublons, idempotent, performance, optimisation

Pour les imports massifs, on évite de re-créer les mêmes ressources. Utilise un **cache `Map`** au niveau module.

### Déclaration

```js
const cache = {
  categories: new Map(),       // name → id
  taxes: new Map(),            // rate → id
  products: new Map(),         // reference → { id, priceHT, priceTTC, taxRate }
  customers: new Map(),        // email → id
}

export const MyService = {
  resetCache() {
    Object.values(cache).forEach(m => m.clear())
  },
  // ...
}
```

### Pattern get-or-create

```js
async function getOrCreateCategory(name) {
  // 1. Cache hit
  if (cache.categories.has(name)) return cache.categories.get(name)

  // 2. Chercher en BDD
  const existing = await getList('categories', 'category')
  for (const cat of existing) {
    if (txt(cat.name?.language) === name) {
      const id = txt(cat.id)
      cache.categories.set(name, id)
      return id
    }
  }

  // 3. Créer
  const xml = builder.build({ prestashop: { category: { ... } } })
  const result = await postXml('/api/categories', xml)
  const id = txt(result.prestashop.category.id)
  cache.categories.set(name, id)
  return id
}
```

**Bonnes pratiques** :

- ✅ Vider le cache au début d'une nouvelle opération d'import (`resetCache()`)
- ✅ Clés simples (`name`, `email`) ou composites (`${productRef}|${valueName}`)
- ✅ Stocker le minimum nécessaire (id) ou un objet complet selon usage
- ❌ Ne pas cacher des données qui peuvent changer pendant l'opération

**Exemple** : [importService.js:93-110](../NewApp/src/services/importService.js)

---

## 9. Auth & sessions

**Mots-clés** : auth, session, login, logout, bcrypt, localStorage, employee, customer

### Auth Basic (API Key)

Géré globalement dans [config/axios.js](../NewApp/src/config/axios.js) :

```js
axios.defaults.auth = {
  username: 'API_KEY',
  password: ''
}
```

→ Dans tes services, **rien à faire**, axios injecte automatiquement.

### Vérifier un mot de passe bcrypt

PrestaShop hash en bcrypt (`$2y$10$...`). JavaScript ne peut pas vérifier directement → on délègue à un endpoint PHP :

```js
async function verifyPassword(plaintext, hash) {
  const res = await fetch('http://localhost:9443/prestashop/NewApp/public/verify_password.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password: plaintext, hash })
  })
  const data = await res.json()
  return data.valid
}
```

### Hasher un mot de passe (pour création)

Même approche :

```js
async function hashPassword(password) {
  const res = await fetch('http://localhost:9443/prestashop/NewApp/public/hash_password.php', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password })
  })
  return (await res.json()).hash    // "$2y$10$..."
}
```

### Sauvegarder la session

```js
saveSession(customer) {
  localStorage.setItem('selectedCustomer', JSON.stringify(customer))
},

getSession() {
  const data = localStorage.getItem('selectedCustomer')
  return data ? JSON.parse(data) : null
},

logout() {
  localStorage.removeItem('selectedCustomer')
},

isAuthenticated() {
  return localStorage.getItem('selectedCustomer') !== null
}
```

### Login complet (employé)

```js
async login(email, password) {
  const employee = await this.getEmployeeByEmail(email)

  if (!employee) return { success: false, message: 'Email introuvable' }
  if (parseInt(employee.active) !== 1) return { success: false, message: 'Compte inactif' }

  const valid = await verifyPassword(password, employee.passwd)
  if (!valid) return { success: false, message: 'Mot de passe incorrect' }

  return { success: true, employee }
}
```

**Exemples** : [authService.js:136-228](../NewApp/src/services/authService.js), [CustomerService.js:74-159](../NewApp/src/services/CustomerService.js)

---

## 10. Images en base64

**Mots-clés** : image, base64, btoa, Uint8Array, arraybuffer, blob, data:image

PrestaShop sert les images en binaire. Pour les afficher dans le front sans URL externe, on les convertit en data URI base64.

### Récupération binaire

```js
const imageRes = await axios.get(`/api/images/products/${productId}/${imageId}`, {
  responseType: 'arraybuffer'
})
```

### Conversion en base64

```js
const base64 = btoa(
  new Uint8Array(imageRes.data).reduce(
    (data, byte) => data + String.fromCharCode(byte),
    ''
  )
)
const dataUrl = `data:image/jpeg;base64,${base64}`
```

→ utilisable directement dans une `<img :src="...">`.

### Upload d'image (POST multipart)

```js
const formData = new FormData()
formData.append('image', blob, 'photo.png')

await axios.post(`/api/images/products/${productId}`, formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

**Exemple** : [ProductService.js:108-122](../NewApp/src/services/ProductService.js), [importService.js:760-770](../NewApp/src/services/importService.js)

---

## 11. CSV : lecture, parsing

**Mots-clés** : CSV, parseCSV, FileReader, séparateur, guillemets, échappés

### Parser CSV robuste (avec quotes échappées `""`)

Le `split(',')` naïf ne fonctionne pas si les champs contiennent eux-mêmes des virgules. Voici un parser solide :

```js
function parseCSV(text) {
  const rows = []
  let row = []
  let field = ''
  let inQuotes = false

  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    if (inQuotes) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++ }   // "" échappé
      else if (c === '"') { inQuotes = false }
      else { field += c }
    } else {
      if (c === '"') { inQuotes = true }
      else if (c === ',') { row.push(field); field = '' }
      else if (c === '\r') { /* skip */ }
      else if (c === '\n') { row.push(field); rows.push(row); row = []; field = '' }
      else { field += c }
    }
  }
  if (field || row.length) { row.push(field); rows.push(row) }

  const headers = rows.shift()
  return rows
    .filter(r => r.some(v => v && v.trim()))
    .map(r => Object.fromEntries(headers.map((h, i) => [h.trim(), (r[i] ?? '').trim()])))
}
```

→ retourne un **tableau d'objets** indexés par les noms de colonnes.

### Lire un fichier uploadé côté client

```js
const readText = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = reject
  reader.readAsText(file)
})

const handleImport = async () => {
  const text = await readText(selectedFile.value)
  const rows = parseCSV(text)
  // ...
}
```

### Parsing date FR

```js
function parseDate(s) {
  if (!s) return new Date().toISOString().split('T')[0]
  const [d, m, y] = s.split('/')
  return `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`
}

parseDate('01/12/2025')   // → "2025-12-01"
```

### Parsing format custom (regex)

Pour un format métier comme `[("T_01";3;"ngoza"),("C_03";1;"")]` :

```js
function parseAchat(s) {
  if (!s) return []
  const items = []
  const re = /\("([^"]+)";(\d+);"([^"]*)"\)/g
  let m
  while ((m = re.exec(s)) !== null) {
    items.push({ ref: m[1], qty: parseInt(m[2]), value: m[3] })
  }
  return items
}
```

**Exemple** : [importService.js:15-61](../NewApp/src/services/importService.js)

---

## 12. Progress callback (onProgress)

**Mots-clés** : progress, callback, progression, UI, barre, avancement

Pour donner du feedback à l'utilisateur pendant une opération longue, le service prend un callback `onProgress`.

### Côté service

```js
async function importFile(rows, onProgress) {
  const results = { success: 0, errors: [] }

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]

    // Notifie l'UI à chaque ligne
    onProgress?.({
      step: 'Import produits',
      current: i + 1,
      total: rows.length,
      label: row.reference,
    })

    try {
      await processRow(row)
      results.success++
    } catch (err) {
      results.errors.push({ line: i + 1, error: err.message })
    }
  }

  return results
}
```

### Côté View

```vue
<script setup>
const currentStep = ref('')
const currentCount = ref(0)
const currentTotal = ref(0)
const currentLabel = ref('')

const onProgress = (p) => {
  currentStep.value = p.step
  currentCount.value = p.current
  currentTotal.value = p.total
  currentLabel.value = p.label
}

const startImport = async () => {
  await ImportService.importFile(rows, onProgress)
}
</script>

<template>
  <div class="progress">
    {{ currentStep }} — {{ currentCount }} / {{ currentTotal }}
    <span v-if="currentLabel">: {{ currentLabel }}</span>
  </div>
</template>
```

**Bonne pratique** : `onProgress?.(...)` avec optional chaining → le service fonctionne aussi sans callback.

**Exemple** : [importService.js:557-562](../NewApp/src/services/importService.js), [ResetService.js:43-59](../NewApp/src/services/ResetService.js)

---

## 13. Suppression en cascade ordonnée

**Mots-clés** : delete, suppression, cascade, ordre, dépendances, FK, foreign key, reset

Quand on réinitialise une base, l'**ordre** des DELETE est crucial à cause des contraintes de clés étrangères.

### Helpers

```js
async function fetchIds(resource, itemKey) {
  try {
    const res = await axios.get(`/api/${resource}`)
    const data = parser.parse(res.data)
    let items = data.prestashop?.[resource]?.[itemKey] || []
    if (!Array.isArray(items)) items = [items]
    return items.map(it => it['@_id']).filter(Boolean)
  } catch (err) {
    if (err.response?.status === 404) return []
    throw err
  }
}

async function safeDelete(resource, id) {
  try {
    await axios.delete(`/api/${resource}/${id}`)
    return true
  } catch (err) {
    console.warn(`⚠️ DELETE ${resource}/${id} a échoué:`, err.message)
    return false
  }
}

async function deleteAll(resource, itemKey, onProgress, options = {}) {
  let ids = await fetchIds(resource, itemKey)
  if (options.exclude) {
    ids = ids.filter(id => !options.exclude.includes(parseInt(id)))
  }

  let success = 0, failed = 0
  for (let i = 0; i < ids.length; i++) {
    onProgress?.({ step: resource, current: i + 1, total: ids.length })
    const ok = await safeDelete(resource, ids[i])
    ok ? success++ : failed++
  }

  return { resource, total: ids.length, success, failed }
}
```

### Ordre de reset (du plus dépendant au moins dépendant)

```js
async resetAll(onProgress) {
  const results = []

  // 1. Commandes (cascade order_history, order_invoice)
  results.push(await deleteAll('orders', 'order', onProgress))

  // 1bis. order_details orphelins (libère order_detail_tax)
  results.push(await deleteAll('order_details', 'order_detail', onProgress))

  // 2. Paniers
  results.push(await deleteAll('carts', 'cart', onProgress))

  // 3. Clients (cascade addresses)
  results.push(await deleteAll('customers', 'customer', onProgress))

  // 4. Adresses orphelines
  results.push(await deleteAll('addresses', 'address', onProgress))

  // 5. Combinaisons (cascade stock_available)
  results.push(await deleteAll('combinations', 'combination', onProgress))

  // 6. Produits (cascade images, stock, combinaisons restantes)
  results.push(await deleteAll('products', 'product', onProgress))

  // 7. Mouvements de stock orphelins
  results.push(await deleteAll('stock_movements', 'stock_movement', onProgress))

  // 8. Valeurs d'options puis options
  results.push(await deleteAll('product_option_values', 'product_option_value', onProgress))
  results.push(await deleteAll('product_options', 'product_option', onProgress))

  // 9. Tax rules → tax_rule_groups → taxes (dans cet ordre !)
  results.push(await deleteAll('tax_rules', 'tax_rule', onProgress))
  results.push(await deleteAll('tax_rule_groups', 'tax_rule_group', onProgress))
  results.push(await deleteAll('taxes', 'tax', onProgress))

  // 10. Catégories sauf root (1) et home (2)
  results.push(await deleteAll('categories', 'category', onProgress, { exclude: [1, 2] }))

  return results
}
```

**À retenir** :

- ⚠️ `orders` AVANT `order_details` (sinon les détails restent orphelins)
- ⚠️ `tax_rules` AVANT `taxes` (sinon les taxes sont soft-deleted au lieu de hard-deleted)
- ⚠️ `combinations` AVANT `products` (cascade)
- ⚠️ Catégories : exclure les IDs système (1 = root, 2 = home)

**Exemple** : [ResetService.js:62-115](../NewApp/src/services/ResetService.js)

---

## 14. Get-or-create idempotent

**Mots-clés** : idempotent, get-or-create, getOrCreate, doublons, cherche-ou-crée

Pattern essentiel pour les imports : ne pas créer de doublons.

```js
async function getOrCreateTax(rate) {
  const key = rate.toFixed(2)

  // 1. Cache mémoire
  if (cache.taxes.has(key)) return cache.taxes.get(key)

  // 2. Chercher en BDD (filtrer par taux et non-soft-deleted)
  const existing = await getList('taxes', 'tax')
  for (const t of existing) {
    const isDeleted = parseInt(txt(t.deleted) || 0) === 1
    if (!isDeleted && Math.abs(num(t.rate) - rate) < 0.001) {
      const id = txt(t.id)
      cache.taxes.set(key, id)
      return id
    }
  }

  // 3. Créer si pas trouvé
  const xml = builder.build({
    prestashop: {
      tax: {
        rate: rate.toFixed(3),
        active: 1,
        name: { language: { '@_id': ID_LANG, '#text': `TVA ${rate}%` } }
      }
    }
  })
  const result = await postXml('/api/taxes', xml)
  const id = txt(result.prestashop.tax.id)
  cache.taxes.set(key, id)
  return id
}
```

**Bonne pratique** : pour les `Number` (taux, prix), utilise une **comparaison avec tolérance** (`Math.abs(a - b) < 0.001`) au lieu d'une égalité stricte (les flottants peuvent légèrement diverger).

**Exemples** : [importService.js:120-160](../NewApp/src/services/importService.js)

---

## 15. Inventaire détaillé des services

**Mots-clés** : inventaire, liste, services, vue d'ensemble, fichiers

### CartService.js

**Rôle** : CRUD complet sur les paniers PrestaShop.

| Méthode | Description |
|---------|-------------|
| `createCart(cartData)` | POST `/api/carts` — retourne `{success, cart_id, data}` |
| `buildCartXML(cartData, cartId?)` | Construit le XML cart (avec cart_rows) |
| `getCart(cartId)` | GET `/api/carts/{id}?display=full` |
| `updateCart(cartId, cartData)` | PUT `/api/carts/{id}` |
| `deleteCart(cartId)` | DELETE `/api/carts/{id}` |
| `getCustomerCarts(customerId)` | GET filtré par `id_customer` |
| `getCartsWithoutOrder()` | Tous les paniers sans commande associée (pour Dashboard) |

### StockService.js

**Rôle** : gestion des stocks et des mouvements.

| Méthode | Description |
|---------|-------------|
| `getStock(productId, combinationId)` | Récupère un stock pour produit + combinaison |
| `updateStock(stockId, newQuantity)` | ⚠️ ancien — ne crée PAS de mouvement |
| `addToStock(productId, combinationId, qty)` | ✅ via `/api/stock_delta` — crée un mouvement |
| `getProductStockHistory(productId, combinationId)` | GET historique journalier |
| `updateProductStockDelta(productId, delta, combinationId)` | POST custom (module `stockdeltaapi`) |
| `getAllStocksWithProducts()` | ⚠️ N+1 requêtes — coûteux |

### ProductService.js

**Rôle** : produits avec images, combinaisons, options, taxes, réductions.

| Méthode | Description |
|---------|-------------|
| `getAllProducts()` | Liste tous, calcul TTC + réductions |
| `getProductById(id)` | Détails complets : images base64, combinaisons, options |
| `getQttComb(idComb)` | Quantité d'une combinaison |
| `getMarqueDate(idproduit, dateForm)` | Retourne `'HOT' \| 'NEW' \| 'NONE'` selon ancienneté |
| `getAllCategories()` | Liste des catégories |

### orderService.js

**Rôle** : commandes (CRUD + changement de statut).

| Méthode | Description |
|---------|-------------|
| `getCustomerOrders(customerId)` | Commandes d'un client, triées par id_DESC |
| `getAllOrders()` | Toutes les commandes |
| `updateOrderState(orderId, newStateId)` | POST sur `/api/order_histories` |
| `createOrder(orderData)` | POST `/api/orders` + force état 11 via order_history |
| `createCart(cartData)` | (doublon avec CartService) |
| `importOrdersFromCSV(csvData)` | Import en masse |
| `buildOrderXML(orderData)` | Helper de construction XML |

### CustomerService.js

**Rôle** : clients (lecture, login, adresses, session).

| Méthode | Description |
|---------|-------------|
| `getAllCustomers()` | Liste tous |
| `getCustomerByEmail(email)` | Recherche par email |
| `getCustomerById(id)` | Détails d'un client |
| `getCustomerAddresses(customerId)` | Adresses associées |
| `login(email, password)` | Auth via bcrypt PHP endpoint |
| `saveSession(customer)` | localStorage `selectedCustomer` |
| `getSession()`, `logout()`, `isAuthenticated()` | Helpers session |

### authService.js

**Rôle** : employés (login admin du back-office).

Symétrique de CustomerService mais pour `/api/employees`. localStorage `employee`.

### ResetService.js

**Rôle** : suppression complète des données métier dans l'ordre.

| Méthode | Description |
|---------|-------------|
| `resetAll(onProgress)` | Cascade ordonnée — voir [section 13](#13-suppression-en-cascade-ordonnée) |

### importService.js

**Rôle** : import en masse depuis 3 CSV + 1 ZIP d'images.

| Méthode | Description |
|---------|-------------|
| `importFile1(csv, onProgress)` | Catégories, taxes, produits |
| `importFile2(csv, onProgress)` | Options, combinaisons, stock initial |
| `importFile3(csv, onProgress)` | Clients, paniers, commandes |
| `importImages(zip, onProgress)` | Images via JSZip + multipart upload |
| `resetCache()` | Vide les Maps de cache |

### UserService.js

**Rôle** : ⚠️ partiel et redondant avec CustomerService. À nettoyer.

### apiService.js

**Rôle** : ⚠️ fichier vide actuellement.

---

## 16. Anti-patterns identifiés

**Mots-clés** : anti-pattern, mauvaise pratique, à éviter, refactor

### ❌ 1. URLs et clés API hardcodées

```js
// ❌ UserService.js / orderService.js
auth: { username: "SHR5P8DJGSJXQSSKKT6SX88N4ZCSFGLZ", password: "" }

// ❌ authService.js / CustomerService.js / importService.js
fetch('http://localhost:9443/prestashop/NewApp/public/verify_password.php', ...)
```

**Fix** : utiliser `import.meta.env.VITE_API_KEY` et `import.meta.env.VITE_PRESTASHOP_URL`, ou bien l'axios pré-configuré qui injecte l'auth automatiquement.

### ❌ 2. Code dupliqué

- `orderService.getDansLePanier()` et `getAllOrders()` sont **identiques** (74 lignes).
- `authService.login()` et `CustomerService.login()` ont la même structure (employee vs customer).

**Fix** : factoriser dans un service générique (`useLogin(resource, sessionKey)`).

### ❌ 3. Requêtes N+1

```js
// ❌ ProductService.getAllProducts : pour N produits, fait 3N requêtes (TVA, réductions, images)
for (const product of products) {
  const tax = await axios.get(`/api/tax_rule_groups/${tg}`)
  const reductions = await axios.get(`/api/specific_prices?filter[id_product]=${id}`)
  const image = await axios.get(`/api/images/products/${id}`, { responseType: 'arraybuffer' })
}
```

**Fix** : `Promise.all()` pour paralléliser, ou batcher les appels.

```js
// ✅ Avec Promise.all
const products = rawProducts.map(async (p) => {
  const [tax, reductions, image] = await Promise.all([
    fetchTax(p.id),
    fetchReductions(p.id),
    fetchImage(p.id),
  ])
  return { ...p, tax, reductions, image }
})
```

### ❌ 4. Parsing CSV naïf

```js
// ❌ orderService.parseCSV : split(",") qui casse sur les champs contenant ","
const values = lines[i].split(",")
```

**Fix** : utiliser le parser robuste d'`importService.js` (gère `""` échappés). Voir [section 11](#11-csv--lecture-parsing).

### ❌ 5. Mélange `fetch()` et `axios`

```js
// PrestaShop API → axios
// PHP endpoints → fetch()
```

**Fix** : uniformiser sur axios pour cohérence (intercepteurs, gestion d'erreurs uniforme).

### ❌ 6. Parser instancié dans chaque fonction

```js
// ❌ Pattern fréquent
async getProduct(id) {
  const parser = new XMLParser({ ... })   // recréé à chaque appel
  // ...
}
```

**Fix** : déclarer parser/builder au niveau module (importService et ResetService font ça correctement).

### ❌ 7. Erreurs silencieuses

```js
// ❌ StockService.getAllStocksWithProducts
} catch (error) {
  console.warn("⚠️ Erreur lors du traitement d'un stock:", error)
  // continue silencieusement
}
```

**Fix** : retourner les erreurs dans le résultat (`{ success, errors }`) pour que l'UI puisse les afficher.

### ❌ 8. Pas de validation des inputs

Aucun service ne valide les IDs, emails, types numériques avant de faire les appels API. PrestaShop renvoie alors des erreurs verbeuses que l'utilisateur subit.

**Fix** : valider côté service (id > 0, email format, etc.) avant axios.

---

## 17. Templates de code à copier-coller

**Mots-clés** : template, snippet, boilerplate, modèle, copier-coller

### 17.1 Squelette de service

```js
import axios from '../config/axios'
import { XMLParser, XMLBuilder } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: '@_', format: true })

const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

export const MyService = {
  async getAll() {
    try {
      const res = await axios.get('/api/my_resource?display=full')
      const data = parser.parse(res.data)
      let items = data.prestashop?.my_resource?.item || []
      if (!Array.isArray(items)) items = [items]
      return items.map(it => ({
        id: txt(it.id),
        name: txt(it.name),
      }))
    } catch (err) {
      console.error('❌ Erreur getAll:', err)
      throw err
    }
  },

  async getById(id) {
    const res = await axios.get(`/api/my_resource/${id}?display=full`)
    return parser.parse(res.data).prestashop.item
  },

  async create(data) {
    const xml = builder.build({
      prestashop: { item: { ...data } }
    })
    const res = await axios.post('/api/my_resource', xml, {
      headers: { 'Content-Type': 'application/xml' }
    })
    return txt(parser.parse(res.data).prestashop.item.id)
  },

  async update(id, data) {
    const xml = builder.build({
      prestashop: { item: { id, ...data } }
    })
    await axios.put(`/api/my_resource/${id}`, xml, {
      headers: { 'Content-Type': 'application/xml' }
    })
  },

  async delete(id) {
    await axios.delete(`/api/my_resource/${id}`)
  },
}
```

### 17.2 Helper postXml avec extraction d'erreur

```js
async function postXml(endpoint, xmlBody) {
  try {
    const res = await axios.post(endpoint, xmlBody, {
      headers: { 'Content-Type': 'application/xml' }
    })
    return parser.parse(res.data)
  } catch (err) {
    if (err.response?.data) {
      const match = String(err.response.data).match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/s)
      if (match) {
        err.message = `${endpoint}: ${match[1]}`
      }
    }
    throw err
  }
}
```

### 17.3 Helper getList paramétré

```js
async function getList(resource, itemKey, filter = '') {
  try {
    const res = await axios.get(`/api/${resource}?display=full${filter}`)
    const data = parser.parse(res.data)
    let items = data.prestashop?.[resource]?.[itemKey] || []
    if (!Array.isArray(items)) items = [items]
    return items
  } catch (err) {
    if (err.response?.status === 404) return []
    throw err
  }
}

// Usage
const products = await getList('products', 'product')
const myProducts = await getList('products', 'product', `&filter[id_category]=${catId}`)
```

### 17.4 Import en masse avec progress

```js
export const MyImportService = {
  async importRows(rows, onProgress) {
    const results = { success: 0, errors: [] }

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i]

      onProgress?.({
        step: 'Import',
        current: i + 1,
        total: rows.length,
        label: row.reference,
      })

      try {
        await this.processRow(row)
        results.success++
      } catch (err) {
        console.error(`❌ Ligne ${i + 1}:`, err)
        results.errors.push({ line: i + 1, error: err.message })
      }
    }

    return results
  },

  async processRow(row) {
    // ... création par ligne
  }
}
```

### 17.5 Session minimale

```js
export const SessionHelper = {
  save(key, data) {
    localStorage.setItem(key, JSON.stringify(data))
  },
  get(key) {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  },
  remove(key) {
    localStorage.removeItem(key)
  },
  exists(key) {
    return localStorage.getItem(key) !== null
  },
}
```

### 17.6 Calcul de prix TTC

```js
export const PriceHelper = {
  htToTtc(priceHT, taxRate) {
    return priceHT * (1 + taxRate / 100)
  },
  ttcToHt(priceTTC, taxRate) {
    return priceTTC / (1 + taxRate / 100)
  },
  formatForPrestaShop(price) {
    return Number(price).toFixed(6)
  },
}
```

---

## 18. Cheatsheet rapide

**Mots-clés** : cheatsheet, mémo, antisèche, rappel

```
╔══════════════════════════════════════════════════════════════╗
║              SERVICE PRESTASHOP — RAPPELS                    ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  SETUP                                                       ║
║    import axios from '../config/axios'                       ║
║    import { XMLParser, XMLBuilder } from 'fast-xml-parser'   ║
║                                                              ║
║  PARSER / BUILDER                                            ║
║    new XMLParser({ ignoreAttributes: false,                  ║
║                    attributeNamePrefix: '@_' })              ║
║                                                              ║
║  HELPERS                                                     ║
║    txt(v)  →  v?.['#text'] ?? v ?? ''                        ║
║    num(v)  →  parseFloat(txt(v)) || 0                        ║
║                                                              ║
║  GET liste                                                   ║
║    axios.get('/api/X?display=full')                          ║
║    items = data.prestashop?.X?.itemKey                       ║
║    if (!Array.isArray(items)) items = [items]                ║
║                                                              ║
║  GET filtré                                                  ║
║    /api/X?filter[field]=value&display=full                   ║
║                                                              ║
║  POST                                                        ║
║    axios.post(url, xml, {                                    ║
║      headers: { 'Content-Type': 'application/xml' }          ║
║    })                                                        ║
║                                                              ║
║  PUT minimal                                                 ║
║    Ne renvoyer QUE les champs writables                      ║
║                                                              ║
║  DELETE en cascade                                           ║
║    Ordre = du plus dépendant au moins                        ║
║                                                              ║
║  CACHE                                                       ║
║    Map<key, id>  →  get-or-create                            ║
║                                                              ║
║  CSV                                                         ║
║    Parser custom robuste (gère "" échappés)                  ║
║    parseFrenchNumber: replace ',' → '.'                      ║
║                                                              ║
║  ERREURS                                                     ║
║    Regex CDATA pour message clair                            ║
║                                                              ║
║  PRIX                                                        ║
║    TTC = HT × (1 + taxRate/100)                              ║
║    toFixed(6) pour XML                                       ║
║                                                              ║
║  PROGRESS                                                    ║
║    onProgress?.({ step, current, total, label })             ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Structure d'un service | `Anatomie` |
| Parser XML PrestaShop | `XMLParser` |
| Construire XML | `XMLBuilder` |
| Requête GET / POST / PUT / DELETE | `Requêtes CRUD` |
| Filtre webservice | `filter[` ou `display=full` |
| Gestion d'erreur | `try catch` ou `CDATA` |
| Calcul HT/TTC | `Conversions HT` ou `priceTTC` |
| Cache en mémoire | `Cache Map` |
| Login / session | `Auth sessions` |
| Image base64 | `base64` ou `Uint8Array` |
| Lire un CSV | `parseCSV` |
| Barre de progression | `onProgress` |
| Reset / suppression cascade | `Suppression en cascade` |
| Ne pas créer de doublons | `get-or-create` ou `idempotent` |
| Vue d'ensemble d'un service | `Inventaire détaillé` |
| Erreur à corriger | `Anti-patterns` |
| Code à copier | `Templates` |
| Rappel express | `Cheatsheet` |
| Refs Vue | Voir [`RefVue.md`](RefVue.md) |
| Views Vue | Voir [`View.md`](View.md) |

---

## 🗂️ Mapping Services ↔ Patterns documentés

Référence rapide pour savoir où regarder le code source d'un pattern.

| Pattern | Service d'exemple |
|---------|-------------------|
| Parser au niveau module | importService, ResetService |
| Parser dans fonction | CartService, StockService, ProductService (à uniformiser) |
| XMLBuilder | CartService, importService, orderService |
| Filtre `filter[]` | CartService, CustomerService, importService |
| Helper postXml + extraction CDATA | importService |
| Helper getList | importService, ResetService |
| Cache `Map` | importService |
| get-or-create | importService (tous les `getOrCreate*`) |
| Auth bcrypt PHP endpoint | authService, CustomerService, importService |
| Sessions localStorage | authService, CustomerService |
| Image base64 | ProductService |
| Image upload multipart | importService |
| Parser CSV robuste | importService |
| Parser CSV naïf (à éviter) | orderService |
| onProgress callback | importService, ResetService |
| Suppression cascade ordonnée | ResetService |
| Calcul HT ↔ TTC | importService, ProductService |
| Delta combinaison | importService, ProductService |
| Genération secure_key (32 hex) | orderService, importService |
| FormData multipart upload | importService |
| Promise dynamic import (JSZip) | importService |
