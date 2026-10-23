package com.finvault.module1_security_rbac.repository;

import com.finvault.module1_security_rbac.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * RoleRepository: Tầng truy xuất dữ liệu nhóm quyền trong hệ thống RBAC
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ phân cấp quyền theo cấu trúc cây (Hierarchical Role Authority: ADMIN > AUDITOR > USER)
 * ==============================================================================
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}
