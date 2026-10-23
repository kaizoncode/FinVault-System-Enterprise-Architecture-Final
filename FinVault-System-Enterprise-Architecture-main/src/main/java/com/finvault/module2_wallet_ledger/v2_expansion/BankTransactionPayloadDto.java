package com.finvault.module2_wallet_ledger.v2_expansion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * [Extension Point - V2.0 Future Expansion 2]: BankTransactionPayloadDto
 * DTO chứa dữ liệu biến động số dư nhận từ Open Banking Webhook
 * ==============================================================================
 */
public class BankTransactionPayloadDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transactionRef;
    private String bankAccountNumber;
    private String bankCode;
    private BigDecimal amount;
    private String currency;
    private String transactionType; // CR = Credit (Thu), DR = Debit (Chi)
    private String description;
    private LocalDateTime transactionTime;
    private String signature;

    public BankTransactionPayloadDto() {
    }

    public BankTransactionPayloadDto(String transactionRef, String bankAccountNumber, String bankCode,
                                     BigDecimal amount, String currency, String transactionType,
                                     String description, LocalDateTime transactionTime) {
        this.transactionRef = transactionRef;
        this.bankAccountNumber = bankAccountNumber;
        this.bankCode = bankCode;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.description = description;
        this.transactionTime = transactionTime;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
