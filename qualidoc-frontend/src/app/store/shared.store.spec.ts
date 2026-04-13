import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import {
  AuthActions, AuthEffects, authReducer,
  SearchActions, searchReducer,
  EstablishmentActions, establishmentsReducer,
  AuthSelectors,
} from './shared.store';
import { AuthService } from '../core/services/auth.service';
import { DEFAULT_USER } from '../testing/test-helpers';
import { AuthState, User } from '../core/models/models';

// ══════════════════════════════════════════════════════════════════════════════
// AUTH REDUCER
// ══════════════════════════════════════════════════════════════════════════════

describe('authReducer', () => {
  const initialState: AuthState = {
    user: null, isAuthenticated: false, loading: false, error: null,
  };

  it('should return the initial state by default', () => {
    const result = authReducer(undefined, { type: 'NOOP' });
    expect(result).toEqual(initialState);
  });

  it('should set loading to true on loadProfile', () => {
    const result = authReducer(initialState, AuthActions.loadProfile());
    expect(result.loading).toBeTrue();
  });

  it('should set the user and isAuthenticated on loadProfileSuccess', () => {
    const result = authReducer(initialState, AuthActions.loadProfileSuccess({ user: DEFAULT_USER }));
    expect(result.user).toEqual(DEFAULT_USER);
    expect(result.isAuthenticated).toBeTrue();
    expect(result.loading).toBeFalse();
  });

  it('should set error on loadProfileFailure', () => {
    const result = authReducer(initialState, AuthActions.loadProfileFailure({ error: 'fail' }));
    expect(result.error).toBe('fail');
    expect(result.loading).toBeFalse();
  });

  it('should reset to initial state on logout', () => {
    const stateWithUser: AuthState = {
      user: DEFAULT_USER, isAuthenticated: true, loading: false, error: null,
    };
    const result = authReducer(stateWithUser, AuthActions.logout());
    expect(result).toEqual(initialState);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// AUTH EFFECTS
// ══════════════════════════════════════════════════════════════════════════════

describe('AuthEffects', () => {
  let effects: AuthEffects;
  let actions$: Observable<any>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['getCurrentUser', 'logout']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        provideMockStore(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });

    effects = TestBed.inject(AuthEffects);
  });

  describe('loadProfile$', () => {
    it('should dispatch loadProfileSuccess when user is available', (done) => {
      authService.getCurrentUser.and.returnValue(DEFAULT_USER);
      actions$ = of(AuthActions.loadProfile());

      effects.loadProfile$.subscribe(action => {
        expect(action).toEqual(AuthActions.loadProfileSuccess({ user: DEFAULT_USER }));
        done();
      });
    });

    it('should dispatch loadProfileFailure when no user is found', (done) => {
      authService.getCurrentUser.and.returnValue(null);
      actions$ = of(AuthActions.loadProfile());

      effects.loadProfile$.subscribe(action => {
        expect(action).toEqual(AuthActions.loadProfileFailure({ error: 'Aucun token valide' }));
        done();
      });
    });
  });

  describe('logout$', () => {
    it('should call AuthService.logout and navigate to /login', (done) => {
      authService.logout.and.returnValue(of(undefined as any));
      actions$ = of(AuthActions.logout());

      effects.logout$.subscribe({
        complete: () => {
          expect(authService.logout).toHaveBeenCalled();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
          done();
        }
      });
    });

    it('should navigate to /login even if logout HTTP call fails', (done) => {
      authService.logout.and.returnValue(throwError(() => new Error('Network error')));
      actions$ = of(AuthActions.logout());

      effects.logout$.subscribe({
        complete: () => {
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
          done();
        }
      });
    });
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// SEARCH REDUCER
// ══════════════════════════════════════════════════════════════════════════════

describe('searchReducer', () => {
  it('should set loading and query on search action', () => {
    const result = searchReducer(undefined, SearchActions.search({ query: 'test' }));
    expect(result.query).toBe('test');
    expect(result.loading).toBeTrue();
    expect(result.error).toBeNull();
  });

  it('should set results on searchSuccess', () => {
    const results = [{ documentId: 'd1', title: 'Doc', type: 'PROCEDURE', establishmentId: 'e1', snippet: null }];
    const result = searchReducer(undefined, SearchActions.searchSuccess({ results }));
    expect(result.results).toEqual(results);
    expect(result.loading).toBeFalse();
  });

  it('should set error on searchFailure', () => {
    const result = searchReducer(undefined, SearchActions.searchFailure({ error: 'fail' }));
    expect(result.error).toBe('fail');
    expect(result.loading).toBeFalse();
  });

  it('should reset on clearSearch', () => {
    const populated = searchReducer(undefined, SearchActions.search({ query: 'test' }));
    const result = searchReducer(populated, SearchActions.clearSearch());
    expect(result.query).toBe('');
    expect(result.results).toEqual([]);
    expect(result.loading).toBeFalse();
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// ESTABLISHMENTS REDUCER
// ══════════════════════════════════════════════════════════════════════════════

describe('establishmentsReducer', () => {
  it('should set loading on load', () => {
    const result = establishmentsReducer(undefined, EstablishmentActions.load());
    expect(result.loading).toBeTrue();
  });

  it('should set items on loadSuccess', () => {
    const estabs = [{ id: 'e1', name: 'Clinique A', code: 'CLA', active: true }];
    const result = establishmentsReducer(undefined, EstablishmentActions.loadSuccess({ establishments: estabs }));
    expect(result.items).toEqual(estabs);
    expect(result.loading).toBeFalse();
  });

  it('should set error on loadFailure', () => {
    const result = establishmentsReducer(undefined, EstablishmentActions.loadFailure({ error: 'fail' }));
    expect(result.error).toBe('fail');
    expect(result.loading).toBeFalse();
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// AUTH SELECTORS
// ══════════════════════════════════════════════════════════════════════════════

describe('AuthSelectors', () => {
  it('isEditor should return true when user role is EDITOR', () => {
    const state = { auth: { user: DEFAULT_USER, isAuthenticated: true, loading: false, error: null } };
    expect(AuthSelectors.isEditor.projector(state.auth)).toBeTrue();
  });

  it('isEditor should return false when user role is READER', () => {
    const readerUser: User = { ...DEFAULT_USER, role: 'READER' };
    const state = { auth: { user: readerUser, isAuthenticated: true, loading: false, error: null } };
    expect(AuthSelectors.isEditor.projector(state.auth)).toBeFalse();
  });

  it('isEditor should return false when user is null', () => {
    const state = { auth: { user: null, isAuthenticated: false, loading: false, error: null } };
    expect(AuthSelectors.isEditor.projector(state.auth)).toBeFalsy();
  });
});
