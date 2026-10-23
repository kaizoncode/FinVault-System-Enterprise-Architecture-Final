package com.finvault.module1_security_rbac.event;

import com.finvault.module1_security_rbac.entity.User;
import org.springframework.context.ApplicationEvent;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserRegisteredEvent: Sự kiện phát sinh ngay khi tài khoản người dùng mới đăng ký thành công.
 *
 * Áp dụng Observer Pattern / Event-Driven Architecture:
 * - Module 2 lắng nghe để tự động khởi tạo Ví tiền mặt cá nhân đầu tiên
 *   và tự động liên kết thành viên phụ thuộc vào Quỹ chung gia đình.
 * - Module 3 lắng nghe để tự động thiết lập bộ 3 ngân sách chuẩn 50/30/20.
 * ==============================================================================
 */
public class UserRegisteredEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final User user;

    public UserRegisteredEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
