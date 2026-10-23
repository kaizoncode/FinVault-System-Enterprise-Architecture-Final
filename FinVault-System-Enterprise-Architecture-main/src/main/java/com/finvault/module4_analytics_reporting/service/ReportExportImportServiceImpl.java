package com.finvault.module4_analytics_reporting.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module4_analytics_reporting.dto.ImportReportResultDto;
import com.finvault.module4_analytics_reporting.dto.TransactionExportFilterRequest;
import com.finvault.module4_analytics_reporting.export_import.ExcelStatementExporter;
import com.finvault.module4_analytics_reporting.export_import.ExcelTransactionImporter;
import com.finvault.module4_analytics_reporting.export_import.PdfStatementExporter;
import com.finvault.module4_analytics_reporting.repository.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ReportExportImportServiceImpl: Triển khai toàn diện xuất báo cáo sao kê và nhập dữ liệu
 *
 * Tính năng chính:
 * - Xuất báo cáo giao dịch ra file Excel (.xlsx) bằng Apache POI
 * - Xuất báo cáo sao kê điện tử (.pdf) bằng OpenPDF
 * - Nhập hàng loạt giao dịch từ file Excel có kiểm soát trùng lặp và tính toàn vẹn số dư ví
 * ==============================================================================
 */
@Service
public class ReportExportImportServiceImpl implements ReportExportImportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportImportServiceImpl.class);

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final ExcelStatementExporter excelExporter;
    private final PdfStatementExporter pdfExporter;
    private final ExcelTransactionImporter excelImporter;

    public ReportExportImportServiceImpl(AnalyticsRepository analyticsRepository,
                                         UserRepository userRepository,
                                         ExcelStatementExporter excelExporter,
                                         PdfStatementExporter pdfExporter,
                                         ExcelTransactionImporter excelImporter) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
        this.excelImporter = excelImporter;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsToExcel(String username, TransactionExportFilterRequest filter) {
        log.info("Yêu cầu xuất Excel sao kê cho cá nhân user: '{}'", username);
        User user = getUserByUsername(username);
        Long targetUserId = (user.hasRole(Role.ROLE_SUPER_ADMIN) || user.hasRole(Role.ROLE_SYSTEM_AUDITOR)) ? null : user.getId();
        List<Transaction> transactions = analyticsRepository.findTransactionsForExport(targetUserId, filter);
        return excelExporter.exportToExcel(username, transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsToPdf(String username, TransactionExportFilterRequest filter) {
        log.info("Yêu cầu xuất PDF sao kê cho cá nhân user: '{}'", username);
        User user = getUserByUsername(username);
        Long targetUserId = (user.hasRole(Role.ROLE_SUPER_ADMIN) || user.hasRole(Role.ROLE_SYSTEM_AUDITOR)) ? null : user.getId();
        List<Transaction> transactions = analyticsRepository.findTransactionsForExport(targetUserId, filter);
        return pdfExporter.exportToPdf(username, transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSystemTransactionsToExcel(TransactionExportFilterRequest filter) {
        log.info("Yêu cầu xuất Excel sao kê toàn hệ thống (Super Admin / Auditor)");
        List<Transaction> transactions = analyticsRepository.findTransactionsForExport(null, filter);
        return excelExporter.exportToExcel("SYSTEM_AUDIT_REPORT", transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSystemTransactionsToPdf(TransactionExportFilterRequest filter) {
        log.info("Yêu cầu xuất PDF sao kê toàn hệ thống (Super Admin / Auditor)");
        List<Transaction> transactions = analyticsRepository.findTransactionsForExport(null, filter);
        return pdfExporter.exportToPdf("SYSTEM_AUDIT_REPORT", transactions);
    }

    @Override
    public ImportReportResultDto importTransactionsFromExcel(String username, Long defaultWalletId, InputStream inputStream) {
        log.info("Yêu cầu nhập hàng loạt giao dịch từ file Excel cho user: '{}'", username);
        return excelImporter.importTransactionsFromExcel(username, defaultWalletId, inputStream);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));
    }
}
