# QualiDoc — Configuration de l'authentification JWT

## Vue d'ensemble

QualiDoc utilise une authentification JWT custom (pas de Keycloak ni d'IdP externe). Le backend genere et valide les tokens directement.

```
┌──────────┐   email/password   ┌──────────┐   verifie BCrypt   ┌────────────┐
│  Client   │ ────────────────→ │ Backend  │ ────────────────→  │ PostgreSQL │
│ (Angular) │ ←──────────────── │ (Spring) │ ←────────────────  │ (app_user) │
└──────────┘  accessToken +     └──────────┘                    └────────────┘
              refreshToken
```

### Tokens

| Token | Type | Algorithme | Duree de vie | Stockage |
|---|---|---|---|---|
| Access token | JWT signe | HS256 | 15 minutes | localStorage (frontend) |
| Refresh token | UUID opaque | - | 7 jours | Table `refresh_token` (hash SHA-256) |

### Claims de l'access token

| Claim | Description |
|---|---|
| `sub` | UUID de l'utilisateur |
| `role` | `EDITOR` ou `READER` |
| `establishment_id` | UUID de l'etablissement |
| `iat` | Timestamp d'emission |
| `exp` | Timestamp d'expiration |

## Generation du JWT_SECRET

Le secret doit faire au minimum 256 bits (32 octets) pour HS256.

```bash
openssl rand -hex 32
```

Exemple de sortie : `a1b2c3d4e5f6...` (64 caracteres hexadecimaux = 256 bits).

**Ne jamais reutiliser un secret entre environnements.** Chaque environnement (dev, staging, prod) doit avoir son propre secret.

## Configuration du secret dans Kubernetes

### secrets.yaml

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: qualidoc-secrets
  namespace: qualidoc
type: Opaque
stringData:
  JWT_SECRET: "REMPLACER_PAR_LA_SORTIE_DE_OPENSSL_RAND_HEX_32"
  DB_USER: "qualidoc"
  DB_PASSWORD: "MOT_DE_PASSE_POSTGRES"
  MINIO_ACCESS_KEY: "CLE_ACCES_MINIO"
  MINIO_SECRET_KEY: "CLE_SECRETE_MINIO"
  MAIL_USER: "notifications@qualidoc.fr"
  MAIL_PASSWORD: "MOT_DE_PASSE_SMTP"
  POSTGRES_PASSWORD: "MOT_DE_PASSE_POSTGRES"
```

> Ne jamais committer `secrets.yaml` avec de vraies valeurs. Utiliser Sealed Secrets, Vault ou External Secrets Operator.

### Variables d'environnement du backend

Le deployment backend doit inclure :

```yaml
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: qualidoc-secrets
        key: JWT_SECRET
  - name: JWT_ACCESS_EXPIRATION
    value: "900"          # 15 minutes (secondes)
  - name: JWT_REFRESH_EXPIRATION
    value: "604800"       # 7 jours (secondes)
```

## Endpoints d'authentification

### POST /api/v1/auth/login

Authentification par email et mot de passe.

```bash
curl -X POST https://api.qualidoc.fr/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "editor@chu-toulouse.fr", "password": "mon-mot-de-passe"}'
```

Reponse :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### POST /api/v1/auth/refresh

Renouvellement du couple access/refresh token. Le refresh token envoye est revoque et un nouveau est genere (rotation).

```bash
curl -X POST https://api.qualidoc.fr/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
```

Reponse : meme format que `/auth/login`.

### POST /api/v1/auth/logout

Revocation du refresh token.

```bash
curl -X POST https://api.qualidoc.fr/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "550e8400-e29b-41d4-a716-446655440000"}'
```

### GET /api/v1/auth/me

Informations de l'utilisateur connecte (necessite un access token valide).

```bash
curl https://api.qualidoc.fr/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Reponse :
```json
{
  "id": "22222222-0000-0000-0000-000000000001",
  "email": "editor@chu-toulouse.fr",
  "firstName": "Marie",
  "lastName": "Dupont",
  "role": "EDITOR",
  "establishmentId": "11111111-0000-0000-0000-000000000001",
  "establishmentName": "CHU de Toulouse"
}
```

## Gestion des utilisateurs

Les utilisateurs sont geres directement via les endpoints admin du backend (pas de console Keycloak).

| Action | Endpoint | Methode |
|---|---|---|
| Lister les utilisateurs | `/api/v1/admin/users` | GET |
| Creer un utilisateur | `/api/v1/admin/users` | POST |
| Modifier un utilisateur | `/api/v1/admin/users/{id}` | PATCH |
| Reinitialiser un mot de passe | `/api/v1/admin/users/{id}/reset-password` | POST |
| Supprimer un utilisateur | `/api/v1/admin/users/{id}` | DELETE |

Ces endpoints sont reserves aux utilisateurs avec le role `EDITOR`.

### Exemple : creer un utilisateur

```bash
curl -X POST https://api.qualidoc.fr/api/v1/admin/users \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nouveau@chu-toulouse.fr",
    "firstName": "Nouveau",
    "lastName": "Utilisateur",
    "password": "mot-de-passe-initial",
    "role": "READER",
    "establishmentId": "11111111-0000-0000-0000-000000000001"
  }'
```

### Exemple : reinitialiser un mot de passe

```bash
curl -X POST https://api.qualidoc.fr/api/v1/admin/users/{id}/reset-password \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"newPassword": "nouveau-mot-de-passe"}'
```

## Durees de vie des tokens

| Token | Duree | Variable d'environnement | Defaut |
|---|---|---|---|
| Access token | 15 minutes | `JWT_ACCESS_EXPIRATION` | `900` (secondes) |
| Refresh token | 7 jours | `JWT_REFRESH_EXPIRATION` | `604800` (secondes) |

Pour ajuster en production, modifier les variables d'environnement du deployment backend.

## Securite

### Rotation des refresh tokens

Chaque appel a `/auth/refresh` :
1. Verifie que le refresh token existe en base et n'est pas revoque ni expire
2. Revoque l'ancien refresh token
3. Genere un nouveau couple access token + refresh token
4. Retourne les nouveaux tokens

Cela empeche la reutilisation d'un refresh token vole : si l'attaquant l'utilise, le token legitime est deja revoque, et inversement.

### Nettoyage automatique des tokens expires

Une tache planifiee (`@Scheduled`) s'execute tous les jours a **3h du matin** et supprime de la table `refresh_token` tous les tokens expires ou revoques.

```kotlin
@Scheduled(cron = "0 0 3 * * *")
fun deleteExpiredTokens() {
    refreshTokenRepository.deleteExpired()
}
```

### Recommandations de production

- **Generer un secret fort** : `openssl rand -hex 32` (256 bits minimum)
- **Ne pas reutiliser le secret** entre environnements
- **Stocker le secret** dans Sealed Secrets, Vault ou un gestionnaire de secrets cloud
- **HTTPS obligatoire** : les tokens transitent dans les headers HTTP
- **Rotation du secret** : en cas de compromission, changer `JWT_SECRET` et redemarrer le backend (tous les access tokens existants seront invalides)
