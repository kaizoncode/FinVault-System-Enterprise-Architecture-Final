package com.finvault.module1_security_rbac.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * TwoFactorVerifyDto: DTO xác thực mã 2FA từ Client
 * ==============================================================================
 */
public class TwoFactorVerifyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã xác thực 2FA không được để trống")
    private String code;

    public TwoFactorVerifyDto() {
    }

    public TwoFactorVerifyDto(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
