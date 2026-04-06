import { Component, Inject, signal, computed } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Folder } from '../../../core/models/models';

export interface MoveDialogData {
  folders: Folder[];
  currentFolderId: string | null;
}

interface FolderNode { folder: Folder; depth: number; }

@Component({
  selector: 'app-move-dialog',
  standalone: true,
  imports: [NgFor, NgIf, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Déplacer vers…</h2>
    <mat-dialog-content>
      <div class="folder-list">

        <!-- Racine -->
        <div class="folder-item"
             [class.selected]="selected() === null"
             (click)="selected.set(null)">
          <mat-icon>home</mat-icon>
          <span>Racine</span>
          <mat-icon class="check" *ngIf="selected() === null">check</mat-icon>
        </div>

        <!-- Dossiers -->
        <div *ngFor="let node of visibleNodes"
             class="folder-item"
             [class.selected]="selected() === node.folder.id"
             [style.padding-left.px]="16 + node.depth * 20"
             (click)="selected.set(node.folder.id)">
          <mat-icon>folder</mat-icon>
          <span>{{ node.folder.name }}</span>
          <mat-icon class="check" *ngIf="selected() === node.folder.id">check</mat-icon>
        </div>

      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Annuler</button>
      <button mat-raised-button color="primary" (click)="confirm()">Déplacer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .folder-list { max-height: 360px; overflow-y: auto; min-width: 320px; }
    .folder-item { display: flex; align-items: center; gap: 8px; padding: 8px 16px; cursor: pointer; border-radius: 4px; }
    .folder-item:hover { background: #f5f5f5; }
    .folder-item.selected { background: #e3f2fd; }
    .folder-item mat-icon { font-size: 18px; width: 18px; height: 18px; color: #1565C0; flex-shrink: 0; }
    .folder-item span { flex: 1; font-size: 14px; }
    .check { color: #1565C0; }
  `]
})
export class MoveDialogComponent {
  selected = signal<string | null>(this.data.currentFolderId);

  visibleNodes: FolderNode[];

  constructor(
    public dialogRef: MatDialogRef<MoveDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MoveDialogData
  ) {
    this.visibleNodes = this.buildNodes();
  }

  private buildNodes(): FolderNode[] {
    const result: FolderNode[] = [];
    const add = (parentId: string | null, depth: number) => {
      this.data.folders
        .filter(f => f.parentId === parentId)
        .sort((a, b) => a.name.localeCompare(b.name))
        .forEach(folder => {
          result.push({ folder, depth });
          add(folder.id, depth + 1);
        });
    };
    add(null, 0);
    return result;
  }

  confirm() {
    this.dialogRef.close(this.selected());
  }
}
