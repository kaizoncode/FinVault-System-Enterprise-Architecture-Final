package com.finvault.module2_wallet_ledger.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * ReconciliationReportDto: Báo cáo đối soát tính toàn vẹn số dư sổ cái (Ledger Reconciliation)
 *
 * Tiêu chí đối soát:
 * - Số dư thực tế trong ví (actualBalance)
 * - Số dư tính toán từ lịch sử giao dịch (calculatedBalance) = Tổng thu - Tổng chi - Tổng chuyển đi + Tổng nhận
 * - Chênh lệch (discrepancy) = actualBalance - calculatedBalance
 * - Cờ cảnh báo bất thường (isDiscrepancyDetected)
 * ==============================================================================
 */
public class ReconciliationReportDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long walletId;
    private String walletName;
    private BigDecimal actualBalance;
    private BigDecimal initialBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalTransfersOut;
    private BigDecimal totalTransfersIn;
    private BigDecimal netFlow;
    private BigDecimal calculatedBalance;
    private BigDecimal discrepancy;
    private Boolean isDiscrepancyDetected;
    private LocalDateTime checkedAt;
    private String statusMessage;

    public ReconciliationReportDto() {
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public BigDecimal getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(BigDecimal actualBalance) {
        this.actualBalance = actualBalance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getTotalTransfersOut() {
        return totalTransfersOut;
    }

    public void setTotalTransfersOut(BigDecimal totalTransfersOut) {
        this.totalTransfersOut = totalTransfersOut;
    }

    public BigDecimal getTotalTransfersIn() {
        return totalTransfersIn;
    }

    public void setTotalTransfersIn(BigDecimal totalTransfersIn) {
        this.totalTransfersIn = totalTransfersIn;
    }

    public BigDecimal getNetFlow() {
        return netFlow;
    }

    public void setNetFlow(BigDecimal netFlow) {
        this.netFlow = netFlow;
    }

    public BigDecimal getCalculatedBalance() {
        return calculatedBalance;
    }

    public void setCalculatedBalance(BigDecimal calculatedBalance) {
        this.calculatedBalance = calculatedBalance;
    }

    public BigDecimal getDiscrepancy() {
        return discrepancy;
    }

    public void setDiscrepancy(BigDecimal discrepancy) {
        this.discrepancy = discrepancy;
    }

    public Boolean getIsDiscrepancyDetected() {
        return isDiscrepancyDetected;
    }

    public void setIsDiscrepancyDetected(Boolean discrepancyDetected) {
        isDiscrepancyDetected = discrepancyDetected;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
