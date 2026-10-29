import { UserDto } from './auth.models';

export interface TenantDto {
  id: string;
  legalName: string;
  nitOrTaxId: string;
  slug: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BranchDto {
  id: string;
  tenantId: string;
  name: string;
  address?: string | null;
  phone?: string | null;
  timezone: string;
  isActive: boolean;
  createdAt: string;
}

export interface UpdateCompanyRequest {
  legalName: string;
  nitOrTaxId: string;
  slug: string;
}

export interface CreateBranchRequest {
  name: string;
  address?: string;
  phone?: string;
  timezone?: string;
}

export interface UpdateBranchRequest {
  name: string;
  address?: string;
  phone?: string;
  timezone?: string;
  isActive?: boolean;
}

export interface OnboardingCompanyDto {
  legalName: string;
  slug: string;
  nitOrTaxId?: string | null;
  logoUrl?: string | null;
}

export interface OnboardingBranchDto {
  name: string;
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  phone?: string | null;
  timezone: string;
  currency: string;
}

export interface OnboardingAdminDto {
  fullName: string;
  username: string;
  email?: string | null;
  password: string;
}

export type BusinessTemplate = 'FAST_FOOD' | 'TRADITIONAL' | 'CAFE' | 'BAR' | 'EMPTY';

export interface OnboardingRequest {
  company: OnboardingCompanyDto;
  branch: OnboardingBranchDto;
  businessTemplate: BusinessTemplate;
  includeDemoCatalog: boolean;
  adminUser: OnboardingAdminDto;
}

export interface OnboardingResultDto {
  tenantId: string;
  branchId: string;
  adminUserId: string;
  slug: string;
  accessToken: string;
  token: string;
  refreshToken?: string | null;
  user: UserDto;
}

export interface CheckAvailabilityResultDto {
  available: boolean;
  suggestedAlternative?: string | null;
  suggestedSlug?: string | null;
  slugAvailable?: boolean;
  nitAvailable?: boolean;
}
