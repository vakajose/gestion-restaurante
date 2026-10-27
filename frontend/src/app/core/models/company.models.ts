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
