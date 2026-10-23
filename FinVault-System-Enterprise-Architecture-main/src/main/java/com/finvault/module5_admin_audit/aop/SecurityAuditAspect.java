package com.finvault.module5_admin_audit.aop;

import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.service.AuditLogPersistenceService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SecurityAuditAspect: Thành phần Spring AOP ghi nhận tự động vết kiểm toán (Audit Trail)
 *
 * Tính năng AOP nâng cao:
 * - Sử dụng @Around advice để đo lường và theo dõi kết quả thực thi
 * - Tự động trích xuất Client IP (hỗ trợ X-Forwarded-For) và User-Agent từ ServletContext
 * - Lọc và làm sạch dữ liệu nhạy cảm (Sanitization) trước khi lưu vết CLOB vào Database
 * - Đảm bảo khi phát sinh lỗi/ngoại lệ, vết thất bại vẫn được ghi nhận đầy đủ với failure_reason
 *   qua AuditLogPersistenceService (Propagation.REQUIRES_NEW) để không bao giờ bị rollback cuốn trôi.
 * ==============================================================================
 */
@Aspect
@Component
public class SecurityAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditAspect.class);

    private final AuditLogPersistenceService auditLogPersistenceService;

    public SecurityAuditAspect(AuditLogPersistenceService auditLogPersistenceService) {
        this.auditLogPersistenceService = auditLogPersistenceService;
    }

    @Around("@annotation(auditable)")
    public Object auditMethodExecution(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String actorUsername = "ANONYMOUS";
        String actorRole = "GUEST";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actorUsername = auth.getName();
            actorRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
        }

        String action = auditable.action();
        if (action.isBlank()) {
            action = joinPoint.getSignature().getName();
        }

        String module = auditable.module();
        String entityName = auditable.entityName();

        // Lấy thông tin Client Network
        String ipAddress = "127.0.0.1";
        String userAgent = "Internal/Service";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ipAddress = getClientIp(request);
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 255) {
                userAgent = userAgent.substring(0, 255);
            }
        }

        // Tham số đầu vào (loại bỏ trường nhạy cảm)
        String oldValueArgs = sanitizeArguments(joinPoint.getArgs());

        Object result;
        try {
            result = joinPoint.proceed();

            String newValueStr = (result != null) ? result.toString() : "VOID";
            if (newValueStr.length() > 3000) {
                newValueStr = newValueStr.substring(0, 3000) + "... [truncated]";
            }

            AuditLog auditLog = new AuditLog(
                    actorUsername,
                    actorRole,
                    action,
                    module,
                    entityName,
                    null,
                    oldValueArgs,
                    newValueStr,
                    ipAddress,
                    userAgent,
                    "SUCCESS",
                    null
            );
            try {
                auditLogPersistenceService.saveAuditLog(auditLog);
            } catch (Exception ex) {
                log.error("AOP Audit: Lỗi khi lưu vết audit thành công: {}", ex.getMessage());
            }

            log.info("AOP Audit: [{}] đã thực hiện thành công hành động [{}] trên module [{}]",
                    actorUsername, action, module);

            return result;
        } catch (Throwable t) {
            String failureReason = t.getMessage();
            if (failureReason != null && failureReason.length() > 500) {
                failureReason = failureReason.substring(0, 500);
            }

            AuditLog failLog = new AuditLog(
                    actorUsername,
                    actorRole,
                    action,
                    module,
                    entityName,
                    null,
                    oldValueArgs,
                    null,
                    ipAddress,
                    userAgent,
                    "FAILED",
                    failureReason
            );
            try {
                auditLogPersistenceService.saveAuditLog(failLog);
            } catch (Exception ex) {
                log.error("AOP Audit: Lỗi khi lưu vết audit thất bại: {}", ex.getMessage());
            }

            log.warn("AOP Audit: [{}] thực hiện THẤT BẠI hành động [{}]: {}",
                    actorUsername, action, failureReason);

            throw t;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitizeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "NO_ARGS";
        }
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    String str = arg.toString();
                    // Che mặt nạ mật khẩu hoặc token
                    if (str.toLowerCase().contains("password") || str.toLowerCase().contains("secret")) {
                        return "[PROTECTED_CREDENTIALS]";
                    }
                    if (str.length() > 500) {
                        return str.substring(0, 500) + "...";
                    }
                    return str;
                })
                .collect(Collectors.joining("; "));
    }
}
