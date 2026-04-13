import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';
import { AuthResponse } from '../models/models';
import { DEFAULT_USER } from '../../testing/test-helpers';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenStorageService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('storeTokens / getAccessToken / getRefreshToken', () => {
    it('should store and retrieve the access token', () => {
      const response: AuthResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
        user: DEFAULT_USER,
      };

      service.storeTokens(response);

      expect(service.getAccessToken()).toBe('access-123');
    });

    it('should store and retrieve the refresh token', () => {
      const response: AuthResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
        user: DEFAULT_USER,
      };

      service.storeTokens(response);

      expect(service.getRefreshToken()).toBe('refresh-456');
    });

    it('should overwrite previous tokens when storing new ones', () => {
      const response1: AuthResponse = {
        accessToken: 'old-access',
        refreshToken: 'old-refresh',
        user: DEFAULT_USER,
      };
      const response2: AuthResponse = {
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
        user: DEFAULT_USER,
      };

      service.storeTokens(response1);
      service.storeTokens(response2);

      expect(service.getAccessToken()).toBe('new-access');
      expect(service.getRefreshToken()).toBe('new-refresh');
    });
  });

  describe('getAccessToken / getRefreshToken when empty', () => {
    it('should return null when no access token is stored', () => {
      expect(service.getAccessToken()).toBeNull();
    });

    it('should return null when no refresh token is stored', () => {
      expect(service.getRefreshToken()).toBeNull();
    });
  });

  describe('clearTokens', () => {
    it('should remove both tokens from localStorage', () => {
      const response: AuthResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
        user: DEFAULT_USER,
      };

      service.storeTokens(response);
      service.clearTokens();

      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
    });

    it('should not throw when clearing already empty storage', () => {
      expect(() => service.clearTokens()).not.toThrow();
    });
  });

  describe('test isolation', () => {
    it('should not see tokens from a previous test (isolation check 1)', () => {
      const response: AuthResponse = {
        accessToken: 'isolation-test',
        refreshToken: 'isolation-refresh',
        user: DEFAULT_USER,
      };
      service.storeTokens(response);
      expect(service.getAccessToken()).toBe('isolation-test');
      // afterEach clears localStorage — isolation check 2 verifies this
    });

    it('should not see tokens from a previous test (isolation check 2)', () => {
      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
    });
  });
});
