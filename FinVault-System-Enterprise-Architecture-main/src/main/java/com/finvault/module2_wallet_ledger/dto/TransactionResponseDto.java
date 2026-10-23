package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionResponseDto: DTO trả về chi tiết thông tin giao dịch tài chính
 * ==============================================================================
 */
public class TransactionResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long walletId;
    private String walletName;
    private Long targetWalletId;
    private String targetWalletName;
    private Long userId;
    private String username;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private TransactionType transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String note;
    private String receiptUrl;
    private ApprovalStatus approvalStatus;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Boolean isRecurring;
    private LocalDateTime createdAt;

    public TransactionResponseDto() {
    }

    public static TransactionResponseDto fromEntity(Transaction t) {
        if (t == null) {
            return null;
        }
        TransactionResponseDto dto = new TransactionResponseDto();
        dto.setId(t.getId());
        if (t.getWallet() != null) {
            dto.setWalletId(t.getWallet().getId());
            dto.setWalletName(t.getWallet().getWalletName());
        }
        if (t.getTargetWallet() != null) {
            dto.setTargetWalletId(t.getTargetWallet().getId());
            dto.setTargetWalletName(t.getTargetWallet().getWalletName());
        }
        if (t.getUser() != null) {
            dto.setUserId(t.getUser().getId());
            dto.setUsername(t.getUser().getUsername());
        }
        if (t.getCategory() != null) {
            dto.setCategoryId(t.getCategory().getId());
            dto.setCategoryName(t.getCategory().getCategoryName());
            dto.setCategoryIcon(t.getCategory().getIcon());
            dto.setCategoryColor(t.getCategory().getColor());
        }
        dto.setTransactionType(t.getTransactionType());
        dto.setAmount(t.getAmount());
        dto.setTransactionDate(t.getTransactionDate());
        dto.setNote(t.getNote());
        dto.setReceiptUrl(t.getReceiptUrl());
        dto.setApprovalStatus(t.getApprovalStatus());
        dto.setApprovedBy(t.getApprovedBy());
        dto.setApprovedAt(t.getApprovedAt());
        dto.setRejectionReason(t.getRejectionReason());
        dto.setIsRecurring(t.getIsRecurring());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public Long getTargetWalletId() {
        return targetWalletId;
    }

    public void setTargetWalletId(Long targetWalletId) {
        this.targetWalletId = targetWalletId;
    }

    public String getTargetWalletName() {
        return targetWalletName;
    }

    public void setTargetWalletName(String targetWalletName) {
        this.targetWalletName = targetWalletName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
