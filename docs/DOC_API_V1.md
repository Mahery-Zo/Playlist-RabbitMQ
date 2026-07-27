# Documentation des Endpoints API GLPI v1 utilisés dans GLPI_NewApp

> Ces endpoints utilisent l'ancienne **API REST v1** de GLPI (`/apirest.php`), proxifiée via `/glpi-api/v1/...`.
> Ils sont utilisés car certaines fonctionnalités (lier un asset à un ticket, uploader un document, assigner un technicien) **ne sont pas disponibles dans l'API v2**.

---

## Table des matières

1. [Authentification](#1-initSession--authentification-v1)
2. [Item_Ticket — Lier un asset à un ticket](#2-item_ticket--lier-un-asset-à-un-ticket)
3. [Item_Ticket — Lire les assets d'un ticket](#3-item_ticket--lire-les-assets-liés-à-un-ticket)
4. [Document — Upload de fichier](#4-document--upload-de-fichier)
5. [Ticket_User — Assigner un technicien](#5-ticket_user--assigner-un-technicien)
6. [Ticket_User — Lire les assignés d'un ticket](#6-ticket_user--lire-les-assignés-dun-ticket)
7. [Différences v1 vs v2](#7-différences-dauthentification-v1-vs-v2)
8. [Récapitulatif](#8-récapitulatif)

---

## Architecture d'authentification

Le projet utilise **deux systèmes d'authentification en parallèle** :

```
┌─────────────────────────────────────────────────────────────┐
│                       LOGIN                                  │
│                                                              │
│  1. POST /glpi-api/token  (OAuth2 v2)                       │
│     → access_token  → localStorage['access_token']          │
│     → Utilisé par : getAll, CreateObj, updateTicketStatus…  │
│                                                              │
│  2. GET /glpi-api/v1/initSession  (Session v1)              │
│     → session_token → localStorage['session_token']         │
│     → Utilisé par : Item_Ticket, Document, Ticket_User      │
└─────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> Les endpoints v1 utilisent le header `Session-Token`, pas `Authorization: Bearer`.
> Les endpoints v2 utilisent `Authorization: Bearer {access_token}`.

---

## 1. `initSession` — Authentification v1

### Endpoint

```
GET /glpi-api/v1/initSession?login={username}&password={password}
```

### Fichier source

[glpiAuth.js:L28-35](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiAuth.js#L28-L35)

### Code

```js
const params = new URLSearchParams({ login: username, password })
const sessRes = await fetch(`${GLPI_URL}/v1/initSession?${params}`, {
    headers: { 'App-Token': APP_TOKEN }
})
if (sessRes.ok) {
    const sess = await sessRes.json()
    localStorage.setItem('session_token', sess.session_token)
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `App-Token` | `import.meta.env.VITE_APP_TOKEN` | ✅ Oui |

### Paramètres (query string)

| Paramètre | Type | Description |
|---|---|---|
| `login` | string | Nom d'utilisateur GLPI |
| `password` | string | Mot de passe |

### Réponse JSON

```json
{
    "session_token": "abc123def456ghi789"
}
```

| Champ | Type | Description |
|---|---|---|
| `session_token` | string | Jeton de session v1, à passer dans le header `Session-Token` pour les appels suivants |

### Stockage

```js
localStorage.setItem('session_token', sess.session_token)
```

> [!NOTE]
> Les identifiants sont passés en **query string** et non en Basic Auth, car le proxy Vite ne transmet pas correctement le header `Authorization: Basic`.

---

## 2. `Item_Ticket` — Lier un asset à un ticket

### Endpoint

```
POST /glpi-api/v1/Item_Ticket
```

### Fichier source

[glpiHelpers.js:L70-85](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L70-L85)

### Code

```js
export async function linkItemToTicket(ticketId, itemtype, itemsId) {
    const session = localStorage.getItem('session_token')
    const res = await fetch(`${GLPI_URL}/v1/Item_Ticket`, {
        method: 'POST',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN,
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            input: { tickets_id: ticketId, itemtype, items_id: itemsId }
        })
    })
    if (!res.ok) throw new Error('Erreur lien Item_Ticket :' + await res.text())
    return res.json()
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `Session-Token` | `localStorage['session_token']` | ✅ Oui |
| `App-Token` | `VITE_APP_TOKEN` | ✅ Oui |
| `Content-Type` | `application/json` | ✅ Oui |

### Body (JSON)

```json
{
    "input": {
        "tickets_id": 12,
        "itemtype": "Computer",
        "items_id": 5
    }
}
```

| Champ | Type | Description |
|---|---|---|
| `input.tickets_id` | integer | ID du ticket GLPI |
| `input.itemtype` | string | Type d'asset : `"Computer"`, `"Monitor"`, `"Phone"` |
| `input.items_id` | integer | ID de l'asset à lier |

> [!WARNING]
> Les noms de champs sont avec un **"s"** : `tickets_id`, `items_id` (pas `ticket_id`, `item_id`).
> Si tu utilises les mauvais noms, les valeurs seront `0` → erreur `Duplicate entry 'Computer-0-0'`.

### Réponse JSON

```json
{
    "id": 42,
    "message": ""
}
```

| Champ | Type | Description |
|---|---|---|
| `id` | integer | ID de la relation Item_Ticket créée |
| `message` | string | Message (vide si succès) |

### Appelée depuis

- [glpiTickets.js:L73](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiTickets.js#L73) — lors de l'import CSV de tickets, pour lier les assets mentionnés dans la colonne `Items`

---

## 3. `Item_Ticket` — Lire les assets liés à un ticket

### Endpoint

```
GET /glpi-api/v1/Ticket/{ticketId}/Item_Ticket
```

### Fichier source

[glpiHelpers.js:L103-114](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L103-L114)

### Code

```js
export async function getTicketItems(ticketId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket/${ticketId}/Item_Ticket`, {
        method: 'GET',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN
        }
    })
    if (!res.ok) return []
    return res.json()
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `Session-Token` | `localStorage['session_token']` | ✅ Oui |
| `App-Token` | `VITE_APP_TOKEN` | ✅ Oui |

### Paramètres URL

| Paramètre | Type | Description |
|---|---|---|
| `ticketId` | integer | ID du ticket dont on veut les items liés |

### Réponse JSON

```json
[
    {
        "id": 42,
        "tickets_id": 12,
        "itemtype": "Computer",
        "items_id": 5
    },
    {
        "id": 43,
        "tickets_id": 12,
        "itemtype": "Monitor",
        "items_id": 8
    }
]
```

| Champ | Type | Description |
|---|---|---|
| `id` | integer | ID de la relation Item_Ticket |
| `tickets_id` | integer | ID du ticket |
| `itemtype` | string | Type de l'asset lié (`"Computer"`, `"Monitor"`, `"Phone"`) |
| `items_id` | integer | ID de l'asset lié |

### Appelée depuis

- [PresenationTicket.vue:L201](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/views/frontoffice/PresenationTicket.vue#L201) — affichage de la fiche ticket (Kanban)
- [TIcketsList.vue:L34](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/views/backoffice/TIcketsList.vue#L34) — affichage de la fiche ticket (liste)
- [CoutView.vue:L59](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/views/backoffice/CoutView.vue#L59) — calcul du coût par catégorie d'asset

### Utilisation typique

Le résultat est ensuite utilisé pour charger le détail de chaque asset via l'API v2 :

```js
for (const it of itemsTickets.value) {
    const asset = await getOne(`Assets/${it.itemtype}`, it.items_id)
    // it.itemtype → "Computer", it.items_id → 5
    // → GET /glpi-api/Assets/Computer/5  (API v2)
}
```

---

## 4. `Document` — Upload de fichier

### Endpoint

```
POST /glpi-api/v1/Document
```

### Fichier source

[glpiHelpers.js:L86-102](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L86-L102)

### Code

```js
export async function uploadDocument(filename, blob, itemtype, itemsId) {
    const session = localStorage.getItem('session_token')
    const manifest = JSON.stringify({
        input: {
            name: filename,
            _filename: [filename],
            itemtype,
            items_id: itemsId
        }
    })
    const form = new FormData()
    form.append('uploadManifest', manifest)
    form.append('filename[0]', blob, filename)

    const res = await fetch(`${GLPI_URL}/v1/Document`, {
        method: 'POST',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN
        },
        body: form
    })
    if (!res.ok) throw new Error('Erreur upload Document: ' + await res.text())
    return res.json()
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `Session-Token` | `localStorage['session_token']` | ✅ Oui |
| `App-Token` | `VITE_APP_TOKEN` | ✅ Oui |

> [!IMPORTANT]
> **Pas de `Content-Type`** dans les headers ! C'est le navigateur qui le génère automatiquement avec le `boundary` du `FormData`.
> Ajouter manuellement `Content-Type: multipart/form-data` **cassera** l'upload.

### Body (FormData — multipart)

Le body est un `FormData` avec 2 champs :

| Champ FormData | Type | Contenu |
|---|---|---|
| `uploadManifest` | string (JSON) | Métadonnées du document (voir ci-dessous) |
| `filename[0]` | Blob/File | Le fichier binaire à uploader |

#### Contenu du `uploadManifest`

```json
{
    "input": {
        "name": "PC-ADM-001.jpg",
        "_filename": ["PC-ADM-001.jpg"],
        "itemtype": "Computer",
        "items_id": 5
    }
}
```

| Champ | Type | Description |
|---|---|---|
| `input.name` | string | Nom du document dans GLPI |
| `input._filename` | string[] | Liste des noms de fichiers (doit correspondre aux `filename[N]` du FormData) |
| `input.itemtype` | string | Type de l'asset auquel le document est lié |
| `input.items_id` | integer | ID de l'asset |

### Réponse JSON

```json
{
    "id": 15,
    "message": "Document successfully added. (PC-ADM-001.jpg)"
}
```

| Champ | Type | Description |
|---|---|---|
| `id` | integer | ID du document créé dans GLPI |
| `message` | string | Message de confirmation |

### Appelée depuis

- [glpiImages.js:L37](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiImages.js#L37) — upload d'images depuis un fichier ZIP lors de l'import d'assets

### Flux complet d'upload d'image

```
ZIP file
  │
  ▼
importImages() ── décompresse ──→ pour chaque fichier image :
  │
  ├─ detectType(bytes) → { ext: 'jpg', mime: 'image/jpeg' }
  │
  ├─ Cherche l'asset correspondant dans assetMap (nom du fichier = nom de l'asset)
  │
  └─ uploadDocument(correctedName, blob, asset.itemtype, asset.id)
       │
       ▼
     POST /v1/Document (FormData: manifest + blob)
```

---

## 5. `Ticket_User` — Assigner un technicien

### Endpoint

```
POST /glpi-api/v1/Ticket_User
```

### Fichier source

[glpiHelpers.js:L151-168](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L151-L168)

### Code

```js
export async function assignTechnician(ticketId, userId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket_User`, {
        method: 'POST',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN,
            'Content-type': 'application/json'
        },
        body: JSON.stringify({
            input: { tickets_id: ticketId, users_id: userId, type: 2 }
        })
    })
    if (!res.ok) throw new Error('Erreur lors de assignation Technicien' + await res.text())
    return res.json()
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `Session-Token` | `localStorage['session_token']` | ✅ Oui |
| `App-Token` | `VITE_APP_TOKEN` | ✅ Oui |
| `Content-Type` | `application/json` | ✅ Oui |

### Body (JSON)

```json
{
    "input": {
        "tickets_id": 12,
        "users_id": 3,
        "type": 2
    }
}
```

| Champ | Type | Description |
|---|---|---|
| `input.tickets_id` | integer | ID du ticket |
| `input.users_id` | integer | ID de l'utilisateur à assigner |
| `input.type` | integer | Rôle de l'utilisateur (voir tableau ci-dessous) |

#### Valeurs du champ `type`

| Valeur | Rôle |
|---|---|
| `1` | **Requester** (demandeur) |
| `2` | **Technician** (technicien assigné) |
| `3` | **Observer** (observateur) |

### Réponse JSON

```json
{
    "id": 28,
    "message": ""
}
```

| Champ | Type | Description |
|---|---|---|
| `id` | integer | ID de la relation Ticket_User créée |
| `message` | string | Message (vide si succès) |

### Appelée depuis

- [PresenationTicket.vue:L152](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/views/frontoffice/PresenationTicket.vue#L152) — quand on assigne un technicien via le dialog du Kanban

---

## 6. `Ticket_User` — Lire les assignés d'un ticket

### Endpoint

```
GET /glpi-api/v1/Ticket/{ticketId}/Ticket_User
```

### Fichier source

[glpiHelpers.js:L179-193](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L179-L193)

### Code

```js
export async function getTicketAssignees(ticketId) {
    const session = localStorage.getItem("session_token")
    const res = await fetch(`${GLPI_URL}/v1/Ticket/${ticketId}/Ticket_User`, {
        method: 'GET',
        headers: {
            'Session-Token': session,
            'App-Token': APP_TOKEN
        }
    })
    if (!res.ok) return []
    const data = await res.json()
    return (Array.isArray(data) ? data : []).filter(u => Number(u.type) === 2)
}
```

### Headers envoyés

| Header | Valeur | Obligatoire |
|---|---|---|
| `Session-Token` | `localStorage['session_token']` | ✅ Oui |
| `App-Token` | `VITE_APP_TOKEN` | ✅ Oui |

### Paramètres URL

| Paramètre | Type | Description |
|---|---|---|
| `ticketId` | integer | ID du ticket |

### Réponse JSON (brute de GLPI)

```json
[
    {
        "id": 28,
        "tickets_id": 12,
        "users_id": 3,
        "type": 1,
        "use_notification": 1,
        "alternative_email": ""
    },
    {
        "id": 29,
        "tickets_id": 12,
        "users_id": 7,
        "type": 2,
        "use_notification": 1,
        "alternative_email": ""
    }
]
```

| Champ | Type | Description |
|---|---|---|
| `id` | integer | ID de la relation Ticket_User |
| `tickets_id` | integer | ID du ticket |
| `users_id` | integer | ID de l'utilisateur |
| `type` | integer | Rôle : `1` = Requester, `2` = Technician, `3` = Observer |
| `use_notification` | integer | `1` si notifications activées |
| `alternative_email` | string | Email alternatif (optionnel) |

### Filtrage côté frontend

Le code filtre pour ne garder que les **techniciens** (`type === 2`) :

```js
return (Array.isArray(data) ? data : []).filter(u => Number(u.type) === 2)
```

---

## 7. Différences d'authentification v1 vs v2

| | API v1 | API v2 |
|---|---|---|
| **Base URL** | `/glpi-api/v1/...` | `/glpi-api/Assistance/...`, `/glpi-api/Assets/...` |
| **Authentification** | `Session-Token` | `Authorization: Bearer {access_token}` |
| **Obtention du token** | `GET /v1/initSession?login=...&password=...` | `POST /token` (OAuth2, grant_type=password) |
| **Header obligatoire** | `App-Token` + `Session-Token` | `App-Token` + `Authorization` |
| **Format du body** | `{ "input": { ... } }` | Données directes `{ "name": "...", ... }` |

### Exemple comparatif

```js
// ─── API v1 ───
fetch('/glpi-api/v1/Item_Ticket', {
    method: 'POST',
    headers: {
        'Session-Token': session,           // ← v1
        'App-Token': APP_TOKEN,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        input: { tickets_id: 12, ... }      // ← wrapper "input"
    })
})

// ─── API v2 ───
fetch('/glpi-api/Assistance/Ticket', {
    method: 'POST',
    headers: {
        'Authorization': `Bearer ${token}`, // ← v2 (OAuth2)
        'App-Token': APP_TOKEN,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        name: 'Mon ticket', ...             // ← données directes
    })
})
```

---

## 8. Récapitulatif

### Tous les endpoints v1 utilisés

| # | Méthode | Endpoint | Fonction JS | Fichier |
|---|---|---|---|---|
| 1 | `GET` | `/v1/initSession` | `login()` | [glpiAuth.js:L29](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiAuth.js#L29) |
| 2 | `POST` | `/v1/Item_Ticket` | `linkItemToTicket()` | [glpiHelpers.js:L72](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L72) |
| 3 | `GET` | `/v1/Ticket/{id}/Item_Ticket` | `getTicketItems()` | [glpiHelpers.js:L105](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L105) |
| 4 | `POST` | `/v1/Document` | `uploadDocument()` | [glpiHelpers.js:L95](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L95) |
| 5 | `POST` | `/v1/Ticket_User` | `assignTechnician()` | [glpiHelpers.js:L154](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L154) |
| 6 | `GET` | `/v1/Ticket/{id}/Ticket_User` | `getTicketAssignees()` | [glpiHelpers.js:L181](file:///c:/xampp/htdocs/S6/Eval2/GLPI_NewApp/src/services/glpiHelpers.js#L181) |

### Champs de réponse par endpoint

| Endpoint | Champs de réponse |
|---|---|
| `initSession` | `session_token` |
| `POST Item_Ticket` | `id`, `message` |
| `GET Item_Ticket` | `id`, `tickets_id`, `itemtype`, `items_id` |
| `POST Document` | `id`, `message` |
| `POST Ticket_User` | `id`, `message` |
| `GET Ticket_User` | `id`, `tickets_id`, `users_id`, `type`, `use_notification`, `alternative_email` |

### Headers requis pour TOUS les appels v1

```js
headers: {
    'Session-Token': localStorage.getItem('session_token'),  // obligatoire
    'App-Token': import.meta.env.VITE_APP_TOKEN              // obligatoire
}
// + 'Content-Type': 'application/json' pour les POST avec body JSON
// PAS de Content-Type pour les POST avec FormData (Document upload)
```
