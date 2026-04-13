import { User, UserRole, AuthResponse } from '../core/models/models';

/**
 * Build a mock JWT token with a given payload.
 * The signature is fake — tests never verify it; they only decode the payload.
 */
export function buildMockJwt(payload: Record<string, unknown>): string {
  const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = base64url(JSON.stringify(payload));
  const signature = 'fake-signature';
  return `${header}.${body}.${signature}`;
}

function base64url(str: string): string {
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// ── Default user ────────────────────────────────────────────────────────────

export const DEFAULT_USER: User = {
  id: 'user-001',
  email: 'test@example.com',
  firstName: 'Jean',
  lastName: 'Dupont',
  fullName: 'Jean Dupont',
  role: 'EDITOR',
  establishmentId: 'estab-001',
};

// ── Pre-built JWT payloads ──────────────────────────────────────────────────

function jwtPayload(overrides: Partial<{
  sub: string; email: string; firstName: string; lastName: string;
  role: UserRole; establishmentId: string; exp: number;
}> = {}): Record<string, unknown> {
  return {
    sub: 'user-001',
    email: 'test@example.com',
    firstName: 'Jean',
    lastName: 'Dupont',
    role: 'EDITOR',
    establishmentId: 'estab-001',
    // expires in 1 hour
    exp: Math.floor(Date.now() / 1000) + 3600,
    iat: Math.floor(Date.now() / 1000),
    ...overrides,
  };
}

export const EDITOR_JWT: string = buildMockJwt(jwtPayload({ role: 'EDITOR' }));
export const READER_JWT: string = buildMockJwt(jwtPayload({ role: 'READER' }));
export const EXPIRED_JWT: string = buildMockJwt(jwtPayload({
  exp: Math.floor(Date.now() / 1000) - 3600, // expired 1 hour ago
}));

// ── Auth response helpers ───────────────────────────────────────────────────

export function buildAuthResponse(accessToken: string = EDITOR_JWT): AuthResponse {
  return {
    accessToken,
    refreshToken: 'refresh-token-abc',
    user: DEFAULT_USER,
  };
}
