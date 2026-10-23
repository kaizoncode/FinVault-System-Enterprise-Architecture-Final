package com.finvault.module1_security_rbac.v2_expansion;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * [Extension Point - V2.0 Future Expansion 1]: WebAuthn FIDO2 Passkey Credential DTO
 * Đại diện cho dữ liệu khóa công khai (Public Key Credential) phục vụ đăng nhập sinh trắc học
 * (Touch ID, Face ID, Windows Hello, YubiKey) không cần mật khẩu.
 * ==============================================================================
 */
public class PasskeyCredentialDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String credentialId;
    private String userHandle;
    private String publicKeyBase64;
    private Long signCount;
    private String authenticatorType;
    private LocalDateTime registeredAt;

    public PasskeyCredentialDto() {
    }

    public PasskeyCredentialDto(String credentialId, String userHandle, String publicKeyBase64, Long signCount, String authenticatorType) {
        this.credentialId = credentialId;
        this.userHandle = userHandle;
        this.publicKeyBase64 = publicKeyBase64;
        this.signCount = signCount;
        this.authenticatorType = authenticatorType;
        this.registeredAt = LocalDateTime.now();
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    public Long getSignCount() {
        return signCount;
    }

    public void setSignCount(Long signCount) {
        this.signCount = signCount;
    }

    public String getAuthenticatorType() {
        return authenticatorType;
    }

    public void setAuthenticatorType(String authenticatorType) {
        this.authenticatorType = authenticatorType;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
