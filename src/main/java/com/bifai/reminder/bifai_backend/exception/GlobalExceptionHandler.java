package com.bifai.reminder.bifai_backend.exception;

import com.bifai.reminder.bifai_backend.dto.ProblemDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errors);
        
        ProblemDetail problem = ProblemDetail.forValidation(errors);
        
        return ResponseEntity.badRequest().body(problem);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> cv.getMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Constraint violation: {}", errors);
        
        ProblemDetail problem = ProblemDetail.forValidation(errors);
        
        return ResponseEntity.badRequest().body(problem);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forNotFound("요청하신 정보");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forAuthentication(ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * BIF 예외 처리
     */
    @ExceptionHandler(BifException.class)
    public ResponseEntity<ProblemDetail> handleBifException(BifException ex) {
        log.warn("BIF exception occurred: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            ex.getClass().getSimpleName().replace("Exception", ""),
            ex.getUserFriendlyMessage(),
            ex.getActionGuide(),
            400
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * 긴급 상황 예외 처리 - 보호자에게 알림 전송
     */
    @ExceptionHandler(EmergencyException.class)
    public ResponseEntity<ProblemDetail> handleEmergencyException(EmergencyException ex) {
        log.error("EMERGENCY: User {} - {} at location ({}, {})", 
            ex.getUserId(), ex.getType().getDescription(), 
            ex.getLatitude(), ex.getLongitude());
        
        // 보호자에게 긴급 알림 이벤트 발행
        if (eventPublisher != null) {
            // EmergencyEvent 발행 (추후 구현)
            // eventPublisher.publishEvent(new EmergencyEvent(ex));
        }
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "긴급 상황",
            ex.getUserFriendlyMessage(),
            ex.getActionGuide(),
            500
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
    
    /**
     * 안전 규칙 위반 예외 처리
     */
    @ExceptionHandler(SafetyViolationException.class)
    public ResponseEntity<ProblemDetail> handleSafetyViolation(SafetyViolationException ex) {
        log.warn("Safety violation: User {} - {}", ex.getUserId(), ex.getType().getDescription());
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "안전 규칙 위반",
            ex.getUserFriendlyMessage(),
            ex.getActionGuide(),
            403
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
    
    /**
     * 인지 과부하 예외 처리
     */
    @ExceptionHandler(CognitiveOverloadException.class)
    public ResponseEntity<ProblemDetail> handleCognitiveOverload(CognitiveOverloadException ex) {
        log.info("Cognitive overload detected: {} items in {}", 
            ex.getInformationCount(), ex.getContext());
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "정보가 너무 많아요",
            ex.getUserFriendlyMessage(),
            ex.getActionGuide(),
            400
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
    
    /**
     * 디바이스 연결 예외 처리
     */
    @ExceptionHandler(DeviceConnectionException.class)
    public ResponseEntity<ProblemDetail> handleDeviceConnection(DeviceConnectionException ex) {
        log.warn("Device connection problem: {} - {}", 
            ex.getDeviceName(), ex.getProblem().getDescription());
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "기기 연결 문제",
            ex.getUserFriendlyMessage(),
            ex.getActionGuide(),
            503
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
    
    /**
     * 중복 리소스 예외 처리
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "중복 등록",
            "이미 등록된 정보예요",
            "다른 이름이나 정보를 사용해 주세요",
            409
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
    
    /**
     * JWT 토큰 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ProblemDetail> handleExpiredJwtException(ExpiredJwtException ex) {
        log.warn("JWT 토큰 만료: {}", ex.getMessage());
        log.debug("JWT Token expired details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("로그인 시간이 만료되었습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * JWT 토큰 형식 오류 예외 처리
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJwtException(MalformedJwtException ex) {
        log.warn("JWT 토큰 형식 오류: {}", ex.getMessage());
        log.debug("Malformed JWT Token details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("로그인 정보가 올바르지 않습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * JWT 토큰 서명 오류 예외 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleJwtSecurityException(SecurityException ex) {
        log.warn("JWT 토큰 서명 오류: {}", ex.getMessage());
        log.debug("JWT Token signature error details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("로그인 정보가 올바르지 않습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * JWT 토큰 지원되지 않음 예외 처리
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedJwtException(UnsupportedJwtException ex) {
        log.warn("지원되지 않는 JWT 토큰: {}", ex.getMessage());
        log.debug("Unsupported JWT Token details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("로그인 정보가 올바르지 않습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * Spring Security 인증 실패 예외 처리
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("인증 실패: {}", ex.getMessage());
        log.debug("Bad credentials details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("아이디 또는 비밀번호가 올바르지 않습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * 계정 비활성화 예외 처리
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ProblemDetail> handleDisabledException(DisabledException ex) {
        log.warn("비활성화된 계정 접근 시도: {}", ex.getMessage());
        log.debug("Disabled account access attempt", ex);
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "계정 비활성화",
            "사용할 수 없는 계정입니다",
            "보호자에게 문의해 주세요",
            401
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * 계정 잠김 예외 처리
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ProblemDetail> handleLockedException(LockedException ex) {
        log.warn("잠긴 계정 접근 시도: {}", ex.getMessage());
        log.debug("Locked account access attempt", ex);
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "계정 잠김",
            "잠긴 계정입니다",
            "보호자에게 문의해 주세요",
            401
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * Spring Security 인증 예외 처리 (포괄적)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex) {
        log.warn("인증 예외 발생: {}", ex.getMessage());
        log.debug("Authentication exception details", ex);
        
        ProblemDetail problem = ProblemDetail.forAuthentication("로그인에 실패했습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
    
    /**
     * Spring Security 접근 권한 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("접근 권한 없음: {}", ex.getMessage());
        log.debug("Access denied details", ex);
        
        ProblemDetail problem = ProblemDetail.forBifUser(
            "권한 없음",
            "이 기능을 사용할 권한이 없습니다",
            "보호자에게 문의하거나 다른 기능을 이용해 주세요",
            403
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
    
    /**
     * Missing Request Parameter 예외 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("필수 파라미터 누락: {}", ex.getMessage());
        log.debug("MissingServletRequestParameterException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "필수 정보 누락",
            String.format("필수 정보가 없습니다: %s", ex.getParameterName()),
            "모든 필수 정보를 입력하고 다시 시도해 주세요",
            400
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * HTTP Method Not Supported 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("지원하지 않는 HTTP 메서드: {}", ex.getMessage());
        log.debug("HttpRequestMethodNotSupportedException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "잘못된 요청 방법",
            "지원하지 않는 요청 방법입니다",
            "올바른 요청 방법을 사용해 주세요",
            405
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    /**
     * No Handler Found 예외 처리 (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("경로를 찾을 수 없음: {}", ex.getMessage());
        log.debug("NoHandlerFoundException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "페이지를 찾을 수 없음",
            "요청하신 페이지를 찾을 수 없습니다",
            "올바른 주소로 다시 시도해 주세요",
            404
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * HTTP Message Not Readable 예외 처리 (잘못된 JSON 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("읽을 수 없는 요청 내용: {}", ex.getMessage());
        log.debug("HttpMessageNotReadableException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "잘못된 요청 내용",
            "요청 내용을 읽을 수 없습니다",
            "올바른 형식으로 다시 시도해 주세요",
            400
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 날짜 파싱 오류 예외 처리
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ProblemDetail> handleDateTimeParseException(DateTimeParseException ex) {
        log.warn("날짜 형식 오류: {}", ex.getMessage());
        log.debug("DateTimeParseException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "잘못된 날짜 형식",
            "날짜 형식이 올바르지 않습니다",
            "YYYY-MM-DD 형식으로 입력해 주세요 (예: 2024-01-15)",
            400
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 메서드 인수 타입 불일치 예외 처리 (주로 날짜/숫자 파싱 오류)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("파라미터 타입 불일치: {}", ex.getMessage());
        log.debug("MethodArgumentTypeMismatchException details", ex);

        String errorMessage = "입력 값이 올바르지 않습니다";
        String actionGuide = "올바른 형식으로 다시 입력해 주세요";

        // 날짜 관련 오류인 경우 더 구체적인 메시지 제공
        if (ex.getRequiredType() != null && ex.getRequiredType().getSimpleName().contains("LocalDate")) {
            errorMessage = "날짜 형식이 올바르지 않습니다";
            actionGuide = "YYYY-MM-DD 형식으로 입력해 주세요 (예: 2024-01-15)";
        }

        ProblemDetail problem = ProblemDetail.forBifUser(
            "잘못된 입력 형식",
            errorMessage,
            actionGuide,
            400
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * IllegalArgumentException 예외 처리 (개발자 친화적 로깅 추가)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("잘못된 요청 파라미터: {}", ex.getMessage());
        log.debug("IllegalArgumentException details", ex);

        ProblemDetail problem = ProblemDetail.forBifUser(
            "잘못된 요청",
            ex.getMessage(), // 개발자가 작성한 메시지 그대로 사용
            "입력 정보를 확인하고 다시 시도해 주세요",
            400
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        // 개발자를 위한 상세 로깅
        log.error("Unexpected error occurred - Type: {}, Message: {}", 
                ex.getClass().getSimpleName(), ex.getMessage());
        log.error("Full stack trace:", ex);
        
        // 특정 예외 타입별 추가 처리
        if (ex instanceof ClassCastException) {
            log.error("ClassCastException detected - likely JWT authentication issue");
        }
        
        ProblemDetail problem = ProblemDetail.forServerError();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}