package com.finvault.module1_security_rbac.service;

import com.finvault.module1_security_rbac.dto.PasswordChangeDto;
import com.finvault.module1_security_rbac.dto.RegisterRequest;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.entity.User;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserService: Interface định nghĩa các nghiệp vụ quản lý tài khoản người dùng
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp SSO (Single Sign-On) qua Google / Microsoft Entra ID OAuth2
 * ==============================================================================
 */
public interface UserService {

    UserResponseDto registerUser(RegisterRequest request);

    UserResponseDto getUserByUsername(String username);

    User getUserEntityByUsername(String username);

    List<UserResponseDto> getAllUsers();

    List<UserResponseDto> getLockedUsers();

    UserResponseDto updateUserStatus(Long userId, boolean isActive);

    void unlockUser(Long userId);

    void changePassword(String username, PasswordChangeDto dto);
}
