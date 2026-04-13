import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { NgIf } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { AuthActions } from '../../../store/shared.store';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="login-backdrop">
      <mat-card class="login-card">
        <div class="login-header">
          <div class="logo-svg" aria-label="QualiDoc"></div>
          <span class="logo-sub">Plateforme qualité</span>
        </div>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Adresse email</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email">
              <mat-icon matPrefix>email</mat-icon>
              <mat-error *ngIf="form.controls.email.hasError('required')">Email requis</mat-error>
              <mat-error *ngIf="form.controls.email.hasError('email')">Email invalide</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Mot de passe</mat-label>
              <input matInput formControlName="password"
                     [type]="hidePassword ? 'password' : 'text'"
                     autocomplete="current-password">
              <mat-icon matPrefix>lock</mat-icon>
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword = !hidePassword"
                      [attr.aria-label]="hidePassword ? 'Afficher le mot de passe' : 'Masquer le mot de passe'">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="form.controls.password.hasError('required')">Mot de passe requis</mat-error>
            </mat-form-field>

            <div class="error-message" *ngIf="errorMessage">
              <mat-icon>error_outline</mat-icon>
              {{ errorMessage }}
            </div>

            <button mat-raised-button color="primary" type="submit"
                    class="full-width submit-btn"
                    [disabled]="loading">
              <mat-spinner *ngIf="loading" diameter="20" class="btn-spinner"></mat-spinner>
              <span *ngIf="!loading">Se connecter</span>
            </button>

          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-backdrop {
      display: flex; align-items: center; justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #0D47A1 0%, #1565C0 40%, #1E88E5 100%);
    }
    .login-card {
      width: 100%; max-width: 420px; margin: 16px;
      padding: 40px 32px 32px;
      border-radius: 12px;
    }
    .login-header {
      text-align: center; margin-bottom: 32px;
    }
    .logo-svg {
      width: 200px; height: 50px; margin: 0 auto;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 260 68'%3E%3Ctext x='112' y='52' text-anchor='end' font-family='Roboto,Arial,sans-serif' font-size='44' font-weight='700' fill='%231565C0'%3EQuali%3C/text%3E%3Cg transform='translate(114,5)'%3E%3Cpath d='M16 0 L32 7 L32 22 C32 32 25 39 16 42 C7 39 0 32 0 22 L0 7 Z' fill='%231565C0'/%3E%3Cpath d='M7 22 L13 28 L25 13' stroke='white' stroke-width='4' stroke-linecap='round' stroke-linejoin='round' fill='none'/%3E%3C/g%3E%3Ctext x='150' y='52' text-anchor='start' font-family='Roboto,Arial,sans-serif' font-size='44' font-weight='300' fill='%2390CAF9'%3EDoc%3C/text%3E%3C/svg%3E");
      background-size: contain;
      background-repeat: no-repeat;
      background-position: center;
    }
    .logo-sub {
      display: block; margin-top: 4px;
      font-size: 13px; color: #90a4ae; letter-spacing: 0.5px;
    }
    .full-width { width: 100%; }
    .submit-btn {
      margin-top: 8px; height: 48px; font-size: 16px;
      display: flex; align-items: center; justify-content: center;
    }
    .btn-spinner { display: inline-block; }
    .error-message {
      display: flex; align-items: center; gap: 8px;
      color: #d32f2f; font-size: 13px;
      margin: -8px 0 12px; padding: 8px 12px;
      background: #ffebee; border-radius: 6px;
    }
    @media (max-width: 480px) {
      .login-card { padding: 32px 20px 24px; }
    }
  `]
})
export class LoginPageComponent {
  private fb          = inject(FormBuilder);
  private authService = inject(AuthService);
  private router      = inject(Router);
  private store       = inject(Store);

  form = this.fb.nonNullable.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  hidePassword = true;
  loading      = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { email, password } = this.form.getRawValue();

    this.authService.login(email, password).subscribe({
      next: () => {
        this.store.dispatch(AuthActions.loadProfile());
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect';
        } else {
          this.errorMessage = 'Une erreur est survenue. Veuillez réessayer.';
        }
      }
    });
  }
}
