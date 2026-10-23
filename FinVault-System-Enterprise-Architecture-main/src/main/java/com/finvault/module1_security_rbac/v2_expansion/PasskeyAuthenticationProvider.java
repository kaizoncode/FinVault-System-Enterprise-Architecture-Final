package com.finvault.module1_security_rbac.v2_expansion;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * [Extension Point - V2.0 Future Expansion 1]: PasskeyAuthenticationProvider
 * Interface mở rộng cung cấp khả năng tích hợp WebAuthn / FIDO2 Passwordless Authentication
 * Cho phép người dùng đăng nhập bằng Windows Hello, Apple FaceID/TouchID hoặc YubiKey phần cứng.
 *
 * Lộ trình tích hợp v2.0:
 * 1. Tích hợp thư viện com.yubico.webauthn:webauthn-server-core
 * 2. Triển khai sinh Challenge ngẫu nhiên (Registration & Assertion Challenge)
 * 3. Lưu trữ Public Key Credential trong bảng app_user_passkeys
 * 4. Xác thực chữ ký số SHA-256 with ECDSA không truyền mật khẩu qua mạng
 * ==============================================================================
 */
public interface PasskeyAuthenticationProvider {

    /**
     * Tạo Registration Challenge cho client để bắt đầu đăng ký khóa sinh trắc học
     */
    String generateRegistrationChallenge(String username);

    /**
     * Xác thực phản hồi đăng ký từ Authenticator và lưu trữ Public Key Credential
     */
    PasskeyCredentialDto registerPasskey(String username, String clientDataJson, String attestationObject);

    /**
     * Tạo Assertion Challenge cho client để chuẩn bị đăng nhập
     */
    String generateAssertionChallenge(String username);

    /**
     * Xác thực chữ ký số Assertion và đăng nhập người dùng mà không cần mật khẩu
     */
    boolean verifyAssertion(String username, String credentialId, String clientDataJson, String authenticatorData, String signature);

    /**
     * Lấy danh sách các Passkeys đã đăng ký của người dùng
     */
    List<PasskeyCredentialDto> getUserPasskeys(String username);

    /**
     * Xóa một khóa Passkey khi người dùng bị mất thiết bị
     */
    void revokePasskey(String username, String credentialId);
}
