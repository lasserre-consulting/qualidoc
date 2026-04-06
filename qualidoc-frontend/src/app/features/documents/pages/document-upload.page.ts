import { Component, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { NgIf, AsyncPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DocumentActions, DocumentSelectors } from '../../../store/documents/documents.store';

// ── Page Upload ───────────────────────────────────────────────────────────────

@Component({
  selector: 'app-document-upload-page',
  standalone: true,
  imports: [
    NgIf, AsyncPipe, FormsModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Uploader un document</h1>
      </div>

      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">

            <!-- Zone drag & drop fichier -->
            <div class="drop-zone"
                 [class.has-file]="selectedFile"
                 (click)="fileInput.click()"
                 (dragover)="$event.preventDefault()"
                 (drop)="onDrop($event)">
              <mat-icon>{{ selectedFile ? 'check_circle' : 'cloud_upload' }}</mat-icon>
              <span *ngIf="!selectedFile">
                Glissez un fichier ici ou cliquez pour sélectionner
              </span>
              <span *ngIf="selectedFile">{{ selectedFile.name }}</span>
              <input #fileInput type="file" hidden
                     accept=".pdf,.doc,.docx,.xls,.xlsx"
                     (change)="onFileSelected($event)">
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Titre du document</mat-label>
              <input matInput formControlName="title" placeholder="Ex : Procédure de stérilisation">
              <mat-error *ngIf="form.get('title')?.hasError('required')">
                Le titre est obligatoire
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Type de document</mat-label>
              <mat-select formControlName="type">
                <mat-option value="PROCEDURE">Procédure</mat-option>
                <mat-option value="PROTOCOL">Protocole</mat-option>
                <mat-option value="FORM">Formulaire qualité</mat-option>
                <mat-option value="AWARENESS_BOOKLET">Livret de sensibilisation</mat-option>
              </mat-select>
              <mat-error *ngIf="form.get('type')?.hasError('required')">
                Le type est obligatoire
              </mat-error>
            </mat-form-field>

            <mat-progress-bar *ngIf="uploading$ | async" mode="indeterminate" />

            <div class="form-actions">
              <button mat-button type="button" (click)="cancel()">Annuler</button>
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="form.invalid || !selectedFile || (uploading$ | async)">
                <mat-icon>upload</mat-icon>
                Publier le document
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 700px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .drop-zone {
      border: 2px dashed #90CAF9; border-radius: 12px; padding: 40px;
      text-align: center; cursor: pointer; margin-bottom: 24px;
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      color: #1565C0; transition: background 0.2s;
    }
    .drop-zone:hover { background: #E3F2FD; }
    .drop-zone.has-file { border-color: #4CAF50; color: #2E7D32; background: #F1F8E9; }
    .drop-zone mat-icon { font-size: 48px; width: 48px; height: 48px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
    .form-actions button { display: flex; align-items: center; gap: 6px; }
  `]
})
export class DocumentUploadPage {
  private store  = inject(Store);
  private router = inject(Router);
  private fb     = inject(FormBuilder);

  uploading$ = this.store.select(DocumentSelectors.uploading);
  selectedFile: File | null = null;

  form = this.fb.group({
    title: ['', Validators.required],
    type:  ['', Validators.required],
  });

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.selectedFile = input.files[0];
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) this.selectedFile = file;
  }

  onSubmit() {
    if (this.form.invalid || !this.selectedFile) return;
    const { title, type } = this.form.value;
    this.store.dispatch(DocumentActions.uploadDocument({
      file: this.selectedFile,
      title: title!,
      docType: type!
    }));
    this.router.navigate(['/documents']);
  }

  cancel() {
    this.router.navigate(['/documents']);
  }
}
