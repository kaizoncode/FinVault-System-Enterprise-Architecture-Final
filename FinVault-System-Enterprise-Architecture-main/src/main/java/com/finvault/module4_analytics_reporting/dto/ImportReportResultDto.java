package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * ImportReportResultDto: DTO phản hồi kết quả import hàng loạt giao dịch từ Excel/CSV
 * ==============================================================================
 */
public class ImportReportResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalRowsProcessed = 0;
    private int successCount = 0;
    private int skippedDuplicatesCount = 0;
    private int failureCount = 0;
    private List<String> errorMessages = new ArrayList<>();

    public ImportReportResultDto() {
    }

    public ImportReportResultDto(int totalRowsProcessed, int successCount, int skippedDuplicatesCount, int failureCount) {
        this.totalRowsProcessed = totalRowsProcessed;
        this.successCount = successCount;
        this.skippedDuplicatesCount = skippedDuplicatesCount;
        this.failureCount = failureCount;
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSkippedDuplicatesCount() {
        return skippedDuplicatesCount;
    }

    public void setSkippedDuplicatesCount(int skippedDuplicatesCount) {
        this.skippedDuplicatesCount = skippedDuplicatesCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
    }
}
