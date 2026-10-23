package com.finvault.module2_wallet_ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransferRequest: DTO chuyển tiền liên ví (Inter-wallet Transfer)
 * ==============================================================================
 */
public class TransferRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Ví nguồn không được để trống")
    private Long sourceWalletId;

    @NotNull(message = "Ví đích không được để trống")
    private Long targetWalletId;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền chuyển phải lớn hơn 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    public TransferRequest() {
    }

    public TransferRequest(Long sourceWalletId, Long targetWalletId, BigDecimal amount, String note) {
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.amount = amount;
        this.note = note;
    }

    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(Long sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }

    public Long getTargetWalletId() {
        return targetWalletId;
    }

    public void setTargetWalletId(Long targetWalletId) {
        this.targetWalletId = targetWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
