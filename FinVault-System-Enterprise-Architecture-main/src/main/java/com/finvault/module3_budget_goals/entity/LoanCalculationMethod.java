package com.finvault.module3_budget_goals.entity;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculationMethod: Phương pháp tính lãi suất và lịch trình trả nợ ngân hàng
 *
 * Các phương pháp tính:
 * - EQUAL_INSTALLMENTS: Trả góp đều định kỳ (Niên kim cố định / Annuity Method)
 *   Số tiền trả mỗi kỳ bằng nhau gồm cả gốc và lãi.
 * - REDUCING_BALANCE: Trả theo dư nợ giảm dần (Reducing Balance Method)
 *   Tiền gốc chia đều hàng tháng, tiền lãi tính trên số dư nợ thực tế còn lại.
 * ==============================================================================
 */
public enum LoanCalculationMethod {
    EQUAL_INSTALLMENTS,
    REDUCING_BALANCE
}
