import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly ACCESS_TOKEN_KEY = 'qd_access_token';
  private readonly REFRESH_TOKEN_KEY = 'qd_refresh_token';

  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/auth`;

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, { email, password }).pipe(
      tap(response => this.storeTokens(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<AuthResponse>(`${this.base}/refresh`, { refreshToken }).pipe(
      tap(response => this.storeTokens(response))
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    this.clearTokens();
    return this.http.post<void>(`${this.base}/logout`, { refreshToken });
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    return !this.isTokenExpired(token);
  }

  getCurrentUser(): User | null {
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = this.decodeToken(token);
      return {
        id: payload.sub ?? '',
        email: payload.email ?? '',
        firstName: payload.firstName ?? '',
        lastName: payload.lastName ?? '',
        fullName: `${payload.firstName ?? ''} ${payload.lastName ?? ''}`.trim(),
        role: payload.role ?? 'READER',
        establishmentId: payload.establishmentId ?? '',
      };
    } catch {
      return null;
    }
  }

  isEditor(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'EDITOR';
  }

  clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  private decodeToken(token: string): any {
    const payload = token.split('.')[1];
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decoded);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.decodeToken(token);
      if (!payload.exp) return false;
      const expiryMs = payload.exp * 1000;
      return Date.now() >= expiryMs;
    } catch {
      return true;
    }
  }

  private storeTokens(response: AuthResponse): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
  }
}
