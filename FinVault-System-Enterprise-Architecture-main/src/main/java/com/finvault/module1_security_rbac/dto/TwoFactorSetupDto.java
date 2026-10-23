package com.finvault.module1_security_rbac.dto;

import java.io.Serializable;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * TwoFactorSetupDto: DTO chứa thông tin khóa bí mật, QR Code và Scratch Codes
 * ==============================================================================
 */
public class TwoFactorSetupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String secretKey;
    private String qrCodeUrl;
    private List<String> scratchCodes;

    public TwoFactorSetupDto() {
    }

    public TwoFactorSetupDto(String secretKey, String qrCodeUrl, List<String> scratchCodes) {
        this.secretKey = secretKey;
        this.qrCodeUrl = qrCodeUrl;
        this.scratchCodes = scratchCodes;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public List<String> getScratchCodes() {
        return scratchCodes;
    }

    public void setScratchCodes(List<String> scratchCodes) {
        this.scratchCodes = scratchCodes;
    }
}
