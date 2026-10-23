package com.finvault.module1_security_rbac.service;

import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * BruteForceProtectionService: Cơ chế bảo vệ chống tấn công dò mật khẩu (Brute-force Protection)
 *
 * Chính sách bảo mật:
 * - Khóa tài khoản tạm thời 15 phút nếu người dùng đăng nhập sai 5 lần liên tiếp
 * - Tự động mở khóa sau khi hết thời gian 15 phút
 * - Tự động xóa bộ đếm thất bại về 0 ngay khi đăng nhập thành công
 * - Cho phép Super Admin mở khóa tài khoản thủ công qua Live Admin CMS
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Cloudflare Turnstile / Google reCAPTCHA v3 khi phát hiện IP bất thường
 * ==============================================================================
 */
@Service
public class BruteForceProtectionService {

    private static final Logger log = LoggerFactory.getLogger(BruteForceProtectionService.class);

    private final UserRepository userRepository;
    private final int maxFailedAttempts;
    private final int lockDurationMinutes;

    public BruteForceProtectionService(
            UserRepository userRepository,
            @Value("${finvault.security.brute-force.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${finvault.security.brute-force.lock-duration-minutes:15}") int lockDurationMinutes) {
        this.userRepository = userRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    /**
     * Tra cứu người dùng theo Username HOẶC Email (hỗ trợ cả 2 phương thức đăng nhập)
     */
    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameAndIsDeletedFalse(usernameOrEmail.trim())
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(usernameOrEmail.trim().toLowerCase()));
    }

    /**
     * Ghi nhận một lần đăng nhập thất bại. Nếu vượt quá ngưỡng, tiến hành khóa tài khoản.
     */
    @Transactional
    public void recordFailedLogin(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        // Issue 2: Nếu tài khoản đang trong trạng thái bị khóa, KHÔNG reset timer và không tăng attempts
        if (isAccountLocked(username)) {
            return;
        }

        Optional<User> userOpt = findUserByUsernameOrEmail(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        String canonicalUsername = user.getUsername();

        // Issue 3: Chống Race Condition bằng atomic query thay cho read-modify-write
        userRepository.incrementFailedLoginAttempts(canonicalUsername);

        // Truy vấn lại sau khi đã increment atomic để kiểm tra ngưỡng khóa
        User updatedUser = userRepository.findByUsernameAndIsDeletedFalse(canonicalUsername).orElse(user);
        int currentAttempts = (updatedUser.getFailedLoginAttempts() != null) ? updatedUser.getFailedLoginAttempts() : 0;

        if (currentAttempts >= maxFailedAttempts) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
            userRepository.lockUserAccount(canonicalUsername, lockUntil);
            log.warn("CẢNH BÁO BẢO MẬT: Tài khoản [{}] đã bị KHÓA trong {} phút do nhập sai mật khẩu {} lần liên tiếp!",
                    canonicalUsername, lockDurationMinutes, currentAttempts);
        } else {
            log.info("Người dùng [{}] nhập sai mật khẩu lần thứ {}/{}", canonicalUsername, currentAttempts, maxFailedAttempts);
        }
    }

    /**
     * Xóa sạch số lần đăng nhập sai khi người dùng đăng nhập thành công
     */
    @Transactional
    public void recordSuccessfulLogin(String username) {
        Optional<User> userOpt = findUserByUsernameOrEmail(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
                log.info("Bộ đếm đăng nhập của người dùng [{}] đã được làm mới thành công.", username);
            }
        }
    }

    /**
     * Kiểm tra xem tài khoản có đang bị khóa hay không
     */
    @Transactional(readOnly = true)
    public boolean isAccountLocked(String username) {
        Optional<User> userOpt = findUserByUsernameOrEmail(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        if (user.getLockedUntil() == null) {
            return false;
        }
        return user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Lấy số phút còn lại cho đến khi tài khoản được tự động mở khóa
     */
    @Transactional(readOnly = true)
    public long getRemainingLockMinutes(String username) {
        Optional<User> userOpt = findUserByUsernameOrEmail(username);
        if (userOpt.isEmpty()) {
            return 0;
        }
        User user = userOpt.get();
        if (user.getLockedUntil() == null || user.getLockedUntil().isBefore(LocalDateTime.now())) {
            return 0;
        }
        return Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
    }

    /**
     * Mở khóa tài khoản thủ công (Dành riêng cho Super Admin)
     */
    @Transactional
    public void unlockAccountManually(String username) {
        Optional<User> userOpt = findUserByUsernameOrEmail(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            log.info("Super Admin đã mở khóa thủ công thành công cho tài khoản [{}].", username);
        }
    }
}
