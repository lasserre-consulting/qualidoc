import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { User } from '../../../core/models/models';

@Component({
  selector: 'app-reset-password-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Réinitialiser le mot de passe</h2>
    <mat-dialog-content>
      <p>Utilisateur : <strong>{{ data.user.firstName }} {{ data.user.lastName }}</strong></p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Nouveau mot de passe</mat-label>
        <input matInput [formControl]="passwordControl" type="password">
        <mat-error>Minimum 8 caractères</mat-error>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="warn" (click)="onSubmit()" [disabled]="passwordControl.invalid">
        Réinitialiser
      </button>
    </mat-dialog-actions>
  `,
  styles: [`.full-width { width: 100%; }`]
})
export class ResetPasswordDialogComponent {
  private fb  = inject(FormBuilder);
  private ref = inject(MatDialogRef<ResetPasswordDialogComponent>);
  data        = inject<{ user: User }>(MAT_DIALOG_DATA);

  passwordControl = this.fb.control('', [Validators.required, Validators.minLength(8)]);

  onSubmit(): void {
    if (this.passwordControl.valid) {
      this.ref.close(this.passwordControl.value);
    }
  }
}
