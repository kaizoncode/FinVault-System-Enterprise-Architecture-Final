package com.finvault.module5_admin_audit.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogFilterRequest: DTO tham số lọc vết kiểm toán (Audit Trail) cho Auditor
 * ==============================================================================
 */
public class AuditLogFilterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String actorUsername;
    private String targetModule;
    private String action;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public AuditLogFilterRequest() {
    }

    public AuditLogFilterRequest(String actorUsername, String targetModule, String action, LocalDateTime startDate, LocalDateTime endDate) {
        this.actorUsername = actorUsername;
        this.targetModule = targetModule;
        this.action = action;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
