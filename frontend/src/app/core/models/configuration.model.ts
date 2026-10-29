export type ParameterScope = 'GLOBAL' | 'TENANT' | 'BRANCH';

export type ParameterCategory = 'POS' | 'INVENTORY' | 'FINANCE' | 'SECURITY' | 'GENERAL';

export type ParameterType = 'STRING' | 'BOOLEAN' | 'INTEGER' | 'DOUBLE' | 'ENUM' | 'JSON';

export type ParameterCriticality = 'LOW' | 'MEDIUM' | 'HIGH';

export interface SystemParameterDto {
  key: string;
  name: string;
  description: string;
  category: ParameterCategory;
  type: ParameterType;
  effectiveValue: string;
  defaultValue: string;
  scope: ParameterScope;
  inheritedFrom: ParameterScope;
  isOverridden: boolean;
  criticality: ParameterCriticality;
  requiresRestart: boolean;
  options: string[];
  updatedAt?: string | null;
  updatedBy?: string | null;
  auditReason?: string | null;
}

export interface UpdateParameterRequest {
  scope?: ParameterScope;
  branchId?: string | null;
  value: string;
  passwordConfirmation?: string;
  auditReason?: string;
}
