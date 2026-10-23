package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.WalletType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletRequest: DTO tiếp nhận yêu cầu khởi tạo hoặc cập nhật Ví tài chính
 * ==============================================================================
 */
public class WalletRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên ví không được để trống")
    @Size(max = 100, message = "Tên ví tối đa 100 ký tự")
    private String walletName;

    @NotNull(message = "Loại ví không được để trống")
    private WalletType walletType;

    @DecimalMin(value = "0.00", message = "Số dư ban đầu không được âm")
    private BigDecimal initialBalance = BigDecimal.ZERO;

    private String currency = "VND";

    private String bankAccountNumber;

    private String bankName;

    @DecimalMin(value = "0.00", message = "Hạn mức chi tiêu không được âm")
    private BigDecimal monthlySpendingLimit = BigDecimal.ZERO;

    public WalletRequest() {
    }

    public WalletRequest(String walletName, WalletType walletType, BigDecimal initialBalance, String currency) {
        this.walletName = walletName;
        this.walletType = walletType;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public void setWalletType(WalletType walletType) {
        this.walletType = walletType;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getMonthlySpendingLimit() {
        return monthlySpendingLimit;
    }

    public void setMonthlySpendingLimit(BigDecimal monthlySpendingLimit) {
        this.monthlySpendingLimit = monthlySpendingLimit;
    }
}
