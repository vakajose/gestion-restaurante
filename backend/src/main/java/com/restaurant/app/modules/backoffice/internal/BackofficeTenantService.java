package com.restaurant.app.modules.backoffice.internal;

import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.SubscriptionStatus;
import com.restaurant.app.core.entitlements.api.SuspensionMode;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.entitlements.api.TenantEntitlementView;
import com.restaurant.app.modules.backoffice.internal.TenantUsageSnapshotReader.TenantStructuralUsage;
import com.restaurant.app.modules.company.api.BranchDto;
import com.restaurant.app.modules.company.api.CompanyPublicApi;
import com.restaurant.app.modules.company.api.TenantDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
class BackofficeTenantService {

    private final CompanyPublicApi companyPublicApi;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionChargeRepository chargeRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final TenantDailyTelemetryRepository telemetryRepository;
    private final TenantUsageSnapshotReader usageReader;
    private final TenantEntitlementService entitlementService;

    BackofficeTenantService(
        CompanyPublicApi companyPublicApi,
        TenantSubscriptionRepository subscriptionRepository,
        SubscriptionChargeRepository chargeRepository,
        SubscriptionPaymentRepository paymentRepository,
        TenantDailyTelemetryRepository telemetryRepository,
        TenantUsageSnapshotReader usageReader,
        TenantEntitlementService entitlementService
    ) {
        this.companyPublicApi = companyPublicApi;
        this.subscriptionRepository = subscriptionRepository;
        this.chargeRepository = chargeRepository;
        this.paymentRepository = paymentRepository;
        this.telemetryRepository = telemetryRepository;
        this.usageReader = usageReader;
        this.entitlementService = entitlementService;
    }

    record TenantSalesAggregate(long paidOrders, BigDecimal grossSales, long cancelledOrders) {
        static final TenantSalesAggregate ZERO = new TenantSalesAggregate(0L, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L);
    }

    record TopTenantSalesDto(
        UUID tenantId,
        String legalName,
        String slug,
        long paidOrdersCount30d,
        BigDecimal grossSalesAmount30d
    ) {
    }

    record DashboardOverviewDto(
        long totalTenants,
        Map<String, Long> statusBreakdown,
        long overQuotaTenantsCount,
        BigDecimal collectedRevenueThisMonth,
        BigDecimal totalPendingDebt,
        List<TopTenantSalesDto> topTenantsBySales30d
    ) {
    }

    record BackofficeTenantListItemDto(
        UUID tenantId,
        String legalName,
        String nitOrTaxId,
        String slug,
        boolean tenantActive,
        SubscriptionStatus subscriptionStatus,
        SuspensionMode effectiveSuspensionMode,
        String planCode,
        String planName,
        Instant currentPeriodEnd,
        BigDecimal pendingBalance,
        boolean overQuota,
        List<QuotaResource> overQuotaResources,
        long activeBranches,
        long activeUsers,
        long monthlyOrders,
        BigDecimal salesLast30Days
    ) {
    }

    record BackofficeTenantDetailDto(
        TenantDto tenant,
        TenantEntitlementView entitlements,
        SuspensionMode autoSuspendModeOnExpire,
        String graceNotes,
        TenantStructuralUsage structuralUsage,
        List<BranchDto> branches,
        BigDecimal salesLast30Days,
        long paidOrdersLast30Days
    ) {
    }

    record DailyTelemetryPointDto(
        LocalDate date,
        long paidOrdersCount,
        BigDecimal grossSalesAmount,
        long cancelledOrdersCount
    ) {
    }

    record BranchTelemetryDto(
        UUID branchId,
        String branchName,
        String timezone,
        boolean isActive,
        long paidOrdersCount,
        BigDecimal grossSalesAmount,
        long cancelledOrdersCount
    ) {
    }

    record TelemetrySummaryDto(
        long paidOrdersCount,
        BigDecimal grossSalesAmount,
        long cancelledOrdersCount,
        BigDecimal averageTicket
    ) {
    }

    record TenantTelemetryReportDto(
        UUID tenantId,
        LocalDate from,
        LocalDate to,
        TelemetrySummaryDto summary,
        List<DailyTelemetryPointDto> dailySeries,
        List<BranchTelemetryDto> branchBreakdown,
        TenantStructuralUsage structuralUsage
    ) {
    }

    public DashboardOverviewDto getDashboardOverview() {
        List<TenantDto> tenants = companyPublicApi.listAllCompanies();
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (SubscriptionStatus st : SubscriptionStatus.values()) {
            breakdown.put(st.name(), 0L);
        }

        long overQuotaCount = 0L;
        for (TenantDto tenant : tenants) {
            TenantEntitlementView view = entitlementService.getEntitlementView(tenant.id());
            breakdown.merge(view.status().name(), 1L, Long::sum);
            if (!view.overQuotaResources().isEmpty()) {
                overQuotaCount++;
            }
        }

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant monthStart = todayUtc.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextMonthStart = todayUtc.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal collectedRevenueMonth = paymentRepository.sumPaymentsInRange(monthStart, nextMonthStart);
        BigDecimal totalPendingDebt = chargeRepository.sumTotalPendingDebt();

        Map<UUID, TenantSalesAggregate> sales30d = aggregateSalesLast30Days();
        Map<UUID, TenantDto> tenantById = tenants.stream()
            .collect(Collectors.toMap(TenantDto::id, Function.identity()));

        List<TopTenantSalesDto> topTenants = sales30d.entrySet().stream()
            .filter(e -> tenantById.containsKey(e.getKey()))
            .sorted((a, b) -> b.getValue().grossSales().compareTo(a.getValue().grossSales()))
            .limit(5)
            .map(e -> {
                TenantDto t = tenantById.get(e.getKey());
                return new TopTenantSalesDto(
                    t.id(),
                    t.legalName(),
                    t.slug(),
                    e.getValue().paidOrders(),
                    e.getValue().grossSales()
                );
            })
            .toList();

        return new DashboardOverviewDto(
            tenants.size(),
            breakdown,
            overQuotaCount,
            collectedRevenueMonth,
            totalPendingDebt,
            topTenants
        );
    }

    public List<BackofficeTenantListItemDto> listTenants(
        String search,
        SubscriptionStatus status,
        Boolean hasDebt,
        Boolean overQuota
    ) {
        List<TenantDto> tenants = companyPublicApi.listAllCompanies();
        Map<UUID, BigDecimal> debtByTenant = loadPendingDebtMap();
        Map<UUID, TenantSalesAggregate> sales30d = aggregateSalesLast30Days();
        String normalizedSearch = search != null && !search.isBlank() ? search.trim().toLowerCase() : null;

        List<BackofficeTenantListItemDto> result = new ArrayList<>();
        for (TenantDto tenant : tenants) {
            if (normalizedSearch != null) {
                boolean matches = tenant.legalName().toLowerCase().contains(normalizedSearch)
                    || tenant.slug().toLowerCase().contains(normalizedSearch)
                    || (tenant.nitOrTaxId() != null && tenant.nitOrTaxId().toLowerCase().contains(normalizedSearch));
                if (!matches) {
                    continue;
                }
            }

            TenantEntitlementView view = entitlementService.getEntitlementView(tenant.id());
            BigDecimal pendingDebt = debtByTenant.getOrDefault(tenant.id(), view.pendingBalance());
            boolean tenantHasDebt = pendingDebt.compareTo(BigDecimal.ZERO) > 0;
            boolean tenantOverQuota = !view.overQuotaResources().isEmpty();

            if (status != null && view.status() != status) {
                continue;
            }
            if (hasDebt != null && hasDebt.booleanValue() != tenantHasDebt) {
                continue;
            }
            if (overQuota != null && overQuota.booleanValue() != tenantOverQuota) {
                continue;
            }

            TenantSalesAggregate agg = sales30d.getOrDefault(tenant.id(), TenantSalesAggregate.ZERO);
            long activeBranches = view.quotas().get(QuotaResource.BRANCHES) != null
                ? view.quotas().get(QuotaResource.BRANCHES).used() : 0L;
            long activeUsers = view.quotas().get(QuotaResource.USERS) != null
                ? view.quotas().get(QuotaResource.USERS).used() : 0L;
            long monthlyOrders = view.quotas().get(QuotaResource.MONTHLY_ORDERS) != null
                ? view.quotas().get(QuotaResource.MONTHLY_ORDERS).used() : 0L;

            result.add(new BackofficeTenantListItemDto(
                tenant.id(),
                tenant.legalName(),
                tenant.nitOrTaxId(),
                tenant.slug(),
                tenant.isActive(),
                view.status(),
                view.effectiveSuspensionMode(),
                view.planCode(),
                view.planName(),
                view.currentPeriodEnd(),
                pendingDebt,
                tenantOverQuota,
                view.overQuotaResources(),
                activeBranches,
                activeUsers,
                monthlyOrders,
                agg.grossSales()
            ));
        }
        return result;
    }

    public BackofficeTenantDetailDto getTenantDetail(UUID tenantId) {
        TenantDto tenant = companyPublicApi.getCompany(tenantId);
        TenantEntitlementView entitlements = entitlementService.getEntitlementView(tenantId);
        TenantStructuralUsage structuralUsage = usageReader.readFullUsage(tenantId);
        TenantSubscription sub = subscriptionRepository.findByTenantId(tenantId).orElse(null);

        LocalDate to = LocalDate.now(structuralUsage.primaryTimezone());
        LocalDate from = to.minusDays(29);
        List<TenantDailyTelemetry> rows = telemetryRepository
            .findByTenantIdAndStatDateBetweenOrderByStatDateAsc(tenantId, from, to);

        BigDecimal sales30d = rows.stream()
            .map(TenantDailyTelemetry::getGrossSalesAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        long orders30d = rows.stream()
            .mapToLong(TenantDailyTelemetry::getPaidOrdersCount)
            .sum();

        return new BackofficeTenantDetailDto(
            tenant,
            entitlements,
            sub != null ? sub.getAutoSuspendModeOnExpire() : SuspensionMode.POS_AND_SHIFT_ONLY,
            sub != null ? sub.getGraceNotes() : null,
            structuralUsage,
            structuralUsage.branches(),
            sales30d,
            orders30d
        );
    }

    public TenantTelemetryReportDto getTenantTelemetry(UUID tenantId, LocalDate from, LocalDate to) {
        companyPublicApi.getCompany(tenantId);
        TenantStructuralUsage structuralUsage = usageReader.readFullUsage(tenantId);

        LocalDate effectiveTo = to != null ? to : LocalDate.now(structuralUsage.primaryTimezone());
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ValidationException("La fecha 'from' no puede ser posterior a 'to'");
        }

        List<TenantDailyTelemetry> rows = telemetryRepository
            .findByTenantIdAndStatDateBetweenOrderByStatDateAsc(tenantId, effectiveFrom, effectiveTo);

        Map<LocalDate, DailyTelemetryPointDto> dailyMap = new LinkedHashMap<>();
        Map<UUID, long[]> branchCounts = new LinkedHashMap<>();
        Map<UUID, BigDecimal> branchSales = new LinkedHashMap<>();

        long totalPaid = 0L;
        long totalCancelled = 0L;
        BigDecimal totalGross = BigDecimal.ZERO;

        for (TenantDailyTelemetry row : rows) {
            totalPaid += row.getPaidOrdersCount();
            totalCancelled += row.getCancelledOrdersCount();
            totalGross = totalGross.add(row.getGrossSalesAmount());

            dailyMap.merge(
                row.getStatDate(),
                new DailyTelemetryPointDto(
                    row.getStatDate(),
                    row.getPaidOrdersCount(),
                    row.getGrossSalesAmount(),
                    row.getCancelledOrdersCount()
                ),
                (a, b) -> new DailyTelemetryPointDto(
                    a.date(),
                    a.paidOrdersCount() + b.paidOrdersCount(),
                    a.grossSalesAmount().add(b.grossSalesAmount()).setScale(2, RoundingMode.HALF_UP),
                    a.cancelledOrdersCount() + b.cancelledOrdersCount()
                )
            );

            branchCounts.computeIfAbsent(row.getBranchId(), k -> new long[2]);
            branchCounts.get(row.getBranchId())[0] += row.getPaidOrdersCount();
            branchCounts.get(row.getBranchId())[1] += row.getCancelledOrdersCount();
            branchSales.merge(row.getBranchId(), row.getGrossSalesAmount(), BigDecimal::add);
        }

        List<BranchTelemetryDto> branchBreakdown = structuralUsage.branches().stream()
            .map(branch -> {
                long[] counts = branchCounts.getOrDefault(branch.id(), new long[]{0L, 0L});
                BigDecimal sales = branchSales.getOrDefault(branch.id(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                return new BranchTelemetryDto(
                    branch.id(),
                    branch.name(),
                    branch.timezone(),
                    branch.isActive(),
                    counts[0],
                    sales,
                    counts[1]
                );
            })
            .sorted(Comparator.comparing(BranchTelemetryDto::grossSalesAmount).reversed())
            .toList();

        totalGross = totalGross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgTicket = totalPaid > 0
            ? totalGross.divide(BigDecimal.valueOf(totalPaid), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new TenantTelemetryReportDto(
            tenantId,
            effectiveFrom,
            effectiveTo,
            new TelemetrySummaryDto(totalPaid, totalGross, totalCancelled, avgTicket),
            new ArrayList<>(dailyMap.values()),
            branchBreakdown,
            structuralUsage
        );
    }

    private Map<UUID, BigDecimal> loadPendingDebtMap() {
        Map<UUID, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : chargeRepository.sumPendingBalanceGroupedByTenant()) {
            UUID tid = (UUID) row[0];
            BigDecimal amount = row[1] != null ? ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            map.put(tid, amount);
        }
        return map;
    }

    private Map<UUID, TenantSalesAggregate> aggregateSalesLast30Days() {
        LocalDate to = LocalDate.now(ZoneOffset.UTC).plusDays(1); // margen para zonas adelantadas
        LocalDate from = to.minusDays(31);
        Map<UUID, TenantSalesAggregate> map = new LinkedHashMap<>();
        for (Object[] row : telemetryRepository.aggregateByTenantInRange(from, to)) {
            UUID tid = (UUID) row[0];
            long paid = ((Number) row[1]).longValue();
            BigDecimal gross = row[2] != null
                ? ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long cancelled = ((Number) row[3]).longValue();
            map.put(tid, new TenantSalesAggregate(paid, gross, cancelled));
        }
        return map;
    }
}
