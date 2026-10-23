package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionRequest: DTO tiếp nhận yêu cầu ghi nhận giao dịch thu chi
 * ==============================================================================
 */
public class TransactionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID ví không được để trống")
    private Long walletId;

    private Long categoryId;

    @NotNull(message = "Loại giao dịch không được để trống")
    private TransactionType transactionType;

    @NotNull(message = "Số tiền giao dịch không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền giao dịch phải lớn hơn 0")
    private BigDecimal amount;

    private LocalDateTime transactionDate;

    @Size(max = 500, message = "Ghi chú giao dịch tối đa 500 ký tự")
    private String note;

    @Pattern(
        regexp = "^(https://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?|/uploads/[a-zA-Z0-9_.-]+)?$",
        message = "receiptUrl chỉ chấp nhận đường dẫn ảnh hóa đơn nội bộ /uploads/... hoặc URL với giao thức https://"
    )
    private String receiptUrl;

    private Boolean isRecurring = false;

    private String recurringInterval;

    public TransactionRequest() {
    }

    public TransactionRequest(Long walletId, Long categoryId, TransactionType transactionType, BigDecimal amount, String note) {
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.note = note;
        this.transactionDate = LocalDateTime.now();
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
}
