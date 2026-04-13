import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { EDITOR_JWT, buildAuthResponse } from '../../testing/test-helpers';
import { of, Subject, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;
  let tokenStorage: jasmine.SpyObj<TokenStorageService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', [
      'getAccessToken', 'refresh',
    ]);
    tokenStorage = jasmine.createSpyObj('TokenStorageService', [
      'clearTokens',
    ]);
    router = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: TokenStorageService, useValue: tokenStorage },
        { provide: Router, useValue: router },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);

    // Reset the module-level isRefreshing flag between tests
    // by providing a default behavior
    authService.getAccessToken.and.returnValue(null);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ── Token attachment ──────────────────────────────────────────────────────

  it('should add Authorization header when a token is present', () => {
    authService.getAccessToken.and.returnValue(EDITOR_JWT);

    http.get(`${environment.apiUrl}/documents`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/documents`);
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${EDITOR_JWT}`);
    req.flush([]);
  });

  it('should NOT add Authorization header when no token is present', () => {
    authService.getAccessToken.and.returnValue(null);

    http.get(`${environment.apiUrl}/documents`).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/documents`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  // ── Auth endpoints bypass ─────────────────────────────────────────────────

  it('should NOT add Authorization header for /auth/login requests', () => {
    authService.getAccessToken.and.returnValue(EDITOR_JWT);

    http.post(`${environment.apiUrl}/auth/login`, {}).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('should NOT add Authorization header for /auth/refresh requests', () => {
    authService.getAccessToken.and.returnValue(EDITOR_JWT);

    http.post(`${environment.apiUrl}/auth/refresh`, {}).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  // ── 401 handling with refresh ─────────────────────────────────────────────

  it('should attempt a token refresh on 401 and replay the request', () => {
    authService.getAccessToken.and.returnValue('old-token');
    const newResponse = buildAuthResponse('new-access-token');
    authService.refresh.and.returnValue(of(newResponse));

    let responseBody: any;
    http.get(`${environment.apiUrl}/documents`).subscribe(body => {
      responseBody = body;
    });

    // First request fails with 401
    const firstReq = httpMock.expectOne(`${environment.apiUrl}/documents`);
    firstReq.flush({ message: 'Expired' }, { status: 401, statusText: 'Unauthorized' });

    // After refresh, the interceptor replays the request with the new token
    const replayedReq = httpMock.expectOne(`${environment.apiUrl}/documents`);
    expect(replayedReq.request.headers.get('Authorization')).toBe('Bearer new-access-token');
    replayedReq.flush([{ id: 'doc-1' }]);

    expect(responseBody).toEqual([{ id: 'doc-1' }]);
  });

  it('should navigate to /login when refresh fails', () => {
    authService.getAccessToken.and.returnValue('old-token');
    authService.refresh.and.returnValue(throwError(() => new Error('Refresh failed')));

    http.get(`${environment.apiUrl}/documents`).subscribe({
      error: () => { /* expected */ }
    });

    const firstReq = httpMock.expectOne(`${environment.apiUrl}/documents`);
    firstReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(tokenStorage.clearTokens).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should not modify non-401 errors', () => {
    authService.getAccessToken.and.returnValue(EDITOR_JWT);
    let errorReceived: any;

    http.get(`${environment.apiUrl}/documents`).subscribe({
      error: err => { errorReceived = err; }
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/documents`);
    req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });

    expect(errorReceived).toBeTruthy();
    expect(errorReceived.status).toBe(500);
    expect(authService.refresh).not.toHaveBeenCalled();
  });

  // ── Concurrent 401 handling (no double-refresh) ───────────────────────────

  it('should not trigger a second refresh when multiple requests get 401 simultaneously', () => {
    authService.getAccessToken.and.returnValue('old-token');

    // Utiliser un Subject pour contrôler le timing : le refresh ne résout pas
    // immédiatement, ce qui permet aux deux 401 d'arriver avant que isRefreshing
    // repasse à false.
    const refreshSubject = new Subject<ReturnType<typeof buildAuthResponse>>();
    authService.refresh.and.returnValue(refreshSubject.asObservable());

    // Lancer deux requêtes concurrentes
    http.get(`${environment.apiUrl}/documents`).subscribe();
    http.get(`${environment.apiUrl}/folders`).subscribe();

    // Les deux répondent 401
    const reqs = httpMock.match(() => true);
    expect(reqs.length).toBe(2);
    reqs[0].flush({}, { status: 401, statusText: 'Unauthorized' });
    reqs[1].flush({}, { status: 401, statusText: 'Unauthorized' });

    // isRefreshing est toujours true — refresh n'est appelé qu'une seule fois
    expect(authService.refresh).toHaveBeenCalledTimes(1);

    // Résoudre le refresh
    refreshSubject.next(buildAuthResponse('refreshed-token'));
    refreshSubject.complete();

    // Les deux requêtes rejouées utilisent le nouveau token
    const replayed = httpMock.match(() => true);
    for (const r of replayed) {
      expect(r.request.headers.get('Authorization')).toBe('Bearer refreshed-token');
      r.flush([]);
    }
  });
});
