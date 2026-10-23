package com.finvault.module3_budget_goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalDepositRequest: DTO nạp tiền từ ví vào Quỹ mục tiêu tiết kiệm
 * ==============================================================================
 */
public class SavingGoalDepositRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền nạp tối thiểu là 10,000 VND")
    private BigDecimal amount;

    @NotNull(message = "ID ví nguồn trích tiền không được để trống")
    private Long sourceWalletId;

    public SavingGoalDepositRequest() {
    }

    public SavingGoalDepositRequest(BigDecimal amount, Long sourceWalletId) {
        this.amount = amount;
        this.sourceWalletId = sourceWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getSourceWalletId() {
        return sourceWalletId;
    }

    public void setSourceWalletId(Long sourceWalletId) {
        this.sourceWalletId = sourceWalletId;
    }
}
