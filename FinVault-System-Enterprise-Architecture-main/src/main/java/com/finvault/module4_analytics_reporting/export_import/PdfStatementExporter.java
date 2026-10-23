package com.finvault.module4_analytics_reporting.export_import;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * PdfStatementExporter: Xuất báo cáo sao kê giao dịch chuẩn định dạng PDF bằng OpenPDF
 *
 * Tính năng kỹ thuật OpenPDF:
 * - Khổ giấy A4, lề trang tiêu chuẩn ngân hàng (Bank Statement Layout)
 * - Khối tóm tắt chỉ số dòng tiền (Executive Financial Summary Cards)
 * - Bảng danh sách giao dịch với màu nền xen kẽ (Zebra Striping)
 * - Phân biệt trực quan số tiền Thu (Xanh lục) và Chi (Đỏ)
 * ==============================================================================
 */
@Component
public class PdfStatementExporter {

    private static final Logger log = LoggerFactory.getLogger(PdfStatementExporter.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0 VND");

    public byte[] exportToPdf(String username, List<Transaction> transactions) {
        log.info("Bắt đầu kết xuất file PDF sao kê cho người dùng: '{}', tổng {} giao dịch", username, transactions.size());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Font định dạng
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(13, 71, 161));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font headerTableFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font incomeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(46, 125, 50));
            Font expenseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(198, 40, 40));

            // 1. Tiêu đề chính
            Paragraph title = new Paragraph("FINVAULT ENTERPRISE - SAO KE TAI CHINH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("He thong Quan tri Tai chinh Ca nhan & Gia dinh cap Doanh nghiep", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);

            // 2. Thông tin siêu dữ liệu (Metadata)
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            PdfPCell cellUser = new PdfPCell(new Phrase("Chu tai khoan: " + username, boldFont));
            cellUser.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(cellUser);

            PdfPCell cellCount = new PdfPCell(new Phrase("Tong so giao dich: " + transactions.size(), normalFont));
            cellCount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellCount.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(cellCount);

            document.add(metaTable);

            // 3. Khối tóm tắt thu chi (chỉ tính các giao dịch đã APPROVED, không tính PENDING/REJECTED hay TRANSFER nội bộ)
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

            BigDecimal netFlow = totalIn.subtract(totalOut);

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);

            PdfPCell inCell = new PdfPCell();
            inCell.setBackgroundColor(new Color(232, 245, 233));
            inCell.setPadding(10);
            inCell.addElement(new Paragraph("Tong Thu Nhap", boldFont));
            inCell.addElement(new Paragraph("+" + CURRENCY_FORMAT.format(totalIn), incomeFont));
            summaryTable.addCell(inCell);

            PdfPCell outCell = new PdfPCell();
            outCell.setBackgroundColor(new Color(255, 235, 238));
            outCell.setPadding(10);
            outCell.addElement(new Paragraph("Tong Chi Tieu", boldFont));
            outCell.addElement(new Paragraph("-" + CURRENCY_FORMAT.format(totalOut), expenseFont));
            summaryTable.addCell(outCell);

            PdfPCell netCell = new PdfPCell();
            netCell.setBackgroundColor(new Color(227, 242, 253));
            netCell.setPadding(10);
            netCell.addElement(new Paragraph("Dong Tien Rong", boldFont));
            Font netColor = (netFlow.compareTo(BigDecimal.ZERO) >= 0) ? incomeFont : expenseFont;
            netCell.addElement(new Paragraph(CURRENCY_FORMAT.format(netFlow), netColor));
            summaryTable.addCell(netCell);

            document.add(summaryTable);

            // 4. Bảng danh sách giao dịch
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 1.8f, 2.5f, 2.2f, 2.6f, 1.8f});

            String[] tableHeaders = {"STT", "Ngay GD", "Loai", "Danh Muc", "Vi Nguon", "So Tien", "Trang Thai"};
            for (String h : tableHeaders) {
                PdfPCell headerCell = new PdfPCell(new Phrase(h, headerTableFont));
                headerCell.setBackgroundColor(new Color(25, 118, 210));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(6);
                table.addCell(headerCell);
            }

            int index = 1;
            Color altColor = new Color(248, 249, 250);

            for (Transaction tx : transactions) {
                Color rowBg = (index % 2 == 0) ? altColor : Color.WHITE;

                // STT
                PdfPCell c0 = new PdfPCell(new Phrase(String.valueOf(index++), normalFont));
                c0.setBackgroundColor(rowBg);
                c0.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c0);

                // Ngày GD
                PdfPCell c1 = new PdfPCell(new Phrase(tx.getTransactionDate().format(DATE_TIME_FORMATTER), normalFont));
                c1.setBackgroundColor(rowBg);
                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c1);

                // Loại
                PdfPCell c2 = new PdfPCell(new Phrase(tx.getTransactionType().name(), normalFont));
                c2.setBackgroundColor(rowBg);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c2);

                // Danh mục
                String cat = (tx.getCategory() != null) ? tx.getCategory().getCategoryName() : "N/A";
                PdfPCell c3 = new PdfPCell(new Phrase(cat, normalFont));
                c3.setBackgroundColor(rowBg);
                table.addCell(c3);

                // Ví nguồn
                String wallet = (tx.getWallet() != null) ? tx.getWallet().getWalletName() : "N/A";
                PdfPCell c4 = new PdfPCell(new Phrase(wallet, normalFont));
                c4.setBackgroundColor(rowBg);
                table.addCell(c4);

                // Số tiền
                boolean isInflow = tx.getTransactionType() == TransactionType.INCOME
                        || (tx.getTransactionType() == TransactionType.TRANSFER && isIncomingTransfer(tx));
                Font fontForAmount = isInflow ? incomeFont : expenseFont;
                String prefix = isInflow ? "+" : "-";
                PdfPCell c5 = new PdfPCell(new Phrase(prefix + CURRENCY_FORMAT.format(tx.getAmount()), fontForAmount));
                c5.setBackgroundColor(rowBg);
                c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c5);

                // Trạng thái
                String status = (tx.getApprovalStatus() != null) ? tx.getApprovalStatus().name() : "N/A";
                PdfPCell c6 = new PdfPCell(new Phrase(status, normalFont));
                c6.setBackgroundColor(rowBg);
                c6.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c6);
            }

            document.add(table);

            // 5. Chân trang
            Paragraph footer = new Paragraph("\n* Bao cao sao ke dien tu duoc phat hanh boi FinVault Enterprise Core Ledger.", subTitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            log.info("Đã tạo thành công file PDF báo cáo sao kê (Kích thước: {} bytes)", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi kết xuất file PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tạo báo cáo PDF: " + e.getMessage(), e);
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
