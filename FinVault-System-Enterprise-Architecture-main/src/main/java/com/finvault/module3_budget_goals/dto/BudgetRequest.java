package com.finvault.module3_budget_goals.dto;

import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.BudgetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetRequest: DTO yêu cầu khởi tạo hoặc cập nhật ngân sách tài chính
 * ==============================================================================
 */
public class BudgetRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên ngân sách không được để trống")
    @Size(max = 100, message = "Tên ngân sách tối đa 100 ký tự")
    private String budgetName;

    private Long categoryId;

    private Long walletId;

    private BudgetType budgetType;

    private AllocationType allocationType;

    @NotNull(message = "Số tiền ngân sách phân bổ không được để trống")
    @DecimalMin(value = "1000.00", message = "Số tiền ngân sách tối thiểu là 1,000 VND")
    private BigDecimal allocatedAmount;

    @NotNull(message = "Ngày bắt đầu ngân sách không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc ngân sách không được để trống")
    private LocalDate endDate;

    public BudgetRequest() {
    }

    public BudgetRequest(String budgetName, Long categoryId, Long walletId,
                         BudgetType budgetType, AllocationType allocationType,
                         BigDecimal allocatedAmount, LocalDate startDate, LocalDate endDate) {
        this.budgetName = budgetName;
        this.categoryId = categoryId;
        this.walletId = walletId;
        this.budgetType = budgetType;
        this.allocationType = allocationType;
        this.allocatedAmount = allocatedAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public BudgetType getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(BudgetType budgetType) {
        this.budgetType = budgetType;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(AllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
