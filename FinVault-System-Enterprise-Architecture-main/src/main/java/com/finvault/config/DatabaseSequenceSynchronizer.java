package com.finvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ==============================================================================
 * DatabaseSequenceSynchronizer: Tự động đồng bộ hóa linh hoạt Sequence và Identity PK
 * cho tất cả các bảng trong hệ thống FinVault.
 *
 * Giải quyết triệt để lỗi xung đột Primary Key:
 * "Unique index or primary key violation: PRIMARY KEY ON PUBLIC.WALLETS(ID) (key: 1000)"
 * khi đăng ký tài khoản mới hoặc khi khởi động lại ứng dụng với cơ sở dữ liệu có sẵn (Persistent H2 / PostgreSQL / MySQL).
 *
 * Tính năng kỹ thuật linh hoạt cao (High Flexibility & Resiliency):
 * 1. Tự động truy vấn MAX(id) thực tế của từng bảng trong cơ sở dữ liệu hiện hành.
 * 2. Đảm bảo Sequence luôn bắt đầu từ MAX(id) + 1 (hoặc tối thiểu 1000 nếu bảng mới).
 * 3. Hỗ trợ đa hệ quản trị cơ sở dữ liệu (H2, PostgreSQL, MySQL/MariaDB, SQL Server, Oracle).
 * 4. Tự động nhận diện động danh sách bảng từ Database Metadata kết hợp bảng nghiệp vụ cốt lõi.
 * 5. Chạy tự động tại cả 2 giai đoạn: sau khi khởi tạo Bean (SmartInitializingSingleton)
 *    và khi ứng dụng sẵn sàng nhận traffic (ApplicationReadyEvent).
 * ==============================================================================
 */
@Component
public class DatabaseSequenceSynchronizer implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSequenceSynchronizer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    // Danh sách bảng nghiệp vụ cốt lõi có khóa chính tự tăng (Identity PK)
    private static final List<String> CORE_TABLES = List.of(
            "roles",
            "app_users",
            "categories",
            "wallets",
            "wallet_members",
            "transactions",
            "budgets",
            "saving_goals",
            "loan_calculations",
            "audit_logs",
            "system_configs",
            "system_notifications"
    );

    public DatabaseSequenceSynchronizer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void afterSingletonsInstantiated() {
        syncAllSequences("SmartInitializingSingleton");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void onApplicationReady() {
        syncAllSequences("ApplicationReadyEvent");
    }

    /**
     * Đồng bộ hóa toàn diện sequence identity cho toàn bộ bảng trong cơ sở dữ liệu.
     */
    public synchronized void syncAllSequences(String triggerSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProductName = metaData.getDatabaseProductName().toLowerCase();
            log.info("[SequenceSync - {}] Kiểm tra và đồng bộ Identity Sequence trên RDBMS: [{}]",
                    triggerSource, metaData.getDatabaseProductName());

            Set<String> targetTables = new LinkedHashSet<>(CORE_TABLES);

            // Tự động quét bổ sung tất cả bảng người dùng từ Database Metadata (Flexible Discovery)
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), conn.getSchema(), "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tbl = rs.getString("TABLE_NAME");
                    if (tbl != null) {
                        String lowerTbl = tbl.toLowerCase();
                        if (!lowerTbl.startsWith("system_") && !lowerTbl.startsWith("information_schema")) {
                            targetTables.add(lowerTbl);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[SequenceSync] Quét metadata bổ sung bỏ qua: {}", e.getMessage());
            }

            int successCount = 0;
            for (String tableName : targetTables) {
                if (syncTableSequence(tableName, dbProductName)) {
                    successCount++;
                }
            }
            log.info("[SequenceSync - {}] Đã đồng bộ thành công [{}/{}] bảng dữ liệu.",
                    triggerSource, successCount, targetTables.size());

        } catch (Exception ex) {
            log.warn("[SequenceSync] Có lỗi trong quá trình đồng bộ Sequence: {}", ex.getMessage());
        }
    }

    /**
     * Đồng bộ hóa sequence của 1 bảng cụ thể theo giá trị MAX(id) thực tế.
     */
    public boolean syncTableSequence(String tableName, String dbProductName) {
        try {
            // Kiểm tra bảng có cột 'id' hay không và lấy MAX(id) thực tế
            String maxQuery = "SELECT COALESCE(MAX(id), 0) FROM " + tableName;
            Long maxId = jdbcTemplate.queryForObject(maxQuery, Long.class);
            if (maxId == null) maxId = 0L;

            // Đảm bảo next identity luôn lớn hơn MAX(id) hiện có và đạt mức an toàn tối thiểu 1000
            // Nếu bảng đã có ID >= 1000 (ví dụ 1000, 1005), nextVal sẽ là maxId + 1 (1001, 1006) để triệt tiêu 100% nguy cơ trùng lặp
            long nextVal = Math.max(maxId + 1, 1000L);

            if (dbProductName.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH " + nextVal);
            } else if (dbProductName.contains("postgres")) {
                try {
                    jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('" + tableName + "', 'id'), " + Math.max(maxId, 1000L) + ", true)");
                } catch (Exception pe) {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH " + nextVal);
                }
            } else if (dbProductName.contains("mysql") || dbProductName.contains("mariadb")) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = " + nextVal);
            } else if (dbProductName.contains("microsoft") || dbProductName.contains("sql server")) {
                jdbcTemplate.execute("DBCC CHECKIDENT('" + tableName + "', RESEED, " + Math.max(maxId, 1000L) + ")");
            } else if (dbProductName.contains("oracle")) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY id GENERATED AS IDENTITY (START WITH " + nextVal + ")");
            }

            log.debug("[SequenceSync] Bảng [{}]: MAX(id) = {}, Next Identity Sequence = {}", tableName, maxId, nextVal);
            return true;

        } catch (Exception ex) {
            // Bỏ qua các bảng không có cột 'id' (như user_roles) hoặc bảng hệ thống
            return false;
        }
    }
}
