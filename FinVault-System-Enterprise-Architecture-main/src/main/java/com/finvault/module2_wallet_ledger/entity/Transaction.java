package com.finvault.module2_wallet_ledger.entity;

import com.finvault.common.BaseEntity;
import com.finvault.module1_security_rbac.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * Transaction Entity: Thực thể Giao dịch tài chính (Lõi Sổ cái Ledger)
 *
 * Tính năng kỹ thuật cốt lõi:
 * - Kế thừa BaseEntity: Tự động Auditing, Soft Delete và Optimistic Locking (@Version)
 * - ACID Compliance: Đảm bảo tính toàn vẹn khi thu, chi và chuyển tiền giữa 2 ví
 * - Tích hợp Workflow phê duyệt chi tiêu gia đình:
 *   + Các giao dịch từ ví gia đình >= 5,000,000 VND tự động gắn cờ PENDING
 *   + Ghi nhận approved_by, approved_at hoặc rejection_reason
 * - Hỗ trợ đính kèm hóa đơn (receipt_url) và giao dịch định kỳ (is_recurring)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ OCR AI quét ảnh hóa đơn tự động bóc tách số tiền và danh mục (v2.0)
 * ==============================================================================
 */
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_id")
    private Wallet targetWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "recurring_interval", length = 20)
    private String recurringInterval;

    @Column(name = "last_recurring_date")
    private java.time.LocalDate lastRecurringDate;

    @Column(name = "linked_transaction_id")
    private Long linkedTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_direction", length = 20)
    private TransferDirection transferDirection;

    public Transaction() {
    }

    public Transaction(Wallet wallet, User user, Category category, TransactionType transactionType,
                       BigDecimal amount, LocalDateTime transactionDate, String note) {
        this.wallet = wallet;
        this.user = user;
        this.category = category;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = (transactionDate != null) ? transactionDate : LocalDateTime.now();
        this.note = note;
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.isRecurring = false;
    }

    public Transaction(User user, Wallet wallet, Category category, TransactionType transactionType,
                       BigDecimal amount, LocalDateTime transactionDate, String note) {
        this(wallet, user, category, transactionType, amount, transactionDate, note);
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Wallet getTargetWallet() {
        return targetWallet;
    }

    public void setTargetWallet(Wallet targetWallet) {
        this.targetWallet = targetWallet;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDescription() {
        return note;
    }

    public void setDescription(String description) {
        this.note = description;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean recurring) {
        isRecurring = recurring;
    }

    public String getRecurringInterval() {
        return recurringInterval;
    }

    public void setRecurringInterval(String recurringInterval) {
        this.recurringInterval = recurringInterval;
    }

    public java.time.LocalDate getLastRecurringDate() {
        return lastRecurringDate;
    }

    public void setLastRecurringDate(java.time.LocalDate lastRecurringDate) {
        this.lastRecurringDate = lastRecurringDate;
    }

    public Long getLinkedTransactionId() {
        return linkedTransactionId;
    }

    public void setLinkedTransactionId(Long linkedTransactionId) {
        this.linkedTransactionId = linkedTransactionId;
    }

    public TransferDirection getTransferDirection() {
        return transferDirection;
    }

    public void setTransferDirection(TransferDirection transferDirection) {
        this.transferDirection = transferDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + getId() +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", approvalStatus=" + approvalStatus +
                '}';
    }
}
