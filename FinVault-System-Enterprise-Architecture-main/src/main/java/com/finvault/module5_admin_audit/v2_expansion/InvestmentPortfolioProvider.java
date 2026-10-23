package com.finvault.module5_admin_audit.v2_expansion;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * [Extension Point - V2.0 Future Expansion 5]: InvestmentPortfolioProvider
 * Interface mở rộng cung cấp khả năng kết nối dữ liệu thị trường tài chính và quản lý
 * danh mục tài sản tích sản đầu tư (Stocks, Bonds, ETFs, SJC Gold, Crypto)
 *
 * Lộ trình tích hợp v2.0:
 * 1. Tự động đồng bộ giá khớp lệnh thị trường thời gian thực (SSI/VNDIRECT Open API, CoinGecko, SJC Gold API).
 * 2. Tự động tái cân bằng danh mục đầu tư (Portfolio Rebalancing) theo khẩu vị rủi ro.
 * 3. Tính toán tỷ suất sinh lời theo thời gian (Time-Weighted Return TWR / Money-Weighted Return MWR).
 * 4. Phân tích ma trận tương quan và cảnh báo mức độ tập trung rủi ro tài sản.
 * ==============================================================================
 */
public interface InvestmentPortfolioProvider {

    /**
     * Lấy toàn bộ danh mục tài sản đầu tư của người dùng
     *
     * @param userId ID người dùng
     * @return Danh sách tài sản trong danh mục
     */
    List<PortfolioAssetDto> getUserPortfolio(Long userId);

    /**
     * Cập nhật giá thị trường trực tiếp mới nhất cho một mã tài sản
     *
     * @param assetCode Mã tài sản (Ví dụ: FPT, HPG, BTC, SJC)
     * @return Giá thị trường hiện tại
     */
    BigDecimal fetchLiveMarketPrice(String assetCode);

    /**
     * Tính toán tổng giá trị thị trường và tổng lãi/lỗ của danh mục
     *
     * @param userId ID người dùng
     * @return Tổng giá trị thị trường hiện tại
     */
    BigDecimal calculatePortfolioTotalMarketValue(Long userId);

    /**
     * Đề xuất tỷ lệ tái cân bằng danh mục dựa trên chiến lược đầu tư (Conservative, Moderate, Aggressive)
     *
     * @param userId ID người dùng
     * @param riskProfile Khẩu vị rủi ro
     * @return Danh sách khuyến nghị mua/bán điều chỉnh tỷ trọng
     */
    List<String> generateRebalancingRecommendations(Long userId, String riskProfile);
}
