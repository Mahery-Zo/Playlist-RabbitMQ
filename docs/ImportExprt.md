# 🛠️ Guide d'extension de l'Import (CSV / ZIP)

> Playbook pratique : **"Pour faire X, modifier Y"**. Indique précisément quel fichier et quelle section toucher selon le besoin.

> **Mots-clés généraux** : import, CSV, ZIP, importService, ImportView, validation, métier, exception, colonne, condition, doublon, action, état, transition, helper, mapping

---

## 📑 Index

- [1. Vue d'ensemble du flow d'import](#1-vue-densemble-du-flow-dimport)
- [2. Carte des fichiers concernés](#2-carte-des-fichiers-concernés)
- [3. Ajouter une validation métier (montant, longueur, format, ...)](#3-ajouter-une-validation-métier)
- [4. Ajouter / renommer une colonne dans le CSV](#4-ajouter--renommer-une-colonne-dans-le-csv)
- [5. Ajouter une action conditionnelle (si colonne X = valeur)](#5-ajouter-une-action-conditionnelle)
- [6. Gérer un nouvel état de commande](#6-gérer-un-nouvel-état-de-commande)
- [7. Détecter / refuser des lignes en doublon](#7-détecter--refuser-des-lignes-en-doublon)
- [8. Skip des lignes selon un critère](#8-skip-des-lignes-selon-un-critère)
- [9. Modifier le format ou la validation de date](#9-modifier-le-format-ou-la-validation-de-date)
- [10. Ajouter une vérification de stock avant la commande](#10-ajouter-une-vérification-de-stock-avant-la-commande)
- [11. Modifier le mapping CSV → API PrestaShop](#11-modifier-le-mapping-csv--api-prestashop)
- [12. Ajouter un nouveau fichier CSV à importer](#12-ajouter-un-nouveau-fichier-csv-à-importer)
- [13. Personnaliser la progress bar / les messages](#13-personnaliser-la-progress-bar--les-messages)
- [14. Gérer une exception PrestaShop spécifique](#14-gérer-une-exception-prestashop-spécifique)
- [15. Ajouter une transition d'état custom (livré, annulé, ...)](#15-ajouter-une-transition-détat-custom)
- [16. Logging / tracer l'avancement](#16-logging--tracer-lavancement)
- [17. Cas particuliers (paniers seuls, état vide, BOM CSV...)](#17-cas-particuliers)
- [18. Anti-patterns à éviter](#18-anti-patterns-à-éviter)
- [19. Cheatsheet "Pour X, modifier Y"](#19-cheatsheet-pour-x-modifier-y)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Vue d'ensemble du flow d'import

**Mots-clés** : architecture, flow, schéma

```
ImportView.vue (vue)
     │
     │ 1. User pick files
     │ 2. read CSV as text
     ▼
ImportService (logique métier)
     │
     ├─ importFile1(csv)  → catégories, taxes, produits
     ├─ importFile2(csv)  → options, combinaisons, stock initial
     ├─ importFile3(csv)  → clients, addresses, paniers, commandes
     └─ importImages(zip) → upload images via /api/images/products/X
            │
            ▼
PrestaShop Webservice + modules custom (orderstateapi, stockdeltaapi)
```

Chaque `importFileX` :
1. parse le CSV (`parseCSV`)
2. valide les colonnes (`validateHeaders`)
3. boucle ligne par ligne, valide chaque champ
4. crée les ressources PrestaShop via les helpers (`getOrCreateCategory`, etc.)
5. retourne `{ success, errors }`

---

## 2. Carte des fichiers concernés

**Mots-clés** : carte, fichiers, structure

| Fichier | Rôle | Quand le modifier |
|---------|------|-------------------|
| [NewApp/src/services/importService.js](../NewApp/src/services/importService.js) | **Logique métier** de tous les imports | 90% des changements |
| [NewApp/src/views/BackOffice/ImportView.vue](../NewApp/src/views/BackOffice/ImportView.vue) | UI de sélection de fichiers et résultats | UI / nouveaux fichiers |
| [modules/stockdeltaapi/](../modules/stockdeltaapi/) | Endpoint webservice custom `/api/stock_delta` | Logique stock |
| [modules/orderstateapi/](../modules/orderstateapi/) | Endpoint webservice custom `/api/order_state_change` | Logique changement d'état commande |
| [NewApp/src/services/StockService.js](../NewApp/src/services/StockService.js) | Wrapper Vue → API stock | Si le service appelle stock_delta |
| [NewApp/src/services/orderService.js](../NewApp/src/services/orderService.js) | Wrapper Vue → API commandes | Si le service appelle order_state_change |

---

## 3. Ajouter une validation métier

**Mots-clés** : validation, montant, positif, négatif, format, longueur

### 3.1 Cas : "le montant doit être > 0"

📄 **Fichier** : `importService.js`
📍 **Section** : helpers en haut du fichier (vers ligne 89-102)

Helpers déjà disponibles :

```js
function assertPositive(value, fieldName) {
  if (!isFinite(value) || value <= 0) {
    throw new Error(`Montant invalide pour ${fieldName} : "${value}" — doit être > 0`)
  }
}

function assertNonNegative(value, fieldName) {
  if (!isFinite(value) || value < 0) {
    throw new Error(`Montant invalide pour ${fieldName} : "${value}" — doit être >= 0`)
  }
}
```

📍 **Usage** : dans `importFile1` / `importFile2` / `importFile3` dans la boucle `for (const row of data)` :

```js
try {
  const priceTTC = parseFrenchNumber(row.prix_ttc)
  assertPositive(priceTTC, 'prix_ttc')   // ← ICI
  // ...
}
```

L'erreur sera attrapée par le `try/catch` et la ligne sera marquée comme échec dans `results.errors`.

### 3.2 Cas : "ajouter une validation custom (ex. longueur min)"

Crée un nouveau helper :

```js
function assertMinLength(value, fieldName, min = 3) {
  if (String(value ?? '').trim().length < min) {
    throw new Error(`${fieldName} doit faire au moins ${min} caractères : "${value}"`)
  }
}

// Usage
assertMinLength(row.nom, 'nom', 2)
```

### 3.3 Cas : "regex format (téléphone, code postal...)"

```js
function assertPattern(value, fieldName, regex, hint = '') {
  if (!regex.test(String(value ?? ''))) {
    throw new Error(`${fieldName} format invalide : "${value}"${hint ? ' — ' + hint : ''}`)
  }
}

assertPattern(row.telephone, 'téléphone', /^\d{10}$/, '10 chiffres')
assertPattern(row.code_postal, 'code postal', /^\d{5}$/, '5 chiffres')
```

---

## 4. Ajouter / renommer une colonne dans le CSV

**Mots-clés** : colonne, header, validateHeaders, nouvelle colonne

### 4.1 Cas : "ajouter une colonne `marque` au fichier 1"

📄 **Fichier** : `importService.js`
📍 **Sections à toucher** :

**Étape 1** — Mettre à jour `validateHeaders` dans `importFile1` :

```js
validateHeaders(headers, [
  'date_availability_produit', 'nom', 'reference',
  'prix_ttc', 'Taxe', 'categorie', 'prix_achat',
  'marque',   // ← AJOUT
])
```

**Étape 2** — Utiliser la nouvelle valeur dans la boucle :

```js
const marque = row.marque?.trim() || ''
const idManufacturer = await getOrCreateManufacturer(marque)   // si tu crées ce helper

const idProduct = await createProduct({
  // ...
  id_manufacturer: idManufacturer,
})
```

**Étape 3** — Si la colonne sert à créer une nouvelle ressource (manufacturer), créer le helper correspondant (sur le modèle de `getOrCreateCategory`).

### 4.2 Cas : "renommer une colonne"

Modifier uniquement `validateHeaders` et les accès `row.X`.

⚠️ Si tu renommes côté CSV, tu dois renommer **partout** dans le code qui lit cette ligne — sinon erreur silencieuse (le champ vaudra `undefined`).

### 4.3 Cas : "rendre une colonne optionnelle"

```js
// Ne pas inclure dans validateHeaders → optionnelle
validateHeaders(headers, ['nom', 'reference'])

// Lire avec fallback
const note = row.note ?? ''
```

---

## 5. Ajouter une action conditionnelle

**Mots-clés** : condition, si, switch, valeur, colonne

### 5.1 Cas : "si état = 'promo', appliquer une réduction"

📄 **Fichier** : `importService.js`
📍 **Section** : dans la boucle `for (const row of data)` de `importFile1` ou `importFile3` selon le besoin

```js
for (const row of data) {
  try {
    // ... création produit ...

    // Action conditionnelle
    if (row.statut === 'promo') {
      const reduction = parseFrenchPercent(row.reduction || '0%')
      await createSpecificPrice({
        id_product: idProduct,
        reduction,
        reduction_type: 'percentage',
      })
    }
  } catch (err) { ... }
}
```

### 5.2 Cas : "trois comportements selon une colonne (switch)"

```js
const e = String(row.etat ?? '').toLowerCase().trim()

switch (true) {
  case e.includes('accept'):
    await createCartAndOrder({ ..., etat: 'paiement accepté' })
    break
  case e.includes('livr'):
    await createCartAndOrder({ ..., etat: 'livré' })
    break
  case e.includes('annul'):
    await createCartAndOrder({ ..., etat: 'annulé' })
    break
  default:
    // état vide → cart seul
    await createCart({ ... })
}
```

C'est exactement le pattern qu'on a dans `createCartAndOrder` avec `consumesStock(etat)`.

### 5.3 Cas : "appliquer une action seulement à certaines lignes (filtre)"

```js
for (const row of data) {
  // Ignorer les lignes commentées (commencent par "//")
  if (row.nom?.startsWith('//')) continue

  // Ignorer les lignes test
  if (row.nom?.toLowerCase().includes('test')) continue

  // ... reste du traitement
}
```

---

## 6. Gérer un nouvel état de commande

**Mots-clés** : état, state, transition, nouveau

### 6.1 Cas : "ajouter l'état 'En préparation' (id=3)" au fichier 3

📄 **Fichier 1** : `importService.js` — dans `createCartAndOrder`

```js
const e = String(etat ?? '').toLowerCase().trim()
let needsOrder = false
let extraState = null

if (e.includes('accept'))   { needsOrder = true; extraState = null }
else if (e.includes('livr')) { needsOrder = true; extraState = 5 }
else if (e.includes('annul')) { needsOrder = true; extraState = 6 }
else if (e.includes('prépar')) { needsOrder = true; extraState = 3 }   // ← AJOUT
```

📄 **Fichier 2** : `modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`

Ajouter une branche dans `manage()` :

```php
} elseif ($new_state === 3) {
    // Nouvel état : "En préparation"
    $this->transitionToPreparation($order);
}
```

Et créer la méthode `transitionToPreparation` (sur le modèle de `transitionToDelivered`).

---

## 7. Détecter / refuser des lignes en doublon

**Mots-clés** : doublon, unicité, set, déjà vu

### 7.1 Cas : "refuser deux clients avec le même email dans le fichier 3"

📄 **Fichier** : `importService.js`
📍 **Section** : début de `importFile3`, avant la boucle

```js
async importFile3(csvText, onProgress) {
  // ... parseCSV, validateHeaders ...

  // ===== Détection des doublons =====
  const seenEmails = new Set()
  const duplicates = []
  data.forEach((row, i) => {
    const email = (row.email ?? '').toLowerCase().trim()
    if (!email) return
    if (seenEmails.has(email)) {
      duplicates.push({ line: i + 1, email, error: 'Email en doublon dans le CSV' })
    } else {
      seenEmails.add(email)
    }
  })

  if (duplicates.length > 0) {
    results.errors.push(...duplicates)
    // Soit : on stoppe l'import
    return results
    // Soit : on continue mais en ayant signalé les doublons
  }

  // ... boucle d'import normale
}
```

### 7.2 Cas : "détecter deux lignes identiques (toutes colonnes)"

```js
const seenSignatures = new Set()
for (let i = 0; i < data.length; i++) {
  const row = data[i]
  const signature = JSON.stringify(row)
  if (seenSignatures.has(signature)) {
    results.errors.push({ line: i + 1, error: 'Ligne identique à une précédente' })
    continue
  }
  seenSignatures.add(signature)
  // ... traitement
}
```

### 7.3 Cas : "lignes en doublon par référence uniquement"

```js
const seenRefs = new Map()
for (let i = 0; i < data.length; i++) {
  const ref = row.reference
  if (seenRefs.has(ref)) {
    const firstLine = seenRefs.get(ref)
    results.errors.push({
      line: i + 1,
      ref,
      error: `Référence "${ref}" déjà présente ligne ${firstLine}`
    })
    continue
  }
  seenRefs.set(ref, i + 1)
  // ... traitement
}
```

---

## 8. Skip des lignes selon un critère

**Mots-clés** : skip, ignorer, continue, filtre

### Pattern général

```js
for (let i = 0; i < data.length; i++) {
  const row = data[i]

  // Skip si critère
  if (shouldSkip(row)) {
    console.log(`Ligne ${i + 1} ignorée :`, row)
    continue
  }

  try { ... } catch (err) { ... }
}
```

### Exemples

```js
// Skip si colonne vide
if (!row.reference?.trim()) continue

// Skip si test
if (row.email?.endsWith('@test.com')) continue

// Skip si date dans le futur
const parsedDate = parseDate(row.date)
if (parsedDate > new Date().toISOString().split('T')[0]) continue

// Skip si déjà importé (basé sur cache)
if (cache.products.has(row.reference)) continue
```

---

## 9. Modifier le format ou la validation de date

**Mots-clés** : date, format, parseDate, regex

📄 **Fichier** : `importService.js`
📍 **Fonction** : `parseDate` (vers ligne 63)

### 9.1 Cas : "accepter aussi le format DD-MM-YYYY"

```js
function parseDate(s) {
  if (!s) return new Date().toISOString().split('T')[0]

  // Accepter DD/MM/YYYY ou DD-MM-YYYY
  const match = String(s).trim().match(/^(\d{2})[\/\-](\d{2})[\/\-](\d{4})$/)
  if (!match) {
    throw new Error(`Format de date invalide : "${s}" — attendu DD/MM/YYYY ou DD-MM-YYYY`)
  }

  const [, d, m, y] = match
  // ... reste identique
}
```

### 9.2 Cas : "accepter aussi le format ISO YYYY-MM-DD"

```js
function parseDate(s) {
  if (!s) return new Date().toISOString().split('T')[0]
  s = String(s).trim()

  // Format ISO
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s

  // Format français DD/MM/YYYY
  const match = s.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (match) {
    const [, d, m, y] = match
    return `${y}-${m}-${d}`
  }

  throw new Error(`Format de date invalide : "${s}"`)
}
```

### 9.3 Cas : "refuser les dates passées"

```js
const parsedDate = parseDate(row.date)
const today = new Date().toISOString().split('T')[0]
if (parsedDate < today) {
  throw new Error(`Date dans le passé : "${row.date}"`)
}
```

---

## 10. Ajouter une vérification de stock avant la commande

**Mots-clés** : stock, vérification, disponible, tracker

📄 **Fichier** : `importService.js`
📍 **Section** : `importFile3`, avant l'appel à `createCartAndOrder`

Pattern déjà en place :

```js
// Tracker de stock disponible (chargé au début)
const stockTracker = new Map()
const stocks = await getList('stock_availables', 'stock_available')
for (const s of stocks) {
  const idProd = String(txt(s.id_product))
  const idAttr = String(txt(s.id_product_attribute))
  const qty = parseInt(txt(s.quantity)) || 0
  stockTracker.set(`${idProd}|${idAttr}`, qty)
}

// Dans la boucle, AVANT createCartAndOrder
if (consumesStock(row.etat)) {
  for (const it of items) {
    const key = `${it.idProduct}|${it.idCombination}`
    const available = stockTracker.get(key) ?? 0
    if (it.qty > available) {
      throw new Error(`Stock insuffisant pour ${it.ref} : ${it.qty} demandé(s), seulement ${available} disponible(s)`)
    }
  }
}
```

### Ajouter une variante : "vérifier aussi le stock physique"

```js
// Charger 2 trackers
const stockTracker = new Map()      // qty disponible
const physicalTracker = new Map()   // qty physique

for (const s of stocks) {
  const key = `${txt(s.id_product)}|${txt(s.id_product_attribute)}`
  stockTracker.set(key, parseInt(txt(s.quantity)) || 0)
  physicalTracker.set(key, parseInt(txt(s.physical_quantity)) || 0)
}

// Vérifier les deux pour une livraison
if (e.includes('livr')) {
  for (const it of items) {
    const key = `${it.idProduct}|${it.idCombination}`
    const phys = physicalTracker.get(key) ?? 0
    if (it.qty > phys) {
      throw new Error(`Stock physique insuffisant pour livrer ${it.ref}`)
    }
  }
}
```

---

## 11. Modifier le mapping CSV → API PrestaShop

**Mots-clés** : mapping, conversion, XMLBuilder, champ

### 11.1 Cas : "ajouter le poids du produit (`weight`) au fichier 1"

📄 **Fichier** : `importService.js`
📍 **Fonction** : `createProduct` (vers ligne 200)

```js
async function createProduct({ name, reference, priceHT, idTaxGroup, idCategory, wholesalePrice, dateAvailable, weight }) {
  const xml = builder.build({
    prestashop: {
      product: {
        name: { language: { '@_id': ID_LANG, '#text': name } },
        reference,
        price: priceHT.toFixed(6),
        wholesale_price: wholesalePrice.toFixed(6),
        weight: (weight || 0).toFixed(3),   // ← AJOUT
        id_tax_rules_group: idTaxGroup,
        // ...
      }
    }
  })
  // ...
}
```

Et dans `importFile1` :

```js
const idProduct = await createProduct({
  // ...
  weight: parseFrenchNumber(row.poids),   // ← AJOUT
})
```

### 11.2 Cas : "mettre la description (texte long)"

```js
// Dans createProduct
description: { language: { '@_id': ID_LANG, '#text': description } },
description_short: { language: { '@_id': ID_LANG, '#text': descShort } },
```

Attention : description doit être nettoyée du HTML si CSV contient du brut.

---

## 12. Ajouter un nouveau fichier CSV à importer

**Mots-clés** : nouveau fichier, file4, additional CSV

### 12.1 ImportView.vue

Ajouter un FileInput :

```vue
<FileInput
  label="Fichier 4 — Promotions"
  accept=".csv"
  :file="file4"
  @file-selected="file4 = $event"
/>
```

Et dans le script :

```js
const file4 = ref(null)

const canImport = computed(() =>
  file1.value || file2.value || file3.value || zipFile.value || file4.value
)

const startImport = async () => {
  // ...
  if (file4.value) {
    const text = await readText(file4.value)
    const r = await ImportService.importFile4(text, onProgress)
    results.value.push({ step: 'Fichier 4 — Promotions', ...r })
  }
}
```

### 12.2 importService.js

Ajouter la méthode :

```js
async importFile4(csvText, onProgress) {
  const results = { success: 0, errors: [] }
  let headers, data
  try {
    const parsed = parseCSV(csvText)
    headers = parsed.headers
    data = parsed.data
    validateHeaders(headers, ['reference', 'reduction_pct', 'date_from', 'date_to'])
  } catch (err) {
    results.errors.push({ line: 'header', error: err.message })
    return results
  }

  for (let i = 0; i < data.length; i++) {
    const row = data[i]
    onProgress?.({ step: 'Fichier 4 — Promotions', current: i + 1, total: data.length, label: row.reference })
    try {
      // ... logique métier
      results.success++
    } catch (err) {
      results.errors.push({ line: i + 1, ref: row.reference, error: err.message })
    }
  }
  return results
}
```

---

## 13. Personnaliser la progress bar / les messages

**Mots-clés** : progress, label, onProgress

📄 **Fichier** : `importService.js`
📍 **Section** : dans n'importe quelle boucle `importFileX`

### Modifier le label affiché

```js
onProgress?.({
  step: 'Fichier 1 — Produits',
  current: i + 1,
  total: data.length,
  label: `${row.reference} (${row.nom})`,   // ← combo référence + nom
})
```

### Ajouter une étape "sous-étape"

```js
onProgress?.({
  step: 'Fichier 3 — Préparation du tracker stock...',
  current: 0,
  total: 0,
  label: '',
})

// après chargement
onProgress?.({ step: 'Fichier 3 — Commandes', ... })
```

---

## 14. Gérer une exception PrestaShop spécifique

**Mots-clés** : exception, CDATA, error, PrestaShop

PrestaShop renvoie ses erreurs dans un XML avec `<message><![CDATA[...]]></message>`.

📄 **Fichier** : `importService.js`
📍 **Helper** : `postXml` (vers ligne 115)

Déjà en place :

```js
async function postXml(endpoint, xmlBody) {
  try {
    const res = await axios.post(endpoint, xmlBody, { ... })
    return parser.parse(res.data)
  } catch (err) {
    if (err.response?.data) {
      const match = String(err.response.data).match(
        /<message><!\[CDATA\[(.*?)\]\]><\/message>/s
      )
      if (match) {
        err.message = `${endpoint}: ${match[1]}`
      }
    }
    throw err
  }
}
```

### Ajouter une retry sur erreur réseau

```js
async function postXml(endpoint, xmlBody, retries = 2) {
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const res = await axios.post(endpoint, xmlBody, { ... })
      return parser.parse(res.data)
    } catch (err) {
      // Retry uniquement sur erreurs réseau (pas sur 4xx)
      if (attempt < retries && !err.response) {
        await new Promise(r => setTimeout(r, 500 * (attempt + 1)))
        continue
      }
      // Extraire le message PrestaShop
      // ...
      throw err
    }
  }
}
```

---

## 15. Ajouter une transition d'état custom

**Mots-clés** : transition, état, custom, livré, annulé

📄 **Fichier 1** : `modules/orderstateapi/classes/WebserviceSpecificManagementOrderStateChange.php`

Ajouter une branche dans `manage()` :

```php
} elseif ($new_state === N) {
    $this->transitionToNewState($order);
}
```

Et créer la méthode (sur le modèle de `transitionToDelivered`) :

```php
private function transitionToNewState(Order $order)
{
    // 1. Changer l'état via OrderHistory
    $history = new OrderHistory();
    $history->id_order = (int)$order->id;
    $history->id_order_state = N;
    $history->changeIdOrderState(N, $order);
    $history->add();

    // 2. Logique métier sur le stock
    foreach ($order->getProducts() as $product) {
        // ... toucher physical_quantity / reserved_quantity / quantity
        // ... insérer stock_mvt si mouvement physique
    }
}
```

📄 **Fichier 2** : `NewApp/src/services/importService.js` — `createCartAndOrder`

```js
else if (e.includes('mon_etat')) { needsOrder = true; extraState = N }
```

📄 **Fichier 3** : `NewApp/src/components/OrderList.vue` — bouton optionnel

```vue
<button v-if="parseInt(item.current_state) === SOURCE_STATE" @click="onCustomTransition(item)">
  Action custom
</button>
```

---

## 16. Logging / tracer l'avancement

**Mots-clés** : log, console, debug, trace

### Niveau "verbose"

```js
console.log(`📦 Ligne ${i + 1}: ref="${row.reference}", taxe=${taxRate}%, prix HT=${priceHT}`)
```

### Niveau "compteurs"

```js
let createdCount = 0, reusedCount = 0

if (cache.products.has(row.reference)) {
  reusedCount++
} else {
  createdCount++
  cache.products.set(row.reference, ...)
}

console.log(`✅ ${createdCount} créés, ${reusedCount} réutilisés`)
```

### Niveau "uniquement les erreurs"

```js
// En haut du fichier
const DEBUG = false

const log = (...args) => DEBUG && console.log(...args)

log('📦 Détail :', row)   // n'affiche rien en prod
```

---

## 17. Cas particuliers

**Mots-clés** : edge case, particulier, vide, BOM, encoding

### 17.1 BOM UTF-8 (caractère invisible en début de fichier)

Certains CSV Excel ont un BOM `﻿` en début. Pour le supprimer :

📄 **Fichier** : `importService.js` — début de `parseCSV`

```js
function parseCSV(text) {
  if (text.charCodeAt(0) === 0xFEFF) {
    text = text.substring(1)   // supprime le BOM
  }
  // ... reste identique
}
```

### 17.2 Encoding différent

Si le CSV n'est pas en UTF-8 (ex. ISO-8859-1), il faut le décoder différemment :

📄 **Fichier** : `ImportView.vue` — fonction `readText`

```js
const readText = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = reject
  reader.readAsText(file, 'ISO-8859-1')   // ← spécifier l'encoding
})
```

### 17.3 Cellule contenant des sauts de ligne

Le parser gère déjà les valeurs entre guillemets, même multi-lignes. Mais si tu as des bugs, vérifie que tous les champs problématiques sont bien entre `"..."`.

### 17.4 État `vide` dans fichier 3 (cart seul)

📄 **Fichier** : `importService.js` — `createCartAndOrder`

```js
if (!needsOrder) return { cartId, orderId: null }
```

→ Quand `etat` est vide, on crée seulement un cart (pas d'order). Pour modifier ce comportement :

```js
// Ne plus créer de cart non plus
if (!needsOrder) {
  console.log('État vide : aucun cart créé')
  return { cartId: null, orderId: null }
}
```

### 17.5 Cellule vide vs absente

```js
row.colonne === ''                    // colonne présente mais vide
row.colonne === undefined             // colonne absente du CSV
row.colonne == null                   // vide OU absente
String(row.colonne ?? '').trim() === ''   // vide OU "    "
```

---

## 18. Anti-patterns à éviter

**Mots-clés** : anti-pattern, erreur

### ❌ 1. Modifier le CSV reçu

```js
data[i].reference = data[i].reference.trim()   // ❌ effets de bord
```

✅ Utiliser des variables locales :

```js
const ref = (row.reference ?? '').trim()
```

### ❌ 2. throw sans catch

```js
const idCategory = await getOrCreateCategory(row.categorie)
// ❌ Si throw, toute la boucle s'arrête
```

✅ Le `try/catch` autour de chaque itération attrape ; **NE JAMAIS** retirer le try/catch principal.

### ❌ 3. Modifier le tracker stock avant la création réussie

```js
// ❌ MAUVAIS — si createCartAndOrder throw, le tracker est faussé
stockTracker.set(key, stockTracker.get(key) - qty)
await createCartAndOrder(...)
```

✅ Modifier APRÈS le succès :

```js
await createCartAndOrder(...)
// SI on arrive ici, tout s'est bien passé
stockTracker.set(key, stockTracker.get(key) - qty)
```

### ❌ 4. Oublier de valider les headers

Si une colonne change de nom (ex. CSV mal généré), sans `validateHeaders` tu obtiens silencieusement `undefined` partout → erreurs cryptiques.

### ❌ 5. Mélanger `await axios.post('/api/X')` et `postXml('/api/X')`

`postXml` extrait les messages d'erreur CDATA, `axios.post` non. Pour la cohérence, **toujours** utiliser `postXml` pour les POST XML vers PrestaShop.

### ❌ 6. Charger les stocks à chaque ligne (N+1)

```js
// ❌ Lourd : 1 requête par ligne
for (const row of data) {
  const stocks = await getList('stock_availables', 'stock_available', `&filter[id_product]=${id}`)
}
```

✅ Charger une fois au début, utiliser un Map :

```js
const stockTracker = new Map()
// chargement initial une fois
```

---

## 19. Cheatsheet "Pour X, modifier Y"

**Mots-clés** : cheatsheet, mémo, rapide

```
╔══════════════════════════════════════════════════════════════╗
║         POUR FAIRE X, MODIFIER Y                             ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Ajouter une validation                                      ║
║    → importService.js : helpers assertX en haut              ║
║    → puis appel dans la boucle for                           ║
║                                                              ║
║  Nouvelle colonne CSV                                        ║
║    → importService.js : validateHeaders(headers, [...])      ║
║    → puis row.nouveauChamp dans la boucle                    ║
║                                                              ║
║  Action conditionnelle                                       ║
║    → importService.js : if (row.X === Y) { ... }             ║
║                                                              ║
║  Nouvel état de commande                                     ║
║    → importService.js createCartAndOrder : ajout cas dans e  ║
║    → orderstateapi : ajout transitionToXxx                   ║
║                                                              ║
║  Détecter doublons                                           ║
║    → importService.js : Set en début de importFileX          ║
║                                                              ║
║  Skip lignes                                                 ║
║    → importService.js : continue dans la boucle              ║
║                                                              ║
║  Nouveau format de date                                      ║
║    → importService.js : parseDate (regex à étendre)          ║
║                                                              ║
║  Stock check                                                 ║
║    → importService.js importFile3 : stockTracker + assert    ║
║                                                              ║
║  Nouveau champ produit                                       ║
║    → importService.js : createProduct (XMLBuilder)           ║
║                                                              ║
║  Nouveau fichier CSV                                         ║
║    → ImportView.vue : <FileInput> + ref file4                ║
║    → importService.js : importFile4 + parseCSV               ║
║                                                              ║
║  Progress label custom                                       ║
║    → importService.js : onProgress?.({ ..., label: '...' })  ║
║                                                              ║
║  Gérer erreur PrestaShop                                     ║
║    → importService.js : postXml (regex CDATA déjà là)        ║
║                                                              ║
║  Retry sur erreur réseau                                     ║
║    → importService.js : postXml + boucle for attempts        ║
║                                                              ║
║  BOM UTF-8 / encoding                                        ║
║    → importService.js : parseCSV en haut (charCodeAt 0xFEFF) ║
║    → OU ImportView.vue : reader.readAsText(file, encoding)   ║
║                                                              ║
║  Logging                                                     ║
║    → importService.js : console.log dans la boucle           ║
║                                                              ║
║  Transition stock custom                                     ║
║    → orderstateapi : nouvelle méthode transitionToXxx        ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches à... | Mot-clé à taper |
|------------------|-----------------|
| Comprendre le flow | `Vue d'ensemble` |
| Localiser les fichiers | `Carte des fichiers` |
| Ajouter "montant > 0" | `Ajouter une validation` |
| Nouvelle colonne CSV | `Ajouter / renommer une colonne` |
| Comportement selon valeur | `action conditionnelle` |
| Nouveau état commande | `nouvel état de commande` |
| Doublons | `lignes en doublon` |
| Ignorer des lignes | `Skip` |
| Format de date | `format ou la validation de date` |
| Stock check | `vérification de stock` |
| Champ produit XMLBuilder | `mapping CSV → API` |
| 4e fichier CSV | `nouveau fichier CSV` |
| Progress label | `progress bar / les messages` |
| Erreur PrestaShop CDATA | `exception PrestaShop` |
| Nouvelle transition stock | `transition d'état custom` |
| Logs / debug | `Logging` |
| BOM / encoding | `Cas particuliers` |
| Erreurs à éviter | `Anti-patterns` |
| Mémo rapide | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |
| Pagination | Voir [`listePagination.md`](listePagination.md) |
| POST / PUT | Voir [`PutPost.md`](PutPost.md) |
| Variables / types | Voir [`variable.md`](variable.md) |
| Filtres | Voir [`filter.md`](filter.md) |
