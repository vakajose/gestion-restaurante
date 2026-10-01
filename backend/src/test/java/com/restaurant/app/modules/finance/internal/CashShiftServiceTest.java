package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.CreateExpenseRequest;
import com.restaurant.app.modules.finance.api.ExpenseApprovalRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CashShiftServiceTest {

    @Autowired
    private CashShiftService cashShiftService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private CashShiftRepository cashShiftRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private UUID tenantId;
    private UUID branchId;
    private UUID adminId;
    private UUID cashierId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByNitOrTaxId("12345678").orElseThrow();
        Branch branch = branchRepository.findByTenantId(tenant.getId()).get(0);
        AppUser admin = appUserRepository.findByTenantIdAndUsername(tenant.getId(), "admin").orElseThrow();
        AppUser cashier = appUserRepository.findByTenantIdAndUsername(tenant.getId(), "cajero1").orElse(admin);

        this.tenantId = tenant.getId();
        this.branchId = branch.getId();
        this.adminId = admin.getId();
        this.cashierId = cashier.getId();

        TenantContextHolder.set(new TenantContext(tenantId, branchId, adminId, "admin", "ADMIN_TENANT"));

        // Limpiar turnos existentes para pruebas aisladas
        cashShiftRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN")
            .ifPresent(s -> {
                s.setStatus("CLOSED");
                cashShiftRepository.save(s);
            });
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe abrir un turno de caja exitosamente con saldo inicial")
    void shouldOpenShiftSuccessfully() {
        OpenShiftRequest request = new OpenShiftRequest(branchId, new BigDecimal("150.00"));
        CashShiftDto shift = cashShiftService.openShift(request);

        assertThat(shift).isNotNull();
        assertThat(shift.id()).isNotNull();
        assertThat(shift.status()).isEqualTo("OPEN");
        assertThat(shift.initialCash()).isEqualByComparingTo("150.00");
        assertThat(shift.totalCashSales()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Debe rechazar la apertura de un segundo turno si ya existe uno abierto en la sucursal")
    void shouldFailWhenOpeningSecondShiftForSameBranch() {
        cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));

        assertThatThrownBy(() -> cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("200.00"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ya existe un turno de caja abierto en esta sucursal");
    }

    @Test
    @DisplayName("Debe calcular correctamente el arqueo de caja con fórmula expected_cash y diferencia")
    void shouldCalculateExpectedCashAndArqueoDifferenceOnClose() {
        // 1. Abrir turno con Bs. 200.00
        CashShiftDto openShift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("200.00")));
        UUID shiftId = openShift.id();

        // 2. Registrar venta en efectivo de Bs. 70.00
        posPublicApi.placeOrder(new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            shiftId,
            "CASH",
            "Mesa 1",
            List.of(new CreateOrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("35.00"), "Sin cebolla"))
        ));

        // 3. Registrar gasto de caja chica aprobado de Bs. 20.00
        expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shiftId,
            "DAILY_OPERATIONAL",
            "Compra de hielo",
            new BigDecimal("20.00"),
            true, // pagado de gaveta
            "https://evidence.com/receipt.jpg",
            LocalDate.now()
        ));

        // 4. Cerrar turno contando Bs. 248.00
        // expected_cash = 200.00 (inicial) + 70.00 (ventas efectivo) - 20.00 (gastos gaveta) = 250.00
        // actual_cash = 248.00
        // difference = 248.00 - 250.00 = -2.00 (faltante)
        CashShiftDto closedShift = cashShiftService.closeShift(shiftId, new CloseShiftRequest(new BigDecimal("248.00"), "Faltante 2 Bs"));

        assertThat(closedShift.status()).isEqualTo("CLOSED");
        assertThat(closedShift.totalCashSales()).isEqualByComparingTo("70.00");
        assertThat(closedShift.totalCashExpenses()).isEqualByComparingTo("20.00");
        assertThat(closedShift.expectedCash()).isEqualByComparingTo("250.00");
        assertThat(closedShift.actualCash()).isEqualByComparingTo("248.00");
        assertThat(closedShift.difference()).isEqualByComparingTo("-2.00");
    }

    @Test
    @DisplayName("Workflow de aprobación de gastos: cajero crea pendiente, manager aprueba y se descuenta del arqueo")
    void shouldEnforceExpenseApprovalWorkflow() {
        // 1. Abrir turno con Bs. 100.00
        CashShiftDto openShift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        UUID shiftId = openShift.id();

        // 2. Cajero registra gasto pagado de gaveta -> queda en PENDING_APPROVAL
        TenantContextHolder.set(new TenantContext(tenantId, branchId, cashierId, "cajero1", "CASHIER"));
        ExpenseDto pendingExpense = expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shiftId,
            "DAILY_OPERATIONAL",
            "Compra de papel toalla",
            new BigDecimal("15.00"),
            true,
            "https://receipt.com/img.png",
            LocalDate.now()
        ));

        assertThat(pendingExpense.approvalStatus()).isEqualTo("PENDING_APPROVAL");

        // Gasto pendiente NO debe descontarse del arqueo aún
        CashShiftDto currentBeforeApproval = cashShiftService.getCurrentShift(branchId).orElseThrow();
        assertThat(currentBeforeApproval.totalCashExpenses()).isEqualByComparingTo("0.00");
        assertThat(currentBeforeApproval.expectedCash()).isEqualByComparingTo("100.00");

        // 3. Manager aprueba el gasto
        TenantContextHolder.set(new TenantContext(tenantId, branchId, adminId, "admin", "ADMIN_TENANT"));
        ExpenseDto approvedExpense = expenseService.approveExpense(
            pendingExpense.id(),
            new ExpenseApprovalRequest("APPROVED", "Gasto legítimo")
        );

        assertThat(approvedExpense.approvalStatus()).isEqualTo("APPROVED");

        // Ahora el gasto SÍ se descuenta del efectivo esperado
        CashShiftDto currentAfterApproval = cashShiftService.getCurrentShift(branchId).orElseThrow();
        assertThat(currentAfterApproval.totalCashExpenses()).isEqualByComparingTo("15.00");
        assertThat(currentAfterApproval.expectedCash()).isEqualByComparingTo("85.00");
    }
}
