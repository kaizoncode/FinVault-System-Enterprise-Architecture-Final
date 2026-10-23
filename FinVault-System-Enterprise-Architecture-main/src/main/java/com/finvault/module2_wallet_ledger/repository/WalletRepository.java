package com.finvault.module2_wallet_ledger.repository;

import com.finvault.module2_wallet_ledger.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletRepository: Tầng truy xuất dữ liệu Ví tài chính & Chống Race Condition
 *
 * Tính năng chống Race Condition cốt lõi:
 * - @Lock(LockModeType.PESSIMISTIC_WRITE): Khóa hàng ví tại tầng Database (SELECT ... FOR UPDATE)
 *   ngăn chặn hiện tượng 2 tiến trình rút/chuyển tiền đồng thời làm sai lệch số dư.
 * - Hỗ trợ tra cứu ví gia đình mà người dùng là chủ sở hữu HOẶC là thành viên tham gia.
 * ==============================================================================
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByIdAndIsDeletedFalse(Long id);

    /**
     * Khóa bi quan (Pessimistic Lock) khi cập nhật số dư hoặc chuyển tiền nhằm chống Race Condition
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id AND w.isDeleted = false")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    /**
     * Khóa bi quan (Pessimistic Lock) kèm kiểm tra chủ sở hữu ví (chống IDOR & Race Condition)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId AND w.user.id = :userId AND w.isDeleted = false")
    Optional<Wallet> findByIdAndUserIdForUpdate(@Param("walletId") Long walletId, @Param("userId") Long userId);

    List<Wallet> findAllByUserIdAndIsDeletedFalse(Long userId);

    List<Wallet> findAllByIsDeletedFalse();

    /**
     * Tra cứu tất cả các ví gia đình mà người dùng có quyền truy cập (là chủ sở hữu HOẶC thành viên)
     */
    @Query("SELECT DISTINCT w FROM Wallet w LEFT JOIN w.members m WHERE (w.isFamilyWallet = true OR w.walletType = com.finvault.module2_wallet_ledger.entity.WalletType.FAMILY_FUND) AND w.isDeleted = false AND (w.user.id = :userId OR (m.user.id = :userId AND m.isActive = true))")
    List<Wallet> findFamilyWalletsAccessibleByUser(@Param("userId") Long userId);

    /**
     * Tra cứu toàn bộ ví (cá nhân và gia đình) mà người dùng có quyền xem/thao tác
     */
    @Query("SELECT DISTINCT w FROM Wallet w LEFT JOIN w.members m WHERE w.isDeleted = false AND w.isActive = true AND (w.user.id = :userId OR (m.user.id = :userId AND m.isActive = true))")
    List<Wallet> findAllAccessibleWalletsByUser(@Param("userId") Long userId);

    long countByUserIdAndIsDeletedFalse(Long userId);
}
