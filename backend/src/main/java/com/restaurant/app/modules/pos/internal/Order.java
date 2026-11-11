package com.restaurant.app.modules.pos.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "cash_shift_id")
    private UUID cashShiftId;

    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber;

    @Column(name = "order_status", nullable = false, length = 20)
    private String orderStatus = "WAITING";

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "client_transaction_id", nullable = false, unique = true)
    private UUID clientTransactionId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "order_type", length = 20)
    private String orderType = "DINE_IN";

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "cancellation_reason", length = 30)
    private String cancellationReason;

    @Column(name = "cancellation_notes", columnDefinition = "TEXT")
    private String cancellationNotes;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancellation_authorized_by")
    private UUID cancellationAuthorizedBy;

    @Column(name = "refund_method", length = 20)
    private String refundMethod;

    @Column(name = "waste_policy_applied", length = 30)
    private String wastePolicyApplied;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    Order() {
    }

    Order(UUID tenantId, UUID branchId, UUID cashShiftId, String ticketNumber,
          String orderStatus, String paymentMethod, BigDecimal totalAmount,
          UUID clientTransactionId, String notes, UUID createdBy, Instant closedAt) {
        this(tenantId, branchId, cashShiftId, ticketNumber, orderStatus, paymentMethod, totalAmount, clientTransactionId, notes, createdBy, closedAt, "DINE_IN", null);
    }

    Order(UUID tenantId, UUID branchId, UUID cashShiftId, String ticketNumber,
          String orderStatus, String paymentMethod, BigDecimal totalAmount,
          UUID clientTransactionId, String notes, UUID createdBy, Instant closedAt,
          String orderType, String customerName) {
        this(tenantId, branchId, cashShiftId, ticketNumber, orderStatus, paymentMethod, totalAmount, clientTransactionId, notes, createdBy, closedAt, orderType, customerName, null, null, BigDecimal.ZERO, null);
    }

    Order(UUID tenantId, UUID branchId, UUID cashShiftId, String ticketNumber,
          String orderStatus, String paymentMethod, BigDecimal totalAmount,
          UUID clientTransactionId, String notes, UUID createdBy, Instant closedAt,
          String orderType, String customerName, String customerPhone, String deliveryAddress,
          BigDecimal deliveryFee, String deliveryNotes) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.cashShiftId = cashShiftId;
        this.ticketNumber = ticketNumber;
        this.orderStatus = orderStatus != null ? orderStatus : "WAITING";
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.clientTransactionId = clientTransactionId;
        this.notes = notes;
        this.createdBy = createdBy;
        this.closedAt = closedAt;
        this.orderType = orderType != null ? orderType : "DINE_IN";
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.deliveryAddress = deliveryAddress;
        this.deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        this.deliveryNotes = deliveryNotes;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getCashShiftId() {
        return cashShiftId;
    }

    public void setCashShiftId(UUID cashShiftId) {
        this.cashShiftId = cashShiftId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public UUID getClientTransactionId() {
        return clientTransactionId;
    }

    public void setClientTransactionId(UUID clientTransactionId) {
        this.clientTransactionId = clientTransactionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUserId() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public void cancel(String reason, String notes, String refundMethod, String wastePolicy, UUID cancelledBy, UUID authorizedBy) {
        if (!"PAID".equalsIgnoreCase(this.orderStatus)) {
            throw new com.restaurant.app.core.common.exception.BusinessRuleException("Solo se pueden anular órdenes en estado PAGADO (PAID)");
        }
        this.orderStatus = "CANCELLED";
        this.cancellationReason = reason;
        this.cancellationNotes = notes;
        this.refundMethod = refundMethod;
        this.wastePolicyApplied = wastePolicy;
        this.cancelledBy = cancelledBy;
        this.cancellationAuthorizedBy = authorizedBy;
        this.cancelledAt = Instant.now();
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationNotes() {
        return cancellationNotes;
    }

    public void setCancellationNotes(String cancellationNotes) {
        this.cancellationNotes = cancellationNotes;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public UUID getCancellationAuthorizedBy() {
        return cancellationAuthorizedBy;
    }

    public void setCancellationAuthorizedBy(UUID cancellationAuthorizedBy) {
        this.cancellationAuthorizedBy = cancellationAuthorizedBy;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getWastePolicyApplied() {
        return wastePolicyApplied;
    }

    public void setWastePolicyApplied(String wastePolicyApplied) {
        this.wastePolicyApplied = wastePolicyApplied;
    }
}
