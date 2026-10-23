package com.finvault.module4_analytics_reporting.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module4_analytics_reporting.dto.ImportReportResultDto;
import com.finvault.module4_analytics_reporting.dto.TransactionExportFilterRequest;
import com.finvault.module4_analytics_reporting.service.ReportExportImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ReportApiController: REST API Xuất/Nhập báo cáo tài chính (Excel & PDF)
 *
 * Tính năng chính:
 * - Xuất báo cáo sao kê cá nhân chuẩn Excel (.xlsx) qua Apache POI
 * - Xuất báo cáo sao kê điện tử (.pdf) qua OpenPDF
 * - Xuất báo cáo kiểm toán toàn hệ thống cho Super Admin và Auditor
 * - Nhập hàng loạt giao dịch từ file Excel (hỗ trợ multipart upload)
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("isAuthenticated()")
public class ReportApiController {

    private final ReportExportImportService reportService;

    public ReportApiController(ReportExportImportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Xuất báo cáo sao kê giao dịch cá nhân ra file Excel (.xlsx) qua GET hoặc POST
     */
    @GetMapping(value = {"/export/excel", "/transactions/export/excel"})
    public ResponseEntity<byte[]> exportPersonalExcelGet(
            @ModelAttribute TransactionExportFilterRequest filter) {
        String username = SecurityUtils.getCurrentUsername();
        boolean isMacro = SecurityUtils.isSuperAdmin() || SecurityUtils.isSystemAuditor();
        byte[] bytes = isMacro
                ? reportService.exportSystemTransactionsToExcel(filter)
                : reportService.exportTransactionsToExcel(username, filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-statement-" + username + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = {"/export/excel", "/transactions/export/excel"})
    public ResponseEntity<byte[]> exportPersonalExcel(
            @RequestBody(required = false) @Valid TransactionExportFilterRequest filter) {
        String username = SecurityUtils.getCurrentUsername();
        boolean isMacro = SecurityUtils.isSuperAdmin() || SecurityUtils.isSystemAuditor();
        byte[] bytes = isMacro
                ? reportService.exportSystemTransactionsToExcel(filter)
                : reportService.exportTransactionsToExcel(username, filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-statement-" + username + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Xuất báo cáo sao kê giao dịch cá nhân ra file PDF (.pdf) qua GET hoặc POST
     */
    @GetMapping(value = {"/export/pdf", "/transactions/export/pdf", "/financial-statement/pdf"})
    public ResponseEntity<byte[]> exportPersonalPdfGet(
            @ModelAttribute TransactionExportFilterRequest filter) {
        String username = SecurityUtils.getCurrentUsername();
        boolean isMacro = SecurityUtils.isSuperAdmin() || SecurityUtils.isSystemAuditor();
        byte[] bytes = isMacro
                ? reportService.exportSystemTransactionsToPdf(filter)
                : reportService.exportTransactionsToPdf(username, filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-statement-" + username + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping(value = {"/export/pdf", "/transactions/export/pdf", "/financial-statement/pdf"})
    public ResponseEntity<byte[]> exportPersonalPdf(
            @RequestBody(required = false) @Valid TransactionExportFilterRequest filter) {
        String username = SecurityUtils.getCurrentUsername();
        boolean isMacro = SecurityUtils.isSuperAdmin() || SecurityUtils.isSystemAuditor();
        byte[] bytes = isMacro
                ? reportService.exportSystemTransactionsToPdf(filter)
                : reportService.exportTransactionsToPdf(username, filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-statement-" + username + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /**
     * Xuất báo cáo kiểm toán toàn hệ thống ra Excel (Super Admin / Auditor)
     */
    @GetMapping("/system/export/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<byte[]> exportSystemExcelGet(
            @ModelAttribute TransactionExportFilterRequest filter) {
        byte[] bytes = reportService.exportSystemTransactionsToExcel(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-system-audit.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/system/export/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<byte[]> exportSystemExcel(
            @RequestBody(required = false) @Valid TransactionExportFilterRequest filter) {
        byte[] bytes = reportService.exportSystemTransactionsToExcel(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-system-audit.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Xuất báo cáo kiểm toán toàn hệ thống ra PDF (Super Admin / Auditor)
     */
    @GetMapping("/system/export/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<byte[]> exportSystemPdfGet(
            @ModelAttribute TransactionExportFilterRequest filter) {
        byte[] bytes = reportService.exportSystemTransactionsToPdf(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-system-audit.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @PostMapping("/system/export/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<byte[]> exportSystemPdf(
            @RequestBody(required = false) @Valid TransactionExportFilterRequest filter) {
        byte[] bytes = reportService.exportSystemTransactionsToPdf(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finvault-system-audit.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /**
     * Nhập hàng loạt giao dịch từ file Excel tải lên
     */
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportReportResultDto>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "walletId", required = false) Long walletId) throws IOException {
        String username = SecurityUtils.getCurrentUsername();
        ImportReportResultDto result = reportService.importTransactionsFromExcel(username, walletId, file.getInputStream());
        return ResponseEntity.ok(ApiResponse.success("Xử lý file Excel hoàn tất", result));
    }
}
