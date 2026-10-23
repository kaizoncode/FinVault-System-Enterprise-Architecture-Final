-- ==============================================================================
-- FINVAULT ENTERPRISE - INITIAL SEED DATA (data.sql)
-- Default Roles, Seeded Accounts, Categories, System Configs
-- Idempotent Insert-Only (WHERE NOT EXISTS) to preserve runtime state across restarts
-- ==============================================================================

-- 1. SEED ROLES
INSERT INTO roles (id, role_name, description)
SELECT 1, 'ROLE_SUPER_ADMIN', 'Quản trị viên cấp cao có toàn quyền hệ thống và Live CMS Edit Mode'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id = 1 OR role_name = 'ROLE_SUPER_ADMIN');

INSERT INTO roles (id, role_name, description)
SELECT 2, 'ROLE_SYSTEM_AUDITOR', 'Kiểm toán viên hệ thống, giám sát Audit Log và Data Masking số dư'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id = 2 OR role_name = 'ROLE_SYSTEM_AUDITOR');

INSERT INTO roles (id, role_name, description)
SELECT 3, 'ROLE_FAMILY_HEAD', 'Chủ hộ gia đình, quản lý Quỹ chung và phê duyệt chi tiêu thành viên'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id = 3 OR role_name = 'ROLE_FAMILY_HEAD');

INSERT INTO roles (id, role_name, description)
SELECT 4, 'ROLE_STANDARD_USER', 'Người dùng cá nhân chuẩn, quản lý ví và ngân sách cá nhân'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id = 4 OR role_name = 'ROLE_STANDARD_USER');

-- 2. SEED USERS (BCrypt hashed credentials)
INSERT INTO app_users (id, username, email, password_hash, full_name, phone_number, is_two_factor_enabled, two_factor_secret, pending_two_factor_secret, scratch_codes, failed_login_attempts, locked_until, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 1, 'admin', 'admin@finvault.com', '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 'Nguyễn Quản Trị Viên', '0901234567', FALSE, NULL, NULL, NULL, 0, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = 1 OR username = 'admin');

INSERT INTO app_users (id, username, email, password_hash, full_name, phone_number, is_two_factor_enabled, two_factor_secret, pending_two_factor_secret, scratch_codes, failed_login_attempts, locked_until, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 2, 'auditor', 'auditor@finvault.com', '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 'Trần Kiểm Toán Viên', '0912345678', FALSE, NULL, NULL, NULL, 0, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = 2 OR username = 'auditor');

INSERT INTO app_users (id, username, email, password_hash, full_name, phone_number, is_two_factor_enabled, two_factor_secret, pending_two_factor_secret, scratch_codes, failed_login_attempts, locked_until, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 3, 'family_head', 'family@finvault.com', '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 'Lê Gia Trưởng', '0923456789', FALSE, NULL, NULL, NULL, 0, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = 3 OR username = 'family_head');

INSERT INTO app_users (id, username, email, password_hash, full_name, phone_number, is_two_factor_enabled, two_factor_secret, pending_two_factor_secret, scratch_codes, failed_login_attempts, locked_until, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 4, 'user1', 'user1@finvault.com', '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 'Phạm Người Dùng Chuẩn', '0934567890', FALSE, NULL, NULL, NULL, 0, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = 4 OR username = 'user1');

INSERT INTO app_users (id, username, email, password_hash, full_name, phone_number, is_two_factor_enabled, two_factor_secret, pending_two_factor_secret, scratch_codes, failed_login_attempts, locked_until, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 5, 'user_2fa', 'user2fa@finvault.com', '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 'Hoàng Bảo Mật 2FA', '0945678901', TRUE, 'JBSWY3DPEHPK3PXP', NULL, '$2a$10$FEC/MeKwGq.y2xqlubzip.hpxujNVsafXGpVXLR2Z92WHQ3rlp5VG', 0, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM app_users WHERE id = 5 OR username = 'user_2fa');

-- 3. SEED USER_ROLES MAPPING
INSERT INTO user_roles (user_id, role_id)
SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 1 AND role_id = 1);

INSERT INTO user_roles (user_id, role_id)
SELECT 2, 2 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 2 AND role_id = 2);

INSERT INTO user_roles (user_id, role_id)
SELECT 3, 3 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 3 AND role_id = 3);

INSERT INTO user_roles (user_id, role_id)
SELECT 4, 4 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 4 AND role_id = 4);

INSERT INTO user_roles (user_id, role_id)
SELECT 5, 4 WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 5 AND role_id = 4);

-- 4. SEED SYSTEM DEFAULT CATEGORIES
-- Thu nhập (Income)
INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 1, NULL, 'Lương & Thu nhập cố định', 'INCOME', 'bi-cash-coin', '#198754', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 1);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 2, NULL, 'Thưởng & Đầu tư sinh lời', 'INCOME', 'bi-graph-up-arrow', '#0dcaf0', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 2);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 3, NULL, 'Kinh doanh phụ & Freelance', 'INCOME', 'bi-laptop', '#20c997', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 3);

-- Chi tiêu (Expense)
INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 4, NULL, 'Ăn uống & Cà phê', 'EXPENSE', 'bi-cup-hot', '#fd7e14', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 4);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 5, NULL, 'Tiền nhà & Hóa đơn tiện ích', 'EXPENSE', 'bi-house-door', '#dc3545', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 5);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 6, NULL, 'Đi lại & Xăng xe vận tải', 'EXPENSE', 'bi-car-front', '#6f42c1', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 6);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 7, NULL, 'Y tế & Chăm sóc sức khỏe', 'EXPENSE', 'bi-heart-pulse', '#d63384', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 7);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 8, NULL, 'Mua sắm & Giải trí gia đình', 'EXPENSE', 'bi-bag-heart', '#ffc107', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 8);

INSERT INTO categories (id, user_id, category_name, category_type, icon, color, is_system_default, is_active, created_at, updated_at, is_deleted)
SELECT 9, NULL, 'Giáo dục & Đào tạo cá nhân', 'EXPENSE', 'bi-book', '#0d6efd', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE id = 9);

-- 5. SEED SYSTEM CONFIGURATIONS (Live Admin CMS)
INSERT INTO system_configs (id, config_key, config_value, config_group, description, is_editable, updated_by, updated_at)
SELECT 1, 'SYSTEM_ALERT_THRESHOLD_WARNING', '80', 'SECURITY_ALERT', 'Ngưỡng cảnh báo chi tiêu vượt mức (Vàng)', TRUE, 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE id = 1 OR config_key = 'SYSTEM_ALERT_THRESHOLD_WARNING');

INSERT INTO system_configs (id, config_key, config_value, config_group, description, is_editable, updated_by, updated_at)
SELECT 2, 'SYSTEM_ALERT_THRESHOLD_CRITICAL', '100', 'SECURITY_ALERT', 'Ngưỡng cảnh báo vỡ ngân sách nghiêm trọng (Đỏ)', TRUE, 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE id = 2 OR config_key = 'SYSTEM_ALERT_THRESHOLD_CRITICAL');

INSERT INTO system_configs (id, config_key, config_value, config_group, description, is_editable, updated_by, updated_at)
SELECT 3, 'FAMILY_EXPENSE_APPROVAL_THRESHOLD', '5000000', 'FAMILY_RULE', 'Số tiền chi tiêu gia đình vượt mức cần Gia trưởng phê duyệt (VND)', TRUE, 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE id = 3 OR config_key = 'FAMILY_EXPENSE_APPROVAL_THRESHOLD');

INSERT INTO system_configs (id, config_key, config_value, config_group, description, is_editable, updated_by, updated_at)
SELECT 4, 'SYSTEM_BANNER_MESSAGE', 'Hệ thống FinVault Enterprise đang hoạt động ổn định với cơ chế Live UI & Dynamic RBAC v1.0.', 'SYSTEM_NOTIFICATION', 'Banner thông báo chạy trên thanh tiêu đề hệ thống', TRUE, 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE id = 4 OR config_key = 'SYSTEM_BANNER_MESSAGE');

INSERT INTO system_configs (id, config_key, config_value, config_group, description, is_editable, updated_by, updated_at)
SELECT 5, 'DEFAULT_CURRENCY', 'VND', 'GENERAL', 'Đơn vị tiền tệ chuẩn mặc định của toàn hệ thống', TRUE, 'admin', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_configs WHERE id = 5 OR config_key = 'DEFAULT_CURRENCY');

-- 6. SEED SYSTEM NOTIFICATIONS
INSERT INTO system_notifications (id, title, content, notification_type, is_active, broadcast_to_roles, created_by, created_at, updated_at)
SELECT 1, 'Cập nhật hệ thống FinVault v1.0', 'Tính năng Live Admin Edit Mode và Dynamic RBAC 4 Roles đã chính thức đi vào hoạt động toàn diện.', 'INFO', TRUE, 'ALL', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_notifications WHERE id = 1);

INSERT INTO system_notifications (id, title, content, notification_type, is_active, broadcast_to_roles, created_by, created_at, updated_at)
SELECT 2, 'Bảo mật 2FA & Mã hóa dữ liệu', 'Khuyến cáo tất cả quản trị viên và gia trưởng kích hoạt bảo mật hai lớp Google Authenticator (TOTP).', 'WARNING', TRUE, 'ROLE_SUPER_ADMIN,ROLE_FAMILY_HEAD', 'admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_notifications WHERE id = 2);

-- 7. SEED WALLETS
INSERT INTO wallets (id, user_id, wallet_name, wallet_type, balance, initial_balance, currency, bank_account_number, bank_name, is_family_wallet, monthly_spending_limit, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 1, 4, 'Ví Tiền mặt Hàng ngày', 'CASH', 8500000.00, 13350000.00, 'VND', NULL, NULL, FALSE, 0.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = 1);

INSERT INTO wallets (id, user_id, wallet_name, wallet_type, balance, initial_balance, currency, bank_account_number, bank_name, is_family_wallet, monthly_spending_limit, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 2, 4, 'Tài khoản Techcombank Cá nhân', 'BANK', 45000000.00, 15000000.00, 'VND', '19034567890123', 'Techcombank', FALSE, 0.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = 2);

INSERT INTO wallets (id, user_id, wallet_name, wallet_type, balance, initial_balance, currency, bank_account_number, bank_name, is_family_wallet, monthly_spending_limit, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 3, 3, 'Quỹ Tài Chính Chung Gia Đình', 'FAMILY_FUND', 185000000.00, 185000000.00, 'VND', '1029384756', 'Vietcombank Priority', TRUE, 50000000.00, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'family_head', 'family_head', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE id = 3);

-- Đồng bộ initial_balance cho các ví seed sẵn chưa có initial_balance để Reconciliation khớp 100%
UPDATE wallets SET initial_balance = 13350000.00 WHERE id = 1 AND initial_balance = 0.00 AND balance = 8500000.00;
UPDATE wallets SET initial_balance = 15000000.00 WHERE id = 2 AND initial_balance = 0.00 AND balance = 45000000.00;
UPDATE wallets SET initial_balance = 185000000.00 WHERE id = 3 AND initial_balance = 0.00 AND balance = 185000000.00;

-- 8. SEED WALLET MEMBERS (Gia đình)
INSERT INTO wallet_members (id, wallet_id, user_id, member_role, spending_limit, is_active, joined_at)
SELECT 1, 3, 3, 'OWNER', 50000000.00, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM wallet_members WHERE id = 1 OR (wallet_id = 3 AND user_id = 3));

INSERT INTO wallet_members (id, wallet_id, user_id, member_role, spending_limit, is_active, joined_at)
SELECT 2, 3, 4, 'MEMBER', 10000000.00, TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM wallet_members WHERE id = 2 OR (wallet_id = 3 AND user_id = 4));

-- 9. SEED BUDGETS (Ngân sách thông minh 50/30/20)
INSERT INTO budgets (id, user_id, category_id, wallet_id, budget_name, budget_type, allocation_type, allocated_amount, spent_amount, start_date, end_date, alert_threshold_80_sent, alert_threshold_100_sent, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 1, 4, NULL, NULL, 'Ngân sách Chi tiêu Thiết yếu (Needs 50%)', 'RULE_50_30_20', 'NEEDS', 15000000.00, 6800000.00, CURRENT_DATE, DATEADD('MONTH', 1, CURRENT_DATE), FALSE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE id = 1);

INSERT INTO budgets (id, user_id, category_id, wallet_id, budget_name, budget_type, allocation_type, allocated_amount, spent_amount, start_date, end_date, alert_threshold_80_sent, alert_threshold_100_sent, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 2, 4, NULL, NULL, 'Ngân sách Sở thích & Cá nhân (Wants 30%)', 'RULE_50_30_20', 'WANTS', 9000000.00, 7500000.00, CURRENT_DATE, DATEADD('MONTH', 1, CURRENT_DATE), TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE id = 2);

INSERT INTO budgets (id, user_id, category_id, wallet_id, budget_name, budget_type, allocation_type, allocated_amount, spent_amount, start_date, end_date, alert_threshold_80_sent, alert_threshold_100_sent, is_active, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 3, 4, NULL, NULL, 'Mục tiêu Tích lũy Định kỳ (Savings 20%)', 'RULE_50_30_20', 'SAVINGS', 6000000.00, 6000000.00, CURRENT_DATE, DATEADD('MONTH', 1, CURRENT_DATE), FALSE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM budgets WHERE id = 3);

UPDATE budgets SET category_id = NULL, wallet_id = NULL WHERE id IN (1, 2, 3) AND budget_type = 'RULE_50_30_20';

-- 10. SEED SAVING GOALS
INSERT INTO saving_goals (id, user_id, wallet_id, goal_name, target_amount, current_amount, target_date, status, notes, created_at, updated_at, is_deleted, version)
SELECT 1, 4, 2, 'Quỹ Dự Phòng Khẩn Cấp (6 Tháng)', 100000000.00, 45000000.00, DATEADD('YEAR', 1, CURRENT_DATE), 'IN_PROGRESS', 'Dành cho các tình huống phát sinh y tế hoặc bảo hiểm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM saving_goals WHERE id = 1);

INSERT INTO saving_goals (id, user_id, wallet_id, goal_name, target_amount, current_amount, target_date, status, notes, created_at, updated_at, is_deleted, version)
SELECT 2, 3, 3, 'Quỹ Mua Xe Ô Tô Gia Đình Mới', 500000000.00, 280000000.00, DATEADD('YEAR', 2, CURRENT_DATE), 'IN_PROGRESS', 'Quỹ đóng góp chung từ các thành viên trong gia đình', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM saving_goals WHERE id = 2);

-- 11. SEED SAMPLE TRANSACTIONS
INSERT INTO transactions (id, wallet_id, target_wallet_id, user_id, category_id, transaction_type, amount, transaction_date, note, receipt_url, approval_status, approved_by, approved_at, rejection_reason, is_recurring, recurring_interval, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 1, 2, NULL, 4, 1, 'INCOME', 30000000.00, CURRENT_TIMESTAMP, 'Nhận lương tháng cố định từ Công ty Phần mềm', NULL, 'APPROVED', 'SYSTEM', CURRENT_TIMESTAMP, NULL, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 1);

INSERT INTO transactions (id, wallet_id, target_wallet_id, user_id, category_id, transaction_type, amount, transaction_date, note, receipt_url, approval_status, approved_by, approved_at, rejection_reason, is_recurring, recurring_interval, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 2, 1, NULL, 4, 4, 'EXPENSE', 350000.00, CURRENT_TIMESTAMP, 'Ăn trưa tiếp khách và cà phê đối tác', NULL, 'APPROVED', 'SYSTEM', CURRENT_TIMESTAMP, NULL, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 2);

INSERT INTO transactions (id, wallet_id, target_wallet_id, user_id, category_id, transaction_type, amount, transaction_date, note, receipt_url, approval_status, approved_by, approved_at, rejection_reason, is_recurring, recurring_interval, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 3, 1, NULL, 4, 5, 'EXPENSE', 4500000.00, CURRENT_TIMESTAMP, 'Thanh toán tiền điện và phí dịch vụ chung cư', NULL, 'APPROVED', 'SYSTEM', CURRENT_TIMESTAMP, NULL, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 3);

INSERT INTO transactions (id, wallet_id, target_wallet_id, user_id, category_id, transaction_type, amount, transaction_date, note, receipt_url, approval_status, approved_by, approved_at, rejection_reason, is_recurring, recurring_interval, created_at, updated_at, created_by, updated_by, is_deleted, version)
SELECT 4, 3, NULL, 4, 8, 'EXPENSE', 7500000.00, CURRENT_TIMESTAMP, 'Mua máy giặt sấy thông minh cho gia đình', NULL, 'PENDING', NULL, NULL, NULL, FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'user1', 'user1', FALSE, 0
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE id = 4);

-- 12. SEED INITIAL AUDIT LOGS
INSERT INTO audit_logs (id, actor_username, actor_role, action, target_module, entity_name, entity_id, old_value, new_value, ip_address, user_agent, timestamp, status, failure_reason)
SELECT 1, 'SYSTEM', 'ROLE_SUPER_ADMIN', 'SYSTEM_INITIALIZATION', 'SYSTEM', 'system_configs', 1, NULL, 'Cấu hình mặc định ban đầu được khởi tạo thành công.', '127.0.0.1', 'FinVault Bootstrapper', CURRENT_TIMESTAMP, 'SUCCESS', NULL
WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE id = 1);

INSERT INTO audit_logs (id, actor_username, actor_role, action, target_module, entity_name, entity_id, old_value, new_value, ip_address, user_agent, timestamp, status, failure_reason)
SELECT 2, 'admin', 'ROLE_SUPER_ADMIN', 'RBAC_BOOTSTRAP', 'SECURITY_IAM', 'roles', 4, NULL, 'Khởi tạo 4 nhóm phân quyền chuẩn: Super Admin, Auditor, Family Head, Standard User.', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0)', CURRENT_TIMESTAMP, 'SUCCESS', NULL
WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE id = 2);

-- 13. DÃY SEQUENCE IDENTITY ĐƯỢC TỰ ĐỘNG ĐỒNG BỘ ĐỘNG THEO MAX(ID) + 1 BỞI DatabaseSequenceSynchronizer
-- (Tránh triệt để lỗi xung đột Primary Key 'key: 1000' khi khởi động lại ứng dụng với cơ sở dữ liệu đã có sẵn)
