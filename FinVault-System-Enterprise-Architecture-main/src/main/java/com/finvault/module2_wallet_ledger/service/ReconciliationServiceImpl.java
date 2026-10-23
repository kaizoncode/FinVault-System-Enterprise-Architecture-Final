package com.finvault.module2_wallet_ledger.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module2_wallet_ledger.dto.ReconciliationReportDto;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * ReconciliationServiceImpl: Triển khai thuật toán đối soát số dư toàn vẹn sổ cái
 *
 * Thuật toán đối soát (Reconciliation Formula):
 * 1. Tổng thu (Total Income): SUM(amount) WHERE wallet_id = :id AND type = 'INCOME' AND status = 'APPROVED'
 * 2. Tổng chi (Total Expense): SUM(amount) WHERE wallet_id = :id AND type = 'EXPENSE' AND status = 'APPROVED'
 * 3. Chuyển đi (Transfers Out): SUM(amount) WHERE wallet_id = :id AND type = 'TRANSFER' AND status = 'APPROVED'
 * 4. Nhận vào (Transfers In): SUM(amount) WHERE target_wallet_id = :id AND type = 'TRANSFER' AND status = 'APPROVED'
 * 5. Dòng tiền ròng (Net Flow) = (Income + Transfers In) - (Expense + Transfers Out)
 * 6. Kiểm tra chênh lệch số dư (Discrepancy) để cảnh báo tức thì cho Auditor và Admin.
 * ==============================================================================
 */
@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationServiceImpl.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public ReconciliationServiceImpl(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            UserService userService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationReportDto reconcileWallet(Long walletId, String username) {
        User user = userService.getUserEntityByUsername(username);
        Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví ID: " + walletId, HttpStatus.NOT_FOUND));

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isAuditor = user.hasRole(Role.ROLE_SYSTEM_AUDITOR);
        boolean isOwner = wallet.getUser().getId().equals(user.getId());
        boolean isActiveMember = wallet.getMembers() != null && wallet.getMembers().stream()
                .anyMatch(m -> m.getUser() != null && m.getUser().getId().equals(user.getId()) && Boolean.TRUE.equals(m.getIsActive()));

        if (!isSuperAdmin && !isAuditor && !isOwner && !isActiveMember) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền thực hiện đối soát trên ví này", HttpStatus.FORBIDDEN);
        }

        return generateReport(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationReportDto> reconcileAllWallets(String auditorOrAdminUsername) {
        User user = userService.getUserEntityByUsername(auditorOrAdminUsername);
        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean isAuditor = user.hasRole(Role.ROLE_SYSTEM_AUDITOR);

        if (!isSuperAdmin && !isAuditor) {
            throw new BusinessException("FORBIDDEN", "Chỉ Super Admin và System Auditor mới có quyền đối soát toàn bộ hệ thống", HttpStatus.FORBIDDEN);
        }

        List<Wallet> allWallets = walletRepository.findAllByIsDeletedFalse();
        List<ReconciliationReportDto> reports = new ArrayList<>(allWallets.size());

        for (Wallet wallet : allWallets) {
            reports.add(generateReport(wallet));
        }

        log.info("Auditor/Admin [{}] đã hoàn tất đối soát số dư toàn bộ {} ví trong hệ thống.", auditorOrAdminUsername, allWallets.size());
        return reports;
    }

    private ReconciliationReportDto generateReport(Wallet wallet) {
        Long walletId = wallet.getId();
        BigDecimal actualBalance = wallet.getBalance();

        BigDecimal totalIncome = transactionRepository.sumTotalIncomeByWallet(walletId);
        BigDecimal totalExpense = transactionRepository.sumTotalExpenseByWallet(walletId);
        BigDecimal totalTransfersOut = transactionRepository.sumTotalTransfersOutByWallet(walletId);
        BigDecimal totalTransfersIn = transactionRepository.sumTotalTransfersInByWallet(walletId);

        // Net Flow tính từ các giao dịch đã hoàn tất
        BigDecimal netFlow = totalIncome.add(totalTransfersIn).subtract(totalExpense).subtract(totalTransfersOut);

        // calculatedBalance = initialBalance + netFlow
        BigDecimal initialBalance = (wallet.getInitialBalance() != null) ? wallet.getInitialBalance() : BigDecimal.ZERO;
        BigDecimal calculatedBalance = initialBalance.add(netFlow);
        BigDecimal discrepancy = actualBalance.subtract(calculatedBalance).abs();

        ReconciliationReportDto dto = new ReconciliationReportDto();
        dto.setWalletId(walletId);
        dto.setWalletName(wallet.getWalletName());
        dto.setActualBalance(actualBalance);
        dto.setInitialBalance(initialBalance);
        dto.setTotalIncome(totalIncome);
        dto.setTotalExpense(totalExpense);
        dto.setTotalTransfersOut(totalTransfersOut);
        dto.setTotalTransfersIn(totalTransfersIn);
        dto.setNetFlow(netFlow);
        dto.setCalculatedBalance(calculatedBalance);
        dto.setDiscrepancy(discrepancy);
        dto.setCheckedAt(LocalDateTime.now());

        // Kiểm tra sai số:
        if (discrepancy.compareTo(BigDecimal.ZERO) == 0) {
            dto.setIsDiscrepancyDetected(false);
            dto.setStatusMessage("Số dư sổ cái hoàn toàn khớp với lịch sử giao dịch (Toàn vẹn 100%)");
        } else {
            dto.setIsDiscrepancyDetected(true);
            dto.setStatusMessage(String.format("LƯU Ý ĐỐI SOÁT: Số dư thực tế (%s %s) lệch so với số dư tính toán (%s %s). Độ lệch: %s %s.",
                    actualBalance.toPlainString(), wallet.getCurrency(),
                    calculatedBalance.toPlainString(), wallet.getCurrency(),
                    discrepancy.toPlainString(), wallet.getCurrency()));
        }

        return dto;
    }
}
