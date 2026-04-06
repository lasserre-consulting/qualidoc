# QualiDoc — Backend Kotlin

API REST de gestion documentaire inter-établissements pour le service qualité.

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.3.20 / JVM 25 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle 9.4.1 (Kotlin DSL) |
| Base de données | PostgreSQL 18 + Flyway |
| Stockage fichiers | MinIO (compatible S3) |
| Recherche | Elasticsearch 9.3.2 |
| Auth | Keycloak 26.5.7 (JWT OAuth2) |
| Tests | JUnit 5 + MockK + Spring Test |

## Architecture

```
src/main/kotlin/com/qualidoc/
├── domain/
│   ├── model/          ← Entités pures (zéro dépendance framework)
│   ├── repository/     ← Interfaces contrats (DIP)
│   └── port/           ← Ports sortants (stockage, search, notif)
├── application/
│   ├── usecase/        ← Logique métier orchestrée (SRP, OCP)
│   └── dto/            ← Commandes et résultats
├── infrastructure/
│   ├── persistence/    ← Implémentations JPA des repositories
│   ├── storage/        ← Adaptateur MinIO
│   ├── search/         ← Adaptateur Elasticsearch (NativeQuery)
│   ├── notification/   ← Adaptateur SMTP
│   └── security/       ← Configuration Spring Security + Keycloak
└── presentation/
    └── controller/     ← Controllers REST (SRP)
```

## Démarrage rapide

### 1. Lancer la stack locale

```bash
docker compose up -d
```

> **Note :** si tu changes de version de PostgreSQL, ajoute `-v` pour supprimer l'ancien volume de données.

Services disponibles :
- **PostgreSQL** : `localhost:5432` (db: qualidoc, user: qualidoc)
- **MinIO** : `http://localhost:9001` (minioadmin / minioadmin)
- **Elasticsearch** : `http://localhost:9200`
- **Keycloak** : `http://localhost:8180` (admin / admin)
- **MailHog** : `http://localhost:8025` (visualisation des emails de dev)

### 2. Configurer Keycloak

1. Ouvrir `http://localhost:8180`
2. Créer le realm **qualidoc**
3. Créer deux rôles de realm : `READER`, `EDITOR`
4. Créer un client `qualidoc-backend` (confidential, bearer-only)
5. Ajouter le claim `establishment_id` dans le mapper du client

### 3. Lancer le backend

```bash
./gradlew bootRun
```

API disponible sur `http://localhost:8080`  
Swagger UI : `http://localhost:8080/swagger-ui.html`

### 4. Lancer les tests

```bash
# Tests unitaires uniquement (aucun service requis)
./gradlew test

# Tests d'intégration (nécessite docker compose up -d)
./gradlew integrationTest
```

**Couverture :**
- 34 tests unitaires : use cases + controllers (MockMvc)
- 13 tests d'intégration : upload, partage, recherche, repository JPA, Elasticsearch

## Variables d'environnement

| Variable | Défaut dev | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/qualidoc` | URL PostgreSQL |
| `DB_USER` | `qualidoc` | Utilisateur BDD |
| `DB_PASSWORD` | `qualidoc` | Mot de passe BDD |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/qualidoc` | Émetteur JWT |
| `MINIO_ENDPOINT` | `http://localhost:9000` | URL MinIO |
| `MINIO_ACCESS_KEY` | `minioadmin` | Clé d'accès MinIO |
| `MINIO_SECRET_KEY` | `minioadmin` | Clé secrète MinIO |
| `ELASTICSEARCH_URI` | `http://localhost:9200` | URL Elasticsearch |
| `MAIL_HOST` | `localhost` | Hôte SMTP |

## Endpoints REST

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/api/v1/documents` | READER, EDITOR | Liste les documents accessibles |
| POST | `/api/v1/documents` | EDITOR | Upload un document |
| GET | `/api/v1/documents/{id}/download` | READER, EDITOR | Télécharge le fichier binaire |
| DELETE | `/api/v1/documents/{id}` | EDITOR | Supprime un document |
| POST | `/api/v1/documents/{id}/share` | EDITOR | Partage avec des établissements |
| GET | `/api/v1/search?q=...` | READER, EDITOR | Recherche full-text |
| GET | `/api/v1/establishments` | READER, EDITOR | Liste les établissements |
| GET | `/api/v1/health` | Public | Statut de l'API |

## Déploiement Kubernetes

Les manifests Kustomize se trouvent dans `../qualidoc-k8s/`.

```bash
# Appliquer tous les manifests
kubectl apply -k ../qualidoc-k8s/

# Vérifier l'état du déploiement
kubectl get pods -n qualidoc
```

L'ingress expose :
- `qualidoc.fr` → frontend Angular
- `api.qualidoc.fr` → backend REST
- `keycloak.qualidoc.fr` → Keycloak
