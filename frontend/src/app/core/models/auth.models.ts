export interface UserDto {
  id: string;
  username: string;
  email: string | null;
  role: string;
  tenantId: string;
  branchId: string | null;
  tenantSlug?: string | null;
  permissions?: string[];
  authorizedBranchIds?: string[];
}

export interface LoginRequest {
  tenantIdentifier?: string;
  login: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  user: UserDto;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  properties?: Record<string, unknown>;
}
