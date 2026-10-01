package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CashShiftSummaryDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import com.restaurant.app.modules.pos.api.ShiftSalesSummaryDto;
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
    private final PosPublicApi posPublicApi;
    private final AppUserRepository appUserRepository;

    CashShiftService(
        CashShiftRepository cashShiftRepository,
        ExpenseRepository expenseRepository,
        PosPublicApi posPublicApi,
        AppUserRepository appUserRepository
    ) {
        this.cashShiftRepository = cashShiftRepository;
        this.expenseRepository = expenseRepository;
        this.posPublicApi = posPublicApi;
        this.appUserRepository = appUserRepository;
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Contexto de tenant requerido para esta operación");
        }
        return tenantId;
    }

    private UUID currentUserId() {
        UUID userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new IllegalStateException("Contexto de usuario requerido para operar la caja");
        }
        return userId;
    }

    public CashShiftDto openShift(OpenShiftRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        if (request.branchId() == null) {
            throw new IllegalArgumentException("El branchId es obligatorio para abrir un turno");
        }

        // Validar que no exista un turno abierto en la misma sucursal
        Optional<CashShift> openShiftOpt = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
            tenantId, request.branchId(), "OPEN"
        );
        if (openShiftOpt.isPresent()) {
            throw new IllegalStateException("Ya existe un turno de caja abierto en esta sucursal");
        }

        BigDecimal initialCash = request.initialCash() != null
            ? request.initialCash().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        CashShift shift = new CashShift(tenantId, request.branchId(), userId, initialCash);
        CashShift saved = cashShiftRepository.save(shift);
        return toDto(saved, initialCash);
    }

    @Transactional(readOnly = true)
    public Optional<CashShiftDto> getCurrentShift(UUID branchId) {
        UUID tenantId = currentTenantId();
        if (branchId == null) {
            return Optional.empty();
        }

        return cashShiftRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN")
            .map(this::enrichActiveShiftDto);
    }

    public CashShiftDto closeShift(UUID shiftId, CloseShiftRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        CashShift shift = cashShiftRepository.findByIdAndTenantId(shiftId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Turno de caja no encontrado con ID: " + shiftId));

        if (!"OPEN".equalsIgnoreCase(shift.getStatus())) {
            throw new IllegalStateException("El turno de caja ya se encuentra cerrado");
        }

        // 1. Obtener ventas consolidadas desde POS
        ShiftSalesSummaryDto salesSummary = posPublicApi.getShiftSalesSummary(tenantId, shift.getId());

        BigDecimal totalCashSales = salesSummary.totalCashSales() != null
            ? salesSummary.totalCashSales()
            : shift.getTotalCashSales();
        BigDecimal totalCardSales = salesSummary.totalCardSales() != null
            ? salesSummary.totalCardSales()
            : shift.getTotalCardSales();
        BigDecimal totalOtherSales = salesSummary.totalOtherSales() != null
            ? salesSummary.totalOtherSales()
            : shift.getTotalOtherSales();

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
        ShiftSalesSummaryDto posSummary = posPublicApi.getShiftSalesSummary(shiftDto.tenantId(), shiftId);
        int approvedCount = expenseRepository.countByTenantIdAndCashShiftIdAndApprovalStatus(
            shiftDto.tenantId(), shiftId, "APPROVED"
        );
        int pendingCount = expenseRepository.countByTenantIdAndCashShiftIdAndApprovalStatus(
            shiftDto.tenantId(), shiftId, "PENDING_APPROVAL"
        );

        return new CashShiftSummaryDto(
            shiftDto,
            posSummary.totalOrders(),
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
        UUID tenantId = shift.getTenantId();
        ShiftSalesSummaryDto summary = posPublicApi.getShiftSalesSummary(tenantId, shift.getId());

        BigDecimal cashSales = summary.totalCashSales() != null && summary.totalCashSales().compareTo(BigDecimal.ZERO) > 0
            ? summary.totalCashSales()
            : shift.getTotalCashSales();
        BigDecimal cardSales = summary.totalCardSales() != null && summary.totalCardSales().compareTo(BigDecimal.ZERO) > 0
            ? summary.totalCardSales()
            : shift.getTotalCardSales();
        BigDecimal otherSales = summary.totalOtherSales() != null && summary.totalOtherSales().compareTo(BigDecimal.ZERO) > 0
            ? summary.totalOtherSales()
            : shift.getTotalOtherSales();

        List<Expense> approvedExpenses = expenseRepository
            .findByTenantIdAndCashShiftIdAndPaidFromCashDrawerTrueAndApprovalStatus(
                tenantId, shift.getId(), "APPROVED"
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
        return appUserRepository.findById(userId)
            .map(AppUser::getUsername)
            .orElse("Usuario");
    }
}
