package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CashShiftSummaryDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.CreateExpenseRequest;
import com.restaurant.app.modules.finance.api.ExpenseApprovalRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
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
    public ResponseEntity<CashShiftDto> openShift(@Valid @RequestBody OpenShiftRequest request) {
        CashShiftDto shift = cashShiftService.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(shift);
    }

    @GetMapping("/shifts/current")
    public ResponseEntity<CashShiftDto> getCurrentShift(@RequestParam(required = false) UUID branchId) {
        return cashShiftService.getCurrentShift(branchId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/shifts/{shiftId}/close")
    public ResponseEntity<CashShiftDto> closeShift(
        @PathVariable UUID shiftId,
        @Valid @RequestBody CloseShiftRequest request
    ) {
        CashShiftDto shift = cashShiftService.closeShift(shiftId, request);
        return ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts")
    public ResponseEntity<List<CashShiftDto>> getShifts(@RequestParam(required = false) UUID branchId) {
        List<CashShiftDto> shifts = cashShiftService.getShifts(branchId);
        return ResponseEntity.ok(shifts);
    }

    @GetMapping("/shifts/{shiftId}")
    public ResponseEntity<CashShiftDto> getShiftById(@PathVariable UUID shiftId) {
        CashShiftDto shift = cashShiftService.getShiftById(shiftId);
        return ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts/{shiftId}/summary")
    public ResponseEntity<CashShiftSummaryDto> getShiftSummary(@PathVariable UUID shiftId) {
        CashShiftSummaryDto summary = cashShiftService.getShiftSummary(shiftId);
        return ResponseEntity.ok(summary);
    }

    // ------------------------------------------------------------------------
    // Gastos Menores y Caja Chica (Expenses)
    // ------------------------------------------------------------------------

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        ExpenseDto expense = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping("/expenses")
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
    public ResponseEntity<ExpenseDto> getExpenseById(@PathVariable UUID expenseId) {
        ExpenseDto expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense);
    }

    @PostMapping("/expenses/{expenseId}/approve")
    public ResponseEntity<ExpenseDto> approveExpense(
        @PathVariable UUID expenseId,
        @Valid @RequestBody ExpenseApprovalRequest request
    ) {
        ExpenseDto expense = expenseService.approveExpense(expenseId, request);
        return ResponseEntity.ok(expense);
    }
}
