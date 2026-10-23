package com.finvault.module3_budget_goals.entity;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalStatus: Trạng thái tiến độ của Quỹ mục tiêu tiết kiệm
 *
 * Các trạng thái:
 * - IN_PROGRESS: Đang trong quá trình tích lũy
 * - ACHIEVED: Đã đạt 100% mục tiêu tiết kiệm
 * - CANCELLED: Đã hủy bỏ hoặc tạm dừng mục tiêu
 * ==============================================================================
 */
public enum SavingGoalStatus {
    IN_PROGRESS,
    ACHIEVED,
    CANCELLED
}
