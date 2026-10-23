package com.finvault.module2_wallet_ledger.service;

import com.finvault.module2_wallet_ledger.dto.WalletMemberDto;
import com.finvault.module2_wallet_ledger.dto.WalletRequest;
import com.finvault.module2_wallet_ledger.dto.WalletResponseDto;
import com.finvault.module2_wallet_ledger.entity.Wallet;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletService: Interface định nghĩa các nghiệp vụ quản lý Ví và Quỹ tài chính gia đình
 * ==============================================================================
 */
public interface WalletService {

    WalletResponseDto createWallet(String username, WalletRequest request);

    WalletResponseDto getWalletById(Long walletId, String username);

    Wallet getWalletEntityById(Long walletId);

    Wallet getWalletEntityForUpdate(Long walletId);

    List<WalletResponseDto> getUserWallets(String username);

    List<WalletResponseDto> getFamilyWallets(String username);

    WalletResponseDto updateWallet(Long walletId, String username, WalletRequest request);

    void deleteWallet(Long walletId, String username);

    WalletResponseDto addFamilyMember(Long walletId, String ownerUsername, WalletMemberDto dto);

    WalletResponseDto removeFamilyMember(Long walletId, String ownerUsername, Long memberUserId);

    WalletResponseDto updateMemberSpendingLimit(Long walletId, String ownerUsername, Long memberUserId, BigDecimal newLimit);
}
