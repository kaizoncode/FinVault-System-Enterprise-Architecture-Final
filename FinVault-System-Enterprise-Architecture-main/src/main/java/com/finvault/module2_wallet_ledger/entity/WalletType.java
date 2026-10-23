package com.finvault.module2_wallet_ledger.entity;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletType: Phân loại các định dạng ví tài chính trong hệ thống
 *
 * Các loại ví hỗ trợ:
 * - CASH: Ví tiền mặt hàng ngày
 * - BANK: Tài khoản ngân hàng liên kết
 * - CREDIT_CARD: Thẻ tín dụng (có hạn mức chi tiêu)
 * - FAMILY_FUND: Quỹ tài chính chung gia đình (có cơ chế phân quyền đa thành viên)
 * - SAVINGS: Sổ tiết kiệm tích lũy
 * ==============================================================================
 */
public enum WalletType {
    CASH,
    BANK,
    CREDIT_CARD,
    FAMILY_FUND,
    SAVINGS
}
