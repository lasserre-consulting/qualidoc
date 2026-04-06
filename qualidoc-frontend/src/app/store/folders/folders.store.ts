import { createAction, createReducer, createSelector, props, on } from '@ngrx/store';
import { inject } from '@angular/core';
import { Actions, ofType, createEffect } from '@ngrx/effects';
import { switchMap, map, catchError, of } from 'rxjs';
import { Folder, FoldersState } from '../../core/models/models';
import { FolderService } from '../../core/services/services';

// ── Actions ───────────────────────────────────────────────────────────────────

export const FolderActions = {
  load:          createAction('[Folders] Load'),
  loadSuccess:   createAction('[Folders] Load Success',   props<{ folders: Folder[] }>()),
  loadFailure:   createAction('[Folders] Load Failure',   props<{ error: string }>()),

  create:        createAction('[Folders] Create',         props<{ name: string; parentId?: string | null }>()),
  createSuccess: createAction('[Folders] Create Success', props<{ folder: Folder }>()),
  createFailure: createAction('[Folders] Create Failure', props<{ error: string }>()),

  rename:        createAction('[Folders] Rename',         props<{ folderId: string; name: string }>()),
  renameSuccess: createAction('[Folders] Rename Success', props<{ folder: Folder }>()),
  renameFailure: createAction('[Folders] Rename Failure', props<{ error: string }>()),

  delete:        createAction('[Folders] Delete',         props<{ folderId: string }>()),
  deleteSuccess: createAction('[Folders] Delete Success', props<{ folderId: string }>()),
  deleteFailure: createAction('[Folders] Delete Failure', props<{ error: string }>()),
};

// ── Reducer ───────────────────────────────────────────────────────────────────

const initialState: FoldersState = { items: [], loading: false, error: null };

export const foldersReducer = createReducer(
  initialState,
  on(FolderActions.load,          s => ({ ...s, loading: true, error: null })),
  on(FolderActions.loadSuccess,   (s, { folders }) => ({ ...s, items: folders, loading: false })),
  on(FolderActions.loadFailure,   (s, { error }) => ({ ...s, loading: false, error })),

  on(FolderActions.createSuccess, (s, { folder }) => ({ ...s, items: [...s.items, folder] })),
  on(FolderActions.renameSuccess, (s, { folder }) => ({
    ...s, items: s.items.map(f => f.id === folder.id ? folder : f)
  })),
  on(FolderActions.deleteSuccess, (s, { folderId }) => ({
    ...s, items: s.items.filter(f => f.id !== folderId && f.parentId !== folderId)
  })),
);

// ── Selectors ─────────────────────────────────────────────────────────────────

const selectState = (state: any) => state.folders as FoldersState;

export const FolderSelectors = {
  all:     createSelector(selectState, s => s.items),
  loading: createSelector(selectState, s => s.loading),
};

// ── Effects ───────────────────────────────────────────────────────────────────

export class FolderEffects {
  private actions$ = inject(Actions);
  private service  = inject(FolderService);

  load$ = createEffect(() =>
    this.actions$.pipe(
      ofType(FolderActions.load),
      switchMap(() => this.service.list().pipe(
        map(folders => FolderActions.loadSuccess({ folders })),
        catchError(err => of(FolderActions.loadFailure({ error: err.message })))
      ))
    )
  );

  create$ = createEffect(() =>
    this.actions$.pipe(
      ofType(FolderActions.create),
      switchMap(({ name, parentId }) => this.service.create(name, parentId).pipe(
        map(folder => FolderActions.createSuccess({ folder })),
        catchError(err => of(FolderActions.createFailure({ error: err.message })))
      ))
    )
  );

  rename$ = createEffect(() =>
    this.actions$.pipe(
      ofType(FolderActions.rename),
      switchMap(({ folderId, name }) => this.service.rename(folderId, name).pipe(
        map(folder => FolderActions.renameSuccess({ folder })),
        catchError(err => of(FolderActions.renameFailure({ error: err.message })))
      ))
    )
  );

  delete$ = createEffect(() =>
    this.actions$.pipe(
      ofType(FolderActions.delete),
      switchMap(({ folderId }) => this.service.delete(folderId).pipe(
        map(() => FolderActions.deleteSuccess({ folderId })),
        catchError(err => of(FolderActions.deleteFailure({ error: err.message })))
      ))
    )
  );
}
