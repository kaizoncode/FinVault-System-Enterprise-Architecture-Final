package com.finvault.module2_wallet_ledger.repository;

import com.finvault.module2_wallet_ledger.entity.WalletMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletMemberRepository: Tầng truy xuất dữ liệu thành viên Quỹ gia đình
 * ==============================================================================
 */
@Repository
public interface WalletMemberRepository extends JpaRepository<WalletMember, Long> {

    Optional<WalletMember> findByWalletIdAndUserId(Long walletId, Long userId);

    List<WalletMember> findAllByWalletIdAndIsActiveTrue(Long walletId);

    List<WalletMember> findAllByUserIdAndIsActiveTrue(Long userId);

    boolean existsByWalletIdAndUserId(Long walletId, Long userId);

    void deleteByWalletIdAndUserId(Long walletId, Long userId);
}
