package com.finvault.module1_security_rbac.dto;

import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserResponseDto: DTO trả về thông tin người dùng an toàn (loại bỏ password & secret)
 * ==============================================================================
 */
public class UserResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Boolean isTwoFactorEnabled;
    private Boolean isActive;
    private Boolean isLocked;
    private LocalDateTime lockedUntil;
    private Set<String> roles;
    private LocalDateTime createdAt;

    public UserResponseDto() {
    }

    public static UserResponseDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setIsTwoFactorEnabled(user.getIsTwoFactorEnabled());
        dto.setIsActive(user.getIsActive());
        dto.setIsLocked(user.isAccountLocked());
        dto.setLockedUntil(user.getLockedUntil());
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet()));
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getIsTwoFactorEnabled() {
        return isTwoFactorEnabled;
    }

    public void setIsTwoFactorEnabled(Boolean twoFactorEnabled) {
        isTwoFactorEnabled = twoFactorEnabled;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean locked) {
        isLocked = locked;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
