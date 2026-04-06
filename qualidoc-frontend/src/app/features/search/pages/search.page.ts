import { Component, inject, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { SearchActions, SearchSelectors } from '../../../store/shared.store';
import { DOCUMENT_TYPE_LABELS, DocumentType, SearchResult } from '../../../core/models/models';
import { PreviewDialogComponent } from '../../documents/components/preview-dialog.component';

@Component({
  selector: 'app-search-page',
  standalone: true,
  imports: [
    AsyncPipe, NgIf, NgFor, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatIconModule, MatButtonModule, MatProgressSpinnerModule, MatChipsModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Recherche</h1>
      </div>

      <!-- Barre de recherche -->
      <mat-card class="search-card">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Rechercher dans les documents...</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput
                 [(ngModel)]="query"
                 (ngModelChange)="onQueryChange($event)"
                 placeholder="Ex : stérilisation, HACCP, procédure...">
          <button mat-icon-button matSuffix
                  *ngIf="query"
                  (click)="clear()">
            <mat-icon>close</mat-icon>
          </button>
        </mat-form-field>
      </mat-card>

      <!-- Spinner -->
      <div *ngIf="loading$ | async" class="loading-center">
        <mat-spinner diameter="32" />
      </div>

      <!-- Résultats -->
      <ng-container *ngIf="!(loading$ | async)">
        <div *ngIf="(results$ | async) as results">

          <p class="results-count" *ngIf="query && query.length >= 2">
            {{ results.length }} résultat{{ results.length > 1 ? 's' : '' }}
            pour « {{ query }} »
          </p>

          <div class="results-list">
            <mat-card class="result-card" *ngFor="let result of results" (click)="openPreview(result)">
              <mat-card-header>
                <mat-icon mat-card-avatar>insert_drive_file</mat-icon>
                <mat-card-title>{{ result.title }}</mat-card-title>
                <mat-card-subtitle>
                  <mat-chip>{{ getTypeLabel(result.type) }}</mat-chip>
                </mat-card-subtitle>
              </mat-card-header>
              <mat-card-content *ngIf="result.snippet">
                <p class="snippet">…{{ result.snippet }}…</p>
              </mat-card-content>
            </mat-card>
          </div>

          <!-- État vide -->
          <div class="empty-state" *ngIf="results.length === 0 && query && query.length >= 2">
            <mat-icon>search_off</mat-icon>
            <p>Aucun document trouvé pour « {{ query }} »</p>
          </div>

          <!-- Invite initiale -->
          <div class="empty-state" *ngIf="!query || query.length < 2">
            <mat-icon>manage_search</mat-icon>
            <p>Saisissez au moins 2 caractères pour lancer la recherche</p>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { margin-bottom: 16px; }
    .page-header h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .search-card { margin-bottom: 24px; padding: 16px; }
    .search-field { width: 100%; }
    .results-count { color: #757575; font-size: 14px; margin-bottom: 16px; }
    .results-list { display: flex; flex-direction: column; gap: 12px; }
    .result-card { cursor: pointer; transition: box-shadow 0.2s; }
    .result-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
    .snippet { color: #555; font-size: 14px; line-height: 1.6; font-style: italic; }
    .empty-state {
      display: flex; flex-direction: column; align-items: center;
      padding: 64px; color: #bdbdbd;
    }
    .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; margin-bottom: 16px; }
    .loading-center { display: flex; justify-content: center; padding: 32px; }
  `]
})
export class SearchPage implements OnDestroy {
  private store   = inject(Store);
  private dialog  = inject(MatDialog);
  private destroy = new Subject<void>();

  results$ = this.store.select(SearchSelectors.results);
  loading$ = this.store.select(SearchSelectors.loading);
  typeLabels = DOCUMENT_TYPE_LABELS;
  query = '';

  private querySubject = new Subject<string>();

  constructor() {
    this.querySubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy)
    ).subscribe(q => {
      if (q.length >= 2) {
        this.store.dispatch(SearchActions.search({ query: q }));
      }
    });
  }

  openPreview(result: SearchResult) {
    this.dialog.open(PreviewDialogComponent, {
      width: '860px',
      maxWidth: '95vw',
      data: {
        document: {
          id: result.documentId,
          title: result.title,
          mimeType: '',
          originalFilename: result.title,
          folderId: null
        }
      }
    });
  }

  getTypeLabel(type: string): string {
    return this.typeLabels[type as DocumentType] ?? type;
  }

  onQueryChange(q: string) {
    this.querySubject.next(q);
  }

  clear() {
    this.query = '';
    this.store.dispatch(SearchActions.clearSearch());
  }

  ngOnDestroy() {
    this.destroy.next();
    this.destroy.complete();
  }
}
