package com.finvault.module1_security_rbac.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * Role Entity: Đại diện cho các nhóm quyền trong hệ thống Dynamic RBAC
 * Hỗ trợ 4 nhóm quyền chuẩn:
 * - ROLE_SUPER_ADMIN: Toàn quyền hệ thống & Live Admin Edit Mode
 * - ROLE_SYSTEM_AUDITOR: Giám sát bảo mật, Audit Trail & Data Masking số dư
 * - ROLE_FAMILY_HEAD: Quản lý Quỹ gia đình, hạn mức chi & phê duyệt chi tiêu
 * - ROLE_STANDARD_USER: Quản lý ví và ngân sách cá nhân chuẩn
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ Dynamic Permission Matrix (phân quyền mịn đến từng action: READ, WRITE, APPROVE, EXPORT)
 * ==============================================================================
 */
@Entity
@Table(name = "roles")
public class Role implements GrantedAuthority, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_SYSTEM_AUDITOR = "ROLE_SYSTEM_AUDITOR";
    public static final String ROLE_FAMILY_HEAD = "ROLE_FAMILY_HEAD";
    public static final String ROLE_STANDARD_USER = "ROLE_STANDARD_USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(name = "description", length = 255)
    private String description;

    public Role() {
    }

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAuthority() {
        return roleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(roleName, role.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleName);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
