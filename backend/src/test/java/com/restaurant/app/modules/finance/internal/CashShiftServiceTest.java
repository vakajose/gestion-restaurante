package com.restaurant.app.modules.finance.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.SystemPermissions;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CloseShiftRequest;
import com.restaurant.app.modules.finance.api.CreateExpenseRequest;
import com.restaurant.app.modules.finance.api.ExpenseApprovalRequest;
import com.restaurant.app.modules.finance.api.ExpenseDto;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.finance.api.OrderAuditSummaryDto;
import com.restaurant.app.modules.finance.api.ShiftAuditDetailDto;
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
    private ConfigurationPublicApi configurationPublicApi;

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
            .isInstanceOf(ResourceConflictException.class)
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

    @Test
    @DisplayName("Debe requerir permiso finance:shifts_supervisor cuando la política es SUPERVISOR_ONLY")
    void shouldEnforceShiftOpeningPolicyWhenSupervisorOnly() {
        configurationPublicApi.setOverride(
            SystemParameter.FINANCE_SHIFT_OPENING_POLICY,
            ParameterScope.BRANCH,
            tenantId,
            branchId,
            "SUPERVISOR_ONLY",
            adminId,
            "Activar política supervisor"
        );

        // Cajero sin permiso de supervisor intenta abrir turno -> 403 / UnauthorizedOperationException
        TenantContextHolder.set(new TenantContext(tenantId, branchId, cashierId, "cajero1", "CASHIER"));
        assertThatThrownBy(() -> cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00"))))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessageContaining("Caja Cerrada — Esperando apertura de turno por parte de un supervisor");

        // Usuario con permiso de supervisor abre turno con éxito
        TenantContextHolder.set(new TenantContext(
            tenantId, branchId, cashierId, "supervisor1", "SUPERVISOR", null,
            java.util.Set.of(SystemPermissions.FINANCE_SHIFTS_SUPERVISOR)
        ));
        CashShiftDto shift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        assertThat(shift.status()).isEqualTo("OPEN");

        configurationPublicApi.resetOverride(
            SystemParameter.FINANCE_SHIFT_OPENING_POLICY,
            ParameterScope.BRANCH,
            tenantId,
            branchId
        );
    }

    @Test
    @DisplayName("Debe rechazar gastos de gaveta que superen el límite finance.max_petty_cash_expense (100.00)")
    void shouldRejectPettyCashExpenseExceedingLimitWhenPaidFromDrawer() {
        CashShiftDto shift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("200.00")));

        // Gasto pagado de gaveta que excede 100.00 -> Rechazado con ValidationException (HTTP 400)
        CreateExpenseRequest exceedingRequest = new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Compra de suministros mayor",
            new BigDecimal("150.00"),
            true, // pagado de gaveta
            "https://evidence.com/receipt.jpg",
            LocalDate.now()
        );
        assertThatThrownBy(() -> expenseService.createExpense(exceedingRequest))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("supera el límite máximo permitido para caja menor");

        // Gasto mayor a 100.00 pero NO pagado de gaveta (ej. transferencia bancaria) -> Permitido
        CreateExpenseRequest validBankRequest = new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Pago de servicio por transferencia",
            new BigDecimal("150.00"),
            false, // NO de gaveta
            "https://evidence.com/receipt.jpg",
            LocalDate.now()
        );
        ExpenseDto validExpense = expenseService.createExpense(validBankRequest);
        assertThat(validExpense).isNotNull();
        assertThat(validExpense.amount()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Debe aplicar dual-control (PENDING_APPROVAL) para usuario con finance:expenses_approve si self-approval es false, y autoaprobar si es true o ADMIN_TENANT")
    void shouldEnforceExpenseSelfApprovalPolicy() {
        CashShiftDto shift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("200.00")));

        // Contexto de Branch Manager con permiso finance:expenses_approve
        TenantContext managerContext = new TenantContext(
            tenantId, branchId, cashierId, "manager1", "BRANCH_MANAGER", null,
            java.util.Set.of(SystemPermissions.FINANCE_EXPENSES_APPROVE)
        );

        // 1. Con self-approval = false (por defecto): queda en PENDING_APPROVAL
        TenantContextHolder.set(managerContext);
        ExpenseDto pendingExpense = expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Gasto por manager",
            new BigDecimal("40.00"),
            true,
            null,
            LocalDate.now()
        ));
        assertThat(pendingExpense.approvalStatus()).isEqualTo("PENDING_APPROVAL");

        // 2. Con ADMIN_TENANT: autoaprueba directamente
        TenantContextHolder.set(new TenantContext(tenantId, branchId, adminId, "admin", "ADMIN_TENANT"));
        ExpenseDto adminExpense = expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Gasto por admin",
            new BigDecimal("50.00"),
            true,
            null,
            LocalDate.now()
        ));
        assertThat(adminExpense.approvalStatus()).isEqualTo("APPROVED");

        // 3. Con self-approval activado (true): manager autoaprueba directamente
        configurationPublicApi.setOverride(
            SystemParameter.FINANCE_EXPENSE_ALLOW_SELF_APPROVAL,
            ParameterScope.BRANCH,
            tenantId,
            branchId,
            "true",
            adminId,
            "Permitir autoaprobación"
        );
        TenantContextHolder.set(managerContext);
        ExpenseDto selfApprovedExpense = expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Gasto con autoaprobación permitida",
            new BigDecimal("60.00"),
            true,
            null,
            LocalDate.now()
        ));
        assertThat(selfApprovedExpense.approvalStatus()).isEqualTo("APPROVED");

        configurationPublicApi.resetOverride(
            SystemParameter.FINANCE_EXPENSE_ALLOW_SELF_APPROVAL,
            ParameterScope.BRANCH,
            tenantId,
            branchId
        );
    }

    @Test
    @DisplayName("Debe permitir múltiples turnos abiertos concurrentes en modo INDIVIDUAL_PER_CASHIER")
    void shouldSupportIndividualCashierShiftsWhenConfigured() {
        // Configurar modalidad INDIVIDUAL_PER_CASHIER en la sucursal
        configurationPublicApi.setOverride(
            SystemParameter.FINANCE_SHIFT_CONCURRENCY_MODE,
            ParameterScope.BRANCH,
            tenantId,
            branchId,
            "INDIVIDUAL_PER_CASHIER",
            adminId,
            "Habilitar turnos independientes por cajero"
        );

        // Crear segundo cajero
        AppUser cashier2 = new AppUser(tenantId, branchId, "cajero2", "cajero2@restaurant.com", "hash", "CASHIER");
        cashier2 = appUserRepository.save(cashier2);
        UUID cashier2Id = cashier2.getId();

        // 1. Cajero 1 abre su turno
        TenantContextHolder.set(new TenantContext(tenantId, branchId, cashierId, "cajero1", "CASHIER"));
        CashShiftDto shift1 = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        assertThat(shift1).isNotNull();
        assertThat(shift1.status()).isEqualTo("OPEN");

        // 2. Cajero 2 abre su propio turno en la misma sucursal
        TenantContextHolder.set(new TenantContext(tenantId, branchId, cashier2Id, "cajero2", "CASHIER"));
        CashShiftDto shift2 = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("150.00")));
        assertThat(shift2).isNotNull();
        assertThat(shift2.status()).isEqualTo("OPEN");
        assertThat(shift2.id()).isNotEqualTo(shift1.id());

        // 3. Cajero 1 no puede abrir otro turno mientras tenga uno abierto
        TenantContextHolder.set(new TenantContext(tenantId, branchId, cashierId, "cajero1", "CASHIER"));
        assertThatThrownBy(() -> cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("50.00"))))
            .isInstanceOf(ResourceConflictException.class)
            .hasMessageContaining("Ya tienes un turno de caja abierto");

        // 4. getActiveShifts debe retornar ambos turnos abiertos
        List<CashShiftDto> activeShifts = cashShiftService.getActiveShifts(branchId);
        assertThat(activeShifts).hasSize(2);

        // Reset
        configurationPublicApi.resetOverride(
            SystemParameter.FINANCE_SHIFT_CONCURRENCY_MODE,
            ParameterScope.BRANCH,
            tenantId,
            branchId
        );
    }

    @Test
    @DisplayName("Debe filtrar turnos históricos por fechas, estado y tipo de discrepancia (TASK-508 / PB-030)")
    void shouldFilterShiftsByDateRangeTimezoneAndDiscrepancy() {
        TenantContextHolder.set(new TenantContext(tenantId, branchId, adminId, "admin", "ADMIN_TENANT"));

        // 1. Crear turno CUADRADO (esperado 100, real 100 -> diff 0)
        CashShiftDto shiftCuadrado = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        cashShiftService.closeShift(shiftCuadrado.id(), new CloseShiftRequest(new BigDecimal("100.00"), "Cierre exacto"));

        // 2. Crear turno FALTANTE (esperado 100, real 80 -> diff -20)
        CashShiftDto shiftFaltante = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        cashShiftService.closeShift(shiftFaltante.id(), new CloseShiftRequest(new BigDecimal("80.00"), "Faltó dinero"));

        // 3. Crear turno SOBRANTE (esperado 100, real 120 -> diff +20)
        CashShiftDto shiftSobrante = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("100.00")));
        cashShiftService.closeShift(shiftSobrante.id(), new CloseShiftRequest(new BigDecimal("120.00"), "Sobró dinero"));

        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        // Filtro FALTANTE
        List<CashShiftDto> faltantes = cashShiftService.getShiftsHistory(
            branchId, todayStr, todayStr, null, "CLOSED", "FALTANTE", 50
        );
        assertThat(faltantes).isNotEmpty();
        assertThat(faltantes).allMatch(s -> s.difference() != null && s.difference().compareTo(BigDecimal.ZERO) < 0);
        assertThat(faltantes.stream().anyMatch(s -> s.id().equals(shiftFaltante.id()))).isTrue();

        // Filtro SOBRANTE
        List<CashShiftDto> sobrantes = cashShiftService.getShiftsHistory(
            branchId, todayStr, todayStr, null, "CLOSED", "SOBRANTE", 50
        );
        assertThat(sobrantes).isNotEmpty();
        assertThat(sobrantes).allMatch(s -> s.difference() != null && s.difference().compareTo(BigDecimal.ZERO) > 0);
        assertThat(sobrantes.stream().anyMatch(s -> s.id().equals(shiftSobrante.id()))).isTrue();

        // Filtro CUADRADO
        List<CashShiftDto> cuadrados = cashShiftService.getShiftsHistory(
            branchId, todayStr, todayStr, null, "CLOSED", "CUADRADO", 50
        );
        assertThat(cuadrados).isNotEmpty();
        assertThat(cuadrados).allMatch(s -> s.difference() != null && s.difference().compareTo(BigDecimal.ZERO) == 0);
        assertThat(cuadrados.stream().anyMatch(s -> s.id().equals(shiftCuadrado.id()))).isTrue();
    }

    @Test
    @DisplayName("Debe recuperar el detalle de auditoría del turno con desglose y gastos (TASK-508 / PB-030)")
    void shouldRetrieveShiftAuditDetailWithSalesBreakdownAndOrders() {
        TenantContextHolder.set(new TenantContext(tenantId, branchId, adminId, "admin", "ADMIN_TENANT"));

        // 1. Abrir turno
        CashShiftDto shift = cashShiftService.openShift(new OpenShiftRequest(branchId, new BigDecimal("200.00")));

        // 2. Registrar egreso de caja menor (auto-aprobado por ser creado por admin)
        ExpenseDto expense = expenseService.createExpense(new CreateExpenseRequest(
            branchId,
            shift.id(),
            "DAILY_OPERATIONAL",
            "Compra de limones",
            new BigDecimal("25.50"),
            true,
            null,
            LocalDate.now()
        ));

        // 3. Simular ventas agregadas en el turno
        cashShiftService.updateRunningSales(tenantId, branchId, shift.id(), "CASH", new BigDecimal("150.00"));
        cashShiftService.updateRunningSales(tenantId, branchId, shift.id(), "CARD", new BigDecimal("80.00"));

        // 4. Obtener detalle de auditoría
        ShiftAuditDetailDto detail = cashShiftService.getShiftAuditDetail(shift.id());

        assertThat(detail).isNotNull();
        assertThat(detail.shift().id()).isEqualTo(shift.id());
        assertThat(detail.cashSales()).isEqualByComparingTo("150.00");
        assertThat(detail.cardSales()).isEqualByComparingTo("80.00");
        assertThat(detail.expenses()).hasSize(1);
        assertThat(detail.expenses().get(0).amount()).isEqualByComparingTo("25.50");
        assertThat(detail.orders()).isNotNull();
        assertThat(detail.refunds()).isNotNull();
    }
}
