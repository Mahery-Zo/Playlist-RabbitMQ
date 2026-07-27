# Référence des Erreurs & Solutions — Projet GLPI

> **Utilisation** : `Ctrl+F` puis colle le message d'erreur (ou un mot-clé) pour trouver la solution.

---

## Table des matières

- [Vue / JavaScript (Frontend)](#1-vue--javascript-frontend)
- [API GLPI](#2-api-glpi)
- [Spring Boot](#3-spring-boot)
- [Node.js / SQLite / Express](#4-nodejs--sqlite--express)
- [Index de recherche rapide](#5-index-de-recherche-rapide)

---

## 1. Vue / JavaScript (Frontend)

---

### ERR-VUE-01 · `A module cannot have multiple default exports`

**Cause** : Deux `export default` dans le même fichier `.js` ou `.vue`.

**Solution** : Un fichier ne peut avoir qu'UN SEUL `export default`. Utilise des exports nommés pour les autres.

```js
// ❌ Interdit
export default function a() {}
export default function b() {}

// ✅ Correct
export default function a() {}
export function b() {}

// ✅ Ou tout en nommé
export function a() {}
export function b() {}
```

---

### ERR-VUE-02 · `<script setup> cannot contain ES module exports`

**Cause** : Tu as mis un `export` à l'intérieur d'un bloc `<script setup>` d'un composant `.vue`.

**Solution** : `<script setup>` exporte automatiquement tout ce qui est déclaré. Retire le mot `export`.

```vue
<!-- ❌ Interdit -->
<script setup>
export const maVariable = 'hello'
</script>

<!-- ✅ Correct — la variable est automatiquement exposée au template -->
<script setup>
const maVariable = 'hello'
</script>
```

> Si tu as besoin d'exporter des constantes réutilisables (comme `STATUS_MAP`), mets-les dans un fichier `.js` séparé (ex: `glpiTickets.js`).

---

### ERR-VUE-03 · `buildLookup is not defined`

**Cause** : Faute de casse à l'import. JavaScript est **case-sensitive**.

**Solution** : Vérifie que le nom importé correspond exactement à celui exporté.

```js
// ❌ Mauvaise casse
import { buildlookup } from './glpiHelpers'  // minuscule 'l'

// ✅ Correspond à l'export
import { buildLookup } from './glpiHelpers'  // majuscule 'L'
```

> **Astuce** : Utilise l'autocomplétion de l'IDE pour éviter ces erreurs.

---

### ERR-VUE-04 · `Promise.getAll is not a function`

**Cause** : Confusion de nom. La méthode s'appelle `Promise.all`, pas `Promise.getAll`.

**Solution** :

```js
// ❌ N'existe pas
const results = await Promise.getAll([fetch1, fetch2])

// ✅ Correct
const results = await Promise.all([fetch1, fetch2])
```

---

### ERR-VUE-05 · `items is not iterable`

**Cause** : `getAll()` n'a pas été `await`. Tu itères sur une `Promise` au lieu d'un tableau.

**Solution** : Ajoute `await` devant l'appel asynchrone.

```js
// ❌ items est une Promise (pas un tableau)
const items = getAll('Assistance/Ticket')
for (const i of items) { ... }  // 💥 not iterable

// ✅ items est un tableau
const items = await getAll('Assistance/Ticket')
for (const i of items) { ... }  // ✅
```

---

### ERR-VUE-06 · `Object is not iterable (cannot read property Symbol(Symbol.iterator))`

**Cause** : `Promise.all(a, b)` avec des arguments séparés au lieu d'un **tableau**.

**Solution** : `Promise.all` prend UN tableau, pas des arguments séparés.

```js
// ❌ Arguments séparés
const [a, b] = await Promise.all(fetchA(), fetchB())

// ✅ Tableau
const [a, b] = await Promise.all([fetchA(), fetchB()])
```

---

### ERR-VUE-07 · `rows.length is not a function`

**Cause** : `.length` est une **propriété**, pas une **fonction**. Tu as mis des parenthèses.

**Solution** :

```js
// ❌ Parenthèses en trop
rows.length()

// ✅ C'est une propriété
rows.length
```

---

### ERR-VUE-08 · `computer.map is not a function`

**Cause** : `computer` est un `ref()`. Tu accèdes au wrapper Ref au lieu de sa valeur `.value`.

**Solution** :

```js
const computers = ref([])

// ❌ computers est un Ref, pas un tableau
computers.map(c => c.name)

// ✅ .value contient le vrai tableau
computers.value.map(c => c.name)
```

> **Rappel** : `.value` dans le `<script>`, jamais dans le `<template>`.

---

### ERR-VUE-09 · `Cannot read properties of undefined (reading 'name')` / `Cannot read properties of null (reading 'name')`

**Cause** : Accès `.name` sur un objet qui est `null` ou `undefined`. Fréquent avec les champs optionnels GLPI.

**Solution** : Utilise l'optional chaining `?.`

```js
// ❌ Plante si status est null
selected.status.name

// ✅ Renvoie undefined au lieu de planter
selected.status?.name
```

**Cas fréquents dans le projet** :
```js
all1.status?.name           // asset sans statut
all1.location?.name         // asset sans localisation
all1.manufacturer?.name     // asset sans fabricant
all1.model?.name            // asset sans modèle
all1.user?.name             // asset sans utilisateur
t.status?.id                // ticket sans statut chargé
LastCout?.cout              // aucun coût trouvé (null)
```

---

### ERR-VUE-10 · `Cannot read properties of undefined (reading 'split')`

**Cause** : Mauvais fichier CSV importé. La colonne attendue n'existe pas → la valeur est `undefined` → `.split()` plante.

**Solution** :

```js
// ❌ row.Date est undefined si la colonne "Date" n'existe pas dans le CSV
const [day, month, year] = row.Date.split('/')

// ✅ Vérifier d'abord
if (!row.Date) { console.error('Colonne Date manquante'); continue }
const [day, month, year] = row.Date.split('/')

// ✅ Ou avec optional chaining + valeur par défaut
const parts = row.Date?.split('/') || []
```

> **Astuce** : vérifie les en-têtes du CSV avant de parser les lignes.

---

### ERR-VUE-11 · `Invalid left-hand side in assignment`

**Cause** : Affectation `=` dans un endroit interdit, typiquement dans un `@click`.

**Solution** :

```html
<!-- ❌ Tente d'assigner à un appel de fonction -->
<div @click="selectTicket(i) = i">

<!-- ✅ Juste appeler la fonction -->
<div @click="selectTicket(i)">

<!-- ✅ Ou assigner à une variable -->
<div @click="selected = i">
```

---

### ERR-VUE-12 · `NaN` affiché dans l'interface

**Cause** : Calcul impliquant `undefined` ou concaténation de `.toFixed()`.

**Solution** : Voir le guide complet → [DOC_BUGS_NULL_NAN.md](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/docs/DOC_BUGS_NULL_NAN.md)

```js
// ❌ NaN si glpi[type] est undefined
glpi[type] += partGLPI

// ✅ Initialiser à 0 la première fois
glpi[type] = (glpi[type] || 0) + partGLPI

// ❌ Concaténation de strings → NaN
a.toFixed(2) + b.toFixed(2)  // "80.00" + "50.00" = "80.0050.00"

// ✅ Additionner d'abord, formater ensuite
(a + b).toFixed(2)
```

---

### ERR-VUE-13 · `...length undefined` / `...cout undefined`

**Cause** : Résultat de `fetch` non parsé. Tu lis les champs sur l'objet `Response` au lieu des données JSON.

**Solution** :

```js
// ❌ res est un objet Response, pas les données
const res = await fetch('http://localhost:8083/api/cout/5/last')
res.cout       // undefined !
res.length     // undefined !

// ✅ Parser le JSON d'abord
const res = await fetch('http://localhost:8083/api/cout/5/last')
const data = await res.json()
data.cout      // 300
```

---

## 2. API GLPI

---

### ERR-GLPI-01 · `CORS policy: blocked by CORS policy: No 'Access-Control-Allow-Origin' header`

**Cause** : Appels directs du navigateur vers GLPI sans passer par le proxy Vite.

**Solution** : Configure le proxy dans `vite.config.js` pour rediriger `/glpi-api` vers le serveur GLPI.

```js
// vite.config.js
export default defineConfig({
  server: {
    proxy: {
      '/glpi-api': {
        target: 'http://votre-serveur-glpi',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/glpi-api/, '')
      }
    }
  }
})
```

> Le navigateur n'appelle jamais GLPI directement. Il passe par `http://localhost:5173/glpi-api/...` et Vite redirige.

---

### ERR-GLPI-02 · `Invalid JSON body`

**Cause** : Header `Content-Type: application/json` envoyé sur une requête `GET`. GLPI tente de parser un body JSON qui n'existe pas.

**Solution** : Ne pas mettre `Content-Type: application/json` sur les requêtes GET.

```js
// ❌ Content-Type inutile et problématique sur un GET
const res = await fetch(url, {
  method: 'GET',
  headers: { ...headers(), 'Content-Type': 'application/json' }
})

// ✅ Pas de Content-Type sur un GET
const res = await fetch(url, {
  method: 'GET',
  headers: headers()  // seulement Authorization + App-Token
})
```

---

### ERR-GLPI-03 · `You are not authenticated` / `ERROR_UNAUTHENTICATED`

**Cause** : Plusieurs causes possibles :
1. Token expiré
2. `headers()` appelé sans les parenthèses → retourne la fonction au lieu de l'objet
3. `Content-Type: application/json` au lieu de `App-Token` dans les headers

**Solution** :

```js
// ❌ headers sans () → envoie la fonction elle-même
headers: headers

// ✅ Appeler la fonction
headers: headers()

// ❌ Mauvais header
headers: { 'Content-Type': 'application/json' }

// ✅ Bons headers
headers: {
  'Authorization': `Bearer ${token}`,
  'App-Token': APP_TOKEN
}
```

> Vérifie aussi que le token n'a pas expiré. Relogge-toi si nécessaire.

---

### ERR-GLPI-04 · `Status of items doit être unique !` / Erreur création dropdown

**Cause** : Tu tentes de recréer un dropdown (State, Location, Manufacturer…) qui existe déjà dans GLPI.

**Solution** : Cherche d'abord si l'élément existe (lookup/cache), puis crée seulement s'il n'existe pas.

```js
// ✅ Pattern utilisé dans glpiAssets.js
async function ensureDropdown(endpoint, name, cache) {
    if (!name) return 0
    const key = name.toLowerCase()
    if (cache[key]) return cache[key]        // déjà en cache → pas de création
    try {
        const res = await CreateObj(endpoint, { name })
        cache[key] = res.id
        return res.id
    } catch (e) {
        // existe déjà côté GLPI → recharger le cache
        const items = await getAll(endpoint)
        for (const it of items) if (it.name) cache[it.name.toLowerCase()] = it.id
        return cache[key] || 0
    }
}
```

---

### ERR-GLPI-05 · `400 Bad Request` (sur un GET)

**Cause** : Paramètres de l'API v1 (comme `range`) utilisés sur l'API v2.

**Solution** : Utilise les paramètres corrects pour la version de l'API :

```js
// ❌ Paramètre v1 sur endpoint v2
fetch('/glpi-api/Assistance/Ticket?range=0-50')

// ✅ Paramètres v2
fetch('/glpi-api/Assistance/Ticket?start=0&limit=300')
```

| API v1 | API v2 |
|---|---|
| `range=0-50` | `start=0&limit=50` |
| `/apirest.php/Ticket` | `/Assistance/Ticket` |

---

### ERR-GLPI-06 · `Duplicate entry 'Computer-0-0' for key 'unicity'`

**Cause** : `tickets_id` ou `items_id` vaut `0` car tu as utilisé les mauvais noms de champs pour `Item_Ticket`.

**Solution** : Utilise les bons noms de champs GLPI (attention au format `snake_case` avec `s`) :

```js
// ❌ Mauvais noms → valeurs à 0
body: JSON.stringify({
  input: { ticket_id: ticketId, itemtype, item_id: itemsId }
})

// ✅ Bons noms (avec le "s")
body: JSON.stringify({
  input: { tickets_id: ticketId, itemtype, items_id: itemsId }
})
```

---

### ERR-GLPI-07 · `Vous n'avez pas les droits requis`

**Cause** : 
1. Le profil GLPI de l'utilisateur n'a pas les droits UPDATE sur la ressource
2. Mauvais champs envoyés pour `Ticket_User` (GLPI ne reconnaît pas le champ → refuse)

**Solution** :

```js
// ✅ Champs corrects pour Ticket_User
body: JSON.stringify({
  input: {
    tickets_id: ticketId,
    users_id: userId,
    type: 2               // 1=Requester, 2=Technician, 3=Observer
  }
})
```

> Vérifie aussi les droits dans **GLPI → Administration → Profils**.

---

### ERR-GLPI-08 · Page HTML `Error - GLPI` (500 Internal Server Error)

**Cause** : `itemtype` est `null` ou vide lors de la création d'un `Item_Ticket`.

**Solution** : Vérifie que `itemtype` est défini avant l'appel.

```js
// ❌ asset peut être undefined → itemtype = undefined
const asset = assetMap?.[assetName]
await linkItemToTicket(result.id, asset.itemtype, asset.id)

// ✅ Vérifier d'abord
const asset = assetMap?.[assetName]
if (asset) {
    await linkItemToTicket(result.id, asset.itemtype, asset.id)
}
```

---

### ERR-GLPI-09 · `paramètre(s) login, password... manquants` / `ERROR_LOGIN_PARAMETERS_MISSING`

**Cause** : L'authentification Basic Auth n'est pas transmise correctement par le proxy. GLPI ne reçoit pas les identifiants.

**Solution** : Passe les identifiants en **query string** au lieu du header `Authorization: Basic`.

```js
// ❌ Basic Auth bloqué par le proxy
fetch('/glpi-api/v1/initSession', {
  headers: { 'Authorization': 'Basic ' + btoa(login + ':' + password) }
})

// ✅ Query string
const params = new URLSearchParams({ login: username, password })
fetch(`/glpi-api/v1/initSession?${params}`, {
  headers: { 'App-Token': APP_TOKEN }
})
```

---

## 3. Spring Boot

---

### ERR-SPRING-01 · `Malformed POM... Expected root element 'project'`

**Cause** : Le fichier `pom.xml` a été remplacé par uniquement le bloc `<dependencies>` sans la structure complète.

**Solution** : Le `pom.xml` doit commencer par `<project>` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>...</parent>
    <groupId>com.example</groupId>
    <artifactId>kanban</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <!-- tes dépendances ici -->
    </dependencies>
</project>
```

---

### ERR-SPRING-02 · `Interface ... should be declared in a file named ...`

**Cause** : Le nom du fichier `.java` ne correspond pas au nom de la classe/interface déclarée (sensible à la casse).

**Solution** : Le nom du fichier doit **exactement** correspondre au nom de la classe.

```
❌ Fichier : kanbanRepository.java  → contient : interface KanbanRepository
✅ Fichier : KanbanRepository.java  → contient : interface KanbanRepository
```

---

### ERR-SPRING-03 · `Unable to find a suitable main class`

**Cause** : Le fichier `KanbanApplication.java` est vide ou ne contient pas la méthode `main`.

**Solution** :

```java
@SpringBootApplication
public class KanbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}
```

---

### ERR-SPRING-04 · `Failed to determine a suitable driver class`

**Cause** : Le fichier `application.properties` est manquant ou ne contient pas la config de la base de données.

**Solution** : Crée/complète `src/main/resources/application.properties` :

```properties
# Exemple SQLite
spring.datasource.url=jdbc:sqlite:kanban.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

# Ou H2 en mémoire
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
```

---

### ERR-SPRING-05 · `Unsatisfied dependency expressed through constructor parameter 0`

**Cause** : L'entité JPA n'a pas l'annotation `@Entity` → Spring ne crée pas le Repository associé → l'injection échoue.

**Solution** :

```java
// ❌ Manque @Entity
public class KanbanConfig {
    @Id
    private Long id;
}

// ✅ Avec @Entity
@Entity
public class KanbanConfig {
    @Id
    private Long id;
}
```

> Vérifie aussi que le package de l'entité est scanné par Spring (même package ou sous-package de `@SpringBootApplication`).

---

## 4. Node.js / SQLite / Express

---

### ERR-NODE-01 · `Invalid character found in method name [0x16 0x03...]`

**Cause** : Tu envoies une requête `https://` vers un serveur qui écoute en `http://` (port 8083 local).

**Solution** :

```js
// ❌ https vers un serveur http local
fetch('https://localhost:8083/api/kanban-colors')

// ✅ http
fetch('http://localhost:8083/api/kanban-colors')
```

> `0x16 0x03` est le début d'un handshake TLS. Le serveur Express reçoit des octets TLS au lieu de texte HTTP → erreur.

---

### ERR-NODE-02 · `near "NO": syntax error` (SQLite)

**Cause** : Faute de frappe : `IF NO EXISTS` au lieu de `IF NOT EXISTS`.

**Solution** :

```sql
-- ❌ Typo
CREATE TABLE IF NO EXISTS kanban_config (...)

-- ✅ Correct
CREATE TABLE IF NOT EXISTS kanban_config (...)
```

---

### ERR-NODE-03 · `near ")": syntax error` (SQLite)

**Cause** : Virgule en trop avant la parenthèse fermante dans le `CREATE TABLE`.

**Solution** :

```sql
-- ❌ Virgule en trop après le dernier champ
CREATE TABLE cout (
    id INTEGER PRIMARY KEY,
    cout REAL,
    idTicket INTEGER,    -- ← virgule en trop
);

-- ✅ Pas de virgule après le dernier champ
CREATE TABLE cout (
    id INTEGER PRIMARY KEY,
    cout REAL,
    idTicket INTEGER
);
```

---

### ERR-NODE-04 · `No 'Access-Control-Allow-Origin'` (depuis le frontend vers Node)

**Cause** : Le backend Node n'a pas été redémarré après l'ajout d'une nouvelle route ou de `cors()`.

**Solution** :

1. Vérifie que `cors()` est bien en place dans `index.js` :
```js
const cors = require('cors')
app.use(cors())   // ← avant toutes les routes
```

2. **Redémarre le serveur** après chaque modification de `index.js` :
```bash
# Ctrl+C pour arrêter
node index.js     # relancer
```

---

### ERR-NODE-05 · `Cannot GET /api/kanban-colors`

**Cause** : La route est définie sans le `/` initial.

**Solution** :

```js
// ❌ Manque le / au début
app.get('api/kanban-colors', ...)

// ✅ Commence par /
app.get('/api/kanban-colors', ...)
```

---

### ERR-NODE-06 · `Failed to fetch` / `ERR_CONNECTION_REFUSED`

**Cause** : Plusieurs causes possibles :
1. Le backend Node est **éteint**
2. Le **port** est incorrect
3. `https` au lieu de `http`

**Solution** : Checklist de diagnostic :

```
1. Le serveur tourne-t-il ?
   → Terminal : node index.js
   → Tu dois voir : "API Node sur http://localhost:8083"

2. Le port est-il correct ?
   → Vérifie dans index.js : app.listen(8083, ...)
   → Vérifie dans le frontend : fetch('http://localhost:8083/...')

3. C'est bien http:// ?
   → Pas https:// sur localhost

4. Le firewall bloque-t-il ?
   → Essaie dans le navigateur : http://localhost:8083/api/kanban-colors
```

---

## 5. Index de recherche rapide

> Copie-colle le message d'erreur dans `Ctrl+F` pour trouver la section correspondante.

| Mot-clé à chercher | Section |
|---|---|
| `multiple default exports` | ERR-VUE-01 |
| `cannot contain ES module exports` | ERR-VUE-02 |
| `is not defined` | ERR-VUE-03 |
| `is not a function` | ERR-VUE-04, 07, 08 |
| `is not iterable` | ERR-VUE-05, 06 |
| `Symbol.iterator` | ERR-VUE-06 |
| `Cannot read properties of undefined` | ERR-VUE-09, 10 |
| `Cannot read properties of null` | ERR-VUE-09 |
| `invalid left-hand side` | ERR-VUE-11 |
| `NaN` | ERR-VUE-12 |
| `undefined` (valeur) | ERR-VUE-13 |
| `CORS` | ERR-GLPI-01, ERR-NODE-04 |
| `Invalid JSON body` | ERR-GLPI-02 |
| `UNAUTHENTICATED` | ERR-GLPI-03 |
| `doit être unique` | ERR-GLPI-04 |
| `400 Bad Request` | ERR-GLPI-05 |
| `Duplicate entry` | ERR-GLPI-06 |
| `droits requis` | ERR-GLPI-07 |
| `500` / `Error - GLPI` | ERR-GLPI-08 |
| `login, password` / `ERROR_LOGIN_PARAMETERS_MISSING` | ERR-GLPI-09 |
| `Malformed POM` | ERR-SPRING-01 |
| `should be declared in a file named` | ERR-SPRING-02 |
| `suitable main class` | ERR-SPRING-03 |
| `suitable driver class` | ERR-SPRING-04 |
| `Unsatisfied dependency` | ERR-SPRING-05 |
| `Invalid character` / `0x16` | ERR-NODE-01 |
| `near "NO"` | ERR-NODE-02 |
| `near ")"` | ERR-NODE-03 |
| `Access-Control-Allow-Origin` | ERR-NODE-04 |
| `Cannot GET` | ERR-NODE-05 |
| `Failed to fetch` / `ERR_CONNECTION_REFUSED` | ERR-NODE-06 |
