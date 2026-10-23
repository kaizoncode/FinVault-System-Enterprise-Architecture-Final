package com.finvault.module5_admin_audit.service;

import com.finvault.module5_admin_audit.dto.DataRecoveryResponseDto;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SupportTicketRecoveryService: Interface xử lý khôi phục dữ liệu đã bị xóa mềm (Soft Delete Recovery)
 * ==============================================================================
 */
public interface SupportTicketRecoveryService {

    List<DataRecoveryResponseDto> getSoftDeletedItems();

    void restoreWallet(Long walletId, String adminUsername);

    void restoreTransaction(Long transactionId, String adminUsername);

    void restoreBudget(Long budgetId, String adminUsername);

    void restoreSavingGoal(Long goalId, String adminUsername);
}
