package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.finance.api.CreateExpenseRequest;
import com.restaurant.app.modules.finance.api.ExpenseApprovalRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
class ExpenseService {

    private static final Set<String> VALID_EXPENSE_TYPES = Set.of("DAILY_OPERATIONAL", "MONTHLY_FIXED_PRORATED");

    private final ExpenseRepository expenseRepository;
    private final CashShiftRepository cashShiftRepository;
    private final AppUserRepository appUserRepository;

    ExpenseService(
        ExpenseRepository expenseRepository,
        CashShiftRepository cashShiftRepository,
        AppUserRepository appUserRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.cashShiftRepository = cashShiftRepository;
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
            throw new IllegalStateException("Contexto de usuario requerido para registrar gastos");
        }
        return userId;
    }

    public ExpenseDto createExpense(CreateExpenseRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();
        String role = TenantContextHolder.getRole();

        if (request.branchId() == null) {
            throw new IllegalArgumentException("El branchId es obligatorio para registrar un gasto");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("La descripción del gasto es obligatoria");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser un valor positivo mayor a 0");
        }

        String expenseType = request.expenseType() != null ? request.expenseType().trim().toUpperCase() : "DAILY_OPERATIONAL";
        if (!VALID_EXPENSE_TYPES.contains(expenseType)) {
            throw new IllegalArgumentException(
                "Tipo de gasto inválido: " + request.expenseType() + ". Valores permitidos: " + VALID_EXPENSE_TYPES
            );
        }

        // Determinar turno de caja asociado
        UUID cashShiftId = request.cashShiftId();
        if (cashShiftId == null) {
            Optional<CashShift> activeShiftOpt = cashShiftRepository.findByTenantIdAndBranchIdAndStatus(
                tenantId, request.branchId(), "OPEN"
            );
            if (activeShiftOpt.isPresent()) {
                cashShiftId = activeShiftOpt.get().getId();
            }
        }

        // Regla de aprobación según SDD:
        // Si se paga de la caja chica gaveta (paidFromCashDrawer = true):
        //   - Cajero -> PENDING_APPROVAL (requiere evidencia o aprobación de encargado)
        //   - Admin/Manager -> APPROVED automáticamente
        // Si no se paga de la gaveta (transferencia/cuenta) -> APPROVED
        String approvalStatus = "APPROVED";
        UUID approvedBy = null;
        if (request.paidFromCashDrawer()) {
            if ("ADMIN_TENANT".equalsIgnoreCase(role) || "BRANCH_MANAGER".equalsIgnoreCase(role)) {
                approvalStatus = "APPROVED";
                approvedBy = userId;
            } else {
                approvalStatus = "PENDING_APPROVAL";
            }
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        LocalDate expenseDate = request.expenseDate() != null ? request.expenseDate() : LocalDate.now();

        Expense expense = new Expense(
            tenantId,
            request.branchId(),
            cashShiftId,
            expenseType,
            request.description().trim(),
            amount,
            request.paidFromCashDrawer(),
            approvalStatus,
            request.evidenceUrl(),
            expenseDate,
            userId
        );
        expense.setApprovedBy(approvedBy);

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    public ExpenseDto approveExpense(UUID expenseId, ExpenseApprovalRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        Expense expense = expenseRepository.findByIdAndTenantId(expenseId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Gasto no encontrado con ID: " + expenseId));

        if (!"PENDING_APPROVAL".equalsIgnoreCase(expense.getApprovalStatus())) {
            throw new IllegalStateException(
                "El gasto ya fue procesado anteriormente con estado: " + expense.getApprovalStatus()
            );
        }

        String newStatus = request.status().trim().toUpperCase();
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException("El estado debe ser APPROVED o REJECTED");
        }

        expense.setApprovalStatus(newStatus);
        expense.setApprovedBy(userId);
        expense.setApprovalNotes(request.approvalNotes());

        Expense updated = expenseRepository.save(expense);
        return toDto(updated);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpenses(
        UUID branchId,
        UUID cashShiftId,
        String approvalStatus,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UUID tenantId = currentTenantId();
        List<Expense> list;

        if (cashShiftId != null) {
            list = expenseRepository.findByTenantIdAndCashShiftIdOrderByCreatedAtDesc(tenantId, cashShiftId);
        } else if (branchId != null && startDate != null && endDate != null) {
            list = expenseRepository.findByTenantIdAndBranchIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                tenantId, branchId, startDate, endDate
            );
        } else if (branchId != null && approvalStatus != null && !approvalStatus.isBlank()) {
            list = expenseRepository.findByTenantIdAndBranchIdAndApprovalStatusOrderByCreatedAtDesc(
                tenantId, branchId, approvalStatus.toUpperCase()
            );
        } else if (branchId != null) {
            list = expenseRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId);
        } else if (approvalStatus != null && !approvalStatus.isBlank()) {
            list = expenseRepository.findByTenantIdAndApprovalStatusOrderByCreatedAtDesc(
                tenantId, approvalStatus.toUpperCase()
            );
        } else {
            list = expenseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        }

        return list.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDto getExpenseById(UUID expenseId) {
        UUID tenantId = currentTenantId();
        Expense expense = expenseRepository.findByIdAndTenantId(expenseId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Gasto no encontrado con ID: " + expenseId));
        return toDto(expense);
    }

    private ExpenseDto toDto(Expense expense) {
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

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return appUserRepository.findById(userId)
            .map(AppUser::getUsername)
            .orElse("Usuario");
    }
}
