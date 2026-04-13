import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/models';

// -- Guard authentification (toutes les routes protegees) ---------------------

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

// -- Guard parametrable par role ---------------------------------------------

export function roleGuard(requiredRole: UserRole): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    if (authService.hasRole(requiredRole)) return true;
    return router.createUrlTree(['/forbidden']);
  };
}

// -- Guard editeur (raccourci retro-compatible) ------------------------------

export const editorGuard: CanActivateFn = roleGuard('EDITOR');
