import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { from, of, switchMap, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  // Ne pas intercepter les requêtes Keycloak elles-mêmes
  if (req.url.includes('/realms/')) {
    return next(req);
  }

  if (!keycloak.isLoggedIn()) {
    return next(req);
  }

  // updateToken s'assure que le token est prêt avant la requête.
  // En cas d'échec du refresh, on tente quand même avec le token existant.
  return from(keycloak.updateToken(30)).pipe(
    catchError(() => of(false)),
    switchMap(() => from(keycloak.getToken())),
    switchMap(token => {
      const authReq = token
        ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : req;
      return next(authReq);
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        keycloak.login();
      } else if (error.status === 403) {
        router.navigate(['/forbidden']);
      }
      return throwError(() => error);
    })
  );
};
