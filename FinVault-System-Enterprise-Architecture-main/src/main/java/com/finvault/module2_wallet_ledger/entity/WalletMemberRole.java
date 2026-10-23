package com.finvault.module2_wallet_ledger.entity;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletMemberRole: Quyền hạn của thành viên tham gia Quỹ gia đình
 *
 * Phân quyền trong Quỹ gia đình:
 * - OWNER: Chủ sở hữu sáng lập Quỹ gia đình (Toàn quyền quản trị quỹ)
 * - ADMIN: Quản trị viên phụ của ví (có thể duyệt chi, mời thành viên)
 * - MEMBER: Thành viên tiêu chuẩn (được chi tiêu trong hạn mức spending_limit)
 * - VIEWER: Thành viên chỉ xem (không thể tạo giao dịch chi tiêu)
 * ==============================================================================
 */
public enum WalletMemberRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}
