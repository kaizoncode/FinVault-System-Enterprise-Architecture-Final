package com.finvault.module3_budget_goals.entity;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * AllocationType: Phân loại nhóm chi tiêu theo Quy tắc 50/30/20
 *
 * Các nhóm phân bổ:
 * - NEEDS: 50% Thu nhập dành cho Nhu cầu thiết yếu (Tiền nhà, Ăn uống, Điện nước, Học phí)
 * - WANTS: 30% Thu nhập dành cho Sở thích cá nhân (Giải trí, Mua sắm, Du lịch, Cà phê)
 * - SAVINGS: 20% Thu nhập dành cho Tích lũy tiết kiệm & Đầu tư tài chính
 * ==============================================================================
 */
public enum AllocationType {
    NEEDS,
    WANTS,
    SAVINGS
}
