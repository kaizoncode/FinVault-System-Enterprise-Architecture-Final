package com.finvault.module4_analytics_reporting.repository;

import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module4_analytics_reporting.dto.CashFlowTrendDto;
import com.finvault.module4_analytics_reporting.dto.CategoryExpenseDistributionDto;
import com.finvault.module4_analytics_reporting.dto.TransactionExportFilterRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * AnalyticsRepository: Interface truy vấn thống kê dữ liệu BI và tổng hợp tài chính
 * ==============================================================================
 */
public interface AnalyticsRepository {

    List<CashFlowTrendDto.CashFlowDataPoint> getDailyCashFlowTrend(Long userId, LocalDate startDate, LocalDate endDate);

    List<CashFlowTrendDto.CashFlowDataPoint> getMonthlyCashFlowTrend(Long userId, int year);

    List<CategoryExpenseDistributionDto.CategoryExpenseItem> getCategoryExpenseDistribution(Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findTransactionsForExport(Long userId, TransactionExportFilterRequest filter);

    BigDecimal sumTotalIncome(Long userId, LocalDate startDate, LocalDate endDate);

    BigDecimal sumTotalExpense(Long userId, LocalDate startDate, LocalDate endDate);

    default BigDecimal sumTotalIncomeByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return sumTotalIncome(userId, startDate, endDate);
    }

    default BigDecimal sumTotalExpenseByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return sumTotalExpense(userId, startDate, endDate);
    }

    BigDecimal sumSystemTotalVolume();

    BigDecimal sumSystemTotalAssets();

    Long countSystemTotalTransactions();
}
