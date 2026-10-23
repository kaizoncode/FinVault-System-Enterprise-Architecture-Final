package com.finvault.module1_security_rbac.service;

import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * CustomUserDetailsService: Nạp thông tin định danh và phân quyền người dùng cho Spring Security
 * Tự động kiểm tra và giải tỏa khóa tài khoản nếu thời gian phạt 15 phút đã trôi qua.
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ đăng nhập đa phương thức (bằng Username HOẶC Email HOẶC Số điện thoại)
 * ==============================================================================
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new UsernameNotFoundException("Tên đăng nhập hoặc email không được để trống");
        }
        String cleanInput = usernameOrEmail.trim();
        String lowerInput = cleanInput.toLowerCase();

        User user = userRepository.findByUsernameAndIsDeletedFalse(cleanInput)
                .or(() -> userRepository.findByUsernameAndIsDeletedFalse(lowerInput))
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(lowerInput))
                .or(() -> userRepository.findByEmailAndIsDeletedFalse(cleanInput))
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Không tìm thấy tài khoản '{}'", usernameOrEmail);
                    return new UsernameNotFoundException("Tài khoản hoặc mật khẩu không chính xác: " + usernameOrEmail);
                });

        // Nếu tài khoản đã từng bị khóa nhưng thời gian khóa đã hết -> tự động giải phóng
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            log.info("Tài khoản [{}] đã tự động hết hạn khóa 15 phút và được mở khóa lại.", user.getUsername());
        }

        return user;
    }
}
