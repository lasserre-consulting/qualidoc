import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminUsersService } from './admin-users.service';
import { User, CreateUserRequest, UpdateUserRequest } from '../models/models';
import { DEFAULT_USER } from '../../testing/test-helpers';
import { environment } from '../../../environments/environment';

describe('AdminUsersService', () => {
  let service: AdminUsersService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/admin/users`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AdminUsersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ── listUsers ─────────────────────────────────────────────────────────────

  describe('listUsers', () => {
    it('should GET /admin/users and return an array of users', () => {
      const mockUsers: User[] = [DEFAULT_USER];

      service.listUsers().subscribe(users => {
        expect(users).toEqual(mockUsers);
        expect(users.length).toBe(1);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });

    it('should return an empty array when no users exist', () => {
      service.listUsers().subscribe(users => {
        expect(users).toEqual([]);
      });

      const req = httpMock.expectOne(baseUrl);
      req.flush([]);
    });
  });

  // ── createUser ────────────────────────────────────────────────────────────

  describe('createUser', () => {
    it('should POST to /admin/users with the correct body', () => {
      const createReq: CreateUserRequest = {
        email: 'new@example.com',
        firstName: 'Marie',
        lastName: 'Martin',
        role: 'READER',
        establishmentId: 'estab-002',
        password: 'tempPass123',
      };

      const createdUser: User = {
        id: 'user-002',
        email: 'new@example.com',
        firstName: 'Marie',
        lastName: 'Martin',
        fullName: 'Marie Martin',
        role: 'READER',
        establishmentId: 'estab-002',
      };

      service.createUser(createReq).subscribe(user => {
        expect(user).toEqual(createdUser);
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(createReq);
      req.flush(createdUser);
    });
  });

  // ── updateUser ────────────────────────────────────────────────────────────

  describe('updateUser', () => {
    it('should PATCH to /admin/users/:id with the update body', () => {
      const updateReq: UpdateUserRequest = {
        firstName: 'Jean-Pierre',
        role: 'EDITOR',
      };

      const updatedUser: User = {
        ...DEFAULT_USER,
        firstName: 'Jean-Pierre',
        fullName: 'Jean-Pierre Dupont',
      };

      service.updateUser('user-001', updateReq).subscribe(user => {
        expect(user).toEqual(updatedUser);
      });

      const req = httpMock.expectOne(`${baseUrl}/user-001`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual(updateReq);
      req.flush(updatedUser);
    });
  });

  // ── resetPassword ─────────────────────────────────────────────────────────

  describe('resetPassword', () => {
    it('should POST to /admin/users/:id/reset-password with new password', () => {
      service.resetPassword('user-001', 'newSecurePass').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/user-001/reset-password`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ newPassword: 'newSecurePass' });
      req.flush(null);
    });
  });

  // ── deleteUser ────────────────────────────────────────────────────────────

  describe('deleteUser', () => {
    it('should DELETE /admin/users/:id', () => {
      service.deleteUser('user-001').subscribe();

      const req = httpMock.expectOne(`${baseUrl}/user-001`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
