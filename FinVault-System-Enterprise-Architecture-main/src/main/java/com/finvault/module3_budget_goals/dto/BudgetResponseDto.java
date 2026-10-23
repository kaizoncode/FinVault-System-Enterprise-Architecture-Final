package com.finvault.module3_budget_goals.dto;

import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.BudgetType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetResponseDto: DTO phản hồi chi tiết thông tin và tiến độ chi tiêu ngân sách
 * ==============================================================================
 */
public class BudgetResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private Long categoryId;
    private String categoryName;
    private Long walletId;
    private String walletName;
    private String budgetName;
    private BudgetType budgetType;
    private AllocationType allocationType;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private Boolean isOverBudget;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean alertThreshold80Sent;
    private Boolean alertThreshold100Sent;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BudgetResponseDto() {
    }

    public static BudgetResponseDto fromEntity(Budget entity) {
        if (entity == null) {
            return null;
        }
        BudgetResponseDto dto = new BudgetResponseDto();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUsername(entity.getUser().getUsername());
        }
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getCategoryName());
        }
        if (entity.getWallet() != null) {
            dto.setWalletId(entity.getWallet().getId());
            dto.setWalletName(entity.getWallet().getWalletName());
        }
        dto.setBudgetName(entity.getBudgetName());
        dto.setBudgetType(entity.getBudgetType());
        dto.setAllocationType(entity.getAllocationType());
        dto.setAllocatedAmount(entity.getAllocatedAmount());
        dto.setSpentAmount(entity.getSpentAmount());
        dto.setRemainingAmount(entity.getRemainingAmount());
        dto.setUsagePercentage(entity.getUsagePercentage());
        dto.setIsOverBudget(entity.isOverBudget());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setAlertThreshold80Sent(entity.getAlertThreshold80Sent());
        dto.setAlertThreshold100Sent(entity.getAlertThreshold100Sent());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
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

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(BigDecimal usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public BigDecimal getSpentPercentage() {
        return usagePercentage;
    }

    public BigDecimal getCurrentSpent() {
        return spentAmount;
    }

    public BigDecimal getLimitAmount() {
        return allocatedAmount;
    }

    public Boolean getIsOverBudget() {
        return isOverBudget;
    }

    public void setIsOverBudget(Boolean overBudget) {
        isOverBudget = overBudget;
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

    public Boolean getAlertThreshold80Sent() {
        return alertThreshold80Sent;
    }

    public void setAlertThreshold80Sent(Boolean alertThreshold80Sent) {
        this.alertThreshold80Sent = alertThreshold80Sent;
    }

    public Boolean getAlertThreshold100Sent() {
        return alertThreshold100Sent;
    }

    public void setAlertThreshold100Sent(Boolean alertThreshold100Sent) {
        this.alertThreshold100Sent = alertThreshold100Sent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
