package com.finvault.module1_security_rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * BootstrapAdminRequest: DTO khởi tạo / cấu hình mật khẩu quản trị ban đầu
 * ==============================================================================
 */
public class BootstrapAdminRequest {

    private String setupToken;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username = "admin";

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải có tối thiểu 8 ký tự")
    private String newPassword;

    public BootstrapAdminRequest() {
    }

    public BootstrapAdminRequest(String setupToken, String username, String newPassword) {
        this.setupToken = setupToken;
        this.username = username;
        this.newPassword = newPassword;
    }

    public String getSetupToken() {
        return setupToken;
    }

    public void setSetupToken(String setupToken) {
        this.setupToken = setupToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
