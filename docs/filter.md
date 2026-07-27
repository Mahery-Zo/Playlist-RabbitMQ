# 🔍 Guide complet : Filtres dans ce projet

> **Mots-clés généraux** : filter, filtre, search, recherche, computed, multi-critères, debounce, reset, PrestaShop, filter[field], URL, query, watch, sort, find, some, every, includes

---

## 📑 Index

- [1. Concept : filtrer côté client vs côté API](#1-concept--filtrer-côté-client-vs-côté-api)
- [2. Filtre simple sur un array (computed)](#2-filtre-simple-sur-un-array-computed)
- [3. Filtre par recherche (text search)](#3-filtre-par-recherche-text-search)
- [4. Filtres multi-critères combinés](#4-filtres-multi-critères-combinés)
- [5. Filtre + tri](#5-filtre--tri)
- [6. Filtre côté serveur PrestaShop (`filter[field]=value`)](#6-filtre-côté-serveur-prestashop-filterfieldvalue)
- [7. Reset des filtres → retour à la page 1](#7-reset-des-filtres--retour-à-la-page-1)
- [8. Debounce (saisie de recherche)](#8-debounce-saisie-de-recherche)
- [9. Filtre URL-synchronisé (deep link)](#9-filtre-url-synchronisé-deep-link)
- [10. find, some, every — quand utiliser quoi](#10-find-some-every--quand-utiliser-quoi)
- [11. Filtres côté Service — patterns avancés](#11-filtres-côté-service--patterns-avancés)
- [12. Filtres pour des données XML PrestaShop](#12-filtres-pour-des-données-xml-prestashop)
- [13. Filtres dans ce projet — récap par vue](#13-filtres-dans-ce-projet--récap-par-vue)
- [14. Composable `useFilters`](#14-composable-usefilters)
- [15. Anti-patterns à éviter](#15-anti-patterns-à-éviter)
- [16. Templates copy-paste](#16-templates-copy-paste)
- [17. Cheatsheet](#17-cheatsheet)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Concept : filtrer côté client vs côté API

**Mots-clés** : client, serveur, choisir, stratégie

| | Côté **client** | Côté **API PrestaShop** |
|---|------|------|
| Quand | Liste **< 500 éléments** déjà chargée | Liste **volumineuse** ou filtre rare |
| Implémentation | `computed` avec `.filter(...)` | `?filter[field]=value` dans l'URL |
| Réactivité | Instantanée (mémoire) | Re-fetch nécessaire |
| Recherche partielle | `includes()` trivial | Plus limité côté PrestaShop |

**Pour ce projet** : la plupart des listes sont < 500 → **côté client** suffit. PrestaShop garde quand même son utilité pour filtrer par ID (clients, commandes…).

---

## 2. Filtre simple sur un array (computed)

**Mots-clés** : computed, filter, simple, basique

Pattern de base : un ref pour les critères, un computed dérivé pour le résultat.

```js
import { ref, computed } from 'vue'

const products = ref([])
const onlyActive = ref(true)

const filteredProducts = computed(() => {
  return products.value.filter(p => !onlyActive.value || p.active === 1)
})
```

**Avantage** : le filtre se recalcule automatiquement dès que `products` ou `onlyActive` change. Pas de manuel `recompute()` à appeler.

---

## 3. Filtre par recherche (text search)

**Mots-clés** : search, recherche, includes, toLowerCase, fuzzy

### Recherche simple (insensible à la casse)

```js
const searchQuery = ref('')

const filtered = computed(() => {
  if (!searchQuery.value) return items.value
  const q = searchQuery.value.toLowerCase().trim()
  return items.value.filter(item =>
    item.name.toLowerCase().includes(q)
  )
})
```

### Recherche multi-champs

```js
const filtered = computed(() => {
  if (!searchQuery.value) return items.value
  const q = searchQuery.value.toLowerCase().trim()
  return items.value.filter(item =>
    item.name.toLowerCase().includes(q) ||
    item.reference.toLowerCase().includes(q) ||
    String(item.id).includes(q)
  )
})
```

### Recherche tous les champs (générique)

```js
const filtered = computed(() => {
  if (!searchQuery.value) return items.value
  const q = searchQuery.value.toLowerCase().trim()
  return items.value.filter(item =>
    Object.values(item).some(v =>
      String(v).toLowerCase().includes(q)
    )
  )
})
```

### Recherche avec mots multiples (chacun doit matcher)

```js
const filtered = computed(() => {
  const words = searchQuery.value.toLowerCase().trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return items.value
  return items.value.filter(item => {
    const txt = `${item.name} ${item.reference}`.toLowerCase()
    return words.every(w => txt.includes(w))
  })
})
```

---

## 4. Filtres multi-critères combinés

**Mots-clés** : multi-critères, combiner, search, category, range, prix

Pattern extrait de **ProductList.vue** : plusieurs critères qui s'additionnent (AND).

```vue
<template>
  <div class="filters">
    <input v-model="filters.search" placeholder="Recherche..." />

    <select v-model="filters.category">
      <option value="">Toutes catégories</option>
      <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
    </select>

    <input v-model.number="filters.priceMin" type="number" placeholder="Prix min" />
    <input v-model.number="filters.priceMax" type="number" placeholder="Prix max" />

    <label>
      <input type="checkbox" v-model="filters.onlyActive" /> Actifs uniquement
    </label>
  </div>

  <div v-for="p in filteredProducts" :key="p.id">{{ p.name }}</div>
  <p>{{ filteredProducts.length }} résultat(s)</p>
</template>

<script setup>
import { ref, computed } from 'vue'

const products = ref([])

const filters = ref({
  search: '',
  category: '',
  priceMin: null,
  priceMax: null,
  onlyActive: false,
})

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    // Filtre recherche
    if (filters.value.search) {
      const q = filters.value.search.toLowerCase()
      if (!p.name.toLowerCase().includes(q)) return false
    }

    // Filtre catégorie
    if (filters.value.category && p.id_category !== filters.value.category) {
      return false
    }

    // Filtre prix
    if (filters.value.priceMin != null && p.price < filters.value.priceMin) return false
    if (filters.value.priceMax != null && p.price > filters.value.priceMax) return false

    // Filtre actif
    if (filters.value.onlyActive && p.active !== 1) return false

    return true
  })
})
</script>
```

**Pattern clé** : `return false` dès qu'un critère ne passe pas → **early exit** lisible.

---

## 5. Filtre + tri

**Mots-clés** : sort, ordre, tri, combiner filter

```js
const filteredAndSorted = computed(() => {
  let result = products.value.filter(/* critères */)

  // Trier (copie pour ne pas muter la source)
  result = [...result].sort((a, b) => {
    if (sortKey.value === 'name') {
      return a.name.localeCompare(b.name) * (sortDir.value === 'asc' ? 1 : -1)
    }
    if (sortKey.value === 'price') {
      return (a.price - b.price) * (sortDir.value === 'asc' ? 1 : -1)
    }
    return 0
  })

  return result
})
```

⚠️ **Toujours `[...arr].sort()`** pour ne pas muter le tableau source (qui peut être réactif).

---

## 6. Filtre côté serveur PrestaShop (`filter[field]=value`)

**Mots-clés** : PrestaShop, webservice, filter[, server-side

PrestaShop accepte des filtres directement dans l'URL :

```
GET /api/products?filter[active]=1
GET /api/products?filter[id_category_default]=3
GET /api/customers?filter[email]=rakoto@mail.com
GET /api/orders?filter[id_customer]=5&filter[current_state]=11
```

⚠️ Encoder l'`@` : `encodeURIComponent(email)`.

### Pattern dans un Service

```js
async getOrdersByCustomer(customerId) {
  const res = await axios.get(
    `/api/orders?filter[id_customer]=${customerId}&display=full&sort=[id_DESC]`
  )
  return parser.parse(res.data)
}
```

### Filtres combinés

```js
const url = `/api/orders?filter[id_customer]=${id}&filter[current_state]=11&display=full`
```

PrestaShop fait un **AND** par défaut entre les filtres.

### Opérateurs spéciaux

```
filter[id]=[5,12]              → IN (5, 12)
filter[date_add]=[2026-01-01,2026-12-31]   → BETWEEN
filter[name]=[Rakoto]%         → LIKE (préfixe)
filter[name]=%[Ra]ko%          → LIKE (contient)
```

### Combinaison avec `display`

```
?display=full                   → tous les champs
?display=[id,reference]         → seulement id et reference
```

---

## 7. Reset des filtres → retour à la page 1

**Mots-clés** : reset, page 1, watch, filtre, pagination

Quand on a une pagination, **toujours** reset la page à 1 dès qu'un filtre change :

```js
import { watch } from 'vue'

watch(
  () => [filters.value.search, filters.value.category, filters.value.priceMin],
  () => { currentPage.value = 1 }
)
```

Ou plus simple avec deep watch :

```js
watch(filters, () => { currentPage.value = 1 }, { deep: true })
```

**Pourquoi c'est important** : si l'utilisateur est sur la page 5 d'une liste à 10 pages, et qu'il filtre pour ne plus avoir que 2 pages, il se retrouve sur une page vide.

### Bouton "Reset filtres"

```vue
<button @click="resetFilters">🔄 Réinitialiser</button>

<script setup>
const defaultFilters = {
  search: '',
  category: '',
  priceMin: null,
  priceMax: null,
  onlyActive: false,
}

const filters = ref({ ...defaultFilters })

const resetFilters = () => {
  filters.value = { ...defaultFilters }
}
</script>
```

---

## 8. Debounce (saisie de recherche)

**Mots-clés** : debounce, search, optimization, performance

Pour éviter de recalculer à chaque touche tapée (utile si le filtre est coûteux ou s'il fait un fetch API) :

### Helper debounce minimaliste

```js
function debounce(fn, ms = 300) {
  let timer
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), ms)
  }
}
```

### Usage dans une vue

```vue
<input v-model="searchInput" />

<script setup>
import { ref, watch } from 'vue'

const searchInput = ref('')           // ce qui est tapé
const searchQuery = ref('')           // ce qui est utilisé pour filtrer (différé)

const update = debounce((v) => {
  searchQuery.value = v
}, 300)

watch(searchInput, (v) => update(v))

// Le filteredProducts utilise searchQuery (et pas searchInput)
const filteredProducts = computed(() => /* ... uses searchQuery */)
</script>
```

**Quand l'utiliser ?**
- ✅ Recherche qui déclenche un fetch API
- ✅ Liste très grosse (>1000 éléments) où le filtre client est lent
- ❌ Liste de quelques dizaines d'items → inutile, le filtre est instantané

---

## 9. Filtre URL-synchronisé (deep link)

**Mots-clés** : URL, query, vue-router, deep link, partager

Permet de partager un lien avec filtres pré-appliqués.

```vue
<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const search = ref(route.query.search || '')
const category = ref(route.query.cat || '')

// Pousser dans l'URL quand les filtres changent
watch([search, category], ([s, c]) => {
  router.replace({
    query: { ...route.query, search: s || undefined, cat: c || undefined }
  })
})
</script>
```

URL résultante : `/products?search=tshirt&cat=3`

---

## 10. find, some, every — quand utiliser quoi

**Mots-clés** : find, some, every, includes, performance

| Méthode | Retourne | Quand utiliser |
|---------|----------|----------------|
| `.find(fn)` | **1er élément** qui passe ou `undefined` | Tu veux UN objet précis |
| `.findIndex(fn)` | Index du premier ou `-1` | Pour modifier/supprimer à un index |
| `.some(fn)` | `true` si **au moins un** passe | Test booléen "y a-t-il …" |
| `.every(fn)` | `true` si **tous** passent | Test booléen "sont-ils tous …" |
| `.filter(fn)` | **Tableau** de tous ceux qui passent | Liste filtrée |
| `.includes(v)` | `true` si la valeur exacte est dedans | Recherche dans un array de primitives |

### Exemples

```js
const products = [{ id: 1, active: 1 }, { id: 2, active: 0 }]

products.find(p => p.id === 2)             // { id: 2, active: 0 }
products.find(p => p.id === 99)            // undefined

products.some(p => p.active === 1)         // true
products.every(p => p.active === 1)        // false

[1, 2, 3].includes(2)                       // true
['a', 'b'].includes('c')                   // false

// Pour les objets, includes ne marche pas pour comparaison "par valeur"
products.includes({ id: 1, active: 1 })    // false ⚠️ (compare par référence)
```

---

## 11. Filtres côté Service — patterns avancés

**Mots-clés** : service, async, fetch, helper

### Helper paramétré pour appels filtrés

```js
async function getList(resource, itemKey, filter = '') {
  const res = await axios.get(`/api/${resource}?display=full${filter}`)
  const data = parser.parse(res.data)
  let items = data.prestashop?.[resource]?.[itemKey] || []
  if (!Array.isArray(items)) items = [items]
  return items
}

// Usage
const products = await getList('products', 'product')
const productsActive = await getList('products', 'product', '&filter[active]=1')
const productsCat = await getList('products', 'product', `&filter[id_category_default]=${catId}`)
```

### Filtre par email (avec encoding)

```js
const safeEmail = encodeURIComponent(email)
const url = `/api/customers?filter[email]=${safeEmail}&display=full`
```

### Filtre + tri + limite côté serveur

```js
const res = await axios.get(
  `/api/orders?filter[id_customer]=${id}&display=full&sort=[id_DESC]&limit=10`
)
```

---

## 12. Filtres pour des données XML PrestaShop

**Mots-clés** : XML, #text, parser, filter sur données XML

Les valeurs PrestaShop sont dans `#text`. Pour filtrer un tableau d'éléments parsés :

```js
const txt = (v) => v?.['#text'] ?? v ?? ''
const num = (v) => parseFloat(txt(v)) || 0

const activeCustomers = customers.filter(c => parseInt(txt(c.active)) === 1)
const expensiveProducts = products.filter(p => num(p.price) > 100)
const paidOrders = orders.filter(o => {
  const state = parseInt(txt(o.current_state))
  return state === 11 || state === 5
})
```

⚠️ Toujours **convertir** (parseInt / parseFloat) avant de comparer numérique. `'11' === 11` est `false`.

---

## 13. Filtres dans ce projet — récap par vue

**Mots-clés** : projet, vues, exemples, ProductList

| Vue / composant | Filtres présents |
|-----------------|------------------|
| [ProductList.vue](../NewApp/src/views/FrontOffice/ProductList.vue) | recherche, catégorie, prix min/max, marques |
| [OrderList.vue](../NewApp/src/components/OrderList.vue) | filtré sur états 11/5/6 |
| [DashboardView.vue](../NewApp/src/views/BackOffice/DashboardView.vue) | états payés (11) + livrés (5) |
| [MyOrders.vue](../NewApp/src/views/FrontOffice/MyOrders.vue) | par `id_customer` (filtre serveur PrestaShop) |
| [StockManagement.vue](../NewApp/src/views/BackOffice/StockManagement.vue) | (pas de filtre, à ajouter possiblement) |

---

## 14. Composable `useFilters`

**Mots-clés** : composable, factoriser, useFilters

Pour ne pas dupliquer la logique de filtres :

```js
// src/composables/useFilters.js
import { ref, computed, watch } from 'vue'

/**
 * @param {Ref<Array>} sourceRef - le tableau source
 * @param {Object} predicates - { key: (item, value) => boolean }
 * @param {Object} defaults - valeurs initiales des filtres
 * @returns { filters, filtered, reset }
 */
export function useFilters(sourceRef, predicates, defaults = {}) {
  const filters = ref({ ...defaults })

  const filtered = computed(() => {
    return (sourceRef.value || []).filter(item => {
      for (const [key, fn] of Object.entries(predicates)) {
        const value = filters.value[key]
        if (value === '' || value == null) continue   // critère ignoré
        if (!fn(item, value)) return false
      }
      return true
    })
  })

  const reset = () => { filters.value = { ...defaults } }

  return { filters, filtered, reset }
}
```

### Usage

```vue
<script setup>
import { ref } from 'vue'
import { useFilters } from '@/composables/useFilters'

const products = ref([])

const { filters, filtered, reset } = useFilters(
  products,
  {
    // critères : key → fn(item, value)
    search:    (p, v) => p.name.toLowerCase().includes(v.toLowerCase()),
    category:  (p, v) => p.id_category_default === v,
    priceMin:  (p, v) => p.price >= v,
    priceMax:  (p, v) => p.price <= v,
    active:    (p, v) => p.active === (v ? 1 : 0),
  },
  // défauts
  { search: '', category: '', priceMin: null, priceMax: null, active: false }
)
</script>

<template>
  <input v-model="filters.search" placeholder="Recherche" />
  <select v-model="filters.category">...</select>
  <button @click="reset">Reset</button>

  <div v-for="p in filtered" :key="p.id">{{ p.name }}</div>
</template>
```

---

## 15. Anti-patterns à éviter

**Mots-clés** : anti-pattern, erreur

### ❌ 1. Filtrer dans une méthode au lieu d'un computed

```js
// ❌ MAUVAIS — re-filtre à chaque accès, pas mémoïsé
const getFiltered = () => items.value.filter(/* ... */)
```

```js
// ✅ BON — computed est mémoïsé
const filtered = computed(() => items.value.filter(/* ... */))
```

### ❌ 2. Modifier le tableau source dans filter()

```js
// ❌ MAUVAIS — un computed ne doit JAMAIS muter sa source
const filtered = computed(() => {
  items.value.push(...)        // ❌ side-effect
  return items.value.filter(/* ... */)
})
```

### ❌ 3. Trier sans copier (mute la source)

```js
items.value.sort((a, b) => ...)        // ❌ mute le ref source
[...items.value].sort((a, b) => ...)   // ✅ copie d'abord
```

### ❌ 4. Comparer string et number sans convertir

```js
// ❌ Toujours false (types différents en strict)
orders.filter(o => o.current_state === 11)   // o.current_state est "11" (string)
```

```js
// ✅
orders.filter(o => parseInt(o.current_state) === 11)
```

### ❌ 5. Oublier de reset la page après filtre

```js
filters.value.search = 'XL'   // ❌ on reste sur la page 5
// → ajouter watch(filters, () => currentPage.value = 1)
```

### ❌ 6. Filtrer côté client une grosse liste fetchée d'un coup

Pour 10 000 éléments, charger TOUT puis filtrer en mémoire = lent + consomme RAM/bande passante.
→ Utiliser les filtres serveur PrestaShop (`?filter[field]=value`).

### ❌ 7. Recalculer un filter dans un v-for

```vue
<!-- ❌ recalcule à chaque render -->
<div v-for="item in items.filter(i => i.active === 1)" :key="item.id">
```

```vue
<!-- ✅ via computed -->
<div v-for="item in activeItems" :key="item.id">
<!-- avec const activeItems = computed(() => items.value.filter(...)) -->
```

### ❌ 8. Mauvais opérateur logique

```js
// ❌ "name doit contenir search ET category" — mais on a fait OR
items.filter(i => i.name.includes(q) || i.id_category === cat)
```

```js
// ✅ AND explicite
items.filter(i => i.name.includes(q) && i.id_category === cat)
```

---

## 16. Templates copy-paste

**Mots-clés** : template, copy-paste, boilerplate

### 16.1 Filtre simple avec recherche

```vue
<template>
  <input v-model="search" placeholder="Rechercher..." />
  <div v-for="item in filtered" :key="item.id">{{ item.name }}</div>
  <p v-if="filtered.length === 0">Aucun résultat</p>
</template>

<script setup>
import { ref, computed } from 'vue'

const items = ref([])
const search = ref('')

const filtered = computed(() => {
  if (!search.value) return items.value
  const q = search.value.toLowerCase().trim()
  return items.value.filter(i =>
    i.name.toLowerCase().includes(q)
  )
})
</script>
```

### 16.2 Filtres multi-critères + reset

```vue
<template>
  <div class="filters">
    <input v-model="filters.search" placeholder="Recherche" />
    <select v-model="filters.category">
      <option value="">Toutes</option>
      <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
    </select>
    <input v-model.number="filters.priceMin" type="number" placeholder="Prix min" />
    <input v-model.number="filters.priceMax" type="number" placeholder="Prix max" />
    <button @click="resetFilters">🔄 Reset</button>
  </div>

  <p>{{ filtered.length }} résultat(s)</p>
  <div v-for="item in filtered" :key="item.id">{{ item.name }}</div>
</template>

<script setup>
import { ref, computed } from 'vue'

const items = ref([])
const categories = ref([])

const defaultFilters = {
  search: '',
  category: '',
  priceMin: null,
  priceMax: null,
}

const filters = ref({ ...defaultFilters })

const filtered = computed(() => {
  return items.value.filter(i => {
    if (filters.value.search) {
      const q = filters.value.search.toLowerCase()
      if (!i.name.toLowerCase().includes(q)) return false
    }
    if (filters.value.category && i.id_category !== filters.value.category) return false
    if (filters.value.priceMin != null && i.price < filters.value.priceMin) return false
    if (filters.value.priceMax != null && i.price > filters.value.priceMax) return false
    return true
  })
})

const resetFilters = () => {
  filters.value = { ...defaultFilters }
}
</script>
```

### 16.3 Filtre + tri + pagination

```vue
<script setup>
import { ref, computed, watch } from 'vue'

const items = ref([])
const search = ref('')
const sortBy = ref('name')
const sortDir = ref('asc')
const currentPage = ref(1)
const pageSize = ref(10)

// 1. Filtrer
const filtered = computed(() => {
  if (!search.value) return items.value
  const q = search.value.toLowerCase()
  return items.value.filter(i => i.name.toLowerCase().includes(q))
})

// 2. Trier
const sorted = computed(() => {
  return [...filtered.value].sort((a, b) => {
    let diff
    if (typeof a[sortBy.value] === 'number') {
      diff = a[sortBy.value] - b[sortBy.value]
    } else {
      diff = String(a[sortBy.value]).localeCompare(String(b[sortBy.value]))
    }
    return sortDir.value === 'asc' ? diff : -diff
  })
})

// 3. Paginer
const totalPages = computed(() => Math.max(1, Math.ceil(sorted.value.length / pageSize.value)))
const paginated = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return sorted.value.slice(start, start + pageSize.value)
})

// Reset à la page 1 quand filtre change
watch([search, sortBy, sortDir], () => { currentPage.value = 1 })
</script>
```

### 16.4 Filtre côté API PrestaShop

```js
// services/MyService.js
import axios from '../config/axios'
import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })

export const MyService = {
  async filterOrders({ customerId, state, dateFrom, dateTo }) {
    const params = []
    if (customerId) params.push(`filter[id_customer]=${customerId}`)
    if (state)      params.push(`filter[current_state]=${state}`)
    if (dateFrom && dateTo) {
      params.push(`filter[date_add]=[${dateFrom},${dateTo}]`)
    }
    params.push('display=full', 'sort=[id_DESC]')

    const url = `/api/orders?${params.join('&')}`
    const res = await axios.get(url)
    const data = parser.parse(res.data)
    let orders = data.prestashop?.orders?.order || []
    if (!Array.isArray(orders)) orders = [orders]
    return orders
  }
}
```

---

## 17. Cheatsheet

**Mots-clés** : cheatsheet, mémo, antisèche, rappel

```
╔══════════════════════════════════════════════════════════════╗
║                    FILTRES — RAPPELS                         ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  COMPUTED FILTER                                             ║
║    const filtered = computed(() =>                           ║
║      items.value.filter(item => /* critères */)              ║
║    )                                                         ║
║                                                              ║
║  RECHERCHE INSENSIBLE CASSE                                  ║
║    item.name.toLowerCase().includes(query.toLowerCase())     ║
║                                                              ║
║  MULTI-CRITÈRES (AND)                                        ║
║    if (!criteria1) return false                              ║
║    if (!criteria2) return false                              ║
║    return true                                               ║
║                                                              ║
║  TRI APRÈS FILTRE                                            ║
║    [...filtered.value].sort((a, b) => a.price - b.price)     ║
║                                                              ║
║  RESET PAGE AU CHANGEMENT DE FILTRE                          ║
║    watch(filters, () => { currentPage.value = 1 }, deep)     ║
║                                                              ║
║  FILTRE API PRESTASHOP                                       ║
║    ?filter[field]=value                                      ║
║    ?filter[id]=[1,2,3]              IN                       ║
║    ?filter[date]=[A,B]              BETWEEN                  ║
║                                                              ║
║  DEBOUNCE RECHERCHE                                          ║
║    const update = debounce((v) => searchQuery.value = v,300) ║
║                                                              ║
║  URL SYNC                                                    ║
║    router.replace({ query: { ...route.query, search: s } })  ║
║                                                              ║
║  COMPARER STRING/NUMBER                                      ║
║    parseInt(x) === N   (pas x === N pour XML string)         ║
║                                                              ║
║  TRI IMMUTABLE                                               ║
║    [...arr].sort()    (ne mute pas la source)                ║
║                                                              ║
║  find / some / every                                         ║
║    .find(fn)    → 1 élément ou undefined                     ║
║    .some(fn)    → true/false : au moins un                   ║
║    .every(fn)   → true/false : tous                          ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Choisir client vs serveur | `client vs côté API` |
| Filtre basique | `Filtre simple` |
| Recherche par texte | `Filtre par recherche` |
| Plusieurs critères AND | `Filtres multi-critères` |
| Trier + filtrer | `Filtre + tri` |
| API PrestaShop | `filter[field]` |
| Reset à la page 1 | `Reset des filtres` |
| Debounce | `Debounce` |
| URL deep link | `URL-synchronisé` |
| find vs some vs every | `find, some, every` |
| Service avec filtre | `côté Service` |
| Données XML | `XML PrestaShop` |
| Exemples du projet | `récap par vue` |
| Composable | `useFilters` |
| Erreurs à éviter | `Anti-patterns` |
| Code prêt | `Templates copy-paste` |
| Rappel express | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |
| Pagination | Voir [`listePagination.md`](listePagination.md) |
| POST / PUT | Voir [`PutPost.md`](PutPost.md) |
| Variables / types | Voir [`variable.md`](variable.md) |
