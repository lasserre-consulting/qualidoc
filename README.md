# QualiDoc

Plateforme de gestion documentaire qualite pour etablissements de sante. Concu pour des groupements d'etablissements (~15 utilisateurs), QualiDoc centralise les procedures, protocoles, formulaires et livrets de sensibilisation avec recherche full-text et gestion par roles.

## Fonctionnalites

- **Gestion de documents** : upload, telechargement, versioning, classement en dossiers arborescents
- **Types de documents** : procedure, protocole, formulaire, livret de sensibilisation
- **Groupements d'etablissements** : partage automatique des documents au sein d'un groupement
- **Recherche full-text** : indexation Elasticsearch du contenu des documents
- **Roles** : `EDITOR` (CRUD complet) / `READER` (lecture seule)
- **Administration** : gestion des utilisateurs (creation, modification, reset mot de passe, suppression)
- **Notifications** : envoi d'emails lors d'actions sur les documents
- **Audit** : journal d'audit RGPD/HDS de toutes les actions

## Stack technique

| Technologie | Version | Role |
|---|---|---|
| Kotlin | 2.3.20 | Langage backend |
| Spring Boot | 4.0.5 | Framework backend |
| Gradle | 9.4.1 | Build backend (Kotlin DSL) |
| PostgreSQL | 18 | Base de donnees relationnelle |
| Flyway | - | Migrations de schema |
| MinIO | - | Stockage objet (compatible S3) |
| Elasticsearch | 9.3.2 | Recherche full-text |
| Angular | 21 | Framework frontend (standalone components) |
| Angular Material | 21 | Composants UI |
| NgRx | 21 | State management |
| JWT custom (HS256) | - | Authentification (access + refresh tokens) |
| Docker / Docker Compose | - | Conteneurisation |
| Jenkins | - | CI/CD |
| Nginx | - | Reverse proxy / serveur frontend |

## Architecture du monorepo

```
qualidoc/
├── qualidoc-backend/           ← API REST Spring Boot (Kotlin, architecture hexagonale)
│   ├── src/main/kotlin/        ← Code source
│   ├── src/main/resources/     ← Config + migrations Flyway
│   ├── docker-compose.yml      ← Stack dev (PostgreSQL, MinIO, ES, MailHog)
│   └── Dockerfile              ← Image Docker backend
├── qualidoc-frontend/          ← SPA Angular 21
│   ├── src/app/                ← Code source (feature-based)
│   └── Dockerfile              ← Image Docker frontend (build + Nginx)
├── qualidoc-k8s/               ← Manifestes Kubernetes (Kustomize)
│   ├── config/                 ← ConfigMap + Secrets
│   ├── backend/                ← Deployment + HPA
│   ├── frontend/               ← Deployment
│   ├── postgres/               ← StatefulSet + PVC
│   ├── minio/                  ← StatefulSet + PVC
│   └── elasticsearch/          ← StatefulSet + PVC
├── docker-compose.prod.yml     ← Deploiement prod (Docker Compose)
├── Jenkinsfile                 ← Pipeline CI/CD
└── README.md                   ← Ce fichier
```

## Demarrage rapide (dev local)

### 1. Lancer la stack d'infrastructure

```bash
cd qualidoc-backend
docker compose up -d
```

Services disponibles :
- **PostgreSQL** : `localhost:5432` (db: qualidoc, user: qualidoc, password: qualidoc)
- **MinIO** : `http://localhost:9001` (minioadmin / minioadmin)
- **Elasticsearch** : `http://localhost:9200`
- **MailHog** : `http://localhost:8025` (visualisation des emails)

### 2. Lancer le backend

```bash
cd qualidoc-backend
./gradlew bootRun --args='--server.port=8083'
```

API disponible sur `http://localhost:8083`
Swagger UI : `http://localhost:8083/swagger-ui.html`

### 3. Lancer le frontend

```bash
cd qualidoc-frontend
npm install
npm start
```

Accessible via `http://localhost/qualidoc/` (via Nginx local).

## Comptes de test (dev)

Les migrations Flyway V2 et V8 inserent des utilisateurs avec un mot de passe commun.

| Email | Role | Mot de passe | Etablissement |
|---|---|---|---|
| `editor@chu-toulouse.fr` | EDITOR | `qualidoc-dev-2026` | CHU de Toulouse |
| `reader@chu-toulouse.fr` | READER | `qualidoc-dev-2026` | CHU de Toulouse |
| `qualite@clinique-saintjean.fr` | EDITOR | `qualidoc-dev-2026` | Clinique Saint-Jean |
| `reader@rangueil.fr` | READER | `qualidoc-dev-2026` | Hopital de Rangueil |

## Deploiement production

Le deploiement passe par un pipeline Jenkins (`Jenkinsfile`) qui execute :

1. **Checkout** : `git pull origin main`
2. **Backend - Test** : `./gradlew cleanTest test`
3. **Frontend - Build** : `npm run build -- --configuration production`
4. **Frontend - Deploy** : copie des fichiers build vers `/var/www/qualidoc/`
5. **Backend - Build & Deploy** : `docker compose -f docker-compose.prod.yml up -d --build backend`

Les secrets de production sont stockes dans `/etc/qualidoc/secrets.env` sur le serveur.

```bash
# Fichier /etc/qualidoc/secrets.env (a creer sur le serveur)
DB_PASSWORD=...
JWT_SECRET=...            # openssl rand -hex 32
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
MAIL_HOST=...
MAIL_PORT=...
```

## Documentation detaillee

| Module | README |
|---|---|
| Backend | [qualidoc-backend/README.md](qualidoc-backend/README.md) |
| Frontend | [qualidoc-frontend/README.md](qualidoc-frontend/README.md) |
| Kubernetes | [qualidoc-k8s/README.md](qualidoc-k8s/README.md) |
| Auth JWT | [qualidoc-k8s/JWT_AUTH_SETUP.md](qualidoc-k8s/JWT_AUTH_SETUP.md) |
