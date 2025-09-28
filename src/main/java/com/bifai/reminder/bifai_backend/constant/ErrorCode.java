package com.bifai.reminder.bifai_backend.constant;

import org.springframework.http.HttpStatus;

/**
 * 표준화된 에러 코드 정의
 *
 * <p>RFC 7807 Problem Details 표준을 따르며,
 * BIF 사용자를 위한 친화적인 메시지와 함께 제공됩니다.</p>
 *
 * <p>각 에러 코드는 다음을 포함합니다:</p>
 * <ul>
 *   <li>고유한 에러 코드 (예: AUTH_001)</li>
 *   <li>HTTP 상태 코드</li>
 *   <li>사용자 친화적 메시지</li>
 *   <li>해결 방법 제안</li>
 * </ul>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
public enum ErrorCode {

    // === 인증 관련 에러 (AUTH_xxx) ===
    AUTH_001(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
             BifErrorMessages.LOGIN_REQUIRED,
             BifErrorMessages.ACTION_TRY_AGAIN),

    AUTH_002(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
             BifErrorMessages.LOGIN_FAILED,
             BifErrorMessages.ACTION_CHECK_INPUT),

    AUTH_003(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
             BifErrorMessages.TOKEN_EXPIRED,
             BifErrorMessages.ACTION_TRY_AGAIN),

    AUTH_004(HttpStatus.FORBIDDEN, "INSUFFICIENT_PERMISSIONS",
             BifErrorMessages.UNAUTHORIZED,
             BifErrorMessages.ACTION_CONTACT_GUARDIAN),

    AUTH_005(HttpStatus.BAD_REQUEST, "DUPLICATE_EMAIL",
             BifErrorMessages.EMAIL_ALREADY_EXISTS,
             BifErrorMessages.ACTION_CHECK_INPUT),

    AUTH_006(HttpStatus.BAD_REQUEST, "DUPLICATE_USERNAME",
             BifErrorMessages.USERNAME_ALREADY_EXISTS,
             BifErrorMessages.ACTION_CHECK_INPUT),

    // === 입력 검증 에러 (VALIDATION_xxx) ===
    VALIDATION_001(HttpStatus.BAD_REQUEST, "INVALID_INPUT_FORMAT",
                   BifErrorMessages.INVALID_INPUT,
                   BifErrorMessages.ACTION_CHECK_INPUT),

    VALIDATION_002(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD_MISSING",
                   BifErrorMessages.REQUIRED_FIELD_MISSING,
                   BifErrorMessages.ACTION_CHECK_INPUT),

    VALIDATION_003(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_FORMAT",
                   BifErrorMessages.INVALID_EMAIL,
                   BifErrorMessages.ACTION_CHECK_INPUT),

    VALIDATION_004(HttpStatus.BAD_REQUEST, "INVALID_PHONE_FORMAT",
                   BifErrorMessages.INVALID_PHONE,
                   BifErrorMessages.ACTION_CHECK_INPUT),

    // === 리소스 관련 에러 (RESOURCE_xxx) ===
    RESOURCE_001(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                 BifErrorMessages.USER_NOT_FOUND,
                 BifErrorMessages.ACTION_TRY_AGAIN),

    RESOURCE_002(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND",
                 BifErrorMessages.SCHEDULE_NOT_FOUND,
                 BifErrorMessages.ACTION_TRY_AGAIN),

    RESOURCE_003(HttpStatus.NOT_FOUND, "REMINDER_NOT_FOUND",
                 BifErrorMessages.REMINDER_NOT_FOUND,
                 BifErrorMessages.ACTION_TRY_AGAIN),

    RESOURCE_004(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND",
                 BifErrorMessages.DEVICE_NOT_FOUND,
                 BifErrorMessages.ACTION_CONTACT_GUARDIAN),

    // === 시스템 에러 (SYSTEM_xxx) ===
    SYSTEM_001(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
               BifErrorMessages.SERVER_ERROR,
               BifErrorMessages.ACTION_WAIT_AND_RETRY),

    SYSTEM_002(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
               BifErrorMessages.SERVER_ERROR,
               BifErrorMessages.ACTION_WAIT_AND_RETRY),

    SYSTEM_003(HttpStatus.REQUEST_TIMEOUT, "REQUEST_TIMEOUT",
               BifErrorMessages.TIMEOUT_ERROR,
               BifErrorMessages.ACTION_TRY_AGAIN),

    SYSTEM_004(HttpStatus.BAD_GATEWAY, "NETWORK_ERROR",
               BifErrorMessages.NETWORK_ERROR,
               BifErrorMessages.ACTION_CHECK_INTERNET),

    // === 긴급 상황 에러 (EMERGENCY_xxx) ===
    EMERGENCY_001(HttpStatus.SERVICE_UNAVAILABLE, "EMERGENCY_SYSTEM_UNAVAILABLE",
                  "긴급 알림 시스템에 문제가 생겼어요",
                  "즉시 보호자에게 직접 연락하세요"),

    EMERGENCY_002(HttpStatus.BAD_REQUEST, "INVALID_EMERGENCY_CONTACT",
                  "긴급 연락처가 설정되지 않았어요",
                  "설정에서 긴급 연락처를 추가해 주세요"),

    // === 위치 관련 에러 (LOCATION_xxx) ===
    LOCATION_001(HttpStatus.FORBIDDEN, "LOCATION_PERMISSION_DENIED",
                 BifErrorMessages.LOCATION_PERMISSION_REQUIRED,
                 "설정에서 위치 권한을 허용해 주세요"),

    LOCATION_002(HttpStatus.SERVICE_UNAVAILABLE, "LOCATION_SERVICE_UNAVAILABLE",
                 BifErrorMessages.LOCATION_NOT_AVAILABLE,
                 BifErrorMessages.ACTION_TRY_AGAIN),

    // === 기본 에러 ===
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UNKNOWN_ERROR",
                  BifErrorMessages.SERVER_ERROR,
                  BifErrorMessages.ACTION_CONTACT_GUARDIAN);

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
    private final String userAction;

    ErrorCode(HttpStatus httpStatus, String code, String message, String userAction) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.userAction = userAction;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getUserAction() {
        return userAction;
    }

    public int getStatusCode() {
        return httpStatus.value();
    }

    /**
     * HTTP 상태 코드와 메시지로 ErrorCode 찾기
     */
    public static ErrorCode fromHttpStatus(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> AUTH_001;
            case FORBIDDEN -> AUTH_004;
            case NOT_FOUND -> RESOURCE_001;
            case BAD_REQUEST -> VALIDATION_001;
            case REQUEST_TIMEOUT -> SYSTEM_003;
            case SERVICE_UNAVAILABLE -> SYSTEM_002;
            case BAD_GATEWAY -> SYSTEM_004;
            default -> SYSTEM_001;
        };
    }

    /**
     * 예외 타입으로 ErrorCode 찾기
     */
    public static ErrorCode fromException(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();

        return switch (exceptionName) {
            case "UsernameNotFoundException", "BadCredentialsException" -> AUTH_002;
            case "AccessDeniedException" -> AUTH_004;
            case "ResourceNotFoundException" -> RESOURCE_001;
            case "DuplicateResourceException" -> AUTH_005;
            case "IllegalArgumentException" -> VALIDATION_001;
            case "TimeoutException" -> SYSTEM_003;
            case "ConnectException", "UnknownHostException" -> SYSTEM_004;
            default -> SYSTEM_001;
        };
    }
}