package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CashShiftSummaryDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.CreateExpenseRequest;
import com.restaurant.app.modules.finance.api.ExpenseApprovalRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.finance.api.ShiftAuditDetailDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance")
class FinanceController {

    private final CashShiftService cashShiftService;
    private final ExpenseService expenseService;

    FinanceController(CashShiftService cashShiftService, ExpenseService expenseService) {
        this.cashShiftService = cashShiftService;
        this.expenseService = expenseService;
    }

    // ------------------------------------------------------------------------
    // Turnos de Caja (Cash Shifts)
    // ------------------------------------------------------------------------

    @PostMapping("/shifts/open")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> openShift(@Valid @RequestBody OpenShiftRequest request) {
        CashShiftDto shift = cashShiftService.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(shift);
    }

    @GetMapping("/shifts/current")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'pos:access') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> getCurrentShift(@RequestParam(required = false) UUID branchId) {
        return cashShiftService.getCurrentShift(branchId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/shifts/active")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'pos:access') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<List<CashShiftDto>> getActiveShifts(@RequestParam(required = false) UUID branchId) {
        List<CashShiftDto> shifts = cashShiftService.getActiveShifts(branchId);
        return ResponseEntity.ok(shifts);
    }

    @PostMapping("/shifts/{shiftId}/close")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> closeShift(
        @PathVariable UUID shiftId,
        @Valid @RequestBody CloseShiftRequest request
    ) {
        CashShiftDto shift = cashShiftService.closeShift(shiftId, request);
        return ResponseEntity.ok(shift);
    }

    @PostMapping("/shifts/{shiftId}/consolidate")
    @PreAuthorize("hasAnyAuthority('finance:shifts_supervisor') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> consolidateShift(
        @PathVariable UUID shiftId,
        @RequestBody(required = false) Map<String, String> body
    ) {
        String notes = body != null ? body.get("notes") : null;
        CashShiftDto shift = cashShiftService.consolidateShift(shiftId, notes);
        return ResponseEntity.ok(shift);
    }

    @PostMapping("/shifts/{shiftId}/reopen")
    @PreAuthorize("hasAnyAuthority('finance:shifts_supervisor') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> reopenShift(@PathVariable UUID shiftId) {
        CashShiftDto shift = cashShiftService.reopenShift(shiftId);
        return ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<List<CashShiftDto>> getShifts(@RequestParam(required = false) UUID branchId) {
        List<CashShiftDto> shifts = cashShiftService.getShifts(branchId);
        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/shifts/{shiftId}")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftDto> getShiftById(@PathVariable UUID shiftId) {
        CashShiftDto shift = cashShiftService.getShiftById(shiftId);
        return ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts/{shiftId}/summary")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<CashShiftSummaryDto> getShiftSummary(@PathVariable UUID shiftId) {
        CashShiftSummaryDto summary = cashShiftService.getShiftSummary(shiftId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/shifts/history")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<List<CashShiftDto>> getShiftsHistory(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) UUID cashierId,
        @RequestParam(required = false, defaultValue = "ALL") String status,
        @RequestParam(required = false, defaultValue = "ALL") String discrepancyType,
        @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        List<CashShiftDto> list = cashShiftService.getShiftsHistory(
            branchId, startDate, endDate, cashierId, status, discrepancyType, limit
        );
        return ResponseEntity.ok(list);
    }

    @GetMapping("/shifts/{shiftId}/audit-detail")
    @PreAuthorize("hasAnyAuthority('finance:shifts', 'finance:shifts_supervisor', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<ShiftAuditDetailDto> getShiftAuditDetail(@PathVariable UUID shiftId) {
        ShiftAuditDetailDto detail = cashShiftService.getShiftAuditDetail(shiftId);
        return ResponseEntity.ok(detail);
    }

    // ------------------------------------------------------------------------
    // Gastos Menores y Caja Chica (Expenses)
    // ------------------------------------------------------------------------

    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('finance:expenses_create') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseDto expense = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyAuthority('finance:expenses_create', 'finance:expenses_approve', 'finance:shifts', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<List<ExpenseDto>> getExpenses(
        @RequestParam(required = false) UUID branchId,
        @RequestParam(required = false) UUID cashShiftId,
        @RequestParam(required = false) String approvalStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ExpenseDto> list = expenseService.getExpenses(branchId, cashShiftId, approvalStatus, startDate, endDate);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/expenses/{expenseId}")
    @PreAuthorize("hasAnyAuthority('finance:expenses_create', 'finance:expenses_approve', 'finance:shifts', 'finance:reports') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<ExpenseDto> getExpenseById(@PathVariable UUID expenseId) {
        ExpenseDto expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense);
    }

    @PostMapping("/expenses/{expenseId}/approve")
    @PreAuthorize("hasAuthority('finance:expenses_approve') or hasAnyRole('ADMIN_TENANT', 'SUPERADMIN')")
    public ResponseEntity<ExpenseDto> approveExpense(
        @PathVariable UUID expenseId,
        @Valid @RequestBody ExpenseApprovalRequest request
    ) {
        ExpenseDto expense = expenseService.approveExpense(expenseId, request);
        return ResponseEntity.ok(expense);
    }
}
