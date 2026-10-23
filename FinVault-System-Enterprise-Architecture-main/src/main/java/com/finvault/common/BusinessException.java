package com.finvault.common;

import org.springframework.http.HttpStatus;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * BusinessException: Ngoại lệ nghiệp vụ tài chính tùy chỉnh với mã lỗi và trạng thái HTTP
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ đa ngôn ngữ hóa thông điệp lỗi (i18n Localization via MessageSource)
 * ==============================================================================
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
