package com.finvault.module1_security_rbac.repository;

import com.finvault.module1_security_rbac.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserRepository: Tầng truy xuất dữ liệu Người dùng, hỗ trợ xác thực, chống Brute-force
 * và quản lý trạng thái tài khoản
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ Full-text Search người dùng theo họ tên, số điện thoại hoặc mã định danh gia đình
 * ==============================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.isDeleted = false")
    boolean existsByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.isActive = true AND u.isDeleted = false")
    long countActiveUsersByRoleName(@Param("roleName") String roleName);

    List<User> findAllByIsDeletedFalse();

    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil > CURRENT_TIMESTAMP AND u.isDeleted = false")
    List<User> findCurrentlyLockedUsers();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.username = :username")
    void resetFailedLoginAttempts(@Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedLoginAttempts(@Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.username = :username")
    void lockUserAccount(@Param("username") String username, @Param("lockedUntil") LocalDateTime lockedUntil);
}
