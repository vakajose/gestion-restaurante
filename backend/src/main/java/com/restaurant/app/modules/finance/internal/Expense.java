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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expenses")
class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "cash_shift_id")
    private UUID cashShiftId;

    @Column(name = "expense_type", nullable = false, length = 30)
    private String expenseType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "paid_from_cash_drawer", nullable = false)
    private boolean paidFromCashDrawer = false;

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "APPROVED";

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate = LocalDate.now();

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    Expense() {
    }

    Expense(
        UUID tenantId,
        UUID branchId,
        UUID cashShiftId,
        String expenseType,
        String description,
        BigDecimal amount,
        boolean paidFromCashDrawer,
        String approvalStatus,
        String evidenceUrl,
        LocalDate expenseDate,
        UUID createdBy
    ) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.cashShiftId = cashShiftId;
        this.expenseType = expenseType;
        this.description = description;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.paidFromCashDrawer = paidFromCashDrawer;
        this.approvalStatus = approvalStatus != null ? approvalStatus : "APPROVED";
        this.evidenceUrl = evidenceUrl;
        this.expenseDate = expenseDate != null ? expenseDate : LocalDate.now();
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
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

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isPaidFromCashDrawer() {
        return paidFromCashDrawer;
    }

    public void setPaidFromCashDrawer(boolean paidFromCashDrawer) {
        this.paidFromCashDrawer = paidFromCashDrawer;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public UUID getCreatedBy() {
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
}
