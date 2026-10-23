package com.finvault.module2_wallet_ledger.v2_expansion;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * [Extension Point - V2.0 Future Expansion 2]: OpenBankingSyncProvider
 * Interface mở rộng cung cấp khả năng kết nối đồng bộ trực tiếp với Open Banking API
 * (VietQR, Napas 247, Vietcombank / Techcombank Corporate Open API).
 *
 * Lộ trình tích hợp v2.0:
 * 1. Tự động truy vấn số dư thực tế từ ngân hàng (Fetch Real-time Bank Balance)
 * 2. Tiếp nhận Webhook thông báo biến động số dư tức thì (Instant Balance Fluctuation Webhook)
 * 3. Tự động sinh giao dịch vào Sổ cái FinVault (Auto-import Ledger Transaction)
 * 4. Đối soát chênh lệch tự động giữa số dư FinVault và Core Banking của ngân hàng
 * ==============================================================================
 */
public interface OpenBankingSyncProvider {

    /**
     * Đồng bộ số dư mới nhất từ cổng ngân hàng về ví FinVault
     */
    BigDecimal fetchLiveBankBalance(String bankName, String bankAccountNumber);

    /**
     * Xử lý dữ liệu Webhook biến động số dư gửi từ cổng thanh toán / ngân hàng đối tác
     */
    boolean processBankWebhookEvent(BankTransactionPayloadDto payload);

    /**
     * Lấy lịch sử giao dịch gần nhất từ Open Banking API để đối soát định kỳ
     */
    List<BankTransactionPayloadDto> fetchRecentBankTransactions(String bankAccountNumber, int limit);

    /**
     * Xác thực chữ ký điện tử HMAC-SHA256 trên Webhook payload của ngân hàng
     */
    boolean verifyWebhookSignature(String rawPayload, String signature, String secretKey);
}
