package com.finvault.module3_budget_goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalRequest: DTO yêu cầu tạo hoặc chỉnh sửa Mục tiêu tiết kiệm
 * ==============================================================================
 */
public class SavingGoalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên mục tiêu tiết kiệm không được để trống")
    @Size(max = 100, message = "Tên mục tiêu tối đa 100 ký tự")
    private String goalName;

    private Long walletId;

    @NotNull(message = "Số tiền mục tiêu không được để trống")
    @DecimalMin(value = "100000.00", message = "Số tiền mục tiêu tối thiểu là 100,000 VND")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.00", message = "Số tiền ban đầu không được âm")
    private BigDecimal initialAmount;

    @NotNull(message = "Ngày dự kiến hoàn thành không được để trống")
    @Future(message = "Ngày hoàn thành mục tiêu phải ở tương lai")
    private LocalDate targetDate;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String notes;

    public SavingGoalRequest() {
    }

    public SavingGoalRequest(String goalName, Long walletId, BigDecimal targetAmount,
                             BigDecimal initialAmount, LocalDate targetDate, String notes) {
        this.goalName = goalName;
        this.walletId = walletId;
        this.targetAmount = targetAmount;
        this.initialAmount = initialAmount;
        this.targetDate = targetDate;
        this.notes = notes;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(BigDecimal initialAmount) {
        this.initialAmount = initialAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
