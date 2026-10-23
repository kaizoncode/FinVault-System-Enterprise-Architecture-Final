package com.finvault.module5_admin_audit.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * Auditable: Annotation đánh dấu các phương thức nghiệp vụ quan trọng cần Spring AOP
 * tự động ghi lại nhật ký kiểm toán (Audit Trail)
 * ==============================================================================
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Tên hành động nghiệp vụ (Ví dụ: CREATE_WALLET, UPDATE_BUDGET, ADMIN_CONFIG_CHANGE)
     */
    String action() default "";

    /**
     * Tên phân hệ nghiệp vụ mục tiêu (Ví dụ: SECURITY_IAM, CORE_LEDGER, BUDGET_GOALS, SYSTEM_CONFIG)
     */
    String module() default "SYSTEM";

    /**
     * Tên thực thể liên quan (Ví dụ: wallets, budgets, system_configs)
     */
    String entityName() default "";
}
