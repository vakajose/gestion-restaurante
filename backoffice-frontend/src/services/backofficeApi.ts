import { apiRequest } from './httpClient';
import type {
  AssignPlanRequest,
  AuditLogDto,
  BackofficeTenantDetailDto,
  BackofficeTenantListItemDto,
  CreatePlanRequest,
  CreatePlatformAdminRequest,
  DashboardOverviewDto,
  EnvironmentOverviewDto,
  GrantGraceRequest,
  HealthOverviewDto,
  LoggerEntryDto,
  LoggersOverviewDto,
  MetricsOverviewDto,
  OnboardingRequest,
  OnboardingResultDto,
  PageResponse,
  PaymentRecordResultDto,
  PlatformAdminDto,
  PlatformAuthResponse,
  PlatformPrincipal,
  PlatformSettingDto,
  ReactivateRequest,
  RecordPaymentRequest,
  StorageConnectionTestResult,
  SubscriptionChargeDto,
  SubscriptionPaymentDto,
  SubscriptionPlanDto,
  SubscriptionStatus,
  SuspendTenantRequest,
  SuspensionMode,
  TenantChargesSummaryDto,
  TenantEntitlementView,
  TenantTelemetryReportDto,
  UpdatePlanRequest,
  UpdateQuotasRequest,
} from '../types/api';

export const authApi = {
  login(login: string, password: string): Promise<PlatformAuthResponse> {
    return apiRequest<PlatformAuthResponse>('/api/v1/backoffice/auth/login', {
      method: 'POST',
      body: { login, password },
      skipAuth: true,
    });
  },

  logout(refreshToken: string | null): Promise<{ message: string }> {
    return apiRequest<{ message: string }>('/api/v1/backoffice/auth/logout', {
      method: 'POST',
      body: refreshToken ? { refreshToken } : {},
      skipRefreshRetry: true,
    });
  },

  me(): Promise<PlatformPrincipal> {
    return apiRequest<PlatformPrincipal>('/api/v1/backoffice/auth/me');
  },

  changePassword(currentPassword: string, newPassword: string): Promise<PlatformPrincipal> {
    return apiRequest<PlatformPrincipal>('/api/v1/backoffice/auth/change-password', {
      method: 'POST',
      body: { currentPassword, newPassword },
    });
  },
};

export interface TenantListFilters {
  search?: string;
  status?: SubscriptionStatus | '';
  hasDebt?: boolean | null;
  overQuota?: boolean | null;
}

export const tenantsApi = {
  getDashboardOverview(): Promise<DashboardOverviewDto> {
    return apiRequest<DashboardOverviewDto>('/api/v1/backoffice/dashboard/overview');
  },

  listTenants(filters: TenantListFilters = {}): Promise<BackofficeTenantListItemDto[]> {
    return apiRequest<BackofficeTenantListItemDto[]>('/api/v1/backoffice/tenants', {
      query: {
        search: filters.search,
        status: filters.status || undefined,
        hasDebt: filters.hasDebt === null ? undefined : filters.hasDebt,
        overQuota: filters.overQuota === null ? undefined : filters.overQuota,
      },
    });
  },

  provisionTenant(payload: OnboardingRequest): Promise<OnboardingResultDto> {
    return apiRequest<OnboardingResultDto>('/api/v1/backoffice/tenants', {
      method: 'POST',
      body: payload,
    });
  },

  getTenantDetail(tenantId: string): Promise<BackofficeTenantDetailDto> {
    return apiRequest<BackofficeTenantDetailDto>(`/api/v1/backoffice/tenants/${tenantId}`);
  },

  getTenantTelemetry(tenantId: string, from?: string, to?: string): Promise<TenantTelemetryReportDto> {
    return apiRequest<TenantTelemetryReportDto>(`/api/v1/backoffice/tenants/${tenantId}/telemetry`, {
      query: { from, to },
    });
  },
};

export const subscriptionsApi = {
  assignPlan(tenantId: string, payload: AssignPlanRequest): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/assign-plan`, {
      method: 'POST',
      body: payload,
    });
  },

  updateQuotas(tenantId: string, payload: UpdateQuotasRequest): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/quotas`, {
      method: 'PUT',
      body: payload,
    });
  },

  grantGrace(tenantId: string, payload: GrantGraceRequest): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/grace`, {
      method: 'POST',
      body: payload,
    });
  },

  suspend(tenantId: string, payload: SuspendTenantRequest): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/suspend`, {
      method: 'POST',
      body: payload,
    });
  },

  reactivate(tenantId: string, payload: ReactivateRequest): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/reactivate`, {
      method: 'POST',
      body: payload,
    });
  },

  updateExpirePolicy(tenantId: string, mode: Exclude<SuspensionMode, 'NONE'>): Promise<TenantEntitlementView> {
    return apiRequest<TenantEntitlementView>(`/api/v1/backoffice/tenants/${tenantId}/subscription/expire-policy`, {
      method: 'PATCH',
      body: { mode },
    });
  },

  listCharges(tenantId: string): Promise<TenantChargesSummaryDto> {
    return apiRequest<TenantChargesSummaryDto>(`/api/v1/backoffice/tenants/${tenantId}/charges`);
  },

  listPayments(tenantId: string): Promise<SubscriptionPaymentDto[]> {
    return apiRequest<SubscriptionPaymentDto[]>(`/api/v1/backoffice/tenants/${tenantId}/payments`);
  },

  recordPayment(tenantId: string, payload: RecordPaymentRequest): Promise<PaymentRecordResultDto> {
    return apiRequest<PaymentRecordResultDto>(`/api/v1/backoffice/tenants/${tenantId}/payments`, {
      method: 'POST',
      body: payload,
    });
  },

  waiveCharge(chargeId: string, reason: string): Promise<SubscriptionChargeDto> {
    return apiRequest<SubscriptionChargeDto>(`/api/v1/backoffice/charges/${chargeId}/waive`, {
      method: 'PATCH',
      body: { reason },
    });
  },
};

export const plansApi = {
  listAll(): Promise<SubscriptionPlanDto[]> {
    return apiRequest<SubscriptionPlanDto[]>('/api/v1/backoffice/plans');
  },

  create(payload: CreatePlanRequest): Promise<SubscriptionPlanDto> {
    return apiRequest<SubscriptionPlanDto>('/api/v1/backoffice/plans', {
      method: 'POST',
      body: payload,
    });
  },

  update(planId: string, payload: UpdatePlanRequest): Promise<SubscriptionPlanDto> {
    return apiRequest<SubscriptionPlanDto>(`/api/v1/backoffice/plans/${planId}`, {
      method: 'PUT',
      body: payload,
    });
  },

  updateStatus(planId: string, isActive: boolean): Promise<SubscriptionPlanDto> {
    return apiRequest<SubscriptionPlanDto>(`/api/v1/backoffice/plans/${planId}/status`, {
      method: 'PATCH',
      body: { isActive },
    });
  },
};

export const adminsApi = {
  listAll(): Promise<PlatformAdminDto[]> {
    return apiRequest<PlatformAdminDto[]>('/api/v1/backoffice/admins');
  },

  create(payload: CreatePlatformAdminRequest): Promise<PlatformAdminDto> {
    return apiRequest<PlatformAdminDto>('/api/v1/backoffice/admins', {
      method: 'POST',
      body: payload,
    });
  },

  updateStatus(adminId: string, isActive: boolean): Promise<PlatformAdminDto> {
    return apiRequest<PlatformAdminDto>(`/api/v1/backoffice/admins/${adminId}/status`, {
      method: 'PATCH',
      body: { isActive },
    });
  },
};

export interface AuditLogQuery {
  actor?: string;
  action?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export const operationsApi = {
  listSettings(): Promise<PlatformSettingDto[]> {
    return apiRequest<PlatformSettingDto[]>('/api/v1/backoffice/settings');
  },

  updateSetting(settingKey: string, value: string): Promise<PlatformSettingDto> {
    return apiRequest<PlatformSettingDto>(`/api/v1/backoffice/settings/${encodeURIComponent(settingKey)}`, {
      method: 'PUT',
      body: { value },
    });
  },

  testActiveStorageConnection(): Promise<StorageConnectionTestResult> {
    return apiRequest<StorageConnectionTestResult>('/api/v1/backoffice/storage/test-connection', {
      method: 'POST',
    });
  },

  getHealth(): Promise<HealthOverviewDto> {
    return apiRequest<HealthOverviewDto>('/api/v1/backoffice/observability/health');
  },

  getMetrics(): Promise<MetricsOverviewDto> {
    return apiRequest<MetricsOverviewDto>('/api/v1/backoffice/observability/metrics');
  },

  getLoggers(prefix = 'com.restaurant.app'): Promise<LoggersOverviewDto> {
    return apiRequest<LoggersOverviewDto>('/api/v1/backoffice/observability/loggers', {
      query: { prefix },
    });
  },

  configureLogger(loggerName: string, configuredLevel: string): Promise<LoggerEntryDto> {
    return apiRequest<LoggerEntryDto>(
      `/api/v1/backoffice/observability/loggers/${encodeURIComponent(loggerName)}`,
      {
        method: 'PUT',
        body: { configuredLevel },
      },
    );
  },

  getEnvironment(): Promise<EnvironmentOverviewDto> {
    return apiRequest<EnvironmentOverviewDto>('/api/v1/backoffice/observability/environment');
  },

  searchAuditLogs(query: AuditLogQuery = {}): Promise<PageResponse<AuditLogDto>> {
    return apiRequest<PageResponse<AuditLogDto>>('/api/v1/backoffice/audit-logs', {
      query: {
        actor: query.actor,
        action: query.action,
        from: query.from,
        to: query.to,
        page: query.page ?? 0,
        size: query.size ?? 25,
      },
    });
  },
};
