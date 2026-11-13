package com.restaurant.app.modules.backoffice.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_payments")
class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "charge_id")
    private UUID chargeId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "BOB";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PlatformPaymentMethod paymentMethod;

    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "recorded_by_admin_id")
    private UUID recordedByAdminId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SubscriptionPayment() {
    }

    SubscriptionPayment(
        UUID tenantId,
        UUID chargeId,
        BigDecimal amount,
        String currency,
        PlatformPaymentMethod paymentMethod,
        String referenceCode,
        String receiptUrl,
        Instant paidAt,
        UUID recordedByAdminId,
        String notes
    ) {
        this.tenantId = tenantId;
        this.chargeId = chargeId;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "BOB";
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        this.receiptUrl = receiptUrl;
        this.paidAt = paidAt != null ? paidAt : Instant.now();
        this.recordedByAdminId = recordedByAdminId;
        this.notes = notes;
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    UUID getChargeId() {
        return chargeId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    PlatformPaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    String getReferenceCode() {
        return referenceCode;
    }

    String getReceiptUrl() {
        return receiptUrl;
    }

    Instant getPaidAt() {
        return paidAt;
    }

    UUID getRecordedByAdminId() {
        return recordedByAdminId;
    }

    String getNotes() {
        return notes;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
