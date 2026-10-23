package com.finvault.module1_security_rbac.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * LoginRequest: DTO tiếp nhận thông tin yêu cầu đăng nhập
 * ==============================================================================
 */
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private String totpCode;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTotpCode() {
        return totpCode;
    }

    public void setTotpCode(String totpCode) {
        this.totpCode = totpCode;
    }
}
