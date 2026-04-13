import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf, NgFor } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EstablishmentActions, EstablishmentSelectors } from '../../../store/shared.store';
import { Establishment, User, UserRole } from '../../../core/models/models';
import { AdminUsersService } from '../../../core/services/admin-users.service';
import { UserFormDialogComponent } from '../components/user-form-dialog.component';
import { ResetPasswordDialogComponent } from '../components/reset-password-dialog.component';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [
    AsyncPipe, NgIf, NgFor, ReactiveFormsModule,
    MatCardModule, MatTabsModule, MatTableModule,
    MatIconModule, MatButtonModule, MatChipsModule, MatProgressSpinnerModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatDialogModule, MatTooltipModule,
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

        <!-- Onglet Utilisateurs -->
        <mat-tab label="Utilisateurs">
          <div class="tab-content">

            <div class="tab-toolbar">
              <mat-form-field appearance="outline" class="filter-field">
                <mat-label>Filtrer par établissement</mat-label>
                <mat-select (selectionChange)="filterByEstablishment($event.value)" [value]="''">
                  <mat-option value="">Tous</mat-option>
                  <mat-option *ngFor="let e of (establishments$ | async) ?? []" [value]="e.id">
                    {{ e.name }}
                  </mat-option>
                </mat-select>
              </mat-form-field>
              <button mat-raised-button color="primary" (click)="openCreateUser()">
                <mat-icon>person_add</mat-icon>
                Nouvel utilisateur
              </button>
            </div>

            <div *ngIf="loadingUsers" class="loading-center">
              <mat-spinner diameter="32" />
            </div>

            <mat-card *ngIf="!loadingUsers">
              <table mat-table [dataSource]="filteredUsers" class="full-table">

                <ng-container matColumnDef="email">
                  <th mat-header-cell *matHeaderCellDef>Email</th>
                  <td mat-cell *matCellDef="let u">{{ u.email }}</td>
                </ng-container>

                <ng-container matColumnDef="firstName">
                  <th mat-header-cell *matHeaderCellDef>Prénom</th>
                  <td mat-cell *matCellDef="let u">{{ u.firstName }}</td>
                </ng-container>

                <ng-container matColumnDef="lastName">
                  <th mat-header-cell *matHeaderCellDef>Nom</th>
                  <td mat-cell *matCellDef="let u">{{ u.lastName }}</td>
                </ng-container>

                <ng-container matColumnDef="role">
                  <th mat-header-cell *matHeaderCellDef>Rôle</th>
                  <td mat-cell *matCellDef="let u">
                    <mat-chip [color]="u.role === 'EDITOR' ? 'primary' : 'accent'" highlighted>
                      {{ u.role === 'EDITOR' ? 'Éditeur' : 'Lecteur' }}
                    </mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="establishment">
                  <th mat-header-cell *matHeaderCellDef>Établissement</th>
                  <td mat-cell *matCellDef="let u">{{ getEstablishmentName(u.establishmentId) }}</td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let u">
                    <button mat-icon-button matTooltip="Modifier" (click)="openEditUser(u)">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Réinitialiser le mot de passe" (click)="openResetPassword(u)">
                      <mat-icon>lock_reset</mat-icon>
                    </button>
                    <button mat-icon-button matTooltip="Supprimer" color="warn" (click)="deleteUser(u)">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="userColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: userColumns;"></tr>

                <tr class="mat-row" *matNoDataRow>
                  <td class="mat-cell empty-row" [attr.colspan]="userColumns.length">
                    Aucun utilisateur trouvé
                  </td>
                </tr>
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
    .tab-toolbar { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .filter-field { width: 280px; }
    .full-table { width: 100%; }
    .estab-name-cell { display: flex; align-items: center; gap: 8px; }
    code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 12px; }
    .loading-center { display: flex; justify-content: center; padding: 32px; }
    .empty-row { text-align: center; padding: 32px; color: #9e9e9e; }
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
  private store        = inject(Store);
  private dialog       = inject(MatDialog);
  private usersService = inject(AdminUsersService);
  private destroyRef   = inject(DestroyRef);

  establishments$  = this.store.select(EstablishmentSelectors.all);
  loadingEstab$    = this.store.select(EstablishmentSelectors.loading);
  estabColumns     = ['name', 'code', 'status'];
  userColumns      = ['email', 'firstName', 'lastName', 'role', 'establishment', 'actions'];

  users: User[]          = [];
  filteredUsers: User[]  = [];
  loadingUsers           = false;
  private establishments: Establishment[] = [];
  private filterEstabId  = '';

  ngOnInit() {
    this.store.dispatch(EstablishmentActions.load());
    this.establishments$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(e => this.establishments = e);
    this.loadUsers();
  }

  loadUsers(): void {
    this.loadingUsers = true;
    this.usersService.listUsers().subscribe({
      next: users => {
        this.users = users;
        this.applyFilter();
        this.loadingUsers = false;
      },
      error: () => {
        this.loadingUsers = false;
      }
    });
  }

  filterByEstablishment(estabId: string): void {
    this.filterEstabId = estabId;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filteredUsers = this.filterEstabId
      ? this.users.filter(u => u.establishmentId === this.filterEstabId)
      : [...this.users];
  }

  getEstablishmentName(id: string): string {
    return this.establishments.find(e => e.id === id)?.name ?? id;
  }

  openCreateUser(): void {
    const ref = this.dialog.open(UserFormDialogComponent, {
      width: '500px',
      data: { mode: 'create', establishments: this.establishments }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.usersService.createUser(result).subscribe({
          next: () => this.loadUsers()
        });
      }
    });
  }

  openEditUser(user: User): void {
    const ref = this.dialog.open(UserFormDialogComponent, {
      width: '500px',
      data: { mode: 'edit', user, establishments: this.establishments }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.usersService.updateUser(user.id, result).subscribe({
          next: () => this.loadUsers()
        });
      }
    });
  }

  openResetPassword(user: User): void {
    const ref = this.dialog.open(ResetPasswordDialogComponent, {
      width: '400px',
      data: { user }
    });
    ref.afterClosed().subscribe(newPassword => {
      if (newPassword) {
        this.usersService.resetPassword(user.id, newPassword).subscribe();
      }
    });
  }

  deleteUser(user: User): void {
    const confirmed = window.confirm(
      `Supprimer l'utilisateur ${user.firstName} ${user.lastName} (${user.email}) ? Cette action est irréversible.`
    );
    if (confirmed) {
      this.usersService.deleteUser(user.id).subscribe({
        next: () => this.loadUsers()
      });
    }
  }
}
