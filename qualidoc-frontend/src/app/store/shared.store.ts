import { createAction, createReducer, createSelector, props, on } from '@ngrx/store';
import { inject } from '@angular/core';
import { Actions, ofType, createEffect } from '@ngrx/effects';
import { switchMap, map, catchError, of, debounceTime, distinctUntilChanged, filter } from 'rxjs';
import {
  SearchState, EstablishmentsState, AuthState,
  SearchResult, Establishment, User
} from '../core/models/models';
import { SearchService, EstablishmentService } from '../core/services/services';
import { KeycloakService } from 'keycloak-angular';

// ══════════════════════════════════════════════════════════════════════════════
// SEARCH
// ══════════════════════════════════════════════════════════════════════════════

export const SearchActions = {
  search:        createAction('[Search] Query',   props<{ query: string }>()),
  searchSuccess: createAction('[Search] Success', props<{ results: SearchResult[] }>()),
  searchFailure: createAction('[Search] Failure', props<{ error: string }>()),
  clearSearch:   createAction('[Search] Clear'),
};

const searchInitial: SearchState = { query: '', results: [], loading: false, error: null };

export const searchReducer = createReducer(
  searchInitial,
  on(SearchActions.search,        (s, { query }) => ({ ...s, query, loading: true, error: null })),
  on(SearchActions.searchSuccess, (s, { results }) => ({ ...s, results, loading: false })),
  on(SearchActions.searchFailure, (s, { error }) => ({ ...s, loading: false, error })),
  on(SearchActions.clearSearch,   () => searchInitial),
);

const selectSearchState = (state: any) => state.search as SearchState;

export const SearchSelectors = {
  query:   createSelector(selectSearchState, s => s.query),
  results: createSelector(selectSearchState, s => s.results),
  loading: createSelector(selectSearchState, s => s.loading),
};

export class SearchEffects {
  private actions$ = inject(Actions);
  private service  = inject(SearchService);

  search$ = createEffect(() =>
    this.actions$.pipe(
      ofType(SearchActions.search),
      filter(({ query }) => query.length >= 2),
      debounceTime(300),
      distinctUntilChanged((a, b) => a.query === b.query),
      switchMap(({ query }) =>
        this.service.search(query).pipe(
          map(results => SearchActions.searchSuccess({ results })),
          catchError(err => of(SearchActions.searchFailure({ error: err.message })))
        )
      )
    )
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ESTABLISHMENTS
// ══════════════════════════════════════════════════════════════════════════════

export const EstablishmentActions = {
  load:        createAction('[Establishments] Load'),
  loadSuccess: createAction('[Establishments] Load Success', props<{ establishments: Establishment[] }>()),
  loadFailure: createAction('[Establishments] Load Failure', props<{ error: string }>()),
};

const estabInitial: EstablishmentsState = { items: [], loading: false, error: null };

export const establishmentsReducer = createReducer(
  estabInitial,
  on(EstablishmentActions.load,        s => ({ ...s, loading: true })),
  on(EstablishmentActions.loadSuccess, (s, { establishments }) => ({ ...s, items: establishments, loading: false })),
  on(EstablishmentActions.loadFailure, (s, { error }) => ({ ...s, loading: false, error })),
);

const selectEstabState = (state: any) => state.establishments as EstablishmentsState;

export const EstablishmentSelectors = {
  all:     createSelector(selectEstabState, s => s.items),
  loading: createSelector(selectEstabState, s => s.loading),
};

export class EstablishmentEffects {
  private actions$ = inject(Actions);
  private service  = inject(EstablishmentService);

  load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EstablishmentActions.load),
      switchMap(() =>
        this.service.list().pipe(
          map(establishments => EstablishmentActions.loadSuccess({ establishments })),
          catchError(err => of(EstablishmentActions.loadFailure({ error: err.message })))
        )
      )
    )
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// AUTH
// ══════════════════════════════════════════════════════════════════════════════

export const AuthActions = {
  loadProfile:        createAction('[Auth] Load Profile'),
  loadProfileSuccess: createAction('[Auth] Load Profile Success', props<{ user: User }>()),
  loadProfileFailure: createAction('[Auth] Load Profile Failure', props<{ error: string }>()),
  logout:             createAction('[Auth] Logout'),
};

const authInitial: AuthState = { user: null, isAuthenticated: false, loading: false, error: null };

export const authReducer = createReducer(
  authInitial,
  on(AuthActions.loadProfile,        s => ({ ...s, loading: true })),
  on(AuthActions.loadProfileSuccess, (s, { user }) => ({ ...s, user, isAuthenticated: true, loading: false })),
  on(AuthActions.loadProfileFailure, (s, { error }) => ({ ...s, loading: false, error })),
  on(AuthActions.logout,             () => authInitial),
);

const selectAuthState = (state: any) => state.auth as AuthState;

export const AuthSelectors = {
  user:            createSelector(selectAuthState, s => s.user),
  isAuthenticated: createSelector(selectAuthState, s => s.isAuthenticated),
  isEditor:        createSelector(selectAuthState, s => s.user?.role === 'EDITOR'),
};

export class AuthEffects {
  private actions$ = inject(Actions);
  private keycloak = inject(KeycloakService);

  loadProfile$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.loadProfile),
      switchMap(async () => {
        try {
          const token = this.keycloak.getKeycloakInstance().tokenParsed as any;
          const roles = this.keycloak.getUserRoles();
          const user: User = {
            id: token?.sub || '',
            email: token?.email || '',
            fullName: `${token?.given_name || ''} ${token?.family_name || ''}`.trim(),
            role: roles.includes('EDITOR') ? 'EDITOR' : 'READER',
            establishmentId: token?.establishment_id || ''
          };
          return AuthActions.loadProfileSuccess({ user });
        } catch (err: any) {
          return AuthActions.loadProfileFailure({ error: err.message });
        }
      })
    )
  );

  logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.logout),
      switchMap(async () => {
        await this.keycloak.logout(window.location.origin);
        return { type: '[Auth] Logout Complete' };
      })
    )
  );
}
