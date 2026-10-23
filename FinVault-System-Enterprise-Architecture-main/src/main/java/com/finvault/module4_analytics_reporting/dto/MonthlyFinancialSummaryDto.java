package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * MonthlyFinancialSummaryDto: DTO tóm tắt các chỉ số tài chính chủ chốt trong tháng
 * ==============================================================================
 */
public class MonthlyFinancialSummaryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private YearMonth yearMonth;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netSavings = BigDecimal.ZERO;
    private BigDecimal savingsRate = BigDecimal.ZERO; // %
    private BigDecimal totalBudget = BigDecimal.ZERO;
    private BigDecimal totalBudgetSpent = BigDecimal.ZERO;
    private BigDecimal budgetUtilizationRate = BigDecimal.ZERO; // %
    private Long transactionCount = 0L;

    public MonthlyFinancialSummaryDto() {
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }

    public BigDecimal getTotalBudgetSpent() {
        return totalBudgetSpent;
    }

    public void setTotalBudgetSpent(BigDecimal totalBudgetSpent) {
        this.totalBudgetSpent = totalBudgetSpent;
    }

    public BigDecimal getBudgetUtilizationRate() {
        return budgetUtilizationRate;
    }

    public void setBudgetUtilizationRate(BigDecimal budgetUtilizationRate) {
        this.budgetUtilizationRate = budgetUtilizationRate;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }
}
