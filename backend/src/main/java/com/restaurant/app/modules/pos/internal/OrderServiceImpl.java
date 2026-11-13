package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ShiftRequiredException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.entitlements.api.QuotaResource;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.core.security.api.UserPublicApi;
import com.restaurant.app.modules.catalog.api.CatalogPublicApi;
import com.restaurant.app.modules.catalog.api.DailyMenuPublicApi;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.company.api.BranchPublicApi;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import com.restaurant.app.modules.finance.api.CashShiftDto;
import com.restaurant.app.modules.finance.api.FinancePublicApi;
import com.restaurant.app.modules.finance.api.ShiftAuthorizationDto;
import com.restaurant.app.modules.finance.api.ValidatePinResponse;
import com.restaurant.app.modules.pos.api.CancelOrderRequest;
import com.restaurant.app.modules.pos.api.CreateOrderItemRequest;
import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.ModifierItemSummary;
import com.restaurant.app.modules.pos.api.OrderCancelledEvent;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.OrderItemDto;
import com.restaurant.app.modules.pos.api.OrderItemModifierDto;
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
    private final BranchPublicApi branchPublicApi;
    private final CatalogPublicApi catalogPublicApi;
    private final ApplicationEventPublisher eventPublisher;
    private final PromotionRepository promotionRepository;
    private final PromotionEngine promotionEngine;
    private final com.restaurant.app.modules.finance.api.FinancePublicApi financePublicApi;
    private final DailyMenuPublicApi dailyMenuPublicApi;
    private final UserPublicApi userPublicApi;
    private final ConfigurationPublicApi configurationPublicApi;
    private final TenantEntitlementService tenantEntitlementService;

    OrderServiceImpl(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        BranchPublicApi branchPublicApi,
        CatalogPublicApi catalogPublicApi,
        ApplicationEventPublisher eventPublisher,
        PromotionRepository promotionRepository,
        PromotionEngine promotionEngine,
        com.restaurant.app.modules.finance.api.FinancePublicApi financePublicApi,
        DailyMenuPublicApi dailyMenuPublicApi,
        UserPublicApi userPublicApi,
        ConfigurationPublicApi configurationPublicApi,
        TenantEntitlementService tenantEntitlementService
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.branchPublicApi = branchPublicApi;
        this.catalogPublicApi = catalogPublicApi;
        this.eventPublisher = eventPublisher;
        this.promotionRepository = promotionRepository;
        this.promotionEngine = promotionEngine;
        this.financePublicApi = financePublicApi;
        this.dailyMenuPublicApi = dailyMenuPublicApi;
        this.userPublicApi = userPublicApi;
        this.configurationPublicApi = configurationPublicApi;
        this.tenantEntitlementService = tenantEntitlementService;
    }

    record CreateOrderResult(OrderDto order, boolean isNew) {
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
            throw new UnauthorizedOperationException("Contexto de usuario requerido para registrar órdenes");
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

        // 1.1. Entitlements de plataforma (bloqueos operativos o exceso sin resolver tras downgrade)
        tenantEntitlementService.assertPosAndShiftOperationsAllowed(tenantId);

        // 2. Validaciones básicas
        if (request.branchId() == null) {
            throw new ValidationException("El branchId es obligatorio");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ValidationException("La orden debe contener al menos un ítem");
        }

        // 2.1. Cuota mensual de órdenes según zona horaria local de la sucursal (ADR-0006)
        ZoneId branchZoneId = getBranchZoneId(request.branchId());
        LocalDate branchToday = LocalDate.now(branchZoneId);
        Instant monthStart = branchToday.withDayOfMonth(1).atStartOfDay(branchZoneId).toInstant();
        Instant nextMonthStart = branchToday.withDayOfMonth(1).plusMonths(1).atStartOfDay(branchZoneId).toInstant();
        long currentMonthOrders = orderRepository.countActiveOrdersInRange(tenantId, monthStart, nextMonthStart);
        tenantEntitlementService.assertQuotaAvailable(tenantId, QuotaResource.MONTHLY_ORDERS, currentMonthOrders + 1);

        // 3. Método de pago y estado
        String paymentMethod = null;
        String orderStatus = "WAITING";
        Instant closedAt = null;

        if (request.paymentMethod() != null && !request.paymentMethod().isBlank()) {
            paymentMethod = request.paymentMethod().trim().toUpperCase();
            if (!VALID_PAYMENT_METHODS.contains(paymentMethod)) {
                throw new ValidationException(
                    "Método de pago inválido: " + request.paymentMethod() + ". Valores permitidos: " + VALID_PAYMENT_METHODS
                );
            }
            orderStatus = "PAID";
            closedAt = Instant.now();
        }

        // 3.1. Invariante Estricto de Turno Abierto en POS (ADR-0013 / PB-041)
        Optional<CashShiftDto> activeShiftOpt = financePublicApi.findActiveShift(tenantId, request.branchId());
        if ("PAID".equals(orderStatus) || "IN_PREPARATION".equals(orderStatus) || activeShiftOpt.isEmpty()) {
            if (activeShiftOpt.isEmpty() || !"OPEN".equalsIgnoreCase(activeShiftOpt.get().status())) {
                throw new ShiftRequiredException();
            }
        }
        UUID cashShiftId = activeShiftOpt.get().id();

        // 4. Generación de ticket_number secuencial diario por sucursal
        String ticketNumber = generateDailyTicketNumber(tenantId, request.branchId());

        // 5. Cálculo de totales y creación de ítems
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItemRequest itemReq : request.items()) {
            if (itemReq.dishId() == null) {
                throw new ValidationException("El dishId es obligatorio para todos los ítems");
            }
            if (itemReq.quantity() <= 0) {
                throw new ValidationException("La cantidad debe ser mayor a 0");
            }
            if (itemReq.unitPrice() == null || itemReq.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("El precio unitario no puede ser nulo ni negativo");
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
                .orElseThrow(() -> new EntityNotFoundException("Promoción", request.appliedPromotionId()));
            if (!promotionEngine.isPromotionActiveNow(promo, now, branchTimezone, request.branchId())) {
                throw new BusinessRuleException("La promoción solicitada no está vigente en este horario para la sucursal");
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

        // 5.2. Invariante de Control Atómico de Cupos de Menú Diario (ADR-0015 / PB-034)
        java.util.Map<UUID, Integer> dishQuantities = new java.util.HashMap<>();
        for (CreateOrderItemRequest itemReq : request.items()) {
            dishQuantities.merge(itemReq.dishId(), itemReq.quantity(), Integer::sum);
        }
        for (java.util.Map.Entry<UUID, Integer> entry : dishQuantities.entrySet()) {
            dailyMenuPublicApi.enforceAndDecrementQuota(cashShiftId, entry.getKey(), entry.getValue());
        }

        // 5.3. Adición de Costo de Envío (Delivery Fee - ADR-0020)
        BigDecimal deliveryFee = request.deliveryFee() != null ? request.deliveryFee().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (deliveryFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El costo de envío no puede ser negativo");
        }
        totalAmount = totalAmount.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        // 6. Persistencia atómica de la orden y sus ítems
        String orderType = request.orderType() != null && !request.orderType().isBlank()
            ? request.orderType().trim().toUpperCase()
            : "DINE_IN";

        if ("DELIVERY".equalsIgnoreCase(orderType)) {
            if (request.deliveryAddress() == null || request.deliveryAddress().trim().isBlank()) {
                throw new ValidationException("La dirección de entrega es obligatoria para órdenes de tipo DELIVERY");
            }
        }

        String customerName = request.customerName() != null ? request.customerName().trim() : null;
        String customerPhone = request.customerPhone() != null ? request.customerPhone().trim() : null;
        String deliveryAddress = request.deliveryAddress() != null ? request.deliveryAddress().trim() : null;
        String deliveryNotes = request.deliveryNotes() != null ? request.deliveryNotes().trim() : null;

        Order order = new Order(
            tenantId,
            request.branchId(),
            cashShiftId,
            ticketNumber,
            orderStatus,
            paymentMethod,
            totalAmount,
            clientTxId,
            request.notes(),
            userId,
            closedAt,
            orderType,
            customerName,
            customerPhone,
            deliveryAddress,
            deliveryFee,
            deliveryNotes
        );
        order.setCashShiftId(cashShiftId);

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        // 7. Registro de venta en turno activo y publicación de OrderPaidEvent
        if ("PAID".equals(savedOrder.getOrderStatus())) {
            financePublicApi.recordSale(
                savedOrder.getTenantId(),
                savedOrder.getBranchId(),
                savedOrder.getCashShiftId(),
                savedOrder.getPaymentMethod(),
                savedOrder.getTotalAmount()
            );
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
        return getOrders(branchId, date, null, null);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(UUID branchId, LocalDate date, UUID cashShiftId, UUID cashierId) {
        UUID tenantId = currentTenantId();

        if (cashShiftId != null) {
            if (cashierId != null) {
                return orderRepository.findByTenantIdAndCashShiftIdAndCreatedByOrderByCreatedAtDesc(
                    tenantId, cashShiftId, cashierId
                ).stream().map(this::toOrderDto).toList();
            }
            return orderRepository.findByTenantIdAndCashShiftIdOrderByCreatedAtDesc(
                tenantId, cashShiftId
            ).stream().map(this::toOrderDto).toList();
        }

        if (branchId != null && date != null) {
            ZoneId zoneId = getBranchZoneId(branchId);
            Instant start = date.atStartOfDay(zoneId).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
            return orderRepository.findByTenantIdAndBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                tenantId, branchId, start, end
            ).stream()
            .filter(o -> cashierId == null || cashierId.equals(o.getCreatedBy()))
            .map(this::toOrderDto).toList();
        } else if (branchId != null) {
            return orderRepository.findByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId)
                .stream()
                .filter(o -> cashierId == null || cashierId.equals(o.getCreatedBy()))
                .map(this::toOrderDto).toList();
        } else if (date != null) {
            ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
            Instant start = date.atStartOfDay(zoneId).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
            return orderRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, start, end)
                .stream()
                .filter(o -> cashierId == null || cashierId.equals(o.getCreatedBy()))
                .map(this::toOrderDto).toList();
        } else {
            return orderRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .filter(o -> cashierId == null || cashierId.equals(o.getCreatedBy()))
                .map(this::toOrderDto).toList();
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
        String tz = branchPublicApi.getBranchTimezone(branchId);
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

    @Override
    @Transactional(readOnly = true)
    public long countActiveOrdersInRange(UUID tenantId, Instant startInclusive, Instant endExclusive) {
        if (tenantId == null || startInclusive == null || endExclusive == null) {
            return 0L;
        }
        return orderRepository.countActiveOrdersInRange(tenantId, startInclusive, endExclusive);
    }

    private OrderDto toOrderDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
            .map(item -> {
                String dishName = catalogPublicApi.findDishById(item.getDishId())
                    .map(DishDto::name)
                    .orElse("Plato");
                List<OrderItemModifierDto> modDtos = item.getModifiers() != null
                    ? item.getModifiers().stream()
                        .map(m -> new OrderItemModifierDto(
                            m.getModifierId(),
                            m.getModifierName(),
                            m.getUnitPrice(),
                            m.getIngredientId(),
                            m.getQuantityConsumed()
                        ))
                        .toList()
                    : List.of();

                return new OrderItemDto(
                    item.getId(),
                    item.getDishId(),
                    dishName,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getNotes(),
                    item.getDiscountAmount(),
                    item.getComboId(),
                    modDtos
                );
            })
            .toList();

        String cashierName = resolveUserName(order.getCreatedBy());

        return new OrderDto(
            order.getId(),
            order.getTenantId(),
            order.getBranchId(),
            order.getCashShiftId(),
            order.getCreatedBy(),
            cashierName,
            order.getTicketNumber(),
            order.getOrderStatus(),
            order.getPaymentMethod(),
            order.getTotalAmount(),
            order.getClientTransactionId(),
            order.getNotes(),
            order.getCreatedAt(),
            order.getClosedAt(),
            itemDtos,
            order.getOrderType() != null ? order.getOrderType() : "DINE_IN",
            order.getCustomerName(),
            order.getCustomerPhone(),
            order.getDeliveryAddress(),
            order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO,
            order.getDeliveryNotes(),
            order.getCancellationReason(),
            order.getCancellationNotes(),
            order.getCancelledAt(),
            order.getCancelledBy(),
            order.getCancellationAuthorizedBy(),
            order.getRefundMethod(),
            order.getWastePolicyApplied()
        );
    }

    @Override
    public OrderDto cancelOrder(UUID tenantId, UUID orderId, CancelOrderRequest request, UUID currentUserId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (!"PAID".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BusinessRuleException("Solo se pueden anular órdenes en estado PAGADO (PAID)");
        }

        UUID authorizedBy = null;
        if (request.supervisorPin() != null && !request.supervisorPin().isBlank()) {
            ValidatePinResponse pinResp = financePublicApi.validateSupervisorPin(tenantId, order.getBranchId(), request.supervisorPin());
            if (!pinResp.valid()) {
                throw new ValidationException("El PIN de supervisor ingresado es inválido o no cuenta con privilegios de autorización");
            }
            authorizedBy = pinResp.supervisorId();
        } else if (request.authorizationRequestId() != null) {
            ShiftAuthorizationDto auth = financePublicApi.getAuthorization(tenantId, request.authorizationRequestId())
                .orElseThrow(() -> new ValidationException("Solicitud de autorización no encontrada"));
            if (!"APPROVED".equalsIgnoreCase(auth.status())) {
                throw new ValidationException("La solicitud de autorización aún no ha sido aprobada");
            }
            authorizedBy = auth.decidedBy();
        } else {
            throw new ValidationException("Debe proporcionar el PIN del supervisor o el ID de una solicitud de autorización aprobada");
        }

        String wastePolicy;
        if (request.wasPrepared()) {
            String configVal = configurationPublicApi.getString(
                SystemParameter.POS_CANCELLATION_PREPARED_WASTE_POLICY,
                order.getBranchId(),
                tenantId
            );
            wastePolicy = configVal != null && !configVal.isBlank() ? configVal : "ALLOW_AUTO_BUFFER_REUSE";
        } else {
            wastePolicy = "CANCELLATION_REVERSAL";
        }

        String refundMethod = request.refundMethod() != null ? request.refundMethod().trim().toUpperCase() : "CASH";
        if ("CASH".equals(refundMethod)) {
            financePublicApi.recordRefund(tenantId, order.getBranchId(), order.getCashShiftId(), order.getTotalAmount());
        }

        order.cancel(
            request.cancellationReason(),
            request.cancellationNotes(),
            refundMethod,
            wastePolicy,
            currentUserId,
            authorizedBy
        );

        Order saved = orderRepository.save(order);

        List<OrderItemSummary> summaries = saved.getItems().stream()
            .map(item -> {
                List<ModifierItemSummary> modSummaries = item.getModifiers() != null
                    ? item.getModifiers().stream()
                        .map(m -> new ModifierItemSummary(
                            m.getModifierId(),
                            m.getModifierName(),
                            m.getIngredientId(),
                            m.getQuantityConsumed()
                        ))
                        .toList()
                    : List.of();
                return new OrderItemSummary(
                    item.getDishId(),
                    "",
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getComboId(),
                    List.of(),
                    modSummaries
                );
            })
            .toList();

        eventPublisher.publishEvent(new OrderCancelledEvent(
            UUID.randomUUID(),
            saved.getTenantId(),
            saved.getBranchId(),
            saved.getId(),
            saved.getCashShiftId(),
            saved.getTicketNumber(),
            saved.getTotalAmount(),
            saved.getRefundMethod(),
            request.wasPrepared(),
            wastePolicy,
            summaries,
            saved.getCancelledAt(),
            saved.getCancelledBy(),
            saved.getCancellationAuthorizedBy()
        ));

        return toOrderDto(saved);
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userPublicApi.findUserSummary(userId)
            .map(u -> (u.fullName() != null && !u.fullName().isBlank()) ? u.fullName() : u.username())
            .orElse("Usuario");
    }
}
