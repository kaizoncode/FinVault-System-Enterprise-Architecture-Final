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
 * Warning80AlertStrategy: Chiến lược cảnh báo vàng khi chi tiêu chạm ngưỡng 80%
 *
 * Tiêu chí kích hoạt:
 * - Tỷ lệ sử dụng >= warningThresholdPercent (Mặc định: 80.0%)
 * - Tỷ lệ sử dụng < criticalThresholdPercent (Mặc định: 100.0%)
 * - Cờ alertThreshold80Sent == false (Chưa từng gửi cảnh báo trong chu kỳ này)
 * ==============================================================================
 */
@Component
public class Warning80AlertStrategy implements BudgetAlertStrategy {

    @Value("${finvault.budget.warning-threshold-percent:80.0}")
    private BigDecimal warningThreshold = new BigDecimal("80.0");

    @Value("${finvault.budget.critical-threshold-percent:100.0}")
    private BigDecimal criticalThreshold = new BigDecimal("100.0");

    private SystemConfigService systemConfigService;

    public Warning80AlertStrategy() {
    }

    @Autowired
    public Warning80AlertStrategy(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    private BigDecimal getEffectiveWarningThreshold() {
        BigDecimal fallback = (warningThreshold != null) ? warningThreshold : new BigDecimal("80.0");
        if (systemConfigService != null) {
            try {
                String raw = systemConfigService.getConfigValue("SYSTEM_ALERT_THRESHOLD_WARNING", fallback.toPlainString());
                if (raw != null && !raw.isBlank()) {
                    return new BigDecimal(raw.trim());
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
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
        if (Boolean.TRUE.equals(budget.getAlertThreshold80Sent())) {
            return false;
        }
        return usagePercentage.compareTo(getEffectiveWarningThreshold()) >= 0
                && usagePercentage.compareTo(getEffectiveCriticalThreshold()) < 0;
    }

    @Override
    public Optional<BudgetAlertNotificationDto> createAlert(Budget budget, BigDecimal usagePercentage) {
        budget.setAlertThreshold80Sent(true);

        String categoryName = (budget.getCategory() != null) ? budget.getCategory().getCategoryName() : "Toàn bộ danh mục";
        String message = String.format("CẢNH BÁO NGÂN SÁCH (80%%): Ngân sách '%s' [%s] đã sử dụng %s%% (%s VND / %s VND). Hãy cân nhắc điều chỉnh chi tiêu!",
                budget.getBudgetName(),
                categoryName,
                usagePercentage.toPlainString(),
                budget.getSpentAmount().toPlainString(),
                budget.getAllocatedAmount().toPlainString());

        BudgetAlertNotificationDto dto = new BudgetAlertNotificationDto(
                budget.getId(),
                budget.getBudgetName(),
                categoryName,
                BudgetAlertNotificationDto.AlertSeverity.WARNING_80,
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
        return 10;
    }
}
