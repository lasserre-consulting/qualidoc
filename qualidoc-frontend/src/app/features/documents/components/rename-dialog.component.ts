import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface RenameDialogData {
  currentTitle: string;
  dialogTitle?: string;
  label?: string;
  confirmLabel?: string;
}

@Component({
  selector: 'app-rename-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>{{ dialogTitle }}</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" style="width:100%; margin-top:8px">
        <mat-label>{{ label }}</mat-label>
        <input matInput [(ngModel)]="title" (keyup.enter)="confirm()" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Annuler</button>
      <button mat-raised-button color="primary" [disabled]="!title.trim()" (click)="confirm()">
        {{ confirmLabel }}
      </button>
    </mat-dialog-actions>
  `
})
export class RenameDialogComponent {
  title: string;
  dialogTitle: string;
  label: string;
  confirmLabel: string;

  constructor(
    public dialogRef: MatDialogRef<RenameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: RenameDialogData
  ) {
    this.title        = data.currentTitle;
    this.dialogTitle  = data.dialogTitle  ?? 'Renommer';
    this.label        = data.label        ?? 'Nouveau nom';
    this.confirmLabel = data.confirmLabel ?? 'Renommer';
  }

  confirm() {
    if (this.title.trim()) this.dialogRef.close(this.title.trim());
  }
}
