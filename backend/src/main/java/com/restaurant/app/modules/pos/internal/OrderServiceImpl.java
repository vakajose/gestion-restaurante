package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.common.domain.Branch;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.ModifierItemSummary;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.OrderItemDto;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import com.restaurant.app.modules.pos.api.SelectedModifierRequest;
import com.restaurant.app.modules.pos.api.SyncOrdersRequest;
import com.restaurant.app.modules.pos.api.SyncOrdersResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import com.restaurant.app.modules.pos.api.PosPublicApi;
import com.restaurant.app.modules.pos.api.ShiftSalesSummaryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
class OrderServiceImpl implements PosPublicApi {

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("CASH", "CARD", "QR", "TRANSFER");
    private static final String DEFAULT_TIMEZONE = "America/La_Paz";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final ApplicationEventPublisher eventPublisher;
    private final PromotionRepository promotionRepository;
    private final PromotionEngine promotionEngine;

    OrderServiceImpl(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        BranchRepository branchRepository,
        CatalogPublicApi catalogPublicApi,
        ApplicationEventPublisher eventPublisher,
        PromotionRepository promotionRepository,
        PromotionEngine promotionEngine
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.branchRepository = branchRepository;
        this.catalogPublicApi = catalogPublicApi;
        this.eventPublisher = eventPublisher;
        this.promotionRepository = promotionRepository;
        this.promotionEngine = promotionEngine;
    }

    record CreateOrderResult(OrderDto order, boolean isNew) {
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
            throw new IllegalStateException("Contexto de usuario requerido para registrar órdenes");
        }
        return userId;
    }

    @Override
    public OrderDto placeOrder(CreateOrderRequest request) {
        return createOrder(request).order();
    }

    public CreateOrderResult createOrder(CreateOrderRequest request) {
        UUID tenantId = currentTenantId();
        UUID userId = currentUserId();

        UUID clientTxId = request.clientTransactionId() != null
            ? request.clientTransactionId()
            : UUID.randomUUID();

        // 1. Idempotencia estricta: si ya existe por clientTransactionId para el tenant, retornar la existente
        Optional<Order> existingOpt = orderRepository.findByTenantIdAndClientTransactionId(tenantId, clientTxId);
        if (existingOpt.isPresent()) {
            return new CreateOrderResult(toOrderDto(existingOpt.get()), false);
        }

        // 2. Validaciones básicas
        if (request.branchId() == null) {
            throw new IllegalArgumentException("El branchId es obligatorio");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un ítem");
        }

        // 3. Método de pago y estado
        String paymentMethod = null;
        String orderStatus = "WAITING";
        Instant closedAt = null;

        if (request.paymentMethod() != null && !request.paymentMethod().isBlank()) {
            paymentMethod = request.paymentMethod().trim().toUpperCase();
            if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) {
                throw new IllegalArgumentException(
                    "Método de pago inválido: " + request.paymentMethod() + ". Valores permitidos: " + VALID_PAYMENT_METHODS
                );
            }
            orderStatus = "PAID";
            closedAt = Instant.now();
        }

        // 4. Generación de ticket_number secuencial diario por sucursal
        String ticketNumber = generateDailyTicketNumber(tenantId, request.branchId());

        // 5. Cálculo de totales y creación de ítems
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItemRequest itemReq : request.items()) {
            if (itemReq.dishId() == null) {
                throw new IllegalArgumentException("El dishId es obligatorio para todos los ítems");
            }
            if (itemReq.quantity() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio unitario no puede ser nulo ni negativo");
            }

            BigDecimal unitPrice = itemReq.unitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal extraModifiersPrice = BigDecimal.ZERO;
            List<OrderItemModifier> itemModifiers = new ArrayList<>();

            if (itemReq.selectedModifiers() != null) {
                for (SelectedModifierRequest modReq : itemReq.selectedModifiers()) {
                    BigDecimal modPrice = modReq.unitPrice() != null ? modReq.unitPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    extraModifiersPrice = extraModifiersPrice.add(modPrice);
                    OrderItemModifier oim = new OrderItemModifier(
                        tenantId,
                        request.branchId(),
                        modReq.modifierId(),
                        modReq.name(),
                        modPrice,
                        modReq.ingredientId(),
                        modReq.quantityConsumed()
                    );
                    itemModifiers.add(oim);
                }
            }

            BigDecimal effectiveUnitPrice = unitPrice.add(extraModifiersPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = effectiveUnitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem(
                tenantId,
                request.branchId(),
                itemReq.dishId(),
                itemReq.quantity(),
                effectiveUnitPrice,
                subtotal,
                itemReq.notes(),
                itemReq.comboId()
            );

            for (OrderItemModifier oim : itemModifiers) {
                item.addModifier(oim);
            }
            orderItems.add(item);
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        // 5.1. Evaluación y aplicación de promociones temporales (ADR-0006)
        String branchTimezone = getBranchZoneId(request.branchId()).getId();
        Instant now = Instant.now();
        Promotion applicablePromo = null;

        if (request.appliedPromotionId() != null) {
            Promotion promo = promotionRepository.findByIdAndTenantId(request.appliedPromotionId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada: " + request.appliedPromotionId()));
            if (!promotionEngine.isPromotionActiveNow(promo, now, branchTimezone, request.branchId())) {
                throw new IllegalArgumentException("La promoción solicitada no está vigente en este horario para la sucursal");
            }
            applicablePromo = promo;
        } else {
            List<Promotion> activePromos = promotionRepository.findActiveWithBranchesByTenantId(tenantId);
            for (Promotion promo : activePromos) {
                if ("AUTOMATIC".equalsIgnoreCase(promo.getApplicationMode()) &&
                    promotionEngine.isPromotionActiveNow(promo, now, branchTimezone, request.branchId())) {
                    applicablePromo = promo;
                    break;
                }
            }
        }

        if (applicablePromo != null) {
            BigDecimal minOrder = applicablePromo.getMinOrderAmount() != null ? applicablePromo.getMinOrderAmount() : BigDecimal.ZERO;
            if (totalAmount.compareTo(minOrder) >= 0) {
                String scope = applicablePromo.getScope();
                if ("GLOBAL".equalsIgnoreCase(scope)) {
                    BigDecimal globalDiscount = BigDecimal.ZERO;
                    if ("PERCENTAGE".equalsIgnoreCase(applicablePromo.getDiscountType())) {
                        BigDecimal rate = applicablePromo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        for (OrderItem item : orderItems) {
                            BigDecimal itemDisc = item.getSubtotal().multiply(rate).setScale(2, RoundingMode.HALF_UP);
                            item.setDiscountAmount(itemDisc);
                            globalDiscount = globalDiscount.add(itemDisc);
                        }
                    } else if ("FIXED_AMOUNT".equalsIgnoreCase(applicablePromo.getDiscountType())) {
                        BigDecimal remaining = applicablePromo.getDiscountValue().min(totalAmount);
                        globalDiscount = remaining;
                        for (OrderItem item : orderItems) {
                            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                            BigDecimal d = item.getSubtotal().min(remaining);
                            item.setDiscountAmount(d);
                            remaining = remaining.subtract(d);
                        }
                    }
                    totalAmount = totalAmount.subtract(globalDiscount);
                } else {
                    // CATEGORY o DISH
                    BigDecimal itemsDiscount = BigDecimal.ZERO;
                    for (OrderItem item : orderItems) {
                        UUID catId = catalogPublicApi.findDishById(item.getDishId()).map(DishDto::categoryId).orElse(null);
                        BigDecimal d = promotionEngine.calculateItemDiscount(applicablePromo, item.getDishId(), catId, item.getSubtotal());
                        if (d.compareTo(BigDecimal.ZERO) > 0) {
                            item.setDiscountAmount(d);
                            itemsDiscount = itemsDiscount.add(d);
                        }
                    }
                    totalAmount = totalAmount.subtract(itemsDiscount);
                }
            }
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 6. Persistencia atómica de la orden y sus ítems
        Order order = new Order(
            tenantId,
            request.branchId(),
            request.cashShiftId(),
            ticketNumber,
            orderStatus,
            paymentMethod,
            totalAmount,
            clientTxId,
            request.notes(),
            userId,
            closedAt
        );

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        // 7. Publicación de OrderPaidEvent si la orden está en estado PAID
        if ("PAID".equals(savedOrder.getOrderStatus())) {
            publishOrderPaidEvent(savedOrder);
        }

        return new CreateOrderResult(toOrderDto(savedOrder), true);
    }

    public SyncOrdersResponse syncOrders(SyncOrdersRequest request) {
        if (request == null || request.orders() == null || request.orders().isEmpty()) {
            return new SyncOrdersResponse(List.of(), 0, 0);
        }

        List<CreateOrderRequest> orderRequests = request.orders();
        int totalReceived = orderRequests.size();
        List<OrderDto> syncedOrders = new ArrayList<>();

        for (CreateOrderRequest orderReq : orderRequests) {
            CreateOrderResult result = createOrder(orderReq);
            syncedOrders.add(result.order());
        }

        return new SyncOrdersResponse(syncedOrders, totalReceived, syncedOrders.size());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(UUID branchId, LocalDate date) {
        UUID tenantId = currentTenantId();

        if (branchId != null && date != null) {
            ZoneId zoneId = getBranchZoneId(branchId);
            Instant start = date.atStartOfDay(zoneId).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
            return orderRepository.findByTenantIdAndBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                tenantId, branchId, start, end
            ).stream().map(this::toOrderDto).toList();
        } else if (branchId != null) {
            return orderRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId)
                .stream().map(this::toOrderDto).toList();
        } else if (date != null) {
            ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
            Instant start = date.atStartOfDay(zoneId).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
            return orderRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end)
                .stream().map(this::toOrderDto).toList();
        } else {
            return orderRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toOrderDto).toList();
        }
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID orderId) {
        UUID tenantId = currentTenantId();
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Orden no encontrada con ID: " + orderId));
        return toOrderDto(order);
    }

    private String generateDailyTicketNumber(UUID tenantId, UUID branchId) {
        ZoneId zoneId = getBranchZoneId(branchId);
        LocalDate today = LocalDate.now(zoneId);
        Instant startOfDay = today.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant();

        long countToday = orderRepository.countByTenantIdAndBranchIdAndCreatedAtBetween(
            tenantId, branchId, startOfDay, endOfDay
        );
        return String.format("T-%04d", 1000 + countToday + 1);
    }

    private ZoneId getBranchZoneId(UUID branchId) {
        String tz = branchRepository.findById(branchId)
            .map(Branch::getTimezone)
            .orElse(DEFAULT_TIMEZONE);
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private void publishOrderPaidEvent(Order order) {
        List<OrderItemSummary> summaries = order.getItems().stream()
            .map(item -> {
                String dishName = catalogPublicApi.findDishById(item.getDishId())
                    .map(DishDto::name)
                    .orElse("Plato");

                List<UUID> componentDishIds = List.of();
                if (item.getComboId() != null) {
                    componentDishIds = catalogPublicApi.findComboById(item.getComboId())
                        .map(combo -> combo.items().stream()
                            .filter(ci -> ci.defaultDishId() != null)
                            .map(ci -> ci.defaultDishId())
                            .toList())
                        .orElse(List.of());
                }

                List<ModifierItemSummary> modSummaries = item.getModifiers().stream()
                    .map(m -> new ModifierItemSummary(m.getModifierId(), m.getModifierName(), m.getIngredientId(), m.getQuantityConsumed()))
                    .toList();

                return new OrderItemSummary(
                    item.getDishId(),
                    dishName,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getComboId(),
                    componentDishIds,
                    modSummaries
                );
            })
            .toList();

        OrderPaidEvent event = new OrderPaidEvent(
            UUID.randomUUID(),
            order.getTenantId(),
            order.getBranchId(),
            order.getId(),
            order.getClientTransactionId(),
            order.getTicketNumber(),
            summaries,
            order.getTotalAmount(),
            order.getPaymentMethod(),
            order.getClosedAt() != null ? order.getClosedAt() : Instant.now(),
            order.getCashShiftId()
        );

        eventPublisher.publishEvent(event);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftSalesSummaryDto getShiftSalesSummary(UUID tenantId, UUID cashShiftId) {
        if (tenantId == null || cashShiftId == null) {
            return new ShiftSalesSummaryDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        List<Order> orders = orderRepository.findByTenantIdAndCashShiftId(tenantId, cashShiftId);
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        BigDecimal other = BigDecimal.ZERO;
        int count = 0;

        for (Order o : orders) {
            if ("PAID".equalsIgnoreCase(o.getOrderStatus())) {
                count++;
                BigDecimal amt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                String method = o.getPaymentMethod() != null ? o.getPaymentMethod().toUpperCase() : "";
                if ("CASH".equals(method)) {
                    cash = cash.add(amt);
                } else if ("CARD".equals(method)) {
                    card = card.add(amt);
                } else if ("QR".equals(method) || "TRANSFER".equals(method)) {
                    other = other.add(amt);
                }
            }
        }
        return new ShiftSalesSummaryDto(
            cash.setScale(2, RoundingMode.HALF_UP),
            card.setScale(2, RoundingMode.HALF_UP),
            other.setScale(2, RoundingMode.HALF_UP),
            count
        );
    }

    private OrderDto toOrderDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
            .map(item -> {
                String dishName = catalogPublicApi.findDishById(item.getDishId())
                    .map(DishDto::name)
                    .orElse("Plato");
                return new OrderItemDto(
                    item.getId(),
                    item.getDishId(),
                    dishName,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getNotes(),
                    item.getDiscountAmount(),
                    item.getComboId()
                );
            })
            .toList();

        return new OrderDto(
            order.getId(),
            order.getTenantId(),
            order.getBranchId(),
            order.getCashShiftId(),
            order.getTicketNumber(),
            order.getOrderStatus(),
            order.getPaymentMethod(),
            order.getTotalAmount(),
            order.getClientTransactionId(),
            order.getNotes(),
            order.getCreatedAt(),
            order.getClosedAt(),
            itemDtos
        );
    }
}
