import { createAction, createReducer, createSelector, props, on } from '@ngrx/store';
import { inject } from '@angular/core';
import { Actions, ofType, createEffect } from '@ngrx/effects';
import { switchMap, map, catchError, of } from 'rxjs';
import { Document, DocumentsState } from '../../core/models/models';
import { DocumentService } from '../../core/services/services';

// ── Actions ───────────────────────────────────────────────────────────────────

export const DocumentActions = {
  loadDocuments:         createAction('[Documents] Load', props<{ folderId?: string | null; all?: boolean }>()),
  loadDocumentsSuccess:  createAction('[Documents] Load Success',  props<{ documents: Document[] }>()),
  loadDocumentsFailure:  createAction('[Documents] Load Failure',  props<{ error: string }>()),

  uploadDocument:        createAction('[Documents] Upload',        props<{ file: File; title: string; docType: string }>()),
  uploadDocumentSuccess: createAction('[Documents] Upload Success', props<{ document: Document }>()),
  uploadDocumentFailure: createAction('[Documents] Upload Failure', props<{ error: string }>()),

  selectDocument:        createAction('[Documents] Select',        props<{ document: Document | null }>()),

  moveDocument:          createAction('[Documents] Move',          props<{ documentId: string; folderId: string | null }>()),
  moveDocumentSuccess:   createAction('[Documents] Move Success',  props<{ document: Document }>()),
  moveDocumentFailure:   createAction('[Documents] Move Failure',  props<{ error: string }>()),

  renameDocument:        createAction('[Documents] Rename',        props<{ documentId: string; title: string }>()),
  renameDocumentSuccess: createAction('[Documents] Rename Success', props<{ document: Document }>()),
  renameDocumentFailure: createAction('[Documents] Rename Failure', props<{ error: string }>()),

  deleteDocument:        createAction('[Documents] Delete',        props<{ documentId: string }>()),
  deleteDocumentSuccess: createAction('[Documents] Delete Success', props<{ documentId: string }>()),
  deleteDocumentFailure: createAction('[Documents] Delete Failure', props<{ error: string }>()),
};

// ── Reducer ───────────────────────────────────────────────────────────────────

const initialState: DocumentsState = {
  items: [], selected: null, loading: false, uploading: false, error: null
};

export const documentsReducer = createReducer(
  initialState,
  on(DocumentActions.loadDocuments,        s => ({ ...s, loading: true, error: null })),
  on(DocumentActions.loadDocumentsSuccess, (s, { documents }) => ({ ...s, items: documents, loading: false })),
  on(DocumentActions.loadDocumentsFailure, (s, { error }) => ({ ...s, loading: false, error })),

  on(DocumentActions.uploadDocument,        s => ({ ...s, uploading: true, error: null })),
  on(DocumentActions.uploadDocumentSuccess, (s, { document }) => ({
    ...s, items: [document, ...s.items], uploading: false
  })),
  on(DocumentActions.uploadDocumentFailure, (s, { error }) => ({ ...s, uploading: false, error })),

  on(DocumentActions.selectDocument, (s, { document }) => ({ ...s, selected: document })),

  on(DocumentActions.moveDocumentSuccess, (s, { document }) => ({
    ...s, items: s.items.filter(d => d.id !== document.id)
  })),

  on(DocumentActions.renameDocument,        s => ({ ...s, error: null })),
  on(DocumentActions.renameDocumentSuccess, (s, { document }) => ({
    ...s, items: s.items.map(d => d.id === document.id ? document : d)
  })),
  on(DocumentActions.renameDocumentFailure, (s, { error }) => ({ ...s, error })),

  on(DocumentActions.deleteDocument,        s => ({ ...s, error: null })),
  on(DocumentActions.deleteDocumentSuccess, (s, { documentId }) => ({
    ...s, items: s.items.filter(d => d.id !== documentId)
  })),
  on(DocumentActions.deleteDocumentFailure, (s, { error }) => ({ ...s, error })),
);

// ── Selectors ─────────────────────────────────────────────────────────────────

const selectDocumentsState = (state: any) => state.documents as DocumentsState;

export const DocumentSelectors = {
  all:       createSelector(selectDocumentsState, s => s.items),
  selected:  createSelector(selectDocumentsState, s => s.selected),
  loading:   createSelector(selectDocumentsState, s => s.loading),
  uploading: createSelector(selectDocumentsState, s => s.uploading),
  error:     createSelector(selectDocumentsState, s => s.error),
  byType:    createSelector(selectDocumentsState, s => {
    return s.items.reduce((acc, doc) => {
      acc[doc.type] = (acc[doc.type] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
  }),
};

// ── Effects ───────────────────────────────────────────────────────────────────

export class DocumentEffects {
  private actions$ = inject(Actions);
  private service  = inject(DocumentService);

  loadDocuments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.loadDocuments),
      switchMap(({ folderId, all }) => this.service.list(folderId, all).pipe(
        map(documents => DocumentActions.loadDocumentsSuccess({ documents })),
        catchError(err  => of(DocumentActions.loadDocumentsFailure({ error: err.message })))
      ))
    )
  );

  uploadDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.uploadDocument),
      switchMap(({ file, title, docType }) =>
        this.service.upload(file, title, docType).pipe(
          map(document => DocumentActions.uploadDocumentSuccess({ document })),
          catchError(err => of(DocumentActions.uploadDocumentFailure({ error: err.message })))
        )
      )
    )
  );

  moveDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.moveDocument),
      switchMap(({ documentId, folderId }) =>
        this.service.move(documentId, folderId).pipe(
          map(document => DocumentActions.moveDocumentSuccess({ document })),
          catchError(err  => of(DocumentActions.moveDocumentFailure({ error: err.message })))
        )
      )
    )
  );

  renameDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.renameDocument),
      switchMap(({ documentId, title }) =>
        this.service.rename(documentId, title).pipe(
          map(document => DocumentActions.renameDocumentSuccess({ document })),
          catchError(err  => of(DocumentActions.renameDocumentFailure({ error: err.message })))
        )
      )
    )
  );

  deleteDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(DocumentActions.deleteDocument),
      switchMap(({ documentId }) =>
        this.service.delete(documentId).pipe(
          map(() => DocumentActions.deleteDocumentSuccess({ documentId })),
          catchError(err => of(DocumentActions.deleteDocumentFailure({ error: err.message })))
        )
      )
    )
  );
}
