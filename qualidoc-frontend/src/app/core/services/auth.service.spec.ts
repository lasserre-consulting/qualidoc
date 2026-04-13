import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';
import { AuthResponse } from '../models/models';
import {
  EDITOR_JWT, READER_JWT, EXPIRED_JWT,
  DEFAULT_USER, buildAuthResponse,
} from '../../testing/test-helpers';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // ── login ─────────────────────────────────────────────────────────────────

  describe('login', () => {
    it('should POST to /auth/login with email and password', () => {
      const mockResponse = buildAuthResponse();

      service.login('test@example.com', 'password123').subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'test@example.com', password: 'password123' });
      req.flush(mockResponse);
    });

    it('should store tokens via TokenStorageService on success', () => {
      spyOn(tokenStorage, 'storeTokens').and.callThrough();
      const mockResponse = buildAuthResponse();

      service.login('test@example.com', 'password123').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockResponse);

      expect(tokenStorage.storeTokens).toHaveBeenCalledOnceWith(mockResponse);
    });

    it('should propagate a 401 error', () => {
      let errorReceived: any;

      service.login('test@example.com', 'wrong').subscribe({
        error: err => { errorReceived = err; }
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      expect(errorReceived).toBeTruthy();
      expect(errorReceived.status).toBe(401);
    });
  });

  // ── refresh ───────────────────────────────────────────────────────────────

  describe('refresh', () => {
    it('should POST to /auth/refresh with the current refresh token', () => {
      // Set up a refresh token in storage
      tokenStorage.storeTokens(buildAuthResponse());
      const newResponse = buildAuthResponse();

      service.refresh().subscribe(response => {
        expect(response).toEqual(newResponse);
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'refresh-token-abc' });
      req.flush(newResponse);
    });

    it('should store new tokens on successful refresh', () => {
      tokenStorage.storeTokens(buildAuthResponse());
      spyOn(tokenStorage, 'storeTokens').and.callThrough();

      const newResponse: AuthResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        user: DEFAULT_USER,
      };

      service.refresh().subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      req.flush(newResponse);

      expect(tokenStorage.storeTokens).toHaveBeenCalledOnceWith(newResponse);
    });
  });

  // ── logout ────────────────────────────────────────────────────────────────

  describe('logout', () => {
    it('should POST to /auth/logout with the refresh token', () => {
      tokenStorage.storeTokens(buildAuthResponse());

      service.logout().subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'refresh-token-abc' });
      req.flush(null);
    });

    it('should clear tokens from storage immediately', () => {
      tokenStorage.storeTokens(buildAuthResponse());
      spyOn(tokenStorage, 'clearTokens').and.callThrough();

      service.logout().subscribe();

      // clearTokens is called synchronously before the HTTP request completes
      expect(tokenStorage.clearTokens).toHaveBeenCalled();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
      req.flush(null);
    });
  });

  // ── isLoggedIn ────────────────────────────────────────────────────────────

  describe('isLoggedIn', () => {
    it('should return true when a valid (non-expired) token is present', () => {
      tokenStorage.storeTokens(buildAuthResponse(EDITOR_JWT));

      expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false when no token is present', () => {
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should return false when the token is expired', () => {
      tokenStorage.storeTokens(buildAuthResponse(EXPIRED_JWT));

      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  // ── getCurrentUser ────────────────────────────────────────────────────────

  describe('getCurrentUser', () => {
    it('should decode a valid JWT and return a User object', () => {
      tokenStorage.storeTokens(buildAuthResponse(EDITOR_JWT));

      const user = service.getCurrentUser();

      expect(user).toBeTruthy();
      expect(user!.id).toBe('user-001');
      expect(user!.email).toBe('test@example.com');
      expect(user!.firstName).toBe('Jean');
      expect(user!.lastName).toBe('Dupont');
      expect(user!.fullName).toBe('Jean Dupont');
      expect(user!.role).toBe('EDITOR');
      expect(user!.establishmentId).toBe('estab-001');
    });

    it('should return null when no token is present', () => {
      expect(service.getCurrentUser()).toBeNull();
    });

    it('should return null when the token has an invalid payload', () => {
      // Store a malformed token
      localStorage.setItem('qd_access_token', 'not.a.valid.jwt');

      expect(service.getCurrentUser()).toBeNull();
    });
  });

  // ── hasRole ───────────────────────────────────────────────────────────────

  describe('hasRole', () => {
    it('should return true when the user has the matching role', () => {
      tokenStorage.storeTokens(buildAuthResponse(EDITOR_JWT));

      expect(service.hasRole('EDITOR')).toBeTrue();
    });

    it('should return false when the user has a different role', () => {
      tokenStorage.storeTokens(buildAuthResponse(READER_JWT));

      expect(service.hasRole('EDITOR')).toBeFalse();
    });

    it('should return false when no token is present', () => {
      expect(service.hasRole('EDITOR')).toBeFalse();
    });
  });

  // ── clearTokens ───────────────────────────────────────────────────────────

  describe('clearTokens', () => {
    it('should delegate to TokenStorageService.clearTokens', () => {
      spyOn(tokenStorage, 'clearTokens');

      service.clearTokens();

      expect(tokenStorage.clearTokens).toHaveBeenCalled();
    });
  });
});
