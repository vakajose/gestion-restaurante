export interface UserDto {
  id: string;
  username: string;
  fullName?: string | null;
  email: string | null;
  phoneNumber?: string | null;
  role: string;
  tenantId: string;
  branchId: string | null;
  tenantSlug?: string | null;
  permissions?: string[];
  authorizedBranchIds?: string[];
  mustChangePassword?: boolean;
  roleValidUntil?: string | null;
}

export interface LoginRequest {
  tenantIdentifier?: string;
  login: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  accessToken?: string;
  refreshToken?: string | null;
  expiresInSeconds?: number;
  tokenType: string;
  user: UserDto;
}

export interface RefreshResponse {
  token: string;
  accessToken?: string;
  refreshToken?: string | null;
  expiresInSeconds?: number;
  tokenType?: string;
}

export interface ActiveSessionDto {
  id: string;
  deviceInfo?: string | null;
  ipAddress?: string | null;
  createdAt: string;
  expiresAt: string;
  isCurrent: boolean;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  properties?: Record<string, unknown>;
}
