package com.finvault.module5_admin_audit.v2_expansion;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * [Extension Point - V2.0 Future Expansion 5]: PortfolioAssetDto
 * DTO lưu trữ thông tin tài sản đầu tư tài chính tích sản (Cổ phiếu, Trái phiếu, ETF, Vàng, Crypto)
 * ==============================================================================
 */
public class PortfolioAssetDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String assetCode;          // Mã tài sản: VNM, FPT, SJC_GOLD, BTC, ETH
    private String assetName;          // Tên tài sản: Cổ phiếu Vinamilk, Vàng miếng SJC
    private String assetClass;         // STOCK, BOND, ETF, GOLD, CRYPTO, REAL_ESTATE
    private BigDecimal quantity;       // Khối lượng sở hữu
    private BigDecimal averageBuyPrice;// Giá vốn bình quân
    private BigDecimal currentMarketPrice; // Giá thị trường hiện tại
    private BigDecimal totalCost;      // Tổng giá vốn đầu tư
    private BigDecimal marketValue;    // Tổng giá trị thị trường
    private BigDecimal unrealizedProfitLoss; // Lãi/Lỗ chưa thực hiện (PnL)
    private BigDecimal returnOnInvestment;   // Tỷ suất sinh lời (% ROI)
    private LocalDateTime lastPriceUpdated;

    public PortfolioAssetDto() {
    }

    public PortfolioAssetDto(String assetCode, String assetName, String assetClass, BigDecimal quantity,
                             BigDecimal averageBuyPrice, BigDecimal currentMarketPrice,
                             BigDecimal totalCost, BigDecimal marketValue,
                             BigDecimal unrealizedProfitLoss, BigDecimal returnOnInvestment,
                             LocalDateTime lastPriceUpdated) {
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.assetClass = assetClass;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentMarketPrice = currentMarketPrice;
        this.totalCost = totalCost;
        this.marketValue = marketValue;
        this.unrealizedProfitLoss = unrealizedProfitLoss;
        this.returnOnInvestment = returnOnInvestment;
        this.lastPriceUpdated = lastPriceUpdated;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(BigDecimal averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }

    public BigDecimal getCurrentMarketPrice() {
        return currentMarketPrice;
    }

    public void setCurrentMarketPrice(BigDecimal currentMarketPrice) {
        this.currentMarketPrice = currentMarketPrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getUnrealizedProfitLoss() {
        return unrealizedProfitLoss;
    }

    public void setUnrealizedProfitLoss(BigDecimal unrealizedProfitLoss) {
        this.unrealizedProfitLoss = unrealizedProfitLoss;
    }

    public BigDecimal getReturnOnInvestment() {
        return returnOnInvestment;
    }

    public void setReturnOnInvestment(BigDecimal returnOnInvestment) {
        this.returnOnInvestment = returnOnInvestment;
    }

    public LocalDateTime getLastPriceUpdated() {
        return lastPriceUpdated;
    }

    public void setLastPriceUpdated(LocalDateTime lastPriceUpdated) {
        this.lastPriceUpdated = lastPriceUpdated;
    }
}
