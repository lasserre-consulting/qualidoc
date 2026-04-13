// ── Dossier ───────────────────────────────────────────────────────────────────
export interface Folder {
  id: string;
  name: string;
  parentId: string | null;
  groupementId: string;
  createdAt: string;
}

// ── Établissement ─────────────────────────────────────────────────────────────
export interface Establishment {
  id: string;
  name: string;
  code: string;
  active: boolean;
}

// ── Utilisateur ───────────────────────────────────────────────────────────────
export type UserRole = 'READER' | 'EDITOR';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: UserRole;
  establishmentId: string;
}

// ── Auth ──────────────────────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  establishmentId: string;
  password: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  role?: UserRole;
  active?: boolean;
}

// ── Document ──────────────────────────────────────────────────────────────────
export type DocumentType = 'PROCEDURE' | 'PROTOCOL' | 'FORM' | 'AWARENESS_BOOKLET';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  PROCEDURE: 'Procédure',
  PROTOCOL: 'Protocole',
  FORM: 'Formulaire qualité',
  AWARENESS_BOOKLET: 'Livret de sensibilisation'
};

export interface Document {
  id: string;
  title: string;
  type: DocumentType;
  typeLabel: string;
  uploaderId: string;
  establishmentId: string;
  folderId: string | null;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  version: number;
  createdAt: string;
}

// ── Recherche ─────────────────────────────────────────────────────────────────
export interface SearchResult {
  documentId: string;
  title: string;
  type: string;
  establishmentId: string;
  snippet: string | null;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export interface DashboardStats {
  totalDocuments: number;
  documentsByType: Record<DocumentType, number>;
  recentDocuments: Document[];
  totalEstablishments: number;
}

// ── État NgRx ─────────────────────────────────────────────────────────────────
export interface AppState {
  auth: AuthState;
  documents: DocumentsState;
  establishments: EstablishmentsState;
  search: SearchState;
  dashboard: DashboardState;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

export interface DocumentsState {
  items: Document[];
  selected: Document | null;
  loading: boolean;
  uploading: boolean;
  error: string | null;
}

export interface FoldersState {
  items: Folder[];
  loading: boolean;
  error: string | null;
}

export interface EstablishmentsState {
  items: Establishment[];
  loading: boolean;
  error: string | null;
}

export interface SearchState {
  query: string;
  results: SearchResult[];
  loading: boolean;
  error: string | null;
}

export interface DashboardState {
  stats: DashboardStats | null;
  loading: boolean;
  error: string | null;
}
