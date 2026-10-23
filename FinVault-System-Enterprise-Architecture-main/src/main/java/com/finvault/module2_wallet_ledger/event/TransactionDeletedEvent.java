package com.finvault.module2_wallet_ledger.event;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionDeletedEvent: Sự kiện xóa/soft-delete giao dịch tài chính
 *
 * Áp dụng Observer Pattern / Event-Driven Architecture:
 * - Module 3 (Budget Automation) lắng nghe sự kiện này để tự động giảm spentAmount
 *   của các ngân sách tương ứng, đảm bảo tính nhất quán giữa Sổ cái và Ngân sách.
 * - Module 5 (Audit Log & CMS) lắng nghe sự kiện để ghi vết kiểm toán tài chính bất đồng bộ.
 *
 * Cải tiến Kiến trúc (Chống LazyInitializationException trong @Async listener):
 * - Đóng gói đầy đủ các giá trị primitive/immutable ID (userId, categoryId, walletId, amount, ...)
 *   được trích xuất an toàn ngay trong transaction context của luồng xóa sự kiện.
 * ==============================================================================
 */
public class TransactionDeletedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Long transactionId;
    private final Long userId;
    private final String username;
    private final Long walletId;
    private final Long categoryId;
    private final BigDecimal amount;
    private final TransactionType transactionType;
    private final LocalDateTime transactionDate;
    private final ApprovalStatus approvalStatus;

    private final Transaction transaction;

    public TransactionDeletedEvent(Object source, Long transactionId, Long userId, String username,
                                   Long walletId, Long categoryId, BigDecimal amount,
                                   TransactionType transactionType, LocalDateTime transactionDate,
                                   ApprovalStatus approvalStatus) {
        super(source);
        this.transactionId = transactionId;
        this.userId = userId;
        this.username = username;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.approvalStatus = approvalStatus;
        this.transaction = null;
    }

    public TransactionDeletedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
        if (transaction != null) {
            this.transactionId = transaction.getId();
            this.userId = (transaction.getUser() != null) ? transaction.getUser().getId() : null;
            this.username = (transaction.getUser() != null) ? transaction.getUser().getUsername() : null;
            this.walletId = (transaction.getWallet() != null) ? transaction.getWallet().getId() : null;
            this.categoryId = (transaction.getCategory() != null) ? transaction.getCategory().getId() : null;
            this.amount = transaction.getAmount();
            this.transactionType = transaction.getTransactionType();
            this.transactionDate = transaction.getTransactionDate();
            this.approvalStatus = transaction.getApprovalStatus();
        } else {
            this.transactionId = null;
            this.userId = null;
            this.username = null;
            this.walletId = null;
            this.categoryId = null;
            this.amount = null;
            this.transactionType = null;
            this.transactionDate = null;
            this.approvalStatus = null;
        }
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Long getWalletId() {
        return walletId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    @Override
    public String toString() {
        return "TransactionDeletedEvent{" +
                "transactionId=" + transactionId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", walletId=" + walletId +
                ", categoryId=" + categoryId +
                ", amount=" + amount +
                ", transactionType=" + transactionType +
                ", transactionDate=" + transactionDate +
                ", approvalStatus=" + approvalStatus +
                '}';
    }
}
