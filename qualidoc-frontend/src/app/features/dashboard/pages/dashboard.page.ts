import { Component, OnInit, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf, NgFor, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { DocumentActions, DocumentSelectors } from '../../../store/documents/documents.store';
import { AuthSelectors } from '../../../store/shared.store';
import { DOCUMENT_TYPE_LABELS } from '../../../core/models/models';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    AsyncPipe, NgIf, NgFor, DecimalPipe, RouterLink,
    MatCardModule, MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, MatChipsModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Tableau de bord</h1>
        <p *ngIf="user$ | async as user">Bonjour {{ user.fullName }}</p>
      </div>

      <div *ngIf="loading$ | async" class="loading-center">
        <mat-spinner diameter="40" />
      </div>

      <ng-container *ngIf="!(loading$ | async)">

        <!-- Cartes de stats -->
        <div class="stats-grid">
          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon class="stat-icon blue">folder_open</mat-icon>
              <div class="stat-value">{{ (documents$ | async)?.length ?? 0 }}</div>
              <div class="stat-label">Documents accessibles</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon class="stat-icon green">description</mat-icon>
              <div class="stat-value">{{ (byType$ | async)?.['PROCEDURE'] ?? 0 }}</div>
              <div class="stat-label">Procédures</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon class="stat-icon teal">assignment</mat-icon>
              <div class="stat-value">{{ (byType$ | async)?.['PROTOCOL'] ?? 0 }}</div>
              <div class="stat-label">Protocoles</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon class="stat-icon orange">article</mat-icon>
              <div class="stat-value">{{ (byType$ | async)?.['FORM'] ?? 0 }}</div>
              <div class="stat-label">Formulaires</div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <mat-icon class="stat-icon purple">menu_book</mat-icon>
              <div class="stat-value">{{ (byType$ | async)?.['AWARENESS_BOOKLET'] ?? 0 }}</div>
              <div class="stat-label">Livrets</div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Actions rapides (éditeurs) -->
        <div class="quick-actions" *ngIf="isEditor$ | async">
          <a mat-raised-button color="primary" routerLink="/documents/upload">
            <mat-icon>upload_file</mat-icon>
            Uploader un document
          </a>
          <a mat-stroked-button routerLink="/search">
            <mat-icon>search</mat-icon>
            Rechercher
          </a>
        </div>

        <!-- Documents récents -->
        <mat-card class="recent-card">
          <mat-card-header>
            <mat-card-title>Documents récents</mat-card-title>
            <a mat-button routerLink="/documents">Voir tout</a>
          </mat-card-header>
          <mat-card-content>
            <div class="doc-list">
              <div class="doc-item"
                   *ngFor="let doc of (documents$ | async)?.slice(0, 5)">
                <mat-icon class="doc-icon">insert_drive_file</mat-icon>
                <div class="doc-info">
                  <span class="doc-title">{{ doc.title }}</span>
                  <span class="doc-meta">
                    <mat-chip highlighted>{{ typeLabels[doc.type] }}</mat-chip>
                    · v{{ doc.version }}
                    · {{ doc.sizeBytes | number }} octets
                  </span>
                </div>
              </div>
              <div *ngIf="(documents$ | async)?.length === 0" class="empty-state">
                <mat-icon>folder_open</mat-icon>
                <p>Aucun document disponible</p>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

      </ng-container>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .page-header p { margin: 4px 0 0; color: #757575; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat-card mat-card-content { display: flex; flex-direction: column; align-items: center; padding: 24px 16px; }
    .stat-icon { font-size: 40px; width: 40px; height: 40px; margin-bottom: 12px; }
    .stat-icon.blue { color: #1565C0; }
    .stat-icon.green { color: #2E7D32; }
    .stat-icon.orange { color: #E65100; }
    .stat-icon.purple { color: #6A1B9A; }
    .stat-value { font-size: 36px; font-weight: 600; line-height: 1; }
    .stat-label { font-size: 13px; color: #757575; margin-top: 4px; }
    .quick-actions { display: flex; gap: 12px; margin-bottom: 24px; }
    .quick-actions a { display: flex; align-items: center; gap: 6px; }
    .recent-card mat-card-header { display: flex; justify-content: space-between; align-items: center; }
    .doc-list { display: flex; flex-direction: column; gap: 8px; }
    .doc-item { display: flex; align-items: center; gap: 12px; padding: 12px; border-radius: 8px; background: #f9f9f9; }
    .doc-icon { color: #1565C0; }
    .doc-info { flex: 1; }
    .doc-title { display: block; font-weight: 500; }
    .doc-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #757575; margin-top: 4px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 32px; color: #bdbdbd; }
    .loading-center { display: flex; justify-content: center; padding: 48px; }
  `]
})
export class DashboardPage implements OnInit {
  private store = inject(Store);

  documents$ = this.store.select(DocumentSelectors.all);
  loading$   = this.store.select(DocumentSelectors.loading);
  byType$    = this.store.select(DocumentSelectors.byType);
  user$      = this.store.select(AuthSelectors.user);
  isEditor$  = this.store.select(AuthSelectors.isEditor);
  typeLabels = DOCUMENT_TYPE_LABELS;

  ngOnInit() {
    this.store.dispatch(DocumentActions.loadDocuments({ all: true }));
  }
}
