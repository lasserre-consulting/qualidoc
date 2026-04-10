import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';

// ── Guard authentification (toutes les routes protégées) ──────────────────────

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const isLoggedIn = await keycloak.isLoggedIn();
  if (!isLoggedIn) {
    await keycloak.login({ redirectUri: window.location.href });
    return false;
  }
  return true;
};

// ── Guard éditeur (upload, partage) ──────────────────────────────────────────

export const editorGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const isLoggedIn = await keycloak.isLoggedIn();
  if (!isLoggedIn) {
    await keycloak.login({ redirectUri: window.location.href });
    return false;
  }

  const hasRole = keycloak.isUserInRole('EDITOR');
  if (!hasRole) {
    router.navigate(['/forbidden']);
    return false;
  }
  return true;
};
