package com.finvault.module1_security_rbac.entity;

import com.finvault.common.BaseEntity;
import com.finvault.module1_security_rbac.crypto.AesEncryptionConverter;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * User Entity: Thực thể Người dùng trung tâm của hệ thống FinVault
 *
 * Tính năng bảo mật tích hợp:
 * - Triển khai Spring Security UserDetails hỗ trợ xác thực trực tiếp
 * - Khóa bí mật 2FA (TOTP) được mã hóa AES-256-GCM tự động thông qua JPA AttributeConverter
 * - Cơ chế bảo vệ Brute-force: đếm số lần sai mật khẩu (failedLoginAttempts)
 *   và khóa tài khoản tạm thời (lockedUntil)
 * - Dynamic RBAC: Ánh xạ nhiều Role chuẩn (SUPER_ADMIN, SYSTEM_AUDITOR, FAMILY_HEAD, STANDARD_USER)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp trường WebAuthn Credential ID để đăng nhập không cần mật khẩu (Passkey v2.0)
 * ==============================================================================
 */
@Entity
@Table(name = "app_users")
public class User extends BaseEntity implements UserDetails {

    private static final long serialVersionUID = 1L;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_two_factor_enabled", nullable = false)
    private Boolean isTwoFactorEnabled = Boolean.FALSE;

    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "pending_two_factor_secret", length = 255)
    private String pendingTwoFactorSecret;

    @Column(name = "scratch_codes", length = 2000)
    private String scratchCodes;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(String username, String email, String passwordHash, String fullName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isActive = Boolean.TRUE;
        this.isTwoFactorEnabled = Boolean.FALSE;
        this.failedLoginAttempts = 0;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles != null ? roles : Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) {
            return true;
        }
        return lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isActive) && !Boolean.TRUE.equals(this.getIsDeleted());
    }

    public boolean hasRole(String roleName) {
        if (roles == null || roles.isEmpty() || roleName == null) {
            return false;
        }
        for (Role r : roles) {
            if (roleName.equals(r.getRoleName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuperAdmin() {
        return hasRole(Role.ROLE_SUPER_ADMIN);
    }

    public boolean isSystemAuditor() {
        return hasRole(Role.ROLE_SYSTEM_AUDITOR);
    }

    public boolean isFamilyHead() {
        return hasRole(Role.ROLE_FAMILY_HEAD);
    }

    public boolean isStandardUser() {
        return hasRole(Role.ROLE_STANDARD_USER);
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        if (this.roles != null) {
            this.roles.remove(role);
        }
    }

    // Getters and Setters
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public String getTwoFactorSecret() {
        return twoFactorSecret;
    }

    public void setTwoFactorSecret(String twoFactorSecret) {
        this.twoFactorSecret = twoFactorSecret;
    }

    public String getPendingTwoFactorSecret() {
        return pendingTwoFactorSecret;
    }

    public void setPendingTwoFactorSecret(String pendingTwoFactorSecret) {
        this.pendingTwoFactorSecret = pendingTwoFactorSecret;
    }

    public String getScratchCodes() {
        return scratchCodes;
    }

    public void setScratchCodes(String scratchCodes) {
        this.scratchCodes = scratchCodes;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", isTwoFactorEnabled=" + isTwoFactorEnabled +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", lockedUntil=" + lockedUntil +
                ", isActive=" + isActive +
                '}';
    }
}
