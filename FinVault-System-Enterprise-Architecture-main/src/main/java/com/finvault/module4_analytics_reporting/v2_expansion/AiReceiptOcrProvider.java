package com.finvault.module4_analytics_reporting.v2_expansion;

import java.io.InputStream;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * [Extension Point - V2.0 Future Expansion 4]: AiReceiptOcrProvider
 * Interface mở rộng cung cấp khả năng tích hợp mô hình AI Computer Vision / OCR
 * (Google Cloud Vision, AWS Textract, hoặc mô hình Deep Learning On-Premises)
 * để tự động quét hóa đơn và tạo giao dịch.
 *
 * Lộ trình tích hợp v2.0:
 * 1. Nhận diện hình ảnh hóa đơn chụp từ điện thoại (JPG/PNG) hoặc file PDF hóa đơn điện tử.
 * 2. Tự động bóc tách số tiền, tên cửa hàng, ngày giờ và danh mục chi tiêu đề xuất.
 * 3. Tự động kiểm tra tính hợp lệ của mã số thuế (MST) và hóa đơn điện tử Tổng cục Thuế.
 * 4. Tự động điền dữ liệu vào form tạo giao dịch của FinVault Core Ledger giúp tiết kiệm thời gian.
 * ==============================================================================
 */
public interface AiReceiptOcrProvider {

    /**
     * Quét và trích xuất dữ liệu hóa đơn từ file ảnh hoặc PDF
     *
     * @param imageStream Luồng dữ liệu ảnh/PDF hóa đơn
     * @param fileName Tên file gốc (để xác định định dạng mime)
     * @return DTO chứa thông tin trích xuất có cấu trúc
     */
    OcrReceiptResultDto scanReceipt(InputStream imageStream, String fileName);

    /**
     * Đánh giá độ tin cậy của hóa đơn và phát hiện hóa đơn có dấu hiệu làm giả / chỉnh sửa
     *
     * @param ocrResult Kết quả bóc tách từ hóa đơn
     * @return true nếu hóa đơn đạt chuẩn tin cậy cao
     */
    boolean validateInvoiceAuthenticity(OcrReceiptResultDto ocrResult);
}
