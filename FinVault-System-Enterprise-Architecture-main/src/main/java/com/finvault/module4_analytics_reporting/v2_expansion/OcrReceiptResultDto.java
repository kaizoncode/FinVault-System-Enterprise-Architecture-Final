package com.finvault.module4_analytics_reporting.v2_expansion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * [Extension Point - V2.0 Future Expansion]: OcrReceiptResultDto
 * DTO chứa kết quả trích xuất thông tin tự động từ hóa đơn điện tử / phiếu thu bằng AI OCR
 * ==============================================================================
 */
public class OcrReceiptResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantName;       // Tên đơn vị bán / nhà hàng / siêu thị
    private String taxCode;            // Mã số thuế đơn vị phát hành hóa đơn
    private String invoiceNumber;      // Số hóa đơn VAT
    private LocalDateTime transactionDate;
    private BigDecimal totalAmount;    // Tổng số tiền thanh toán
    private BigDecimal taxAmount;      // Tiền thuế GTGT (VAT)
    private String currency = "VND";
    private String suggestedCategory;  // Danh mục đề xuất (Ăn uống, Mua sắm, v.v.)
    private Double confidenceScore;    // Độ tin cậy nhận diện (0.0 - 1.0)
    private List<OcrLineItem> lineItems = new ArrayList<>();

    public OcrReceiptResultDto() {
    }

    public static class OcrLineItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public OcrLineItem() {
        }

        public OcrLineItem(String itemName, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSuggestedCategory() {
        return suggestedCategory;
    }

    public void setSuggestedCategory(String suggestedCategory) {
        this.suggestedCategory = suggestedCategory;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<OcrLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OcrLineItem> lineItems) {
        this.lineItems = lineItems;
    }
}
