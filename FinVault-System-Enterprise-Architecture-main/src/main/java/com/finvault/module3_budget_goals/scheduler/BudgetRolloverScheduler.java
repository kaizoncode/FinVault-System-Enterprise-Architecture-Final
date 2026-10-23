package com.finvault.module3_budget_goals.scheduler;

import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetRolloverScheduler: Tác vụ tự động kiểm tra và gia hạn chu kỳ ngân sách định kỳ
 *
 * Lịch trình thực thi:
 * - Chạy tự động vào 00:00:00 ngày đầu tiên của mỗi tháng (Cron: 0 0 0 1 * ?)
 * - Quét các ngân sách đã hết hạn chu kỳ, tổng kết số dư và tự động tạo chu kỳ mới cho tháng kế tiếp.
 * ==============================================================================
 */
@Component
public class BudgetRolloverScheduler {

    private static final Logger log = LoggerFactory.getLogger(BudgetRolloverScheduler.class);

    private final BudgetRepository budgetRepository;

    public BudgetRolloverScheduler(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    /**
     * Tự động quét và xử lý các ngân sách hết hạn vào đầu tháng
     */
    @Scheduled(cron = "${finvault.budget.rollover-cron:0 0 0 1 * ?}")
    @Transactional
    public void processMonthlyBudgetRollover() {
        LocalDate today = LocalDate.now();
        log.info("Bắt đầu tác vụ kiểm tra gia hạn chu kỳ ngân sách hàng tháng (Ngày: {})", today);

        List<Budget> expiredBudgets = budgetRepository.findExpiredActiveBudgets(today);
        if (expiredBudgets.isEmpty()) {
            log.info("Không có ngân sách nào hết hạn cần đóng chu kỳ.");
            return;
        }

        int count = 0;
        for (Budget budget : expiredBudgets) {
            budget.setIsActive(false);
            budgetRepository.save(budget);
            count++;
            log.info("Đã đóng chu kỳ cũ của ngân sách [{}]: Đã chi {} / Phân bổ {}",
                    budget.getBudgetName(), budget.getSpentAmount(), budget.getAllocatedAmount());

            // Tự động khởi tạo chu kỳ mới dựa trên thời lượng ban đầu (originalDuration)
            Long duration = budget.getOriginalDuration();
            if (duration == null && budget.getStartDate() != null && budget.getEndDate() != null) {
                duration = java.time.temporal.ChronoUnit.DAYS.between(budget.getStartDate(), budget.getEndDate()) + 1;
            }

            Budget newCycle = new Budget();
            newCycle.setUser(budget.getUser());
            newCycle.setCategory(budget.getCategory());
            newCycle.setWallet(budget.getWallet());
            newCycle.setBudgetName(budget.getBudgetName());
            newCycle.setBudgetType(budget.getBudgetType());
            newCycle.setAllocationType(budget.getAllocationType());
            newCycle.setAllocatedAmount(budget.getAllocatedAmount());
            newCycle.setSpentAmount(BigDecimal.ZERO);
            newCycle.setStartDate(today);
            if (duration != null && duration > 0) {
                newCycle.setEndDate(today.plusDays(duration - 1));
                newCycle.setOriginalDuration(duration);
            } else {
                newCycle.setEndDate(today.plusMonths(1).minusDays(1));
            }
            newCycle.setIsActive(true);
            newCycle.setAlertThreshold80Sent(false);
            newCycle.setAlertThreshold100Sent(false);
            budgetRepository.save(newCycle);
            log.info("Đã kích hoạt chu kỳ mới cho ngân sách [{}] (Từ {} đến {})",
                    newCycle.getBudgetName(), newCycle.getStartDate(), newCycle.getEndDate());
        }

        log.info("Hoàn tất tác vụ Rollover. Đã tái kích hoạt thành công {} chu kỳ ngân sách mới.", count);
    }
}
