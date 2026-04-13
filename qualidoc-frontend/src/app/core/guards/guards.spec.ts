import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { authGuard, roleGuard, editorGuard } from './guards';
import { AuthService } from '../services/auth.service';

describe('Guards', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'hasRole']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
      ],
    });

    router = TestBed.inject(Router);
  });

  // ── authGuard ─────────────────────────────────────────────────────────────

  describe('authGuard', () => {
    it('should return true when the user is logged in', () => {
      authService.isLoggedIn.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() =>
        authGuard({} as any, {} as any)
      );

      expect(result).toBeTrue();
    });

    it('should return a UrlTree to /login when the user is not logged in', () => {
      authService.isLoggedIn.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() =>
        authGuard({} as any, {} as any)
      );

      expect(result).toBeInstanceOf(UrlTree);
      expect((result as UrlTree).toString()).toBe('/login');
    });
  });

  // ── roleGuard ─────────────────────────────────────────────────────────────

  describe('roleGuard', () => {
    it('should return true when the user has the required role', () => {
      authService.hasRole.and.returnValue(true);

      const guard = roleGuard('EDITOR');
      const result = TestBed.runInInjectionContext(() =>
        guard({} as any, {} as any)
      );

      expect(result).toBeTrue();
      expect(authService.hasRole).toHaveBeenCalledWith('EDITOR');
    });

    it('should return a UrlTree to /forbidden when the user does not have the required role', () => {
      authService.hasRole.and.returnValue(false);

      const guard = roleGuard('EDITOR');
      const result = TestBed.runInInjectionContext(() =>
        guard({} as any, {} as any)
      );

      expect(result).toBeInstanceOf(UrlTree);
      expect((result as UrlTree).toString()).toBe('/forbidden');
    });

    it('should work with READER role', () => {
      authService.hasRole.and.returnValue(true);

      const guard = roleGuard('READER');
      const result = TestBed.runInInjectionContext(() =>
        guard({} as any, {} as any)
      );

      expect(result).toBeTrue();
      expect(authService.hasRole).toHaveBeenCalledWith('READER');
    });
  });

  // ── editorGuard ───────────────────────────────────────────────────────────

  describe('editorGuard', () => {
    it('should return true when the user has the EDITOR role', () => {
      authService.hasRole.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() =>
        editorGuard({} as any, {} as any)
      );

      expect(result).toBeTrue();
      expect(authService.hasRole).toHaveBeenCalledWith('EDITOR');
    });

    it('should redirect to /forbidden when the user is not an editor', () => {
      authService.hasRole.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() =>
        editorGuard({} as any, {} as any)
      );

      expect(result).toBeInstanceOf(UrlTree);
      expect((result as UrlTree).toString()).toBe('/forbidden');
    });
  });
});
