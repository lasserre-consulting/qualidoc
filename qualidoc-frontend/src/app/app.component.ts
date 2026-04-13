import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthActions, AuthSelectors } from './store/shared.store';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe, NgIf,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatBadgeModule, MatTooltipModule,
  ],
  template: `
    <!-- Login page: pas de sidenav -->
    <ng-container *ngIf="isLoginPage(); else appLayout">
      <router-outlet />
    </ng-container>

    <ng-template #appLayout>
      <mat-sidenav-container class="sidenav-container">

        <!-- Sidebar navigation -->
        <mat-sidenav mode="side" opened class="sidenav">
          <div class="sidenav-header">
            <div class="logo-svg" aria-label="QualiDoc"></div>
            <span class="logo-sub">Plateforme qualité</span>
          </div>

          <mat-nav-list>
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
              <mat-icon matListItemIcon>dashboard</mat-icon>
              <span matListItemTitle>Tableau de bord</span>
            </a>
            <a mat-list-item routerLink="/documents" routerLinkActive="active-link">
              <mat-icon matListItemIcon>folder_open</mat-icon>
              <span matListItemTitle>Documents</span>
            </a>
            <a mat-list-item routerLink="/search" routerLinkActive="active-link">
              <mat-icon matListItemIcon>search</mat-icon>
              <span matListItemTitle>Recherche</span>
            </a>
            <a mat-list-item routerLink="/admin" routerLinkActive="active-link"
               *ngIf="isEditor$ | async">
              <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
              <span matListItemTitle>Administration</span>
            </a>
          </mat-nav-list>

          <!-- User info en bas -->
          <div class="sidenav-footer" *ngIf="user$ | async as user">
            <mat-icon>account_circle</mat-icon>
            <div class="user-info">
              <span class="user-name">{{ user.fullName }}</span>
              <span class="user-role">{{ user.role === 'EDITOR' ? 'Éditeur' : 'Lecteur' }}</span>
            </div>
            <button mat-icon-button (click)="logout()" matTooltip="Déconnexion">
              <mat-icon>logout</mat-icon>
            </button>
          </div>
        </mat-sidenav>

        <!-- Contenu principal -->
        <mat-sidenav-content class="main-content">
          <router-outlet />
        </mat-sidenav-content>

      </mat-sidenav-container>
    </ng-template>
  `,
  styles: [`
    .sidenav-container { height: 100vh; }
    .sidenav { width: 240px; display: flex; flex-direction: column; border-right: 1px solid #e0e0e0; }
    .sidenav-header { padding: 20px 16px 14px; background: #1565C0; color: white; }
    .logo-svg {
      width: 168px; height: 42px;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 260 68'%3E%3Ctext x='112' y='52' text-anchor='end' font-family='Roboto,Arial,sans-serif' font-size='44' font-weight='700' fill='white'%3EQuali%3C/text%3E%3Cg transform='translate(114,5)'%3E%3Cpath d='M16 0 L32 7 L32 22 C32 32 25 39 16 42 C7 39 0 32 0 22 L0 7 Z' fill='white'/%3E%3Cpath d='M7 22 L13 28 L25 13' stroke='%231565C0' stroke-width='4' stroke-linecap='round' stroke-linejoin='round' fill='none'/%3E%3C/g%3E%3Ctext x='150' y='52' text-anchor='start' font-family='Roboto,Arial,sans-serif' font-size='44' font-weight='300' fill='rgba(255,255,255,0.85)'%3EDoc%3C/text%3E%3C/svg%3E");
      background-size: contain;
      background-repeat: no-repeat;
      background-position: left center;
    }
    .logo-sub { font-size: 11px; opacity: 0.7; margin-top: 2px; display: block; }
    .active-link { background: rgba(21, 101, 192, 0.1); color: #1565C0; }
    .active-link mat-icon { color: #1565C0; }
    .main-content { background: #f5f5f5; }
    .sidenav-footer {
      margin-top: auto; padding: 12px 16px;
      border-top: 1px solid #e0e0e0;
      display: flex; align-items: center; gap: 8px;
    }
    .user-info { flex: 1; overflow: hidden; }
    .user-name { display: block; font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .user-role { display: block; font-size: 11px; color: #757575; }
  `]
})
export class AppComponent implements OnInit {
  private store       = inject(Store);
  private router      = inject(Router);
  private authService = inject(AuthService);

  user$    = this.store.select(AuthSelectors.user);
  isEditor$ = this.store.select(AuthSelectors.isEditor);

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.store.dispatch(AuthActions.loadProfile());
    }
  }

  isLoginPage(): boolean {
    return this.router.url === '/login';
  }

  logout() {
    this.store.dispatch(AuthActions.logout());
  }
}
