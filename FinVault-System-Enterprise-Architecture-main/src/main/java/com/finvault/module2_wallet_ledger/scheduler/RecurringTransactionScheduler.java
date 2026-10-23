package com.finvault.module2_wallet_ledger.scheduler;

import com.finvault.module2_wallet_ledger.dto.TransactionRequest;
import com.finvault.module2_wallet_ledger.dto.TransferRequest;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * RecurringTransactionScheduler: Lập lịch tự động khởi tạo giao dịch định kỳ (Daily, Weekly, Monthly, Yearly)
 *
 * Lịch trình thực thi:
 * - Chạy tự động lúc 06:00:00 sáng mỗi ngày (Cron: 0 0 6 * * ?)
 * - Quét các giao dịch có isRecurring = true, kiểm tra chu kỳ và tự động tạo giao dịch mới
 * ==============================================================================
 */
@Component
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public RecurringTransactionScheduler(TransactionRepository transactionRepository,
                                         TransactionService transactionService) {
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    /**
     * Tự động quét và xử lý các giao dịch định kỳ đến hạn vào 06:00:00 mỗi ngày
     */
    @Scheduled(cron = "${finvault.transaction.recurring-cron:0 0 6 * * ?}")
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        log.info("Bắt đầu tác vụ kiểm tra giao dịch định kỳ cho ngày: {}", today);

        List<Transaction> recurringTemplates = transactionRepository.findAllByIsRecurringTrueAndIsDeletedFalse();
        if (recurringTemplates.isEmpty()) {
            log.info("Không có giao dịch định kỳ nào cần thực thi.");
            return;
        }

        int processed = 0;
        for (Transaction template : recurringTemplates) {
            try {
                if (isDue(template, today)) {
                    executeRecurringTransaction(template, today);
                    processed++;
                }
            } catch (Exception e) {
                log.error("Lỗi khi thực thi giao dịch định kỳ ID [{}]: {}", template.getId(), e.getMessage(), e);
            }
        }
        log.info("Hoàn tất tác vụ giao dịch định kỳ. Đã thực thi thành công {} giao dịch mới.", processed);
    }

    public boolean isDue(Transaction template, LocalDate today) {
        if (template.getTransactionDate() == null || template.getUser() == null || template.getWallet() == null) {
            return false;
        }
        // Tránh chạy trùng lặp trong cùng một ngày
        if (template.getLastRecurringDate() != null && template.getLastRecurringDate().isEqual(today)) {
            return false;
        }

        LocalDate originDate = template.getTransactionDate().toLocalDate();
        // Chưa tới ngày bắt đầu giao dịch gốc
        if (originDate.isAfter(today)) {
            return false;
        }

        String interval = (template.getRecurringInterval() != null && !template.getRecurringInterval().isBlank())
                ? template.getRecurringInterval().trim().toUpperCase()
                : "MONTHLY";

        int effectiveDayOfMonth = Math.min(originDate.getDayOfMonth(), today.lengthOfMonth());
        return switch (interval) {
            case "DAILY" -> !originDate.isEqual(today);
            case "WEEKLY" -> !originDate.isEqual(today) && originDate.getDayOfWeek() == today.getDayOfWeek();
            case "MONTHLY" -> !originDate.isEqual(today) && today.getDayOfMonth() == effectiveDayOfMonth;
            case "YEARLY" -> !originDate.isEqual(today) && originDate.getMonth() == today.getMonth() && today.getDayOfMonth() == effectiveDayOfMonth;
            default -> false;
        };
    }

    public void executeRecurringTransaction(Transaction template, LocalDate today) {
        String username = template.getUser().getUsername();

        if (template.getTransactionType() == TransactionType.TRANSFER && template.getTargetWallet() != null) {
            // Đối với giao dịch chuyển khoản kép (Debit & Credit), chỉ xử lý bản ghi OUTGOING để tránh chuyển tiền 2 lần
            if (template.getTransferDirection() != null && template.getTransferDirection() != TransferDirection.OUTGOING) {
                return;
            }
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setSourceWalletId(template.getWallet().getId());
            transferRequest.setTargetWalletId(template.getTargetWallet().getId());
            transferRequest.setAmount(template.getAmount());
            transferRequest.setNote(template.getNote() != null ? template.getNote() + " (Định kỳ)" : "Chuyển tiền định kỳ");
            transactionService.transferFunds(username, transferRequest);
        } else {
            TransactionRequest request = new TransactionRequest();
            request.setWalletId(template.getWallet().getId());
            request.setCategoryId(template.getCategory() != null ? template.getCategory().getId() : null);
            request.setTransactionType(template.getTransactionType());
            request.setAmount(template.getAmount());
            request.setTransactionDate(today.atTime(template.getTransactionDate().toLocalTime()));
            request.setNote(template.getNote() != null ? template.getNote() + " (Định kỳ)" : "Giao dịch định kỳ");
            request.setReceiptUrl(template.getReceiptUrl());
            request.setIsRecurring(false);
            transactionService.createTransaction(username, request);
        }

        template.setLastRecurringDate(today);
        transactionRepository.save(template);
        log.info("Đã tạo thành công giao dịch định kỳ mới từ mẫu [ID: {}] cho người dùng [{}]", template.getId(), username);
    }
}
