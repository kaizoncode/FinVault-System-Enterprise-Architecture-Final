package com.finvault.module2_wallet_ledger.entity;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * ApprovalStatus: Trạng thái phê duyệt chi tiêu của Quỹ gia đình
 *
 * Quy trình phê duyệt:
 * - PENDING: Giao dịch lớn (>= 5,000,000 VND) đang chờ Gia trưởng duyệt
 * - APPROVED: Giao dịch đã được duyệt hoặc khoản chi thông thường đã hoàn tất
 * - REJECTED: Giao dịch bị Gia trưởng từ chối (không trừ tiền ví)
 * ==============================================================================
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
