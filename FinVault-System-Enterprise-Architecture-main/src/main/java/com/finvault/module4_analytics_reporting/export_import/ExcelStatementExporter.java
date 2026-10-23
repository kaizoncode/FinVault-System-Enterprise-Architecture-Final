package com.finvault.module4_analytics_reporting.export_import;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ExcelStatementExporter: Xuất báo cáo sao kê giao dịch chuẩn định dạng Microsoft Excel (.xlsx)
 *
 * Tính năng kỹ thuật Apache POI:
 * - Áp dụng Color Palette chuẩn doanh nghiệp (Navy Blue, Light Grey, Green Income, Red Expense)
 * - Tự động định dạng ô tiền tệ chuẩn (Currency NumberFormat: #,##0 "VND")
 * - Tự động co giãn độ rộng cột (Auto-size column width)
 * - Khối tóm tắt tài chính (Executive Summary Cards) ở đầu trang tính
 * ==============================================================================
 */
@Component
public class ExcelStatementExporter {

    private static final Logger log = LoggerFactory.getLogger(ExcelStatementExporter.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportToExcel(String username, List<Transaction> transactions) {
        log.info("Bắt đầu kết xuất file Excel sao kê cho người dùng: '{}', tổng {} giao dịch", username, transactions.size());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sao kê giao dịch");
            sheet.setDisplayGridlines(true);

            // 1. Tạo Fonts & Styles
            DataFormat dataFormat = workbook.createDataFormat();

            // Font tiêu đề chính
            XSSFFont titleFont = (XSSFFont) workbook.createFont();
            titleFont.setFontName("Segoe UI");
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Font tiêu đề bảng
            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setFontName("Segoe UI");
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Style dữ liệu thông thường
            XSSFFont dataFont = (XSSFFont) workbook.createFont();
            dataFont.setFontName("Segoe UI");
            dataFont.setFontHeightInPoints((short) 10);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(dataFont);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style ô tiền tệ Thu nhập (Xanh lục)
            CellStyle incomeStyle = workbook.createCellStyle();
            incomeStyle.cloneStyleFrom(dataStyle);
            incomeStyle.setDataFormat(dataFormat.getFormat("+#,##0 \"VND\""));
            XSSFFont incomeFont = (XSSFFont) workbook.createFont();
            incomeFont.setFontName("Segoe UI");
            incomeFont.setBold(true);
            incomeFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            incomeStyle.setFont(incomeFont);
            incomeStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Style ô tiền tệ Chi tiêu (Đỏ)
            CellStyle expenseStyle = workbook.createCellStyle();
            expenseStyle.cloneStyleFrom(dataStyle);
            expenseStyle.setDataFormat(dataFormat.getFormat("-#,##0 \"VND\""));
            XSSFFont expenseFont = (XSSFFont) workbook.createFont();
            expenseFont.setFontName("Segoe UI");
            expenseFont.setBold(true);
            expenseFont.setColor(IndexedColors.RED.getIndex());
            expenseStyle.setFont(expenseFont);
            expenseStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Style ngày tháng
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            dateStyle.setAlignment(HorizontalAlignment.CENTER);

            // 2. Viết Header Tiêu đề
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("BÁO CÁO SAO KÊ TÀI CHÍNH - FINVAULT ENTERPRISE");
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(2);
            metaRow.createCell(1).setCellValue("Tài khoản: " + username + "  |  Tổng số bản ghi: " + transactions.size());

            // 3. Tính toán tổng quan thu/chi (chỉ tính các giao dịch đã APPROVED, không tính PENDING/REJECTED hay TRANSFER nội bộ)
            BigDecimal totalIn = transactions.stream()
                    .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                    .filter(t -> t.getApprovalStatus() == null || t.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOut = transactions.stream()
                    .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                    .filter(t -> t.getApprovalStatus() == null || t.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Row summaryRow = sheet.createRow(4);
            summaryRow.createCell(1).setCellValue("Tổng Thu Nhập: " + totalIn.toPlainString() + " VND  |  Tổng Chi Tiêu: " + totalOut.toPlainString() + " VND  |  Dòng Tiền Ròng: " + totalIn.subtract(totalOut).toPlainString() + " VND");

            // 4. Header Bảng giao dịch
            String[] headers = {
                    "STT", "Mã Giao Dịch", "Thời Gian", "Loại", "Danh Mục", "Ví Nguồn", "Số Tiền", "Trạng Thái", "Ghi Chú"
            };

            int startRow = 6;
            Row headerRow = sheet.createRow(startRow);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 5. Ghi dữ liệu từng dòng giao dịch
            int currentRow = startRow + 1;
            int index = 1;

            for (Transaction tx : transactions) {
                Row row = sheet.createRow(currentRow++);
                row.setHeightInPoints(20);

                // STT
                Cell c0 = row.createCell(1);
                c0.setCellValue(index++);
                c0.setCellStyle(dateStyle);

                // Mã giao dịch
                Cell c1 = row.createCell(2);
                c1.setCellValue("TX-" + tx.getId());
                c1.setCellStyle(dataStyle);

                // Thời gian
                Cell c2 = row.createCell(3);
                c2.setCellValue(tx.getTransactionDate().format(DATE_TIME_FORMATTER));
                c2.setCellStyle(dateStyle);

                // Loại giao dịch
                Cell c3 = row.createCell(4);
                c3.setCellValue(tx.getTransactionType().name());
                c3.setCellStyle(dateStyle);

                // Danh mục
                Cell c4 = row.createCell(5);
                c4.setCellValue((tx.getCategory() != null) ? tx.getCategory().getCategoryName() : "N/A");
                c4.setCellStyle(dataStyle);

                // Ví nguồn
                Cell c5 = row.createCell(6);
                c5.setCellValue((tx.getWallet() != null) ? tx.getWallet().getWalletName() : "N/A");
                c5.setCellStyle(dataStyle);

                // Số tiền
                Cell c6 = row.createCell(7);
                c6.setCellValue(tx.getAmount().doubleValue());
                boolean isInflow = tx.getTransactionType() == TransactionType.INCOME
                        || (tx.getTransactionType() == TransactionType.TRANSFER && isIncomingTransfer(tx));
                if (isInflow) {
                    c6.setCellStyle(incomeStyle);
                } else {
                    c6.setCellStyle(expenseStyle);
                }

                // Trạng thái phê duyệt
                Cell c7 = row.createCell(8);
                c7.setCellValue((tx.getApprovalStatus() != null) ? tx.getApprovalStatus().name() : "N/A");
                c7.setCellStyle(dateStyle);

                // Ghi chú
                Cell c8 = row.createCell(9);
                c8.setCellValue((tx.getDescription() != null) ? tx.getDescription() : "");
                c8.setCellStyle(dataStyle);
            }

            // 6. Tự động co giãn cột cho đẹp mắt
            for (int i = 1; i <= headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Giới hạn độ rộng tối đa tránh vỡ trang
                if (sheet.getColumnWidth(i) > 12000) {
                    sheet.setColumnWidth(i, 12000);
                }
            }

            workbook.write(out);
            log.info("Đã tạo thành công file Excel báo cáo sao kê (Kích thước: {} bytes)", out.size());
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi I/O khi kết xuất báo cáo Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi trong quá trình tạo file Excel: " + e.getMessage(), e);
        }
    }

    private boolean isIncomingTransfer(Transaction tx) {
        if (tx.getTransferDirection() != null) {
            return tx.getTransferDirection() == TransferDirection.INCOMING;
        }
        if (tx.getNote() != null) {
            String lower = tx.getNote().trim().toLowerCase();
            return lower.startsWith("nhận từ") || lower.startsWith("[saving_withdraw") || lower.startsWith("hoàn tiền");
        }
        return false;
    }
}
