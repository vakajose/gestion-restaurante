package com.restaurant.app.modules.finance.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shift_authorizations")
class ShiftAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "cash_shift_id")
    private UUID cashShiftId;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decision_notes", columnDefinition = "TEXT")
    private String decisionNotes;

    @Column(name = "decision_channel", length = 30)
    private String decisionChannel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    ShiftAuthorization() {}

    ShiftAuthorization(UUID tenantId, UUID branchId, UUID cashShiftId, String requestType,
                       UUID referenceId, UUID requestedBy, BigDecimal amount, String justification) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.cashShiftId = cashShiftId;
        this.requestType = requestType;
        this.referenceId = referenceId;
        this.requestedBy = requestedBy;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.justification = justification;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public void approve(UUID decidedBy, String decisionNotes, String decisionChannel) {
        this.status = "APPROVED";
        this.decidedBy = decidedBy;
        this.decisionNotes = decisionNotes;
        this.decisionChannel = decisionChannel;
        this.decidedAt = Instant.now();
    }

    public void reject(UUID decidedBy, String decisionNotes, String decisionChannel) {
        this.status = "REJECTED";
        this.decidedBy = decidedBy;
        this.decisionNotes = decisionNotes;
        this.decisionChannel = decisionChannel;
        this.decidedAt = Instant.now();
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

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public void setDecisionNotes(String decisionNotes) {
        this.decisionNotes = decisionNotes;
    }

    public String getDecisionChannel() {
        return decisionChannel;
    }

    public void setDecisionChannel(String decisionChannel) {
        this.decisionChannel = decisionChannel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
