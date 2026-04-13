import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// ── Guard authentification (toutes les routes protégées) ──────────────────────

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

// ── Guard éditeur (upload, partage, admin) ───────────────────────────────────

export const editorGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isEditor()) return true;
  return router.createUrlTree(['/forbidden']);
};
