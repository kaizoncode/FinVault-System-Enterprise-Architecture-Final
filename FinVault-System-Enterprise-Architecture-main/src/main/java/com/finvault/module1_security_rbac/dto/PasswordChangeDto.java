package com.finvault.module1_security_rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * PasswordChangeDto: DTO đổi mật khẩu người dùng
 * ==============================================================================
 */
public class PasswordChangeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải có tối thiểu 8 ký tự")
    private String newPassword;

    public PasswordChangeDto() {
    }

    public PasswordChangeDto(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
