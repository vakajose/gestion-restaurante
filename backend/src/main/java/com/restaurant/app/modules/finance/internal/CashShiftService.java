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
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CashShiftSummaryDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

    CashShiftService(
        CashShiftRepository cashShiftRepository,
        ExpenseRepository expenseRepository,
        UserPublicApi userPublicApi,
        ConfigurationPublicApi configurationPublicApi,
        DailyMenuPublicApi dailyMenuPublicApi
    ) {
        this.cashShiftRepository = cashShiftRepository;
        this.expenseRepository = expenseRepository;
        this.userPublicApi = userPublicApi;
        this.configurationPublicApi = configurationPublicApi;
        this.dailyMenuPublicApi = dailyMenuPublicApi;
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
        return cashShiftRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN")
            .map(this::enrichActiveShiftDto);
    }

    @Transactional(readOnly = true)
    public Optional<CashShiftDto> findActiveShiftForUser(UUID tenantId, UUID branchId, UUID userId) {
        if (tenantId == null || branchId == null || userId == null) {
            return Optional.empty();
        }
        return cashShiftRepository.findByTenantIdAndBranchIdAndOpenedByAndStatus(tenantId, branchId, userId, "OPEN")
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

        // 3. Fórmula del arqueo de caja (ADR / SDD):
        // expected_cash = initial_cash + total_cash_sales - total_cash_expenses_approved
        BigDecimal initialCash = shift.getInitialCash() != null ? shift.getInitialCash() : BigDecimal.ZERO;
        BigDecimal expectedCash = initialCash.add(totalCashSales).subtract(totalCashExpenses).setScale(2, RoundingMode.HALF_UP);

        BigDecimal actualCash = request.actualCash() != null
            ? request.actualCash().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal difference = actualCash.subtract(expectedCash).setScale(2, RoundingMode.HALF_UP);

        // 4. Actualizar y cerrar turno
        shift.setTotalCashSales(totalCashSales);
        shift.setTotalCardSales(totalCardSales);
        shift.setTotalOtherSales(totalOtherSales);
        shift.setTotalCashExpenses(totalCashExpenses);
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

        BigDecimal expectedCash = shift.getInitialCash().add(cashSales).subtract(cashExpenses).setScale(2, RoundingMode.HALF_UP);

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
            expectedCash,
            shift.getActualCash(),
            shift.getDifference(),
            shift.getStatus()
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
            expectedCash != null ? expectedCash : shift.getExpectedCash(),
            shift.getActualCash(),
            shift.getDifference(),
            shift.getStatus()
        );
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userPublicApi.findUserSummary(userId)
            .map(UserSummaryDto::username)
            .orElse("Usuario");
    }
}
