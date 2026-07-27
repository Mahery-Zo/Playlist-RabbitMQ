# 📚 Guide complet : `ref` dans Vue 3

> **Mots-clés** : ref, reactive, .value, reactivity, réactivité, Composition API, setup, Proxy, computed, watch, watchEffect, unwrap, déballage, template, script, useState, double binding, v-model, deep, shallowRef

---

## 📑 Index

- [1. Qu'est-ce qu'un `ref` ?](#1-quest-ce-quun-ref-)
- [2. Pourquoi `.value` ?](#2-pourquoi-value-)
- [3. Avec `.value` vs simple `=` : la différence fondamentale](#3-avec-value-vs-simple--la-différence-fondamentale)
- [4. Règles d'or](#4-règles-dor)
- [5. Cas d'usage courants](#5-cas-dusage-courants)
- [6. ref vs reactive](#6-ref-vs-reactive)
- [7. Auto-déballage (auto-unwrap)](#7-auto-déballage-auto-unwrap)
- [8. Pièges classiques](#8-pièges-classiques)
- [9. Exemples réutilisables](#9-exemples-réutilisables)
- [10. Cheatsheet rapide](#10-cheatsheet-rapide)

---

## 1. Qu'est-ce qu'un `ref` ?

**Mots-clés** : définition, base, fondement, RefImpl, wrapper, conteneur

`ref()` est une fonction de Vue 3 qui crée une **référence réactive** vers une valeur. C'est un **conteneur** qui enveloppe ta valeur pour que Vue puisse :

- détecter quand elle change
- mettre à jour automatiquement le DOM, les `computed`, les `watch` qui en dépendent

```js
import { ref } from 'vue'

const compteur = ref(0)
// compteur n'est PAS le nombre 0
// compteur est un objet { value: 0, ...mécanique réactive }
```

**À retenir** : `ref(x)` ne renvoie **jamais** `x` directement. Il renvoie un **objet wrapper** qui contient `x` dans sa propriété `.value`.

---

## 2. Pourquoi `.value` ?

**Mots-clés** : value, accès, lecture, écriture, getter, setter, proxy, JavaScript primitives

JavaScript ne sait pas observer les **primitives** (number, string, boolean). Si on faisait :

```js
let x = 0
x = 1   // ❌ Aucun moyen pour Vue de savoir que ça a changé
```

Pour rendre la valeur observable, Vue l'enveloppe dans un objet avec une propriété `.value`. **Lire ou écrire `.value` déclenche les hooks de réactivité** (getter/setter sous le capot).

```js
const x = ref(0)
console.log(x.value)  // 0   ← getter déclenché → Vue note la dépendance
x.value = 1           //     ← setter déclenché → Vue notifie les watchers
```

---

## 3. Avec `.value` vs simple `=` : la différence fondamentale

**Mots-clés** : différence, comparaison, erreur, perdre la réactivité, écrasement, .value vs =

C'est **LA** confusion la plus fréquente. Comparons les 4 cas :

### ✅ Cas 1 — Lire la valeur

```js
const compteur = ref(0)

// ✅ DANS LE SCRIPT
console.log(compteur.value)   // 0   ← lit le contenu
console.log(compteur)         // RefImpl { value: 0 } ← lit le wrapper (inutile)

// ✅ DANS LE TEMPLATE
// {{ compteur }}   ← Vue auto-déballe, affiche 0 (pas besoin de .value)
```

### ✅ Cas 2 — Modifier la valeur

```js
const compteur = ref(0)

// ✅ CORRECT : on modifie le contenu du ref
compteur.value = 5
compteur.value++

// ❌ ERREUR : on remplace le ref par un nombre — la réactivité est PERDUE
compteur = 5
//        ^ écrase le wrapper, le template ne réagira plus
//          (et donne aussi une TypeError car compteur est `const`)
```

### ❌ Cas 3 — Oublier `.value` lors d'une opération

```js
const compteur = ref(0)

// ❌ ERREUR — on additionne l'objet wrapper, pas le nombre
const double = compteur * 2
// → NaN (l'objet RefImpl n'est pas un nombre)

// ✅ CORRECT
const double = compteur.value * 2
```

### ❌ Cas 4 — Réassigner un tableau/objet sans `.value`

```js
const liste = ref([])

// ❌ ERREUR — on remplace le ref par un tableau
liste = [1, 2, 3]
// → la variable `liste` pointe maintenant vers un tableau brut, plus de réactivité

// ✅ CORRECT
liste.value = [1, 2, 3]
// → Vue détecte le changement, le template se met à jour
```

### 📋 Tableau récapitulatif

| Situation | Code | Effet |
|-----------|------|-------|
| Lire dans le script | `x.value` | ✅ Récupère la valeur |
| Lire dans le template | `{{ x }}` | ✅ Auto-déballé par Vue |
| Modifier dans le script | `x.value = 5` | ✅ Réactivité préservée |
| Modifier dans le script | `x = 5` | ❌ **Bug : casse le ref** |
| Calcul / comparaison | `x.value * 2` | ✅ Opération sur la valeur |
| Calcul / comparaison | `x * 2` | ❌ NaN, retourne `[object Object]`, etc. |

---

## 4. Règles d'or

**Mots-clés** : règles, mémo, rappel, principes, do, don't, à faire, à éviter

### 🟢 À FAIRE

1. **Toujours utiliser `const`** pour déclarer un ref (jamais `let`) :
   ```js
   const compteur = ref(0)   // ✅
   ```

2. **Modifier via `.value`** dans le `<script>` :
   ```js
   compteur.value++          // ✅
   user.value = { id: 1 }    // ✅
   ```

3. **Pas de `.value` dans le `<template>`** (auto-déballage) :
   ```html
   <p>{{ compteur }}</p>     <!-- ✅ -->
   ```

### 🔴 À ÉVITER

1. **Ne jamais réassigner le ref lui-même** :
   ```js
   compteur = 5              // ❌ casse la réactivité (et erreur const)
   ```

2. **Ne pas déstructurer un objet ref** :
   ```js
   const { value } = compteur     // ❌ perd la réactivité après
   ```
   Utiliser plutôt `toRefs()` ou accéder via `.value` à chaque fois.

3. **Ne pas faire de calcul sur le ref sans `.value`** :
   ```js
   const total = panier * prix    // ❌ NaN
   const total = panier.value * prix.value   // ✅
   ```

---

## 5. Cas d'usage courants

**Mots-clés** : exemples, usage, scénarios, état, formulaire, liste, async, fetch

### 5.1 État local d'un composant

```vue
<script setup>
import { ref } from 'vue'

const isOpen = ref(false)
const username = ref('')
const items = ref([])

const toggle = () => { isOpen.value = !isOpen.value }
const addItem = (item) => { items.value.push(item) }   // ✅ mutation autorisée
const reset = () => { items.value = [] }               // ✅ via .value
</script>
```

### 5.2 Données chargées depuis une API

```js
const products = ref([])
const loading = ref(false)
const error = ref(null)

const fetchProducts = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/products')
    products.value = res.data    // ✅ remplacement complet via .value
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
```

### 5.3 Référence à un élément DOM

```vue
<script setup>
import { ref, onMounted } from 'vue'

const inputEl = ref(null)   // sera assigné automatiquement par Vue

onMounted(() => {
  inputEl.value.focus()     // ✅ accès via .value
})
</script>

<template>
  <input ref="inputEl" />   <!-- même nom que la variable -->
</template>
```

### 5.4 v-model

```vue
<script setup>
import { ref } from 'vue'

const search = ref('')
</script>

<template>
  <!-- v-model met à jour search.value automatiquement -->
  <input v-model="search" />
  <p>Recherche : {{ search }}</p>
</template>
```

### 5.5 Computed dérivé d'un ref

```js
import { ref, computed } from 'vue'

const items = ref([{ price: 10 }, { price: 20 }])

const total = computed(() => {
  // ⚠️ DANS un computed, on utilise .value pour LIRE
  return items.value.reduce((sum, item) => sum + item.price, 0)
})

// Pour LIRE le computed depuis le script : total.value
// Depuis le template : {{ total }}
```

### 5.6 Watcher

```js
import { ref, watch } from 'vue'

const userId = ref(1)

watch(userId, async (newId, oldId) => {
  console.log(`userId est passé de ${oldId} à ${newId}`)
  // ⚠️ newId et oldId sont les valeurs déjà déballées (pas besoin de .value)
})
```

---

## 6. ref vs reactive

**Mots-clés** : reactive, comparaison, alternative, choisir, objet, primitive

Vue propose **deux** API de réactivité :

| | `ref()` | `reactive()` |
|---|---------|--------------|
| Accepte primitives ? | ✅ Oui | ❌ Non |
| Accepte objets ? | ✅ Oui (via `.value`) | ✅ Oui |
| Accès | `x.value` | `x.prop` |
| Remplacement complet | `x.value = newObj` ✅ | `x = newObj` ❌ (casse) |
| Déstructuration | Perd réactivité | Perd réactivité |
| Recommandation officielle | ✅ Par défaut | Cas spécifiques |

```js
// ref — fonctionne pour tout
const count = ref(0)
const user = ref({ name: 'Alice' })

// reactive — uniquement objets
const state = reactive({ count: 0, name: 'Alice' })
state.count++           // ✅
state = { count: 5 }    // ❌ casse la réactivité (impossible de remplacer)
```

**Recommandation** : utiliser `ref()` par défaut. C'est plus uniforme, et tu n'as jamais à te demander si ta variable est une primitive ou un objet.

---

## 7. Auto-déballage (auto-unwrap)

**Mots-clés** : auto-unwrap, déballage, template, automatique, sans .value

Dans certains contextes, Vue **déballe automatiquement** les refs — tu n'as PAS besoin d'écrire `.value`.

### ✅ Contextes où `.value` est implicite

```vue
<script setup>
const compteur = ref(0)
</script>

<template>
  <p>{{ compteur }}</p>           <!-- ✅ auto-déballé -->
  <p>{{ compteur + 1 }}</p>       <!-- ✅ auto-déballé -->
  <input v-model="compteur" />    <!-- ✅ auto-déballé -->
</template>
```

### ❌ Contextes où `.value` reste nécessaire

```js
// Dans le <script> :
const compteur = ref(0)

console.log(compteur.value)       // ✅ obligatoire
if (compteur.value > 10) {}       // ✅ obligatoire
compteur.value = 5                // ✅ obligatoire
```

### ⚠️ Subtilité — refs imbriqués

```js
const inner = ref(10)
const outer = ref({ inner })    // outer.value.inner est un ref imbriqué

// Dans le script
console.log(outer.value.inner.value)   // 10  — double .value

// Dans le template :  {{ outer.inner }}   ← Vue déballe les deux niveaux
```

---

## 8. Pièges classiques

**Mots-clés** : pièges, erreurs, bugs, anti-patterns, mistakes, problèmes

### Piège 1 — Réassignation au lieu de mutation

```js
const items = ref([1, 2, 3])

// ❌ MAUVAIS — on essaie de remplacer le ref
items = [4, 5, 6]
// → TypeError car items est const
// → même avec let, on perdrait la réactivité

// ✅ BON
items.value = [4, 5, 6]
```

### Piège 2 — Déstructuration

```js
const user = ref({ name: 'Alice', age: 30 })

// ❌ name et age sont des valeurs figées, non réactives
const { name, age } = user.value

// ✅ Soit garder l'accès complet
console.log(user.value.name)

// ✅ Soit utiliser toRefs (pour reactive) ou toRef (pour ref)
import { toRef } from 'vue'
const name = toRef(user.value, 'name')   // name.value reste réactif
```

### Piège 3 — Oublier `.value` dans une condition

```js
const isLoading = ref(true)

// ❌ TOUJOURS truthy (un ref est un objet)
if (isLoading) { ... }

// ✅ Lit la vraie valeur
if (isLoading.value) { ... }
```

### Piège 4 — Confondre tableau/objet et leur ref

```js
const list = ref([1, 2, 3])

// ❌ MAUVAIS : .push sur le ref lui-même
list.push(4)             // TypeError: list.push is not a function

// ✅ BON : .push sur la valeur (le tableau)
list.value.push(4)       // mutation détectée par Vue
```

### Piège 5 — Réassigner un ref dans une fonction callback

```js
const data = ref(null)

const loadData = async () => {
  // ❌ Remplace la variable locale, pas le contenu du ref
  data = await fetch('/api/data')

  // ✅ Met à jour le contenu réactif
  data.value = await fetch('/api/data').then(r => r.json())
}
```

---

## 9. Exemples réutilisables

**Mots-clés** : snippets, copy-paste, templates, modèles, composables

### 9.1 Composable de chargement de données

```js
// useFetch.js
import { ref } from 'vue'
import axios from 'axios'

export function useFetch(url) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const execute = async () => {
    loading.value = true
    error.value = null
    try {
      const res = await axios.get(url)
      data.value = res.data
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}
```

Usage :
```vue
<script setup>
import { useFetch } from './useFetch'

const { data: products, loading, error, execute } = useFetch('/api/products')
execute()
</script>

<template>
  <div v-if="loading">Chargement...</div>
  <div v-else-if="error">{{ error }}</div>
  <ul v-else>
    <li v-for="p in products" :key="p.id">{{ p.name }}</li>
  </ul>
</template>
```

### 9.2 Compteur avec increment/decrement

```vue
<script setup>
import { ref, computed } from 'vue'

const count = ref(0)
const isPositive = computed(() => count.value > 0)

const increment = () => count.value++
const decrement = () => count.value--
const reset = () => { count.value = 0 }
</script>

<template>
  <p :class="{ green: isPositive }">{{ count }}</p>
  <button @click="increment">+</button>
  <button @click="decrement">-</button>
  <button @click="reset">Reset</button>
</template>
```

### 9.3 Formulaire avec validation

```vue
<script setup>
import { ref, computed } from 'vue'

const email = ref('')
const password = ref('')

const isEmailValid = computed(() => /^.+@.+\..+$/.test(email.value))
const isPasswordValid = computed(() => password.value.length >= 8)
const canSubmit = computed(() => isEmailValid.value && isPasswordValid.value)

const submit = async () => {
  if (!canSubmit.value) return
  await axios.post('/api/login', {
    email: email.value,
    password: password.value
  })
}
</script>

<template>
  <form @submit.prevent="submit">
    <input v-model="email" type="email" />
    <input v-model="password" type="password" />
    <button :disabled="!canSubmit">Envoyer</button>
  </form>
</template>
```

### 9.4 Toggle / Modal

```vue
<script setup>
import { ref } from 'vue'

const isOpen = ref(false)
const toggle = () => { isOpen.value = !isOpen.value }
const open = () => { isOpen.value = true }
const close = () => { isOpen.value = false }
</script>

<template>
  <button @click="open">Ouvrir</button>
  <div v-if="isOpen" class="modal">
    <button @click="close">×</button>
    <slot />
  </div>
</template>
```

### 9.5 Liste avec ajout / suppression

```vue
<script setup>
import { ref } from 'vue'

const todos = ref([])
const newTodo = ref('')

const addTodo = () => {
  if (!newTodo.value.trim()) return
  todos.value.push({ id: Date.now(), text: newTodo.value, done: false })
  newTodo.value = ''
}

const removeTodo = (id) => {
  todos.value = todos.value.filter(t => t.id !== id)
}

const toggleTodo = (id) => {
  const todo = todos.value.find(t => t.id === id)
  if (todo) todo.done = !todo.done   // ✅ mutation d'un objet du tableau
}
</script>

<template>
  <input v-model="newTodo" @keyup.enter="addTodo" />
  <ul>
    <li v-for="todo in todos" :key="todo.id">
      <input type="checkbox" :checked="todo.done" @change="toggleTodo(todo.id)" />
      {{ todo.text }}
      <button @click="removeTodo(todo.id)">×</button>
    </li>
  </ul>
</template>
```

---

## 10. Cheatsheet rapide

**Mots-clés** : cheatsheet, résumé, antisèche, recap, mémo

```js
// ╔══════════════════════════════════════════════════════╗
// ║              REF — RAPPEL ULTRA-RAPIDE               ║
// ╠══════════════════════════════════════════════════════╣
// ║                                                      ║
// ║  Création        const x = ref(initialValue)         ║
// ║                                                      ║
// ║  Lecture script  x.value                             ║
// ║  Lecture template {{ x }}                            ║
// ║                                                      ║
// ║  Écriture        x.value = newValue   ✅              ║
// ║                  x = newValue          ❌ JAMAIS      ║
// ║                                                      ║
// ║  Mutation array  x.value.push(...)    ✅              ║
// ║  Mutation object x.value.key = ...    ✅              ║
// ║                                                      ║
// ║  Remplacement    x.value = [...]      ✅              ║
// ║                  x.value = {...}      ✅              ║
// ║                                                      ║
// ║  Calcul          x.value * 2          ✅              ║
// ║                  x * 2                ❌ NaN          ║
// ║                                                      ║
// ║  Condition       if (x.value) {}      ✅              ║
// ║                  if (x) {}            ❌ tjs truthy   ║
// ║                                                      ║
// ╚══════════════════════════════════════════════════════╝
```

---

## 🔍 Aide-mémoire pour Ctrl+F

| Tu cherches… | Mot-clé à taper |
|--------------|-----------------|
| Définition de ref | `Qu'est-ce qu'un ref` |
| Pourquoi `.value` existe | `Pourquoi .value` |
| Différence .value vs = | `différence fondamentale` |
| Règles à suivre | `Règles d'or` |
| Auto-déballage en template | `auto-déballage` ou `auto-unwrap` |
| Erreurs à éviter | `Pièges classiques` |
| Code à copier-coller | `Exemples réutilisables` |
| Rappel express | `Cheatsheet` |
| Comparaison avec reactive | `ref vs reactive` |
| DOM ref / element ref | `élément DOM` |
| v-model | `v-model` |
| Computed | `Computed dérivé` |
| Watcher | `Watcher` |
| Composable | `useFetch` ou `composable` |
| Tableau / Array | `Mutation array` ou `Liste avec ajout` |
| Formulaire | `Formulaire avec validation` |
