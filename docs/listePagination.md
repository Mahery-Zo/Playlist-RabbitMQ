# 📃 Guide complet : Pagination dans Vue 3 + PrestaShop

> **Mots-clés généraux** : pagination, page, pageSize, offset, limit, prev, next, scroll infini, infinite scroll, load more, slice, filter, search, computed, ref, axios, PrestaShop webservice, performance, lazy, ?display=full, ?limit=, ?offset=

---

## 📑 Index

- [1. Pourquoi paginer ?](#1-pourquoi-paginer-)
- [2. Pagination côté client vs côté serveur](#2-pagination-côté-client-vs-côté-serveur)
- [3. État (state) d'une pagination](#3-état-state-dune-pagination)
- [4. Pagination côté client (slice sur un tableau)](#4-pagination-côté-client-slice-sur-un-tableau)
- [5. Pagination côté serveur (PrestaShop : `limit` & `offset`)](#5-pagination-côté-serveur-prestashop--limit--offset)
- [6. UI : prev / next, numéros de pages, ellipses](#6-ui--prev--next-numéros-de-pages-ellipses)
- [7. "Load more" (charger plus)](#7-load-more-charger-plus)
- [8. Scroll infini (IntersectionObserver)](#8-scroll-infini-intersectionobserver)
- [9. Search + pagination combinés](#9-search--pagination-combinés)
- [10. Réinitialisation des filtres → retour à la page 1](#10-réinitialisation-des-filtres--retour-à-la-page-1)
- [11. URL-synchronization (page dans l'URL)](#11-url-synchronization-page-dans-lurl)
- [12. Pagination dans le contexte de ce projet](#12-pagination-dans-le-contexte-de-ce-projet)
- [13. Composable réutilisable `usePagination`](#13-composable-réutilisable-usepagination)
- [14. Composant réutilisable `<Pagination />`](#14-composant-réutilisable-pagination-)
- [15. Anti-patterns à éviter](#15-anti-patterns-à-éviter)
- [16. Templates de code complets](#16-templates-de-code-complets)
- [17. Cheatsheet rapide](#17-cheatsheet-rapide)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Pourquoi paginer ?

**Mots-clés** : performance, UX, DOM, scroll, mémoire, raison, motivation

Sans pagination, afficher 1 000 produits :
- Charge **1 000 lignes DOM** → ralentit le rendu
- Force l'utilisateur à scroller sans fin pour trouver
- Mémoire JS et bande passante consommées inutilement

Avec pagination :
- N éléments par page (typiquement 10-50)
- Navigation rapide (prev/next ou clic direct sur la page N)
- Le DOM ne contient que la page visible

Règle pratique : dès qu'une liste dépasse **20-30 lignes**, paginer.

---

## 2. Pagination côté client vs côté serveur

**Mots-clés** : client, serveur, slice, fetch, choisir

| | Côté **client** | Côté **serveur** |
|---|------|------|
| Chargement initial | Tout récupéré d'un coup | Une page à la fois |
| Navigation | Instantanée (slice JS) | Re-fetch à chaque changement |
| Recommandé pour | Liste **< 500 éléments** | Liste **> 500** ou volume inconnu |
| Tri / filtre | En mémoire (rapide) | Re-fetch (lent mais scalable) |
| Code | Simple (un `computed`) | Plus de logique async |

**Pour ce projet** : la plupart des listes (produits, commandes, paniers) ont **< 200 éléments** → la pagination côté client suffit largement.

---

## 3. État (state) d'une pagination

**Mots-clés** : state, ref, currentPage, pageSize, totalPages, computed

Trois variables minimum :

```js
const items = ref([])         // toutes les données (chargées une fois)
const currentPage = ref(1)    // page actuelle (1-indexed)
const pageSize = ref(10)      // nombre d'éléments par page
```

Trois `computed` dérivés :

```js
import { computed } from 'vue'

const totalPages = computed(() => Math.ceil(items.value.length / pageSize.value))

const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return items.value.slice(start, start + pageSize.value)
})

const canPrev = computed(() => currentPage.value > 1)
const canNext = computed(() => currentPage.value < totalPages.value)
```

Actions :

```js
const prevPage = () => { if (canPrev.value) currentPage.value-- }
const nextPage = () => { if (canNext.value) currentPage.value++ }
const goToPage = (p) => {
  if (p >= 1 && p <= totalPages.value) currentPage.value = p
}
```

---

## 4. Pagination côté client (slice sur un tableau)

**Mots-clés** : slice, computed, frontend, mémoire

### Exemple complet

```vue
<template>
  <div>
    <table>
      <tr v-for="item in paginatedItems" :key="item.id">
        <td>{{ item.name }}</td>
      </tr>
    </table>

    <div class="pagination">
      <button :disabled="!canPrev" @click="prevPage">← Précédent</button>
      <span>Page {{ currentPage }} / {{ totalPages }}</span>
      <button :disabled="!canNext" @click="nextPage">Suivant →</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ProductService } from '../services/ProductService'

const items = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const totalPages = computed(() => Math.ceil(items.value.length / pageSize.value))
const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return items.value.slice(start, start + pageSize.value)
})
const canPrev = computed(() => currentPage.value > 1)
const canNext = computed(() => currentPage.value < totalPages.value)

const prevPage = () => { if (canPrev.value) currentPage.value-- }
const nextPage = () => { if (canNext.value) currentPage.value++ }

onMounted(async () => {
  items.value = await ProductService.getAllProducts()
})
</script>
```

**Avantages** : navigation instantanée, simple à coder.
**Inconvénient** : tout est chargé en mémoire au démarrage (pas adapté > 500 lignes).

---

## 5. Pagination côté serveur (PrestaShop : `limit` & `offset`)

**Mots-clés** : limit, offset, server-side, webservice, PrestaShop, page, fetch

PrestaShop accepte les paramètres `limit` et `offset` sur ses endpoints REST :

```
GET /api/products?display=full&limit=10           → 10 premiers
GET /api/products?display=full&limit=10,20        → "limit=N,start" — N items à partir de l'offset 20
GET /api/products?display=full&limit=20&sort=[id_DESC]
```

⚠️ **Syntaxe particulière PrestaShop** : `limit=N,offset` (avec une virgule), pas `limit=N&offset=M`.

### Exemple complet

```vue
<script setup>
import { ref, computed, watch } from 'vue'
import axios from '../config/axios'
import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })

const items = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const loading = ref(false)

const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value))

const fetchPage = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value

    // Récupérer la page
    const res = await axios.get(
      `/api/products?display=full&limit=${pageSize.value},${offset}`
    )
    const data = parser.parse(res.data)
    let products = data.prestashop?.products?.product || []
    if (!Array.isArray(products)) products = [products]
    items.value = products

    // Compter le total (1ère page seulement, ou à chaque rechargement de filtre)
    if (currentPage.value === 1 || totalCount.value === 0) {
      const countRes = await axios.get('/api/products?display=[id]')
      const countData = parser.parse(countRes.data)
      const all = countData.prestashop?.products?.product || []
      totalCount.value = Array.isArray(all) ? all.length : 1
    }
  } finally {
    loading.value = false
  }
}

// Refetch à chaque changement de page
watch(currentPage, fetchPage, { immediate: true })
</script>
```

**Avantages** : scalable (charge uniquement la page nécessaire).
**Inconvénient** : latence à chaque navigation, code plus complexe.

---

## 6. UI : prev / next, numéros de pages, ellipses

**Mots-clés** : UI, navigation, prev, next, pages, ellipsis, …

### Variante minimaliste — prev / next + indicateur

```vue
<div class="pagination">
  <button :disabled="!canPrev" @click="prevPage">← Précédent</button>
  <span>Page {{ currentPage }} / {{ totalPages }}</span>
  <button :disabled="!canNext" @click="nextPage">Suivant →</button>
</div>
```

### Variante avec numéros de pages cliquables + ellipses

```vue
<template>
  <div class="pagination">
    <button :disabled="!canPrev" @click="prevPage">‹</button>

    <button
      v-for="(p, i) in pageNumbers"
      :key="i"
      :class="{ active: p === currentPage, ellipsis: p === '…' }"
      :disabled="p === '…'"
      @click="p !== '…' && goToPage(p)"
    >
      {{ p }}
    </button>

    <button :disabled="!canNext" @click="nextPage">›</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// Calculer la liste à afficher : [1, '…', 4, 5, 6, '…', 12]
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = [1]
  if (cur > 3) pages.push('…')

  const start = Math.max(2, cur - 1)
  const end = Math.min(total - 1, cur + 1)
  for (let i = start; i <= end; i++) pages.push(i)

  if (cur < total - 2) pages.push('…')
  pages.push(total)
  return pages
})
</script>

<style scoped>
.pagination button {
  padding: 6px 12px;
  margin: 0 2px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
}
.pagination button.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}
.pagination button.ellipsis {
  cursor: default;
  border: none;
}
.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
```

---

## 7. "Load more" (charger plus)

**Mots-clés** : load more, charger plus, append, infinite, accumulation

Alternative à la pagination classique : un seul bouton qui **ajoute** des items à la liste affichée.

```vue
<template>
  <div>
    <div v-for="item in visibleItems" :key="item.id">{{ item.name }}</div>

    <button v-if="canLoadMore" @click="loadMore" :disabled="loading">
      {{ loading ? '...' : 'Charger plus' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const allItems = ref([])
const visibleCount = ref(10)
const pageSize = 10
const loading = ref(false)

const visibleItems = computed(() => allItems.value.slice(0, visibleCount.value))
const canLoadMore = computed(() => visibleCount.value < allItems.value.length)

const loadMore = () => { visibleCount.value += pageSize }

onMounted(async () => {
  allItems.value = await SomeService.getAll()
})
</script>
```

**Avantages** : UX fluide, pas de navigation.
**Inconvénients** : pas de notion de "page", la liste finit par devenir longue.

---

## 8. Scroll infini (IntersectionObserver)

**Mots-clés** : infinite scroll, scroll infini, IntersectionObserver, sentinelle, lazy

Variante automatique du "load more" : le chargement se déclenche quand l'utilisateur arrive en bas de la liste.

```vue
<template>
  <div>
    <div v-for="item in visibleItems" :key="item.id">{{ item.name }}</div>
    <div ref="sentinel" v-if="canLoadMore" class="loading-sentinel">
      ⏳ Chargement...
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const allItems = ref([])
const visibleCount = ref(10)
const pageSize = 10
const sentinel = ref(null)

const visibleItems = computed(() => allItems.value.slice(0, visibleCount.value))
const canLoadMore = computed(() => visibleCount.value < allItems.value.length)

let observer

onMounted(async () => {
  allItems.value = await SomeService.getAll()

  observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && canLoadMore.value) {
      visibleCount.value += pageSize
    }
  }, { rootMargin: '100px' })

  if (sentinel.value) observer.observe(sentinel.value)
})

onUnmounted(() => observer?.disconnect())
</script>
```

⚠️ **Note** : avec `v-if`, la sentinelle disparaît quand `canLoadMore = false`, ce qui désengage l'observer automatiquement.

---

## 9. Search + pagination combinés

**Mots-clés** : search, filter, recherche, combiner

Quand on combine recherche et pagination, **toujours** :
1. Filtrer d'abord (computed)
2. Paginer le résultat (computed dérivé)
3. **Reset à la page 1** dès que le filtre change

```vue
<script setup>
import { ref, computed, watch } from 'vue'

const allItems = ref([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 1. Filtrer
const filteredItems = computed(() => {
  if (!searchQuery.value) return allItems.value
  const q = searchQuery.value.toLowerCase()
  return allItems.value.filter(i => i.name.toLowerCase().includes(q))
})

// 2. Total et paginer
const totalPages = computed(() => Math.ceil(filteredItems.value.length / pageSize.value))
const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

// 3. Reset page quand filtre change
watch(searchQuery, () => { currentPage.value = 1 })
</script>
```

---

## 10. Réinitialisation des filtres → retour à la page 1

**Mots-clés** : reset, page 1, filter change, watch

Toujours `watch` les filtres pour ramener à la page 1 :

```js
import { watch } from 'vue'

watch([searchQuery, selectedCategory, priceMin, priceMax], () => {
  currentPage.value = 1
})
```

Sans ça : si l'utilisateur est sur la page 5 et change un filtre qui réduit les résultats à 2 pages, il se retrouve sur une page vide.

---

## 11. URL-synchronization (page dans l'URL)

**Mots-clés** : URL, vue-router, query, deep link, partage, bookmark

Sync la page courante avec l'URL pour permettre le partage de lien et la navigation back/forward du navigateur.

```vue
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const currentPage = ref(parseInt(route.query.page) || 1)
const pageSize = ref(10)

// Pousser dans l'URL quand la page change
watch(currentPage, (p) => {
  router.replace({ query: { ...route.query, page: p } })
})

// Réagir aux changements d'URL (back/forward)
watch(() => route.query.page, (p) => {
  currentPage.value = parseInt(p) || 1
})
</script>
```

URL résultante : `/products?page=3`

---

## 12. Pagination dans le contexte de ce projet

**Mots-clés** : projet, NewApp, ProductList, OrderList, exemples concrets

Les listes susceptibles d'être paginées dans ce projet :

| Vue | Type | Charge actuelle | Recommandation |
|-----|------|----------------|----------------|
| [ProductList.vue](../NewApp/src/views/FrontOffice/ProductList.vue) | Produits boutique | ~50 produits | Pagination côté client (10/page) |
| [OrderList.vue](../NewApp/src/components/OrderList.vue) | Commandes BO | Variable | Côté client si < 500, sinon serveur |
| [CustomerSelection.vue](../NewApp/src/views/FrontOffice/CustomerSelection.vue) | Clients | Variable | Côté client |
| [DashboardView.vue](../NewApp/src/views/BackOffice/DashboardView.vue) — tableau par jour | Activité | < 365 (1 an) | Pas besoin |

### Exemple — ajouter pagination à [ProductList.vue](../NewApp/src/views/FrontOffice/ProductList.vue)

Insérer dans le script :

```js
import { computed, watch } from 'vue'

const currentPage = ref(1)
const pageSize = ref(12)   // 12 produits/page (multiple de 3 et 4 pour la grille)

// `filteredProducts` existe déjà dans ProductList → on pagine son résultat
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredProducts.value.slice(start, start + pageSize.value)
})
const totalPages = computed(() => Math.ceil(filteredProducts.value.length / pageSize.value))

watch(filteredProducts, () => { currentPage.value = 1 })   // reset si filtres changent
```

Et dans le template, remplacer `v-for="product in filteredProducts"` par `v-for="product in paginatedProducts"`, puis ajouter le composant de pagination.

---

## 13. Composable réutilisable `usePagination`

**Mots-clés** : composable, useCompose, factoriser, réutilisable

Un composable pour ne pas dupliquer la logique partout :

```js
// src/composables/usePagination.js
import { ref, computed, watch } from 'vue'

export function usePagination(sourceRef, options = {}) {
  const pageSize = ref(options.pageSize || 10)
  const currentPage = ref(1)

  const totalItems = computed(() => sourceRef.value?.length || 0)
  const totalPages = computed(() => Math.max(1, Math.ceil(totalItems.value / pageSize.value)))

  const paginated = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return (sourceRef.value || []).slice(start, start + pageSize.value)
  })

  const canPrev = computed(() => currentPage.value > 1)
  const canNext = computed(() => currentPage.value < totalPages.value)

  const prev = () => { if (canPrev.value) currentPage.value-- }
  const next = () => { if (canNext.value) currentPage.value++ }
  const goTo = (p) => {
    if (p >= 1 && p <= totalPages.value) currentPage.value = p
  }

  // Reset si la source change (filtres, recherche, etc.)
  watch(sourceRef, () => { currentPage.value = 1 })

  return {
    currentPage, pageSize, totalItems, totalPages, paginated,
    canPrev, canNext, prev, next, goTo,
  }
}
```

### Usage

```vue
<script setup>
import { ref, computed } from 'vue'
import { usePagination } from '../composables/usePagination'

const allProducts = ref([])
const searchQuery = ref('')

const filteredProducts = computed(() => {
  if (!searchQuery.value) return allProducts.value
  return allProducts.value.filter(p => p.name.includes(searchQuery.value))
})

const {
  currentPage, totalPages, paginated, canPrev, canNext, prev, next, goTo
} = usePagination(filteredProducts, { pageSize: 12 })
</script>

<template>
  <input v-model="searchQuery" />
  <div v-for="p in paginated" :key="p.id">{{ p.name }}</div>
  <button :disabled="!canPrev" @click="prev">‹</button>
  <span>{{ currentPage }} / {{ totalPages }}</span>
  <button :disabled="!canNext" @click="next">›</button>
</template>
```

**Note** : le composable s'occupe du reset automatique grâce au `watch(sourceRef)`. Plus besoin de le faire manuellement.

---

## 14. Composant réutilisable `<Pagination />`

**Mots-clés** : composant, Pagination, reusable, controls

```vue
<!-- src/components/Pagination.vue -->
<template>
  <div class="pagination">
    <button :disabled="!canPrev" @click="$emit('prev')">‹</button>

    <button
      v-for="p in pageNumbers"
      :key="p"
      :class="{ active: p === current, ellipsis: p === '…' }"
      :disabled="p === '…'"
      @click="p !== '…' && $emit('go-to', p)"
    >{{ p }}</button>

    <button :disabled="!canNext" @click="$emit('next')">›</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, required: true },
  total: { type: Number, required: true },
})
defineEmits(['prev', 'next', 'go-to'])

const canPrev = computed(() => props.current > 1)
const canNext = computed(() => props.current < props.total)

const pageNumbers = computed(() => {
  if (props.total <= 7) {
    return Array.from({ length: props.total }, (_, i) => i + 1)
  }
  const pages = [1]
  if (props.current > 3) pages.push('…')
  const start = Math.max(2, props.current - 1)
  const end = Math.min(props.total - 1, props.current + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  if (props.current < props.total - 2) pages.push('…')
  pages.push(props.total)
  return pages
})
</script>

<style scoped>
.pagination { display: flex; gap: 4px; justify-content: center; margin: 20px 0; }
.pagination button {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  min-width: 36px;
}
.pagination button.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}
.pagination button.ellipsis { cursor: default; border: none; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
```

### Usage

```vue
<script setup>
import Pagination from '../components/Pagination.vue'
import { usePagination } from '../composables/usePagination'

const { currentPage, totalPages, paginated, prev, next, goTo } = usePagination(filteredProducts)
</script>

<template>
  <div v-for="p in paginated" :key="p.id">...</div>

  <Pagination
    :current="currentPage"
    :total="totalPages"
    @prev="prev"
    @next="next"
    @go-to="goTo"
  />
</template>
```

---

## 15. Anti-patterns à éviter

**Mots-clés** : anti-pattern, éviter, mauvaise pratique

### ❌ 1. Pagination sans reset au changement de filtre
L'utilisateur sur la page 5 change un filtre qui réduit à 2 pages → page vide.
→ **Toujours** `watch(filters, () => currentPage.value = 1)`.

### ❌ 2. Slice qui mute la source
```js
items.value = items.value.slice(start, end)   // ❌ détruit les autres pages
```
→ Faire un `computed` qui slice, sans muter `items`.

### ❌ 3. PageSize trop grand
50+ éléments par page → DOM lourd, scroll long. → Vise **10-20**.

### ❌ 4. Pas d'indicateur de page totale
"Page 3 / ?" → l'utilisateur ne sait pas s'il en reste. → Calcule **toujours** `totalPages`.

### ❌ 5. Numéros de page qui n'incluent pas les ellipses
Avec 100 pages, afficher [1, 2, 3, …, 100] est mieux que [1, 2, 3, 4, 5, 6, …, 100] qui scroll horizontalement.

### ❌ 6. Pas de désactivation des boutons aux extrémités
Sans `:disabled="!canPrev"`, cliquer sur "Prev" en page 1 crée des bugs.

### ❌ 7. Re-fetch inutile
En pagination **côté client**, ne **jamais** refetch à chaque changement de page. Tout est déjà en mémoire.

---

## 16. Templates de code complets

**Mots-clés** : template, copy-paste, boilerplate, snippets

### 16.1 Pagination basique (sans composable)

```vue
<template>
  <table>
    <tr v-for="item in paginatedItems" :key="item.id">
      <td>{{ item.name }}</td>
    </tr>
  </table>

  <div class="pagination">
    <button :disabled="!canPrev" @click="currentPage--">← Précédent</button>
    <span>Page {{ currentPage }} / {{ totalPages }}</span>
    <button :disabled="!canNext" @click="currentPage++">Suivant →</button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const items = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const totalPages = computed(() => Math.max(1, Math.ceil(items.value.length / pageSize.value)))
const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return items.value.slice(start, start + pageSize.value)
})
const canPrev = computed(() => currentPage.value > 1)
const canNext = computed(() => currentPage.value < totalPages.value)

onMounted(async () => {
  items.value = await SomeService.getAll()
})
</script>
```

### 16.2 Pagination + recherche + page-size selector

```vue
<template>
  <div class="controls">
    <input v-model="search" placeholder="Rechercher..." />
    <select v-model.number="pageSize">
      <option :value="10">10</option>
      <option :value="20">20</option>
      <option :value="50">50</option>
    </select>
  </div>

  <table v-if="paginatedItems.length > 0">
    <tr v-for="item in paginatedItems" :key="item.id">
      <td>{{ item.name }}</td>
    </tr>
  </table>
  <p v-else>Aucun résultat</p>

  <Pagination
    :current="currentPage"
    :total="totalPages"
    @prev="prev"
    @next="next"
    @go-to="goTo"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Pagination from '../components/Pagination.vue'
import { usePagination } from '../composables/usePagination'

const allItems = ref([])
const search = ref('')

const filtered = computed(() => {
  if (!search.value) return allItems.value
  const q = search.value.toLowerCase()
  return allItems.value.filter(i => i.name.toLowerCase().includes(q))
})

const {
  currentPage, pageSize, totalPages, paginated, prev, next, goTo
} = usePagination(filtered, { pageSize: 10 })

const paginatedItems = paginated   // alias pour le template

onMounted(async () => {
  allItems.value = await SomeService.getAll()
})
</script>
```

### 16.3 Pagination serveur (PrestaShop)

```vue
<script setup>
import { ref, computed, watch } from 'vue'
import axios from '../config/axios'
import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' })

const items = ref([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize.value)))

const fetchPage = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    const res = await axios.get(
      `/api/products?display=full&limit=${pageSize.value},${offset}`
    )
    const data = parser.parse(res.data)
    let list = data.prestashop?.products?.product || []
    if (!Array.isArray(list)) list = [list]
    items.value = list
  } finally {
    loading.value = false
  }
}

const fetchTotal = async () => {
  const res = await axios.get('/api/products?display=[id]')
  const data = parser.parse(res.data)
  const list = data.prestashop?.products?.product || []
  totalCount.value = Array.isArray(list) ? list.length : (list ? 1 : 0)
}

const prev = () => { if (currentPage.value > 1) currentPage.value-- }
const next = () => { if (currentPage.value < totalPages.value) currentPage.value++ }

watch(currentPage, fetchPage)

fetchTotal().then(fetchPage)
</script>

<template>
  <p v-if="loading">⏳ Chargement...</p>
  <div v-for="p in items" :key="p.id">...</div>
  <button :disabled="currentPage <= 1" @click="prev">‹</button>
  <span>{{ currentPage }} / {{ totalPages }}</span>
  <button :disabled="currentPage >= totalPages" @click="next">›</button>
</template>
```

---

## 17. Cheatsheet rapide

**Mots-clés** : cheatsheet, résumé, antisèche

```
╔══════════════════════════════════════════════════════════════╗
║              PAGINATION — RAPPELS RAPIDES                    ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  STATE                                                       ║
║    items, currentPage, pageSize                              ║
║                                                              ║
║  COMPUTED                                                    ║
║    totalPages = Math.ceil(items.length / pageSize)           ║
║    paginated  = items.slice((page-1)*size, page*size)        ║
║    canPrev    = page > 1                                     ║
║    canNext    = page < totalPages                            ║
║                                                              ║
║  ACTIONS                                                     ║
║    prev: if (canPrev) page--                                 ║
║    next: if (canNext) page++                                 ║
║    goTo(p): if (1 <= p <= totalPages) page = p               ║
║                                                              ║
║  RESET AU CHANGEMENT DE FILTRE                               ║
║    watch(filter, () => currentPage.value = 1)                ║
║                                                              ║
║  CÔTÉ SERVEUR (PrestaShop)                                   ║
║    /api/X?display=full&limit=N,offset                        ║
║    ⚠️ virgule (pas &)                                        ║
║                                                              ║
║  LOAD MORE                                                   ║
║    visibleCount += pageSize  ← juste un compteur             ║
║                                                              ║
║  INFINITE SCROLL                                             ║
║    IntersectionObserver sur sentinelle en bas                ║
║                                                              ║
║  COMPOSABLE                                                  ║
║    usePagination(filteredRef, { pageSize: 10 })              ║
║      → { currentPage, paginated, prev, next, goTo, ... }     ║
║                                                              ║
║  COMPOSANT                                                   ║
║    <Pagination :current :total @prev @next @go-to>           ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Pourquoi paginer | `Pourquoi paginer` |
| Choisir client vs serveur | `client vs serveur` |
| Variables d'état | `État (state)` |
| Slice JS basique | `slice sur un tableau` |
| PrestaShop limit/offset | `limit & offset` ou `PrestaShop` |
| Numéros + ellipses | `numéros de pages, ellipses` |
| Bouton "Charger plus" | `Load more` |
| Scroll automatique | `Scroll infini` ou `IntersectionObserver` |
| Recherche + pagination | `Search + pagination` |
| Reset à la page 1 | `Réinitialisation` |
| Page dans l'URL | `URL-synchronization` |
| Adapter à ce projet | `contexte de ce projet` |
| Composable réutilisable | `usePagination` |
| Composant `<Pagination>` | `Composant réutilisable` |
| Erreurs à éviter | `Anti-patterns` |
| Templates copy-paste | `Templates de code complets` |
| Cheatsheet | `Cheatsheet` |
| Vue refs | Voir [`RefVue.md`](RefVue.md) |
| Patterns View | Voir [`View.md`](View.md) |
| Services / API | Voir [`Service.md`](Service.md) |

---

## 🗂️ Mapping Patterns ↔ Vues du projet

| Pattern documenté | À implémenter dans |
|-------------------|---------------------|
| Pagination côté client | ProductList, OrderList, CustomerSelection |
| Pagination + filter (search) | ProductList |
| Load more | (suggéré pour le scroll-friendly mobile) |
| Pagination côté serveur | (si OrderList dépasse 500 commandes) |
| Composable `usePagination` | À créer dans `src/composables/usePagination.js` |
| Composant `<Pagination />` | À créer dans `src/components/Pagination.vue` |
