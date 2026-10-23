package com.finvault.module4_analytics_reporting.dto;

import com.finvault.module2_wallet_ledger.entity.TransactionType;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * TransactionExportFilterRequest: DTO tiêu chí lọc giao dịch phục vụ xuất báo cáo sao kê PDF/Excel
 * ==============================================================================
 */
public class TransactionExportFilterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long walletId;
    private Long categoryId;
    private TransactionType transactionType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String keyword;

    public TransactionExportFilterRequest() {
    }

    public TransactionExportFilterRequest(Long walletId, Long categoryId, TransactionType transactionType,
                                          LocalDate startDate, LocalDate endDate, String keyword) {
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.transactionType = transactionType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.keyword = keyword;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
