package com.bifai.reminder.bifai_backend.exception;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.response.BifApiResponse;
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
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errors);
        
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                "VALIDATION_ERROR",
                "입력하신 정보를 다시 확인해 주세요",
                errors
            )
        );
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> cv.getMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Constraint violation: {}", errors);
        
        return ResponseEntity.badRequest().body(
            ApiResponse.error(
                "CONSTRAINT_VIOLATION",
                "요청 정보가 올바르지 않습니다",
                errors
            )
        );
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse.error(
                "RESOURCE_NOT_FOUND",
                "요청하신 정보를 찾을 수 없습니다",
                "다시 시도해 주시거나, 도움이 필요하시면 보호자에게 문의해 주세요"
            )
        );
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiResponse.error(
                "UNAUTHORIZED",
                "로그인이 필요합니다",
                "다시 로그인해 주세요"
            )
        );
    }
    
    /**
     * BIF 예외 처리
     */
    @ExceptionHandler(BifException.class)
    public ResponseEntity<BifApiResponse<Void>> handleBifException(BifException ex) {
        log.warn("BIF exception occurred: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            BifApiResponse.error(
                ex.getClass().getSimpleName(),
                ex.getUserFriendlyMessage(),
                ex.getActionGuide()
            )
        );
    }
    
    /**
     * 긴급 상황 예외 처리 - 보호자에게 알림 전송
     */
    @ExceptionHandler(EmergencyException.class)
    public ResponseEntity<BifApiResponse<Void>> handleEmergencyException(EmergencyException ex) {
        log.error("EMERGENCY: User {} - {} at location ({}, {})", 
            ex.getUserId(), ex.getType().getDescription(), 
            ex.getLatitude(), ex.getLongitude());
        
        // 보호자에게 긴급 알림 이벤트 발행
        if (eventPublisher != null) {
            // EmergencyEvent 발행 (추후 구현)
            // eventPublisher.publishEvent(new EmergencyEvent(ex));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            BifApiResponse.error(
                "EMERGENCY",
                ex.getUserFriendlyMessage(),
                ex.getActionGuide()
            )
        );
    }
    
    /**
     * 안전 규칙 위반 예외 처리
     */
    @ExceptionHandler(SafetyViolationException.class)
    public ResponseEntity<BifApiResponse<Void>> handleSafetyViolation(SafetyViolationException ex) {
        log.warn("Safety violation: User {} - {}", ex.getUserId(), ex.getType().getDescription());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            BifApiResponse.error(
                "SAFETY_VIOLATION",
                ex.getUserFriendlyMessage(),
                ex.getActionGuide()
            )
        );
    }
    
    /**
     * 인지 과부하 예외 처리
     */
    @ExceptionHandler(CognitiveOverloadException.class)
    public ResponseEntity<BifApiResponse<Void>> handleCognitiveOverload(CognitiveOverloadException ex) {
        log.info("Cognitive overload detected: {} items in {}", 
            ex.getInformationCount(), ex.getContext());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            BifApiResponse.error(
                "TOO_COMPLEX",
                ex.getUserFriendlyMessage(),
                ex.getActionGuide()
            )
        );
    }
    
    /**
     * 디바이스 연결 예외 처리
     */
    @ExceptionHandler(DeviceConnectionException.class)
    public ResponseEntity<BifApiResponse<Void>> handleDeviceConnection(DeviceConnectionException ex) {
        log.warn("Device connection problem: {} - {}", 
            ex.getDeviceName(), ex.getProblem().getDescription());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            BifApiResponse.error(
                "DEVICE_PROBLEM",
                ex.getUserFriendlyMessage(),
                ex.getActionGuide()
            )
        );
    }
    
    /**
     * 중복 리소스 예외 처리
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BifApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            BifApiResponse.error(
                "ALREADY_EXISTS",
                "이미 등록된 정보예요.",
                "다른 이름이나 정보를 사용해 주세요."
            )
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error(
                "INTERNAL_ERROR",
                "일시적인 오류가 발생했습니다",
                "잠시 후 다시 시도해 주세요. 문제가 계속되면 보호자에게 알려주세요"
            )
        );
    }
}