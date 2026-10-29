import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UserSummaryDto,
  CreateUserRequest,
  UpdateUserRequest,
  UpdateProfileRequest,
  ChangePasswordRequest,
  RoleDto,
  PermissionCatalogItemDto,
  CreateRoleRequest,
  UpdateRoleRequest,
  ResetPasswordResponse,
  AssignBranchesRequest,
} from '../models/user.models';
import { UserDto } from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(): Observable<UserSummaryDto[]> {
    return this.http.get<UserSummaryDto[]>('/api/v1/users');
  }

  getUserById(id: string): Observable<UserSummaryDto> {
    return this.http.get<UserSummaryDto>(`/api/v1/users/${id}`);
  }

  createUser(request: CreateUserRequest): Observable<UserSummaryDto> {
    return this.http.post<UserSummaryDto>('/api/v1/users', request);
  }

  updateUser(id: string, request: UpdateUserRequest): Observable<UserSummaryDto> {
    return this.http.put<UserSummaryDto>(`/api/v1/users/${id}`, request);
  }

  toggleUserStatus(id: string, isActive: boolean): Observable<UserSummaryDto> {
    return this.http.patch<UserSummaryDto>(`/api/v1/users/${id}/status`, null, {
      params: { isActive: isActive.toString() },
    });
  }

  resetUserPassword(userId: string, temporaryPassword: string): Observable<ResetPasswordResponse> {
    return this.http.post<ResetPasswordResponse>(`/api/v1/users/${userId}/reset-password`, {
      temporaryPassword,
    });
  }

  assignUserBranches(userId: string, request: AssignBranchesRequest): Observable<UserSummaryDto> {
    return this.http.post<UserSummaryDto>(`/api/v1/users/${userId}/branches`, request);
  }

  revokeUserSessions(userId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/users/${userId}/revoke-sessions`, {});
  }

  getRoles(): Observable<RoleDto[]> {
    return this.http.get<RoleDto[]>('/api/v1/roles');
  }

  getPermissionsCatalog(): Observable<PermissionCatalogItemDto[]> {
    return this.http.get<PermissionCatalogItemDto[]>('/api/v1/permissions/catalog');
  }

  createRole(request: CreateRoleRequest): Observable<RoleDto> {
    return this.http.post<RoleDto>('/api/v1/roles', request);
  }

  updateRole(id: string, request: UpdateRoleRequest): Observable<RoleDto> {
    return this.http.put<RoleDto>(`/api/v1/roles/${id}`, request);
  }

  deleteRole(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/roles/${id}`);
  }

  getProfile(): Observable<UserDto> {
    return this.http.get<UserDto>('/api/v1/profile');
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserDto> {
    return this.http.put<UserDto>('/api/v1/profile', request);
  }

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>('/api/v1/profile/password', request);
  }
}
