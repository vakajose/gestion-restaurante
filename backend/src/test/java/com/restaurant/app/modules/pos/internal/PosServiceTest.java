package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.domain.Tenant;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.common.repository.TenantRepository;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.TenantContext;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import com.restaurant.app.modules.pos.api.SyncOrdersRequest;
import com.restaurant.app.modules.pos.api.SyncOrdersResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Transactional
class PosServiceTest {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private ApplicationEvents applicationEvents;

    private Tenant testTenant;
    private Branch testBranch;
    private AppUser testUser;
    private UUID testDishId1;
    private UUID testDishId2;

    @BeforeEach
    void setUp() {
        testTenant = tenantRepository.save(new Tenant("POS Test Tenant", "NIT-" + UUID.randomUUID()));
        testBranch = branchRepository.save(new Branch(testTenant.getId(), "Sucursal POS", "America/La_Paz"));
        testUser = appUserRepository.save(new AppUser(
            testTenant.getId(),
            testBranch.getId(),
            "cashier_" + UUID.randomUUID(),
            "cashier@test.com",
            "dummy_hash",
            "CASHIER"
        ));

        // Use active menu or random dish UUIDs for testing
        testDishId1 = UUID.randomUUID();
        testDishId2 = UUID.randomUUID();

        TenantContextHolder.set(new TenantContext(
            testTenant.getId(),
            testBranch.getId(),
            testUser.getId(),
            testUser.getUsername(),
            testUser.getRole()
        ));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Debe crear orden exitosa con cálculo correcto de subtotales, total y ticket secuencial")
    void shouldCreateOrderSuccessfully() {
        UUID clientTxId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
            clientTxId,
            testBranch.getId(),
            null,
            "CASH",
            "Mesa 5",
            List.of(
                new CreateOrderItemRequest(testDishId1, 2, new BigDecimal("15.50"), "Sin cebolla"),
                new CreateOrderItemRequest(testDishId2, 1, new BigDecimal("10.00"), null)
            )
        );

        OrderServiceImpl.CreateOrderResult result = orderService.createOrder(request);

        assertThat(result.isNew()).isTrue();
        OrderDto order = result.order();
        assertThat(order.id()).isNotNull();
        assertThat(order.ticketNumber()).isEqualTo("T-1001");
        assertThat(order.orderStatus()).isEqualTo("PAID");
        assertThat(order.paymentMethod()).isEqualTo("CASH");
        assertThat(order.clientTransactionId()).isEqualTo(clientTxId);
        assertThat(order.totalAmount()).isEqualByComparingTo("41.00");
        assertThat(order.items()).hasSize(2);
        assertThat(order.items().get(0).subtotal()).isEqualByComparingTo("31.00");
        assertThat(order.items().get(1).subtotal()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Idempotencia estricta: enviar dos veces la misma orden con el mismo clientTransactionId retorna la misma orden sin duplicar")
    void shouldBeStrictlyIdempotent() {
        UUID clientTxId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
            clientTxId,
            testBranch.getId(),
            null,
            "CARD",
            "Pago rápido",
            List.of(new CreateOrderItemRequest(testDishId1, 1, new BigDecimal("25.00"), null))
        );

        OrderServiceImpl.CreateOrderResult firstResult = orderService.createOrder(request);
        assertThat(firstResult.isNew()).isTrue();

        long initialOrdersCount = orderRepository.count();

        // Segunda llamada idéntica
        OrderServiceImpl.CreateOrderResult secondResult = orderService.createOrder(request);
        assertThat(secondResult.isNew()).isFalse();
        assertThat(secondResult.order().id()).isEqualTo(firstResult.order().id());
        assertThat(secondResult.order().ticketNumber()).isEqualTo(firstResult.order().ticketNumber());

        // Asegurar que no se crearon registros duplicados en la base de datos
        assertThat(orderRepository.count()).isEqualTo(initialOrdersCount);
    }

    @Test
    @DisplayName("Debe publicar OrderPaidEvent con todos los datos requeridos cuando la orden está pagada")
    void shouldPublishOrderPaidEventWhenOrderIsPaid() {
        UUID clientTxId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
            clientTxId,
            testBranch.getId(),
            null,
            "QR",
            "Pago QR móvil",
            List.of(
                new CreateOrderItemRequest(testDishId1, 3, new BigDecimal("12.00"), "Extra picante")
            )
        );

        orderService.createOrder(request);

        long eventCount = applicationEvents.stream(OrderPaidEvent.class)
            .filter(e -> e.clientTransactionId().equals(clientTxId))
            .count();
        assertThat(eventCount).isEqualTo(1);

        OrderPaidEvent event = applicationEvents.stream(OrderPaidEvent.class)
            .filter(e -> e.clientTransactionId().equals(clientTxId))
            .findFirst()
            .orElseThrow();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.tenantId()).isEqualTo(testTenant.getId());
        assertThat(event.branchId()).isEqualTo(testBranch.getId());
        assertThat(event.orderId()).isNotNull();
        assertThat(event.ticketNumber()).isEqualTo("T-1001");
        assertThat(event.totalAmount()).isEqualByComparingTo("36.00");
        assertThat(event.paymentMethod()).isEqualTo("QR");
        assertThat(event.paidAt()).isNotNull();
        assertThat(event.items()).hasSize(1);
        assertThat(event.items().getFirst().quantity()).isEqualTo(3);
        assertThat(event.items().getFirst().subtotal()).isEqualByComparingTo("36.00");
    }

    @Test
    @DisplayName("No debe publicar OrderPaidEvent si la orden no tiene método de pago (estado WAITING)")
    void shouldNotPublishEventWhenOrderIsNotPaid() {
        UUID clientTxId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
            clientTxId,
            testBranch.getId(),
            null,
            null, // sin método de pago
            "Pedido pendiente de pago",
            List.of(new CreateOrderItemRequest(testDishId1, 1, new BigDecimal("20.00"), null))
        );

        OrderServiceImpl.CreateOrderResult result = orderService.createOrder(request);
        assertThat(result.order().orderStatus()).isEqualTo("WAITING");
        assertThat(result.order().closedAt()).isNull();

        long eventCount = applicationEvents.stream(OrderPaidEvent.class)
            .filter(e -> e.clientTransactionId().equals(clientTxId))
            .count();
        assertThat(eventCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Debe generar tickets secuenciales T-1001, T-1002 para órdenes en el mismo día")
    void shouldGenerateSequentialTicketNumbers() {
        CreateOrderRequest req1 = new CreateOrderRequest(
            UUID.randomUUID(), testBranch.getId(), null, "CASH", null,
            List.of(new CreateOrderItemRequest(testDishId1, 1, new BigDecimal("10.00"), null))
        );
        CreateOrderRequest req2 = new CreateOrderRequest(
            UUID.randomUUID(), testBranch.getId(), null, "CASH", null,
            List.of(new CreateOrderItemRequest(testDishId2, 1, new BigDecimal("15.00"), null))
        );

        OrderDto order1 = orderService.createOrder(req1).order();
        OrderDto order2 = orderService.createOrder(req2).order();

        assertThat(order1.ticketNumber()).isEqualTo("T-1001");
        assertThat(order2.ticketNumber()).isEqualTo("T-1002");
    }

    @Test
    @DisplayName("Batch sync: procesa lista de órdenes offline de Dexie.js aplicando idempotencia")
    void shouldSyncBatchOfOfflineOrders() {
        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();
        UUID tx3 = UUID.randomUUID();

        // Orden 1 ya fue sincronizada previamente
        CreateOrderRequest req1 = new CreateOrderRequest(
            tx1, testBranch.getId(), null, "CASH", "Previa",
            List.of(new CreateOrderItemRequest(testDishId1, 1, new BigDecimal("10.00"), null))
        );
        OrderDto existingOrder = orderService.createOrder(req1).order();

        // Lote offline con la orden 1 (reintento) + órdenes 2 y 3 nuevas
        CreateOrderRequest req2 = new CreateOrderRequest(
            tx2, testBranch.getId(), null, "CARD", "Nueva 1",
            List.of(new CreateOrderItemRequest(testDishId1, 2, new BigDecimal("10.00"), null))
        );
        CreateOrderRequest req3 = new CreateOrderRequest(
            tx3, testBranch.getId(), null, "TRANSFER", "Nueva 2",
            List.of(new CreateOrderItemRequest(testDishId2, 1, new BigDecimal("25.00"), null))
        );

        SyncOrdersRequest syncRequest = new SyncOrdersRequest(List.of(req1, req2, req3));
        SyncOrdersResponse syncResponse = orderService.syncOrders(syncRequest);

        assertThat(syncResponse.totalReceived()).isEqualTo(3);
        assertThat(syncResponse.totalProcessed()).isEqualTo(3);
        assertThat(syncResponse.syncedOrders()).hasSize(3);

        // La orden 1 debe mantener su ID original
        OrderDto synced1 = syncResponse.syncedOrders().stream()
            .filter(o -> o.clientTransactionId().equals(tx1))
            .findFirst().orElseThrow();
        assertThat(synced1.id()).isEqualTo(existingOrder.id());
    }

    @Test
    @DisplayName("Debe rechazar órdenes sin ítems o con método de pago inválido")
    void shouldRejectInvalidOrders() {
        // Sin ítems
        CreateOrderRequest emptyItemsReq = new CreateOrderRequest(
            UUID.randomUUID(), testBranch.getId(), null, "CASH", null, List.of()
        );
        assertThatThrownBy(() -> orderService.createOrder(emptyItemsReq))
            .isInstanceOf(IllegalArgumentException.class);

        // Método de pago no soportado
        CreateOrderRequest invalidPaymentReq = new CreateOrderRequest(
            UUID.randomUUID(), testBranch.getId(), null, "BITCOIN", null,
            List.of(new CreateOrderItemRequest(testDishId1, 1, new BigDecimal("10.00"), null))
        );
        assertThatThrownBy(() -> orderService.createOrder(invalidPaymentReq))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Método de pago inválido");
    }
}
