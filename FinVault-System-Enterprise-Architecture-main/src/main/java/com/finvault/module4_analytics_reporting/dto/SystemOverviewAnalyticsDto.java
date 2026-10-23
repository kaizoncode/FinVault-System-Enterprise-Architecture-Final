package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * SystemOverviewAnalyticsDto: DTO phân tích thống kê toàn hệ thống (Macro BI) cho Admin / Auditor
 * ==============================================================================
 */
public class SystemOverviewAnalyticsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalActiveUsers = 0L;
    private Long totalWallets = 0L;
    private Long totalTransactions = 0L;
    private BigDecimal systemTotalVolume = BigDecimal.ZERO; // Tổng luân chuyển tiền tệ
    private BigDecimal systemTotalAssets = BigDecimal.ZERO; // Tổng số dư toàn bộ các ví
    private BigDecimal totalBudgetAllocated = BigDecimal.ZERO;
    private Long totalSavingGoals = 0L;
    private BigDecimal totalSavingTargetAmount = BigDecimal.ZERO;
    private BigDecimal totalSavingCurrentAmount = BigDecimal.ZERO;
    private Double averageFinancialHealthScore = 0.0;

    public SystemOverviewAnalyticsDto() {
    }

    public Long getTotalActiveUsers() {
        return totalActiveUsers;
    }

    public void setTotalActiveUsers(Long totalActiveUsers) {
        this.totalActiveUsers = totalActiveUsers;
    }

    public Long getTotalWallets() {
        return totalWallets;
    }

    public void setTotalWallets(Long totalWallets) {
        this.totalWallets = totalWallets;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getSystemTotalVolume() {
        return systemTotalVolume;
    }

    public void setSystemTotalVolume(BigDecimal systemTotalVolume) {
        this.systemTotalVolume = systemTotalVolume;
    }

    public BigDecimal getSystemTotalAssets() {
        return systemTotalAssets;
    }

    public void setSystemTotalAssets(BigDecimal systemTotalAssets) {
        this.systemTotalAssets = systemTotalAssets;
    }

    public BigDecimal getTotalBudgetAllocated() {
        return totalBudgetAllocated;
    }

    public void setTotalBudgetAllocated(BigDecimal totalBudgetAllocated) {
        this.totalBudgetAllocated = totalBudgetAllocated;
    }

    public Long getTotalSavingGoals() {
        return totalSavingGoals;
    }

    public void setTotalSavingGoals(Long totalSavingGoals) {
        this.totalSavingGoals = totalSavingGoals;
    }

    public BigDecimal getTotalSavingTargetAmount() {
        return totalSavingTargetAmount;
    }

    public void setTotalSavingTargetAmount(BigDecimal totalSavingTargetAmount) {
        this.totalSavingTargetAmount = totalSavingTargetAmount;
    }

    public BigDecimal getTotalSavingCurrentAmount() {
        return totalSavingCurrentAmount;
    }

    public void setTotalSavingCurrentAmount(BigDecimal totalSavingCurrentAmount) {
        this.totalSavingCurrentAmount = totalSavingCurrentAmount;
    }

    public Double getAverageFinancialHealthScore() {
        return averageFinancialHealthScore;
    }

    public void setAverageFinancialHealthScore(Double averageFinancialHealthScore) {
        this.averageFinancialHealthScore = averageFinancialHealthScore;
    }
}
