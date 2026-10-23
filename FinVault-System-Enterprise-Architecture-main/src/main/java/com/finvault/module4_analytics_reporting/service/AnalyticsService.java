package com.finvault.module4_analytics_reporting.service;

import com.finvault.module4_analytics_reporting.dto.CashFlowTrendDto;
import com.finvault.module4_analytics_reporting.dto.CategoryExpenseDistributionDto;
import com.finvault.module4_analytics_reporting.dto.MonthlyFinancialSummaryDto;
import com.finvault.module4_analytics_reporting.dto.SystemOverviewAnalyticsDto;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * AnalyticsService: Interface cung cấp các dịch vụ phân tích dữ liệu kinh doanh (BI)
 * và cấp nguồn dữ liệu JSON cho biểu đồ động (Chart.js)
 * ==============================================================================
 */
public interface AnalyticsService {

    CashFlowTrendDto getDailyCashFlowTrend(String username, LocalDate startDate, LocalDate endDate);

    CashFlowTrendDto getMonthlyCashFlowTrend(String username, int year);

    CategoryExpenseDistributionDto getCategoryExpenseDistribution(String username, LocalDate startDate, LocalDate endDate);

    MonthlyFinancialSummaryDto getMonthlyFinancialSummary(String username, YearMonth yearMonth);

    SystemOverviewAnalyticsDto getSystemOverviewAnalytics();

    CashFlowTrendDto getSystemWideCashFlowTrend(int year);
}
