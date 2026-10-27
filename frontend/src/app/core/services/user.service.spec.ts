import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UserService } from './user.service';
import { CreateUserRequest, UpdateUserRequest, UserSummaryDto } from '../models/user.models';

describe('UserService', () => {
  let service: UserService;
  let httpTesting: HttpTestingController;

  const mockUsers: UserSummaryDto[] = [
    {
      id: 'u-1',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'admin',
      email: 'admin@restaurant.com',
      role: 'ADMIN_TENANT',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    },
    {
      id: 'u-2',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'cajero1',
      email: null,
      role: 'CASHIER',
      isActive: true,
      createdAt: '2026-10-25T11:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe obtener la lista de colaboradores vía GET /api/v1/users', () => {
    service.getUsers().subscribe((users) => {
      expect(users.length).toBe(2);
      expect(users[0].username).toBe('admin');
    });

    const req = httpTesting.expectOne('/api/v1/users');
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);
  });

  it('debe crear un nuevo colaborador vía POST /api/v1/users', () => {
    const newReq: CreateUserRequest = {
      username: 'cocinero1',
      email: 'cocina@restaurant.com',
      password: 'password123',
      role: 'KITCHEN',
      branchId: 'b-1',
    };

    service.createUser(newReq).subscribe((created) => {
      expect(created.username).toBe('cocinero1');
      expect(created.role).toBe('KITCHEN');
    });

    const req = httpTesting.expectOne('/api/v1/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newReq);
    req.flush({ ...newReq, id: 'u-3', tenantId: 't-1', isActive: true, createdAt: '2026-10-27T10:00:00Z' });
  });

  it('debe actualizar un colaborador vía PUT /api/v1/users/{id}', () => {
    const updateReq: UpdateUserRequest = {
      username: 'admin_edit',
      email: 'admin_edit@test.com',
      role: 'ADMIN_TENANT',
      branchId: 'b-1',
      isActive: true,
    };

    service.updateUser('u-1', updateReq).subscribe((updated) => {
      expect(updated.username).toBe('admin_edit');
    });

    const req = httpTesting.expectOne('/api/v1/users/u-1');
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockUsers[0], username: 'admin_edit', email: 'admin_edit@test.com' });
  });

  it('debe conmutar el estado del colaborador vía PATCH /api/v1/users/{id}/status', () => {
    service.toggleUserStatus('u-2', false).subscribe((res) => {
      expect(res.isActive).toBe(false);
    });

    const req = httpTesting.expectOne('/api/v1/users/u-2/status?isActive=false');
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...mockUsers[1], isActive: false });
  });

  it('debe cambiar la contraseña propia vía PUT /api/v1/profile/password', () => {
    service.changePassword({ currentPassword: 'old', newPassword: 'new' }).subscribe((res) => {
      expect(res.message).toBe('Contraseña actualizada exitosamente');
    });

    const req = httpTesting.expectOne('/api/v1/profile/password');
    expect(req.request.method).toBe('PUT');
    req.flush({ message: 'Contraseña actualizada exitosamente' });
  });
});
