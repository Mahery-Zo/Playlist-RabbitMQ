# 🎵 Naina Playlist

> Système de gestion de fichiers MP3 et de génération de playlists intelligentes.

Pipeline d'ingestion asynchrone en **Java** communiquant via **RabbitMQ**, **API REST Spring Boot**, base de données **MySQL**, et interface web **Vue 3**.

---

## 📐 Architecture

```
                 ┌──────────────────────── BACKEND DESKTOP (Java standalone) ────────────────────────┐
                 │                                                                                    │
   repertoire/ ──▶  P1 Watcher ──(queue.new-files)──▶ P2 Metadata ──(queue.metadata)──▶ P3 Uploader  │
 (dépôt mp3)       │ scan                               │ extraction tags               │ POST + del │
                 │                                                                       │            │
                 └───────────────────────────────────────────────────────────────────────┼────────────┘
                                                                                         │ HTTP (multipart)
                                                                                         ▼
                                                                            ┌─────────────────────────┐
                                         Vue 3 (Vite)  ◀── REST / stream ──│   API Spring Boot       │
                                         - CRUD                             │   + stockage fichiers   │
                                         - génération playlist              └───────────┬─────────────┘
                                         - lecture / download zip                       │ JPA
                                                                                        ▼
                                                                                      MySQL
```

**Principe** : les 3 programmes du pipeline ne s'appellent jamais directement. Chacun se connecte à RabbitMQ, consomme la queue qui le concerne et produit vers la suivante. Si un programme tombe, les messages restent dans la queue → reprise sans perte.

---

## 🛠️ Stack technique

| Couche | Choix | Librairies clés |
|---|---|---|
| Pipeline (P1 / P2 / P3) | Java 19 standalone (Maven multi-module) | `amqp-client`, `jaudiotagger`, `jackson`, `dotenv-java` |
| Logs | SLF4J + Logback | Fichier rotatif par programme |
| Message broker | RabbitMQ | Exchange direct + DLQ |
| API | Spring Boot 3 (Java 19) | Spring Web, Spring Data JPA, Spring Security (JWT) |
| Base de données | MySQL 8 | Flyway (migrations) |
| Frontend | Vue 3 + Vite | Pinia, Vue Router, Axios |
| Infra dev | Docker Compose | RabbitMQ + MySQL |

---

## ✅ Prérequis

- **Java 19** (`java -version` → `19.x`)
- **Maven** (`mvn -version`)
- **Docker Desktop** (pour RabbitMQ et MySQL)
- **Node.js ≥ 18** et **npm** (pour le frontend)

---

## 🚀 Installation & lancement

### 1. Cloner le projet et configurer l'environnement

```bash
git clone <url-du-repo>
cd Naina_Playlist

# Créer le fichier de configuration
cp .env.example .env
# Adapter les valeurs dans .env si nécessaire
```

### 2. Démarrer l'infrastructure (RabbitMQ + MySQL)

```bash
docker compose up -d
```

Vérifier : UI RabbitMQ sur [http://localhost:15672](http://localhost:15672) (guest / guest).

### 3. Compiler le backend

```bash
mvn install -DskipTests
```

### 4. Lancer les programmes — un terminal par programme

Ouvrir **4 terminaux** (ou double-cliquer les scripts `.bat` dans `scripts/`) :

| Ordre | Script | Programme |
|---|---|---|
| 1 | `scripts/3-api.bat` | API Spring Boot (port 8090) — attendre « Started ApiApplication » |
| 2 | `scripts/6-p3-uploader.bat` | P3 Uploader (consumer) |
| 3 | `scripts/5-p2-metadata.bat` | P2 Metadata (consumer) |
| 4 | `scripts/4-p1-watcher.bat` | P1 Watcher (producer) |

> **Astuce** : lancer les consumers (P3, P2) avant le producer (P1) est recommandé, mais l'ordre n'est pas critique grâce à la persistance des messages RabbitMQ.

### 5. Lancer le frontend

```bash
cd front
npm install
npm run dev
```

Le frontend est accessible sur [http://localhost:5173](http://localhost:5173).

---

## 🎧 Utilisation

### Ingestion automatique de MP3

1. **Déposer** un ou plusieurs fichiers `.mp3` dans le dossier `repertoire/`.
2. **P1 (Watcher)** détecte les nouveaux fichiers (scan toutes les 3 min, configurable) et publie un message dans `queue.new-files`.
3. **P2 (Metadata)** extrait les tags ID3 (titre, artiste, album, genre, année, durée, bitrate) et publie dans `queue.metadata`.
4. **P3 (Uploader)** envoie le fichier + métadonnées à l'API, puis **supprime l'original** du répertoire.
5. Le fichier est désormais stocké par l'API dans `storage/songs/` et enregistré en base MySQL.

> Pour un test immédiat sans attendre le scan, relancer P1 ou utiliser le script `scripts/gen-demo-mp3.bat` pour générer un MP3 de démo.

### Interface web

- **Bibliothèque** : visualiser, rechercher, éditer et supprimer des chansons (CRUD complet).
- **Générateur de playlists** : créer des playlists par critères (genres, artistes, durée cible) avec inclusion/exclusion.
- **Mes playlists** : sauvegarder, consulter et gérer ses playlists.
- **Éditeur de playlist** : réordonner, ajouter, supprimer ou remplacer des titres.
- **Lecture audio** : écouter directement dans le navigateur via streaming.
- **Téléchargement** : exporter une playlist complète en archive `.zip`.
- **Fusion de playlists** : fusionner plusieurs playlists (sans doublons).

---

## 📁 Structure du projet

```
Naina_Playlist/
├── docker-compose.yml          # RabbitMQ (+ UI :15672) + MySQL
├── pom.xml                     # POM parent Maven (multi-module)
├── .env.example                # Template de configuration
├── blacklist.json              # Artistes/genres à exclure du pipeline
├── max_duration.txt            # Durée max (sec) d'un MP3 accepté par P3
│
├── common/                     # Module partagé : DTOs, noms de queues, sérialisation
│   └── src/main/java/
│
├── watcher/                    # P1 — Scan du répertoire + producer RabbitMQ
│   └── src/main/java/
│
├── metadata/                   # P2 — Extraction tags ID3 (consumer P1 → producer P3)
│   └── src/main/java/
│
├── uploader/                   # P3 — Upload API + suppression original (consumer P2)
│   └── src/main/java/
│
├── api/                        # API Spring Boot (REST + stockage fichiers + MySQL)
│   └── src/
│       ├── main/java/          # Controllers, services, entities, security (JWT)
│       └── main/resources/
│           └── db/migration/   # Scripts Flyway (V1__init.sql, …)
│
├── front/                      # Frontend Vue 3 (Vite)
│   └── src/
│       ├── views/              # LoginView, LibraryView, GeneratorView, PlaylistsView, PlaylistEditView
│       ├── components/         # AppHeader, AudioPlayer, CriteriaForm, SongTable, PlaylistCard, …
│       ├── stores/             # Pinia stores
│       ├── api/                # Client Axios
│       └── router/             # Vue Router
│
├── scripts/                    # Scripts .bat de lancement (Windows)
├── logs/                       # Logs rotatifs (watcher.log, metadata.log, uploader.log)
├── repertoire/                 # Dossier de dépôt des MP3 (surveillé par P1)
└── storage/                    # Stockage persistant des fichiers par l'API
```

---

## 🔌 Endpoints API

### Songs (CRUD + ingestion + streaming)

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/songs` | Créer une chanson (multipart : fichier + metadata JSON) — utilisé par P3 |
| `GET` | `/api/songs` | Lister les chansons (filtres : `?genre=`, `?artist=`, `?q=`) |
| `GET` | `/api/songs/{id}` | Détail d'une chanson |
| `PUT` | `/api/songs/{id}` | Modifier les métadonnées |
| `DELETE` | `/api/songs/{id}` | Supprimer (DB + fichier) |
| `GET` | `/api/songs/{id}/stream` | Streaming audio (`Accept-Ranges`, pour `<audio>`) |

### Playlists

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/playlists/generate` | Générer une playlist selon critères (non sauvegardée) |
| `POST` | `/api/playlists` | Sauvegarder une playlist |
| `GET` | `/api/playlists?userId=` | Lister les playlists d'un utilisateur |
| `GET` | `/api/playlists/{id}` | Détail d'une playlist |
| `PUT` | `/api/playlists/{id}` | Éditer la composition |
| `DELETE` | `/api/playlists/{id}` | Supprimer |
| `GET` | `/api/playlists/{id}/download` | Télécharger en ZIP (streaming) |

### Authentification

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Inscription |
| `POST` | `/api/auth/login` | Connexion → JWT |

---

## 🧠 Génération de playlist (algorithme)

1. **Filtrer** les chansons par critères : genres (inclus/exclus), artistes (inclus/exclus).
2. **Sélectionner** un sous-ensemble dont la somme des durées approche la `targetDurationSec` (algorithme glouton avec mélange aléatoire pour la variété).
3. **Retourner** la liste ordonnée. Le client peut ensuite ajuster (ajouter / retirer / remplacer des titres).

---

## 🗄️ Schéma MySQL

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

## 🐇 RabbitMQ — Topologie

- **Exchange** : `naina.exchange` (direct)
- **Queues** (durables, messages persistants) :
  - `queue.new-files` — P1 → P2
  - `queue.metadata` — P2 → P3
  - `queue.metadata.dlq` — dead-letter pour échecs d'upload
- **Ack manuel** côté consumers (pas de perte si crash), `prefetch=1`.
- UI de management : [http://localhost:15672](http://localhost:15672) (guest / guest en dev).

---

## ⚙️ Configuration

Toute la configuration est externalisée dans le fichier `.env` à la racine du projet.

| Variable | Programme(s) | Défaut |
|---|---|---|
| `REPERTOIRE_DIR` | P1, P3 | `./repertoire` |
| `SCAN_INTERVAL_SEC` | P1 | `180` (3 min) |
| `RABBIT_HOST` / `RABBIT_PORT` | P1, P2, P3 | `localhost` / `5672` |
| `RABBIT_USER` / `RABBIT_PASS` | P1, P2, P3 | `guest` / `guest` |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` | API | `localhost` / `3306` / `naina` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | API | `naina` / `naina` |
| `API_PORT` | API | `8090` |
| `API_BASE_URL` | P3 | `http://localhost:8090` |
| `STORAGE_DIR` | API | `./storage/songs` |
| `JWT_SECRET` | API | *(à définir)* |
| `JWT_EXPIRATION` | API | `86400000` (24h) |

---

## 📜 Scripts utilitaires (`scripts/`)

| Script | Description |
|---|---|
| `1-infra.bat` | Démarre RabbitMQ + MySQL (Docker Compose) |
| `2-build.bat` | Compile le projet Maven |
| `3-api.bat` | Lance l'API Spring Boot |
| `4-p1-watcher.bat` | Lance P1 (Watcher) |
| `5-p2-metadata.bat` | Lance P2 (Metadata) |
| `6-p3-uploader.bat` | Lance P3 (Uploader) |
| `gen-demo-mp3.bat` | Génère un MP3 de démo taggé dans `repertoire/` |
| `check.bat` | Vérifie le résultat (`curl` sur l'API) |

---

## 🧪 Vérification rapide

```bash
# Après avoir lancé tout le pipeline et déposé un MP3 :
curl http://localhost:8090/api/songs
```

Résultat attendu :
- La chanson apparaît avec ses métadonnées dans la réponse JSON.
- Le fichier a **disparu** de `repertoire/` (supprimé par P3).
- Un fichier est apparu dans `storage/songs/` (copie conservée par l'API).
- Les queues RabbitMQ sont à 0 (aucun message en attente, `queue.metadata.dlq` à 0 = aucun échec).

---

## 🛑 Tout arrêter

1. Fermer les terminaux des programmes Java (ou `Ctrl+C`).
2. Arrêter l'infrastructure Docker :
   ```bash
   docker compose down
   ```
   > Les données MySQL sont conservées dans le volume Docker `naina-mysql-data`.

---

## 🌐 Déploiement en Production

Pour déployer ce projet dans le cadre de votre portfolio (sur un **VPS** avec Docker ou via des plateformes **PaaS gratuites** comme Vercel, Render, Aiven et CloudAMQP), consultez le [Guide de Déploiement](file:///c:/Users/ttcho/OneDrive/ITU/S6/Naina_Playlist/DEPLOYMENT.md).

