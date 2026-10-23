package com.finvault.module3_budget_goals.rule;

import com.finvault.module3_budget_goals.dto.BudgetAlertNotificationDto;
import com.finvault.module3_budget_goals.entity.Budget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetAlertRuleEngine: Động cơ quy tắc kiểm tra và kích hoạt cảnh báo ngân sách
 *
 * Áp dụng Strategy Pattern:
 * - Tự động phát hiện toàn bộ các Bean triển khai BudgetAlertStrategy trong Spring Context.
 * - Sắp xếp chiến lược theo độ ưu tiên giảm dần (ưu tiên kiểm tra 100% trước, sau đó mới đến 80%).
 * - Đảm bảo tính mở rộng cao (Open/Closed Principle) khi bổ sung các rule cảnh báo mới.
 * ==============================================================================
 */
@Component
public class BudgetAlertRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertRuleEngine.class);

    private final List<BudgetAlertStrategy> strategies;

    public BudgetAlertRuleEngine(List<BudgetAlertStrategy> strategies) {
        // Sắp xếp các chiến lược theo độ ưu tiên giảm dần
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(BudgetAlertStrategy::getPriority).reversed())
                .toList();
        log.info("BudgetAlertRuleEngine khởi tạo thành công với {} chiến lược cảnh báo.", this.strategies.size());
    }

    /**
     * Đánh giá ngân sách và thực thi chiến lược phù hợp nhất
     */
    public Optional<BudgetAlertNotificationDto> evaluateAndCreateAlert(Budget budget) {
        if (budget == null || !Boolean.TRUE.equals(budget.getIsActive())) {
            return Optional.empty();
        }

        BigDecimal usagePercentage = budget.getUsagePercentage();

        for (BudgetAlertStrategy strategy : strategies) {
            if (strategy.shouldTrigger(budget, usagePercentage)) {
                log.info("Ngân sách [{}] đạt {}% -> Kích hoạt chiến lược: [{}]",
                        budget.getBudgetName(), usagePercentage, strategy.getClass().getSimpleName());
                return strategy.createAlert(budget, usagePercentage);
            }
        }

        return Optional.empty();
    }
}
