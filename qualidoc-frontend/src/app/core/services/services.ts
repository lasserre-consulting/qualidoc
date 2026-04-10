import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Document, Establishment, Folder, SearchResult, DashboardStats
} from '../models/models';

// ── Documents ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/documents`;

  list(folderId?: string | null, all?: boolean): Observable<Document[]> {
    let params = new HttpParams();
    if (all) params = params.set('all', 'true');
    else if (folderId) params = params.set('folderId', folderId);
    return this.http.get<Document[]>(this.base, { params });
  }

  upload(file: File, title: string, type: string, folderId?: string | null): Observable<Document> {
    const form = new FormData();
    form.append('file', file);
    form.append('title', title);
    form.append('type', type);
    if (folderId) form.append('folderId', folderId);
    return this.http.post<Document>(this.base, form);
  }

  download(documentId: string): Observable<Blob> {
    return this.http.get(`${this.base}/${documentId}/download`, {
      responseType: 'blob'
    });
  }

  move(documentId: string, folderId: string | null): Observable<Document> {
    return this.http.patch<Document>(`${this.base}/${documentId}/move`, { folderId });
  }

  rename(documentId: string, title: string): Observable<Document> {
    return this.http.patch<Document>(`${this.base}/${documentId}/rename`, { title });
  }

  delete(documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${documentId}`);
  }
}

// ── Dossiers ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class FolderService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/folders`;

  list(): Observable<Folder[]> {
    return this.http.get<Folder[]>(this.base);
  }

  create(name: string, parentId?: string | null): Observable<Folder> {
    return this.http.post<Folder>(this.base, { name, parentId: parentId ?? null });
  }

  rename(folderId: string, name: string): Observable<Folder> {
    return this.http.patch<Folder>(`${this.base}/${folderId}/rename`, { name });
  }

  delete(folderId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${folderId}`);
  }
}

// ── Établissements ────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class EstablishmentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/establishments`;

  list(): Observable<Establishment[]> {
    return this.http.get<Establishment[]>(this.base);
  }
}

// ── Recherche ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class SearchService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/search`;

  search(query: string): Observable<SearchResult[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<SearchResult[]>(this.base, { params });
  }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${environment.apiUrl}/dashboard/stats`);
  }
}
