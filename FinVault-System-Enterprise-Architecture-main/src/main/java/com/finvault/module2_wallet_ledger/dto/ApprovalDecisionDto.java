package com.finvault.module2_wallet_ledger.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * ApprovalDecisionDto: DTO phê duyệt hoặc từ chối khoản chi lớn của Quỹ gia đình
 * ==============================================================================
 */
public class ApprovalDecisionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Quyết định phê duyệt (true = Duyệt, false = Từ chối) không được để trống")
    private Boolean approved;

    private String rejectionReason;

    public ApprovalDecisionDto() {
    }

    public ApprovalDecisionDto(Boolean approved, String rejectionReason) {
        this.approved = approved;
        this.rejectionReason = rejectionReason;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
