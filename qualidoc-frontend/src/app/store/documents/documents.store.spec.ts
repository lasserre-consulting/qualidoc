import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError } from 'rxjs';

import { DocumentActions, documentsReducer, DocumentEffects, DocumentSelectors } from './documents.store';
import { DocumentService } from '../../core/services/services';
import { Document, DocumentsState } from '../../core/models/models';

const mockDoc: Document = {
  id: 'doc-001',
  title: 'Protocole Hygi\u00e8ne',
  type: 'PROTOCOL',
  typeLabel: 'Protocole',
  uploaderId: 'user-001',
  establishmentId: 'estab-001',
  folderId: null,
  originalFilename: 'protocole.pdf',
  mimeType: 'application/pdf',
  sizeBytes: 12345,
  version: 1,
  createdAt: '2026-01-15T10:00:00Z',
};

// ══════════════════════════════════════════════════════════════════════════════
// REDUCER
// ══════════════════════════════════════════════════════════════════════════════

describe('documentsReducer', () => {
  const initial: DocumentsState = {
    items: [], selected: null, loading: false, uploading: false, error: null,
  };

  it('should return initial state by default', () => {
    const result = documentsReducer(undefined, { type: 'NOOP' });
    expect(result).toEqual(initial);
  });

  it('should set loading on loadDocuments', () => {
    const result = documentsReducer(initial, DocumentActions.loadDocuments({ folderId: null }));
    expect(result.loading).toBeTrue();
    expect(result.error).toBeNull();
  });

  it('should set items on loadDocumentsSuccess', () => {
    const result = documentsReducer(initial, DocumentActions.loadDocumentsSuccess({ documents: [mockDoc] }));
    expect(result.items).toEqual([mockDoc]);
    expect(result.loading).toBeFalse();
  });

  it('should set error on loadDocumentsFailure', () => {
    const result = documentsReducer(initial, DocumentActions.loadDocumentsFailure({ error: 'fail' }));
    expect(result.error).toBe('fail');
    expect(result.loading).toBeFalse();
  });

  it('should set uploading on uploadDocument', () => {
    const result = documentsReducer(initial, DocumentActions.uploadDocument({
      file: new File([], 'f.pdf'), title: 'Test', docType: 'PROTOCOL',
    }));
    expect(result.uploading).toBeTrue();
  });

  it('should prepend document on uploadDocumentSuccess', () => {
    const stateWithDocs = { ...initial, items: [mockDoc] };
    const newDoc = { ...mockDoc, id: 'doc-002', title: 'New' };
    const result = documentsReducer(stateWithDocs, DocumentActions.uploadDocumentSuccess({ document: newDoc }));
    expect(result.items[0].id).toBe('doc-002');
    expect(result.items.length).toBe(2);
    expect(result.uploading).toBeFalse();
  });

  it('should remove document on moveDocumentSuccess', () => {
    const stateWithDocs = { ...initial, items: [mockDoc] };
    const movedDoc = { ...mockDoc, folderId: 'folder-x' };
    const result = documentsReducer(stateWithDocs, DocumentActions.moveDocumentSuccess({ document: movedDoc }));
    expect(result.items.length).toBe(0);
  });

  it('should update document on renameDocumentSuccess', () => {
    const stateWithDocs = { ...initial, items: [mockDoc] };
    const renamed = { ...mockDoc, title: 'Renamed' };
    const result = documentsReducer(stateWithDocs, DocumentActions.renameDocumentSuccess({ document: renamed }));
    expect(result.items[0].title).toBe('Renamed');
  });

  it('should remove document on deleteDocumentSuccess', () => {
    const stateWithDocs = { ...initial, items: [mockDoc] };
    const result = documentsReducer(stateWithDocs, DocumentActions.deleteDocumentSuccess({ documentId: 'doc-001' }));
    expect(result.items.length).toBe(0);
  });

  it('should set selected on selectDocument', () => {
    const result = documentsReducer(initial, DocumentActions.selectDocument({ document: mockDoc }));
    expect(result.selected).toEqual(mockDoc);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// SELECTORS
// ══════════════════════════════════════════════════════════════════════════════

describe('DocumentSelectors', () => {
  it('byType should group documents by type', () => {
    const state: DocumentsState = {
      items: [
        mockDoc,
        { ...mockDoc, id: 'doc-002', type: 'PROTOCOL' },
        { ...mockDoc, id: 'doc-003', type: 'FORM' },
      ],
      selected: null, loading: false, uploading: false, error: null,
    };

    const result = DocumentSelectors.byType.projector(state);
    expect(result['PROTOCOL']).toBe(2);
    expect(result['FORM']).toBe(1);
  });
});

// ══════════════════════════════════════════════════════════════════════════════
// EFFECTS
// ══════════════════════════════════════════════════════════════════════════════

describe('DocumentEffects', () => {
  let effects: DocumentEffects;
  let actions$: Observable<any>;
  let documentService: jasmine.SpyObj<DocumentService>;

  beforeEach(() => {
    documentService = jasmine.createSpyObj('DocumentService', [
      'list', 'upload', 'move', 'rename', 'delete',
    ]);

    TestBed.configureTestingModule({
      providers: [
        DocumentEffects,
        provideMockActions(() => actions$),
        { provide: DocumentService, useValue: documentService },
      ],
    });

    effects = TestBed.inject(DocumentEffects);
  });

  describe('loadDocuments$', () => {
    it('should dispatch loadDocumentsSuccess on success', (done) => {
      documentService.list.and.returnValue(of([mockDoc]));
      actions$ = of(DocumentActions.loadDocuments({ folderId: null }));

      effects.loadDocuments$.subscribe(action => {
        expect(action).toEqual(DocumentActions.loadDocumentsSuccess({ documents: [mockDoc] }));
        done();
      });
    });

    it('should dispatch loadDocumentsFailure on error', (done) => {
      documentService.list.and.returnValue(throwError(() => new Error('Network error')));
      actions$ = of(DocumentActions.loadDocuments({ folderId: null }));

      effects.loadDocuments$.subscribe(action => {
        expect(action).toEqual(DocumentActions.loadDocumentsFailure({ error: 'Network error' }));
        done();
      });
    });
  });

  describe('deleteDocument$', () => {
    it('should dispatch deleteDocumentSuccess on success', (done) => {
      documentService.delete.and.returnValue(of(undefined as any));
      actions$ = of(DocumentActions.deleteDocument({ documentId: 'doc-001' }));

      effects.deleteDocument$.subscribe(action => {
        expect(action).toEqual(DocumentActions.deleteDocumentSuccess({ documentId: 'doc-001' }));
        done();
      });
    });
  });

  describe('renameDocument$', () => {
    it('should dispatch renameDocumentSuccess on success', (done) => {
      const renamed = { ...mockDoc, title: 'Renamed' };
      documentService.rename.and.returnValue(of(renamed));
      actions$ = of(DocumentActions.renameDocument({ documentId: 'doc-001', title: 'Renamed' }));

      effects.renameDocument$.subscribe(action => {
        expect(action).toEqual(DocumentActions.renameDocumentSuccess({ document: renamed }));
        done();
      });
    });
  });

  describe('moveDocument$', () => {
    it('should dispatch moveDocumentSuccess on success', (done) => {
      const moved = { ...mockDoc, folderId: 'folder-x' };
      documentService.move.and.returnValue(of(moved));
      actions$ = of(DocumentActions.moveDocument({ documentId: 'doc-001', folderId: 'folder-x' }));

      effects.moveDocument$.subscribe(action => {
        expect(action).toEqual(DocumentActions.moveDocumentSuccess({ document: moved }));
        done();
      });
    });
  });
});
