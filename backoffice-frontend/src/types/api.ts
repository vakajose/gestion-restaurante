export type PlatformRole = 'SUPERADMIN' | 'PLATFORM_OPERATOR';

export interface PlatformPrincipal {
  adminId: string;
  username: string;
  email: string;
  fullName: string;
  role: PlatformRole;
  mustChangePassword: boolean;
}

export interface PlatformAuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  admin: PlatformPrincipal;
}

export interface PlatformAdminDto {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: PlatformRole;
  isActive: boolean;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface CreatePlatformAdminRequest {
  username: string;
  email: string;
  fullName: string;
  initialPassword: string;
  role: PlatformRole;
}

export interface UpdateAdminStatusRequest {
  isActive: boolean;
}

export type BillingCycle = 'WEEKLY' | 'MONTHLY' | 'ANNUAL' | 'CUSTOM';
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'GRACE_PERIOD' | 'SUSPENDED';
export type SuspensionMode = 'NONE' | 'POS_AND_SHIFT_ONLY' | 'TOTAL';
export type QuotaResource = 'BRANCHES' | 'USERS' | 'DISHES' | 'INVENTORY_ITEMS' | 'MONTHLY_ORDERS';
export type ChargeStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'WAIVED';
export type PlatformPaymentMethod = 'QR' | 'TRANSFER' | 'CASH' | 'CARD' | 'OTHER';

export interface SubscriptionPlanDto {
  id: string;
  code: string;
  name: string;
  description: string | null;
  billingCycle: BillingCycle;
  durationDays: number;
  price: number;
  currency: string;
  maxBranches: number | null;
  maxUsers: number | null;
  maxDishes: number | null;
  maxInventoryItems: number | null;
  maxMonthlyOrders: number | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  code: string;
  name: string;
  description?: string | null;
  billingCycle: BillingCycle;
  durationDays: number;
  price: number;
  currency: string;
  maxBranches?: number | null;
  maxUsers?: number | null;
  maxDishes?: number | null;
  maxInventoryItems?: number | null;
  maxMonthlyOrders?: number | null;
}

export interface UpdatePlanRequest {
  name: string;
  description?: string | null;
  billingCycle: BillingCycle;
  durationDays: number;
  price: number;
  currency: string;
  maxBranches?: number | null;
  maxUsers?: number | null;
  maxDishes?: number | null;
  maxInventoryItems?: number | null;
  maxMonthlyOrders?: number | null;
}

export interface ResourceQuotaUsage {
  used: number;
  limit: number | null;
  overQuota: boolean;
}

export interface TenantEntitlementView {
  tenantId: string;
  managedSubscription: boolean;
  status: SubscriptionStatus;
  effectiveSuspensionMode: SuspensionMode;
  suspensionReason: string | null;
  planCode: string | null;
  planName: string | null;
  currentPeriodStart: string | null;
  currentPeriodEnd: string | null;
  graceUntil: string | null;
  graceMaxOrders: number | null;
  graceOrdersUsed: number;
  pendingBalance: number;
  currency: string;
  loginAllowed: boolean;
  posOperationsAllowed: boolean;
  quotas: Record<QuotaResource, ResourceQuotaUsage>;
  overQuotaResources: QuotaResource[];
}

export interface TopTenantSalesDto {
  tenantId: string;
  legalName: string;
  slug: string;
  paidOrdersCount30d: number;
  grossSalesAmount30d: number;
}

export interface DashboardOverviewDto {
  totalTenants: number;
  statusBreakdown: Record<string, number>;
  overQuotaTenantsCount: number;
  collectedRevenueThisMonth: number;
  totalPendingDebt: number;
  topTenantsBySales30d: TopTenantSalesDto[];
}

export interface BackofficeTenantListItemDto {
  tenantId: string;
  legalName: string;
  nitOrTaxId: string | null;
  slug: string;
  tenantActive: boolean;
  subscriptionStatus: SubscriptionStatus;
  effectiveSuspensionMode: SuspensionMode;
  planCode: string | null;
  planName: string | null;
  currentPeriodEnd: string | null;
  pendingBalance: number;
  overQuota: boolean;
  overQuotaResources: QuotaResource[];
  activeBranches: number;
  activeUsers: number;
  monthlyOrders: number;
  salesLast30Days: number;
}

export interface TenantDto {
  id: string;
  legalName: string;
  nitOrTaxId: string | null;
  slug: string;
  logoUrl: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BranchDto {
  id: string;
  tenantId: string;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  currency: string | null;
  timezone: string;
  isActive: boolean;
  createdAt: string;
}

export interface TenantStructuralUsage {
  activeBranches: number;
  totalBranches: number;
  activeUsers: number;
  totalUsers: number;
  activeDishes: number;
  totalDishes: number;
  totalIngredients: number;
  monthlyOrders: number;
  stockRecordsCount: number;
  distinctStockedIngredients: number;
  totalInventoryUnits: number;
  totalInventoryValuation: number;
}

export interface BackofficeTenantDetailDto {
  tenant: TenantDto;
  entitlements: TenantEntitlementView;
  autoSuspendModeOnExpire: SuspensionMode;
  graceNotes: string | null;
  structuralUsage: TenantStructuralUsage;
  branches: BranchDto[];
  salesLast30Days: number;
  paidOrdersLast30Days: number;
}

export interface DailyTelemetryPointDto {
  date: string;
  paidOrdersCount: number;
  grossSalesAmount: number;
  cancelledOrdersCount: number;
}

export interface BranchTelemetryDto {
  branchId: string;
  branchName: string;
  timezone: string;
  isActive: boolean;
  paidOrdersCount: number;
  grossSalesAmount: number;
  cancelledOrdersCount: number;
}

export interface TelemetrySummaryDto {
  paidOrdersCount: number;
  grossSalesAmount: number;
  cancelledOrdersCount: number;
  averageTicket: number;
}

export interface TenantTelemetryReportDto {
  tenantId: string;
  from: string;
  to: string;
  summary: TelemetrySummaryDto;
  dailySeries: DailyTelemetryPointDto[];
  branchBreakdown: BranchTelemetryDto[];
  structuralUsage: TenantStructuralUsage;
}

export interface OnboardingRequest {
  company: {
    legalName: string;
    slug: string;
    nitOrTaxId?: string | null;
  };
  branch: {
    name: string;
    address: string;
    phone?: string | null;
    timezone?: string;
    currency?: string;
  };
  businessTemplate: 'TRADITIONAL' | 'FAST_FOOD' | 'CAFE' | 'BAR' | 'EMPTY';
  includeDemoCatalog: boolean;
  adminUser: {
    fullName: string;
    username: string;
    email: string;
    password: string;
  };
}

export interface OnboardingResultDto {
  tenantId: string;
  branchId: string;
  adminUserId: string;
  slug: string;
}

export interface SubscriptionChargeDto {
  id: string;
  tenantId: string;
  planId: string | null;
  planNameSnapshot: string;
  periodStart: string;
  periodEnd: string;
  amountDue: number;
  paidAmount: number;
  remainingAmount: number;
  currency: string;
  chargeStatus: ChargeStatus;
  dueDate: string | null;
  notes: string | null;
  createdAt: string;
}

export interface TenantChargesSummaryDto {
  tenantId: string;
  totalPendingBalance: number;
  currency: string;
  charges: SubscriptionChargeDto[];
}

export interface SubscriptionPaymentDto {
  id: string;
  tenantId: string;
  chargeId: string | null;
  amount: number;
  currency: string;
  paymentMethod: PlatformPaymentMethod;
  referenceCode: string | null;
  receiptUrl: string | null;
  paidAt: string;
  recordedByAdminId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface PaymentRecordResultDto {
  payment: SubscriptionPaymentDto;
  remainingPendingBalance: number;
  allocatedChargeIds: string[];
}

export interface AssignPlanRequest {
  planId?: string;
  planCode?: string;
  markAsPaid: boolean;
  paymentMethod?: PlatformPaymentMethod;
  referenceCode?: string;
  customAmount?: number | null;
  customDurationDays?: number | null;
  dueDate?: string | null;
  notes?: string;
}

export interface UpdateQuotasRequest {
  maxBranches: number | null;
  maxUsers: number | null;
  maxDishes: number | null;
  maxInventoryItems: number | null;
  maxMonthlyOrders: number | null;
}

export interface GrantGraceRequest {
  graceUntil?: string | null;
  graceMaxOrders?: number | null;
  notes?: string;
}

export interface SuspendTenantRequest {
  mode: 'POS_AND_SHIFT_ONLY' | 'TOTAL';
  reason: string;
}

export interface ReactivateRequest {
  reason?: string;
  extendDays?: number | null;
}

export interface RecordPaymentRequest {
  amount: number;
  currency?: string;
  paymentMethod: PlatformPaymentMethod;
  referenceCode?: string;
  chargeId?: string | null;
  notes?: string;
  autoReactivateIfSettled?: boolean;
}

export interface PlatformSettingDto {
  key: string;
  value: string;
  valueType: string;
  description: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface StorageConnectionTestResult {
  provider: 'LOCAL' | 'S3';
  reachable: boolean;
  targetDescription: string;
  message: string;
  checkedAt: string;
}

export interface HealthNodeDto {
  status: string;
  details: Record<string, unknown>;
  components: Record<string, HealthNodeDto>;
}

export interface HealthOverviewDto {
  status: string;
  checkedAt: string;
  components: Record<string, HealthNodeDto>;
}

export interface MetricMeasurementDto {
  statistic: string;
  value: number;
}

export interface MetricEntryDto {
  name: string;
  description: string | null;
  baseUnit: string | null;
  measurements: MetricMeasurementDto[];
}

export interface MetricsOverviewDto {
  collectedAt: string;
  metrics: MetricEntryDto[];
  totalAvailableMeters: number;
}

export interface LoggerEntryDto {
  name: string;
  configuredLevel: string | null;
  effectiveLevel: string | null;
}

export interface LoggersOverviewDto {
  supportedLevels: string[];
  loggers: LoggerEntryDto[];
}

export interface EnvironmentOverviewDto {
  activeProfiles: string[];
  defaultProfiles: string[];
  properties: Record<string, string>;
}

export interface AuditLogDto {
  id: string;
  actorAdminId: string | null;
  actorUsername: string;
  action: string;
  targetType: string;
  targetId: string | null;
  detailsJson: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
