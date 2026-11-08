package com.restaurant.app.modules.pos;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishPriceDto;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.CreateShiftAuthorizationRequest;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.OpenShiftRequest;
import com.restaurant.app.modules.finance.api.ShiftAuthorizationDto;
import com.restaurant.app.modules.inventory.api.InventoryPublicApi;
import com.restaurant.app.modules.inventory.api.KardexMovementDto;
import com.restaurant.app.modules.pos.api.CancelOrderRequest;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class OrderCancellationIntegrationTests {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private PosPublicApi posPublicApi;

    @Autowired
    private FinancePublicApi financePublicApi;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private InventoryPublicApi inventoryPublicApi;

    @Autowired
    private UserPublicApi userPublicApi;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID tenantId;
    private UUID branchId;
    private UUID cashShiftId;
    private UUID cashierId;
    private UUID supervisorId;
    private UUID testDishId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(new Tenant("Restaurante Cancel Test", "cancel-test"));
        tenantId = tenant.getId();

        Branch branch = branchRepository.save(new Branch(tenantId, "Sucursal Cancelaciones", "America/La_Paz"));
        branchId = branch.getId();

        // Crear supervisor con PIN
        AppUser supervisor = new AppUser(
            tenantId,
            branchId,
            "supervisor_branch",
            "supervisor@test.com",
            passwordEncoder.encode("superPass123"),
            "BRANCH_MANAGER"
        );
        supervisor.setPinHash(passwordEncoder.encode("4321"));
        supervisor = appUserRepository.save(supervisor);
        supervisorId = supervisor.getId();

        // Crear cajero
        AppUser cashier = new AppUser(
            tenantId,
            branchId,
            "cajero_test",
            "cajero@test.com",
            passwordEncoder.encode("cajaPass123"),
            "CASHIER"
        );
        cashier = appUserRepository.save(cashier);
        cashierId = cashier.getId();

        TenantContextHolder.set(new TenantContext(
            tenantId,
            branchId,
            cashierId,
            "cajero_test",
            "CASHIER"
        ));

        // Catálogo demo
        catalogPublicApi.seedDemoCatalog(tenantId, branchId, "FAST_FOOD");
        List<DishPriceDto> menu = catalogPublicApi.getActiveMenuForBranch(tenantId, branchId);
        testDishId = menu.get(0).dishId();

        // Abrir turno con saldo inicial de 100.00
        CashShiftDto shift = financePublicApi.openShift(new OpenShiftRequest(
            branchId,
            new BigDecimal("100.00")
        ));
        cashShiftId = shift.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe anular orden con PIN de supervisor in-situ y registrar egreso por devolución en caja (PB-031 / ADR-0019)")
    void shouldCancelOrderWithSupervisorPinAndRecordRefund() {
        // 1. Crear comanda pagada en efectivo
        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Comanda para cancelar",
            List.of(new CreateOrderItemRequest(testDishId, 2, new BigDecimal("25.00"), "Sin cebolla"))
        );
        OrderDto created = posPublicApi.placeOrder(orderReq);
        assertThat(created.orderStatus()).isEqualTo("PAID");
        BigDecimal orderAmount = created.totalAmount();

        // 2. Anular la orden mediante PIN de supervisor
        CancelOrderRequest cancelReq = new CancelOrderRequest(
            "CUSTOMER_RETRACTED",
            "Cliente no puede esperar el despacho",
            "CASH",
            false,
            "4321",
            null
        );
        OrderDto cancelled = posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId);

        // 3. Verificaciones de la orden
        assertThat(cancelled.orderStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationReason()).isEqualTo("CUSTOMER_RETRACTED");
        assertThat(cancelled.cancellationNotes()).isEqualTo("Cliente no puede esperar el despacho");
        assertThat(cancelled.refundMethod()).isEqualTo("CASH");
        assertThat(cancelled.cancellationAuthorizedBy()).isEqualTo(supervisorId);
        assertThat(cancelled.cancelledBy()).isEqualTo(cashierId);
        assertThat(cancelled.wastePolicyApplied()).isEqualTo("CANCELLATION_REVERSAL");

        // 4. Verificar impacto en finanzas y arqueo de caja
        CashShiftDto activeShift = financePublicApi.findActiveShift(tenantId, branchId).orElseThrow();
        assertThat(activeShift.totalCashRefunds()).isEqualByComparingTo(orderAmount);

        // Efectivo esperado = Saldo Inicial (100) + Ventas Efectivo (orderAmount) - Gastos (0) - Devoluciones (orderAmount) = 100.00
        BigDecimal expected = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);
        assertThat(activeShift.expectedCash()).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Debe rechazar cancelación cuando el PIN de supervisor es inválido")
    void shouldRejectCancellationWhenPinIsInvalid() {
        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Comanda para cancelar",
            List.of(new CreateOrderItemRequest(testDishId, 1, new BigDecimal("25.00"), null))
        );
        OrderDto created = posPublicApi.placeOrder(orderReq);

        CancelOrderRequest cancelReq = new CancelOrderRequest(
            "TYPING_ERROR",
            "Mesa equivocada",
            "CASH",
            false,
            "9999", // PIN incorrecto
            null
        );

        assertThatThrownBy(() -> posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("PIN de supervisor");
    }

    @Test
    @DisplayName("Debe anular orden mediante solicitud remota aprobada por el supervisor")
    void shouldCancelOrderWithApprovedRemoteAuthorization() {
        // 1. Crear comanda
        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "QR",
            "Comanda cobrada con QR",
            List.of(new CreateOrderItemRequest(testDishId, 1, new BigDecimal("25.00"), null))
        );
        OrderDto created = posPublicApi.placeOrder(orderReq);

        // 2. Crear solicitud de autorización de turno remota
        ShiftAuthorizationDto authReq = financePublicApi.createAuthorizationRequest(
            tenantId,
            cashierId,
            new CreateShiftAuthorizationRequest(
                branchId,
                cashShiftId,
                "ORDER_CANCELLATION",
                created.id(),
                created.totalAmount(),
                "Cliente pagó por QR pero se cayó el sistema"
            )
        );
        assertThat(authReq.status()).isEqualTo("PENDING");

        // 3. Intentar anular con solicitud pendiente debe fallar
        CancelOrderRequest cancelReq = new CancelOrderRequest(
            "KITCHEN_ISSUE",
            "Sin insumos",
            "DIGITAL",
            false,
            null,
            authReq.id()
        );
        assertThatThrownBy(() -> posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("no ha sido aprobada");

        // 4. El supervisor aprueba la solicitud
        financePublicApi.approveAuthorization(tenantId, supervisorId, authReq.id(), "Aprobado remotamente", "REMOTE_SESSION");

        // 5. Ahora la anulación debe proceder exitosamente
        OrderDto cancelled = posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId);
        assertThat(cancelled.orderStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationAuthorizedBy()).isEqualTo(supervisorId);
        assertThat(cancelled.refundMethod()).isEqualTo("DIGITAL");
    }

    @Test
    @DisplayName("Debe publicar OrderCancelledEvent con CANCELLATION_REVERSAL cuando el plato no fue preparado")
    void shouldPublishOrderCancelledEventWithReversalWhenNotPrepared() {
        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Comanda no cocinada",
            List.of(new CreateOrderItemRequest(testDishId, 1, new BigDecimal("25.00"), null))
        );
        OrderDto created = posPublicApi.placeOrder(orderReq);

        CancelOrderRequest cancelReq = new CancelOrderRequest(
            "CUSTOMER_RETRACTED",
            "Cliente se fue antes de cocinar",
            "CASH",
            false, // wasPrepared = false
            "4321",
            null
        );
        OrderDto cancelled = posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId);
        assertThat(cancelled.orderStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.wastePolicyApplied()).isEqualTo("CANCELLATION_REVERSAL");

        // Validar evento publicado en ApplicationEvents
        OrderCancelledEvent event = applicationEvents.stream(OrderCancelledEvent.class)
            .filter(e -> e.orderId().equals(created.id()))
            .findFirst()
            .orElseThrow();

        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.branchId()).isEqualTo(branchId);
        assertThat(event.wasPrepared()).isFalse();
        assertThat(event.wastePolicyApplied()).isEqualTo("CANCELLATION_REVERSAL");
        assertThat(event.authorizedBy()).isEqualTo(supervisorId);
        assertThat(event.refundMethod()).isEqualTo("CASH");
        assertThat(event.items()).hasSize(1);
    }

    @Test
    @DisplayName("Debe publicar OrderCancelledEvent con ALLOW_AUTO_BUFFER_REUSE cuando el plato fue preparado")
    void shouldPublishOrderCancelledEventWithBufferWhenPrepared() {
        CreateOrderRequest orderReq = new CreateOrderRequest(
            UUID.randomUUID(),
            branchId,
            cashShiftId,
            "CASH",
            "Comanda ya cocinada",
            List.of(new CreateOrderItemRequest(testDishId, 1, new BigDecimal("25.00"), null))
        );
        OrderDto created = posPublicApi.placeOrder(orderReq);

        CancelOrderRequest cancelReq = new CancelOrderRequest(
            "QUALITY_COMPLAINT",
            "Plato preparado pero cancelado",
            "CASH",
            true, // wasPrepared = true
            "4321",
            null
        );
        OrderDto cancelled = posPublicApi.cancelOrder(tenantId, created.id(), cancelReq, cashierId);
        assertThat(cancelled.orderStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.wastePolicyApplied()).isEqualTo("ALLOW_AUTO_BUFFER_REUSE");

        OrderCancelledEvent event = applicationEvents.stream(OrderCancelledEvent.class)
            .filter(e -> e.orderId().equals(created.id()))
            .findFirst()
            .orElseThrow();

        assertThat(event.wasPrepared()).isTrue();
        assertThat(event.wastePolicyApplied()).isEqualTo("ALLOW_AUTO_BUFFER_REUSE");
    }
}
