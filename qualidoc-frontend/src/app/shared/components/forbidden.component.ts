import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="forbidden-container">
      <mat-icon class="forbidden-icon">lock</mat-icon>
      <h1>Accès refusé</h1>
      <p>Vous n'avez pas les droits nécessaires pour accéder à cette page.</p>
      <a mat-raised-button color="primary" routerLink="/dashboard">
        <mat-icon>home</mat-icon>
        Retour au tableau de bord
      </a>
    </div>
  `,
  styles: [`
    .forbidden-container {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; height: 100vh; gap: 16px;
      color: #555;
    }
    .forbidden-icon { font-size: 80px; width: 80px; height: 80px; color: #e0e0e0; }
    h1 { margin: 0; font-size: 32px; font-weight: 500; }
    p { color: #9e9e9e; }
    a { display: flex; align-items: center; gap: 6px; }
  `]
})
export class ForbiddenComponent {}
