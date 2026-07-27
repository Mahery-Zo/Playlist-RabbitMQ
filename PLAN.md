# Naina_Playlist — Plan d'architecture & d'implémentation

> Système de gestion MP3 + génération de playlists.
> Pipeline d'ingestion en **Java** communiquant par **RabbitMQ**, **API Spring Boot**, base **MySQL**, front **Vue 3**.

---

## 1. Vue d'ensemble

```
                 ┌──────────────────────── BACKEND DESKTOP (Java standalone) ────────────────────────┐
                 │                                                                                    │
   inbox/  ──▶  P1 Watcher ──(queue.new-files)──▶ P2 Metadata ──(queue.metadata)──▶ P3 Uploader      │
 (dépôt mp3)      │ copie                              │ extraction tags                │ POST + delete │
                 │                                                                       │             │
                 └───────────────────────────────────────────────────────────────────────┼───────────┘
                                                                                          │ HTTP (multipart)
                                                                                          ▼
                                                                             ┌─────────────────────────┐
                                          Vue 3 (Vite)  ◀── REST / stream ──▶│   API Spring Boot       │
                                          - CRUD                              │   + stockage fichiers   │
                                          - génération playlist               └───────────┬─────────────┘
                                          - lecture / download zip                         │ JPA
                                                                                           ▼
                                                                                       MySQL
```

**Principe message-broker (asynchrone)** : les 3 programmes ne s'appellent jamais directement. Chacun se connecte à RabbitMQ, **consomme** la queue qui le concerne et **produit** vers la suivante. Le message reçu *est* l'argument du programme. Si un programme tombe, les messages restent dans la queue → reprise sans perte.

---

## 2. Stack technique

| Couche | Choix | Librairie clé |
|--------|-------|---------------|
| Pipeline P1/P2/P3 | Java 19 standalone (Maven multi-module) | `com.rabbitmq:amqp-client`, `jaudiotagger`, `jackson`, `dotenv-java` |
| Logs | SLF4J + Logback | fichier rotatif par programme |
| Message broker | RabbitMQ | exchange direct + DLQ |
| API | Spring Boot 3 (Java 19) | Spring Web, Spring Data JPA, Spring Security (JWT) |
| DB | MySQL 8 | Flyway (migrations) |
| Front | Vue 3 + Vite | Pinia, Vue Router, Axios |
| Infra dev | Docker Compose | RabbitMQ + MySQL |

---

## 3. Arborescence du projet

```
Naina_Playlist/
├─ docker-compose.yml            # rabbitmq (+ UI :15672) + mysql
├─ pom.xml                       # parent Maven (modules)
├─ common/                       # DTOs partagés, noms de queues, sérialisation JSON
│   └─ src/main/java/.../model/  # NewFileMessage, SongMetadataMessage, ...
├─ watcher/   (P1)               # scan dossier + producer
├─ metadata/  (P2)               # extraction tags, consumer P1 / producer P3
├─ uploader/  (P3)               # POST API + suppression, consumer P2
├─ api/                          # Spring Boot (REST + stockage + MySQL)
│   └─ src/main/resources/db/migration/  # Flyway V1__init.sql ...
└─ front/                        # Vue 3 (projet npm séparé)
```

---

## 4. Le dépôt de musique (« Répertoire ») & cycle de vie d'un fichier

**Un seul dossier** configurable (`.env` → `REPERTOIRE_DIR`) stocke la musique : l'utilisateur y dépose ses `.mp3`.

- P1 maintient un **index local persistant** des fichiers déjà mis en queue (chemin + taille/hash) pour ne pas ré-enfiler un fichier encore en cours de traitement entre deux scans.
- La seule **copie** du fichier est celle que **l'API conserve dans son propre stockage** (`STORAGE_DIR`). Une fois l'upload confirmé, P3 **supprime l'original du répertoire**.

Cycle : **détecté → métadonnées extraites → uploadé à l'API (qui garde sa copie) → original supprimé du répertoire par P3**.

---

## 5. Les 3 programmes du pipeline

### P1 — Watcher (Producer)
- **Déclencheur** : scan périodique du `REPERTOIRE_DIR`, intervalle **configurable dans `.env`** (`SCAN_INTERVAL_SEC`, défaut 180 = 3 min).
- Détecte les **nouveaux** `.mp3` uniquement (filtre extension + **index local persistant** des fichiers déjà enfilés, par chemin + taille/hash) pour éviter les doublons entre deux scans.
- **Publie** un message par fichier vers `queue.new-files`.
- **Log** : fichier détecté, message publié.

Message publié (`NewFileMessage`) :
```json
{ "fileId": "uuid", "path": "C:/.../repertoire/song.mp3",
  "originalName": "song.mp3", "sizeBytes": 5242880, "detectedAt": "2026-06-10T10:00:00Z" }
```

### P2 — Metadata (Consumer P1 → Producer P3)
- **Consomme** `queue.new-files`.
- Extrait les tags ID3 + entête audio avec **jaudiotagger** : `title, artist, album, genre, year, durationSec, bitrate, sampleRate`.
- **Publie** vers `queue.metadata`. **Log** : extraction OK/KO. Ack après publication réussie.

Message publié (`SongMetadataMessage`) :
```json
{ "fileId": "uuid", "path": "C:/.../repertoire/song.mp3", "originalName": "song.mp3",
  "metadata": { "title": "...", "artist": "...", "album": "...", "genre": "...",
                "year": 2024, "durationSec": 213, "bitrate": 320 } }
```

### P3 — Uploader (Consumer P2)
- **Consomme** `queue.metadata`.
- Envoie **fichier + métadonnées** à l'API : `POST /api/songs` en `multipart/form-data` (part `file` + part `metadata` JSON).
- Si réponse **2xx** → **supprime le fichier du répertoire** puis **ack** (et retire son entrée de l'index P1 via la même clé, ou laisse l'absence du fichier faire foi).
- Si échec → **nack** → retry / dead-letter queue (`queue.metadata.dlq`). **Log** : upload OK/KO, suppression.

---

## 6. RabbitMQ — topologie

- Exchange direct `naina.exchange`.
- Queues (toutes **durables**, messages persistants) :
  - `queue.new-files` (P1→P2)
  - `queue.metadata` (P2→P3)
  - `queue.metadata.dlq` (dead-letter pour échecs d'upload)
- **Ack manuel** côté consumers (pas de perte si crash). `prefetch=1` au départ (traitement séquentiel, simple à déboguer).
- UI de management sur `http://localhost:15672` (guest/guest en dev).

---

## 7. API Spring Boot

### Stockage des fichiers
Fichiers mp3 sur le **système de fichiers** de l'API (`storage/songs/<id>.mp3`), chemin enregistré en DB. (Plus simple et plus léger qu'un BLOB MySQL.)

### Endpoints
**Songs (CRUD + ingestion + lecture)**
- `POST   /api/songs` — *(P3)* multipart file + metadata → crée la chanson.
- `GET    /api/songs` — liste + filtres `?genre=&artist=&q=`.
- `GET    /api/songs/{id}` — détail.
- `PUT    /api/songs/{id}` — modifier métadonnées.
- `DELETE /api/songs/{id}` — supprimer (DB + fichier).
- `GET    /api/songs/{id}/stream` — streaming audio (`Accept-Ranges`, pour `<audio>`).

**Playlists**
- `POST /api/playlists/generate` — body critères → renvoie une sélection (non sauvegardée).
- `POST /api/playlists` — sauvegarder (name, userId, songIds[] ordonnés).
- `GET  /api/playlists?userId=` — playlists d'un utilisateur.
- `GET  /api/playlists/{id}` — détail.
- `PUT  /api/playlists/{id}` — éditer la composition (ajouter / retirer / remplacer un titre).
- `DELETE /api/playlists/{id}`.
- `GET  /api/playlists/{id}/download` — **zip** des mp3 (StreamingResponseBody).

**Auth (basique)**
- `POST /api/auth/register`, `POST /api/auth/login` → JWT. (Peut être simplifié si l'auth n'est pas notée.)

### Génération de playlist (algorithme)
Entrée : `{ genres: [...], targetDurationSec, ... }`.
1. Filtrer les chansons par critères (genre, etc.).
2. Sélectionner un sous-ensemble dont la **somme des durées** approche `targetDurationSec` (glouton avec mélange aléatoire pour la variété ; option : approche type sac-à-dos pour coller au plus près).
3. Retourner la liste **ordonnée**. Le client ajuste ensuite (add/remove/replace).

---

## 8. Schéma MySQL

```sql
users(           id PK, username UNIQUE, password_hash, created_at )
songs(           id PK, title, artist, album, genre, year,
                 duration_sec, bitrate, original_name, file_path, size_bytes, created_at )
playlists(       id PK, user_id FK→users, name, created_at )
playlist_songs(  playlist_id FK→playlists, song_id FK→songs, position,
                 PRIMARY KEY(playlist_id, position) )   -- ordre conservé
```
Migrations versionnées via **Flyway** (`V1__init.sql`, …).

---

## 9. Front Vue 3

- **Vues** : Login/Register · **Bibliothèque** (table CRUD + lecteur audio) · **Générateur** (formulaire critères → résultats) · **Mes playlists** (sauvegardées) · **Éditeur de playlist** (réordonner, ajouter/retirer/remplacer, bouton *Télécharger .zip*).
- État : **Pinia**. Routage : **Vue Router**. HTTP : **Axios** (intercepteur JWT).
- Lecture : `<audio :src="/api/songs/{id}/stream">`.

---

## 10. Configuration (externalisée — fichier `.env`)

| Variable | Programme | Défaut |
|----------|-----------|--------|
| `REPERTOIRE_DIR` | P1, P3 | `./repertoire` |
| `SCAN_INTERVAL_SEC` | P1 | `180` (3 min) |
| `RABBIT_HOST/PORT/USER/PASS` | P1/P2/P3 | `localhost:5672` |
| `API_PORT` / `API_BASE_URL` | API / P3 | `8090` / `http://localhost:8090` (8080+8081 déjà pris sur la machine) |
| `spring.datasource.*` | API | MySQL local |
| `STORAGE_DIR` | API | `./storage/songs` |
| `JWT_SECRET`, `JWT_EXPIRATION` | API | — |

> Les programmes Java lisent le `.env` au démarrage (ex. via `dotenv-java`) ; l'API mappe ces valeurs dans `application.yml`.

---

## 11. Phases d'implémentation (ordre conseillé)

| Phase | Livrable | Vérifiable par |
|-------|----------|----------------|
| **0** | Skeleton Maven + `docker-compose` (RabbitMQ+MySQL) + module `common` | `docker compose up`, UI Rabbit accessible |
| **1** | **P1** scanne `inbox/`, copie, publie dans `queue.new-files` | message visible dans l'UI Rabbit |
| **2** | **P2** consomme, extrait les tags, publie `queue.metadata` | logs métadonnées corrects |
| **3** | **API** skeleton + MySQL + `POST /api/songs` (+ Flyway) | insert via curl |
| **4** | **P3** consomme, upload à l'API, supprime l'original | chanson en DB + fichier supprimé du répertoire |
| **5** | API CRUD complet + `/stream` | liste/édition/lecture |
| **6** | Génération playlist + sauvegarde + download zip | zip téléchargeable |
| **7** | Front Vue (bibliothèque, générateur, playlists) | parcours bout-en-bout |
| **8** | Auth/users, gestion d'erreurs, DLQ, polish | retries, logs, tests |

---

## 12. Décisions confirmées

1. **Un seul répertoire** de stockage (`REPERTOIRE_DIR`). La « copie » = celle gardée par l'API. P3 supprime l'original du répertoire après upload.
2. **Auth utilisateurs : JWT** (Spring Security).
3. **Stockage fichiers : filesystem** de l'API (chemin en DB).
4. **Détection : scan périodique**, intervalle **configurable dans `.env`** (`SCAN_INTERVAL_SEC`, défaut 3 min).
5. **Java 19** + Maven (environnement : `java 19.0.2`).
```
