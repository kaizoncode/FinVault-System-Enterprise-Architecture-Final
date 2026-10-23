package com.finvault.module3_budget_goals.rule;

import com.finvault.module3_budget_goals.dto.BudgetAlertNotificationDto;
import com.finvault.module3_budget_goals.entity.Budget;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetAlertStrategy: Interface chiến lược cảnh báo ngân sách (Strategy Pattern)
 *
 * Cho phép linh hoạt mở rộng các ngưỡng cảnh báo mới (ví dụ: cảnh báo 50%, 90% hoặc dự báo AI)
 * mà không phải sửa đổi mã nguồn của Rule Engine (Open/Closed Principle).
 * ==============================================================================
 */
public interface BudgetAlertStrategy {

    /**
     * Kiểm tra xem chiến lược này có áp dụng cho tình trạng ngân sách hiện tại hay không
     */
    boolean shouldTrigger(Budget budget, BigDecimal usagePercentage);

    /**
     * Thực thi chiến lược, tạo thông báo cảnh báo và cập nhật cờ trạng thái trên thực thể Budget
     */
    Optional<BudgetAlertNotificationDto> createAlert(Budget budget, BigDecimal usagePercentage);

    /**
     * Mức độ ưu tiên của chiến lược (Số càng lớn độ ưu tiên càng cao)
     */
    int getPriority();
}
