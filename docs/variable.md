# 🧮 Guide complet : manipulation des variables (.vue / .js)

> **Mots-clés généraux** : variable, type, number, string, date, array, object, boolean, parseInt, parseFloat, toFixed, JSON, localStorage, form, v-model, format français, virgule, point, decimal, padStart, split, map, filter, reduce, computed, ref, reactive, conversion, casting, typeof, isNaN, truthy, falsy, null, undefined

---

## 📑 Index

- [1. Types JavaScript en bref](#1-types-javascript-en-bref)
- [2. Numbers — parseInt, parseFloat, toFixed](#2-numbers--parseint-parsefloat-tofixed)
- [3. Strings — manipulations courantes](#3-strings--manipulations-courantes)
- [4. Dates — création, parsing, formatage, comparaison](#4-dates--création-parsing-formatage-comparaison)
- [5. Arrays — map, filter, reduce, find, sort](#5-arrays--map-filter-reduce-find-sort)
- [6. Objects — destructuring, spread, JSON](#6-objects--destructuring-spread-json)
- [7. Booléens et truthy/falsy](#7-booléens-et-truthyfalsy)
- [8. Formulaires Vue — v-model et conversion](#8-formulaires-vue--v-model-et-conversion)
- [9. Formats français (virgule, %, dates)](#9-formats-français-virgule--dates)
- [10. localStorage (toujours string)](#10-localstorage-toujours-string)
- [11. URL query params (Vue Router)](#11-url-query-params-vue-router)
- [12. XML PrestaShop — extraction `#text`](#12-xml-prestashop--extraction-text)
- [13. Conversion HT / TTC (prix)](#13-conversion-ht--ttc-prix)
- [14. ref vs reactive — choisir](#14-ref-vs-reactive--choisir)
- [15. Anti-patterns à éviter](#15-anti-patterns-à-éviter)
- [16. Helpers réutilisables](#16-helpers-réutilisables)
- [17. Templates copy-paste](#17-templates-copy-paste)
- [18. Cheatsheet](#18-cheatsheet)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Types JavaScript en bref

**Mots-clés** : types, typeof, primitive, objet

Les 7 types primitifs :

| Type | Exemple | `typeof` |
|------|---------|----------|
| `string` | `"hello"`, `'42'` | `"string"` |
| `number` | `42`, `3.14`, `NaN`, `Infinity` | `"number"` |
| `boolean` | `true`, `false` | `"boolean"` |
| `null` | `null` | `"object"` ⚠️ |
| `undefined` | `undefined` | `"undefined"` |
| `symbol` | rare en pratique | `"symbol"` |
| `bigint` | `42n` | `"bigint"` |

Objets non-primitifs :

| Type | Exemple | `typeof` |
|------|---------|----------|
| Array | `[1, 2, 3]` | `"object"` |
| Object | `{ a: 1 }` | `"object"` |
| Function | `() => 42` | `"function"` |
| Date | `new Date()` | `"object"` |

### Détection fiable

```js
typeof x === 'string'      // ok
Array.isArray(x)           // PAS typeof === 'array' (n'existe pas)
x === null                 // PAS typeof === 'null'
x instanceof Date          // pour les Dates
Number.isInteger(x)        // entier ?
```

---

## 2. Numbers — parseInt, parseFloat, toFixed

**Mots-clés** : number, parseInt, parseFloat, toFixed, Math, round, NaN

### Conversion string → number

```js
parseInt('42')              // 42 (entier)
parseInt('42.7')            // 42 (TRONQUE — pas d'arrondi)
parseInt('42abc')           // 42 (s'arrête au premier non-chiffre)
parseInt('abc42')           // NaN

parseFloat('3.14')          // 3.14
parseFloat('3,14')          // 3 ⚠️ (la virgule arrête le parse)
parseFloat('3.14e2')        // 314 (notation scientifique)

Number('42')                // 42
Number('42.7')              // 42.7
Number('')                  // 0 ⚠️ (string vide → 0)
Number('abc')               // NaN
Number(null)                // 0 ⚠️
Number(undefined)           // NaN

+'42'                       // 42 (raccourci avec opérateur +)
```

### Toujours utiliser le 2e param de parseInt

```js
parseInt('08')              // 8 (en général, OK)
parseInt('08', 10)          // ✅ 8 (forcer base 10, plus sûr)
```

### Vérifier NaN

```js
isNaN(NaN)                  // true
isNaN('abc')                // true ⚠️ (convertit en number d'abord)
Number.isNaN(NaN)           // ✅ true (strict)
Number.isNaN('abc')         // ✅ false (n'est pas NaN, c'est une string)
isFinite(42)                // true
isFinite(Infinity)          // false
```

### Formater pour affichage

```js
(12.5).toFixed(2)           // "12.50" (string !)
(12.5).toFixed(6)           // "12.500000" — format PrestaShop pour les prix
(12.345).toFixed(2)         // "12.35" (arrondi)
parseFloat((12.5).toFixed(2))   // 12.5 (re-number)

Math.round(12.5)            // 13
Math.round(12.4)            // 12
Math.floor(12.9)            // 12
Math.ceil(12.1)             // 13
Math.abs(-5)                // 5
Math.max(1, 5, 3)           // 5
Math.min(1, 5, 3)           // 1
```

### Arrondir à N décimales correctement

```js
function round(num, decimals = 2) {
  const f = Math.pow(10, decimals)
  return Math.round(num * f) / f
}

round(12.345, 2)            // 12.35
```

### Division entière

```js
Math.floor(10 / 3)          // 3
Math.trunc(-3.7)            // -3 (tronque vers 0)
10 % 3                      // 1 (reste)
```

---

## 3. Strings — manipulations courantes

**Mots-clés** : string, replace, split, trim, includes, startsWith, padStart, slice, template literal

### Modifications

```js
'hello'.toUpperCase()       // 'HELLO'
'HELLO'.toLowerCase()       // 'hello'
'  hello  '.trim()          // 'hello' (enlève les espaces autour)
'  hello  '.trimStart()     // 'hello  '
'  hello  '.trimEnd()       // '  hello'

'12,5'.replace(',', '.')                  // '12.5'
'11,65%'.replace(',', '.').replace('%','')// '11.65'
'a-b-c'.replaceAll('-', '_')              // 'a_b_c'

'1'.padStart(2, '0')        // '01' (utile pour dates)
'9'.padStart(3, '0')        // '009'
```

### Tests

```js
'rakoto@yopmail.com'.includes('@')        // true
'PANIER-30'.startsWith('PANIER')          // true
'photo.jpg'.endsWith('.jpg')              // true

'abc'.length                              // 3
'abc'[0]                                  // 'a'
'abc'.charAt(1)                           // 'b'
```

### Découper / fusionner

```js
'01/12/2025'.split('/')                   // ['01', '12', '2025']
'a,b,c'.split(',')                        // ['a', 'b', 'c']
['a', 'b', 'c'].join(' - ')               // 'a - b - c'

'hello world'.slice(0, 5)                 // 'hello'
'hello world'.substring(6)                // 'world'
'hello world'.substring(0, 5)             // 'hello'
```

### Template literals (interpolation)

```js
const name = 'Rakoto'
const age = 25

// ✅ Préféré
`Bonjour ${name}, vous avez ${age} ans`

// ❌ Vieux style
'Bonjour ' + name + ', vous avez ' + age + ' ans'

// Multi-ligne
`Ligne 1
Ligne 2
Ligne 3`
```

### Regex

```js
'abc123'.match(/\d+/)               // ['123', index: 3, ...]
'abc123'.match(/\d+/)?.[0]          // '123'
/^\d+$/.test('123')                 // true
/^\d+$/.test('abc')                 // false

// Format CDATA PrestaShop
const xml = '<message><![CDATA[Erreur 42]]></message>'
xml.match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/s)?.[1]
// → 'Erreur 42'
```

---

## 4. Dates — création, parsing, formatage, comparaison

**Mots-clés** : Date, parsing, format, ISO, timestamp, comparer, age, diff

### Création

```js
new Date()                                    // maintenant
new Date('2026-05-19')                        // ISO format
new Date('2026-05-19T10:30:00')              // avec heure
new Date(2026, 4, 19)                         // ⚠️ mois 0-indexé (4 = mai)
new Date(1716120000000)                       // depuis timestamp ms

Date.now()                                    // timestamp actuel en ms
```

### Lecture (locale time)

```js
const d = new Date('2026-05-19T10:30:00')

d.getFullYear()         // 2026
d.getMonth()            // 4 (mai, 0-indexé !)
d.getDate()             // 19 (jour du mois)
d.getDay()              // 2 (jour semaine : 0=dimanche)
d.getHours()            // 10
d.getMinutes()          // 30
d.getTime()             // 1747650600000 (timestamp ms)
```

### Parsing DD/MM/YYYY (format français)

```js
function parseDate(s) {
  if (!s) return new Date().toISOString().split('T')[0]
  const match = s.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!match) throw new Error(`Format invalide : "${s}" — attendu DD/MM/YYYY`)
  const [, d, m, y] = match
  return `${y}-${m}-${d}`            // → "YYYY-MM-DD"
}

parseDate('01/12/2025')                       // "2025-12-01"
parseDate('29/02/2026')                       // → ⚠️ date invalide
```

### Formatage pour affichage

```js
const d = new Date('2026-05-19T10:30:00')

// ISO (standard)
d.toISOString()                               // "2026-05-19T10:30:00.000Z"
d.toISOString().split('T')[0]                 // "2026-05-19"

// Local
d.toLocaleDateString('fr-FR')                 // "19/05/2026"
d.toLocaleTimeString('fr-FR')                 // "10:30:00"
d.toLocaleString('fr-FR')                     // "19/05/2026, 10:30:00"

// Format custom
d.toLocaleDateString('fr-FR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
})  // "19/05/2026, 10:30"
```

### Formatage manuel (sans librairie)

```js
function formatDate(dateStr) {
  // YYYY-MM-DD → DD/MM/YYYY
  const [y, m, d] = dateStr.split('-')
  return `${d}/${m}/${y}`
}

function formatTime(dateTimeStr) {
  // YYYY-MM-DD HH:MM:SS → HH:MM
  const time = dateTimeStr.split(' ')[1]
  return time ? time.substring(0, 5) : ''
}

formatDate('2026-05-19')                      // "19/05/2026"
formatTime('2026-05-19 10:30:45')             // "10:30"
```

### Comparaisons

```js
const a = new Date('2026-05-19')
const b = new Date('2026-05-20')

a < b                                          // true
a.getTime() === b.getTime()                    // false
a < b ? 'avant' : 'après'                      // 'avant'

// Différence en jours
const diffMs = b - a
const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))   // 1
```

### Aujourd'hui / hier / demain

```js
const today = new Date()
const yesterday = new Date(today)
yesterday.setDate(today.getDate() - 1)

const tomorrow = new Date(today)
tomorrow.setDate(today.getDate() + 1)

// Ajouter 30 jours
const inAMonth = new Date()
inAMonth.setDate(inAMonth.getDate() + 30)
```

### Date "PrestaShop" (`YYYY-MM-DD HH:MM:SS`)

```js
function nowPrestaShop() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
       + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

nowPrestaShop()                                // "2026-05-19 10:30:45"
```

---

## 5. Arrays — map, filter, reduce, find, sort

**Mots-clés** : array, map, filter, reduce, find, some, every, sort, push, splice

### Création / accès

```js
const arr = [1, 2, 3]
arr[0]                                         // 1
arr.length                                     // 3
arr.includes(2)                                // true
arr.indexOf(2)                                 // 1
arr[arr.length - 1]                            // 3 (dernier)
arr.at(-1)                                     // 3 (ES2022)
```

### Transformation (ne mute pas)

```js
[1, 2, 3].map(x => x * 2)                      // [2, 4, 6]
[1, 2, 3, 4].filter(x => x > 2)                // [3, 4]
[1, 2, 3].reduce((sum, x) => sum + x, 0)       // 6

// Map d'objets
products.map(p => ({ ...p, priceTTC: p.price * 1.2 }))
products.filter(p => p.active === 1)
products.reduce((total, p) => total + p.price, 0)
```

### Recherche

```js
[1, 2, 3].find(x => x > 1)                     // 2 (premier)
[1, 2, 3].findIndex(x => x > 1)                // 1
[1, 2, 3].some(x => x > 2)                     // true (au moins un)
[1, 2, 3].every(x => x > 0)                    // true (tous)
```

### Tri (mute !)

```js
[3, 1, 2].sort()                               // [1, 2, 3]
[10, 1, 5].sort()                              // [1, 10, 5] ⚠️ ordre lexicographique !
[10, 1, 5].sort((a, b) => a - b)               // [1, 5, 10] (numérique)
[10, 1, 5].sort((a, b) => b - a)               // [10, 5, 1] (descendant)

// Trier par champ
products.sort((a, b) => a.price - b.price)
orders.sort((a, b) => b.date_add.localeCompare(a.date_add))   // date desc
```

### Modification (mute)

```js
const arr = [1, 2, 3]
arr.push(4)                                    // [1, 2, 3, 4]
arr.pop()                                      // 4 (et arr devient [1, 2, 3])
arr.unshift(0)                                 // [0, 1, 2, 3]
arr.shift()                                    // 0 (et arr devient [1, 2, 3])
arr.splice(1, 1)                               // supprime à l'index 1 → [1, 3]
```

### Modification (immutable, préféré en Vue/React)

```js
const arr = [1, 2, 3]
const arr2 = [...arr, 4]                       // [1, 2, 3, 4]
const arr3 = arr.filter(x => x !== 2)          // [1, 3]
const arr4 = arr.toReversed()                  // [3, 2, 1] (ES2023)
const arr5 = [...arr].reverse()                // copie + reverse (pre-ES2023)
```

### Conversion

```js
Array.from('abc')                              // ['a', 'b', 'c']
Array.from({ length: 5 }, (_, i) => i * 2)     // [0, 2, 4, 6, 8]
[...new Set([1, 2, 1, 3])]                     // [1, 2, 3] (dédoublonner)
```

---

## 6. Objects — destructuring, spread, JSON

**Mots-clés** : object, destructuring, spread, Object.keys, Object.values, Object.entries, JSON

### Accès et modification

```js
const obj = { a: 1, b: 2 }
obj.a                                          // 1
obj['a']                                       // 1
obj.c = 3                                      // ajoute
delete obj.b                                   // supprime

'a' in obj                                     // true
obj.hasOwnProperty('a')                        // true (préfère in)
```

### Destructuring

```js
const { a, b } = { a: 1, b: 2, c: 3 }
// a = 1, b = 2

// Avec rename
const { a: x } = { a: 1 }                      // x = 1

// Avec défaut
const { name = 'Anon' } = obj

// Imbriqué
const { user: { name } } = response
```

### Spread / merge

```js
const a = { x: 1, y: 2 }
const b = { y: 3, z: 4 }

const merged = { ...a, ...b }                  // { x: 1, y: 3, z: 4 } (b écrase a)
const withExtra = { ...a, w: 5 }               // { x: 1, y: 2, w: 5 }

// Update immutable
const updated = { ...user, name: 'New' }
```

### Iterate

```js
const obj = { a: 1, b: 2 }

Object.keys(obj)                               // ['a', 'b']
Object.values(obj)                             // [1, 2]
Object.entries(obj)                            // [['a', 1], ['b', 2]]

for (const [key, value] of Object.entries(obj)) {
  console.log(key, value)
}
```

### JSON

```js
JSON.stringify({ a: 1 })                       // '{"a":1}'
JSON.stringify({ a: 1 }, null, 2)              // formaté (indent 2 espaces)

JSON.parse('{"a":1}')                          // { a: 1 }
JSON.parse('invalid')                          // ❌ SyntaxError

// Pattern safe
function safeParse(str, fallback = null) {
  try { return JSON.parse(str) } catch { return fallback }
}
```

### Conversion en Map / Set

```js
const map = new Map()
map.set('key1', 42)
map.get('key1')                                // 42
map.has('key1')                                // true
map.delete('key1')

// Object → Map
const m = new Map(Object.entries({ a: 1 }))

const set = new Set([1, 2, 2, 3])              // { 1, 2, 3 }
set.has(2)                                     // true
```

---

## 7. Booléens et truthy/falsy

**Mots-clés** : boolean, truthy, falsy, !!, ||, ??

### Valeurs falsy en JS

```js
false, 0, -0, '', null, undefined, NaN, 0n
```

**Tout le reste est truthy** (y compris `'0'`, `'false'`, `[]`, `{}`).

```js
Boolean('')                                    // false
Boolean('0')                                   // true ⚠️
Boolean([])                                    // true ⚠️
Boolean(null)                                  // false
!!'hello'                                      // true (raccourci Boolean())
```

### Opérateurs logiques

```js
// || retourne la première valeur truthy
'a' || 'b'                                     // 'a'
'' || 'fallback'                               // 'fallback'
0 || 42                                        // 42

// ?? retourne le 1er non-null/non-undefined
0 ?? 42                                        // 0 (préserve les falsy)
'' ?? 'fallback'                               // ''
null ?? 'fallback'                             // 'fallback'

// && retourne la première valeur falsy ou la dernière
true && 'value'                                // 'value'
false && 'value'                               // false
```

### Optional chaining

```js
user?.address?.city                            // ne plante pas si user/address null
user?.fn?.()                                   // appel safe
arr?.[0]                                       // index safe
```

### Comparaisons

```js
1 == '1'                                       // true (coercion ⚠️)
1 === '1'                                      // false (strict, préféré)
null == undefined                              // true ⚠️
null === undefined                             // false
```

**Règle d'or** : **toujours utiliser `===` et `!==`**.

---

## 8. Formulaires Vue — v-model et conversion

**Mots-clés** : v-model, input, form, number, trim, lazy

### Bases

```vue
<input v-model="text" />                       <!-- string -->
<input v-model.number="age" type="number" />   <!-- ⚠️ conversion auto en number -->
<input v-model.trim="name" />                  <!-- trim auto -->
<input v-model.lazy="text" />                  <!-- update sur blur (pas chaque touche) -->

<input type="checkbox" v-model="active" />     <!-- boolean -->
<input type="checkbox" v-model="hobbies" value="sport" />   <!-- multi-select -->

<select v-model="country">
  <option value="FR">France</option>
</select>

<select v-model="categories" multiple>         <!-- → array -->
  <option v-for="c in cats" :value="c.id">{{ c.name }}</option>
</select>

<input type="radio" v-model="gender" value="M" />
<input type="radio" v-model="gender" value="F" />
```

### Conversion explicite

```vue
<input v-model="priceStr" />

<script setup>
const priceStr = ref('')

// Convertir en number quand nécessaire
const priceNum = computed(() => parseFloat(priceStr.value) || 0)
</script>
```

### Type input vs type de la value

```vue
<!-- type number, mais sans .number → reste string ! -->
<input type="number" v-model="x" />            <!-- x = "42" (string) -->
<input type="number" v-model.number="x" />     <!-- x = 42 (number) -->

<!-- Input type=date donne YYYY-MM-DD (string) -->
<input type="date" v-model="d" />              <!-- d = "2026-05-19" -->
```

### Validation côté front

```vue
<script setup>
import { ref, computed } from 'vue'

const email = ref('')
const password = ref('')

const isEmailValid = computed(() => /^.+@.+\..+$/.test(email.value))
const isPasswordValid = computed(() => password.value.length >= 8)
const canSubmit = computed(() => isEmailValid.value && isPasswordValid.value)
</script>

<template>
  <input v-model="email" type="email" />
  <input v-model="password" type="password" />
  <button :disabled="!canSubmit">Envoyer</button>
</template>
```

---

## 9. Formats français (virgule, %, dates)

**Mots-clés** : français, virgule, point, décimal, %, locale

CSV provenant d'Excel français utilise `,` comme séparateur décimal et `%` :

```js
// Helpers du projet (importService.js)
const parseFrenchNumber = (s) => parseFloat(String(s ?? '').replace(',', '.')) || 0
const parseFrenchPercent = (s) =>
  parseFloat(String(s ?? '').replace(',', '.').replace('%', '')) || 0

parseFrenchNumber('12,5')                      // 12.5
parseFrenchNumber('1234,56')                   // 1234.56
parseFrenchPercent('11,65%')                   // 11.65
parseFrenchPercent('20%')                      // 20
```

### Affichage en français

```js
(1234.56).toLocaleString('fr-FR')              // "1 234,56"
(1234.56).toLocaleString('fr-FR', {
  style: 'currency',
  currency: 'EUR'
})                                              // "1 234,56 €"

(0.1165).toLocaleString('fr-FR', {
  style: 'percent',
  minimumFractionDigits: 2
})                                              // "11,65 %"
```

### Date au format français

```js
new Date().toLocaleDateString('fr-FR')         // "19/05/2026"
new Date().toLocaleDateString('fr-FR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric'
})                                              // "lundi 19 mai 2026"
```

---

## 10. localStorage (toujours string)

**Mots-clés** : localStorage, session, persistance, JSON.parse/stringify

**Tout** ce qui sort de localStorage est une **string**. Convertir si nécessaire.

```js
// Écriture
localStorage.setItem('count', 42)              // stocke "42" (auto-toString)
localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Rakoto' }))
localStorage.setItem('isActive', 'true')

// Lecture
localStorage.getItem('count')                  // "42" (string !)
parseInt(localStorage.getItem('count'))        // 42

JSON.parse(localStorage.getItem('user'))       // { id: 1, name: 'Rakoto' }
localStorage.getItem('isActive') === 'true'    // boolean true

// Avec fallback
const raw = localStorage.getItem('user')
const user = raw ? JSON.parse(raw) : null

// Suppression
localStorage.removeItem('count')
localStorage.clear()                           // tout effacer
```

### Helper réutilisable

```js
export const Storage = {
  set(key, value) {
    localStorage.setItem(key, JSON.stringify(value))
  },
  get(key, fallback = null) {
    const raw = localStorage.getItem(key)
    if (raw === null) return fallback
    try { return JSON.parse(raw) } catch { return raw }
  },
  remove(key) { localStorage.removeItem(key) }
}

Storage.set('user', { id: 1 })
Storage.get('user')                            // { id: 1 }
Storage.get('missing', [])                     // []
```

---

## 11. URL query params (Vue Router)

**Mots-clés** : url, query, route, useRoute, params

**Tout** ce qui vient de `route.query` est une **string**.

```js
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

// Lire — toujours string !
const page = route.query.page                  // "3" (string)
const pageNum = parseInt(route.query.page) || 1
const search = route.query.q || ''

// Lire un param de route
const productId = route.params.id              // "42"

// Écrire dans l'URL
router.push({ query: { ...route.query, page: 3 } })
router.replace({ query: { page: 3 } })         // sans entrée historique
```

### Pattern : sync state ↔ URL

```js
watch(currentPage, (p) => {
  router.replace({ query: { ...route.query, page: p } })
})

watch(() => route.query.page, (p) => {
  currentPage.value = parseInt(p) || 1
})
```

---

## 12. XML PrestaShop — extraction `#text`

**Mots-clés** : XML, fast-xml-parser, #text, @_, parse

`fast-xml-parser` retourne des structures imbriquées. Selon que l'élément a des attributs ou non, l'accès change.

### Structure typique

```xml
<id>42</id>
<id_category xlink:href="..."><![CDATA[3]]></id_category>
```

→

```js
{
  id: 42,                                      // simple si pas d'attribut
  id_category: {
    '#text': '3',
    '@_xlink:href': '...'
  }
}
```

### Helpers

```js
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

txt(obj.id)                                    // "42"
txt(obj.id_category)                           // "3" (extrait #text)
num(obj.price)                                 // 19.99
```

### Élément multilingue

```xml
<name>
  <language id="1"><![CDATA[T-shirt]]></language>
</name>
```

```js
const name = product.name?.language?.['#text']
           ?? product.name?.['#text']
           ?? product.name
           ?? ''
```

### Tableau possible (1 ou plusieurs)

```js
let items = data.prestashop?.products?.product || []
if (!Array.isArray(items)) items = [items]
```

---

## 13. Conversion HT / TTC (prix)

**Mots-clés** : HT, TTC, taxe, prix, conversion

```js
// HT → TTC
const priceTTC = priceHT * (1 + taxRate / 100)

// TTC → HT
const priceHT = priceTTC / (1 + taxRate / 100)

// PrestaShop attend 6 décimales pour le XML
priceHT.toFixed(6)                             // "11.196429"

// Delta combinaison (combination.price est le delta HT)
const deltaTTC = priceComboTTC - priceProductTTC
const deltaHT = deltaTTC / (1 + taxRate / 100)
```

Voir aussi [Service.md — section 7](Service.md#7-conversions-ht--ttc--prix).

---

## 14. ref vs reactive — choisir

**Mots-clés** : ref, reactive, Vue, réactivité

Trois règles :

| Cas | Recommandé |
|-----|-----------|
| Primitive (number, string, boolean) | `ref()` |
| Objet/array que tu remplaceras entièrement | `ref()` |
| Objet aux nombreuses propriétés modifiées indépendamment | `reactive()` (ou `ref()` quand même) |

Recommandation officielle : `ref()` par défaut, pour la cohérence.

```js
// ref
const count = ref(0)
count.value++

const items = ref([])
items.value = [1, 2, 3]                        // remplacement complet OK
items.value.push(4)                            // mutation OK

// reactive (objets uniquement)
const form = reactive({ name: '', email: '' })
form.name = 'Rakoto'                           // pas de .value
// ❌ form = { name: 'X' } — casse la réactivité
```

Voir [RefVue.md](RefVue.md) pour les détails complets.

---

## 15. Anti-patterns à éviter

**Mots-clés** : anti-pattern, erreur, mauvaise pratique

### ❌ 1. `==` au lieu de `===`

```js
'1' == 1                                       // true (coercion)
'1' === 1                                      // false (strict, préféré)
null == undefined                              // true ⚠️
```

→ **Toujours `===` et `!==`**.

### ❌ 2. `parseInt` sans radix

```js
parseInt('08')                                 // 8 (ok en moderne)
parseInt('08', 10)                             // ✅ explicite, sûr partout
```

### ❌ 3. `isNaN(x)` vs `Number.isNaN(x)`

```js
isNaN('abc')                                   // true ⚠️ (convertit en number)
Number.isNaN('abc')                            // ✅ false (strict)
```

### ❌ 4. Modifier un array dans un computed

```js
// ❌ MAUVAIS — mute la source
const sorted = computed(() => items.value.sort())

// ✅ BON — copie d'abord
const sorted = computed(() => [...items.value].sort())
```

### ❌ 5. `Date.parse(s)` avec format non-ISO

```js
Date.parse('19/05/2026')                       // NaN ⚠️
Date.parse('2026-05-19')                       // OK (ISO)
```

→ Parser manuellement les formats locaux.

### ❌ 6. `JSON.parse` sans try/catch

```js
JSON.parse(localStorage.getItem('user'))       // ❌ si invalide → SyntaxError
```

→ Toujours wrapper :

```js
function safeParse(str, fallback = null) {
  try { return str ? JSON.parse(str) : fallback } catch { return fallback }
}
```

### ❌ 7. Comparer Date directement

```js
new Date('2026-05-19') === new Date('2026-05-19')   // false ⚠️ (objets différents)
new Date('2026-05-19').getTime() === new Date('2026-05-19').getTime()   // ✅ true
```

### ❌ 8. Confondre `null` et `undefined`

```js
const obj = { a: null, b: undefined }
obj.a == obj.b                                 // true (coercion)
obj.a === obj.b                                // false (strict)

// Pour tester "vide"
value == null                                  // intentionnel (couvre null + undefined)
```

### ❌ 9. Mutation de props/store directement

```js
// ❌ dans un composant
const props = defineProps(['items'])
props.items.push(item)                         // anti-pattern

// ✅ Emit pour demander au parent de modifier
emit('add-item', item)
```

### ❌ 10. Concaténation au lieu de template literal

```js
'Bonjour ' + name + ', ' + age + ' ans'        // ❌
`Bonjour ${name}, ${age} ans`                  // ✅
```

---

## 16. Helpers réutilisables

**Mots-clés** : helper, utility, composable, reusable

Crée un fichier `src/utils/format.js` :

```js
// Numbers
export const toNum = (s, fallback = 0) => {
  const n = parseFloat(String(s ?? '').replace(',', '.'))
  return Number.isFinite(n) ? n : fallback
}

export const toPercent = (s) => toNum(String(s ?? '').replace('%', ''))

export const round = (n, decimals = 2) => {
  const f = Math.pow(10, decimals)
  return Math.round(n * f) / f
}

// Dates
export const parseDate = (s) => {
  if (!s) return new Date().toISOString().split('T')[0]
  const match = String(s).trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!match) throw new Error(`Date invalide : "${s}"`)
  const [, d, m, y] = match
  return `${y}-${m}-${d}`
}

export const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.substring(0, 10).split('-')
  return `${d}/${m}/${y}`
}

export const formatTime = (dateTimeStr) => {
  const time = String(dateTimeStr).split(' ')[1] || ''
  return time.substring(0, 5)
}

export const nowPrestaShop = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
       + `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// Money
export const htToTtc = (ht, rate) => ht * (1 + rate / 100)
export const ttcToHt = (ttc, rate) => ttc / (1 + rate / 100)
export const formatEUR = (n) =>
  (Number(n) || 0).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' })

// Storage
export const Storage = {
  set(key, value) {
    localStorage.setItem(key, JSON.stringify(value))
  },
  get(key, fallback = null) {
    const raw = localStorage.getItem(key)
    if (raw === null) return fallback
    try { return JSON.parse(raw) } catch { return raw }
  },
  remove(key) { localStorage.removeItem(key) }
}

// JSON safe
export const safeParse = (str, fallback = null) => {
  try { return str ? JSON.parse(str) : fallback } catch { return fallback }
}
```

Usage :

```js
import { toNum, formatDate, formatEUR, Storage } from '@/utils/format'

const price = toNum('12,5')                    // 12.5
const date = formatDate('2026-05-19')          // "19/05/2026"
const display = formatEUR(1234.56)             // "1 234,56 €"
Storage.set('selectedCustomer', { id: 1 })
```

---

## 17. Templates copy-paste

**Mots-clés** : template, snippet, copy-paste

### 17.1 Form avec validation et conversion

```vue
<template>
  <form @submit.prevent="onSubmit">
    <input v-model.trim="form.name" placeholder="Nom" required />
    <input v-model="form.email" type="email" placeholder="Email" required />
    <input v-model.number="form.age" type="number" min="18" />
    <input v-model="form.priceStr" placeholder="Prix (ex: 12,50)" />
    <input v-model="form.date" type="date" />

    <button type="submit" :disabled="!canSubmit">Envoyer</button>
  </form>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { toNum } from '@/utils/format'

const form = reactive({
  name: '',
  email: '',
  age: 18,
  priceStr: '',
  date: new Date().toISOString().split('T')[0],
})

const price = computed(() => toNum(form.priceStr))

const canSubmit = computed(() =>
  form.name.length > 0 &&
  /^.+@.+\..+$/.test(form.email) &&
  form.age >= 18 &&
  price.value > 0 &&
  form.date.length > 0
)

const onSubmit = async () => {
  const payload = {
    name: form.name,
    email: form.email,
    age: form.age,
    price: price.value,                        // number, pas la string
    date: form.date,                           // déjà YYYY-MM-DD
  }
  await Service.save(payload)
}
</script>
```

### 17.2 Affichage de données mixtes

```vue
<template>
  <table>
    <tr v-for="item in items" :key="item.id">
      <td>{{ formatDate(item.date_add) }}</td>
      <td>{{ formatTime(item.date_add) }}</td>
      <td>{{ formatEUR(item.total_paid) }}</td>
      <td>{{ item.reference?.toUpperCase() || '—' }}</td>
      <td :class="item.active === 1 ? 'green' : 'red'">
        {{ item.active === 1 ? '✓' : '✗' }}
      </td>
    </tr>
  </table>
</template>

<script setup>
import { formatDate, formatTime, formatEUR } from '@/utils/format'
</script>
```

### 17.3 Tri d'une liste

```vue
<script setup>
import { ref, computed } from 'vue'

const items = ref([])
const sortKey = ref('date_add')
const sortDir = ref('desc')

const sortedItems = computed(() => {
  return [...items.value].sort((a, b) => {
    const av = a[sortKey.value]
    const bv = b[sortKey.value]
    let diff
    if (typeof av === 'number') diff = av - bv
    else diff = String(av).localeCompare(String(bv))
    return sortDir.value === 'asc' ? diff : -diff
  })
})

const toggleSort = (key) => {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = 'asc'
  }
}
</script>
```

---

## 18. Cheatsheet

**Mots-clés** : cheatsheet, antisèche, résumé

```
╔══════════════════════════════════════════════════════════════╗
║         MANIPULATION DE VARIABLES — RAPPELS RAPIDES          ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  NUMBERS                                                     ║
║    parseInt(s, 10)        → entier                           ║
║    parseFloat(s)          → décimal (mais '12,5' = 12)       ║
║    (n).toFixed(2)         → string '12.50'                   ║
║    Number.isNaN(x)        → strict NaN check                 ║
║                                                              ║
║  STRINGS                                                     ║
║    .trim() .toLowerCase() .replace(',', '.')                 ║
║    .split('/') .padStart(2, '0')                             ║
║    `template ${var}`                                         ║
║                                                              ║
║  DATES                                                       ║
║    new Date()                                                ║
║    .toISOString().split('T')[0]   → 'YYYY-MM-DD'             ║
║    DD/MM/YYYY → split('/'), inverser                         ║
║                                                              ║
║  ARRAYS                                                      ║
║    .map(fn) .filter(fn) .reduce(fn, init)                    ║
║    .find(fn) .some(fn) .every(fn)                            ║
║    [...arr, x]            → ajouter immutable                ║
║    arr.sort((a, b) => a - b)  → numérique                    ║
║                                                              ║
║  OBJECTS                                                     ║
║    { ...obj, key: value }                                    ║
║    const { a, b } = obj                                      ║
║    Object.keys / values / entries                            ║
║                                                              ║
║  BOOLEAN                                                     ║
║    Falsy : false, 0, '', null, undefined, NaN                ║
║    ??                     → 1er non-nullish                  ║
║    ||                     → 1er truthy                       ║
║    obj?.prop?.method?.()                                     ║
║                                                              ║
║  V-MODEL                                                     ║
║    .number   → conversion auto                               ║
║    .trim     → trim auto                                     ║
║    .lazy     → update sur blur                               ║
║                                                              ║
║  LOCALSTORAGE                                                ║
║    setItem(key, JSON.stringify(obj))                         ║
║    JSON.parse(getItem(key) ?? '{}')                          ║
║                                                              ║
║  XML PRESTASHOP                                              ║
║    obj.field?.['#text'] ?? obj.field ?? ''                   ║
║    obj.name?.language?.['#text']                             ║
║                                                              ║
║  COMPARAISONS                                                ║
║    ===  pas ==                                               ║
║    Date.getTime() pour comparer                              ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Types JS | `Types JavaScript` |
| parseFloat / toFixed | `Numbers` |
| String manipulation | `Strings` |
| Manipuler une date | `Dates` |
| Map / filter / reduce | `Arrays` |
| Destructuring / spread | `Objects` |
| Truthy / falsy / ?? / \|\| | `Booléens` |
| v-model avec number | `Formulaires Vue` |
| Format français (12,5) | `Formats français` |
| localStorage | `localStorage` |
| URL query | `URL query params` |
| XML PrestaShop `#text` | `XML PrestaShop` |
| HT / TTC | `Conversion HT` |
| ref vs reactive | `ref vs reactive` |
| Erreurs courantes | `Anti-patterns` |
| Helpers à copier | `Helpers réutilisables` |
| Code prêt | `Templates copy-paste` |
| Rappel express | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |
| Pagination | Voir [`listePagination.md`](listePagination.md) |
| POST / PUT | Voir [`PutPost.md`](PutPost.md) |

---

## 🗂️ Mapping types ↔ contextes du projet

| Type / contexte | Où dans le projet |
|-----------------|-------------------|
| Date DD/MM/YYYY | CSV file 1 / file 3 — `parseDate` dans importService |
| Number avec virgule | CSV — `parseFrenchNumber` dans importService |
| % avec virgule | CSV — `parseFrenchPercent` dans importService |
| XML `#text` | toutes les réponses PrestaShop dans les Services |
| localStorage JSON | `selectedCustomer`, `cart_*`, `employee` |
| URL query | (à implémenter pour pagination) |
| Form .number | StockManagement (quantités), DashboardView |
| Date toLocaleDateString | OrderList, MyOrders, DashboardView |
| Array sort | OrderList (tri par date) |
| Array .filter | ProductList (multi-critères) |
| Array .reduce | totalPrice (cartStore) |
