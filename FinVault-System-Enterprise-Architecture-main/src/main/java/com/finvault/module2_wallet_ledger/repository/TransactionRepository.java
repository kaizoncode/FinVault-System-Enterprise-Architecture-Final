package com.finvault.module2_wallet_ledger.repository;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionRepository: Tầng truy xuất dữ liệu Giao dịch & Hỗ trợ Đối soát Sổ cái
 *
 * Tính năng chính:
 * - Truy vấn phân trang lịch sử giao dịch theo Ví, Người dùng, Khoảng thời gian
 * - Tra cứu các khoản chi gia đình đang chờ duyệt (PENDING)
 * - Các hàm tổng hợp (SUM) phục vụ thuật toán đối soát số dư toàn vẹn (Reconciliation)
 * ==============================================================================
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndIsDeletedFalse(Long id);

    List<Transaction> findAllByWalletIdAndIsDeletedFalseOrderByTransactionDateDesc(Long walletId);

    Page<Transaction> findAllByWalletIdAndIsDeletedFalseOrderByTransactionDateDesc(Long walletId, Pageable pageable);

    List<Transaction> findAllByUserIdAndIsDeletedFalseOrderByTransactionDateDesc(Long userId);

    Page<Transaction> findAllByUserIdAndIsDeletedFalseOrderByTransactionDateDesc(Long userId, Pageable pageable);

    List<Transaction> findAllByIsRecurringTrueAndIsDeletedFalse();

    /**
     * Tra cứu các giao dịch đang chờ duyệt (PENDING) thuộc các ví gia đình mà người dùng là Chủ quỹ hoặc Quản trị viên
     */
    @Query("SELECT t FROM Transaction t WHERE t.approvalStatus = 'PENDING' AND t.isDeleted = false " +
           "AND t.wallet.id IN (SELECT w.id FROM Wallet w LEFT JOIN w.members m WHERE w.isFamilyWallet = true AND (w.user.id = :userId OR (m.user.id = :userId AND (m.memberRole = 'ADMIN' OR m.memberRole = 'OWNER')))) " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findPendingFamilyTransactions(@Param("userId") Long userId);

    /**
     * Lọc giao dịch theo ví và khoảng thời gian
     */
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId AND t.isDeleted = false " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsByWalletAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // =========================================================================
    // CÁC HÀM TỔNG HỢP (AGGREGATION) PHỤC VỤ ĐỐI SOÁT SỐ DƯ (RECONCILIATION)
    // =========================================================================

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND t.transactionType = 'INCOME' AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false")
    BigDecimal sumTotalIncomeByWallet(@Param("walletId") Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND t.transactionType = 'EXPENSE' AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false")
    BigDecimal sumTotalExpenseByWallet(@Param("walletId") Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND t.transactionType = 'TRANSFER' AND (t.transferDirection = 'OUTGOING' OR t.transferDirection IS NULL) AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false")
    BigDecimal sumTotalTransfersOutByWallet(@Param("walletId") Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND t.transactionType = 'TRANSFER' AND t.transferDirection = 'INCOMING' AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false")
    BigDecimal sumTotalTransfersInByWallet(@Param("walletId") Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND (t.transactionType = 'EXPENSE' OR (t.transactionType = 'TRANSFER' AND (t.transferDirection = 'OUTGOING' OR t.transferDirection IS NULL))) " +
           "AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumExpenseAndTransferByWalletAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND t.user.id = :userId AND t.transactionType = 'EXPENSE' " +
           "AND t.approvalStatus = 'APPROVED' AND t.isDeleted = false " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumMemberMonthlyExpenseByWallet(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
