export interface UserSummaryDto {
  id: string;
  tenantId: string;
  branchId?: string | null;
  username: string;
  fullName?: string | null;
  email?: string | null;
  phoneNumber?: string | null;
  role: string;
  isActive: boolean;
  mustChangePassword?: boolean;
  roleValidUntil?: string | null;
  authorizedBranchIds?: string[];
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  fullName?: string | null;
  email?: string;
  phoneNumber?: string | null;
  password: string;
  role: string;
  branchId?: string | null;
  authorizedBranchIds?: string[];
  roleValidUntil?: string | null;
}

export interface UpdateUserRequest {
  username: string;
  fullName?: string | null;
  email?: string;
  phoneNumber?: string | null;
  role: string;
  branchId?: string | null;
  authorizedBranchIds?: string[];
  roleValidUntil?: string | null;
  isActive?: boolean;
  newPassword?: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  phoneNumber?: string | null;
  email?: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdatePinRequest {
  pin: string;
}

export interface PermissionCatalogItemDto {
  code: string;
  name: string;
  description: string;
  module: string;
}

export interface RoleDto {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  description?: string | null;
  isSystem: boolean;
  permissions: string[];
}

export interface CreateRoleRequest {
  code?: string;
  name: string;
  description?: string;
  permissions: string[];
}

export interface UpdateRoleRequest {
  name: string;
  description?: string;
  permissions: string[];
}

export interface ResetPasswordRequest {
  temporaryPassword: string;
}

export interface ResetPasswordResponse {
  userId: string;
  mustChangePassword: boolean;
  resetAt: string;
}

export interface AssignBranchesRequest {
  defaultBranchId: string | null;
  authorizedBranchIds: string[];
}
