package com.finvault.module4_analytics_reporting.export_import;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.repository.CategoryRepository;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.repository.WalletMemberRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module4_analytics_reporting.dto.ImportReportResultDto;
import com.finvault.module5_admin_audit.service.SystemConfigService;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ExcelTransactionImporter: Bộ đọc và nhập hàng loạt dữ liệu giao dịch từ Excel thông minh & linh hoạt (Smart Flexible Importer)
 *
 * Tính năng nâng cao:
 * 1. Tự động nhận diện dòng tiêu đề (Header Auto-Detection): Tự động quét 15 dòng đầu để tìm header.
 * 2. Ánh xạ cột động (Dynamic Column Mapping): Tương thích với mọi định dạng Excel (kể cả file xuất của FinVault, sao kê ngân hàng VCB/TCB/BIDV hoặc file nhập chuẩn).
 * 3. Hỗ trợ đa dạng định dạng ngày, phân loại thu/chi thông minh theo dấu số tiền hoặc cột loại.
 * 4. Tự động đối soát ví theo ID hoặc tên ví (ví dụ: "Ví Tiền Mặt", "Quỹ Gia Đình").
 * 5. Tự động đối chiếu danh mục theo ID hoặc tên danh mục tiếng Việt/Anh.
 * 6. Kiểm tra trùng lặp giao dịch (Duplicate Detection) trong khung thời gian 5 phút.
 * ==============================================================================
 */
@Component
public class ExcelTransactionImporter {

    private static final Logger log = LoggerFactory.getLogger(ExcelTransactionImporter.class);

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    };

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExcelTransactionRowService rowService;
    private final SystemConfigService systemConfigService;

    @org.springframework.beans.factory.annotation.Value("${finvault.family.approval-threshold-vnd:5000000}")
    private BigDecimal approvalThreshold;

    public ExcelTransactionImporter(TransactionRepository transactionRepository,
                                    WalletRepository walletRepository,
                                    WalletMemberRepository walletMemberRepository,
                                    CategoryRepository categoryRepository,
                                    UserRepository userRepository,
                                    ExcelTransactionRowService rowService) {
        this(transactionRepository, walletRepository, walletMemberRepository, categoryRepository, userRepository, rowService, null);
    }

    @Autowired
    public ExcelTransactionImporter(TransactionRepository transactionRepository,
                                    WalletRepository walletRepository,
                                    WalletMemberRepository walletMemberRepository,
                                    CategoryRepository categoryRepository,
                                    UserRepository userRepository,
                                    ExcelTransactionRowService rowService,
                                    SystemConfigService systemConfigService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.walletMemberRepository = walletMemberRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.rowService = rowService;
        this.systemConfigService = systemConfigService;
    }

    private BigDecimal getEffectiveApprovalThreshold() {
        BigDecimal fallback = (approvalThreshold != null) ? approvalThreshold : new BigDecimal("5000000");
        if (systemConfigService != null) {
            try {
                String raw = systemConfigService.getConfigValue("FAMILY_EXPENSE_APPROVAL_THRESHOLD", fallback.toPlainString());
                if (raw != null && !raw.isBlank()) {
                    return new BigDecimal(raw.trim());
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    public ImportReportResultDto importTransactionsFromExcel(String username, Long defaultWalletId, InputStream inputStream) {
        log.info("Bắt đầu xử lý nhập hàng loạt giao dịch từ Excel cho user: '{}'", username);
        ImportReportResultDto result = new ImportReportResultDto();

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));

        if (defaultWalletId != null) {
            walletRepository.findByIdAndIsDeletedFalse(defaultWalletId)
                    .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví mặc định ID: " + defaultWalletId));
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                log.warn("File Excel trống hoặc không có sheet nào.");
                return result;
            }

            int headerRowIndex = -1;
            Map<String, Integer> colMap = new HashMap<>();

            // 1. Quét tìm dòng Header (từ dòng 0 đến tối đa 15 dòng đầu)
            int maxScanRows = Math.min(15, sheet.getLastRowNum() + 1);
            for (int r = 0; r < maxScanRows; r++) {
                Row row = sheet.getRow(r);
                if (row != null && isHeaderRow(row)) {
                    headerRowIndex = r;
                    colMap = buildColumnMap(row);
                    log.info("Đã tự động nhận diện dòng tiêu đề Excel tại dòng index {}: {}", r, colMap);
                    break;
                }
            }

            // Fallback nếu không phát hiện được header sau 15 dòng
            if (headerRowIndex == -1 || colMap.size() < 2) {
                log.warn("Không tìm thấy dòng Header tiêu chuẩn, áp dụng fallback cấu hình mặc định (0=Date, 1=Type, 2=Amount, 3=Category, 4=Note, 5=Wallet)");
                headerRowIndex = 0;
                colMap.put("DATE_COL", 0);
                colMap.put("TYPE_COL", 1);
                colMap.put("AMOUNT_COL", 2);
                colMap.put("CATEGORY_COL", 3);
                colMap.put("NOTE_COL", 4);
                colMap.put("WALLET_COL", 5);
            }

            int startRow = headerRowIndex + 1;
            int totalSheetRows = sheet.getLastRowNum();

            for (int r = startRow; r <= totalSheetRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isSummaryOrEmptyRow(row, colMap)) {
                    continue;
                }

                int displayRowNumber = r + 1; // 1-indexed cho hiển thị người dùng

                // Nếu dòng có trạng thái REJECTED / TỪ CHỐI thì bỏ qua không nhập
                if (colMap.containsKey("STATUS_COL")) {
                    String statusStr = normalizeText(getCellStringValue(row.getCell(colMap.get("STATUS_COL")))).toUpperCase();
                    if (statusStr.contains("REJECTED") || statusStr.contains("TU CHOI")) {
                        log.debug("Dòng {}: Bỏ qua giao dịch có trạng thái REJECTED", displayRowNumber);
                        continue;
                    }
                }

                result.setTotalRowsProcessed(result.getTotalRowsProcessed() + 1);

                try {
                    // 1. Ngày giao dịch
                    LocalDateTime txDate = LocalDateTime.now();
                    if (colMap.containsKey("DATE_COL")) {
                        txDate = parseRowDateTime(row.getCell(colMap.get("DATE_COL")));
                    }

                    // 2. Số tiền và dấu
                    BigDecimal amount = BigDecimal.ZERO;
                    boolean isAmountNegative = false;
                    if (colMap.containsKey("AMOUNT_COL")) {
                        ParsedAmount parsed = parseAmount(row.getCell(colMap.get("AMOUNT_COL")));
                        amount = parsed.amount;
                        isAmountNegative = parsed.isNegative;
                    }

                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        result.setFailureCount(result.getFailureCount() + 1);
                        result.addErrorMessage("Dòng " + displayRowNumber + ": Số tiền không hợp lệ hoặc phải lớn hơn 0");
                        continue;
                    }

                    // 3. Phân loại giao dịch (INCOME / EXPENSE / TRANSFER)
                    TransactionType txType;
                    if (colMap.containsKey("TYPE_COL")) {
                        String typeStr = getCellStringValue(row.getCell(colMap.get("TYPE_COL")));
                        txType = parseTransactionType(typeStr, isAmountNegative);
                    } else {
                        // Tự động phân loại theo dấu số tiền
                        txType = isAmountNegative ? TransactionType.EXPENSE : TransactionType.INCOME;
                    }

                    // 4. Danh mục (kiểm tra tính tương thích với loại giao dịch)
                    Category category = null;
                    if (colMap.containsKey("CATEGORY_COL")) {
                        String categoryStr = getCellStringValue(row.getCell(colMap.get("CATEGORY_COL")));
                        category = resolveCategory(user.getId(), categoryStr);
                        if (category != null) {
                            if (txType == TransactionType.TRANSFER
                                    || (category.getCategoryType() != null && !category.getCategoryType().name().equals(txType.name()))) {
                                category = null;
                            }
                        }
                    }

                    // 5. Ghi chú / Mô tả
                    String description = "";
                    if (colMap.containsKey("NOTE_COL")) {
                        description = getCellStringValue(row.getCell(colMap.get("NOTE_COL")));
                    }

                    // 6. Ví tài chính
                    String walletStr = null;
                    if (colMap.containsKey("WALLET_COL")) {
                        walletStr = getCellStringValue(row.getCell(colMap.get("WALLET_COL")));
                    }
                    Long targetWalletId = resolveWalletId(user, walletStr, defaultWalletId);

                    if (targetWalletId == null) {
                        result.setFailureCount(result.getFailureCount() + 1);
                        result.addErrorMessage("Dòng " + displayRowNumber + ": Không xác định được ví tài chính nhận giao dịch");
                        continue;
                    }

                    // 7. Kiểm tra trùng lặp (Duplicate Detection)
                    boolean isDuplicate = checkDuplicateTransaction(user.getId(), targetWalletId, amount, txDate, description);
                    if (isDuplicate) {
                        result.setSkippedDuplicatesCount(result.getSkippedDuplicatesCount() + 1);
                        log.debug("Dòng {}: Bỏ qua giao dịch trùng lặp [{}] số tiền {}", displayRowNumber, description, amount);
                        continue;
                    }

                    // 8. Ủy quyền xử lý từng dòng cho ExcelTransactionRowService với @Transactional(REQUIRES_NEW)
                    rowService.importSingleRow(
                            user,
                            targetWalletId,
                            category,
                            txType,
                            amount,
                            txDate,
                            description,
                            getEffectiveApprovalThreshold()
                    );
                    result.setSuccessCount(result.getSuccessCount() + 1);

                } catch (Exception e) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.addErrorMessage("Dòng " + displayRowNumber + ": " + e.getMessage());
                    log.warn("Lỗi khi xử lý dòng {} trong file Excel: {}", displayRowNumber, e.getMessage());
                }
            }

            log.info("Hoàn tất nhập dữ liệu Excel. Tổng: {}, Thành công: {}, Trùng bỏ qua: {}, Lỗi: {}",
                    result.getTotalRowsProcessed(), result.getSuccessCount(), result.getSkippedDuplicatesCount(), result.getFailureCount());
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel nhập giao dịch: {}", e.getMessage(), e);
            throw new BusinessException("EXCEL_IMPORT_FAILED", "Không thể đọc định dạng file Excel: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra một dòng có phải là Header bảng hay không (chứa ít nhất 2 từ khóa nhận diện)
     */
    private boolean isHeaderRow(Row row) {
        if (row == null) return false;
        Set<String> matchedCategories = new HashSet<>();
        for (Cell cell : row) {
            String text = normalizeText(getCellStringValue(cell));
            if (text.isBlank()) continue;

            if (containsAny(text, "ngay", "date", "thoi gian", "time")) {
                matchedCategories.add("DATE");
            }
            if (containsAny(text, "so tien", "amount", "tien", "gia tri", "phat sinh")) {
                matchedCategories.add("AMOUNT");
            }
            if (containsAny(text, "loai", "type", "thu/chi", "thu chi", "phan loai")) {
                matchedCategories.add("TYPE");
            }
            if (containsAny(text, "danh muc", "category", "nhom")) {
                matchedCategories.add("CATEGORY");
            }
            if (containsAny(text, "vi", "wallet", "tai khoan", "nguon")) {
                matchedCategories.add("WALLET");
            }
            if (containsAny(text, "ghi chu", "note", "noi dung", "mo ta", "description")) {
                matchedCategories.add("NOTE");
            }
        }
        return matchedCategories.size() >= 2;
    }

    /**
     * Xây dựng bản đồ ánh xạ cột tự động từ dòng Header
     */
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> colMap = new HashMap<>();
        for (Cell cell : headerRow) {
            int colIdx = cell.getColumnIndex();
            String text = normalizeText(getCellStringValue(cell));
            if (text.isBlank()) continue;

            if (!colMap.containsKey("DATE_COL") && containsAny(text, "ngay", "date", "thoi gian", "time")) {
                colMap.put("DATE_COL", colIdx);
            } else if (!colMap.containsKey("AMOUNT_COL") && containsAny(text, "so tien", "amount", "tien", "gia tri", "phat sinh")) {
                colMap.put("AMOUNT_COL", colIdx);
            } else if (!colMap.containsKey("TYPE_COL") && containsAny(text, "loai", "type", "thu/chi", "thu chi", "phan loai")) {
                colMap.put("TYPE_COL", colIdx);
            } else if (!colMap.containsKey("CATEGORY_COL") && containsAny(text, "danh muc", "category", "nhom")) {
                colMap.put("CATEGORY_COL", colIdx);
            } else if (!colMap.containsKey("WALLET_COL") && containsAny(text, "vi", "wallet", "tai khoan", "nguon")) {
                colMap.put("WALLET_COL", colIdx);
            } else if (!colMap.containsKey("NOTE_COL") && containsAny(text, "ghi chu", "note", "noi dung", "mo ta", "description")) {
                colMap.put("NOTE_COL", colIdx);
            } else if (!colMap.containsKey("STATUS_COL") && containsAny(text, "trang thai", "status")) {
                colMap.put("STATUS_COL", colIdx);
            }
        }
        return colMap;
    }

    /**
     * Bỏ qua dòng trống hoặc dòng tóm tắt/tổng cộng
     */
    private boolean isSummaryOrEmptyRow(Row row, Map<String, Integer> colMap) {
        if (row == null) return true;
        boolean allBlank = true;
        for (Cell cell : row) {
            String val = getCellStringValue(cell);
            if (!val.isBlank()) {
                allBlank = false;
                String normalized = normalizeText(val);
                if (normalized.startsWith("tong cong") || normalized.startsWith("tong thu") ||
                        normalized.startsWith("tong chi") || normalized.startsWith("total") ||
                        normalized.startsWith("dong tien rong")) {
                    return true;
                }
            }
        }
        if (allBlank) return true;

        // Nếu cột Amount không có dữ liệu hợp lệ thì coi là dòng phi giao dịch
        Integer amountCol = colMap.get("AMOUNT_COL");
        if (amountCol != null) {
            Cell amountCell = row.getCell(amountCol);
            String amountStr = getCellStringValue(amountCell);
            if (amountStr.isBlank() && (amountCell == null || amountCell.getCellType() != CellType.NUMERIC)) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime parseRowDateTime(Cell cell) {
        if (cell == null) {
            return LocalDateTime.now();
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }
        String str = getCellStringValue(cell);
        if (str == null || str.isBlank()) {
            return LocalDateTime.now();
        }

        String cleanStr = str.trim();
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                if (cleanStr.length() <= 10) {
                    return java.time.LocalDate.parse(cleanStr, dtf).atStartOfDay();
                }
                return LocalDateTime.parse(cleanStr, dtf);
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private static class ParsedAmount {
        BigDecimal amount = BigDecimal.ZERO;
        boolean isNegative = false;
    }

    private ParsedAmount parseAmount(Cell cell) {
        ParsedAmount result = new ParsedAmount();
        if (cell == null) {
            return result;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val < 0) {
                result.isNegative = true;
                result.amount = BigDecimal.valueOf(Math.abs(val));
            } else {
                result.amount = BigDecimal.valueOf(val);
            }
            return result;
        }

        String str = getCellStringValue(cell);
        if (str == null || str.isBlank()) {
            return result;
        }

        String trimmed = str.trim();
        if (trimmed.startsWith("-") || (trimmed.startsWith("(") && trimmed.endsWith(")"))) {
            result.isNegative = true;
        }

        String clean = cleanNumberString(trimmed);
        try {
            BigDecimal val = new BigDecimal(clean);
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                result.isNegative = true;
                result.amount = val.abs();
            } else {
                result.amount = val;
            }
        } catch (Exception e) {
            result.amount = BigDecimal.ZERO;
        }
        return result;
    }

    private String cleanNumberString(String str) {
        if (str == null) return "0";
        String s = str.replaceAll("[^0-9,.-]", "");
        if (s.isBlank()) return "0";

        if (s.contains(",") && s.contains(".")) {
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            int commaCount = s.length() - s.replace(",", "").length();
            if (commaCount > 1 || (s.indexOf(',') == s.length() - 4 && commaCount == 1 && !s.contains("."))) {
                s = s.replace(",", "");
            } else {
                s = s.replace(',', '.');
            }
        } else if (s.contains(".")) {
            int dotCount = s.length() - s.replace(".", "").length();
            if (dotCount > 1) {
                s = s.replace(".", "");
            } else if (s.indexOf('.') == s.length() - 4 && !s.startsWith("0.")) {
                s = s.replace(".", "");
            }
        }
        return s.isBlank() ? "0" : s;
    }

    private TransactionType parseTransactionType(String typeStr, boolean isAmountNegative) {
        if (typeStr == null || typeStr.isBlank()) {
            return isAmountNegative ? TransactionType.EXPENSE : TransactionType.INCOME;
        }
        String clean = normalizeText(typeStr).toUpperCase();
        if (clean.contains("TRANSFER") || clean.contains("CHUYEN KHOAN") || clean.contains("TIET KIEM") || clean.contains("SAVING")) {
            return TransactionType.TRANSFER;
        }
        if (clean.contains("THU") || clean.contains("INCOME") || clean.contains("CREDIT") || clean.contains("VAO")) {
            return TransactionType.INCOME;
        }
        if (clean.contains("CHI") || clean.contains("EXPENSE") || clean.contains("DEBIT") || clean.contains("RA")) {
            return TransactionType.EXPENSE;
        }
        return isAmountNegative ? TransactionType.EXPENSE : TransactionType.INCOME;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (type == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val) && !Double.isInfinite(val)) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return "";
    }

    private Category resolveCategory(Long userId, String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank() || "N/A".equalsIgnoreCase(categoryStr.trim())) {
            return null;
        }
        // Thử tìm theo ID số
        try {
            Long catId = Long.parseLong(categoryStr.trim());
            Optional<Category> opt = categoryRepository.findByIdAndIsDeletedFalse(catId);
            if (opt.isPresent()) return opt.get();
        } catch (NumberFormatException ignored) {
        }

        // Tìm theo tên danh mục (cả exact match và normalized match)
        List<Category> userCats = categoryRepository.findAvailableCategoriesForUser(userId);
        String targetNormalized = normalizeText(categoryStr);
        for (Category c : userCats) {
            if (c.getCategoryName().equalsIgnoreCase(categoryStr.trim()) ||
                    normalizeText(c.getCategoryName()).equals(targetNormalized)) {
                return c;
            }
        }
        return null;
    }

    private Long resolveWalletId(User user, String walletStr, Long defaultWalletId) {
        if (walletStr != null && !walletStr.isBlank() && !"N/A".equalsIgnoreCase(walletStr.trim())) {
            String clean = walletStr.trim();
            // 1. Thử parse ID số
            try {
                Long parsedId = Long.parseLong(clean);
                if (walletRepository.findByIdAndIsDeletedFalse(parsedId).isPresent()) {
                    return parsedId;
                }
            } catch (NumberFormatException ignored) {
            }

            // 2. Thử đối chiếu tên ví (không phân biệt hoa thường và dấu)
            List<Wallet> accessibleWallets = walletRepository.findAllAccessibleWalletsByUser(user.getId());
            String normalizedTarget = normalizeText(clean);
            for (Wallet w : accessibleWallets) {
                if (w.getWalletName().equalsIgnoreCase(clean) || normalizeText(w.getWalletName()).equals(normalizedTarget)) {
                    return w.getId();
                }
            }
        }

        // 3. Fallback về defaultWalletId nếu có
        if (defaultWalletId != null) {
            return defaultWalletId;
        }

        // 4. Fallback về ví đầu tiên của người dùng
        List<Wallet> userWallets = walletRepository.findAllByUserIdAndIsDeletedFalse(user.getId());
        if (!userWallets.isEmpty()) {
            return userWallets.get(0).getId();
        }

        return null;
    }

    private boolean checkDuplicateTransaction(Long userId, Long walletId, BigDecimal amount, LocalDateTime date, String description) {
        LocalDateTime windowStart = date.minusMinutes(5);
        LocalDateTime windowEnd = date.plusMinutes(5);

        List<Transaction> existing = transactionRepository.findTransactionsByWalletAndDateRange(walletId, windowStart, windowEnd);
        return existing.stream().anyMatch(tx ->
                tx.getUser().getId().equals(userId) &&
                        tx.getAmount().compareTo(amount) == 0 &&
                        ((description == null && tx.getDescription() == null) ||
                                (description != null && description.equalsIgnoreCase(tx.getDescription())))
        );
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.replace('đ', 'd').replace('Đ', 'D').toLowerCase().trim();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
