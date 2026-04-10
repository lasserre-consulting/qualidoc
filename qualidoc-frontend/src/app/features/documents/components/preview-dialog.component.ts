import { Component, Inject, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { NgIf } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DocumentService } from '../../../core/services/services';
import { Document } from '../../../core/models/models';

export interface PreviewDialogData {
  document: Document;
}

@Component({
  selector: 'app-preview-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgIf, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="preview-header">
      <h2 mat-dialog-title>{{ data.document.title }}</h2>
      <button mat-icon-button (click)="dialogRef.close()">
        <mat-icon>close</mat-icon>
      </button>
    </div>

    <mat-dialog-content class="preview-content">
      <div *ngIf="loading" class="loading-center">
        <mat-spinner diameter="48" />
      </div>

      <div *ngIf="error" class="error-msg">
        <mat-icon>error_outline</mat-icon>
        Impossible de charger le document.
      </div>

      <iframe *ngIf="!loading && !error && isPdf"
              [src]="safeUrl"
              class="preview-frame"
              frameborder="0">
      </iframe>

      <img *ngIf="!loading && !error && isImage"
           [src]="safeUrl"
           class="preview-img"
           [alt]="data.document.title" />

      <div *ngIf="!loading && !error && !isPdf && !isImage" class="unsupported">
        <mat-icon>insert_drive_file</mat-icon>
        <p>Aperçu non disponible pour ce type de fichier.</p>
        <button mat-raised-button color="primary" (click)="download()">
          <mat-icon>download</mat-icon> Télécharger
        </button>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="download()">
        <mat-icon>download</mat-icon> Télécharger
      </button>
      <button mat-button (click)="dialogRef.close()">Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 16px 0;
    }
    .preview-header h2 { margin: 0; font-size: 18px; font-weight: 500; }
    .preview-content {
      padding: 16px;
      max-height: 75vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .loading-center { display: flex; justify-content: center; padding: 48px; }
    .preview-frame { width: 100%; height: 70vh; min-width: 700px; }
    .preview-img { max-width: 100%; max-height: 70vh; object-fit: contain; }
    .unsupported { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #9e9e9e; }
    .unsupported mat-icon { font-size: 64px; width: 64px; height: 64px; }
    .error-msg { display: flex; align-items: center; gap: 8px; color: #f44336; }
  `]
})
export class PreviewDialogComponent implements OnInit, OnDestroy {
  loading = true;
  error   = false;
  isPdf   = false;
  isImage = false;
  safeUrl: SafeResourceUrl | null = null;

  private objectUrl: string | null = null;
  private blob: Blob | null = null;

  constructor(
    public dialogRef: MatDialogRef<PreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PreviewDialogData,
    private sanitizer: DomSanitizer,
    private service: DocumentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.service.download(this.data.document.id).subscribe({
      next: blob => {
        this.blob = blob;
        this.isPdf   = blob.type === 'application/pdf' || this.data.document.mimeType === 'application/pdf';
        this.isImage = blob.type.startsWith('image/') || this.data.document.mimeType.startsWith('image/');
        this.objectUrl = URL.createObjectURL(blob);
        this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.error   = true;
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy() {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
  }

  download() {
    if (!this.blob) return;
    const url = URL.createObjectURL(this.blob);
    const a = window.document.createElement('a');
    a.href = url;
    a.download = this.data.document.originalFilename ?? this.data.document.title;
    a.click();
    URL.revokeObjectURL(url);
  }
}
