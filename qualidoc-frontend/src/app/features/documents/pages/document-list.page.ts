import { Component, OnInit, inject, computed, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { AsyncPipe, NgIf, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentActions, DocumentSelectors } from '../../../store/documents/documents.store';
import { EstablishmentActions, AuthSelectors } from '../../../store/shared.store';
import { Document, DocumentType, DOCUMENT_TYPE_LABELS } from '../../../core/models/models';
import { DocumentService } from '../../../core/services/services';
import { PreviewDialogComponent } from '../components/preview-dialog.component';
import { RenameDialogComponent } from '../components/rename-dialog.component';
import { MoveDialogComponent } from '../components/move-dialog.component';
import { FolderTreeComponent } from '../components/folder-tree.component';
import { FolderSelectors } from '../../../store/folders/folders.store';

@Component({
  selector: 'app-document-list-page',
  standalone: true,
  imports: [
    AsyncPipe, NgIf, DecimalPipe, RouterLink, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatTableModule,
    MatSelectModule, MatFormFieldModule, MatChipsModule,
    MatProgressSpinnerModule, MatTooltipModule,
    FolderTreeComponent,
  ],
  template: `
    <div class="page-layout">

      <!-- Arborescence de dossiers -->
      <div class="folder-sidebar">
        <app-folder-tree (folderSelected)="onFolderSelected($event)" />
      </div>

      <!-- Contenu principal -->
      <div class="content-area">
        <div class="page-header">
          <div class="header-left">
            <h1>Documents</h1>
            <span class="folder-breadcrumb" *ngIf="currentFolderName()">
              <mat-icon>folder</mat-icon> {{ currentFolderName() }}
            </span>
          </div>
          <a mat-raised-button color="primary" routerLink="/documents/upload"
             *ngIf="isEditor$ | async">
            <mat-icon>upload_file</mat-icon>
            Uploader
          </a>
        </div>

        <!-- Filtre par type -->
        <mat-card class="filter-card">
          <mat-form-field appearance="outline">
            <mat-label>Filtrer par type</mat-label>
            <mat-select [ngModel]="selectedType()" (ngModelChange)="selectedType.set($event)">
              <mat-option value="">Tous</mat-option>
              <mat-option value="PROCEDURE">Procédures</mat-option>
              <mat-option value="PROTOCOL">Protocoles</mat-option>
              <mat-option value="FORM">Formulaires</mat-option>
              <mat-option value="AWARENESS_BOOKLET">Livrets</mat-option>
            </mat-select>
          </mat-form-field>
        </mat-card>

        <div *ngIf="loading$ | async" class="loading-center">
          <mat-spinner diameter="40" />
        </div>

        <mat-card *ngIf="!(loading$ | async)">
          <table mat-table [dataSource]="filteredDocuments" class="doc-table">

            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Titre</th>
              <td mat-cell *matCellDef="let doc">
                <div class="doc-title-cell">
                  <mat-icon class="doc-icon">insert_drive_file</mat-icon>
                  <span>{{ doc.title }}</span>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let doc">
                <mat-chip>{{ getTypeLabel(doc.type) }}</mat-chip>
              </td>
            </ng-container>

            <ng-container matColumnDef="version">
              <th mat-header-cell *matHeaderCellDef>Version</th>
              <td mat-cell *matCellDef="let doc">v{{ doc.version }}</td>
            </ng-container>

            <ng-container matColumnDef="size">
              <th mat-header-cell *matHeaderCellDef>Taille</th>
              <td mat-cell *matCellDef="let doc">{{ doc.sizeBytes | number }} o</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let doc">
                <button mat-icon-button matTooltip="Visualiser" (click)="previewDocument(doc)">
                  <mat-icon>visibility</mat-icon>
                </button>
                <button mat-icon-button matTooltip="Télécharger" (click)="downloadDocument(doc)">
                  <mat-icon>download</mat-icon>
                </button>
                <ng-container *ngIf="isEditor$ | async">
                  <button mat-icon-button matTooltip="Déplacer" (click)="moveDocument(doc)">
                    <mat-icon>drive_file_move</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Renommer" (click)="renameDocument(doc)">
                    <mat-icon>edit</mat-icon>
                  </button>
                  <button mat-icon-button matTooltip="Supprimer" color="warn" (click)="deleteDocument(doc)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </ng-container>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell empty-row" [attr.colspan]="displayedColumns.length">
                Aucun document dans ce dossier
              </td>
            </tr>
          </table>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .page-layout { display: flex; height: calc(100vh - 64px); overflow: hidden; }
    .folder-sidebar { width: 240px; min-width: 240px; overflow-y: auto; }
    .content-area { flex: 1; overflow-y: auto; padding: 24px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .header-left { display: flex; align-items: center; gap: 12px; }
    .header-left h1 { margin: 0; font-size: 24px; font-weight: 500; }
    .header-left a { display: flex; align-items: center; gap: 6px; }
    .folder-breadcrumb { display: flex; align-items: center; gap: 4px; color: #1565C0; font-size: 13px; }
    .filter-card { margin-bottom: 16px; padding: 16px; }
    .filter-card mat-form-field { width: 280px; }
    .doc-table { width: 100%; }
    .doc-title-cell { display: flex; align-items: center; gap: 8px; }
    .doc-icon { color: #1565C0; font-size: 20px; width: 20px; height: 20px; }
    .empty-row { text-align: center; padding: 32px; color: #9e9e9e; }
    .loading-center { display: flex; justify-content: center; padding: 48px; }
  `]
})
export class DocumentListPage implements OnInit {
  private store   = inject(Store);
  private dialog  = inject(MatDialog);
  private service = inject(DocumentService);

  documents$ = this.store.select(DocumentSelectors.all);
  loading$   = this.store.select(DocumentSelectors.loading);
  isEditor$  = this.store.select(AuthSelectors.isEditor);

  typeLabels       = DOCUMENT_TYPE_LABELS;
  displayedColumns = ['title', 'type', 'version', 'size', 'actions'];

  private allDocuments = toSignal(this.store.select(DocumentSelectors.all), { initialValue: [] as Document[] });
  private allFolders   = toSignal(this.store.select(FolderSelectors.all),   { initialValue: [] as any[] });
  selectedType     = signal('');
  currentFolderId  = signal<string | null>(null);

  currentFolderName = computed(() => {
    const id = this.currentFolderId();
    if (!id) return null;
    return this.allFolders().find(f => f.id === id)?.name ?? null;
  });

  get filteredDocuments(): Document[] {
    const docs = this.allDocuments();
    const type = this.selectedType();
    return type ? docs.filter(d => d.type === type) : docs;
  }

  ngOnInit() {
    this.store.dispatch(DocumentActions.loadDocuments({ folderId: null }));
    this.store.dispatch(EstablishmentActions.load());
  }

  onFolderSelected(folderId: string | null) {
    this.currentFolderId.set(folderId);
    this.store.dispatch(DocumentActions.loadDocuments({ folderId }));
  }

  getTypeLabel(type: string): string {
    return this.typeLabels[type as DocumentType] ?? type;
  }

  previewDocument(doc: Document) {
    this.dialog.open(PreviewDialogComponent, {
      width: '860px',
      maxWidth: '95vw',
      data: { document: doc }
    });
  }

  downloadDocument(doc: Document) {
    this.service.download(doc.id).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = doc.originalFilename ?? doc.title;
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  moveDocument(doc: Document) {
    const ref = this.dialog.open(MoveDialogComponent, {
      width: '400px',
      data: { folders: this.allFolders(), currentFolderId: doc.folderId }
    });
    ref.afterClosed().subscribe(result => {
      if (result !== undefined) {
        this.store.dispatch(DocumentActions.moveDocument({ documentId: doc.id, folderId: result }));
      }
    });
  }

  renameDocument(doc: Document) {
    const ref = this.dialog.open(RenameDialogComponent, {
      width: '420px',
      data: { currentTitle: doc.title, dialogTitle: 'Renommer le document', label: 'Nouveau titre', confirmLabel: 'Renommer' }
    });
    ref.afterClosed().subscribe(newTitle => {
      if (newTitle) {
        this.store.dispatch(DocumentActions.renameDocument({ documentId: doc.id, title: newTitle }));
      }
    });
  }

  deleteDocument(doc: Document) {
    const confirmed = window.confirm(`Supprimer « ${doc.title} » ? Cette action est irréversible.`);
    if (confirmed) {
      this.store.dispatch(DocumentActions.deleteDocument({ documentId: doc.id }));
    }
  }
}
