import { Component, OnInit, inject, signal, computed, output } from '@angular/core';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { toSignal } from '@angular/core/rxjs-interop';
import { FolderActions, FolderSelectors } from '../../../store/folders/folders.store';
import { AuthSelectors } from '../../../store/shared.store';
import { Folder } from '../../../core/models/models';
import { RenameDialogComponent } from './rename-dialog.component';

interface FolderNode {
  folder: Folder;
  depth: number;
}

@Component({
  selector: 'app-folder-tree',
  standalone: true,
  imports: [NgFor, NgIf, NgClass, MatIconModule, MatButtonModule, MatTooltipModule],
  template: `
    <div class="tree-container">
      <div class="tree-header">
        <span class="tree-title">Dossiers</span>
        <button mat-icon-button matTooltip="Nouveau dossier à la racine"
                *ngIf="isEditor()"
                (click)="createFolder(null)">
          <mat-icon>create_new_folder</mat-icon>
        </button>
      </div>

      <!-- Racine -->
      <div class="folder-row"
           [class.selected]="selectedFolderId() === null"
           (click)="selectFolder(null)">
        <mat-icon class="folder-icon">home</mat-icon>
        <span class="folder-name">Racine</span>
      </div>

      <!-- Dossiers -->
      <ng-container *ngFor="let node of visibleNodes()">
        <div class="folder-row"
             [class.selected]="selectedFolderId() === node.folder.id"
             [style.padding-left.px]="16 + node.depth * 20"
             (click)="selectFolder(node.folder.id)">

          <button mat-icon-button class="expand-btn"
                  (click)="$event.stopPropagation(); toggleExpand(node.folder.id)"
                  *ngIf="hasChildren(node.folder.id)">
            <mat-icon>{{ isExpanded(node.folder.id) ? 'expand_more' : 'chevron_right' }}</mat-icon>
          </button>
          <span *ngIf="!hasChildren(node.folder.id)" class="expand-spacer"></span>

          <mat-icon class="folder-icon">folder</mat-icon>
          <span class="folder-name">{{ node.folder.name }}</span>

          <div class="folder-actions" *ngIf="isEditor()" (click)="$event.stopPropagation()">
            <button mat-icon-button matTooltip="Nouveau sous-dossier"
                    (click)="createFolder(node.folder.id)">
              <mat-icon>add</mat-icon>
            </button>
            <button mat-icon-button matTooltip="Renommer"
                    (click)="renameFolder(node.folder)">
              <mat-icon>edit</mat-icon>
            </button>
            <button mat-icon-button matTooltip="Supprimer (contenu inclus)" color="warn"
                    (click)="deleteFolder(node.folder)">
              <mat-icon>delete</mat-icon>
            </button>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .tree-container { height: 100%; border-right: 1px solid #e0e0e0; background: #fafafa; }
    .tree-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 8px 4px 16px; border-bottom: 1px solid #e0e0e0; }
    .tree-title { font-weight: 500; font-size: 14px; color: #424242; }
    .folder-row { display: flex; align-items: center; padding: 4px 8px 4px 16px; cursor: pointer; min-height: 36px; gap: 4px; }
    .folder-row:hover { background: #eeeeee; }
    .folder-row.selected { background: #e3f2fd; }
    .folder-icon { font-size: 18px; width: 18px; height: 18px; color: #1565C0; flex-shrink: 0; }
    .folder-name { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .expand-btn { height: 30px; line-height: 20px; flex-shrink: 0; }
    .expand-btn mat-icon { font-size: 16px; }
    .expand-spacer { width: 20px; flex-shrink: 0; }
    .folder-actions { display: none; align-items: center; }
    .folder-actions button { height: 30px; line-height: 24px; }
    .folder-actions mat-icon { font-size: 14px; }
    .folder-row:hover .folder-actions { display: flex; }
  `]
})
export class FolderTreeComponent implements OnInit {
  private store  = inject(Store);
  private dialog = inject(MatDialog);

  folderSelected = output<string | null>();

  private allFolders  = toSignal(this.store.select(FolderSelectors.all), { initialValue: [] as Folder[] });
  isEditor            = toSignal(this.store.select(AuthSelectors.isEditor), { initialValue: false });
  selectedFolderId    = signal<string | null>(null);
  private expandedIds = signal<Set<string>>(new Set());

  hasChildren(folderId: string): boolean {
    return this.allFolders().some(f => f.parentId === folderId);
  }

  isExpanded(folderId: string): boolean {
    return this.expandedIds().has(folderId);
  }

  toggleExpand(folderId: string) {
    const set = new Set(this.expandedIds());
    set.has(folderId) ? set.delete(folderId) : set.add(folderId);
    this.expandedIds.set(set);
  }

  visibleNodes = computed<FolderNode[]>(() => {
    const folders = this.allFolders();
    const expanded = this.expandedIds();
    const result: FolderNode[] = [];

    const addChildren = (parentId: string | null, depth: number) => {
      folders
        .filter(f => f.parentId === parentId)
        .sort((a, b) => a.name.localeCompare(b.name))
        .forEach(folder => {
          result.push({ folder, depth });
          if (expanded.has(folder.id)) {
            addChildren(folder.id, depth + 1);
          }
        });
    };

    addChildren(null, 0);
    return result;
  });

  ngOnInit() {
    this.store.dispatch(FolderActions.load());
  }

  selectFolder(folderId: string | null) {
    this.selectedFolderId.set(folderId);
    this.folderSelected.emit(folderId);
  }

  createFolder(parentId: string | null) {
    const ref = this.dialog.open(RenameDialogComponent, {
      width: '380px',
      data: { currentTitle: '', dialogTitle: 'Nouveau dossier', label: 'Nom du dossier', confirmLabel: 'Créer' }
    });
    ref.afterClosed().subscribe(name => {
      if (name) {
        this.store.dispatch(FolderActions.create({ name, parentId }));
        if (parentId) {
          const set = new Set(this.expandedIds());
          set.add(parentId);
          this.expandedIds.set(set);
        }
      }
    });
  }

  renameFolder(folder: Folder) {
    const ref = this.dialog.open(RenameDialogComponent, {
      width: '380px',
      data: { currentTitle: folder.name }
    });
    ref.afterClosed().subscribe(name => {
      if (name) this.store.dispatch(FolderActions.rename({ folderId: folder.id, name }));
    });
  }

  deleteFolder(folder: Folder) {
    const confirmed = window.confirm(
      `Supprimer « ${folder.name} » ?\n\nTous les sous-dossiers et documents contenus seront supprimés définitivement.`
    );
    if (confirmed) {
      if (this.selectedFolderId() === folder.id) this.selectFolder(null);
      this.store.dispatch(FolderActions.delete({ folderId: folder.id }));
    }
  }
}
