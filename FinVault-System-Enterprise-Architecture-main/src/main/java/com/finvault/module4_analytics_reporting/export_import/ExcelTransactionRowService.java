package com.finvault.module4_analytics_reporting.export_import;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletMember;
import com.finvault.module2_wallet_ledger.entity.WalletMemberRole;
import com.finvault.module2_wallet_ledger.event.TransactionCreatedEvent;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.repository.WalletMemberRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ExcelTransactionRowService: Xử lý giao dịch nguyên tử cho từng dòng nhập từ file Excel.
 *
 * Tính năng kỹ thuật nâng cao:
 * - Cô lập giao dịch (Transaction Isolation): Chạy với Propagation.REQUIRES_NEW để mỗi dòng
 *   được thực hiện trong một giao dịch độc lập. Khi một dòng bị lỗi, chỉ dòng đó bị rollback
 *   thất bại, không gây hỏng toàn bộ phiên nhập hàng trăm giao dịch khác.
 * - Kiểm tra hạn mức chi tiêu thành viên gia đình (WalletMember.spendingLimit) - [H-3].
 * - Kích hoạt sự kiện TransactionCreatedEvent để liên thông tự động với Module 3 (Budget)
 *   và Module 5 (Audit Log) theo kiến trúc hướng sự kiện (EDA).
 * ==============================================================================
 */
@Service
public class ExcelTransactionRowService {

    private static final Logger log = LoggerFactory.getLogger(ExcelTransactionRowService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExcelTransactionRowService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            WalletMemberRepository walletMemberRepository,
            ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.walletMemberRepository = walletMemberRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Nhập và lưu một dòng giao dịch độc lập với transaction riêng biệt.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction importSingleRow(User user,
                                       Long walletId,
                                       Category category,
                                       TransactionType txType,
                                       BigDecimal amount,
                                       LocalDateTime txDate,
                                       String description,
                                       BigDecimal approvalThreshold) {
        if (walletId == null) {
            throw new BusinessException("WALLET_NOT_FOUND", "Không xác định được ví tài chính nhận giao dịch", HttpStatus.BAD_REQUEST);
        }

        Wallet targetWallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví ID: " + walletId, HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(targetWallet.getIsActive())) {
            throw new BusinessException("WALLET_INACTIVE", "Ví đã bị đóng băng hoặc ngưng hoạt động", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra quyền hạn trên ví (Chống lỗ hổng IDOR - [H-3])
        boolean isOwner = targetWallet.getUser().getId().equals(user.getId());
        Optional<WalletMember> memberOpt = walletMemberRepository.findByWalletIdAndUserId(targetWallet.getId(), user.getId());
        boolean isMember = memberOpt.map(m -> Boolean.TRUE.equals(m.getIsActive())).orElse(false);

        if (!isOwner && !isMember) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền thêm giao dịch vào ví ID: " + targetWallet.getId(), HttpStatus.FORBIDDEN);
        }
        if (!isOwner && memberOpt.isPresent() && memberOpt.get().getMemberRole() == WalletMemberRole.VIEWER) {
            throw new BusinessException("FORBIDDEN", "Thành viên với quyền VIEWER không được thêm giao dịch vào ví", HttpStatus.FORBIDDEN);
        }

        // 1. Kiểm tra hạn mức chi tiêu thành viên gia đình (WalletMember.getSpendingLimit)
        if (memberOpt.isPresent()) {
            WalletMember member = memberOpt.get();
            if (txType == TransactionType.EXPENSE
                    && member.getSpendingLimit() != null
                    && member.getSpendingLimit().compareTo(BigDecimal.ZERO) > 0) {
                if (amount.compareTo(member.getSpendingLimit()) > 0) {
                    throw new BusinessException("SPENDING_LIMIT_EXCEEDED",
                            String.format("Khoản chi (%s VND) vượt quá hạn mức cá nhân được cấp (%s VND)",
                                    amount.toPlainString(), member.getSpendingLimit().toPlainString()), HttpStatus.BAD_REQUEST);
                }
            }
        }

        // Quy trình phê duyệt quỹ gia đình - [L-C3]
        boolean isFamilyWallet = Boolean.TRUE.equals(targetWallet.getIsFamilyWallet());
        boolean requiresApproval = isFamilyWallet
                && (txType == TransactionType.EXPENSE)
                && (approvalThreshold != null && amount.compareTo(approvalThreshold) >= 0)
                && !isOwner;

        Transaction tx = new Transaction(
                targetWallet,
                user,
                category,
                txType,
                amount,
                txDate,
                (description != null && !description.isBlank()) ? description : "Nhập từ file Excel"
        );

        if (requiresApproval) {
            // Tạm thời KHÔNG trừ tiền ví, chuyển sang trạng thái chờ duyệt
            tx.setApprovalStatus(ApprovalStatus.PENDING);
        } else {
            // Cập nhật số dư ví
            if (txType == TransactionType.INCOME) {
                targetWallet.deposit(amount);
            } else if (txType == TransactionType.EXPENSE) {
                targetWallet.withdraw(amount);
            } else if (txType == TransactionType.TRANSFER) {
                String lowerDesc = (description != null) ? description.trim().toLowerCase() : "";
                boolean isIncoming = lowerDesc.startsWith("nhận từ") || lowerDesc.startsWith("[saving_withdraw") || lowerDesc.startsWith("hoàn tiền");
                tx.setTransferDirection(isIncoming ? TransferDirection.INCOMING : TransferDirection.OUTGOING);
                if (isIncoming) {
                    targetWallet.deposit(amount);
                } else {
                    targetWallet.withdraw(amount);
                }
            }
            walletRepository.save(targetWallet);
            tx.setApprovalStatus(ApprovalStatus.APPROVED);
        }

        Transaction savedTx = transactionRepository.save(tx);

        // 2. Sau khi lưu thành công, phát sự kiện TransactionCreatedEvent nếu đã APPROVED
        if (savedTx.getApprovalStatus() == ApprovalStatus.APPROVED) {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
        }

        return savedTx;
    }
}
