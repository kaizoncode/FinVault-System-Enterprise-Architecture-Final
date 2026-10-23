package com.finvault.module2_wallet_ledger.entity;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransferDirection: Xác định chiều của giao dịch chuyển tiền liên ví (Double-Entry Ledger)
 * - OUTGOING: Ví nguồn trích tiền chuyển đi (Debit / Ghi nợ)
 * - INCOMING: Ví đích nhận tiền chuyển đến (Credit / Ghi có)
 * ==============================================================================
 */
public enum TransferDirection {
    OUTGOING,
    INCOMING
}
