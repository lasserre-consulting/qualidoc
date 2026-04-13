# QualiDoc — Backend Kotlin

API REST de gestion documentaire inter-etablissements pour le service qualite.

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.3.20 / JVM 25 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle 9.4.1 (Kotlin DSL) |
| Base de donnees | PostgreSQL 18 + Flyway |
| Stockage fichiers | MinIO (compatible S3) |
| Recherche | Elasticsearch 9.3.2 |
| Auth | JWT custom (HS256, access + refresh tokens) |
| Mots de passe | BCrypt (strength 12) |
| Tests | JUnit 5 + MockK + Spring Test |

## Architecture hexagonale

```
src/main/kotlin/com/qualidoc/
├── domain/
│   ├── model/          ← Entites pures (zero dependance framework)
│   ├── repository/     ← Interfaces contrats (DIP)
│   └── port/           ← Ports sortants (stockage, search, notif, JWT)
├── application/
│   ├── usecase/        ← Logique metier orchestree (SRP, OCP)
│   └── dto/            ← Commandes et resultats
├── infrastructure/
│   ├── persistence/    ← Implementations JPA des repositories
│   ├── storage/        ← Adaptateur MinIO
│   ├── search/         ← Adaptateur Elasticsearch (NativeQuery)
│   ├── notification/   ← Adaptateur SMTP
│   └── security/       ← JWT custom + Spring Security (filtre Bearer)
└── presentation/
    └── controller/     ← Controllers REST (SRP)
```

## Authentification JWT custom

L'authentification repose sur un systeme JWT custom (pas de Keycloak ni d'OAuth2 externe).

### Fonctionnement

1. L'utilisateur s'authentifie via `POST /api/v1/auth/login` avec email + mot de passe
2. Le backend verifie le mot de passe (BCrypt strength 12) et retourne un couple access token + refresh token
3. L'access token (HS256, 15 min) contient : `sub` (user ID), `role`, `establishment_id`
4. Le refresh token (opaque UUID, 7 jours) est stocke en base (hash SHA-256)
5. A expiration, le frontend appelle `POST /api/v1/auth/refresh` pour obtenir un nouveau couple
6. Les refresh tokens sont a rotation : chaque refresh invalide l'ancien token et en genere un nouveau

### Securite

- **Algorithme** : HMAC-SHA256 (cle secrete partagee via `JWT_SECRET`)
- **Access token** : 15 minutes (configurable via `JWT_ACCESS_EXPIRATION`)
- **Refresh token** : 7 jours (configurable via `JWT_REFRESH_EXPIRATION`)
- **Rotation** : chaque appel a `/auth/refresh` revoque l'ancien refresh token
- **Nettoyage** : tache CRON a 3h du matin supprime les refresh tokens expires
- **Mots de passe** : BCrypt strength 12

### Filtre Spring Security

Le `JwtAuthenticationFilter` intercepte chaque requete, extrait le Bearer token du header `Authorization`, le valide via `JwtService.parseAccessToken()`, et place un `AuthenticatedUser(id, role, establishmentId)` dans le `SecurityContext`.

## API REST

### Authentification

| Methode | Route | Role | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Authentification email/mot de passe |
| POST | `/api/v1/auth/refresh` | Public | Renouvellement access + refresh token |
| POST | `/api/v1/auth/logout` | Public | Revocation du refresh token |
| GET | `/api/v1/auth/me` | Authentifie | Informations de l'utilisateur connecte |

### Documents

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/documents` | READER, EDITOR | Liste les documents accessibles |
| POST | `/api/v1/documents` | EDITOR | Upload un document (multipart/form-data) |
| GET | `/api/v1/documents/{id}/download` | READER, EDITOR | Telecharge le fichier binaire |
| PATCH | `/api/v1/documents/{id}/move` | EDITOR | Deplace un document dans un dossier |
| PATCH | `/api/v1/documents/{id}/rename` | EDITOR | Renomme un document |
| DELETE | `/api/v1/documents/{id}` | EDITOR | Supprime un document |

### Dossiers

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/folders` | READER, EDITOR | Liste les dossiers du groupement |
| POST | `/api/v1/folders` | EDITOR | Cree un dossier |
| PATCH | `/api/v1/folders/{id}/rename` | EDITOR | Renomme un dossier |
| DELETE | `/api/v1/folders/{id}` | EDITOR | Supprime un dossier |

### Recherche

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/search?q=...` | READER, EDITOR | Recherche full-text (Elasticsearch) |

### Etablissements

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/establishments` | READER, EDITOR | Liste les etablissements |

### Administration

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/admin/users` | EDITOR | Liste tous les utilisateurs |
| POST | `/api/v1/admin/users` | EDITOR | Cree un nouvel utilisateur |
| PATCH | `/api/v1/admin/users/{id}` | EDITOR | Modifie un utilisateur |
| POST | `/api/v1/admin/users/{id}/reset-password` | EDITOR | Reinitialise le mot de passe |
| DELETE | `/api/v1/admin/users/{id}` | EDITOR | Supprime un utilisateur |

### Sante

| Methode | Route | Role | Description |
|---|---|---|---|
| GET | `/api/v1/health` | Public | Statut de l'API |

## Variables d'environnement

| Variable | Defaut dev | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/qualidoc` | URL PostgreSQL |
| `DB_USER` | `qualidoc` | Utilisateur BDD |
| `DB_PASSWORD` | `qualidoc` | Mot de passe BDD |
| `JWT_SECRET` | *(obligatoire)* | Cle secrete HS256 (`openssl rand -hex 32`) |
| `JWT_ACCESS_EXPIRATION` | `900` | Duree de vie access token (secondes) |
| `JWT_REFRESH_EXPIRATION` | `604800` | Duree de vie refresh token (secondes) |
| `MINIO_ENDPOINT` | `http://localhost:9000` | URL MinIO |
| `MINIO_ACCESS_KEY` | `minioadmin` | Cle d'acces MinIO |
| `MINIO_SECRET_KEY` | `minioadmin` | Cle secrete MinIO |
| `MINIO_BUCKET` | `qualidoc-documents` | Nom du bucket |
| `ELASTICSEARCH_URI` | `http://localhost:9200` | URL Elasticsearch |
| `MAIL_HOST` | `localhost` | Hote SMTP |
| `MAIL_PORT` | `25` | Port SMTP |

## Migrations Flyway

| Migration | Description |
|---|---|
| `V1__initial_schema.sql` | Schema initial : establishment, app_user, document, document_share, audit_log, notification + index |
| `V2__seed_dev_data.sql` | Donnees de dev : 5 etablissements + 4 utilisateurs de test |
| `V3__remove_document_share.sql` | Suppression de la table document_share (cloisonnement par groupement) |
| `V4__add_protocol_type.sql` | Ajout du type PROTOCOL (separation PROCEDURE/PROTOCOL) |
| `V5__add_groupement.sql` | Ajout de groupement_id sur establishment (2 groupements de test) |
| `V6__add_folder.sql` | Arborescence de dossiers par groupement (table folder + FK sur document) |
| `V7__auth_custom.sql` | Auth JWT custom : colonne password_hash sur app_user + table refresh_token |
| `V8__seed_dev_passwords.sql` | Mots de passe dev BCrypt (mot de passe commun : `qualidoc-dev-2026`) |

## Demarrage en dev

### 1. Lancer la stack d'infrastructure

```bash
docker compose up -d
```

Services disponibles :
- **PostgreSQL** : `localhost:5432` (db: qualidoc, user/password: qualidoc)
- **MinIO** : `http://localhost:9001` (minioadmin / minioadmin)
- **Elasticsearch** : `http://localhost:9200`
- **MailHog** : `http://localhost:8025` (visualisation des emails)

### 2. Lancer le backend

```bash
./gradlew bootRun --args='--server.port=8083'
```

API disponible sur `http://localhost:8083`
Swagger UI : `http://localhost:8083/swagger-ui.html`

## Tests

```bash
# Tests unitaires uniquement (aucun service requis)
./gradlew test

# Tests d'integration (necessite docker compose up -d)
./gradlew integrationTest
```

**Couverture :**
- 34 tests unitaires : use cases + controllers (MockMvc)
- 13 tests d'integration : upload, partage, recherche, repository JPA, Elasticsearch

## Build Docker

```bash
docker build -t qualidoc-backend:latest .
```
