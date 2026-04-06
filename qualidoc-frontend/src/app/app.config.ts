import { ApplicationConfig, APP_INITIALIZER, isDevMode } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { KeycloakService } from 'keycloak-angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { documentsReducer } from './store/documents/documents.store';
import { foldersReducer, FolderEffects } from './store/folders/folders.store';
import { searchReducer, establishmentsReducer, authReducer } from './store/shared.store';
import { DocumentEffects } from './store/documents/documents.store';
import { SearchEffects, EstablishmentEffects, AuthEffects } from './store/shared.store';
import { environment } from '../environments/environment';

function initializeKeycloak(keycloak: KeycloakService) {
  return () =>
    keycloak.init({
      config: {
        url: environment.keycloak.url,
        realm: environment.keycloak.realm,
        clientId: environment.keycloak.clientId,
      },
      initOptions: {
        onLoad: 'login-required',
        checkLoginIframe: false,
      },
      enableBearerInterceptor: false, // on gère ça manuellement avec authInterceptor
    });
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),

    // NgRx Store
    provideStore({
      documents:      documentsReducer,
      folders:        foldersReducer,
      search:         searchReducer,
      establishments: establishmentsReducer,
      auth:           authReducer,
    }),
    provideEffects([DocumentEffects, FolderEffects, SearchEffects, EstablishmentEffects, AuthEffects]),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() }),

    // Keycloak
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService],
    },
  ],
};
