# 📥 Documentation complète : Processus d'import (Vue + Service)

> Tout le flux d'import expliqué bloc par bloc, du composant Vue [ImportView.vue](../NewApp/src/views/BackOffice/ImportView.vue) jusqu'au service [importService.js](../NewApp/src/services/importService.js). Chaque bloc de code est annoté avec son rôle, ses fichiers et ses lignes.

> **Mots-clés** : import, CSV, ZIP, ImportView, importService, parseCSV, validateHeaders, parseDate, getOrCreate, cache, stock_delta, order_state_change, webservice PrestaShop

---

## 📑 Index

- [1. Architecture globale](#1-architecture-globale)
- [2. Vue d'ensemble du flux d'import](#2-vue-densemble-du-flux-dimport)
- [3. ImportView.vue — Structure UI](#3-importviewvue--structure-ui)
- [4. ImportView.vue — État réactif et computed](#4-importviewvue--état-réactif-et-computed)
- [5. ImportView.vue — Lecture des fichiers (`readText`)](#5-importviewvue--lecture-des-fichiers-readtext)
- [6. ImportView.vue — Orchestration (`startImport`)](#6-importviewvue--orchestration-startimport)
- [7. ImportView.vue — Composant inline `FileInput`](#7-importviewvue--composant-inline-fileinput)
- [8. importService.js — Setup global](#8-importservicejs--setup-global)
- [9. importService.js — Helper `parseCSV`](#9-importservicejs--helper-parsecsv)
- [10. importService.js — Validation (`validateHeaders`, `parseDate`, `assertPositive`)](#10-importservicejs--validation-validateheaders-parsedate-assertpositive)
- [11. importService.js — Parsers de format (`parseFrenchNumber`, `parseAchat`)](#11-importservicejs--parsers-de-format-parsefrenchnumber-parseachat)
- [12. importService.js — Couche HTTP (`postXml`, `getList`)](#12-importservicejs--couche-http-postxml-getlist)
- [13. importService.js — Cache anti-doublons](#13-importservicejs--cache-anti-doublons)
- [14. importService.js — `getOrCreate` (catégories, taxes, options, valeurs)](#14-importservicejs--getorcreate-catégories-taxes-options-valeurs)
- [15. importService.js — Création produits & combinaisons](#15-importservicejs--création-produits--combinaisons)
- [16. importService.js — `setStock` via `/api/stock_delta`](#16-importservicejs--setstock-via-apistock_delta)
- [17. importService.js — Clients, mots de passe, adresses](#17-importservicejs--clients-mots-de-passe-adresses)
- [18. importService.js — `createCartAndOrder` (le cœur du processus)](#18-importservicejs--createcartandorder-le-cœur-du-processus)
- [19. importService.js — `importFile1` (Produits)](#19-importservicejs--importfile1-produits)
- [20. importService.js — `importFile2` (Combinaisons + stock)](#20-importservicejs--importfile2-combinaisons--stock)
- [21. importService.js — `importFile3` (Clients, paniers, commandes)](#21-importservicejs--importfile3-clients-paniers-commandes)
- [22. importService.js — `importImages` (ZIP → /api/images)](#22-importservicejs--importimages-zip--apiimages)
- [23. Récap des fichiers concernés](#23-récap-des-fichiers-concernés)

---

## 1. Architecture globale

**Mots-clés** : architecture, couches, séparation des responsabilités

```
┌──────────────────────────────────────────────────────────────┐
│  ImportView.vue (UI)                                         │
│   • 4 inputs fichiers (file1, file2, file3, zipFile)         │
│   • Bouton "Lancer l'import"                                 │
│   • Barre de progression + tableau de résultats              │
│   • Lit les fichiers en texte → délègue au service           │
└──────────────────┬───────────────────────────────────────────┘
                   │ readText() + onProgress()
                   ▼
┌──────────────────────────────────────────────────────────────┐
│  importService.js (logique métier)                           │
│   • parseCSV / validateHeaders / parseDate                   │
│   • Cache anti-doublons en mémoire                           │
│   • getOrCreate* : catégorie, taxe, option, client, adresse  │
│   • createProduct / createCombination / setStock             │
│   • createCartAndOrder (panier + commande + transitions)     │
└──────────────────┬───────────────────────────────────────────┘
                   │ axios + XML (XMLParser / XMLBuilder)
                   ▼
┌──────────────────────────────────────────────────────────────┐
│  PrestaShop Webservice + modules custom                      │
│   • /api/categories, /api/products, /api/combinations …      │
│   • /api/stock_delta   (module stockdeltaapi)                │
│   • /api/order_state_change  (module orderstateapi)          │
│   • /helpers/hash_password.php  (helper PHP, bcrypt)         │
└──────────────────────────────────────────────────────────────┘
```

**Séparation des responsabilités** :
- **Vue** = pilote (clics, progression, affichage erreurs)
- **Service** = règles métier + appels HTTP + cache
- **PrestaShop** = persistance + logique cœur (états commande, prix, etc.)

---

## 2. Vue d'ensemble du flux d'import

**Mots-clés** : ordre d'import, dépendances, séquentiel

L'ordre est **strict** et imposé par les dépendances de données :

| Étape | Fichier | Crée | Dépend de |
|-------|---------|------|-----------|
| 1 | `Fichier 1 — Produits` | Catégories, Taxes, Tax Rule Groups, Produits | — |
| 2 | `Fichier 2 — Combinaisons` | Options, Valeurs, Combinaisons, **Stock initial** | Fichier 1 (produits) |
| 3 | `Fichier 3 — Commandes` | Clients, Adresses, Paniers, Commandes | Fichiers 1 & 2 (produits + combinaisons + stock) |
| 4 | `Images.zip` | Images associées aux produits | Fichier 1 (produits) |

> ⚠️ Si l'utilisateur lance un import partiel (uniquement fichier 2 par exemple), le service **rechargera** les produits depuis l'API pour reconstruire le cache. C'est volontaire et indispensable.

---

## 3. ImportView.vue — Structure UI

**Mots-clés** : template, FileInput, info-box, progress-bar, results-table

Référence : [ImportView.vue:1-116](../NewApp/src/views/BackOffice/ImportView.vue#L1-L116).

### Bloc info-box ([ImportView.vue:8-16](../NewApp/src/views/BackOffice/ImportView.vue#L8-L16))

```vue
<div class="info-box">
  <h3>📋 Ordre d'import (respecter)</h3>
  <ol>
    <li><strong>Fichier 1</strong> — Catégories, produits, taxes</li>
    <li><strong>Fichier 2</strong> — Options, combinaisons, stock initial</li>
    <li><strong>Fichier 3</strong> — Clients, adresses, paniers, commandes</li>
    <li><strong>Images.zip</strong> — Images des produits (nommées par référence)</li>
  </ol>
</div>
```

**Pourquoi** : guide l'utilisateur sur l'ordre de dépendance. Le composant ne **bloque pas** l'ordre côté client (l'utilisateur peut cocher seulement fichier 3), mais l'info-box rappelle la règle.

### Bloc inputs ([ImportView.vue:18-44](../NewApp/src/views/BackOffice/ImportView.vue#L18-L44))

```vue
<div class="files-grid">
  <FileInput label="Fichier 1 — Produits"      accept=".csv" :file="file1" @file-selected="file1 = $event" />
  <FileInput label="Fichier 2 — Combinaisons"  accept=".csv" :file="file2" @file-selected="file2 = $event" />
  <FileInput label="Fichier 3 — Commandes"     accept=".csv" :file="file3" @file-selected="file3 = $event" />
  <FileInput label="Images.zip"                accept=".zip" :file="zipFile" @file-selected="zipFile = $event" />
</div>
```

**Pourquoi 4 `FileInput` séparés** : permet d'importer **uniquement les fichiers nécessaires** (par exemple ré-importer juste les images, ou juste les commandes). Chaque ref locale est indépendante.

### Checkbox "Ne pas importer les images" ([ImportView.vue:46-56](../NewApp/src/views/BackOffice/ImportView.vue#L46-L56))

```vue
<div class="form-group">
  <label class="checkbox-label">
    <input v-model="form.terms" type="checkbox" @change="validateField('terms')" />
    Ne pas importer d'images
  </label>
</div>
```

**Logique** : si la case est cochée (`form.terms === true`), même si le ZIP est fourni il sera **ignoré** (voir [ImportView.vue:187](../NewApp/src/views/BackOffice/ImportView.vue#L187)). Utile pour des tests rapides sans uploader les Mo d'images.

### Bouton d'import ([ImportView.vue:58-62](../NewApp/src/views/BackOffice/ImportView.vue#L58-L62))

```vue
<button class="btn-import" :disabled="!canImport || running" @click="startImport">
  {{ running ? '⏳ Import en cours...' : '🚀 Lancer l\'import' }}
</button>
```

**Pourquoi `:disabled`** : empêche les doubles clics pendant un import en cours et désactive le bouton si **aucun fichier** n'est sélectionné (`canImport` est `false`).

### Barre de progression ([ImportView.vue:64-73](../NewApp/src/views/BackOffice/ImportView.vue#L64-L73))

```vue
<div v-if="running || done" class="progress-section">
  <h3>{{ running ? '⏳ ' + currentStep : '✅ Import terminé' }}</h3>
  <div v-if="running && currentLabel" class="current-label">
    Élément en cours : <code>{{ currentLabel }}</code>
  </div>
  <div v-if="running && currentTotal > 0" class="progress-bar">
    <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
    <span class="progress-text">{{ currentCount }} / {{ currentTotal }}</span>
  </div>
</div>
```

**Mécanique** : les variables `currentStep`, `currentLabel`, `currentTotal`, `currentCount` sont **mises à jour par le service** via le callback `onProgress` (voir bloc suivant). Le service appelle `onProgress` à chaque ligne traitée → UI réactive.

### Tableau de résultats + erreurs ([ImportView.vue:75-104](../NewApp/src/views/BackOffice/ImportView.vue#L75-L104))

```vue
<table class="results-table">
  <thead>
    <tr><th>Étape</th><th class="center">✅ Succès</th><th class="center">❌ Erreurs</th></tr>
  </thead>
  <tbody>
    <tr v-for="r in results" :key="r.step">
      <td><strong>{{ r.step }}</strong></td>
      <td class="center success">{{ r.success }}</td>
      <td class="center" :class="{ error: r.errors.length > 0 }">{{ r.errors.length }}</td>
    </tr>
  </tbody>
</table>

<div v-for="r in results" :key="'err-' + r.step">
  <details v-if="r.errors.length > 0" class="errors-details">
    <summary>Erreurs de "{{ r.step }}" ({{ r.errors.length }})</summary>
    <ul>
      <li v-for="(err, i) in r.errors" :key="i">
        <code>{{ err.ref || err.email || err.filename || ('Ligne ' + err.line) }}</code> : {{ err.error }}
      </li>
    </ul>
  </details>
</div>
```

**Pourquoi `<details>`** : les erreurs sont **collapsibles** (par défaut fermées). On affiche le résumé "Erreurs de X (N)" et l'utilisateur déplie pour lire le détail. Évite de polluer l'écran si 50+ erreurs.

### Erreur fatale ([ImportView.vue:106-114](../NewApp/src/views/BackOffice/ImportView.vue#L106-L114))

```vue
<div v-if="globalError" class="error-fatal">
  <div class="error-fatal-title">❌ Import annulé — une erreur a été détectée</div>
  <div class="error-fatal-lines">
    <div v-for="(line, i) in globalError.split('\n')" :key="i" :class="i === 0 ? 'error-location' : 'error-cause'">
      {{ line }}
    </div>
  </div>
  <div class="error-fatal-hint">Corrigez le fichier CSV puis relancez l'import depuis le début.</div>
</div>
```

**Comportement** : le service **throw** dès la première erreur grave (date invalide, montant ≤ 0, produit manquant…). Le `catch` du `startImport` capture ce throw → `globalError` est rempli et **tous les résultats partiels sont effacés** (`results.value = []`). C'est volontairement strict pour éviter une base partiellement importée.

---

## 4. ImportView.vue — État réactif et computed

**Mots-clés** : ref, computed, état local

Référence : [ImportView.vue:118-148](../NewApp/src/views/BackOffice/ImportView.vue#L118-L148).

```js
import { ref, computed, h } from 'vue'
import { ImportService } from '../../services/importService'

const file1 = ref(null)
const file2 = ref(null)
const file3 = ref(null)
const zipFile = ref(null)

const running = ref(false)
const done = ref(false)
const globalError = ref('')
const results = ref([])

const currentStep = ref('')
const currentLabel = ref('')
const currentTotal = ref(0)
const currentCount = ref(0)

const form = ref({ terms: false })

const canImport = computed(() => file1.value || file2.value || file3.value || zipFile.value)

const progressPercent = computed(() => {
  if (currentTotal.value === 0) return 0
  return (currentCount.value / currentTotal.value) * 100
})
```

| Variable | Rôle |
|----------|------|
| `file1`-`file3`, `zipFile` | Stockent les `File` objects sélectionnés par l'utilisateur |
| `running` | Booléen vrai pendant l'import (désactive le bouton, affiche la barre) |
| `done` | Booléen vrai quand l'import s'est terminé (affiche "✅ Import terminé") |
| `globalError` | Message d'erreur fatale (interrompt et masque les résultats) |
| `results` | Tableau de `{ step, success, errors }` (un par fichier importé) |
| `currentStep` / `currentLabel` / `currentTotal` / `currentCount` | Mis à jour par `onProgress`, alimentent la barre |
| `form.terms` | Case "Ne pas importer d'images" |
| `canImport` | **Computed** : vrai si au moins un fichier est sélectionné |
| `progressPercent` | **Computed** : pourcentage 0-100 pour la largeur CSS de la barre |

---

## 5. ImportView.vue — Lecture des fichiers (`readText`)

**Mots-clés** : FileReader, Promise, async

Référence : [ImportView.vue:157-162](../NewApp/src/views/BackOffice/ImportView.vue#L157-L162).

```js
const readText = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = reject
  reader.readAsText(file)
})
```

**Pourquoi cette fonction** : `FileReader` est une API DOM **événementielle** (pas une Promise). On l'enveloppe dans une Promise pour pouvoir faire `await readText(file)` proprement dans `startImport`.

> Note : `readAsText` lit le fichier en UTF-8 par défaut — donc on attend des CSV encodés en UTF-8 (les accents `é`, `è`, `à` doivent être correctement encodés sinon l'import garde des `?` ou `�`).

---

## 6. ImportView.vue — Orchestration (`startImport`)

**Mots-clés** : startImport, séquentiel, try/catch, resetCache

Référence : [ImportView.vue:164-199](../NewApp/src/views/BackOffice/ImportView.vue#L164-L199).

```js
const startImport = async () => {
  running.value = true
  done.value = false
  globalError.value = ''
  results.value = []
  ImportService.resetCache()

  try {
    if (file1.value) {
      const text = await readText(file1.value)
      const r = await ImportService.importFile1(text, onProgress)
      results.value.push({ step: 'Fichier 1 — Produits', ...r })
    }
    if (file2.value) {
      const text = await readText(file2.value)
      const r = await ImportService.importFile2(text, onProgress)
      results.value.push({ step: 'Fichier 2 — Combinaisons', ...r })
    }
    if (file3.value) {
      const text = await readText(file3.value)
      const r = await ImportService.importFile3(text, onProgress)
      results.value.push({ step: 'Fichier 3 — Commandes', ...r })
    }
    if (zipFile.value && form.value.terms == false) {
      const r = await ImportService.importImages(zipFile.value, onProgress)
      results.value.push({ step: 'Images', ...r })
    }
    done.value = true
  } catch (err) {
    console.error('❌ Import interrompu :', err)
    globalError.value = err.message
    results.value = [] // annuler l'affichage des résultats partiels
  } finally {
    running.value = false
  }
}
```

**Phases** :
1. **Reset** (`running=true`, `done=false`, vider erreurs/résultats, appeler `ImportService.resetCache()` pour repartir d'un état propre).
2. **Exécution séquentielle** : chaque `await` garantit que le fichier précédent est **terminé** avant de commencer le suivant. C'est crucial parce que Fichier 2 dépend de produits créés en Fichier 1, etc.
3. **Conditional sur le ZIP** : `file.value && form.value.terms == false` — si la checkbox est cochée, on saute les images même si le ZIP est fourni.
4. **try/catch global** : si un import throw (ex. date invalide en Fichier 3), tout s'arrête. `globalError.value = err.message` affiche le détail à l'écran. **`results.value = []`** efface volontairement les résultats partiels — on ne veut pas afficher "Fichier 1 OK : 50 produits" alors qu'on a tout interrompu.
5. **`finally`** : `running = false` quoi qu'il arrive (sinon le bouton resterait gris pour toujours).

> ⚠️ **Pourquoi pas en parallèle ?** Les fichiers 1 → 2 → 3 sont **séquentiellement dépendants** (produits avant combinaisons avant commandes). Le parallélisme casserait l'intégrité.

### Le callback `onProgress` ([ImportView.vue:150-155](../NewApp/src/views/BackOffice/ImportView.vue#L150-L155))

```js
const onProgress = (p) => {
  currentStep.value = p.step
  currentLabel.value = p.label || ''
  currentTotal.value = p.total
  currentCount.value = p.current
}
```

**Mécanique** : le service appelle ce callback à chaque ligne traitée (`onProgress?.({ step, current, total, label })`). Comme on assigne à des `ref`, Vue ré-affiche immédiatement la barre de progression. Pas de polling, pas de setInterval — c'est event-driven.

---

## 7. ImportView.vue — Composant inline `FileInput`

**Mots-clés** : composant inline, h() (render function), props

Référence : [ImportView.vue:202-213](../NewApp/src/views/BackOffice/ImportView.vue#L202-L213).

```js
const FileInput = {
  props: ['label', 'accept', 'file'],
  emits: ['file-selected'],
  setup(props, { emit }) {
    const onChange = (e) => emit('file-selected', e.target.files[0])
    return () => h('div', { class: 'file-input' }, [
      h('label', { class: 'file-label' }, props.label),
      h('input', { type: 'file', accept: props.accept, onChange }),
      props.file ? h('span', { class: 'file-name' }, '✅ ' + props.file.name) : null,
    ])
  },
}
```

**Pourquoi un composant inline et pas un fichier séparé** :
- Petit (8 lignes) → pas la peine de créer `FileInput.vue`.
- 100% local à `ImportView` → pas besoin d'être réutilisé ailleurs.
- Utilise `h()` (render function) car c'est plus rapide à écrire qu'un template séparé pour ce niveau de simplicité.

**Pattern d'événement** : `emit('file-selected', e.target.files[0])` → parent capte avec `@file-selected="file1 = $event"`. C'est l'inverse de v-model pour rester explicite.

---

## 8. importService.js — Setup global

**Mots-clés** : XMLParser, XMLBuilder, constantes, axios

Référence : [importService.js:1-10](../NewApp/src/services/importService.js#L1-L10).

```js
import axios from '../config/axios'
import { XMLParser, XMLBuilder } from 'fast-xml-parser'
import { phpHelperUrl } from '../config/api'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })
const builder = new XMLBuilder({ ignoreAttributes: false, attributeNamePrefix: '@_', format: true })

const ID_LANG = 1
const ID_SHOP = 1
const ID_COUNTRY = 8 // France par défaut — adapter si besoin
```

| Élément | Rôle |
|---------|------|
| `axios` | Instance configurée avec **Basic Auth** automatique (voir [axios.js:1-21](../NewApp/src/config/axios.js#L1-L21)) |
| `XMLParser` / `XMLBuilder` | PrestaShop **n'accepte que du XML** sur la plupart des endpoints |
| `ignoreAttributes: false` | Garde les `xlink:href`, `@_id="1"` (pour les langues), etc. |
| `attributeNamePrefix: '@_'` | Convention : `<name id="1">` devient `{ '@_id': '1' }` |
| `format: true` | Indente le XML généré (utile en debug) |
| `ID_LANG = 1` | Français par défaut |
| `ID_SHOP = 1` | Boutique par défaut |
| `ID_COUNTRY = 8` | France ; à adapter selon le contexte |
| `phpHelperUrl` | URL des helpers PHP custom (ex. `hash_password.php`), définie dans [api.js:11](../NewApp/src/config/api.js#L11) |

---

## 9. importService.js — Helper `parseCSV`

**Mots-clés** : CSV, RFC 4180, guillemets, échappement

Référence : [importService.js:16-44](../NewApp/src/services/importService.js#L16-L44).

```js
function parseCSV(text) {
  const rows = []
  let row = []
  let field = ''
  let inQuotes = false

  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    if (inQuotes) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++ }
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
  if (field.length > 0 || row.length > 0) { row.push(field); rows.push(row) }

  const headers = (rows.shift() || []).map(h => h.trim())
  const data = rows
    .filter(r => r.some(v => v && v.trim()))
    .map(r => Object.fromEntries(headers.map((h, i) => [h, (r[i] ?? '').trim()])))

  return { headers, data }
}
```

**Pourquoi un parser maison plutôt que `papaparse` ou autre** : pour rester **0 dépendance** sur cette fonction critique et avoir un contrôle total sur :
- Le **séparateur virgule** uniquement (les fichiers du projet ne mélangent pas `;` et `,`).
- L'**échappement RFC 4180** : `"foo, bar"` est un seul champ ; `"foo""bar"` est `foo"bar` (les `""` au sein de guillemets représentent un guillemet littéral).
- **Skip `\r`** pour gérer les fichiers Windows (`\r\n`) sans casser.

**Sortie** : un objet `{ headers, data }` où `data` est un tableau de **dictionnaires** clés=colonnes du header. La ligne `row.some(v => v && v.trim())` **filtre les lignes complètement vides** (parfois Excel ajoute des lignes vides en fin).

---

## 10. importService.js — Validation (`validateHeaders`, `parseDate`, `assertPositive`)

**Mots-clés** : validation, throw, format strict

### `validateHeaders` ([importService.js:49-55](../NewApp/src/services/importService.js#L49-L55))

```js
function validateHeaders(actual, expected) {
  const missing = expected.filter(col => !actual.includes(col), console.log("TT :"))
  console.log("TEST VALIDATE HEADERS")
  if (missing.length > 0) {
    throw new Error(`Nom(s) de colonne non conforme(s). Manquant : ${missing.join(', ')}. Attendu : ${expected.join(', ')}. Reçu : ${actual.join(', ')}`)
  }
}
```

**Logique** : compare les en-têtes du CSV (`actual`) avec les en-têtes attendus (`expected`). Si **un seul** manque, throw avec un message complet listant manquants / attendus / reçus.

**Pourquoi un throw plutôt qu'un retour bool** : permet à l'appelant de faire un `try { … } catch` global et d'afficher l'erreur dans `globalError`.

> Le `console.log` deuxième argument du `filter` est sans effet (le predicate n'utilise pas `thisArg`) — c'est un trace de debug laissée sciemment ou non.

### `parseDate` ([importService.js:64-85](../NewApp/src/services/importService.js#L64-L85))

```js
function parseDate(s) {
  if (!s) return new Date().toISOString().split('T')[0]

  // Format strict DD/MM/YYYY
  const match = String(s).trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!match) {
    throw new Error(`Format de date invalide : "${s}" — attendu DD/MM/YYYY`)
  }

  const [, d, m, y] = match
  const day = parseInt(d, 10)
  const month = parseInt(m, 10)
  const year = parseInt(y, 10)

  // Vérifier que la date est réelle (ex: 31/02/2026 doit être refusé)
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    throw new Error(`Date inexistante : "${s}"`)
  }

  return `${y}-${m}-${d}`
}
```

**Deux niveaux de validation** :
1. **Format** : regex `^(\d{2})\/(\d{2})\/(\d{4})$` — refuse `1/1/26`, `2026-01-01`, `01-01-2026`.
2. **Réalité** : `new Date(2026, 1, 31)` JavaScript va **silencieusement** rouler à `2026-03-03`. On vérifie donc que les composants ressortent **identiques** → sinon `31/02/2026` est rejeté.

**Sortie** : ISO `YYYY-MM-DD` (format attendu par MySQL).

### `assertPositive` / `assertNonNegative` ([importService.js:90-103](../NewApp/src/services/importService.js#L90-L103))

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

**Pourquoi `isFinite`** : protège contre `NaN` (si `parseFloat` a échoué) et `Infinity`. Avec uniquement `value <= 0`, `NaN` passerait (`NaN <= 0` est `false`).

---

## 11. importService.js — Parsers de format (`parseFrenchNumber`, `parseAchat`)

**Mots-clés** : format français, virgule décimale, regex, ligne d'achat

### `parseFrenchNumber` et `parseFrenchPercent` ([importService.js:57-58](../NewApp/src/services/importService.js#L57-L58))

```js
const parseFrenchNumber = (s) => parseFloat(String(s ?? '').replace(',', '.')) || 0
const parseFrenchPercent = (s) => parseFloat(String(s ?? '').replace(',', '.').replace('%', '')) || 0
```

**Logique** :
- En CSV français, les nombres décimaux utilisent la **virgule** (`12,5`). JavaScript `parseFloat` n'accepte que le point → on remplace.
- `parseFrenchPercent` enlève aussi le `%` (utile si la colonne `Taxe` contient `20%`).
- Le `|| 0` retourne 0 si la valeur est non-parseable → mais c'est ensuite **revérifié** par `assertPositive` qui throw si ≤ 0.

### `parseAchat` ([importService.js:105-114](../NewApp/src/services/importService.js#L105-L114))

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

**Format attendu dans la colonne `achat` du Fichier 3** :
```
("REF-PRODUIT";QTE;"VARIANTE") ("REF-PRODUIT-2";QTE;"")
```

Exemple : `("TSHIRT-001";2;"Rouge") ("MUG-PS";1;"")`

**Sortie** : `[{ ref: 'TSHIRT-001', qty: 2, value: 'Rouge' }, { ref: 'MUG-PS', qty: 1, value: '' }]`

**Regex décomposée** :
- `\(` et `\)` : parenthèses littérales (groupent un achat)
- `"([^"]+)"` : référence produit entre guillemets (au moins un caractère)
- `;(\d+);` : quantité entière entre `;`
- `"([^"]*)"` : variante entre guillemets (peut être vide → `*` et non `+`)
- Flag `/g` : multiple matches

---

## 12. importService.js — Couche HTTP (`postXml`, `getList`)

**Mots-clés** : POST XML, GET liste, parsing, gestion d'erreurs

### `postXml` ([importService.js:116-142](../NewApp/src/services/importService.js#L116-L142))

```js
async function postXml(endpoint, xmlBody) {
  try {
    const res = await axios.post(endpoint, xmlBody, { headers: { 'Content-Type': 'application/xml' } })
    const parsed = parser.parse(res.data)
    
    // Intercepter les erreurs silencieuses de PrestaShop
    if (parsed && parsed.prestashop && parsed.prestashop.errors) {
      const psError = parsed.prestashop.errors.error;
      const errMsg = psError.message ? psError.message : JSON.stringify(psError);
      throw new Error(`Refusé par PrestaShop : ${errMsg}`);
    }
    
    return parsed
  } catch (err) {
    // Extraire le message d'erreur de la réponse PrestaShop
    if (err.response?.data) {
      const match = String(err.response.data).match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/s)
      if (match) {
        console.error(`📝 PrestaShop error sur ${endpoint}:`, match[1])
        err.message = `${endpoint}: ${match[1]}`
      } else {
        console.error(`📝 Réponse brute sur ${endpoint}:`, err.response.data)
      }
    }
    throw err
  }
}
```

**Trois niveaux de gestion d'erreur** :
1. **HTTP 4xx/5xx** : axios throw → on capture `err.response.data` et on extrait le `<message>` du XML PrestaShop pour mettre un message lisible.
2. **HTTP 200 mais XML d'erreur** (cas vicieux PrestaShop) : la réponse contient `<prestashop><errors><error>…</error></errors></prestashop>`. On le détecte et on convertit en throw.
3. **Réponse OK** : retourne le XML parsé.

**Pourquoi le regex `<message><![CDATA[…]]>`** : PrestaShop encode les messages d'erreur en CDATA pour éviter les caractères spéciaux. La regex avec flag `/s` (dotall) capture même les retours à la ligne.

### `getList` ([importService.js:144-155](../NewApp/src/services/importService.js#L144-L155))

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
```

**Pattern récurrent dans le projet** (déjà vu dans [StockService.js:25-29](../NewApp/src/services/StockService.js#L25-L29)). Trois subtilités :
- `display=full` : sans ça, l'API ne renvoie que l'`id` et le `xlink:href`.
- `if (!Array.isArray(items)) items = [items]` : XML parsé donne un objet pour 1 élément, un tableau pour N → on uniformise.
- `404 → []` : si la ressource est vide (ex. aucun produit), PrestaShop renvoie un 404. On le traite comme un tableau vide, **pas comme une erreur**.

### Helpers `txt` et `num` ([importService.js:157-158](../NewApp/src/services/importService.js#L157-L158))

```js
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0
```

**Pourquoi** : `XMLParser` produit `{ "#text": "12.50", "@_xlink:href": "..." }` quand un élément a des attributs. Pour récupérer juste la valeur textuelle, on prend `#text` (et fallback sur la valeur brute si pas d'attribut).

---

## 13. importService.js — Cache anti-doublons

**Mots-clés** : cache, idempotence, Map

Référence : [importService.js:164-178](../NewApp/src/services/importService.js#L164-L178).

```js
const cache = {
  categories: new Map(),
  taxes: new Map(),
  taxRuleGroups: new Map(),
  products: new Map(),         // ref → {id, priceHT, priceTTC, taxRate}
  options: new Map(),
  optionValues: new Map(),
  combinations: new Map(),     // `${productRef}|${valueName}` → id
  customers: new Map(),
  addresses: new Map(),
}

function resetCache() {
  Object.values(cache).forEach(m => m.clear())
}
```

**Rôle** : si le CSV contient 50 produits dans la catégorie "Vêtements", on ne veut pas faire **50 GET** sur `/api/categories` ni **50 POST** pour vérifier qu'elle existe. On retient l'`id` de la catégorie au premier passage et on le réutilise pour les 49 suivants.

**Clé / Valeur typiques** :
| Cache | Clé | Valeur |
|-------|-----|--------|
| `categories` | nom de catégorie | id de la catégorie |
| `taxes` | taux (string `"20.00"`) | id de la taxe |
| `taxRuleGroups` | taux | id du groupe |
| `products` | référence | objet `{ id, priceHT, priceTTC, taxRate }` |
| `options` | nom (ex. "Couleur") | id |
| `optionValues` | `"${idOption}|${name}"` (ex. `"1|Rouge"`) | id |
| `combinations` | `"${productRef}|${valueName}"` | id de la combinaison |
| `customers` | email | id |
| `addresses` | `"${idCustomer}|${alias}"` | id |

**`resetCache()`** est appelé au début de `startImport` côté Vue ([ImportView.vue:169](../NewApp/src/views/BackOffice/ImportView.vue#L169)) pour repartir d'un état propre à chaque lancement.

---

## 14. importService.js — `getOrCreate` (catégories, taxes, options, valeurs)

**Mots-clés** : idempotent, get-or-create, recherche existant

### `getOrCreateCategory` ([importService.js:184-213](../NewApp/src/services/importService.js#L184-L213))

```js
async function getOrCreateCategory(name) {
  if (cache.categories.has(name)) return cache.categories.get(name)

  // Chercher une catégorie active existante avec ce nom
  const existing = await getList('categories', 'category')
  for (const cat of existing) {
    const catName = txt(cat.name?.language) || txt(cat.name)
    if (catName === name && parseInt(txt(cat.active)) === 1) {
      const id = txt(cat.id)
      cache.categories.set(name, id)
      return id
    }
  }

  const xml = builder.build({
    prestashop: {
      category: {
        active: 1,
        id_parent: 2,
        name: { language: { '@_id': ID_LANG, '#text': name } },
        link_rewrite: { language: { '@_id': ID_LANG, '#text': name.toLowerCase().replace(/\s+/g, '-') } },
      },
    },
  })

  const result = await postXml('/api/categories', xml)
  const id = txt(result.prestashop.category.id)
  cache.categories.set(name, id)
  return id
}
```

**Algorithme `getOrCreate` standard** (utilisé pour toutes les ressources) :
1. **Cache hit ?** → retourner immédiatement.
2. **GET tous les existants** et chercher par nom.
3. Si trouvé **et actif** (`active === 1`) → cacher + retourner.
4. Sinon → **POST** une nouvelle ressource avec un XML minimal mais valide.
5. Cacher l'id retourné.

**Pourquoi `active === 1`** : PrestaShop fait du **soft-delete** sur certaines ressources (la ligne reste en base avec `active=0`). On veut **ignorer les soft-deleted** et créer une nouvelle entrée.

**Sur `id_parent: 2`** : 2 = "Accueil" (root), la racine de l'arbo PrestaShop. Toutes les catégories importées sont des enfants directs de la racine.

**Sur `link_rewrite`** : le slug URL, généré en kebab-case à partir du nom (`"Vêtements et accessoires"` → `vetements-et-accessoires`).

### `getOrCreateTax` ([importService.js:215-245](../NewApp/src/services/importService.js#L215-L245))

```js
async function getOrCreateTax(rate) {
  const key = rate.toFixed(2)
  if (cache.taxes.has(key)) return cache.taxes.get(key)

  const existing = await getList('taxes', 'tax')
  for (const t of existing) {
    const r = num(t.rate)
    const isDeleted = parseInt(txt(t.deleted) || 0) === 1
    if (!isDeleted && Math.abs(r - rate) < 0.001) {
      const id = txt(t.id)
      cache.taxes.set(key, id)
      return id
    }
  }

  const xml = builder.build({
    prestashop: {
      tax: {
        rate: rate.toFixed(3),
        active: 1,
        name: { language: { '@_id': ID_LANG, '#text': `TVA ${rate}%` } },
      },
    },
  })

  const result = await postXml('/api/taxes', xml)
  const id = txt(result.prestashop.tax.id)
  cache.taxes.set(key, id)
  return id
}
```

**Spécificités** :
- **Clé cache** = `rate.toFixed(2)` (string) pour éviter les piéges de précision flottante (`0.2 + 0.1 !== 0.3`).
- **Comparaison `Math.abs(r - rate) < 0.001`** : tolérance flottante (la base peut stocker `20.000` ou `20.0001`).
- **Filtre `isDeleted`** : on **ignore** les taxes soft-deleted dans la BDD pour ne pas réutiliser une taxe orpheline.
- `rate.toFixed(3)` à la création : PrestaShop attend une précision à 3 décimales.

### `getOrCreateTaxRuleGroup` ([importService.js:247-287](../NewApp/src/services/importService.js#L247-L287))

```js
async function getOrCreateTaxRuleGroup(rate, idTax) {
  const key = rate.toFixed(2)
  if (cache.taxRuleGroups.has(key)) return cache.taxRuleGroups.get(key)

  // Chercher un groupe existant qui contient une tax_rule pointant vers idTax
  const rules = await getList('tax_rules', 'tax_rule')
  for (const r of rules) {
    if (parseInt(txt(r.id_tax)) === parseInt(idTax)) {
      const idGroup = txt(r.id_tax_rules_group)
      cache.taxRuleGroups.set(key, idGroup)
      return idGroup
    }
  }

  // Sinon : créer le groupe puis la règle qui le lie à la taxe
  const xmlGroup = builder.build({ prestashop: { tax_rule_group: { name: `Groupe ${rate}%`, active: 1 } } })
  const groupResult = await postXml('/api/tax_rule_groups', xmlGroup)
  const idGroup = txt(groupResult.prestashop.tax_rule_group.id)

  const xmlRule = builder.build({
    prestashop: {
      tax_rule: {
        id_tax_rules_group: idGroup,
        id_country: ID_COUNTRY,
        id_state: 0,
        id_tax: idTax,
        behavior: 0,
      },
    },
  })
  await postXml('/api/tax_rules', xmlRule)

  cache.taxRuleGroups.set(key, idGroup)
  return idGroup
}
```

**Pourquoi un tax_rule_group ?** PrestaShop **ne lie pas une taxe directement à un produit**. La hiérarchie est :
```
Product → id_tax_rules_group → tax_rule (pour un pays/état) → id_tax
```

C'est ce qui permet d'avoir "TVA 20% en France, 19% en Allemagne, 21% en Belgique" sous un seul groupe.

Dans notre cas mono-pays, on crée un groupe trivial avec une seule règle pointant vers la taxe et le pays par défaut (`ID_COUNTRY = 8` = France).

---

## 15. importService.js — Création produits & combinaisons

**Mots-clés** : createProduct, createCombination, XML, associations

### `createProduct` ([importService.js:289-316](../NewApp/src/services/importService.js#L289-L316))

```js
async function createProduct({ name, reference, priceHT, idTaxGroup, idCategory, wholesalePrice, dateAvailable }) {
  const xml = builder.build({
    prestashop: {
      product: {
        name: { language: { '@_id': ID_LANG, '#text': name } },
        link_rewrite: { language: { '@_id': ID_LANG, '#text': reference.toLowerCase().replace(/[^a-z0-9]/g, '-') } },
        reference,
        price: priceHT.toFixed(6),
        wholesale_price: wholesalePrice.toFixed(6),
        id_tax_rules_group: idTaxGroup,
        id_category_default: idCategory,
        active: 1,
        state: 1,
        available_for_order: 1,
        show_price: 1,
        minimal_quantity: 1,
        date_add: new Date().toISOString().replace('T', ' ').substring(0, 19),
        available_date: dateAvailable,
        associations: {
          categories: { category: { id: idCategory } },
        },
      },
    },
  })

  const result = await postXml('/api/products', xml)
  return txt(result.prestashop.product.id)
}
```

**Champs essentiels** :
- `price` : **prix HT** à 6 décimales (PrestaShop calcule le TTC à l'affichage en appliquant le tax_rule_group).
- `wholesale_price` : prix d'achat (utilisé pour le calcul des marges).
- `active=1, state=1, available_for_order=1, show_price=1` : tous les drapeaux pour rendre le produit visible et achetable.
- `minimal_quantity=1` : minimum 1 unité par commande.
- `date_add` : timestamp ISO converti au format MySQL (`'2026-05-26 12:34:56'`).
- `available_date` : date à partir de laquelle le produit est disponible.
- `associations.categories` : on **doit** associer le produit à au moins une catégorie pour qu'il apparaisse au catalogue.

**Pourquoi `link_rewrite` est généré du `reference`** : la référence est unique → on s'assure que le slug URL l'est aussi.

### `createCombination` ([importService.js:369-391](../NewApp/src/services/importService.js#L369-L391))

```js
async function createCombination({ idProduct, priceDeltaHT, optionValueId, reference }) {
  const xml = builder.build({
    prestashop: {
      combination: {
        id_product: idProduct,
        reference: reference || '',
        price: priceDeltaHT.toFixed(6),
        wholesale_price: '0.000000',
        ecotax: '0.000000',
        weight: '0.000000',
        unit_price_impact: '0.000000',
        minimal_quantity: 1,
        default_on: 0,
        associations: {
          product_option_values: { product_option_value: { id: optionValueId } },
        },
      },
    },
  })

  const result = await postXml('/api/combinations', xml)
  return txt(result.prestashop.combination.id)
}
```

**Concept clé : `price` = DELTA, pas prix absolu** :
- Si le produit est à `100€` HT et qu'on crée une combinaison "Rouge" qui doit coûter `120€` → on passe `priceDeltaHT = 20` (pas 120 !).
- PrestaShop additionne `product.price + combination.price` à l'affichage.
- C'est calculé dans `importFile2` : `const deltaTTC = priceComboTTC - product.priceTTC`.

**`default_on: 0`** : aucune combinaison n'est marquée comme défaut. Pour en marquer une → `default_on: 1` sur une seule combinaison du produit.

---

## 16. importService.js — `setStock` via `/api/stock_delta`

**Mots-clés** : stock_delta, set_to, physical_quantity, module custom

Référence : [importService.js:393-401](../NewApp/src/services/importService.js#L393-L401).

```js
async function setStock(idProduct, idCombination, quantity) {
  // Utilise /api/stock_delta en mode 'init' : SET quantity = physical_quantity = N
  // (le webservice standard /api/stock_availables n'expose pas physical_quantity)
  await axios.post('/api/stock_delta', {
    id_product: idProduct,
    id_product_attribute: idCombination,
    set_to: quantity,
  }, { headers: { 'Content-Type': 'application/json' } })
}
```

**Pourquoi `/api/stock_delta` et pas `/api/stock_availables`** :
- Le webservice **standard** `/api/stock_availables` n'expose **que** `quantity` (pas `physical_quantity` ni `reserved_quantity`).
- Quand on initialise un stock à 50, on veut `physical_quantity = 50 ET quantity = 50` (rien réservé encore).
- Le module custom `stockdeltaapi` (voir `modules/stockdeltaapi/`) ajoute un mode `set_to` qui fait un `UPDATE ps_stock_available SET physical_quantity = X, quantity = X` directement en SQL.

**Format JSON** (et non XML) car c'est un module custom qui accepte les deux.

---

## 17. importService.js — Clients, mots de passe, adresses

**Mots-clés** : hashPassword, bcrypt, customer, address

### `hashPassword` ([importService.js:403-412](../NewApp/src/services/importService.js#L403-L412))

```js
async function hashPassword(password) {
  const res = await fetch(phpHelperUrl('hash_password.php'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  })
  if (!res.ok) throw new Error('Erreur hash password')
  const data = await res.json()
  return data.hash
}
```

**Pourquoi un helper PHP externe** :
- PrestaShop attend un **hash bcrypt** dans la colonne `passwd` (champ validé par `isHashedPassword`).
- JavaScript dans le navigateur **ne peut pas générer du bcrypt** facilement (besoin de WebAssembly ou bibliothèque lourde).
- On délègue donc à un script PHP qui utilise la fonction native `password_hash(..., PASSWORD_BCRYPT)`.
- Le helper est posé à la racine du projet (`hash_password.php`), URL définie par `VITE_PHP_HELPERS_URL` dans `.env`.

### `getOrCreateCustomer` ([importService.js:414-446](../NewApp/src/services/importService.js#L414-L446))

```js
async function getOrCreateCustomer({ email, nom, password }) {
  if (cache.customers.has(email)) return cache.customers.get(email)

  // Chercher si le client existe déjà (par email)
  const existing = await getList('customers', 'customer', `&filter[email]=${encodeURIComponent(email)}`)
  if (existing.length > 0) {
    const id = txt(existing[0].id)
    cache.customers.set(email, id)
    return id
  }

  // Hasher le password en bcrypt (PrestaShop exige isHashedPassword)
  const hashedPassword = await hashPassword(password)

  const xml = builder.build({
    prestashop: {
      customer: {
        id_default_group: 3,
        id_lang: ID_LANG,
        passwd: hashedPassword,
        firstname: nom,
        lastname: nom,
        email,
        active: 1,
      },
    },
  })

  const result = await postXml('/api/customers', xml)
  const id = txt(result.prestashop.customer.id)
  cache.customers.set(email, id)
  return id
}
```

**Logique** :
- Cherche par **email** (filtre webservice : `filter[email]=...`).
- Si trouvé → retourne l'id.
- Sinon → hash le mot de passe + POST customer.
- `id_default_group: 3` = groupe "Client" par défaut dans PrestaShop (1=Visitor, 2=Guest, 3=Customer).
- `firstname` et `lastname` sont **tous deux** = `nom` parce que le CSV ne distingue pas. Simplification volontaire.

### `getOrCreateAddress` ([importService.js:448-471](../NewApp/src/services/importService.js#L448-L471))

```js
async function getOrCreateAddress({ idCustomer, nom, alias }) {
  const key = `${idCustomer}|${alias}`
  if (cache.addresses.has(key)) return cache.addresses.get(key)

  const xml = builder.build({
    prestashop: {
      address: {
        id_customer: idCustomer,
        id_country: ID_COUNTRY,
        alias,
        firstname: nom,
        lastname: nom,
        address1: alias,
        city: 'Antananarivo',
        postcode: '101',
      },
    },
  })

  const result = await postXml('/api/addresses', xml)
  const id = txt(result.prestashop.address.id)
  cache.addresses.set(key, id)
  return id
}
```

**Pourquoi `city: 'Antananarivo'` en dur** : le CSV ne contient pas de ville détaillée → valeur de référence du projet (Madagascar). À adapter selon le contexte.

**Clé cache `${idCustomer}|${alias}`** : un même client peut avoir plusieurs adresses (alias "Maison", "Bureau", etc.).

**⚠️ Pas de "check existing"** : contrairement à `getOrCreateCustomer`, on ne cherche pas dans la BDD une adresse existante. Cause : les adresses PrestaShop sont **soft-deletées** très facilement → la lookup serait peu fiable. On préfère créer une nouvelle adresse à chaque import.

---

## 18. importService.js — `createCartAndOrder` (le cœur du processus)

**Mots-clés** : cart, order, order_state_change, transition, secure_key

Référence : [importService.js:473-622](../NewApp/src/services/importService.js#L473-L622). C'est la fonction **la plus complexe** du service.

### Phase 1 — Dispatcher d'état ([importService.js:479-484](../NewApp/src/services/importService.js#L479-L484))

```js
const e = String(etat ?? '').toLowerCase().trim()
let needsOrder = false
let extraState = null
if (e.includes('accept'))   { needsOrder = true; extraState = null }
else if (e.includes('livr')) { needsOrder = true; extraState = 5 }
else if (e.includes('annul')) { needsOrder = true; extraState = 6 }
```

| Valeur `etat` du CSV | needsOrder | extraState | Transitions |
|----------------------|------------|------------|-------------|
| `""` (vide) | `false` | `null` | Crée **uniquement** un panier (pas d'order) |
| `"Paiement accepté"` | `true` | `null` | Crée order état 11 (reserved++, qty--) |
| `"Livré"` | `true` | `5` | État 11 puis transition vers 5 (phys--, reserved--) |
| `"Annulé"` | `true` | `6` | État 11 puis transition vers 6 (libère réservation) |

C'est le dispatcher principal qui orchestre tout le pipeline post-création.

### Phase 2 — Création du panier ([importService.js:485-524](../NewApp/src/services/importService.js#L485-L524))

```js
const cartRows = items.map(it => ({
  id_product: it.idProduct,
  id_product_attribute: it.idCombination || 0,
  quantity: it.qty,
  id_address_delivery: idAddress,
}))

const cartXml = builder.build({
  prestashop: {
    cart: {
      id_currency: 1,
      id_customer: idCustomer,
      id_lang: ID_LANG,
      id_shop: ID_SHOP,
      id_address_delivery: idAddress,
      id_address_invoice: idAddress,
      id_carrier: 2,
      date_add: `${date} 12:00:00`,
      associations: {
        cart_rows: { cart_row: cartRows },
      },
    },
  },
})

const cartResult = await postXml('/api/carts', cartXml)
const cartId = txt(cartResult.prestashop.cart.id)
```

**Points clés** :
- `id_currency: 1` = devise par défaut (€ ou Ariary selon config).
- `id_carrier: 2` = transporteur par défaut.
- `cart_rows` est une association : chaque ligne `{ id_product, id_product_attribute, quantity, id_address_delivery }`.
- `id_product_attribute: 0` si **pas de combinaison** (produit simple).
- `date_add: '${date} 12:00:00'` — on force la date du CSV mais PrestaShop l'écrase souvent par `NOW()` (d'où le PUT en phase 3).

### Phase 3 — Forcer la date du panier (PUT) ([importService.js:513-524](../NewApp/src/services/importService.js#L513-L524))

```js
try {
  const getCartRes = await axios.get(`/api/carts/${cartId}`);
  const cartData = parser.parse(getCartRes.data);
  cartData.prestashop.cart.date_add = `${date} 12:00:00`;
  cartData.prestashop.cart.date_upd = `${date} 12:00:00`;
  delete cartData.prestashop.cart.associations; // IMPORTANT: Évite l'erreur 500
  const updateCartXml = builder.build(cartData);
  await axios.put(`/api/carts/${cartId}`, updateCartXml, { headers: { 'Content-Type': 'application/xml' } });
} catch(e) {
  console.warn(`Impossible de forcer la date du panier ${cartId}`, e.message);
}
```

**Pourquoi un PUT après le POST** : PrestaShop **ignore silencieusement** `date_add` au POST (il met `NOW()`). Le seul moyen de forcer une date passée (utile pour des imports historiques) est un PUT après création.

**`delete cartData.prestashop.cart.associations`** : si on renvoie le XML complet avec ses associations, PrestaShop crash en interne (erreur 500). On retire donc les associations avant le PUT.

**`try/catch warn`** : si le PUT échoue, ce n'est pas bloquant — l'import continue avec la date du jour. C'est un best-effort.

### Phase 4 — Si pas d'order, sortir ([importService.js:526](../NewApp/src/services/importService.js#L526))

```js
if (!needsOrder) return { cartId, orderId: null }
```

Pour `etat=""`, on s'arrête là (panier sans commande validée).

### Phase 5 — Calcul des totaux + secure_key ([importService.js:528-534](../NewApp/src/services/importService.js#L528-L534))

```js
const totalTTC = items.reduce((sum, it) => sum + (it.priceTTC * it.qty), 0)
const totalHT = totalTTC / 1.2

// secure_key : 32 caractères hexadécimaux (format md5)
const secureKey = Array.from({ length: 32 }, () =>
  Math.floor(Math.random() * 16).toString(16)
).join('')
```

**`totalHT = totalTTC / 1.2`** : approximation **fixe à 20% TVA**. Pas idéal en multi-taux, mais simplifie le calcul. À adapter si besoin de précision (calcul par produit).

**`secureKey`** : PrestaShop exige une clé sécurisée (32 hexa) sur chaque commande pour les liens publics. On génère un MD5-like aléatoire.

### Phase 6 — POST de la commande ([importService.js:536-573](../NewApp/src/services/importService.js#L536-L573))

```js
const orderXml = builder.build({
  prestashop: {
    order: {
      id_address_delivery: idAddress,
      id_address_invoice: idAddress,
      id_cart: cartId,
      id_currency: 1,
      id_lang: ID_LANG,
      id_customer: idCustomer,
      id_carrier: 2,
      current_state: 11,
      module: 'ps_cashondelivery',
      payment: 'Paiement à la livraison',
      secure_key: secureKey,
      valid: 1,
      total_paid: totalTTC.toFixed(6),
      total_paid_real: totalTTC.toFixed(6),
      total_paid_tax_incl: totalTTC.toFixed(6),
      total_paid_tax_excl: totalHT.toFixed(6),
      total_products: totalHT.toFixed(6),
      total_products_wt: totalTTC.toFixed(6),
      total_shipping: '0.000000',
      // ... tous les totaux à 0
      conversion_rate: '1.000000',
      date_add: `${date} 12:00:00`,
    },
  },
})

const orderResult = await postXml('/api/orders', orderXml)
const orderId = txt(orderResult.prestashop.order.id)
```

**Tous les champs obligatoires** sont remplis :
- `id_cart` lie l'order au panier qu'on vient de créer.
- `current_state: 11` = "Paiement à distance accepté" (créé directement validé).
- `module + payment` : on dit qu'on utilise "Cash On Delivery" (paiement à la livraison) — c'est le module le plus simple à scripter.
- `valid: 1` : marque la commande comme **valide** (visible en BO).
- `total_*` : tous nécessaires sinon PrestaShop refuse avec "missing field".

### Phase 7 — Forcer la date de l'order (PUT) ([importService.js:575-590](../NewApp/src/services/importService.js#L575-L590))

```js
try {
  const getOrderRes = await axios.get(`/api/orders/${orderId}`);
  const orderData = parser.parse(getOrderRes.data);
  orderData.prestashop.order.date_add = `${date} 12:00:00`;
  orderData.prestashop.order.date_upd = `${date} 12:00:00`;
  delete orderData.prestashop.order.associations;
  delete orderData.prestashop.order.shipping_number; // évite XML malformé "notFilterable"
  const updateOrderXml = builder.build(orderData);
  await axios.put(`/api/orders/${orderId}`, updateOrderXml, { headers: { 'Content-Type': 'application/xml' } });
} catch(e) { … }
```

Même logique que pour le panier, avec en plus :
- `delete orderData.prestashop.order.shipping_number` : ce champ est marqué `notFilterable` côté PrestaShop et son XML retourné est mal formé — on le retire avant le PUT.

### Phase 8 — Validation initiale via `/api/order_state_change` ([importService.js:592-604](../NewApp/src/services/importService.js#L592-L604))

```js
// 1. Forcer l'état "Paiement à distance accepté" (11) ET réserver le stock
// via /api/order_state_change : reserved_quantity++ (PrestaShop décrémente qty
// automatiquement lors de la création de la commande, donc on ne le re-fait pas)
try {
  await axios.post('/api/order_state_change', {
    id_order: parseInt(orderId),
    new_state: 11,
    date_add: `${date} 12:00:00`,
  }, { headers: { 'Content-Type': 'application/json' } })
  console.log(`✅ Commande ${orderId} validée + stock réservé`)
} catch (err) {
  console.warn(`⚠️ Impossible de valider/réserver pour la commande ${orderId}:`, err.message)
}
```

**Pourquoi un nouvel appel alors que `current_state=11` est déjà dans le POST** :
- Le POST `/api/orders` crée la commande mais **ne déclenche pas** la logique de transition (le webservice standard est neutre).
- L'endpoint custom `/api/order_state_change` (module `orderstateapi`) **simule la transition** : il ajoute une entrée `order_history` ET incrémente `reserved_quantity` ET crée éventuellement un `stock_mvt`.
- Pour les détails, voir [docs/commande.md](commande.md).

### Phase 9 — Transition supplémentaire (livré ou annulé) ([importService.js:606-619](../NewApp/src/services/importService.js#L606-L619))

```js
// 2. Si état "livré" ou "annulé", appliquer la transition supplémentaire
if (extraState) {
  try {
    await axios.post('/api/order_state_change', {
      id_order: parseInt(orderId),
      new_state: extraState,
      date_add: `${date} 12:00:00`,
    }, { headers: { 'Content-Type': 'application/json' } })
    const label = extraState === 5 ? 'livrée' : 'annulée'
    console.log(`✅ Commande ${orderId} ${label}`)
  } catch (err) {
    console.warn(`⚠️ Transition vers ${extraState} échouée pour la commande ${orderId}:`, err.message)
  }
}
```

**Chaîne d'états** : `null → 11 → extraState (5 ou 6)`.
- `5` = Livré → décrémente `physical_quantity` + crée un `stock_mvt` négatif.
- `6` = Annulé → libère `reserved_quantity` (voir [docs/Annulation.md](Annulation.md)).

---

## 19. importService.js — `importFile1` (Produits)

**Mots-clés** : importFile1, produits, validation, throw

Référence : [importService.js:631-688](../NewApp/src/services/importService.js#L631-L688).

```js
async importFile1(csvText, onProgress) {
  const results = { success: 0, errors: [] }

  let headers, data
  try {
    const parsed = parseCSV(csvText)
    headers = parsed.headers
    data = parsed.data
    validateHeaders(headers, [
      'date_availability_produit', 'nom', 'reference',
      'prix_ttc', 'Taxe', 'categorie', 'prix_achat'
    ])
  } catch (err) {
    results.errors.push({ line: 'header', error: err.message })
    return results
  }

  for (let i = 0; i < data.length; i++) {
    const row = data[i]
    onProgress?.({ step: 'Fichier 1 — Produits', current: i + 1, total: data.length, label: row.reference })

    try {
      const taxRate = parseFrenchPercent(row.Taxe)
      const priceTTC = parseFrenchNumber(row.prix_ttc)
      const wholesalePrice = parseFrenchNumber(row.prix_achat)

      // Validations montants positifs
      assertPositive(priceTTC, 'prix_ttc')
      assertPositive(wholesalePrice, 'prix_achat')
      assertNonNegative(taxRate, 'Taxe')

      const priceHT = priceTTC / (1 + taxRate / 100)

      const idCategory = await getOrCreateCategory(row.categorie)
      const idTax = await getOrCreateTax(taxRate)
      const idTaxGroup = await getOrCreateTaxRuleGroup(taxRate, idTax)

      const idProduct = await createProduct({
        name: row.nom,
        reference: row.reference,
        priceHT,
        idTaxGroup,
        idCategory,
        wholesalePrice,
        dateAvailable: parseDate(row.date_availability_produit),
      })

      cache.products.set(row.reference, { id: idProduct, priceHT, priceTTC, taxRate })
      results.success++
    } catch (err) {
      const context = `Fichier 1 — Produits | Ligne ${i + 1}${row.reference ? ` | Référence : ${row.reference}` : ''}`
      throw new Error(`${context}\nCause : ${err.message}`)
    }
  }

  return results
}
```

**Pipeline par ligne** :
1. Parser les nombres français.
2. Valider que `prix_ttc > 0`, `prix_achat > 0`, `Taxe >= 0`.
3. Convertir TTC → HT (`priceTTC / (1 + taxRate/100)`).
4. **getOrCreate** catégorie + taxe + tax_rule_group.
5. **createProduct** avec tous les IDs.
6. **Cacher** le produit (`cache.products.set(ref, { id, priceHT, priceTTC, taxRate })`) — utilisé par Fichier 2 et 3.

**Gestion d'erreur** :
- **Headers KO** → on push une erreur "header" et **return** (les autres fichiers continueront).
- **Erreur sur une ligne** → on **throw** avec un message contextuel (`Fichier 1 | Ligne 5 | Référence : TSHIRT\nCause : Montant invalide`). Ce throw remonte au `catch` global de `startImport` → erreur fatale.

---

## 20. importService.js — `importFile2` (Combinaisons + stock)

**Mots-clés** : importFile2, combinaisons, options, stock_initial

Référence : [importService.js:690-766](../NewApp/src/services/importService.js#L690-L766).

### Chargement du cache produits si vide ([importService.js:707-720](../NewApp/src/services/importService.js#L707-L720))

```js
// Charger produits depuis BD si cache vide
if (cache.products.size === 0) {
  const products = await getList('products', 'product')
  for (const p of products) {
    const ref = txt(p.reference)
    if (ref) {
      cache.products.set(ref, {
        id: txt(p.id),
        priceHT: num(p.price),
        priceTTC: num(p.price),
        taxRate: 0,
      })
    }
  }
}
```

**Pourquoi** : si l'utilisateur lance **uniquement le Fichier 2** (sans le 1 dans la même session), le cache est vide → on **recharge** les produits existants depuis l'API pour pouvoir faire les lookups par référence.

**⚠️ Limitation** : on perd `taxRate` (pas dans la réponse simple `/api/products`). Pour les calculs de delta prix, on assume `taxRate=0` → le delta TTC = delta HT. Acceptable car en import partiel on ne créé pas vraiment de nouveau prix.

### Boucle principale ([importService.js:722-763](../NewApp/src/services/importService.js#L722-L763))

```js
for (let i = 0; i < data.length; i++) {
  const row = data[i]
  onProgress?.({ step: 'Fichier 2 — Combinaisons', current: i + 1, total: data.length, label: row.reference })

  try {
    const product = cache.products.get(row.reference)
    if (!product) throw new Error(`Produit "${row.reference}" introuvable — vérifiez que le Fichier 1 a bien été importé avant`)

    const stock = parseInt(row.stock_initial) || 0
    const priceComboTTC = parseFrenchNumber(row.prix_vente_ttc) || product.priceTTC

    assertPositive(stock, 'stock_initial')
    if (row.prix_vente_ttc) assertPositive(priceComboTTC, 'prix_vente_ttc')

    if (!row.specificité || !row.karazany) {
      // Pas de combinaison : juste setter le stock du produit
      await setStock(product.id, 0, stock)
    } else {
      // Combinaison : créer option + value + combinaison + setter stock
      const idOption = await getOrCreateProductOption(row.specificité)
      const idValue = await getOrCreateProductOptionValue(idOption, row.karazany)

      const deltaTTC = priceComboTTC - product.priceTTC
      const deltaHT = deltaTTC / (1 + product.taxRate / 100)

      const idCombination = await createCombination({
        idProduct: product.id,
        priceDeltaHT: deltaHT,
        optionValueId: idValue,
        reference: `${row.reference}-${row.karazany}`,
      })

      cache.combinations.set(`${row.reference}|${row.karazany}`, idCombination)
      await setStock(product.id, idCombination, stock)
    }

    results.success++
  } catch (err) {
    const variantInfo = row.karazany ? ` | Variante : ${row.karazany}` : ''
    const context = `Fichier 2 — Combinaisons | Ligne ${i + 1}${row.reference ? ` | Référence : ${row.reference}` : ''}${variantInfo}`
    throw new Error(`${context}\nCause : ${err.message}`)
  }
}
```

**Deux branches** :
1. **Pas de combinaison** (`specificité` ou `karazany` vide) → on set juste le stock du produit (combinationId=0).
2. **Avec combinaison** :
   - getOrCreate de l'option (ex. "Couleur") et de sa valeur (ex. "Rouge").
   - Calculer le delta de prix (combinaison TTC − produit TTC).
   - createCombination avec ce delta.
   - setStock sur la combinaison.

**`reference: '${row.reference}-${row.karazany}'`** : on crée une référence concaténée (`TSHIRT-Rouge`) qui sera utilisée pour re-recharger le cache combinaisons en Fichier 3.

---

## 21. importService.js — `importFile3` (Clients, paniers, commandes)

**Mots-clés** : importFile3, stockTracker, vérification stock, commande

Référence : [importService.js:768-920](../NewApp/src/services/importService.js#L768-L920).

### Rechargement caches si vides ([importService.js:784-808](../NewApp/src/services/importService.js#L784-L808))

```js
if (cache.products.size === 0) {
  // ... recharger produits
  const combos = await getList('combinations', 'combination')
  for (const c of combos) {
    const ref = c.reference ? txt(c.reference) : ''
    if (ref && ref.includes('-')) {
      const parts = ref.split('-')
      const productRef = parts[0]
      const valueName = parts.slice(1).join('-')
      cache.combinations.set(`${productRef}|${valueName}`, txt(c.id))
    }
  }
}
```

**Logique** : pareil que Fichier 2 + on recharge les combinaisons en décomposant leur référence concaténée `TSHIRT-Rouge` → key cache `TSHIRT|Rouge`. **`parts.slice(1).join('-')`** : si la variante elle-même contient un `-` (ex. `bleu-clair`), on prend tout ce qui suit le premier `-`.

### Tracker de stock en mémoire ([importService.js:810-820](../NewApp/src/services/importService.js#L810-L820))

```js
const stockTracker = new Map()
const stocks = await getList('stock_availables', 'stock_available')
for (const s of stocks) {
  const idProd = String(txt(s.id_product))
  const idAttr = String(txt(s.id_product_attribute))
  const qty = parseInt(txt(s.quantity)) || 0
  stockTracker.set(`${idProd}|${idAttr}`, qty)
}
console.log(`📦 Tracker stock chargé : ${stockTracker.size} entrées`)
```

**Pourquoi** : on doit **vérifier avant** de créer une commande qu'il y a assez de stock. Mais l'API webservice est trop lente pour faire un GET par produit à chaque ligne. **Solution** : charger **tout le stock une fois** au début, et **décrémenter en mémoire** au fur et à mesure des commandes créées.

### Détection des états consommateurs de stock ([importService.js:822-826](../NewApp/src/services/importService.js#L822-L826))

```js
const consumesStock = (etat) => {
  const e = String(etat ?? '').toLowerCase().trim()
  return e.includes('accept') || e.includes('livr')   // pas pour annulé ni vide
}
```

**Liste blanche** : `paiement accepté` et `livré` consomment du stock. `annulé` et vide non — pas la peine de vérifier la dispo pour un panier non validé ou une commande annulée.

### Pipeline par ligne ([importService.js:828-917](../NewApp/src/services/importService.js#L828-L917))

```js
for (let i = 0; i < data.length; i++) {
  const row = data[i]
  onProgress?.({ step: 'Fichier 3 — Commandes', current: i + 1, total: data.length, label: row.email })

  try {
    const parsedDate = parseDate(row.date)
    
    const idCustomer = await getOrCreateCustomer({ email: row.email, nom: row.nom, password: row.pwd })
    const idAddress = await getOrCreateAddress({ idCustomer, nom: row.nom, alias: row.adresse })

    // Parser la colonne achat → items
    const purchases = parseAchat(row.achat)
    const items = []
    for (const p of purchases) {
      const product = cache.products.get(p.ref)
      if (!product) throw new Error(`Produit "${p.ref}" non trouvé — …`)
      assertPositive(p.qty, `quantité de ${p.ref}`)

      let idCombination = 0
      if (p.value) {
        const comboId = cache.combinations.get(`${p.ref}|${p.value}`)
        if (!comboId) throw new Error(`Combinaison "${p.ref}/${p.value}" non trouvée — …`)
        idCombination = comboId
      }

      // Dédupliquer : si même item déjà présent, on additionne les quantités
      const existingItem = items.find(it => it.idProduct === product.id && it.idCombination === idCombination)
      if (existingItem) {
        existingItem.qty += p.qty
      } else {
        items.push({ idProduct: product.id, idCombination, qty: p.qty, priceTTC: product.priceTTC, ref: p.ref, value: p.value })
      }
    }

    // ===== Vérification du stock disponible =====
    if (consumesStock(row.etat)) {
      for (const it of items) {
        const key = `${it.idProduct}|${it.idCombination}`
        const available = stockTracker.get(key) ?? 0
        if (it.qty > available) {
          const label = it.value ? `${it.ref}/${it.value}` : it.ref
          throw new Error(`Stock insuffisant pour ${label} : ${it.qty} demandé(s), seulement ${available} disponible(s)`)
        }
      }
    }

    await createCartAndOrder({ idCustomer, idAddress, items, etat: row.etat, date: parsedDate })

    // ===== Décrémenter le tracker pour les ordres suivants =====
    if (consumesStock(row.etat)) {
      for (const it of items) {
        const key = `${it.idProduct}|${it.idCombination}`
        stockTracker.set(key, (stockTracker.get(key) ?? 0) - it.qty)
      }
    }

    results.success++
  } catch (err) {
    const context = `Fichier 3 — Commandes | Ligne ${i + 1}${row.email ? ` | Client : ${row.email}` : ''}`
    throw new Error(`${context}\nCause : ${err.message}`)
  }
}
```

**Étapes par ligne** :
1. **Parser la date** (throw si KO).
2. **getOrCreate** client + adresse.
3. **Parser `achat`** → tableau d'items, en **dédupliquant** : si le client commande 2× la même variante en plusieurs `()()`, on additionne au lieu de créer 2 cart_rows.
4. **Vérifier le stock** (si état consomme). Throw si insuffisant.
5. **Créer panier + commande** via `createCartAndOrder`.
6. **Décrémenter** le tracker pour les itérations suivantes.

**Pourquoi dédupliquer les items** : sinon PrestaShop crée 2 lignes `cart_row` avec le même `(id_product, id_product_attribute)` ce qui peut casser l'affichage du panier en BO.

---

## 22. importService.js — `importImages` (ZIP → /api/images)

**Mots-clés** : JSZip, blob, FormData, multipart, multer

Référence : [importService.js:922-978](../NewApp/src/services/importService.js#L922-L978).

```js
async importImages(zipFile, onProgress) {
  const results = { success: 0, errors: [] }

  let JSZip
  try {
    JSZip = (await import(/* @vite-ignore */ 'jszip')).default
  } catch (e) {
    throw new Error('JSZip non installé. Dans le dossier NewApp, lance : npm install jszip puis redémarre Vite')
  }

  const zip = await JSZip.loadAsync(zipFile)
  const entries = Object.values(zip.files).filter(f => {
    if (f.dir) return false
    if (!/\.(png|jpe?g)$/i.test(f.name)) return false
    // Ignorer les fichiers de métadonnées macOS (.__MACOSX, ._filename)
    const filename = f.name.split('/').pop()
    if (filename.startsWith('._')) return false
    if (f.name.includes('__MACOSX')) return false
    return true
  })

  if (cache.products.size === 0) {
    const products = await getList('products', 'product')
    for (const p of products) {
      const ref = txt(p.reference)
      if (ref) cache.products.set(ref, { id: txt(p.id) })
    }
  }

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i]
    const filename = entry.name.split('/').pop()
    const reference = filename.replace(/\.[^.]+$/, '')

    onProgress?.({ step: 'Images', current: i + 1, total: entries.length, label: filename })

    try {
      const product = cache.products.get(reference)
      if (!product) throw new Error(`Produit ${reference} non trouvé`)

      const blob = await entry.async('blob')
      const formData = new FormData()
      formData.append('image', blob, filename)

      await axios.post(`/api/images/products/${product.id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })

      results.success++
    } catch (err) {
      console.error(`❌ Image ${filename}:`, err)
      results.errors.push({ filename, error: err.message })
    }
  }

  return results
}
```

**Points clés** :
- **`import(/* @vite-ignore */ 'jszip')`** : import dynamique → JSZip n'est chargé que si l'utilisateur lance l'import d'images. Réduit le bundle initial.
- **Commentaire `@vite-ignore`** : empêche Vite de pré-bundler `jszip` → évite des erreurs CommonJS/ESM.
- **Filtre macOS** : un ZIP créé sur Mac contient des fichiers de métadonnées (`__MACOSX/._photo.jpg`) qu'il faut ignorer.
- **`reference = filename.replace(/\.[^.]+$/, '')`** : on extrait la référence du nom de fichier (ex. `TSHIRT-001.jpg` → référence `TSHIRT-001`).
- **`formData.append('image', blob, filename)`** : PrestaShop attend un champ nommé exactement `image` (vu dans le contrôleur d'upload). Le nom de fichier est conservé.
- **Erreurs non-fatales** : contrairement aux Fichier 1/2/3, on ne **throw pas** sur une erreur d'image — on **push** dans `results.errors` et on continue. C'est volontaire : une image manquante ne casse pas le catalogue.

---

## 23. Récap des fichiers concernés

### Couche Vue (UI)
| Fichier | Rôle |
|---------|------|
| [NewApp/src/views/BackOffice/ImportView.vue:1-116](../NewApp/src/views/BackOffice/ImportView.vue#L1-L116) | Template HTML (inputs, progression, résultats) |
| [NewApp/src/views/BackOffice/ImportView.vue:118-199](../NewApp/src/views/BackOffice/ImportView.vue#L118-L199) | Script setup (état, orchestration `startImport`) |
| [NewApp/src/views/BackOffice/ImportView.vue:202-213](../NewApp/src/views/BackOffice/ImportView.vue#L202-L213) | Composant inline `FileInput` |

### Couche Service
| Fichier / section | Rôle |
|-------------------|------|
| [NewApp/src/services/importService.js:1-10](../NewApp/src/services/importService.js#L1-L10) | Imports + constantes |
| [NewApp/src/services/importService.js:16-44](../NewApp/src/services/importService.js#L16-L44) | `parseCSV` |
| [NewApp/src/services/importService.js:49-103](../NewApp/src/services/importService.js#L49-L103) | Validators (`validateHeaders`, `parseDate`, `assertPositive`) |
| [NewApp/src/services/importService.js:105-114](../NewApp/src/services/importService.js#L105-L114) | `parseAchat` |
| [NewApp/src/services/importService.js:116-158](../NewApp/src/services/importService.js#L116-L158) | Couche HTTP (`postXml`, `getList`, `txt`, `num`) |
| [NewApp/src/services/importService.js:164-178](../NewApp/src/services/importService.js#L164-L178) | Cache anti-doublons |
| [NewApp/src/services/importService.js:184-287](../NewApp/src/services/importService.js#L184-L287) | `getOrCreate` (catégorie, taxe, tax_rule_group) |
| [NewApp/src/services/importService.js:289-391](../NewApp/src/services/importService.js#L289-L391) | createProduct, options, valeurs, combinaisons |
| [NewApp/src/services/importService.js:393-401](../NewApp/src/services/importService.js#L393-L401) | `setStock` (via `/api/stock_delta`) |
| [NewApp/src/services/importService.js:403-471](../NewApp/src/services/importService.js#L403-L471) | hashPassword, getOrCreateCustomer, getOrCreateAddress |
| [NewApp/src/services/importService.js:473-622](../NewApp/src/services/importService.js#L473-L622) | `createCartAndOrder` |
| [NewApp/src/services/importService.js:631-688](../NewApp/src/services/importService.js#L631-L688) | `importFile1` (Produits) |
| [NewApp/src/services/importService.js:690-766](../NewApp/src/services/importService.js#L690-L766) | `importFile2` (Combinaisons + stock) |
| [NewApp/src/services/importService.js:768-920](../NewApp/src/services/importService.js#L768-L920) | `importFile3` (Clients, paniers, commandes) |
| [NewApp/src/services/importService.js:922-978](../NewApp/src/services/importService.js#L922-L978) | `importImages` (ZIP → /api/images) |

### Couche modules custom
| Module | Endpoint exposé | Utilisé pour |
|--------|------------------|--------------|
| `modules/stockdeltaapi/` | `POST /api/stock_delta` (mode `set_to`) | Initialiser `physical_quantity` + `quantity` |
| `modules/orderstateapi/` | `POST /api/order_state_change` | Transitions d'état (11, 5, 6) avec impacts stock |

### Helpers PHP
| Fichier | Rôle |
|---------|------|
| `hash_password.php` (racine PrestaShop) | Hash bcrypt pour `customer.passwd` |

### Documentation liée
| Fichier | Sujet |
|---------|-------|
| [docs/RefVue.md](RefVue.md) | Documentation des refs Vue |
| [docs/Service.md](Service.md) | Conventions des services |
| [docs/PutPost.md](PutPost.md) | Pattern PUT/POST en XML |
| [docs/filter.md](filter.md) | Syntaxe `filter[champ]=valeur` |
| [docs/commande.md](commande.md) | Cycle de vie d'une commande |
| [docs/Annulation.md](Annulation.md) | Workflow d'annulation |
| [docs/order_slip.md](order_slip.md) | API `/api/order_slip` (avoirs) |

---

> 💡 **TL;DR** : Le Vue [ImportView.vue](../NewApp/src/views/BackOffice/ImportView.vue) sert d'**orchestrateur** : il lit les fichiers, lance l'import séquentiel, affiche la progression. Le service [importService.js](../NewApp/src/services/importService.js) contient **toute la logique métier** : parsing, validation, get-or-create idempotent, création de produits/combinaisons/stock/commandes/transitions. Le tout repose sur 2 modules PrestaShop custom (`stockdeltaapi`, `orderstateapi`) + 1 helper PHP (`hash_password.php`).
