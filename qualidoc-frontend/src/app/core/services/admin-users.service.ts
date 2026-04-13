import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, CreateUserRequest, UpdateUserRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/admin/users`;

  listUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.base);
  }

  createUser(req: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.base, req);
  }

  updateUser(id: string, req: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>(`${this.base}/${id}`, req);
  }

  resetPassword(id: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/reset-password`, { newPassword });
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
