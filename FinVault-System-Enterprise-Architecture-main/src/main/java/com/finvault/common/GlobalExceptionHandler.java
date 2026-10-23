package com.finvault.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * GlobalExceptionHandler: Xử lý ngoại lệ tập trung cho cả REST API và Web MVC Views
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Sentry / OpenTelemetry Tracing để cảnh báo lỗi thời gian thực lên Slack/Telegram
 * ==============================================================================
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        return uri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains("application/json"));
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Ngoại lệ nghiệp vụ tài chính [{}]: {}", ex.getErrorCode(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(ex.getHttpStatus())
                    .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("status", ex.getHttpStatus().value());
        return mav;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Dữ liệu đầu vào không hợp lệ: {}", errors);
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_FAILED", "Dữ liệu đầu vào không hợp lệ", errors));
    }

    /**
     * Lưu ý: Handler này chỉ bắt AccessDeniedException
     * khi exception được throw từ bên trong Controller layer (ví dụ: @PreAuthorize failure).
     * Đối với request bị chặn tại Security Filter Chain level, cần cấu hình
     * AccessDeniedHandler trong SecurityConfig.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Từ chối truy cập cho URI [{}]: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "Bạn không có quyền thực hiện hành động này"));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "ACCESS_DENIED");
        mav.addObject("errorMessage", "Bạn không có quyền hạn truy cập tài nguyên này.");
        mav.addObject("status", HttpStatus.FORBIDDEN.value());
        return mav;
    }

    /**
     * Lưu ý: Handler này chỉ bắt AuthenticationException
     * khi exception được throw từ bên trong Controller layer (ví dụ: @PreAuthorize failure).
     * Đối với request bị chặn tại Security Filter Chain level, cần cấu hình
     * AuthenticationEntryPoint trong SecurityConfig.
     */
    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Lỗi xác thực người dùng: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Thông tin xác thực không chính xác"));
        }
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác.");
        return mav;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public Object handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Không tìm thấy thực thể: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "NOT_FOUND");
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("status", HttpStatus.NOT_FOUND.value());
        return mav;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingException(OptimisticLockingFailureException ex) {
        log.error("Xung đột đồng thời (Race Condition) khi cập nhật dữ liệu: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONCURRENCY_CONFLICT", "Dữ liệu đã bị thay đổi bởi giao dịch khác trong cùng thời điểm. Vui lòng tải lại và thử lại."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("Dung lượng tệp vượt quá giới hạn cho phép: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("FILE_TOO_LARGE", "Dung lượng tệp vượt quá giới hạn 10MB cho phép."));
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Lỗi hệ thống không lường trước tại URI [{}]: ", request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi nội bộ trên máy chủ: " + ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorCode", "INTERNAL_SERVER_ERROR");
        mav.addObject("errorMessage", "Đã xảy ra lỗi hệ thống nghiêm trọng. Vui lòng liên hệ quản trị viên.");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return mav;
    }
}
