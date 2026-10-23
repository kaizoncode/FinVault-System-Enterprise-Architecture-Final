package com.finvault.module4_analytics_reporting.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module4_analytics_reporting.dto.*;
import com.finvault.module4_analytics_reporting.service.AnalyticsService;
import com.finvault.module4_analytics_reporting.service.FinancialHealthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * AnalyticsApiController: REST API Cung cấp dữ liệu thống kê BI và trực quan hóa (Chart.js)
 *
 * Tính năng chính:
 * - Chuỗi dữ liệu biến động dòng tiền ngày/tháng (Line / Bar Chart)
 * - Tỷ trọng phân bổ chi tiêu theo danh mục (Doughnut / Pie Chart)
 * - Bảng chỉ số tóm tắt tài chính hàng tháng
 * - Điểm số sức khỏe tài chính toàn diện (Financial Health Score)
 * - Thống kê vĩ mô toàn hệ thống cho Super Admin & Auditor
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("isAuthenticated()")
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;
    private final FinancialHealthService financialHealthService;

    public AnalyticsApiController(AnalyticsService analyticsService,
                                  FinancialHealthService financialHealthService) {
        this.analyticsService = analyticsService;
        this.financialHealthService = financialHealthService;
    }

    /**
     * Dữ liệu dòng tiền theo từng ngày (Dùng cho biểu đồ Line Chart)
     */
    @GetMapping("/cashflow/daily")
    public ResponseEntity<ApiResponse<CashFlowTrendDto>> getDailyCashFlowTrend(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String username = SecurityUtils.getCurrentUsername();
        CashFlowTrendDto dto = analyticsService.getDailyCashFlowTrend(username, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Xu hướng dòng tiền theo ngày", dto));
    }

    /**
     * Dữ liệu dòng tiền 12 tháng trong năm (Dùng cho biểu đồ Bar Chart)
     */
    @GetMapping("/cashflow/monthly")
    public ResponseEntity<ApiResponse<CashFlowTrendDto>> getMonthlyCashFlowTrend(
            @RequestParam(value = "year", required = false, defaultValue = "0") int year) {
        String username = SecurityUtils.getCurrentUsername();
        CashFlowTrendDto dto = analyticsService.getMonthlyCashFlowTrend(username, year);
        return ResponseEntity.ok(ApiResponse.success("Xu hướng dòng tiền theo tháng", dto));
    }

    /**
     * Tỷ trọng chi tiêu theo danh mục (Dùng cho biểu đồ Doughnut Chart)
     */
    @GetMapping(value = {"/category-expenses", "/expenses/category"})
    public ResponseEntity<ApiResponse<CategoryExpenseDistributionDto>> getCategoryExpenseDistribution(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String username = SecurityUtils.getCurrentUsername();
        CategoryExpenseDistributionDto dto = analyticsService.getCategoryExpenseDistribution(username, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Cơ cấu chi tiêu theo danh mục", dto));
    }

    /**
     * Bảng chỉ số tóm tắt tài chính hàng tháng
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<MonthlyFinancialSummaryDto>> getMonthlyFinancialSummary(
            @RequestParam(value = "yearMonth", required = false) String yearMonthStr) {
        String username = SecurityUtils.getCurrentUsername();
        YearMonth ym = (yearMonthStr != null && !yearMonthStr.isBlank())
                ? YearMonth.parse(yearMonthStr)
                : YearMonth.now();

        MonthlyFinancialSummaryDto dto = analyticsService.getMonthlyFinancialSummary(username, ym);
        return ResponseEntity.ok(ApiResponse.success("Tóm tắt tài chính tháng " + ym, dto));
    }

    /**
     * Đánh giá Điểm Sức khỏe Tài chính toàn diện (Thang điểm 0 - 100)
     */
    @GetMapping("/financial-health")
    public ResponseEntity<ApiResponse<FinancialHealthScoreDto>> getFinancialHealthScore() {
        String username = SecurityUtils.getCurrentUsername();
        FinancialHealthScoreDto dto = financialHealthService.calculateFinancialHealth(username);
        return ResponseEntity.ok(ApiResponse.success("Điểm sức khỏe tài chính", dto));
    }

    /**
     * Phân tích vĩ mô toàn hệ thống (Super Admin & System Auditor)
     */
    @GetMapping("/system/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<ApiResponse<SystemOverviewAnalyticsDto>> getSystemOverview() {
        SystemOverviewAnalyticsDto dto = analyticsService.getSystemOverviewAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Thống kê vĩ mô toàn hệ thống", dto));
    }

    /**
     * Biến động dòng tiền toàn hệ thống theo năm (Super Admin & System Auditor)
     */
    @GetMapping("/system/cashflow")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<ApiResponse<CashFlowTrendDto>> getSystemWideCashFlow(
            @RequestParam(value = "year", required = false, defaultValue = "0") int year) {
        CashFlowTrendDto dto = analyticsService.getSystemWideCashFlowTrend(year);
        return ResponseEntity.ok(ApiResponse.success("Biến động dòng tiền toàn hệ thống", dto));
    }
}
