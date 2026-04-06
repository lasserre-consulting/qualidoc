# QualiDoc — Frontend Angular

Interface SPA Angular 21 avec Angular Material, NgRx et authentification Keycloak.

## Stack technique

| Couche | Technologie |
|---|---|
| Framework | Angular 21 (standalone components) |
| UI | Angular Material 21 |
| État | NgRx 21 (Store + Effects + DevTools) |
| Auth | Keycloak Angular + keycloak-js 25 |
| Build | Angular CLI + esbuild |
| Style | SCSS + Material theming |

## Structure

```
src/app/
├── core/
│   ├── models/models.ts          ← Interfaces TypeScript du domaine
│   ├── services/services.ts      ← Services HTTP (DocumentService, SearchService…)
│   ├── interceptors/             ← Injection JWT automatique
│   └── guards/                   ← authGuard, editorGuard
├── store/
│   ├── documents/documents.store.ts  ← Actions + Reducer + Effects + Selectors
│   └── shared.store.ts               ← Auth + Search + Establishments
├── features/
│   ├── dashboard/pages/          ← Tableau de bord (stats + documents récents)
│   ├── documents/pages/          ← Liste + Upload + Dialog partage
│   ├── search/pages/             ← Recherche full-text
│   └── admin/pages/              ← Établissements + métriques
└── shared/components/            ← ForbiddenComponent
```

## Démarrage rapide

```bash
# Installation des dépendances
npm install

# Lancement en développement (proxy vers backend :8080)
npm start
# → http://localhost:4200

# Build de production
npm run build:prod
# → dist/qualidoc-frontend/
```

## Prérequis

Le backend Spring Boot doit tourner sur `:8080` et Keycloak sur `:8180`.

```bash
# Démarrer la stack (depuis qualidoc-backend/)
docker-compose up -d

# Démarrer le backend
cd qualidoc-backend && ./gradlew bootRun

# Démarrer le frontend
cd qualidoc-frontend && npm start
```

## Configuration Keycloak

Dans Keycloak (`http://localhost:8180`) :
1. Créer le realm `qualidoc`
2. Créer un client `qualidoc-frontend` (public, redirect URI : `http://localhost:4200/*`)
3. Créer les rôles de realm : `READER`, `EDITOR`
4. Ajouter le mapper `establishment_id` dans le token JWT

## Pages disponibles

| Route | Rôle | Description |
|---|---|---|
| `/dashboard` | READER, EDITOR | Tableau de bord avec stats |
| `/documents` | READER, EDITOR | Liste et téléchargement |
| `/documents/upload` | EDITOR | Upload avec drag & drop |
| `/search` | READER, EDITOR | Recherche full-text |
| `/admin` | EDITOR | Établissements + métriques |

## NgRx DevTools

En développement, ouvre l'extension Redux DevTools dans Chrome/Firefox pour visualiser le store en temps réel et rejouer les actions.
