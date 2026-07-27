# 📘 Guide des Views Vue 3 — Bonnes pratiques de ce projet

> **Mots-clés généraux** : Vue 3, Composition API, script setup, ref, computed, onMounted, Pinia, vue-router, axios, PrestaShop, NewApp, BackOffice, FrontOffice, async, await, try catch, localStorage, services, store

---

## 📑 Index

- [1. Structure type d'une View](#1-structure-type-dune-view)
- [2. Pattern Loading / Error / Success](#2-pattern-loading--error--success)
- [3. Chargement de données au montage (onMounted)](#3-chargement-de-données-au-montage-onmounted)
- [4. Formulaires (login, checkout)](#4-formulaires-login-checkout)
- [5. Listes, filtres et recherche](#5-listes-filtres-et-recherche)
- [6. Pinia store (cartStore)](#6-pinia-store-cartstore)
- [7. localStorage et session](#7-localstorage-et-session)
- [8. Navigation et redirection](#8-navigation-et-redirection)
- [9. Computed properties (filtres, agrégations, totaux)](#9-computed-properties-filtres-agrégations-totaux)
- [10. Watchers (watch, watchEffect)](#10-watchers-watch-watcheffect)
- [11. Communication parent ↔ enfant (props / emits)](#11-communication-parent--enfant-props--emits)
- [12. Tableau avec lignes expandables](#12-tableau-avec-lignes-expandables)
- [13. Upload de fichiers (CSV, ZIP)](#13-upload-de-fichiers-csv-zip)
- [14. Confirmation et actions destructives](#14-confirmation-et-actions-destructives)
- [15. Messages temporaires (toast auto-disparition)](#15-messages-temporaires-toast-auto-disparition)
- [16. v-for avec clés composites](#16-v-for-avec-clés-composites)
- [17. Rechargement à la navigation (onActivated, visibilitychange)](#17-rechargement-à-la-navigation)
- [18. Anti-patterns identifiés dans le projet](#18-anti-patterns-identifiés-dans-le-projet)
- [19. Templates de code à copier-coller](#19-templates-de-code-à-copier-coller)
- [20. Cheatsheet rapide](#20-cheatsheet-rapide)
- [📋 Aide-mémoire Ctrl+F](#-aide-mémoire-ctrlf)

---

## 1. Structure type d'une View

**Mots-clés** : structure, template, script, style, scoped, sections, ordre, organisation

Toutes tes Views suivent cette structure tripartite. Garde-la cohérente.

```vue
<template>
  <!-- 1. HTML — structure visuelle -->
</template>

<script setup>
// 2.1 Imports (vue, vue-router, services, stores)
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { OrderService } from '../../services/OrderService'

// 2.2 Refs / State
const loading = ref(false)
const errorMessage = ref('')
const items = ref([])

// 2.3 Computed
const total = computed(() => items.value.length)

// 2.4 Fonctions utilitaires
const formatDate = (d) => d.split(' ')[0]

// 2.5 Fonctions principales (chargement, actions)
const load = async () => { ... }
const handleSubmit = async () => { ... }

// 2.6 Lifecycle hooks
onMounted(load)
</script>

<style scoped>
/* 3. CSS scopé au composant */
</style>
```

**Bonnes pratiques** :

- ✅ `<script setup>` (Composition API moderne, pas Options API)
- ✅ `<style scoped>` toujours pour éviter les conflits CSS
- ✅ Imports en haut groupés (Vue, router, services, stores, composants)
- ✅ Refs déclarés avant les fonctions qui les utilisent

---

## 2. Pattern Loading / Error / Success

**Mots-clés** : loading, spinner, error, errorMessage, successMessage, état, async, chargement

Pattern récurrent dans **toutes** tes Views asynchrones.

### Refs

```js
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
```

### Template

```vue
<div v-if="loading" class="loading">
  <div class="spinner"></div>
  <p>⏳ Chargement...</p>
</div>

<div v-if="errorMessage" class="error-message">
  ❌ {{ errorMessage }}
</div>

<div v-if="successMessage" class="success-message">
  ✅ {{ successMessage }}
</div>
```

### Fonction async — pattern `try / catch / finally`

```js
const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await SomeService.getData()
    items.value = data
  } catch (err) {
    console.error('❌ Erreur:', err)
    errorMessage.value = 'Erreur lors du chargement'
  } finally {
    loading.value = false  // ← toujours dans finally
  }
}
```

**À retenir** :
- `loading.value = false` **toujours dans `finally`** (sinon reste bloqué si erreur)
- Reset `errorMessage` au début (pas à la fin)
- `console.error` ET `errorMessage.value` pour debug + UX
- Exemple typique : [MyOrders.vue:108-115](../NewApp/src/views/FrontOffice/MyOrders.vue), [ProductList.vue:191-200](../NewApp/src/views/FrontOffice/ProductList.vue)

---

## 3. Chargement de données au montage (onMounted)

**Mots-clés** : onMounted, montage, lifecycle, hook, async, démarrage, init

### Chargement simple

```js
import { onMounted } from 'vue'

onMounted(async () => {
  await loadOrders()
})
```

### Chargement parallèle (`Promise.all`)

Utilisé dans **DashboardView** et **ProductList** pour gagner du temps.

```js
onMounted(async () => {
  await Promise.all([
    loadOrders(),
    loadDansPanier(),
    loadCustomers()
  ])
})
```

### Chargement avec dépendances séquentielles

Quand l'un dépend de l'autre :

```js
onMounted(async () => {
  await loadCustomers()         // d'abord
  await loadOrdersForCustomer() // ensuite
})
```

**Exemple** : [DashboardView.vue:222-226](../NewApp/src/views/BackOffice/DashboardView.vue)

---

## 4. Formulaires (login, checkout)

**Mots-clés** : form, formulaire, login, submit, validation, v-model, input, prevent, disabled

### Pattern de formulaire — extrait de CustomerLogin.vue

```vue
<template>
  <form @submit.prevent="handleLogin">
    <input
      v-model="email"
      type="email"
      placeholder="Email"
      :disabled="loading"
      required
    />
    <input
      v-model="password"
      type="password"
      placeholder="Mot de passe"
      :disabled="loading"
      required
    />
    <button type="submit" :disabled="loading">
      {{ loading ? 'Connexion...' : 'Se connecter' }}
    </button>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>
  </form>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { CustomerService } from '../../services/CustomerService'

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const router = useRouter()

const handleLogin = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await CustomerService.login(email.value, password.value)
    if (result.success) {
      CustomerService.saveSession(result.customer)
      successMessage.value = '✅ Connexion réussie !'
      // Redirection avec léger délai pour voir le message
      setTimeout(() => router.push('/shop/products'), 1000)
    } else {
      errorMessage.value = result.message || 'Identifiants incorrects'
    }
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
```

**Points clés** :
- `@submit.prevent` empêche le rechargement de la page
- `:disabled="loading"` désactive tous les champs pendant la requête
- Le bouton change de texte (`'Connexion...' : 'Se connecter'`)
- Validation HTML5 (`required`, `type="email"`)
- Délai avant redirection (1000ms) pour que le user voie le succès

### Formulaire avec validation computed

```js
const isEmailValid = computed(() => /^.+@.+\..+$/.test(email.value))
const isPasswordValid = computed(() => password.value.length >= 8)
const canSubmit = computed(() => isEmailValid.value && isPasswordValid.value && !loading.value)
```

```vue
<button :disabled="!canSubmit">Envoyer</button>
```

**Exemples** : [CustomerLogin.vue:71-103](../NewApp/src/views/FrontOffice/CustomerLogin.vue), [LoginView.vue:66-99](../NewApp/src/views/auth/LoginView.vue), [CheckoutView.vue:194-237](../NewApp/src/views/FrontOffice/CheckoutView.vue)

---

## 5. Listes, filtres et recherche

**Mots-clés** : v-for, liste, filter, search, recherche, filtres, multicritères, computed, ProductList

### Filtres multicritères avec `computed`

Pattern extrait de **ProductList.vue** :

```vue
<template>
  <div class="filters">
    <input v-model="filters.search" placeholder="Rechercher..." />
    <select v-model="filters.category">
      <option value="">Toutes catégories</option>
      <option v-for="cat in categories" :key="cat.id" :value="cat.id">
        {{ cat.name }}
      </option>
    </select>
    <input v-model.number="filters.priceMin" type="number" placeholder="Prix min" />
    <input v-model.number="filters.priceMax" type="number" placeholder="Prix max" />
  </div>

  <div class="products-grid">
    <div
      v-for="product in filteredProducts"
      :key="product.id"
      class="product-card"
    >
      {{ product.name }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const products = ref([])
const categories = ref([])
const filters = ref({
  search: '',
  category: '',
  priceMin: null,
  priceMax: null,
})

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    // Filtre nom
    if (filters.value.search && !p.name.toLowerCase().includes(filters.value.search.toLowerCase())) {
      return false
    }
    // Filtre catégorie
    if (filters.value.category && p.id_category !== filters.value.category) {
      return false
    }
    // Filtre prix
    if (filters.value.priceMin != null && p.price < filters.value.priceMin) return false
    if (filters.value.priceMax != null && p.price > filters.value.priceMax) return false

    return true
  })
})
</script>
```

**Points clés** :
- `v-model.number` convertit automatiquement la valeur en number
- Un seul `computed` qui retourne le résultat filtré (réactif sur tous les critères)
- `early return false` à chaque critère qui ne passe pas → lisibilité

**Exemple** : [ProductList.vue:205-240](../NewApp/src/views/FrontOffice/ProductList.vue)

### Image error handling

```vue
<img :src="product.image_url" @error="handleImageError" />
```

```js
const handleImageError = (e) => {
  e.target.src = '/placeholder.png'
}
```

---

## 6. Pinia store (cartStore)

**Mots-clés** : pinia, store, useCartStore, état global, partagé, items, totalPrice, addToCart

### Importer et utiliser le store

```js
import { useCartStore } from '../../stores/cartStore'

const cartStore = useCartStore()
```

### Lecture (réactif automatique)

```vue
<template>
  <div>Articles : {{ cartStore.items.length }}</div>
  <div>Total : {{ cartStore.totalPrice }} €</div>
  <div v-for="item in cartStore.items" :key="item.product_id">
    {{ item.name }}
  </div>
</template>
```

### Actions (mutations)

```js
// Ajouter
await cartStore.addToCart(product, 2, combinationId)

// Modifier quantité
await cartStore.updateQuantity(productId, combinationId, newQty)

// Supprimer
await cartStore.removeFromCart(productId, combinationId)

// Vider (avec suppression côté PrestaShop)
await cartStore.clearCart()
```

**Bonne pratique** : tu **n'écris jamais** directement dans `cartStore.items` depuis une View. Toujours via les méthodes du store.

**Exemples** : [CartView.vue:217-237](../NewApp/src/views/FrontOffice/CartView.vue), [ProductDetail.vue:225-283](../NewApp/src/views/FrontOffice/ProductDetail.vue)

---

## 7. localStorage et session

**Mots-clés** : localStorage, session, persistance, selectedCustomer, isAnonymous, pendingCheckout

### Sauvegarder

```js
localStorage.setItem('selectedCustomer', JSON.stringify({
  id: customer.id,
  firstname: customer.firstname,
  email: customer.email
}))
```

### Lire (toujours avec un fallback)

```js
const stored = localStorage.getItem('selectedCustomer')
const customer = stored ? JSON.parse(stored) : null
```

### Flags booléens

```js
localStorage.setItem('isAnonymous', 'true')

// Lecture (attention : tout est string)
const isAnonymous = localStorage.getItem('isAnonymous') === 'true'
```

### Supprimer

```js
localStorage.removeItem('selectedCustomer')
```

### Vider tout (debug)

```js
// Dans la console du navigateur
localStorage.clear()
```

**À éviter** :
- ❌ Stocker des objets sans `JSON.stringify` → tu auras `[object Object]`
- ❌ Confier des secrets à localStorage (accessible par tous les scripts du domaine)
- ❌ Supposer que localStorage existe (SSR / mode strict du navigateur)

**Exemples** : [CustomerSelection.vue:130-136](../NewApp/src/views/FrontOffice/CustomerSelection.vue), [CartView.vue:260](../NewApp/src/views/FrontOffice/CartView.vue)

---

## 8. Navigation et redirection

**Mots-clés** : router, useRouter, push, replace, navigation, redirection, params, query

### Setup

```js
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()  // pour naviguer
const route = useRoute()    // pour lire la route actuelle
```

### Pousser une nouvelle route

```js
router.push('/shop/products')
router.push({ name: 'product-detail', params: { id: 42 } })
router.push({ path: '/checkout', query: { from: 'cart' } })
```

### Lire les params d'URL

```js
const productId = route.params.id
const fromQuery = route.query.from
```

### Redirection avec délai (pour voir le toast)

```js
successMessage.value = '✅ Commande validée !'
setTimeout(() => {
  cartStore.clearCart()
  router.push('/shop/products')
}, 3000)
```

### Navigation conditionnelle

```js
// Selon le contexte stocké
const pendingCheckout = localStorage.getItem('pendingCheckout')
if (pendingCheckout) {
  localStorage.removeItem('pendingCheckout')
  router.push('/checkout')
} else {
  router.push('/shop/products')
}
```

**Exemples** : [LoginView.vue:86-88](../NewApp/src/views/auth/LoginView.vue), [CustomerSelection.vue:150-158](../NewApp/src/views/FrontOffice/CustomerSelection.vue)

---

## 9. Computed properties (filtres, agrégations, totaux)

**Mots-clés** : computed, agrégation, total, reduce, groupBy, sum, calcul, dérivé, getter

### Agrégation simple (somme)

```js
const totalSpent = computed(() => {
  return orders.value.reduce((sum, o) => sum + parseFloat(o.total_paid), 0)
})
```

### Groupement par date

Extrait de **DashboardView.vue** :

```js
const ordersByDate = computed(() => {
  const grouped = {}
  for (const order of allOrders.value) {
    const date = order.date_add.split(' ')[0]  // YYYY-MM-DD
    if (!grouped[date]) {
      grouped[date] = { orders: [], total: 0 }
    }
    grouped[date].orders.push(order)
    grouped[date].total += order.total_paid
  }
  return grouped
})
```

### Tri d'un objet groupé

```js
const sortedOrdersByDate = computed(() => {
  const sorted = {}
  const dates = Object.keys(ordersByDate.value).sort((a, b) => b.localeCompare(a))
  for (const date of dates) {
    sorted[date] = ordersByDate.value[date]
  }
  return sorted
})
```

### Computed dérivé d'un autre computed

```js
const totalOrders = computed(() => allOrders.value.length)

const totalAmount = computed(() => {
  return allOrders.value.reduce((sum, o) => sum + o.total_paid, 0)
})

const averagePerDay = computed(() => {
  const days = Object.keys(ordersByDate.value).length
  return days > 0 ? totalAmount.value / days : 0
})
```

### Computed conditionnel sur un objet selectionné

Extrait de **ProductDetail.vue** :

```js
const finalPriceTTC = computed(() => {
  if (!product.value) return 0
  const base = product.value.price_ttc || 0
  const delta = selectedCombination.value?.price_impact_ttc || 0
  return base + delta
})
```

**Bonnes pratiques** :
- ✅ Préférer un `computed` à une fonction normale pour les valeurs dérivées d'un état réactif (mémoisation automatique)
- ✅ Pas de side-effects dans un `computed` (pas de fetch, pas d'écriture localStorage)
- ✅ Lire `.value` à l'intérieur d'un `computed` (script), pas dans le template

**Exemples** : [DashboardView.vue:264-307](../NewApp/src/views/BackOffice/DashboardView.vue), [ProductDetail.vue:171-176](../NewApp/src/views/FrontOffice/ProductDetail.vue)

---

## 10. Watchers (watch, watchEffect)

**Mots-clés** : watch, watchEffect, observer, réaction, side-effect, fetch déclenché

### `watch` — réagir au changement d'un ref

Extrait de **ProductList.vue** :

```js
import { watch } from 'vue'

watch(dateForm, async (newDate, oldDate) => {
  console.log(`Date changée: ${oldDate} → ${newDate}`)
  await loadProducts()
})
```

### Watch sur plusieurs sources

```js
watch([userId, filters], async ([newId, newFilters], [oldId, oldFilters]) => {
  await reload()
})
```

### Watch sur un objet imbriqué (deep)

```js
watch(filters, async (newFilters) => {
  await reload()
}, { deep: true })
```

### Watch immédiat (déclencher au montage aussi)

```js
watch(productId, async () => {
  await loadProduct()
}, { immediate: true })
```

### `watchEffect` — sans dépendances explicites

```js
import { watchEffect } from 'vue'

watchEffect(() => {
  // Toutes les refs lues à l'intérieur sont automatiquement watchées
  console.log('Total:', total.value)
})
```

**Quand utiliser quoi** :
- `computed` → quand tu veux une **valeur dérivée**
- `watch` → quand tu veux un **side-effect** (fetch, navigation, log) au changement d'un ref précis
- `watchEffect` → équivalent du watch, mais auto-détecte les dépendances. Pratique pour les debug logs

**Exemple** : [ProductList.vue:331-333](../NewApp/src/views/FrontOffice/ProductList.vue)

---

## 11. Communication parent ↔ enfant (props / emits)

**Mots-clés** : props, emit, defineProps, defineEmits, événement, parent, enfant

### Parent → Enfant (props)

```vue
<!-- OrderAdmin.vue -->
<template>
  <OrderList @orders-loaded="updateStats" />
</template>
```

### Enfant → Parent (emit)

```vue
<!-- OrderList.vue (enfant) -->
<script setup>
const emit = defineEmits(['orders-loaded'])

const load = async () => {
  const orders = await OrderService.getAllOrders()
  emit('orders-loaded', orders)   // ← envoie au parent
}
</script>
```

### Parent reçoit l'event

```vue
<!-- OrderAdmin.vue (parent) -->
<script setup>
const updateStats = (orders) => {
  totalOrders.value = orders.length
  paidOrders.value = orders.filter(o => parseInt(o.current_state) === 11).length
}
</script>
```

**Convention de nommage** :
- Événement en `kebab-case` côté template (`@orders-loaded`)
- Émission en `kebab-case` côté script (`emit('orders-loaded')`)

**Exemple** : [OrderAdmin.vue:57,96-121](../NewApp/src/views/BackOffice/OrderAdmin.vue)

---

## 12. Tableau avec lignes expandables

**Mots-clés** : table, tableau, expand, collapse, accordion, details, lignes, sub-rows

Pattern extrait de **DashboardView.vue**.

### Setup du state

```js
const expandedDates = ref([])

const toggleDate = (date) => {
  const idx = expandedDates.value.indexOf(date)
  if (idx > -1) {
    expandedDates.value.splice(idx, 1)
  } else {
    expandedDates.value.push(date)
  }
}
```

### Template avec `<template v-for>`

Important : pour avoir 2 `<tr>` par itération dans un `<tbody>`, il faut utiliser `<template v-for>` (le `<tr>` ne peut pas être nesté).

```vue
<table>
  <tbody>
    <template v-for="(data, date) in sortedOrdersByDate" :key="date">
      <tr @click="toggleDate(date)" :class="{ expanded: expandedDates.includes(date) }">
        <td>{{ formatDate(date) }}</td>
        <td>{{ data.orders.length }}</td>
        <td>{{ data.total.toFixed(2) }} €</td>
      </tr>

      <!-- Ligne de détails (visible uniquement si expandé) -->
      <tr v-if="expandedDates.includes(date)" class="details-row">
        <td colspan="3">
          <table class="details-table">
            <tr v-for="order in data.orders" :key="order.id">
              <td>{{ order.reference }}</td>
              <td>{{ order.total_paid }} €</td>
            </tr>
          </table>
        </td>
      </tr>
    </template>
  </tbody>
</table>
```

**Piège classique** : si tu mets le 2ème `<tr>` à côté du `<tr v-for>` (en sibling), la variable de boucle (`date`) n'est plus dans le scope. → utilise `<template v-for>`.

**Exemple** : [DashboardView.vue:73-98,310-317](../NewApp/src/views/BackOffice/DashboardView.vue)

---

## 13. Upload de fichiers (CSV, ZIP)

**Mots-clés** : file, upload, FileReader, async, readAsText, FormData, multipart, CSV, ZIP

### Input de fichier

```vue
<input type="file" accept=".csv" @change="onFileSelected" />
```

```js
const selectedFile = ref(null)

const onFileSelected = (e) => {
  selectedFile.value = e.target.files[0]
}
```

### Lire le contenu en texte (CSV)

```js
const readText = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = reject
  reader.readAsText(file)
})

const handleImport = async () => {
  const text = await readText(selectedFile.value)
  await SomeService.import(text)
}
```

### Upload binaire (ZIP, image) → FormData

```js
const formData = new FormData()
formData.append('image', blob, 'photo.png')

await axios.post('/api/images/products/42', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
```

### Progress callback

Pattern dans **ImportView.vue** :

```js
const currentStep = ref('')
const currentLabel = ref('')
const currentTotal = ref(0)
const currentCount = ref(0)

const progressPercent = computed(() => {
  if (currentTotal.value === 0) return 0
  return (currentCount.value / currentTotal.value) * 100
})

const onProgress = (p) => {
  currentStep.value = p.step
  currentLabel.value = p.label
  currentTotal.value = p.total
  currentCount.value = p.current
}

// Le service appelle onProgress() à chaque ligne
await ImportService.importFile1(text, onProgress)
```

```vue
<div class="progress-bar">
  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
  <span>{{ currentCount }} / {{ currentTotal }}</span>
</div>
```

**Exemple** : [ImportView.vue:111-171](../NewApp/src/views/BackOffice/ImportView.vue)

---

## 14. Confirmation et actions destructives

**Mots-clés** : confirm, confirmation, destructive, suppression, vider, danger, modal

### Pattern à 2 niveaux — checkbox + confirm()

Extrait de **ResetView.vue** :

```vue
<template>
  <label>
    <input type="checkbox" v-model="confirmed" />
    Je confirme vouloir supprimer toutes les données
  </label>

  <button :disabled="!confirmed || running" @click="startReset" class="btn-danger">
    🔥 Réinitialiser
  </button>
</template>

<script setup>
const confirmed = ref(false)

const startReset = async () => {
  if (!confirm('⚠️ Cette action est irréversible. Continuer ?')) return

  // ... action destructive
}
</script>
```

### Confirmation simple pour suppression unique

```js
const removeItem = (id) => {
  if (!confirm('Supprimer cet article ?')) return
  cartStore.removeFromCart(id)
}
```

**Bonnes pratiques** :
- ✅ Toujours `confirm()` AVANT une action irréversible
- ✅ Bouton désactivé jusqu'à confirmation explicite (checkbox)
- ✅ Couleur **rouge** (`.btn-danger`) pour bien signaler le danger
- ✅ Message clair sur ce qui sera supprimé

**Exemples** : [ResetView.vue:112-143](../NewApp/src/views/BackOffice/ResetView.vue), [CartView.vue:241,249](../NewApp/src/views/FrontOffice/CartView.vue)

---

## 15. Messages temporaires (toast auto-disparition)

**Mots-clés** : toast, success, message, setTimeout, auto, disparition, notification

Pattern simple sans librairie :

```js
const successMessage = ref('')

const doAction = async () => {
  // ... action
  successMessage.value = '✅ Action réussie !'

  // Disparaît après 3 secondes
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}
```

```vue
<div v-if="successMessage" class="success-message">
  {{ successMessage }}
</div>
```

### Avec navigation différée

```js
successMessage.value = '✅ Commande validée !'
setTimeout(() => {
  cartStore.clearCart()
  router.push('/shop/products')
}, 3000)
```

**Bonnes pratiques** :
- Durée typique : **2000ms** (succès simple) → **3000ms** (avant navigation)
- Toujours `v-if` (pas `v-show`) pour les messages temporaires
- Reset du message au début de chaque nouvelle action

**Exemples** : [ProductDetail.vue:274-276](../NewApp/src/views/FrontOffice/ProductDetail.vue), [StockManagement.vue:304-306](../NewApp/src/views/BackOffice/StockManagement.vue), [CartView.vue:231](../NewApp/src/views/FrontOffice/CartView.vue)

---

## 16. v-for avec clés composites

**Mots-clés** : v-for, key, clé, composite, unique, template literal, identifiant

### Clé simple (ID unique)

```vue
<div v-for="product in products" :key="product.id">
  {{ product.name }}
</div>
```

### Clé composite (identifiant multi-champ)

Quand un seul ID ne suffit pas — typique pour un panier avec combinaisons :

```vue
<div
  v-for="item in cartStore.items"
  :key="`${item.product_id}-${item.combination_id}`"
>
  {{ item.name }}
</div>
```

### Clé sur deux dimensions

Pour les options à plusieurs valeurs sélectionnées :

```vue
<div v-for="option in product.product_options" :key="option.id">
  <select
    v-model="selectedOptions[product.id][option.id]"
    @change="updateSelectedCombination(product.id)"
  >
    <option v-for="value in option.values" :key="value.id" :value="value.id">
      {{ value.name }}
    </option>
  </select>
</div>
```

**Pourquoi c'est important** : Vue utilise la clé pour optimiser le re-render. Une clé non unique ou changeante provoque des bugs visuels (mauvais composants gardés, états mélangés).

**À ÉVITER** : `:key="index"` quand la liste peut être réordonnée ou filtrée. Préférer toujours un ID stable.

**Exemples** : [CartView.vue:36](../NewApp/src/views/FrontOffice/CartView.vue), [ProductList.vue:106](../NewApp/src/views/FrontOffice/ProductList.vue)

---

## 17. Rechargement à la navigation

**Mots-clés** : onActivated, onMounted, visibilitychange, refresh, refetch, focus, rechargement

Problème typique : tu crées un cart depuis une autre page, tu reviens sur OrderAdmin, mais le compteur "Dans le panier" est figé car `onMounted` ne se redéclenche pas.

### Solution combinée

Pattern extrait de **OrderAdmin.vue** :

```js
import { ref, onMounted, onActivated } from 'vue'
import { CartService } from '../../services/CartService'

const errorOrders = ref(0)

const loadCartsWithoutOrder = async () => {
  try {
    const carts = await CartService.getCartsWithoutOrder()
    errorOrders.value = carts.length
  } catch (err) {
    console.error('❌ Erreur:', err)
  }
}

// 1. Premier chargement
onMounted(loadCartsWithoutOrder)

// 2. Rechargement à chaque retour sur la vue (si <keep-alive> est utilisé)
onActivated(loadCartsWithoutOrder)

// 3. Rechargement quand l'onglet redevient visible
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) loadCartsWithoutOrder()
  })
}
```

**Quand chacun se déclenche** :
- `onMounted` → 1 seule fois, au tout premier montage
- `onActivated` → à chaque fois que le composant redevient actif (nécessite `<keep-alive>` autour de `<router-view>`)
- `visibilitychange` → quand l'utilisateur revient sur l'onglet du navigateur

**Exemple** : [OrderAdmin.vue:75-92](../NewApp/src/views/BackOffice/OrderAdmin.vue)

---

## 18. Anti-patterns identifiés dans le projet

**Mots-clés** : anti-pattern, à éviter, mauvaise pratique, refactor, technical debt

Liste des patterns repérés dans le code à **éviter** dans les nouvelles Views :

### ❌ Anti-pattern 1 — Imports dynamiques inutiles

Vu dans **UnifiedLoginView.vue:139** et **CartView.vue:139** :

```js
// ❌ MAUVAIS — dans une fonction async
const { useCartStore } = await import('../../stores/cartStore')
```

```js
// ✅ BON — en haut du fichier
import { useCartStore } from '../../stores/cartStore'
```

Un import dynamique se justifie seulement pour le lazy-loading (gros modules, code splitting). Pour un store Pinia, c'est inutile.

### ❌ Anti-pattern 2 — `alert()` au lieu d'un toast

```js
// ❌ MAUVAIS — bloque l'UI
alert('Produit ajouté !')

// ✅ BON
successMessage.value = '✅ Produit ajouté'
setTimeout(() => successMessage.value = '', 3000)
```

### ❌ Anti-pattern 3 — `console.log` excessifs en production

```js
// ❌ Trop verbeux
console.log('🎯 Le produit a des options')
console.log('✅ Toutes les options sélectionnées ?', allOptionsSelected)
console.log('📤 Appel cartStore.addToCart...')

// ✅ Garder uniquement les erreurs en prod
console.error('❌ Erreur ajout cart:', err)
```

Tu peux les laisser en dev, mais penser à les retirer pour la prod (ou utiliser un wrapper avec un flag `DEBUG`).

### ❌ Anti-pattern 4 — Code dupliqué entre LoginViews

`CustomerLogin.vue` et `LoginView.vue` ont des structures quasi-identiques. → factoriser dans un composable `useLogin()`.

### ❌ Anti-pattern 5 — Réassignation d'un ref sans `.value`

```js
const data = ref(null)

// ❌
data = await axios.get(...)

// ✅
data.value = (await axios.get(...)).data
```

(Voir [RefVue.md](RefVue.md) pour le détail.)

### ❌ Anti-pattern 6 — `if (loading)` au lieu de `if (loading.value)` dans le script

```js
const loading = ref(false)

// ❌ TOUJOURS truthy (loading est un objet)
if (loading) { ... }

// ✅
if (loading.value) { ... }
```

### ❌ Anti-pattern 7 — Computed avec side-effect

```js
// ❌ MAUVAIS — un computed ne doit JAMAIS modifier d'état ni faire de fetch
const total = computed(() => {
  axios.post('/api/log', { ... })
  return items.value.length
})

// ✅ Utiliser watch pour les side-effects
watch(items, () => {
  axios.post('/api/log', { ... })
})
```

---

## 19. Templates de code à copier-coller

**Mots-clés** : template, snippet, copy-paste, modèle, starter, boilerplate

### 19.1 View basique avec loading + load au montage

```vue
<template>
  <div class="page">
    <h1>📦 Titre</h1>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>Chargement...</p>
    </div>

    <div v-if="errorMessage" class="error-message">
      ❌ {{ errorMessage }}
    </div>

    <div v-if="!loading && !errorMessage && items.length > 0">
      <div v-for="item in items" :key="item.id">
        {{ item.name }}
      </div>
    </div>

    <div v-if="!loading && items.length === 0" class="no-data">
      Aucune donnée
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { SomeService } from '../../services/SomeService'

const items = ref([])
const loading = ref(false)
const errorMessage = ref('')

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    items.value = await SomeService.getAll()
  } catch (err) {
    console.error('❌ Erreur:', err)
    errorMessage.value = 'Erreur lors du chargement'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 30px; max-width: 1200px; margin: 0 auto; }
.loading { text-align: center; padding: 60px; }
.spinner {
  width: 50px; height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-message {
  background: #fee2e2; border: 1px solid #fca5a5;
  color: #991b1b; padding: 15px; border-radius: 8px;
}
.no-data { text-align: center; padding: 60px; color: #64748b; }
</style>
```

### 19.2 Formulaire complet

```vue
<template>
  <form @submit.prevent="onSubmit">
    <input v-model="form.name" placeholder="Nom" required :disabled="loading" />
    <input v-model="form.email" type="email" placeholder="Email" required :disabled="loading" />
    <button type="submit" :disabled="!canSubmit">
      {{ loading ? 'Envoi...' : 'Envoyer' }}
    </button>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>
  </form>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const form = reactive({ name: '', email: '' })
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const canSubmit = computed(() =>
  !loading.value && form.name.length > 0 && /\S+@\S+\.\S+/.test(form.email)
)

const onSubmit = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    await SomeService.save(form)
    successMessage.value = '✅ Enregistré !'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
```

### 19.3 Liste avec filtre

```vue
<template>
  <input v-model="search" placeholder="Recherche..." class="search-input" />

  <div class="grid">
    <div v-for="item in filteredItems" :key="item.id" class="card">
      {{ item.name }}
    </div>
  </div>

  <p v-if="filteredItems.length === 0">Aucun résultat</p>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const items = ref([])
const search = ref('')

const filteredItems = computed(() => {
  if (!search.value) return items.value
  const q = search.value.toLowerCase()
  return items.value.filter(i => i.name.toLowerCase().includes(q))
})

onMounted(async () => {
  items.value = await SomeService.getAll()
})
</script>
```

### 19.4 Page admin avec stats + tableau

```vue
<template>
  <div class="admin-page">
    <header><h1>Gestion</h1></header>

    <div class="stats-grid">
      <div class="stat-card">
        <h3>{{ totalCount }}</h3>
        <p>Total</p>
      </div>
      <div class="stat-card success">
        <h3>{{ activeCount }}</h3>
        <p>Actifs</p>
      </div>
    </div>

    <table>
      <thead>
        <tr><th>Nom</th><th>Statut</th><th>Action</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.name }}</td>
          <td>
            <span class="badge" :class="item.active ? 'active' : 'inactive'">
              {{ item.active ? 'Actif' : 'Inactif' }}
            </span>
          </td>
          <td>
            <button @click="onEdit(item)">✏️</button>
            <button @click="onDelete(item)" class="btn-danger">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const items = ref([])

const totalCount = computed(() => items.value.length)
const activeCount = computed(() => items.value.filter(i => i.active).length)

const onEdit = (item) => router.push(`/admin/edit/${item.id}`)

const onDelete = async (item) => {
  if (!confirm(`Supprimer "${item.name}" ?`)) return
  await SomeService.delete(item.id)
  items.value = items.value.filter(i => i.id !== item.id)
}

onMounted(async () => {
  items.value = await SomeService.getAll()
})
</script>
```

---

## 20. Cheatsheet rapide

**Mots-clés** : cheatsheet, résumé, mémo, antisèche, vite

```
╔══════════════════════════════════════════════════════════════╗
║              VIEW VUE 3 — CHECKLIST RAPIDE                   ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  STRUCTURE                                                   ║
║    <template>  <script setup>  <style scoped>                ║
║                                                              ║
║  STATE                                                       ║
║    const loading = ref(false)                                ║
║    const errorMessage = ref('')                              ║
║    const items = ref([])                                     ║
║                                                              ║
║  CHARGEMENT                                                  ║
║    onMounted(async () => { await load() })                   ║
║    Parallèle → Promise.all([a, b, c])                        ║
║                                                              ║
║  TRY / CATCH                                                 ║
║    try { ... } catch (e) { ... } finally { loading=false }   ║
║                                                              ║
║  COMPUTED                                                    ║
║    const total = computed(() => items.value.length)          ║
║                                                              ║
║  V-FOR KEY                                                   ║
║    :key="item.id"  ou  :key="`${a}-${b}`"                    ║
║                                                              ║
║  ROUTER                                                      ║
║    const router = useRouter()                                ║
║    router.push('/path')                                      ║
║                                                              ║
║  STORE                                                       ║
║    const store = useCartStore()                              ║
║    store.items   store.addItem(...)                          ║
║                                                              ║
║  LOCALSTORAGE                                                ║
║    setItem(key, JSON.stringify(obj))                         ║
║    JSON.parse(getItem(key))                                  ║
║                                                              ║
║  TOAST                                                       ║
║    successMessage.value = '...'                              ║
║    setTimeout(() => successMessage.value = '', 3000)         ║
║                                                              ║
║  CONFIRM                                                     ║
║    if (!confirm('Sûr ?')) return                             ║
║                                                              ║
║  REFRESH AUTO                                                ║
║    onMounted + onActivated + visibilitychange                ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 📋 Aide-mémoire Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Structure d'une View | `Structure type` |
| Loader / Spinner | `loading` ou `Pattern Loading` |
| Erreur dans une fonction async | `try catch finally` |
| Chargement au montage | `onMounted` |
| Plusieurs chargements parallèles | `Promise.all` |
| Formulaire login | `Formulaires` ou `CustomerLogin` |
| Recherche / filtres | `Listes filtres` ou `filteredProducts` |
| Pinia | `Pinia store` ou `useCartStore` |
| localStorage | `localStorage` |
| Navigation | `Navigation et redirection` ou `router.push` |
| Total / agrégation | `Computed properties` ou `reduce` |
| Watcher | `Watchers` ou `watch` |
| Communication parent enfant | `emit` ou `parent enfant` |
| Tableau expandable | `Tableau avec lignes expandables` |
| Upload de fichier | `Upload de fichiers` ou `FileReader` |
| Confirmation | `Confirmation` ou `actions destructives` |
| Toast / message temporaire | `toast` ou `Messages temporaires` |
| Clé v-for | `v-for clés composites` |
| Rechargement automatique | `onActivated` ou `visibilitychange` |
| Erreur à corriger | `Anti-patterns` |
| Code à copier | `Templates à copier-coller` |
| Rappel express | `Cheatsheet` |
| Différence ref / .value | Voir [`RefVue.md`](RefVue.md) |

---

## 🗂️ Mapping Views ↔ Patterns documentés

Référence rapide pour savoir où regarder le code source d'un pattern.

| Pattern | Vues d'exemple |
|---------|----------------|
| Loading / Error / Success | Toutes les vues async |
| onMounted + Promise.all | DashboardView, ProductList |
| Form login | CustomerLogin, LoginView, UnifiedLoginView |
| Filtres multicritères | ProductList |
| Pinia (cartStore) | CartView, CheckoutView, ProductDetail |
| Computed groupé / trié | DashboardView |
| Watch | ProductList |
| Emit parent ↔ enfant | OrderAdmin ↔ OrderList |
| Tableau expandable | DashboardView |
| Upload fichier | ImportView |
| Confirmation destructive | ResetView, CartView |
| Toast | CartView, ProductDetail, StockManagement |
| Refresh onActivated | OrderAdmin |
| Combinaisons produit | ProductDetail, StockManagement |
| Reactive objects | StockManagement |
| Inline render component | ImportView (FileInput) |
