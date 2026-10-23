package com.finvault.module3_budget_goals.rule;

import com.finvault.module3_budget_goals.dto.BudgetAlertNotificationDto;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module5_admin_audit.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * Critical100AlertStrategy: Chiến lược cảnh báo đỏ khi chi tiêu chạm hoặc vượt 100% ngân sách
 *
 * Tiêu chí kích hoạt:
 * - Tỷ lệ sử dụng >= criticalThresholdPercent (Mặc định: 100.0%)
 * - Cờ alertThreshold100Sent == false (Chưa từng gửi cảnh báo 100% trong chu kỳ này)
 * ==============================================================================
 */
@Component
public class Critical100AlertStrategy implements BudgetAlertStrategy {

    @Value("${finvault.budget.critical-threshold-percent:100.0}")
    private BigDecimal criticalThreshold = new BigDecimal("100.0");

    private SystemConfigService systemConfigService;

    public Critical100AlertStrategy() {
    }

    @Autowired
    public Critical100AlertStrategy(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    private BigDecimal getEffectiveCriticalThreshold() {
        BigDecimal fallback = (criticalThreshold != null) ? criticalThreshold : new BigDecimal("100.0");
        if (systemConfigService != null) {
            try {
                String raw = systemConfigService.getConfigValue("SYSTEM_ALERT_THRESHOLD_CRITICAL", fallback.toPlainString());
                if (raw != null && !raw.isBlank()) {
                    return new BigDecimal(raw.trim());
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    @Override
    public boolean shouldTrigger(Budget budget, BigDecimal usagePercentage) {
        if (Boolean.TRUE.equals(budget.getAlertThreshold100Sent())) {
            return false;
        }
        return usagePercentage.compareTo(getEffectiveCriticalThreshold()) >= 0;
    }

    @Override
    public Optional<BudgetAlertNotificationDto> createAlert(Budget budget, BigDecimal usagePercentage) {
        budget.setAlertThreshold100Sent(true);

        String categoryName = (budget.getCategory() != null) ? budget.getCategory().getCategoryName() : "Toàn bộ danh mục";
        String message = String.format("NGUY CƠ VƯỢT HẠN MỨC (100%%): Ngân sách '%s' [%s] đã đạt %s%%! Bạn đã chi tiêu %s VND vượt quá hoặc chạm mức ngân sách %s VND!",
                budget.getBudgetName(),
                categoryName,
                usagePercentage.toPlainString(),
                budget.getSpentAmount().toPlainString(),
                budget.getAllocatedAmount().toPlainString());

        BudgetAlertNotificationDto dto = new BudgetAlertNotificationDto(
                budget.getId(),
                budget.getBudgetName(),
                categoryName,
                BudgetAlertNotificationDto.AlertSeverity.CRITICAL_100,
                budget.getAllocatedAmount(),
                budget.getSpentAmount(),
                budget.getRemainingAmount(),
                usagePercentage,
                message
        );

        return Optional.of(dto);
    }

    @Override
    public int getPriority() {
        return 20; // Độ ưu tiên cao nhất
    }
}
