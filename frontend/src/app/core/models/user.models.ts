export interface UserSummaryDto {
  id: string;
  tenantId: string;
  branchId?: string | null;
  username: string;
  email?: string | null;
  role: string;
  isActive: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  email?: string;
  password: string;
  role: string;
  branchId?: string | null;
}

export interface UpdateUserRequest {
  username: string;
  email?: string;
  role: string;
  branchId?: string | null;
  isActive?: boolean;
  newPassword?: string;
}

export interface UpdateProfileRequest {
  email?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
