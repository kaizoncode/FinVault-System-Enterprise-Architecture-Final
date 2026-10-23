package com.finvault.module3_budget_goals.entity;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetType: Phân loại hình thức quản lý ngân sách
 *
 * Các loại ngân sách:
 * - CUSTOM: Ngân sách người dùng tự định nghĩa theo danh mục và số tiền
 * - RULE_50_30_20: Ngân sách quản lý theo quy tắc chuẩn 50/30/20
 * - STANDARD_TEMPLATE: Mẫu ngân sách chuẩn hệ thống do Super Admin khởi tạo
 * ==============================================================================
 */
public enum BudgetType {
    CUSTOM,
    RULE_50_30_20,
    STANDARD_TEMPLATE
}
