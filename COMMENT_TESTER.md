# Comment tester le backend (Phases 0 → 4)

Le backend complet : tu déposes un `.mp3` dans `repertoire/`, et il se retrouve
automatiquement en base via la chaîne **P1 → P2 → P3 → API → MySQL**.

Des scripts Windows sont dans `scripts/` (double-clic ou exécution en terminal).

---

## Prérequis (une seule fois / au début de session)

1. **Lancer Docker Desktop** (sinon l'infra ne démarre pas).
2. Avoir un fichier `.env` à la racine (déjà présent ; sinon copier `.env.example`).

---

## Étape par étape

### 1. Démarrer l'infra (RabbitMQ + MySQL)
Double-clic sur **`scripts/1-infra.bat`**
ou en terminal à la racine :
```
docker compose up -d
```
Vérifier : UI RabbitMQ sur http://localhost:15672 (guest / guest).

### 2. Compiler le projet
**`scripts/2-build.bat`** ou :
```
mvn install -DskipTests
```

### 3. Démarrer les programmes — **un terminal par programme**
Ouvre 4 fenêtres (ou double-clique les 4 .bat dans cet ordre) :

| Ordre | Script | Programme |
|-------|--------|-----------|
| 1 | `scripts/3-api.bat` | API (port 8090) — attendre « Started ApiApplication » |
| 2 | `scripts/6-p3-uploader.bat` | P3 Uploader |
| 3 | `scripts/5-p2-metadata.bat` | P2 Metadata |
| 4 | `scripts/4-p1-watcher.bat` | P1 Watcher |

> Les consumers (P3, P2) avant le producer (P1), mais l'ordre n'est pas critique :
> RabbitMQ garde les messages en attente.

### 4. Déposer un mp3 à traiter
- Soit copier un vrai `.mp3` dans le dossier **`repertoire/`**,
- soit générer un fichier de démo taggé : **`scripts/gen-demo-mp3.bat`**.

P1 scanne toutes les **3 min** (réglable via `SCAN_INTERVAL_SEC` dans `.env`).
Pour un test immédiat, **relance P1** après avoir déposé le fichier (il scanne au démarrage).

### 5. Vérifier le résultat
**`scripts/check.bat`** ou manuellement :

```
curl http://localhost:8090/api/songs
```
Tu dois voir la chanson avec ses métadonnées. Et :
- le fichier a **disparu de `repertoire/`** (supprimé par P3) ;
- un fichier est apparu dans **`storage/songs/`** (copie gardée par l'API) ;
- les queues RabbitMQ sont à 0 (`queue.metadata.dlq` à 0 = aucun échec).

---

## Ce que chaque phase vérifie

| Phase | Quoi observer |
|-------|---------------|
| **0** | `docker compose ps` montre `naina-rabbitmq` et `naina-mysql` « Up » |
| **1 (P1)** | Log P1 « Nouveau mp3 publié » ; dans l'UI RabbitMQ, `queue.new-files` reçoit un message |
| **2 (P2)** | Log P2 « Métadonnées extraites : … titre/artiste/genre/durée » ; `queue.metadata` reçoit un message |
| **3 (API)** | `GET /api/songs` répond ; un `POST` crée une ligne en base + un fichier dans `storage/songs/` |
| **4 (P3)** | Log P3 « Uploadé puis supprimé du répertoire (HTTP 201) » ; `repertoire/` se vide |

---

## Logs
Chaque programme écrit aussi dans `logs/` : `watcher.log`, `metadata.log`, `uploader.log`.

## Tout arrêter
- Fermer les fenêtres des programmes (ou Ctrl+C).
- Infra : `docker compose down` (les données MySQL sont conservées dans le volume).
