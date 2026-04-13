import { ApplicationConfig, isDevMode } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { documentsReducer } from './store/documents/documents.store';
import { foldersReducer, FolderEffects } from './store/folders/folders.store';
import { searchReducer, establishmentsReducer, authReducer } from './store/shared.store';
import { DocumentEffects } from './store/documents/documents.store';
import { SearchEffects, EstablishmentEffects, AuthEffects } from './store/shared.store';

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
  ],
};
