export interface UserDto {
  id: string;
  username: string;
  email: string | null;
  role: string;
  tenantId: string;
  branchId: string | null;
}

export interface LoginRequest {
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
