import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { NgIf, NgFor } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Establishment, User } from '../../../core/models/models';

interface DialogData {
  mode: 'create' | 'edit';
  user?: User;
  establishments: Establishment[];
}

@Component({
  selector: 'app-user-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgIf, NgFor,
    MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'create' ? 'Nouvel utilisateur' : 'Modifier l\\'utilisateur' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-grid">

        <mat-form-field appearance="outline" class="full-width" *ngIf="data.mode === 'create'">
          <mat-label>Email</mat-label>
          <input matInput formControlName="email" type="email">
          <mat-error>Email requis et valide</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Prénom</mat-label>
          <input matInput formControlName="firstName">
          <mat-error>Prénom requis</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="lastName">
          <mat-error>Nom requis</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Rôle</mat-label>
          <mat-select formControlName="role">
            <mat-option value="READER">Lecteur</mat-option>
            <mat-option value="EDITOR">Éditeur</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" *ngIf="data.mode === 'create'">
          <mat-label>Établissement</mat-label>
          <mat-select formControlName="establishmentId">
            <mat-option *ngFor="let e of data.establishments" [value]="e.id">
              {{ e.name }}
            </mat-option>
          </mat-select>
          <mat-error>Établissement requis</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width" *ngIf="data.mode === 'create'">
          <mat-label>Mot de passe temporaire</mat-label>
          <input matInput formControlName="password" type="password">
          <mat-error>Mot de passe requis (min. 8 caractères)</mat-error>
        </mat-form-field>

        <mat-slide-toggle formControlName="active" *ngIf="data.mode === 'edit'" class="full-width">
          Compte actif
        </mat-slide-toggle>

      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="form.invalid">
        {{ data.mode === 'create' ? 'Créer' : 'Enregistrer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-grid { display: flex; flex-wrap: wrap; gap: 0 16px; }
    .form-grid mat-form-field { flex: 1 1 200px; }
    .full-width { flex: 1 1 100% !important; }
    mat-dialog-content { max-height: 70vh; }
  `]
})
export class UserFormDialogComponent {
  private fb  = inject(FormBuilder);
  private ref = inject(MatDialogRef<UserFormDialogComponent>);
  data        = inject<DialogData>(MAT_DIALOG_DATA);

  form = this.data.mode === 'create'
    ? this.fb.nonNullable.group({
        email:           ['', [Validators.required, Validators.email]],
        firstName:       ['', Validators.required],
        lastName:        ['', Validators.required],
        role:            ['READER' as 'READER' | 'EDITOR', Validators.required],
        establishmentId: ['', Validators.required],
        password:        ['', [Validators.required, Validators.minLength(8)]],
        active:          [true],
      })
    : this.fb.nonNullable.group({
        email:           [{ value: this.data.user?.email ?? '', disabled: true }],
        firstName:       [this.data.user?.firstName ?? '', Validators.required],
        lastName:        [this.data.user?.lastName ?? '', Validators.required],
        role:            [(this.data.user?.role ?? 'READER') as 'READER' | 'EDITOR', Validators.required],
        establishmentId: [{ value: this.data.user?.establishmentId ?? '', disabled: true }],
        password:        [{ value: '', disabled: true }],
        active:          [true],
      });

  onSubmit(): void {
    if (this.form.invalid) return;

    if (this.data.mode === 'create') {
      const v = this.form.getRawValue();
      this.ref.close({
        email: v.email,
        firstName: v.firstName,
        lastName: v.lastName,
        role: v.role,
        establishmentId: v.establishmentId,
        password: v.password,
      });
    } else {
      const v = this.form.getRawValue();
      this.ref.close({
        firstName: v.firstName,
        lastName: v.lastName,
        role: v.role,
        active: v.active,
      });
    }
  }
}
