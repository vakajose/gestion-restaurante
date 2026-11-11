package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CashShiftSummaryDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.finance.api.OrderAuditSummaryDto;
import com.restaurant.app.modules.finance.api.ShiftAuditDetailDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
class CashShiftService {

    private final CashShiftRepository cashShiftRepository;
    private final ExpenseRepository expenseRepository;
    private final UserPublicApi userPublicApi;
    private final ConfigurationPublicApi configurationPublicApi;
    private final DailyMenuPublicApi dailyMenuPublicApi;
    private final BranchPublicApi branchPublicApi;
    private final JdbcClient jdbcClient;

    CashShiftService(
        CashShiftRepository cashShiftRepository,
        ExpenseRepository expenseRepository,
        UserPublicApi userPublicApi,
        ConfigurationPublicApi configurationPublicApi,
        DailyMenuPublicApi dailyMenuPublicApi,
        BranchPublicApi branchPublicApi,
        JdbcClient jdbcClient
    ) {
        this.cashShiftRepository = cashShiftRepository;
        this.expenseRepository = expenseRepository;
        this.userPublicApi = userPublicApi;
        this.configurationPublicApi = configurationPublicApi;
        this.dailyMenuPublicApi = dailyMenuPublicApi;
        this.branchPublicApi = branchPublicApi;
        this.jdbcClient = jdbcClient;
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedOperationException("Contexto de tenant requerido para esta operación");
        }
        return tenantId;
    }

    private UUID currentUserId() {
        UUID userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new UnauthorizedOperationException("Contexto de usuario requerido para operar la caja");
        }
        return userId;
    }

    public CashShiftDto openShift(OpenShiftRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        if (request.branchId() == null) {
            throw new ValidationException("El branchId es obligatorio para abrir un turno");
        }

        // Validar política de apertura de turno (ADR-0013)
        String openingPolicy = "ALL_AUTHORIZED";
        try {
            openingPolicy = configurationPublicApi.getString(
                SystemParameter.FINANCE_SHIFT_OPENING_POLICY,
                request.branchId(),
                tenantId
            );
        } catch (Exception ignored) {}

        if ("SUPERVISOR_ONLY".equalsIgnoreCase(openingPolicy)) {
            TenantContext context = TenantContextHolder.getContext().orElse(null);
            boolean isSupervisor = context != null && (
                "ADMIN_TENANT".equalsIgnoreCase(context.role()) ||
                "SUPERADMIN".equalsIgnoreCase(context.role()) ||
                context.hasPermission(SystemPermissions.FINANCE_SHIFTS_SUPERVISOR)
            );
            if (!isSupervisor) {
                throw new UnauthorizedOperationException(
                    "Caja Cerrada — Esperando apertura de turno por parte de un supervisor"
                );
            }
        }

        // Validar modo de concurrencia de turnos (ADR-0016)
        String concurrencyMode = getConcurrencyMode(tenantId, request.branchId());
        if ("INDIVIDUAL_PER_CASHIER".equalsIgnoreCase(concurrencyMode)) {
            Optional<CashShift> userShift = cashShiftRepository.findByTenantIdAndBranchIdAndOpenedByAndStatus(
                tenantId, request.branchId(), userId, "OPEN"
            );
            if (userShift.isPresent()) {
                throw new ResourceConflictException("Ya tienes un turno de caja abierto en esta sucursal");
            }
        } else {
            // Modo SINGLE_PER_BRANCH (Default)
            Optional<CashShift> openShiftOpt = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
                tenantId, request.branchId(), "OPEN"
            );
            if (openShiftOpt.isPresent()) {
                throw new ResourceConflictException("Ya existe un turno de caja abierto en esta sucursal");
            }
        }

        BigDecimal initialCash = request.initialCash() != null
            ? request.initialCash().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        CashShift shift = new CashShift(tenantId, request.branchId(), userId, initialCash);
        CashShift saved = cashShiftRepository.save(shift);

        if (request.menuTemplateId() != null) {
            dailyMenuPublicApi.instantiateTemplateForShift(
                tenantId,
                request.branchId(),
                saved.getId(),
                request.menuTemplateId(),
                request.customQuotas(),
                userId
            );
        }

        return toDto(saved, initialCash);
    }

    private String getConcurrencyMode(UUID tenantId, UUID branchId) {
        try {
            return configurationPublicApi.getString(
                SystemParameter.FINANCE_SHIFT_CONCURRENCY_MODE,
                branchId,
                tenantId
            );
        } catch (Exception e) {
            return "SINGLE_PER_BRANCH";
        }
    }

    @Transactional(readOnly = true)
    public Optional<CashShiftDto> findActiveShift(UUID tenantId, UUID branchId) {
        if (tenantId == null || branchId == null) {
            return Optional.empty();
        }
        String concurrencyMode = getConcurrencyMode(tenantId, branchId);
        if ("INDIVIDUAL_PER_CASHIER".equalsIgnoreCase(concurrencyMode)) {
            UUID userId = TenantContextHolder.getUserId();
            if (userId != null) {
                Optional<CashShiftDto> userShift = findActiveShiftForUser(tenantId, branchId, userId);
                if (userShift.isPresent()) {
                    return userShift;
                }
            }
        }
        return cashShiftRepository.findFirstByTenantIdAndBranchIdAndStatusOrderByOpenedAtDesc(tenantId, branchId, "OPEN")
            .map(this::enrichActiveShiftDto);
    }

    @Transactional(readOnly = true)
    public Optional<CashShiftDto> findActiveShiftForUser(UUID tenantId, UUID branchId, UUID userId) {
        if (tenantId == null || branchId == null || userId == null) {
            return Optional.empty();
        }
        return cashShiftRepository.findFirstByTenantIdAndBranchIdAndOpenedByAndStatusOrderByOpenedAtDesc(tenantId, branchId, userId, "OPEN")
            .map(this::enrichActiveShiftDto);
    }

    @Transactional(readOnly = true)
    public List<CashShiftDto> listActiveShifts(UUID tenantId, UUID branchId) {
        if (tenantId == null || branchId == null) {
            return List.of();
        }
        return cashShiftRepository.findByTenantIdAndBranchIdAndStatusOrderByOpenedAtDesc(tenantId, branchId, "OPEN")
            .stream()
            .map(this::enrichActiveShiftDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CashShiftDto> getCurrentShift(UUID branchId) {
        UUID tenantId = currentTenantId();
        return findActiveShift(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<CashShiftDto> getActiveShifts(UUID branchId) {
        UUID tenantId = currentTenantId();
        return listActiveShifts(tenantId, branchId);
    }

    public CashShiftDto closeShift(UUID shiftId, CloseShiftRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Turno de caja", shiftId));

        if (!"OPEN".equalsIgnoreCase(shift.getStatus())) {
            throw new BusinessRuleException("El turno de caja ya se encuentra cerrado");
        }

        // 1. Totales de ventas acumulados en el turno
        BigDecimal totalCashSales = shift.getTotalCashSales() != null ? shift.getTotalCashSales() : BigDecimal.ZERO;
        BigDecimal totalCardSales = shift.getTotalCardSales() != null ? shift.getTotalCardSales() : BigDecimal.ZERO;
        BigDecimal totalOtherSales = shift.getTotalOtherSales() != null ? shift.getTotalOtherSales() : BigDecimal.ZERO;

        // 2. Sumar gastos de caja chica aprobados y pagados con efectivo de la gaveta
        List<Expense> approvedExpenses = expenseRepository
            .findByTenantIdAndCashShiftIdAndPaidFromCashDrawerTrueAndApprovalStatus(
                tenantId, shift.getId(), "APPROVED"
            );

        BigDecimal totalCashExpenses = approvedExpenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        // 3. Fórmula del arqueo de caja (ADR-0019 / SDD):
        // expected_cash = initial_cash + total_cash_sales - total_cash_expenses_approved - total_cash_refunds
        BigDecimal initialCash = shift.getInitialCash() != null ? shift.getInitialCash() : BigDecimal.ZERO;
        BigDecimal totalCashRefunds = shift.getTotalCashRefunds() != null ? shift.getTotalCashRefunds() : BigDecimal.ZERO;
        BigDecimal expectedCash = initialCash.add(totalCashSales)
            .subtract(totalCashExpenses)
            .subtract(totalCashRefunds)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualCash = request.actualCash() != null
            ? request.actualCash().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal difference = actualCash.subtract(expectedCash).setScale(2, RoundingMode.HALF_UP);

        // 4. Actualizar y cerrar turno
        shift.setTotalCashSales(totalCashSales);
        shift.setTotalCardSales(totalCardSales);
        shift.setTotalOtherSales(totalOtherSales);
        shift.setTotalCashExpenses(totalCashExpenses);
        shift.setTotalCashRefunds(totalCashRefunds);
        shift.setExpectedCash(expectedCash);
        shift.setActualCash(actualCash);
        shift.setDifference(difference);
        shift.setStatus("CLOSED");
        shift.setClosedBy(userId);
        shift.setClosedAt(Instant.now());

        CashShift closed = cashShiftRepository.save(shift);
        return toDto(closed, expectedCash);
    }

    @Transactional(readOnly = true)
    public List<CashShiftDto> getShifts(UUID branchId) {
        UUID tenantId = currentTenantId();
        List<CashShift> shifts;
        if (branchId != null) {
            shifts = cashShiftRepository.findByTenantIdAndBranchIdOrderByOpenedAtDesc(tenantId, branchId);
        } else {
            shifts = cashShiftRepository.findByTenantIdOrderByOpenedAtDesc(tenantId);
        }
        return shifts.stream().map(s -> "OPEN".equals(s.getStatus()) ? enrichActiveShiftDto(s) : toDto(s, s.getExpectedCash())).toList();
    }

    @Transactional(readOnly = true)
    public List<CashShiftDto> getShiftsHistory(
        UUID branchId,
        String startDate,
        String endDate,
        UUID cashierId,
        String status,
        String discrepancyType,
        int limit
    ) {
        UUID tenantId = currentTenantId();

        TenantContext context = TenantContextHolder.getContext().orElse(null);
        boolean isGlobalAdmin = context != null && (
            "ADMIN_TENANT".equalsIgnoreCase(context.role()) ||
            "SUPERADMIN".equalsIgnoreCase(context.role())
        );

        UUID effectiveBranchId = branchId;
        if (!isGlobalAdmin && context != null && context.branchId() != null) {
            effectiveBranchId = context.branchId();
        }

        String branchTimezone = "America/La_Paz";
        if (effectiveBranchId != null) {
            try {
                String tz = branchPublicApi.getBranchTimezone(effectiveBranchId);
                if (tz != null && !tz.isBlank()) {
                    branchTimezone = tz;
                }
            } catch (Exception ignored) {}
        }

        ZoneId zone = ZoneId.of(branchTimezone);
        Instant startUtc = null;
        if (startDate != null && !startDate.isBlank()) {
            startUtc = LocalDate.parse(startDate).atStartOfDay(zone).toInstant();
        }
        Instant endUtc = null;
        if (endDate != null && !endDate.isBlank()) {
            endUtc = LocalDate.parse(endDate).atTime(LocalTime.MAX).atZone(zone).toInstant();
        }

        int effectiveLimit = limit > 0 ? Math.min(limit, 200) : 50;
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        List<CashShift> shifts = cashShiftRepository.findShiftsWithFilters(
            tenantId,
            effectiveBranchId,
            startUtc,
            endUtc,
            cashierId,
            (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.toUpperCase() : null,
            (discrepancyType != null && !discrepancyType.isBlank() && !"ALL".equalsIgnoreCase(discrepancyType)) ? discrepancyType.toUpperCase() : null,
            pageable
        );

        return shifts.stream()
            .map(s -> "OPEN".equals(s.getStatus()) ? enrichActiveShiftDto(s) : toDto(s, s.getExpectedCash()))
            .toList();
    }

    @Transactional(readOnly = true)
    public ShiftAuditDetailDto getShiftAuditDetail(UUID shiftId) {
        UUID tenantId = currentTenantId();
        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Turno de caja", shiftId));

        CashShiftDto shiftDto = "OPEN".equals(shift.getStatus())
            ? enrichActiveShiftDto(shift)
            : toDto(shift, shift.getExpectedCash());

        List<OrderAuditSummaryDto> allOrders = jdbcClient.sql("""
            SELECT id, ticket_number, created_at, customer_name, payment_method, total_amount, order_status, order_type, cancellation_reason
            FROM orders
            WHERE tenant_id = :tenantId AND cash_shift_id = :shiftId
            ORDER BY created_at DESC
        """)
        .param("tenantId", tenantId)
        .param("shiftId", shiftId)
        .query(OrderAuditSummaryDto.class)
        .list();

        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal qrSales = BigDecimal.ZERO;
        BigDecimal transferSales = BigDecimal.ZERO;

        for (OrderAuditSummaryDto order : allOrders) {
            if ("PAID".equalsIgnoreCase(order.orderStatus())) {
                String pm = order.paymentMethod() != null ? order.paymentMethod().toUpperCase() : "CASH";
                BigDecimal amt = order.totalAmount() != null ? order.totalAmount() : BigDecimal.ZERO;
                switch (pm) {
                    case "CARD" -> cardSales = cardSales.add(amt);
                    case "QR" -> qrSales = qrSales.add(amt);
                    case "TRANSFER" -> transferSales = transferSales.add(amt);
                    default -> cashSales = cashSales.add(amt);
                }
            }
        }

        if (cashSales.compareTo(BigDecimal.ZERO) == 0 && shift.getTotalCashSales() != null && shift.getTotalCashSales().compareTo(BigDecimal.ZERO) > 0) {
            cashSales = shift.getTotalCashSales();
        }
        if (cardSales.compareTo(BigDecimal.ZERO) == 0 && shift.getTotalCardSales() != null && shift.getTotalCardSales().compareTo(BigDecimal.ZERO) > 0) {
            cardSales = shift.getTotalCardSales();
        }

        List<OrderAuditSummaryDto> refunds = allOrders.stream()
            .filter(o -> "CANCELLED".equalsIgnoreCase(o.orderStatus()))
            .toList();

        List<Expense> expenses = expenseRepository.findByTenantIdAndCashShiftIdOrderByCreatedAtDesc(tenantId, shiftId);
        List<ExpenseDto> expenseDtos = expenses.stream().map(this::toExpenseDto).toList();

        return new ShiftAuditDetailDto(
            shiftDto,
            cashSales,
            cardSales,
            qrSales,
            transferSales,
            allOrders,
            expenseDtos,
            refunds
        );
    }

    private ExpenseDto toExpenseDto(Expense expense) {
        return new ExpenseDto(
            expense.getId(),
            expense.getTenantId(),
            expense.getBranchId(),
            expense.getCashShiftId(),
            expense.getExpenseType(),
            expense.getDescription(),
            expense.getAmount(),
            expense.isPaidFromCashDrawer(),
            expense.getApprovalStatus(),
            expense.getApprovedBy(),
            resolveUserName(expense.getApprovedBy()),
            expense.getEvidenceUrl(),
            expense.getApprovalNotes(),
            expense.getExpenseDate(),
            expense.getCreatedBy(),
            resolveUserName(expense.getCreatedBy()),
            expense.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public CashShiftDto getShiftById(UUID shiftId) {
        UUID tenantId = currentTenantId();
        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Turno no encontrado con ID: " + shiftId));
        return "OPEN".equals(shift.getStatus()) ? enrichActiveShiftDto(shift) : toDto(shift, shift.getExpectedCash());
    }

    @Transactional(readOnly = true)
    public CashShiftSummaryDto getShiftSummary(UUID shiftId) {
        CashShiftDto shiftDto = getShiftById(shiftId);
        int approvedCount = expenseRepository.countByTenantIdAndCashShiftIdAndApprovalStatus(
            shiftDto.tenantId(), shiftId, "APPROVED"
        );
        int pendingCount = expenseRepository.countByTenantIdAndCashShiftIdAndApprovalStatus(
            shiftDto.tenantId(), shiftId, "PENDING_APPROVAL"
        );

        return new CashShiftSummaryDto(
            shiftDto,
            0,
            approvedCount,
            pendingCount
        );
    }

    void updateRunningSales(UUID tenantId, UUID branchId, UUID cashShiftId, String paymentMethod, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Optional<CashShift> shiftOpt;
        if (cashShiftId != null) {
            shiftOpt = cashShiftRepository.findByIdAndTenantId(cashShiftId, tenantId);
        } else {
            shiftOpt = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN");
        }

        if (shiftOpt.isPresent()) {
            CashShift shift = shiftOpt.get();
            if ("OPEN".equalsIgnoreCase(shift.getStatus())) {
                String method = paymentMethod != null ? paymentMethod.toUpperCase() : "CASH";
                if ("CASH".equals(method)) {
                    shift.setTotalCashSales(shift.getTotalCashSales().add(amount).setScale(2, RoundingMode.HALF_UP));
                } else if ("CARD".equals(method)) {
                    shift.setTotalCardSales(shift.getTotalCardSales().add(amount).setScale(2, RoundingMode.HALF_UP));
                } else if ("QR".equals(method) || "TRANSFER".equals(method)) {
                    shift.setTotalOtherSales(shift.getTotalOtherSales().add(amount).setScale(2, RoundingMode.HALF_UP));
                }
                cashShiftRepository.save(shift);
            }
        }
    }

    void recordRefund(UUID tenantId, UUID branchId, UUID cashShiftId, BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Optional<CashShift> shiftOpt;
        if (cashShiftId != null) {
            shiftOpt = cashShiftRepository.findByIdAndTenantId(cashShiftId, tenantId);
        } else {
            shiftOpt = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN");
        }

        if (shiftOpt.isPresent()) {
            CashShift shift = shiftOpt.get();
            BigDecimal current = shift.getTotalCashRefunds() != null ? shift.getTotalCashRefunds() : BigDecimal.ZERO;
            shift.setTotalCashRefunds(current.add(refundAmount).setScale(2, RoundingMode.HALF_UP));
            cashShiftRepository.save(shift);
        }
    }

    private CashShiftDto enrichActiveShiftDto(CashShift shift) {
        BigDecimal cashSales = shift.getTotalCashSales() != null ? shift.getTotalCashSales() : BigDecimal.ZERO;
        BigDecimal cardSales = shift.getTotalCardSales() != null ? shift.getTotalCardSales() : BigDecimal.ZERO;
        BigDecimal otherSales = shift.getTotalOtherSales() != null ? shift.getTotalOtherSales() : BigDecimal.ZERO;

        List<Expense> approvedExpenses = expenseRepository
            .findByTenantIdAndCashShiftIdAndPaidFromCashDrawerTrueAndApprovalStatus(
                shift.getTenantId(), shift.getId(), "APPROVED"
            );
        BigDecimal cashExpenses = approvedExpenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashRefunds = shift.getTotalCashRefunds() != null ? shift.getTotalCashRefunds() : BigDecimal.ZERO;
        BigDecimal expectedCash = shift.getInitialCash()
            .add(cashSales)
            .subtract(cashExpenses)
            .subtract(cashRefunds)
            .setScale(2, RoundingMode.HALF_UP);

        return new CashShiftDto(
            shift.getId(),
            shift.getTenantId(),
            shift.getBranchId(),
            shift.getOpenedBy(),
            resolveUserName(shift.getOpenedBy()),
            shift.getClosedBy(),
            resolveUserName(shift.getClosedBy()),
            shift.getOpenedAt(),
            shift.getClosedAt(),
            shift.getInitialCash(),
            cashSales,
            cardSales,
            otherSales,
            cashExpenses,
            cashRefunds,
            expectedCash,
            shift.getActualCash(),
            shift.getDifference(),
            shift.getStatus(),
            shift.getConsolidatedBy(),
            resolveUserName(shift.getConsolidatedBy()),
            shift.getConsolidatedAt(),
            shift.getConsolidationNotes()
        );
    }

    private CashShiftDto toDto(CashShift shift, BigDecimal expectedCash) {
        return new CashShiftDto(
            shift.getId(),
            shift.getTenantId(),
            shift.getBranchId(),
            shift.getOpenedBy(),
            resolveUserName(shift.getOpenedBy()),
            shift.getClosedBy(),
            resolveUserName(shift.getClosedBy()),
            shift.getOpenedAt(),
            shift.getClosedAt(),
            shift.getInitialCash(),
            shift.getTotalCashSales(),
            shift.getTotalCardSales(),
            shift.getTotalOtherSales(),
            shift.getTotalCashExpenses(),
            shift.getTotalCashRefunds() != null ? shift.getTotalCashRefunds() : BigDecimal.ZERO,
            expectedCash != null ? expectedCash : shift.getExpectedCash(),
            shift.getActualCash(),
            shift.getDifference(),
            shift.getStatus(),
            shift.getConsolidatedBy(),
            resolveUserName(shift.getConsolidatedBy()),
            shift.getConsolidatedAt(),
            shift.getConsolidationNotes()
        );
    }

    public CashShiftDto consolidateShift(UUID shiftId, String notes) {
        UUID tenantId = currentTenantId();
        UUID adminId = currentUserId();

        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Turno de caja", shiftId));

        if (!"CLOSED".equalsIgnoreCase(shift.getStatus())) {
            throw new BusinessRuleException("Solo se pueden consolidar turnos en estado CERRADO");
        }

        shift.consolidate(adminId, notes);
        CashShift saved = cashShiftRepository.save(shift);
        return toDto(saved, saved.getExpectedCash());
    }

    public CashShiftDto reopenShift(UUID shiftId) {
        UUID tenantId = currentTenantId();

        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Turno de caja", shiftId));

        if (!"CONSOLIDATED".equalsIgnoreCase(shift.getStatus())) {
            throw new BusinessRuleException("Solo se pueden reabrir turnos en estado CONSOLIDADO");
        }

        shift.reopenToClosed();
        CashShift saved = cashShiftRepository.save(shift);
        return toDto(saved, saved.getExpectedCash());
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userPublicApi.findUserSummary(userId)
            .map(u -> (u.fullName() != null && !u.fullName().isBlank()) ? u.fullName() : u.username())
            .orElse("Usuario");
    }
}
