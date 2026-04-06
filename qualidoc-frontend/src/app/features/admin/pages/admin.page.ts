import { Component, OnInit, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EstablishmentActions, EstablishmentSelectors } from '../../../store/shared.store';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [
    AsyncPipe, NgIf,
    MatCardModule, MatTabsModule, MatTableModule,
    MatIconModule, MatButtonModule, MatChipsModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Administration</h1>
      </div>

      <mat-tab-group>

        <!-- Onglet Établissements -->
        <mat-tab label="Établissements">
          <div class="tab-content">
            <div *ngIf="loadingEstab$ | async" class="loading-center">
              <mat-spinner diameter="32" />
            </div>
            <mat-card *ngIf="!(loadingEstab$ | async)">
              <table mat-table [dataSource]="(establishments$ | async) ?? []" class="full-table">

                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let e">
                    <div class="estab-name-cell">
                      <mat-icon>business</mat-icon>
                      {{ e.name }}
                    </div>
                  </td>
                </ng-container>

                <ng-container matColumnDef="code">
                  <th mat-header-cell *matHeaderCellDef>Code</th>
                  <td mat-cell *matCellDef="let e">
                    <code>{{ e.code }}</code>
                  </td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let e">
                    <mat-chip [color]="e.active ? 'primary' : 'warn'" highlighted>
                      {{ e.active ? 'Actif' : 'Inactif' }}
                    </mat-chip>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="estabColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: estabColumns;"></tr>
              </table>
            </mat-card>
          </div>
        </mat-tab>

        <!-- Onglet Statistiques d'usage -->
        <mat-tab label="Statistiques">
          <div class="tab-content">
            <div class="stats-info">
              <mat-card>
                <mat-card-header>
                  <mat-card-title>
                    <mat-icon>bar_chart</mat-icon>
                    Métriques d'usage
                  </mat-card-title>
                </mat-card-header>
                <mat-card-content>
                  <p class="info-text">
                    Les métriques détaillées sont disponibles via
                    <strong>Grafana</strong> connecté à Prometheus
                    (endpoint <code>/actuator/prometheus</code>).
                  </p>
                  <div class="metrics-grid">
                    <div class="metric-item">
                      <mat-icon color="primary">folder</mat-icon>
                      <span>Documents uploadés</span>
                    </div>
                    <div class="metric-item">
                      <mat-icon color="accent">share</mat-icon>
                      <span>Partages effectués</span>
                    </div>
                    <div class="metric-item">
                      <mat-icon color="warn">search</mat-icon>
                      <span>Recherches lancées</span>
                    </div>
                    <div class="metric-item">
                      <mat-icon>people</mat-icon>
                      <span>Utilisateurs actifs</span>
                    </div>
                  </div>
                </mat-card-content>
              </mat-card>
            </div>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .tab-content { padding: 24px 0; }
    .full-table { width: 100%; }
    .estab-name-cell { display: flex; align-items: center; gap: 8px; }
    code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
    .loading-center { display: flex; justify-content: center; padding: 32px; }
    .stats-info mat-card-title { display: flex; align-items: center; gap: 8px; }
    .info-text { color: #555; margin-bottom: 24px; }
    .metrics-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
    .metric-item {
      display: flex; align-items: center; gap: 12px;
      padding: 16px; background: #f9f9f9; border-radius: 8px;
      font-size: 14px;
    }
  `]
})
export class AdminPage implements OnInit {
  private store = inject(Store);

  establishments$  = this.store.select(EstablishmentSelectors.all);
  loadingEstab$    = this.store.select(EstablishmentSelectors.loading);
  estabColumns     = ['name', 'code', 'status'];

  ngOnInit() {
    this.store.dispatch(EstablishmentActions.load());
  }
}
