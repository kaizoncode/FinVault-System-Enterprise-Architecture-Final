package com.finvault.module4_analytics_reporting.service;

import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import com.finvault.module3_budget_goals.repository.SavingGoalRepository;
import com.finvault.module4_analytics_reporting.dto.*;
import com.finvault.module4_analytics_reporting.repository.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * AnalyticsServiceImpl: Triển khai các dịch vụ Thống kê BI và trực quan hóa dữ liệu
 *
 * Tính năng chính:
 * - Phân tích dòng tiền theo ngày, tháng, năm
 * - Phân tích tỷ trọng chi tiêu danh mục (Doughnut / Pie Chart)
 * - Tóm tắt bảng cân đối tài chính tháng
 * - Phân tích vĩ mô toàn hệ thống cho Super Admin và Auditor
 * ==============================================================================
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;
    private final BudgetRepository budgetRepository;
    private final WalletRepository walletRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final FinancialHealthService financialHealthService;

    public AnalyticsServiceImpl(UserRepository userRepository,
                                AnalyticsRepository analyticsRepository,
                                BudgetRepository budgetRepository,
                                WalletRepository walletRepository,
                                SavingGoalRepository savingGoalRepository,
                                FinancialHealthService financialHealthService) {
        this.userRepository = userRepository;
        this.analyticsRepository = analyticsRepository;
        this.budgetRepository = budgetRepository;
        this.walletRepository = walletRepository;
        this.savingGoalRepository = savingGoalRepository;
        this.financialHealthService = financialHealthService;
    }

    @Override
    public CashFlowTrendDto getDailyCashFlowTrend(String username, LocalDate startDate, LocalDate endDate) {
        User user = getUserByUsername(username);
        Long targetUserId = isMacroRole(user) ? null : user.getId();

        LocalDate actualStart = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate actualEnd = (endDate != null) ? endDate : LocalDate.now();

        if (actualEnd.isBefore(actualStart)) {
            throw new BusinessException("INVALID_DATE_RANGE", "Ngày kết thúc không thể trước ngày bắt đầu");
        }

        List<CashFlowTrendDto.CashFlowDataPoint> points = analyticsRepository.getDailyCashFlowTrend(targetUserId, actualStart, actualEnd);

        BigDecimal totalIn = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getInflow).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getOutflow).reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowTrendDto dto = new CashFlowTrendDto();
        dto.setTimeUnit("DAY");
        dto.setStartDate(actualStart);
        dto.setEndDate(actualEnd);
        dto.setTotalInflow(totalIn);
        dto.setTotalOutflow(totalOut);
        dto.setNetCashFlow(totalIn.subtract(totalOut));
        dto.setDataPoints(points);

        return dto;
    }

    @Override
    public CashFlowTrendDto getMonthlyCashFlowTrend(String username, int year) {
        User user = getUserByUsername(username);
        Long targetUserId = isMacroRole(user) ? null : user.getId();
        int targetYear = (year > 1900 && year < 2100) ? year : LocalDate.now().getYear();

        List<CashFlowTrendDto.CashFlowDataPoint> points = analyticsRepository.getMonthlyCashFlowTrend(targetUserId, targetYear);

        BigDecimal totalIn = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getInflow).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getOutflow).reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowTrendDto dto = new CashFlowTrendDto();
        dto.setTimeUnit("MONTH");
        dto.setStartDate(LocalDate.of(targetYear, 1, 1));
        dto.setEndDate(LocalDate.of(targetYear, 12, 31));
        dto.setTotalInflow(totalIn);
        dto.setTotalOutflow(totalOut);
        dto.setNetCashFlow(totalIn.subtract(totalOut));
        dto.setDataPoints(points);

        return dto;
    }

    @Override
    public CategoryExpenseDistributionDto getCategoryExpenseDistribution(String username, LocalDate startDate, LocalDate endDate) {
        User user = getUserByUsername(username);
        Long targetUserId = isMacroRole(user) ? null : user.getId();

        LocalDate actualStart = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate actualEnd = (endDate != null) ? endDate : LocalDate.now();

        List<CategoryExpenseDistributionDto.CategoryExpenseItem> items =
                analyticsRepository.getCategoryExpenseDistribution(targetUserId, actualStart, actualEnd);

        BigDecimal totalExpense = items.stream()
                .map(CategoryExpenseDistributionDto.CategoryExpenseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CategoryExpenseDistributionDto dto = new CategoryExpenseDistributionDto();
        dto.setTotalExpense(totalExpense);
        dto.setItems(items);
        return dto;
    }

    @Override
    public MonthlyFinancialSummaryDto getMonthlyFinancialSummary(String username, YearMonth yearMonth) {
        User user = getUserByUsername(username);
        Long targetUserId = isMacroRole(user) ? null : user.getId();
        YearMonth targetYm = (yearMonth != null) ? yearMonth : YearMonth.now();

        LocalDate start = targetYm.atDay(1);
        LocalDate end = targetYm.atEndOfMonth();

        BigDecimal income = analyticsRepository.sumTotalIncome(targetUserId, start, end);
        BigDecimal expense = analyticsRepository.sumTotalExpense(targetUserId, start, end);
        BigDecimal net = income.subtract(expense);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = net.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP);
        }

        BigDecimal budgetAllocated = budgetRepository.sumTotalAllocatedAmount(targetUserId, start);
        BigDecimal budgetSpent = budgetRepository.sumTotalSpentAmount(targetUserId, start);
        BigDecimal budgetUsageRate = BigDecimal.ZERO;
        if (budgetAllocated.compareTo(BigDecimal.ZERO) > 0) {
            budgetUsageRate = budgetSpent.multiply(BigDecimal.valueOf(100)).divide(budgetAllocated, 2, RoundingMode.HALF_UP);
        }

        MonthlyFinancialSummaryDto summary = new MonthlyFinancialSummaryDto();
        summary.setYearMonth(targetYm);
        summary.setTotalIncome(income);
        summary.setTotalExpense(expense);
        summary.setNetSavings(net);
        summary.setSavingsRate(savingsRate);
        summary.setTotalBudget(budgetAllocated);
        summary.setTotalBudgetSpent(budgetSpent);
        summary.setBudgetUtilizationRate(budgetUsageRate);

        return summary;
    }

    @Override
    public SystemOverviewAnalyticsDto getSystemOverviewAnalytics() {
        log.info("Tổng hợp số liệu phân tích vĩ mô toàn hệ thống");

        List<User> activeUsers = userRepository.findAllByIsDeletedFalse();
        long userCount = activeUsers.size();
        long walletCount = walletRepository.findAllByIsDeletedFalse().size();
        long txCount = analyticsRepository.countSystemTotalTransactions();
        BigDecimal totalVolume = analyticsRepository.sumSystemTotalVolume();
        BigDecimal totalAssets = analyticsRepository.sumSystemTotalAssets();

        LocalDate today = LocalDate.now();
        BigDecimal totalBudget = budgetRepository.sumTotalAllocatedAmount(null, today);

        long savingGoalCount = savingGoalRepository.findAll().stream().filter(g -> !g.getIsDeleted()).count();
        BigDecimal totalSavingTarget = activeUsers.stream()
                .map(u -> savingGoalRepository.sumTotalTargetAmountByUserId(u.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSavingCurrent = activeUsers.stream()
                .map(u -> savingGoalRepository.sumTotalCurrentAmountByUserId(u.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Điểm sức khỏe tài chính bình quân của các người dùng trong hệ thống (không tính tài khoản hệ thống thuần túy)
        double avgHealthScore = 0.0;
        List<User> financialUsers = activeUsers.stream()
                .filter(u -> !u.hasRole(Role.ROLE_SUPER_ADMIN) && !u.hasRole(Role.ROLE_SYSTEM_AUDITOR))
                .toList();
        List<User> targetUsers = financialUsers.isEmpty() ? activeUsers : financialUsers;
        if (!targetUsers.isEmpty()) {
            double totalScore = 0.0;
            for (User u : targetUsers) {
                try {
                    FinancialHealthScoreDto health = financialHealthService.calculateFinancialHealthByUserId(u.getId());
                    totalScore += health.getTotalScore();
                } catch (Exception ignored) {
                }
            }
            avgHealthScore = Math.round((totalScore / targetUsers.size()) * 10.0) / 10.0;
        }

        SystemOverviewAnalyticsDto dto = new SystemOverviewAnalyticsDto();
        dto.setTotalActiveUsers(userCount);
        dto.setTotalWallets(walletCount);
        dto.setTotalTransactions(txCount);
        dto.setSystemTotalVolume(totalVolume);
        dto.setSystemTotalAssets(totalAssets);
        dto.setTotalBudgetAllocated(totalBudget);
        dto.setTotalSavingGoals(savingGoalCount);
        dto.setTotalSavingTargetAmount(totalSavingTarget);
        dto.setTotalSavingCurrentAmount(totalSavingCurrent);
        dto.setAverageFinancialHealthScore(avgHealthScore);

        return dto;
    }

    private boolean isMacroRole(User user) {
        if (user != null && (user.hasRole(Role.ROLE_SUPER_ADMIN) || user.hasRole(Role.ROLE_SYSTEM_AUDITOR))) {
            return true;
        }
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isSystemAuditor();
    }

    @Override
    public CashFlowTrendDto getSystemWideCashFlowTrend(int year) {
        int targetYear = (year > 1900 && year < 2100) ? year : LocalDate.now().getYear();
        List<CashFlowTrendDto.CashFlowDataPoint> points = analyticsRepository.getMonthlyCashFlowTrend(null, targetYear);

        BigDecimal totalIn = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getInflow).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOut = points.stream().map(CashFlowTrendDto.CashFlowDataPoint::getOutflow).reduce(BigDecimal.ZERO, BigDecimal::add);

        CashFlowTrendDto dto = new CashFlowTrendDto();
        dto.setTimeUnit("MONTH");
        dto.setStartDate(LocalDate.of(targetYear, 1, 1));
        dto.setEndDate(LocalDate.of(targetYear, 12, 31));
        dto.setTotalInflow(totalIn);
        dto.setTotalOutflow(totalOut);
        dto.setNetCashFlow(totalIn.subtract(totalOut));
        dto.setDataPoints(points);

        return dto;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));
    }
}
