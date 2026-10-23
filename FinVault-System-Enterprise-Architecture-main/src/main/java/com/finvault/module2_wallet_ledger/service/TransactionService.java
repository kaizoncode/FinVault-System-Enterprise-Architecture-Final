package com.finvault.module2_wallet_ledger.service;

import com.finvault.module2_wallet_ledger.dto.*;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionService: Interface định nghĩa các nghiệp vụ Lõi Giao dịch & Quản lý Sổ cái
 *
 * Tính năng kỹ thuật nâng cao:
 * - Giao dịch ACID chống Race Condition với Pessimistic Locking
 * - Chống Deadlock khi chuyển tiền liên ví theo thuật toán Global Resource Ordering
 * - Quy trình phê duyệt chi tiêu gia đình tự động kích hoạt khi vượt ngưỡng 5,000,000 VND
 * - Phát sự kiện TransactionCreatedEvent (Observer Pattern) kết nối Module 3 và Module 5
 * ==============================================================================
 */
public interface TransactionService {

    TransactionResponseDto createTransaction(String username, TransactionRequest request);

    TransactionResponseDto transferFunds(String username, TransferRequest request);

    TransactionResponseDto approveOrRejectTransaction(Long transactionId, String approverUsername, ApprovalDecisionDto decision);

    Page<TransactionResponseDto> getTransactionsByWallet(Long walletId, String username, Pageable pageable);

    Page<TransactionResponseDto> getUserTransactions(String username, Pageable pageable);

    List<TransactionResponseDto> getPendingFamilyTransactions(String approverUsername);

    TransactionResponseDto getTransactionById(Long transactionId, String username);

    Transaction getTransactionEntityById(Long transactionId);

    TransactionResponseDto updateTransaction(Long transactionId, String username, TransactionRequest request);

    void deleteTransaction(Long transactionId, String username);
}
