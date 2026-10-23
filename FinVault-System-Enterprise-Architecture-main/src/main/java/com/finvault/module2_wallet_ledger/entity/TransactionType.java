package com.finvault.module2_wallet_ledger.entity;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionType: Phân loại hình thức giao dịch tài chính
 *
 * Các hình thức:
 * - INCOME: Thu tiền vào ví
 * - EXPENSE: Chi tiền ra khỏi ví
 * - TRANSFER: Chuyển khoản nội bộ giữa hai ví
 * ==============================================================================
 */
public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
