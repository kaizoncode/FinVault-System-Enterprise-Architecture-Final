package com.finvault.module4_analytics_reporting.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.LoanCalculation;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import com.finvault.module3_budget_goals.repository.LoanCalculationRepository;
import com.finvault.module3_budget_goals.repository.SavingGoalRepository;
import com.finvault.module4_analytics_reporting.dto.FinancialHealthScoreDto;
import com.finvault.module4_analytics_reporting.repository.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * FinancialHealthServiceImpl: Thuật toán định lượng Sức khỏe Tài chính cá nhân & gia đình
 *
 * 4 Trụ cột đánh giá theo chuẩn Cố vấn Tài chính Quốc tế (CFP):
 * 1. Savings Rate (Tỷ lệ tiết kiệm): Thu nhập tích lũy hàng tháng.
 * 2. Budget Discipline (Kỷ luật ngân sách): Mức độ tuân thủ trần chi tiêu.
 * 3. Emergency Reserve (Quỹ khẩn cấp): Số tháng chi tiêu có thể duy trì khi mất thu nhập.
 * 4. Debt Burden (Gánh nặng nợ DTI): Tỷ lệ nợ phải trả trên thu nhập hàng tháng.
 * ==============================================================================
 */
@Service
@Transactional(readOnly = true)
public class FinancialHealthServiceImpl implements FinancialHealthService {

    private static final Logger log = LoggerFactory.getLogger(FinancialHealthServiceImpl.class);

    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final WalletRepository walletRepository;
    private final LoanCalculationRepository loanCalculationRepository;

    public FinancialHealthServiceImpl(UserRepository userRepository,
                                      AnalyticsRepository analyticsRepository,
                                      BudgetRepository budgetRepository,
                                      SavingGoalRepository savingGoalRepository,
                                      WalletRepository walletRepository,
                                      LoanCalculationRepository loanCalculationRepository) {
        this.userRepository = userRepository;
        this.analyticsRepository = analyticsRepository;
        this.budgetRepository = budgetRepository;
        this.savingGoalRepository = savingGoalRepository;
        this.walletRepository = walletRepository;
        this.loanCalculationRepository = loanCalculationRepository;
    }

    @Override
    public FinancialHealthScoreDto calculateFinancialHealth(String username) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));
        return calculateFinancialHealthByUserId(user.getId());
    }

    @Override
    public FinancialHealthScoreDto calculateFinancialHealthByUserId(Long userId) {
        log.info("Bắt đầu phân tích và đánh giá Sức khỏe tài chính cho User ID: {}", userId);

        LocalDate today = LocalDate.now();
        LocalDate past90Days = today.minusDays(90);
        LocalDate past30Days = today.minusDays(30);

        // 1. Phân tích Dòng tiền 90 ngày gần nhất (Trung bình 3 tháng) và 30 ngày gần nhất
        BigDecimal totalIncome90d = analyticsRepository.sumTotalIncome(userId, past90Days, today);
        if (totalIncome90d == null) totalIncome90d = BigDecimal.ZERO;
        BigDecimal totalExpense90d = analyticsRepository.sumTotalExpense(userId, past90Days, today);
        if (totalExpense90d == null) totalExpense90d = BigDecimal.ZERO;
        BigDecimal totalExpense30d = analyticsRepository.sumTotalExpenseByDateRange(userId, past30Days, today);
        if (totalExpense30d == null) totalExpense30d = BigDecimal.ZERO;

        BigDecimal monthlyIncome = totalIncome90d.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense90d = totalExpense90d.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpense = totalExpense30d.max(avgMonthlyExpense90d);

        // --- TRỤ CỘT 1: TỶ LỆ TIẾT KIỆM (Max 30 điểm) ---
        BigDecimal savingsRate = BigDecimal.ZERO;
        int savingsScore;
        if (totalIncome90d.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netSavings = totalIncome90d.subtract(totalExpense90d);
            savingsRate = netSavings.multiply(BigDecimal.valueOf(100)).divide(totalIncome90d, 2, RoundingMode.HALF_UP);

            if (savingsRate.compareTo(new BigDecimal("30.00")) >= 0) {
                savingsScore = 30;
            } else if (savingsRate.compareTo(new BigDecimal("20.00")) >= 0) {
                savingsScore = 25;
            } else if (savingsRate.compareTo(new BigDecimal("15.00")) >= 0) {
                savingsScore = 20;
            } else if (savingsRate.compareTo(new BigDecimal("10.00")) >= 0) {
                savingsScore = 15;
            } else if (savingsRate.compareTo(new BigDecimal("5.00")) >= 0) {
                savingsScore = 10;
            } else if (savingsRate.compareTo(BigDecimal.ZERO) >= 0) {
                savingsScore = 5;
            } else {
                savingsScore = 0; // Chi tiêu vượt thu nhập (âm dòng tiền)
            }
        } else {
            if (totalExpense90d.compareTo(BigDecimal.ZERO) > 0) {
                savingsRate = new BigDecimal("-100.00");
                savingsScore = 0; // Chi tiêu vượt thu nhập hoàn toàn / thâm hụt dòng tiền
            } else {
                savingsScore = 5; // Chưa có thu nhập và chi tiêu
            }
        }

        // --- TRỤ CỘT 2: KỶ LUẬT NGÂN SÁCH (Max 25 điểm) ---
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsByDate(userId, today);
        int budgetScore;
        BigDecimal budgetAdherence = new BigDecimal("100.00");
        List<String> exceededBudgetNames = new ArrayList<>();
        List<String> warningBudgetNames = new ArrayList<>();

        if (activeBudgets == null || activeBudgets.isEmpty()) {
            budgetScore = 15;
        } else {
            int exceededCount = 0;
            int warningCount = 0;
            BigDecimal totalBudgetAllocated = BigDecimal.ZERO;
            BigDecimal totalBudgetSpent = BigDecimal.ZERO;

            for (Budget b : activeBudgets) {
                BigDecimal alloc = (b.getAllocatedAmount() != null) ? b.getAllocatedAmount() : BigDecimal.ZERO;
                BigDecimal spent = (b.getSpentAmount() != null) ? b.getSpentAmount() : BigDecimal.ZERO;
                totalBudgetAllocated = totalBudgetAllocated.add(alloc);
                totalBudgetSpent = totalBudgetSpent.add(spent);

                boolean isSavingsAllocation = b.getAllocationType() == com.finvault.module3_budget_goals.entity.AllocationType.SAVINGS;
                boolean isExceeded = isSavingsAllocation
                        ? (alloc.compareTo(BigDecimal.ZERO) > 0 && spent.compareTo(alloc) > 0)
                        : b.isOverBudget();

                if (isExceeded) {
                    exceededCount++;
                    exceededBudgetNames.add(b.getBudgetName());
                } else if (!isSavingsAllocation && b.getUsagePercentage().compareTo(new BigDecimal("80.00")) >= 0) {
                    warningCount++;
                    warningBudgetNames.add(b.getBudgetName());
                }
            }

            long compliantCount = activeBudgets.size() - exceededCount;
            budgetAdherence = BigDecimal.valueOf(compliantCount * 100L)
                    .divide(BigDecimal.valueOf(activeBudgets.size()), 2, RoundingMode.HALF_UP);

            if (exceededCount == 0 && warningCount == 0) {
                budgetScore = 25;
            } else if (exceededCount == 0) {
                budgetScore = Math.max(15, 25 - (warningCount * 3));
            } else {
                int rawScore = (int) Math.round(25.0 * compliantCount / activeBudgets.size()) - (exceededCount * 6) - (warningCount * 2);
                budgetScore = Math.min(10, Math.max(0, rawScore));
                if (totalBudgetAllocated.compareTo(BigDecimal.ZERO) > 0 && totalBudgetSpent.compareTo(totalBudgetAllocated) > 0) {
                    budgetScore = Math.min(budgetScore, 4);
                }
            }
        }

        // --- TRỤ CỘT 3: QUỸ DỰ PHÒNG KHẨN CẤP & THANH KHOẢN TIỀN MẶT (Max 25 điểm) ---
        List<Wallet> wallets = walletRepository.findAllByUserIdAndIsDeletedFalse(userId);
        BigDecimal totalLiquidAssets = (wallets != null) ? wallets.stream()
                .filter(w -> !Boolean.FALSE.equals(w.getIsActive()))
                .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalSavings = savingGoalRepository.sumTotalCurrentAmountByUserId(userId);
        if (totalSavings == null) totalSavings = BigDecimal.ZERO;
        BigDecimal totalReserves = totalLiquidAssets.add(totalSavings);

        BigDecimal emergencyMonths = BigDecimal.ZERO;
        BigDecimal liquidMonths = BigDecimal.ZERO;
        int emergencyScore;

        if (totalLiquidAssets.compareTo(BigDecimal.ZERO) <= 0) {
            emergencyScore = (totalSavings.compareTo(BigDecimal.ZERO) > 0) ? 3 : 0;
            if (monthlyExpense.compareTo(BigDecimal.ZERO) > 0 && totalReserves.compareTo(BigDecimal.ZERO) > 0) {
                emergencyMonths = totalReserves.divide(monthlyExpense, 2, RoundingMode.HALF_UP);
            }
        } else if (monthlyExpense.compareTo(BigDecimal.ZERO) > 0) {
            liquidMonths = totalLiquidAssets.divide(monthlyExpense, 2, RoundingMode.HALF_UP);
            BigDecimal totalMonths = totalReserves.divide(monthlyExpense, 2, RoundingMode.HALF_UP);
            emergencyMonths = totalMonths;

            if (totalMonths.compareTo(new BigDecimal("6.00")) >= 0) {
                emergencyScore = 25;
            } else if (totalMonths.compareTo(new BigDecimal("3.00")) >= 0) {
                emergencyScore = 18;
            } else if (totalMonths.compareTo(new BigDecimal("1.00")) >= 0) {
                emergencyScore = 10;
            } else {
                emergencyScore = 4;
            }

            if (liquidMonths.compareTo(new BigDecimal("0.50")) < 0) {
                emergencyScore = Math.min(emergencyScore, 8);
            }
        } else {
            emergencyScore = (totalLiquidAssets.compareTo(new BigDecimal("10000000")) >= 0) ? 20 : 10;
            emergencyMonths = (totalLiquidAssets.compareTo(new BigDecimal("10000000")) >= 0) ? new BigDecimal("6.00") : new BigDecimal("1.00");
        }

        // --- TRỤ CỘT 4: GÁNH NẶNG NỢ DTI (Max 20 điểm) ---
        List<LoanCalculation> loans = loanCalculationRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        BigDecimal monthlyDebtPayment = (loans != null) ? loans.stream()
                .map(l -> l.getMonthlyPayment() != null ? l.getMonthlyPayment() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal dtiRatio;
        int debtScore;
        if (monthlyDebtPayment.compareTo(BigDecimal.ZERO) <= 0) {
            dtiRatio = BigDecimal.ZERO;
            debtScore = 20;
        } else if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            dtiRatio = new BigDecimal("100.00");
            debtScore = 0;
        } else {
            dtiRatio = monthlyDebtPayment.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP);

            if (dtiRatio.compareTo(new BigDecimal("15.00")) <= 0) {
                debtScore = 20;
            } else if (dtiRatio.compareTo(new BigDecimal("30.00")) <= 0) {
                debtScore = 15;
            } else if (dtiRatio.compareTo(new BigDecimal("40.00")) <= 0) {
                debtScore = 8;
            } else {
                debtScore = 0;
            }
        }

        // Tổng kết điểm số & Áp dụng chế tài thanh khoản / thâm hụt dòng tiền
        int totalScore = savingsScore + budgetScore + emergencyScore + debtScore;
        if (totalLiquidAssets.compareTo(BigDecimal.ZERO) <= 0) {
            totalScore = Math.min(totalScore, 35);
        }
        if (totalExpense30d.compareTo(totalIncome90d.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP)) > 0 && savingsScore == 0) {
            totalScore = Math.min(totalScore, 55);
        }
        if (totalScore > 100) totalScore = 100;
        if (totalScore < 0) totalScore = 0;

        String grade;
        String assessment;
        List<String> strengths = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // Phân tích chi tiết từng trụ cột theo dữ liệu thực tế
        if (totalLiquidAssets.compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add(String.format("BÁO ĐỘNG ĐỎ THANH KHOẢN: Số dư tiền mặt trong các ví cá nhân hiện tại là %s VND (đã cạn kiệt). Cần bổ sung thanh khoản hoặc rút từ quỹ dự phòng ngay lập tức!", totalLiquidAssets.toPlainString()));
        } else if (monthlyExpense.compareTo(BigDecimal.ZERO) > 0 && liquidMonths.compareTo(new BigDecimal("0.50")) < 0) {
            recommendations.add(String.format("Cảnh báo thanh khoản ngắn hạn: Tiền mặt trong ví (%s VND) chỉ đủ trang trải %.2f tháng chi tiêu (< 2 tuần).", totalLiquidAssets.toPlainString(), liquidMonths));
        } else {
            strengths.add(String.format("Thanh khoản ví cá nhân khả dụng đạt %s VND.", totalLiquidAssets.toPlainString()));
        }

        if (savingsScore >= 25) {
            strengths.add(String.format("Tỷ lệ tiết kiệm ròng đạt %s%% (Điểm: %d/30), vượt chuẩn khuyến nghị CFP 20%%.", savingsRate.toPlainString(), savingsScore));
        } else if (savingsScore >= 10) {
            recommendations.add(String.format("Tỷ lệ tiết kiệm hiện đạt %s%% (%d/30 điểm). Hãy nâng mức tích lũy lên tối thiểu 20%% thu nhập.", savingsRate.toPlainString(), savingsScore));
        } else {
            recommendations.add(String.format("Thâm hụt dòng tiền nghiêm trọng: Tỷ lệ tiết kiệm ở mức %s%% (%d/30 điểm), chi tiêu đang vượt quá thu nhập!", savingsRate.toPlainString(), savingsScore));
        }

        if (!exceededBudgetNames.isEmpty()) {
            recommendations.add(String.format("Đã vỡ ngân sách tại %d hạng mục (%s). Điểm kỷ luật ngân sách bị trừ còn %d/25.",
                    exceededBudgetNames.size(), String.join(", ", exceededBudgetNames), budgetScore));
        } else if (!warningBudgetNames.isEmpty()) {
            recommendations.add(String.format("Có %d ngân sách đã chạm ngưỡng cảnh báo >= 80%% (%s). Cần hãm phanh chi tiêu ngay.",
                    warningBudgetNames.size(), String.join(", ", warningBudgetNames)));
        } else if (activeBudgets != null && !activeBudgets.isEmpty()) {
            strengths.add(String.format("Tuân thủ tuyệt đối 100%% kỷ luật ngân sách 50/30/20 (%d/25 điểm).", budgetScore));
        }

        if (emergencyScore >= 18) {
            strengths.add(String.format("Quỹ dự phòng khẩn cấp đạt %s tháng chi tiêu (%d/25 điểm), đảm bảo an toàn trước rủi ro.", emergencyMonths.toPlainString(), emergencyScore));
        } else {
            recommendations.add(String.format("Quỹ dự phòng và thanh khoản hiện đạt %s tháng (%d/25 điểm). Mục tiêu an toàn cần đạt từ 3 đến 6 tháng chi phí sinh hoạt.", emergencyMonths.toPlainString(), emergencyScore));
        }

        if (debtScore >= 15) {
            strengths.add(String.format("Tỷ lệ nợ trên thu nhập (DTI) ở mức an toàn %s%% (%d/20 điểm).", dtiRatio.toPlainString(), debtScore));
        } else {
            recommendations.add(String.format("Gánh nặng nợ (DTI) đang ở mức cao %s%% (%d/20 điểm). Không nên vay thêm và ưu tiên tất toán nợ lãi suất cao.", dtiRatio.toPlainString(), debtScore));
        }

        if (totalScore >= 85) {
            grade = "EXCELLENT";
            assessment = "Tình trạng tài chính xuất sắc! Dòng tiền ổn định, tích lũy vượt trội và khả năng phòng thủ rủi ro rất cao.";
        } else if (totalScore >= 70) {
            grade = "GOOD";
            assessment = "Tình trạng tài chính tốt. Bạn đang quản lý tốt thu chi nhưng cần tối ưu hóa một số danh mục chi tiêu.";
        } else if (totalScore >= 50) {
            grade = "FAIR";
            assessment = "Tình trạng tài chính ở mức trung bình. Cần thắt chặt chi tiêu và củng cố quỹ dự phòng thanh khoản.";
        } else {
            grade = "POOR";
            assessment = (totalLiquidAssets.compareTo(BigDecimal.ZERO) <= 0)
                    ? "BÁO ĐỘNG ĐỎ: Ví tiền mặt đã cạn kiệt (0 VND)! Rủi ro mất thanh khoản nghiêm trọng, cần tái cơ cấu tài chính khẩn cấp."
                    : "Cảnh báo rủi ro tài chính cao! Chi tiêu đang vượt thu nhập hoặc vỡ ngân sách, quỹ dự phòng mỏng manh.";
        }

        FinancialHealthScoreDto dto = new FinancialHealthScoreDto();
        dto.setTotalScore(totalScore);
        dto.setHealthGrade(grade);
        dto.setSummaryAssessment(assessment);
        dto.setSavingsRate(savingsRate);
        dto.setSavingsRateScore(savingsScore);
        dto.setBudgetAdherenceRate(budgetAdherence);
        dto.setBudgetAdherenceScore(budgetScore);
        dto.setEmergencyFundMonths(emergencyMonths);
        dto.setEmergencyFundScore(emergencyScore);
        dto.setDebtToIncomeRatio(dtiRatio);
        dto.setDebtToIncomeScore(debtScore);
        dto.setStrengthPoints(strengths);
        dto.setImprovementRecommendations(recommendations);

        log.info("Hoàn tất đánh giá sức khỏe tài chính User ID [{}]: Tổng điểm {}/100, Xếp loại [{}]",
                userId, totalScore, grade);
        return dto;
    }
}
