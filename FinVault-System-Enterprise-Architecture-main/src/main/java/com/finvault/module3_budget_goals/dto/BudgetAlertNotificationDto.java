package com.finvault.module3_budget_goals.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetAlertNotificationDto: DTO đóng gói dữ liệu cảnh báo ngân sách thời gian thực
 * đẩy qua STOMP WebSocket tới giao diện người dùng
 * ==============================================================================
 */
public class BudgetAlertNotificationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum AlertSeverity {
        WARNING_80,  // Cảnh báo vàng: Chi tiêu chạm ngưỡng 80%
        CRITICAL_100 // Cảnh báo đỏ: Chi tiêu chạm hoặc vượt ngưỡng 100%
    }

    private Long budgetId;
    private String budgetName;
    private String categoryName;
    private AlertSeverity severity;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usagePercentage;
    private String alertMessage;
    private LocalDateTime timestamp;

    public BudgetAlertNotificationDto() {
        this.timestamp = LocalDateTime.now();
    }

    public BudgetAlertNotificationDto(Long budgetId, String budgetName, String categoryName,
                                      AlertSeverity severity, BigDecimal allocatedAmount,
                                      BigDecimal spentAmount, BigDecimal remainingAmount,
                                      BigDecimal usagePercentage, String alertMessage) {
        this.budgetId = budgetId;
        this.budgetName = budgetName;
        this.categoryName = categoryName;
        this.severity = severity;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
        this.remainingAmount = remainingAmount;
        this.usagePercentage = usagePercentage;
        this.alertMessage = alertMessage;
        this.timestamp = LocalDateTime.now();
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
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

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
