package com.finvault.module5_admin_audit.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * DataRecoveryResponseDto: DTO phản hồi danh sách các đối tượng bị xóa mềm (Soft-deleted)
 * có thể khôi phục (Ví, Giao dịch, Ngân sách, Mục tiêu)
 * ==============================================================================
 */
public class DataRecoveryResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String entityType; // WALLET, TRANSACTION, BUDGET, SAVING_GOAL
    private Long entityId;
    private String entityIdentifier; // Tên ví, tên ngân sách, mã giao dịch
    private String ownerUsername;
    private LocalDateTime deletedAt;
    private String additionalDetails;

    public DataRecoveryResponseDto() {
    }

    public DataRecoveryResponseDto(String entityType, Long entityId, String entityIdentifier, String ownerUsername, LocalDateTime deletedAt, String additionalDetails) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityIdentifier = entityIdentifier;
        this.ownerUsername = ownerUsername;
        this.deletedAt = deletedAt;
        this.additionalDetails = additionalDetails;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityIdentifier() {
        return entityIdentifier;
    }

    public void setEntityIdentifier(String entityIdentifier) {
        this.entityIdentifier = entityIdentifier;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    public String getItemType() {
        return this.entityType;
    }

    public Long getItemId() {
        return this.entityId;
    }

    public String getTitle() {
        return this.entityIdentifier != null
                ? (this.additionalDetails != null && !this.additionalDetails.isBlank()
                        ? this.entityIdentifier + " (" + this.additionalDetails + ")"
                        : this.entityIdentifier)
                : "";
    }
}
