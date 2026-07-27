# Guide Pratique — Bugs `null`, `undefined`, `NaN` et pièges courants

> Guide basé sur les bugs réellement rencontrés dans le projet GLPI_NewApp.

---

## Table des matières

1. [undefined / null — Propriété inexistante](#1-undefined--null--accéder-à-une-propriété-qui-nexiste-pas)
2. [Valeur manquante — Valeur par défaut](#2-valeur-manquante--donner-une-valeur-par-défaut)
3. [NaN — Calcul avec valeur non-numérique](#3-nan--calcul-avec-une-valeur-non-numérique)
4. [Le piège fetch](#4-le-piège-fetch)
5. [Le piège ref (.value)](#5-le-piège-ref-value)
6. [Tableaux vides / non-itérables](#6-tableaux-vides--non-itérables)
7. [Tableau récapitulatif](#7-tableau-récapitulatif)
8. [Le réflexe à prendre](#8-le-réflexe-à-prendre)

---

## 1. `undefined` / `null` — Accéder à une propriété qui n'existe pas

### Symptôme

```
Cannot read properties of undefined (reading 'xxx')
Cannot read properties of null (reading 'xxx')
```

### Cause

Tu lis `a.b.c` mais `a.b` est `null` ou `undefined`.

### Solution : l'optional chaining `?.`

```js
// ❌ Plante si status est null
selected.status.name

// ✅ Renvoie undefined au lieu de planter
selected.status?.name
```

### Exemples concrets dans le projet

```js
all1.status?.name          // status peut être null (asset sans statut)
LastCout?.cout             // LastCout peut être null (aucun coût trouvé)
t.status?.id               // status peut être absent sur certains tickets
row.Status?.toLowerCase()  // Status peut être vide dans le CSV
```

### Règle

> Dès qu'une valeur **peut être `null`** (un champ GLPI optionnel, un résultat de recherche, une réponse API), mets `?.` avant d'accéder à sa propriété.

---

## 2. Valeur manquante — Donner une valeur par défaut

### `|| 0` ou `|| ''` (valeur par défaut si « falsy »)

```js
const cout = Number(c.cost_fixed) || 0     // 0 si undefined/NaN
userLookup[name] || 0                       // 0 si user pas trouvé
values[i] || ''                             // '' si valeur CSV manquante
```

### `??` (nullish coalescing) — Plus précis

Remplace **seulement** si `null` ou `undefined` (garde le `0` ou `''`) :

```js
const x = valeur ?? 0
```

### Différence entre `||` et `??`

```js
0 || 5       // → 5   (0 est "falsy", donc remplacé)
0 ?? 5       // → 0   (0 n'est PAS null/undefined, donc gardé)

'' || 'vide' // → 'vide'  ('' est "falsy")
'' ?? 'vide' // → ''       ('' n'est PAS null/undefined)
```

### Quand utiliser quoi ?

| Situation | Outil | Pourquoi |
|---|---|---|
| Valeur pour un calcul | `\|\| 0` | On veut remplacer `0`, `NaN`, `undefined` |
| Valeur qui peut être `0` légitimement | `?? 0` | On ne veut PAS écraser un vrai `0` |
| Texte par défaut | `\|\| ''` | On veut remplacer `null`, `undefined` |

---

## 3. `NaN` — Calcul avec une valeur non-numérique

### Symptôme

Affichage `NaN` dans le tableau, résultat de calcul incorrect.

### Causes fréquentes

```js
undefined + 5              // → NaN  (champ absent)
glpi[type] += 10           // → NaN si glpi[type] était undefined au départ
"80.00" + "50.00"          // → "80.0050.00" (concaténation de texte !)
Number("80.0050.00")       // → NaN
```

### Solutions

#### a) `Number(x) || 0` pour convertir et sécuriser

```js
const duration = Number(c.duration) || 0
const costTime = Number(c.cost_time) || 0
```

#### b) Initialiser l'accumulateur avant d'additionner

```js
// ❌ NaN si glpi[type] est undefined la première fois
glpi[type] += valeur

// ✅ Part de 0 la première fois
glpi[type] = (glpi[type] || 0) + valeur
```

Exemple concret dans `CoutView.vue` :
```js
for (const it of items) {
    glpi[it.itemtype] = Number((glpi[it.itemtype] || 0) + partGLPI)
    sqlite[it.itemtype] = Number((sqlite[it.itemtype] || 0) + partSuper)
}
```

#### c) Calculer sur des nombres, formater à la fin

```js
// ✅ Addition puis formatage
(a + b).toFixed(2)

// ❌ Concatène 2 textes → produit une string invalide → NaN
a.toFixed(2) + b.toFixed(2)  // "80.00" + "50.00" = "80.0050.00"
```

### Schéma de la propagation du NaN

```
undefined ──→ undefined + 5 ──→ NaN ──→ NaN + 10 ──→ NaN ──→ NaN.toFixed(2) ──→ "NaN"
                                         ↑
                                   NaN est contagieux !
                                   Toute opération avec NaN donne NaN
```

---

## 4. Le piège `fetch`

C'est la **cause de beaucoup de `undefined`** dans le projet.

### Le problème

```js
const res = await fetch(url)    // res = objet Response (PAS les données !)
res.cout                        // ❌ undefined — Response n'a pas de champ "cout"
```

### La solution

```js
const res = await fetch(url)
const data = await res.json()   // ✅ Parse le corps JSON
data.cout                       // ✅ La vraie valeur
```

### Exemple concret dans le projet

```js
// ❌ Incorrect
const res = await fetch(`http://localhost:8083/api/cout/${id}/last`)
res.cout  // undefined !

// ✅ Correct
const res = await fetch(`http://localhost:8083/api/cout/${id}/last`)
const data = await res.json()
data.cout  // 300
```

### Schéma du flux fetch

```
fetch(url)
   │
   ▼
Response {           ← Objet Response (métadonnées HTTP)
  ok: true,
  status: 200,
  json()  ──────→   { cout: 300, idTicket: 12 }   ← Les vraies données
}
```

### Règle

> `fetch` → **toujours** `await res.json()` avant de lire les champs.

---

## 5. Le piège `ref` (`.value`)

### Le problème

```js
const x = ref(null)

x.cout          // ❌ undefined — x est le Ref wrapper, pas la valeur
x.value.cout    // ✅ dans le <script>
```

### Règles

| Contexte | Syntaxe | Pourquoi |
|---|---|---|
| `<script setup>` | `x.value.cout` | Le ref doit être « déballé » manuellement |
| `<template>` | `{{ x.cout }}` | Vue déballe automatiquement les refs |

### Exemples concrets

```js
// Dans <script setup>
const selected = ref(null)
selected.value.id        // ✅ accès correct dans le script
selected.id              // ❌ undefined

const cout = ref(0)
cout.value               // ✅ → 0
Number(cout.value)       // ✅ → 0
```

```html
<!-- Dans <template> -->
{{ selected.id }}        <!-- ✅ Vue déballe automatiquement -->
{{ selected.value.id }}  <!-- ❌ inutile et peut planter -->
```

### Attention : un objet venant de `.json()` n'est PAS un ref

```js
const data = await res.json()
data.cout         // ✅ accès direct, pas de .value
data.value        // ❌ undefined — ce n'est pas un ref
```

---

## 6. Tableaux vides / non-itérables

### Garantir un tableau

```js
const data = await res.json()
return Array.isArray(data) ? data : []   // ✅ Garantit un tableau
```

### `.get()` vs `.all()` (better-sqlite3)

| Méthode | Retour | Usage |
|---|---|---|
| `.get()` | 1 objet (ou `undefined`) | Chercher UNE ligne |
| `.all()` | Tableau d'objets | Chercher PLUSIEURS lignes |

```js
// ❌ .get() retourne un objet, pas un tableau
db.prepare('SELECT * from cout').get()
// → { id: 1, cout: 300 }
// .length → undefined !

// ✅ .all() retourne un tableau
db.prepare('SELECT * from cout').all()
// → [{ id: 1, cout: 300 }, { id: 2, cout: 150 }]
// .length → 2
```

### Longueur sûre d'un tableau potentiellement null

```js
items?.length || 0       // ✅ 0 si items est null/undefined
items.length             // ❌ plante si items est null
```

---

## 7. Tableau récapitulatif

| Problème | Outil | Exemple |
|---|---|---|
| Propriété sur `null`/`undefined` | `?.` | `a.status?.name` |
| Valeur manquante | `\|\| 0` ou `?? 0` | `Number(x) \|\| 0` |
| Texte → nombre | `Number()` | `Number("50")` |
| Accumulateur `NaN` | `(x \|\| 0) +` | `glpi[t] = (glpi[t] \|\| 0) + v` |
| Résultat de `fetch` | `await res.json()` | `const d = await res.json()` |
| Ref Vue | `.value` (script seulement) | `t.value.id` |
| Tableau sûr | `Array.isArray` / `.all()` | `Array.isArray(d) ? d : []` |

---

## 8. Le réflexe à prendre

Avant d'accéder à une donnée venant de **l'extérieur** (API GLPI, fetch, CSV, base SQLite), pose-toi ces 4 questions :

```
┌─────────────────────────────────────────────────────┐
│  La donnée peut-elle être null/absente ?             │
│  → Utilise ?.                                        │
│                                                      │
│  Dois-je l'utiliser dans un calcul ?                 │
│  → Number(x) || 0                                   │
│                                                      │
│  Est-ce un résultat de fetch ?                       │
│  → await res.json() d'abord                          │
│                                                      │
│  Est-ce un ref Vue ?                                 │
│  → .value dans le script, rien dans le template      │
└─────────────────────────────────────────────────────┘
```

### Checklist rapide pour chaque nouvelle variable

- [ ] Vérifier si elle peut être `null` → ajouter `?.`
- [ ] Vérifier si elle entre dans un calcul → ajouter `Number(x) || 0`
- [ ] Vérifier si elle vient d'un `fetch` → `await res.json()`
- [ ] Vérifier si c'est un `ref` → `.value` dans le `<script>`
- [ ] Vérifier si c'est un tableau → `Array.isArray()` ou `.all()`
