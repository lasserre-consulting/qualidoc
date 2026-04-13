import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError } from 'rxjs';

import { FolderActions, foldersReducer, FolderEffects } from './folders.store';
import { FolderService } from '../../core/services/services';
import { Folder, FoldersState } from '../../core/models/models';

const mockFolder: Folder = {
  id: 'folder-001',
  name: 'Protocoles 2026',
  parentId: null,
  groupementId: 'grp-001',
  createdAt: '2026-01-10T08:00:00Z',
};

// ══════════════════════════════════════════════════════════════════════════════
// REDUCER
// ══════════════════════════════════════════════════════════════════════════════

describe('foldersReducer', () => {
  const initial: FoldersState = { items: [], loading: false, error: null };

  it('should return initial state by default', () => {
    const result = foldersReducer(undefined, { type: 'NOOP' });
    expect(result).toEqual(initial);
  });

  it('should set loading on load', () => {
    const result = foldersReducer(initial, FolderActions.load());
    expect(result.loading).toBeTrue();
    expect(result.error).toBeNull();
  });

  it('should set items on loadSuccess', () => {
    const result = foldersReducer(initial, FolderActions.loadSuccess({ folders: [mockFolder] }));
    expect(result.items).toEqual([mockFolder]);
    expect(result.loading).toBeFalse();
  });

  it('should set error on loadFailure', () => {
    const result = foldersReducer(initial, FolderActions.loadFailure({ error: 'fail' }));
    expect(result.error).toBe('fail');
    expect(result.loading).toBeFalse();
  });

  it('should add folder on createSuccess', () => {
    const result = foldersReducer(initial, FolderActions.createSuccess({ folder: mockFolder }));
    expect(result.items.length).toBe(1);
    expect(result.items[0]).toEqual(mockFolder);
  });

  it('should update folder on renameSuccess', () => {
    const stateWithFolders = { ...initial, items: [mockFolder] };
    const renamed = { ...mockFolder, name: 'Renamed' };
    const result = foldersReducer(stateWithFolders, FolderActions.renameSuccess({ folder: renamed }));
    expect(result.items[0].name).toBe('Renamed');
  });

  it('should remove folder and its children on deleteSuccess', () => {
    const child: Folder = { ...mockFolder, id: 'folder-002', parentId: 'folder-001' };
    const stateWithFolders = { ...initial, items: [mockFolder, child] };
    const result = foldersReducer(stateWithFolders, FolderActions.deleteSuccess({ folderId: 'folder-001' }));
    expect(result.items.length).toBe(0);
  });

  it('should not remove unrelated folders on deleteSuccess', () => {
    const other: Folder = { ...mockFolder, id: 'folder-099', parentId: null };
    const stateWithFolders = { ...initial, items: [mockFolder, other] };
    const result = foldersReducer(stateWithFolders, FolderActions.deleteSuccess({ folderId: 'folder-001' }));
    expect(result.items.length).toBe(1);
    expect(result.items[0].id).toBe('folder-099');
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// EFFECTS
// ══════════════════════════════════════════════════════════════════════════════

describe('FolderEffects', () => {
  let effects: FolderEffects;
  let actions$: Observable<any>;
  let folderService: jasmine.SpyObj<FolderService>;

  beforeEach(() => {
    folderService = jasmine.createSpyObj('FolderService', [
      'list', 'create', 'rename', 'delete',
    ]);

    TestBed.configureTestingModule({
      providers: [
        FolderEffects,
        provideMockActions(() => actions$),
        { provide: FolderService, useValue: folderService },
      ],
    });

    effects = TestBed.inject(FolderEffects);
  });

  describe('load$', () => {
    it('should dispatch loadSuccess on success', (done) => {
      folderService.list.and.returnValue(of([mockFolder]));
      actions$ = of(FolderActions.load());

      effects.load$.subscribe(action => {
        expect(action).toEqual(FolderActions.loadSuccess({ folders: [mockFolder] }));
        done();
      });
    });

    it('should dispatch loadFailure on error', (done) => {
      folderService.list.and.returnValue(throwError(() => new Error('Network error')));
      actions$ = of(FolderActions.load());

      effects.load$.subscribe(action => {
        expect(action).toEqual(FolderActions.loadFailure({ error: 'Network error' }));
        done();
      });
    });
  });

  describe('create$', () => {
    it('should dispatch createSuccess on success', (done) => {
      folderService.create.and.returnValue(of(mockFolder));
      actions$ = of(FolderActions.create({ name: 'Protocoles 2026' }));

      effects.create$.subscribe(action => {
        expect(action).toEqual(FolderActions.createSuccess({ folder: mockFolder }));
        done();
      });
    });
  });

  describe('rename$', () => {
    it('should dispatch renameSuccess on success', (done) => {
      const renamed = { ...mockFolder, name: 'Renamed' };
      folderService.rename.and.returnValue(of(renamed));
      actions$ = of(FolderActions.rename({ folderId: 'folder-001', name: 'Renamed' }));

      effects.rename$.subscribe(action => {
        expect(action).toEqual(FolderActions.renameSuccess({ folder: renamed }));
        done();
      });
    });
  });

  describe('delete$', () => {
    it('should dispatch deleteSuccess on success', (done) => {
      folderService.delete.and.returnValue(of(undefined as any));
      actions$ = of(FolderActions.delete({ folderId: 'folder-001' }));

      effects.delete$.subscribe(action => {
        expect(action).toEqual(FolderActions.deleteSuccess({ folderId: 'folder-001' }));
        done();
      });
    });
  });
});
