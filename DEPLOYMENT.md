# 🌐 Guide de Déploiement en Production — Naina Playlist

Ce document décrit comment déployer l'application **Naina Playlist** pour votre portfolio. Vous pouvez choisir entre un déploiement payant sur **VPS** (recommandé pour la stabilité) ou un déploiement 100% **gratuit** (PaaS).

---

## 🏗️ Option A : Déploiement Docker sur un VPS (Recommandé)

Cette option utilise un serveur VPS (ex. OVH, DigitalOcean, Hetzner, Scaleway) sous Linux avec Docker. L'ensemble des services tourne dans un réseau Docker sécurisé, et un serveur Caddy ou Nginx gère le trafic HTTPS et sert le frontend.

### 1. Préparer le fichier `docker-compose.prod.yml` sur le VPS

Créez un fichier `docker-compose.prod.yml` à la racine de votre projet sur le VPS :

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8
    container_name: naina-prod-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: naina
      MYSQL_USER: naina
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - naina-prod-mysql-data:/var/lib/mysql

  rabbitmq:
    image: rabbitmq:3-management
    container_name: naina-prod-rabbitmq
    restart: always
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBIT_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBIT_PASS}

  api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: naina-prod-api
    restart: always
    depends_on:
      - mysql
      - rabbitmq
    environment:
      API_PORT: 8090
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: naina
      MYSQL_USER: naina
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      RABBIT_HOST: rabbitmq
      RABBIT_PORT: 5672
      RABBIT_USER: ${RABBIT_USER}
      RABBIT_PASS: ${RABBIT_PASS}
      STORAGE_DIR: /app/storage/songs
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: 86400000
      CORS_ALLOWED_ORIGINS: https://votre-portfolio.com,http://localhost:5173
    volumes:
      - naina-prod-storage:/app/storage/songs

  # Serveur Caddy pour gérer le HTTPS automatique et servir le front
  web:
    image: caddy:2-alpine
    container_name: naina-prod-web
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./front/dist:/usr/share/caddy
      - caddy_data:/data
      - caddy_config:/config
    entrypoint: [ "caddy", "file-server", "--root", "/usr/share/caddy", "--listen", ":80", "--try-files", "index.html" ]

volumes:
  naina-prod-mysql-data:
  naina-prod-storage:
  caddy_data:
  caddy_config:
```

### 2. Étapes de déploiement sur le VPS

1. **Cloner** votre dépôt Git sur le VPS.
2. **Compiler** le frontend en local ou sur le VPS :
   ```bash
   cd front
   npm install
   npm run build
   ```
3. **Créer le fichier `.env`** de production sur le VPS avec des mots de passe sécurisés :
   ```env
   MYSQL_PASSWORD=un_mot_de_passe_secret_db
   MYSQL_ROOT_PASSWORD=un_autre_mot_de_passe_secret
   RABBIT_USER=admin_naina
   RABBIT_PASS=un_mot_de_passe_secret_rabbit
   JWT_SECRET=mettez_une_cle_tres_longue_et_aleatoire_ici
   ```
4. **Lancer les conteneurs** :
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```

---

## ☁️ Option B : Déploiement Multi-Plateformes 100% Gratuit

Si vous préférez ne pas payer d'hébergement, vous pouvez combiner différents services gratuits.

```
┌─────────────────┐             ┌────────────────────┐
│   Vercel        │             │   Render           │
│   (Frontend)    │ ───HTTPS──▶ │   (API Spring)     │
└─────────────────┘             └─────────┬──────────┘
                                          │
                    ┌─────────────────────┴─────────────────────┐
                    ▼                                           ▼
          ┌───────────────────┐                       ┌───────────────────┐
          │   Aiven Cloud     │                       │   CloudAMQP       │
          │   (Base MySQL)    │                       │   (RabbitMQ)      │
          └───────────────────┘                       └───────────────────┘
```

### 1. Base de données MySQL (Aiven)
1. Créez un compte gratuit sur [Aiven.io](https://aiven.io/).
2. Créez un service **MySQL** (sélectionnez le plan gratuit "Free tier").
3. Notez l'URI de connexion, l'hôte, le port, le nom d'utilisateur et le mot de passe.

### 2. Message Broker RabbitMQ (CloudAMQP)
1. Créez un compte gratuit sur [CloudAMQP](https://www.cloudamqp.com/).
2. Créez une nouvelle instance avec le plan gratuit **Little Lemur**.
3. Récupérez l'URI RabbitMQ et les identifiants depuis la page de détails (ex: `amqps://user:pass@host/vhost`).

### 3. API REST Spring Boot (Render)
1. Créez un compte sur [Render.com](https://render.com/).
2. Cliquez sur **New +** > **Web Service**.
3. Connectez votre dépôt Git.
4. Remplissez les configurations :
   - **Name** : `naina-playlist-api`
   - **Environment** : `Docker`
   - **Docker Command** : (laisser vide, il utilisera le `Dockerfile` à la racine)
   - **Plan** : `Free`
5. Ajoutez les variables d'environnement suivantes dans l'onglet **Environment** de Render :
   - `API_PORT` = `8090` (ou laissez par défaut)
   - `MYSQL_HOST` = *(Hôte de votre base Aiven)*
   - `MYSQL_PORT` = *(Port de votre base Aiven, ex: 12345)*
   - `MYSQL_DATABASE` = *(Nom de la base Aiven)*
   - `MYSQL_USER` = *(Utilisateur Aiven)*
   - `MYSQL_PASSWORD` = *(Mot de passe Aiven)*
   - `RABBIT_HOST` = *(Hôte CloudAMQP)*
   - `RABBIT_PORT` = `5672` (ou utilisez l'URI complète si nécessaire)
   - `RABBIT_USER` = *(Utilisateur CloudAMQP)*
   - `RABBIT_PASS` = *(Mot de passe CloudAMQP)*
   - `JWT_SECRET` = *(Une clé secrète longue et aléatoire)*
   - `CORS_ALLOWED_ORIGINS` = `https://votre-frontend.vercel.app`
6. Déployez le service.

> [!WARNING]
> Sur le plan gratuit de Render, l'instance s'endort après 15 minutes d'inactivité. L'accès initial au site peut prendre jusqu'à 2 minutes (le temps que le serveur sorte de veille).

### 4. Frontend Vue 3 (Vercel)
1. Créez un compte sur [Vercel](https://vercel.com/).
2. Cliquez sur **Add New** > **Project** et connectez votre dépôt Git.
3. Configurez le projet :
   - **Framework Preset** : `Vite`
   - **Root Directory** : `front`
4. Ajoutez la variable d'environnement de build :
   - `VITE_API_BASE_URL` = `https://votre-api-render.onrender.com/api` (l'URL de votre API Render suivie de `/api`).
5. Dans `front/vercel.json`, modifiez la destination du rewrite vers votre API Render :
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://votre-api-render.onrender.com/api/:path*"
       },
       {
         "source": "/(.*)",
         "destination": "/index.html"
       }
     ]
   }
   ```
6. Cliquez sur **Deploy**.

---

## 🎧 Exécuter le pipeline d'ingestion depuis votre machine

Puisque le pipeline d'ingestion (P1 Watcher, P2 Metadata, P3 Uploader) scanne un répertoire local pour y extraire les tags et uploader les MP3, vous pouvez le faire tourner en local tout en le connectant à votre infrastructure déployée.

Pour cela, mettez simplement à jour le fichier `.env` sur votre machine locale :

```env
# Mettre les identifiants RabbitMQ en ligne (ex: CloudAMQP ou votre VPS)
RABBIT_HOST=votre-rabbitmq-en-ligne.com
RABBIT_PORT=5672
RABBIT_USER=votre_utilisateur
RABBIT_PASS=votre_mot_de_passe

# Mettre l'URL de votre API en production
API_BASE_URL=https://naina-playlist-api.onrender.com

# Configuration locale
REPERTOIRE_DIR=./repertoire
SCAN_INTERVAL_SEC=180
```

Lancez vos scripts locaux (`scripts/4-p1-watcher.bat`, `5-p2-metadata.bat`, `6-p3-uploader.bat`). Les fichiers MP3 déposés dans votre dossier local `repertoire/` seront extraits et envoyés directement dans votre base de données et votre stockage cloud de production !
