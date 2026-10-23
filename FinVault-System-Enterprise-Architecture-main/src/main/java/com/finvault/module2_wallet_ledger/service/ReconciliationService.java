package com.finvault.module2_wallet_ledger.service;

import com.finvault.module2_wallet_ledger.dto.ReconciliationReportDto;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * ReconciliationService: Dịch vụ đối soát tính toàn vẹn số dư sổ cái (Ledger Reconciliation)
 *
 * Tiêu chí đối soát:
 * - So sánh số dư hiện tại trong ví (Actual Balance) với tổng dòng tiền phát sinh
 *   từ toàn bộ lịch sử giao dịch (Calculated Balance = Thu - Chi - Chuyển đi + Nhận).
 * - Phát hiện sai lệch số dư (Discrepancy Detection) do gian lận, lỗi hệ thống hoặc Race Condition.
 * ==============================================================================
 */
public interface ReconciliationService {

    ReconciliationReportDto reconcileWallet(Long walletId, String username);

    List<ReconciliationReportDto> reconcileAllWallets(String auditorOrAdminUsername);
}
