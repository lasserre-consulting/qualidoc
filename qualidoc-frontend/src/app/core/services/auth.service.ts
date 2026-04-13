import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User, UserRole } from '../models/models';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);
  private base = `${environment.apiUrl}/auth`;

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, { email, password }).pipe(
      tap(response => this.tokenStorage.storeTokens(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    return this.http.post<AuthResponse>(`${this.base}/refresh`, { refreshToken }).pipe(
      tap(response => this.tokenStorage.storeTokens(response))
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    this.tokenStorage.clearTokens();
    return this.http.post<void>(`${this.base}/logout`, { refreshToken });
  }

  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  isLoggedIn(): boolean {
    const token = this.tokenStorage.getAccessToken();
    if (!token) return false;
    return !this.isTokenExpired(token);
  }

  getCurrentUser(): User | null {
    const token = this.tokenStorage.getAccessToken();
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

  hasRole(role: UserRole): boolean {
    const user = this.getCurrentUser();
    return user?.role === role;
  }

  clearTokens(): void {
    this.tokenStorage.clearTokens();
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
}
