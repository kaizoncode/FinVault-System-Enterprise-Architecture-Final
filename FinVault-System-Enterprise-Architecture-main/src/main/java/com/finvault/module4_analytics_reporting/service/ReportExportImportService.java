package com.finvault.module4_analytics_reporting.service;

import com.finvault.module4_analytics_reporting.dto.ImportReportResultDto;
import com.finvault.module4_analytics_reporting.dto.TransactionExportFilterRequest;

import java.io.InputStream;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ReportExportImportService: Interface xuất sao kê (Excel/PDF) và nhập dữ liệu hàng loạt
 * ==============================================================================
 */
public interface ReportExportImportService {

    byte[] exportTransactionsToExcel(String username, TransactionExportFilterRequest filter);

    byte[] exportTransactionsToPdf(String username, TransactionExportFilterRequest filter);

    byte[] exportSystemTransactionsToExcel(TransactionExportFilterRequest filter);

    byte[] exportSystemTransactionsToPdf(TransactionExportFilterRequest filter);

    ImportReportResultDto importTransactionsFromExcel(String username, Long defaultWalletId, InputStream inputStream);
}
