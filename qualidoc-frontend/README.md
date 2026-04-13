# QualiDoc — Frontend Angular

Interface SPA Angular 21 avec Angular Material, NgRx et authentification JWT custom.

## Stack technique

| Couche | Technologie |
|---|---|
| Framework | Angular 21 (standalone components) |
| UI | Angular Material 21 |
| Etat | NgRx 21 (Store + Effects + DevTools) |
| Auth | JWT custom (AuthService + TokenStorageService + AuthInterceptor) |
| Build | Angular CLI + esbuild |
| Style | SCSS + Material theming |

## Architecture feature-based

```
src/app/
├── core/
│   ├── models/models.ts             ← Interfaces TypeScript du domaine
│   ├── services/
│   │   ├── auth.service.ts          ← Login, refresh, logout, decodage JWT
│   │   ├── token-storage.service.ts ← Stockage access/refresh tokens (localStorage)
│   │   └── ...                      ← DocumentService, SearchService, etc.
│   ├── interceptors/
│   │   └── auth.interceptor.ts      ← Injection Bearer + refresh automatique sur 401
│   └── guards/                      ← authGuard, editorGuard
├── store/
│   ├── documents/documents.store.ts ← Actions + Reducer + Effects + Selectors
│   └── shared.store.ts              ← Auth + Search + Establishments
├── features/
│   ├── auth/                        ← Page de login
│   ├── dashboard/pages/             ← Tableau de bord (stats + documents recents)
│   ├── documents/pages/             ← Liste + Upload + Dialog partage
│   ├── search/pages/                ← Recherche full-text
│   └── admin/pages/                 ← Gestion utilisateurs + metriques
└── shared/components/               ← ForbiddenComponent
```

## Flux d'authentification

```
┌──────────┐    POST /auth/login     ┌──────────┐
│  Login   │ ──────────────────────→ │ Backend  │
│  Page    │ ←────────────────────── │  API     │
└──────────┘  { accessToken,         └──────────┘
              refreshToken }
      │
      ▼
┌──────────────────┐
│ TokenStorageService │  → localStorage
└──────────────────┘
      │
      ▼
┌──────────────────┐     Authorization: Bearer <token>
│ AuthInterceptor  │ ──────────────────────────────────→ API
└──────────────────┘
      │
      │ Si 401 recu :
      ▼
┌──────────────────┐    POST /auth/refresh
│ AuthService      │ ──────────────────────────────────→ API
│ .refresh()       │ ←──────────────────────────────────
└──────────────────┘  Nouveau couple access/refresh
      │
      │ Si refresh echoue :
      ▼
  Redirection → /login
```

### Detail du flux

1. L'utilisateur saisit email + mot de passe sur la page `/login`
2. `AuthService.login()` appelle `POST /auth/login` et stocke les tokens via `TokenStorageService`
3. `AuthInterceptor` injecte automatiquement le header `Authorization: Bearer <accessToken>` sur chaque requete (sauf `/auth/login` et `/auth/refresh`)
4. Sur une reponse 401, l'intercepteur tente un refresh automatique via `POST /auth/refresh`
5. Pendant le refresh, les autres requetes en cours sont mises en file d'attente et reprises une fois le nouveau token obtenu
6. Si le refresh echoue, les tokens sont effaces et l'utilisateur est redirige vers `/login`

## Roles

| Role | Permissions |
|---|---|
| **EDITOR** | Lire, telecharger, uploader, deplacer, renommer, supprimer des documents. Creer/renommer/supprimer des dossiers. Gerer les utilisateurs (admin). |
| **READER** | Lire et telecharger des documents. Recherche full-text. Consultation du tableau de bord. |

Les guards Angular (`authGuard`, `editorGuard`) protegent les routes cote frontend. Le backend applique egalement les restrictions par role.

## State management NgRx

Le store NgRx est organise en deux slices :

### `documents` (documents.store.ts)
- **Actions** : chargement, upload, deplacement, renommage, suppression de documents
- **Reducer** : etat de la liste (documents, loading, error)
- **Effects** : appels HTTP vers l'API backend
- **Selectors** : acces reactif aux documents, etat de chargement

### `shared` (shared.store.ts)
- Auth : utilisateur connecte, etat d'authentification
- Search : resultats de recherche, query
- Establishments : liste des etablissements

En developpement, l'extension **Redux DevTools** permet de visualiser le store et rejouer les actions.

## Configuration

### environment.ts (dev)

```typescript
export const environment = {
  production: false,
  apiUrl: '/qualidoc/api/v1',
};
```

### environment.prod.ts (production)

```typescript
export const environment = {
  production: true,
  apiUrl: '/qualidoc/api/v1',
};
```

L'`apiUrl` utilise le prefixe `/qualidoc/` car l'application est deployee sous ce sous-chemin via Nginx.

## Pages disponibles

| Route | Role | Description |
|---|---|---|
| `/login` | Public | Page de connexion |
| `/dashboard` | READER, EDITOR | Tableau de bord avec stats |
| `/documents` | READER, EDITOR | Liste et telechargement |
| `/documents/upload` | EDITOR | Upload avec drag & drop |
| `/search` | READER, EDITOR | Recherche full-text |
| `/admin` | EDITOR | Gestion utilisateurs + metriques |

## Commandes de developpement

### Prerequis

Le backend Spring Boot doit tourner (port 8083) et la stack Docker (PostgreSQL, MinIO, ES) doit etre lancee.

### Installation et lancement

```bash
# Installation des dependances
npm install

# Lancement en developpement
npm start
```

Accessible via `http://localhost/qualidoc/` (via Nginx local qui proxifie vers le serveur de dev Angular).

### Tests

```bash
npm test
```

### Build de production

```bash
npm run build -- --configuration production
```

Les fichiers compiles sont generes dans `dist/qualidoc-frontend/browser/`.

## NgRx DevTools

En developpement, ouvrir l'extension Redux DevTools dans Chrome/Firefox pour visualiser le store en temps reel et rejouer les actions.
