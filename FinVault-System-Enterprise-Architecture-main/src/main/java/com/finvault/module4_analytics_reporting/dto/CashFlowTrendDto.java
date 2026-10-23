package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * CashFlowTrendDto: DTO cấu trúc chuỗi dữ liệu biến động dòng tiền phục vụ biểu đồ Chart.js
 * ==============================================================================
 */
public class CashFlowTrendDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String timeUnit; // DAY, MONTH, YEAR
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalInflow = BigDecimal.ZERO;
    private BigDecimal totalOutflow = BigDecimal.ZERO;
    private BigDecimal netCashFlow = BigDecimal.ZERO;

    private List<CashFlowDataPoint> dataPoints = new ArrayList<>();

    public CashFlowTrendDto() {
    }

    public static class CashFlowDataPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String label; // "2026-10", "16/10/2026", v.v.
        private LocalDate date;
        private BigDecimal inflow = BigDecimal.ZERO;
        private BigDecimal outflow = BigDecimal.ZERO;
        private BigDecimal netFlow = BigDecimal.ZERO;

        public CashFlowDataPoint() {
        }

        public CashFlowDataPoint(String label, LocalDate date, BigDecimal inflow, BigDecimal outflow, BigDecimal netFlow) {
            this.label = label;
            this.date = date;
            this.inflow = inflow;
            this.outflow = outflow;
            this.netFlow = netFlow;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getInflow() {
            return inflow;
        }

        public void setInflow(BigDecimal inflow) {
            this.inflow = inflow;
        }

        public BigDecimal getOutflow() {
            return outflow;
        }

        public void setOutflow(BigDecimal outflow) {
            this.outflow = outflow;
        }

        public BigDecimal getNetFlow() {
            return netFlow;
        }

        public void setNetFlow(BigDecimal netFlow) {
            this.netFlow = netFlow;
        }
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalInflow() {
        return totalInflow;
    }

    public void setTotalInflow(BigDecimal totalInflow) {
        this.totalInflow = totalInflow;
    }

    public BigDecimal getTotalOutflow() {
        return totalOutflow;
    }

    public void setTotalOutflow(BigDecimal totalOutflow) {
        this.totalOutflow = totalOutflow;
    }

    public BigDecimal getNetCashFlow() {
        return netCashFlow;
    }

    public void setNetCashFlow(BigDecimal netFlow) {
        this.netCashFlow = netFlow;
    }

    public List<CashFlowDataPoint> getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(List<CashFlowDataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }
}
