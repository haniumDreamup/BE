package com.bifai.reminder.bifai_backend.util;

import com.bifai.reminder.bifai_backend.constant.ErrorCode;
import com.bifai.reminder.bifai_backend.exception.BifException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

/**
 * 구조화된 로깅 유틸리티
 *
 * <p>MDC(Mapped Diagnostic Context)를 활용하여 일관된 로깅 형식을 제공합니다.</p>
 * <p>요청 추적, 에러 컨텍스트, 성능 측정 등을 지원합니다.</p>
 *
 * <p>MDC에 포함되는 정보:</p>
 * <ul>
 *   <li>traceId: 요청 추적 ID</li>
 *   <li>userId: 사용자 ID (인증된 경우)</li>
 *   <li>operation: 수행 중인 작업</li>
 *   <li>requestPath: 요청 경로</li>
 *   <li>userAgent: 사용자 에이전트</li>
 * </ul>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class LoggingUtil {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String OPERATION = "operation";
    private static final String REQUEST_PATH = "requestPath";
    private static final String USER_AGENT = "userAgent";
    private static final String ERROR_CODE = "errorCode";
    private static final String EXECUTION_TIME = "executionTime";

    /**
     * 요청 컨텍스트 초기화
     * 컨트롤러 진입 시 호출하여 MDC 설정
     */
    public static void initRequestContext() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID, traceId);

        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            MDC.put(REQUEST_PATH, request.getRequestURI());
            MDC.put(USER_AGENT, request.getHeader("User-Agent"));

            log.info("Request started: {} {}", request.getMethod(), request.getRequestURI());
        } catch (IllegalStateException e) {
            // RequestContextHolder 사용 불가능한 상황 (비웹 컨텍스트)
            log.debug("No web request context available");
        }
    }

    /**
     * 사용자 정보 설정
     * 인증 성공 후 호출
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }

    /**
     * 작업 컨텍스트 설정
     * 주요 비즈니스 로직 시작 시 호출
     */
    public static void setOperation(String operation) {
        MDC.put(OPERATION, operation);
        log.debug("Starting operation: {}", operation);
    }

    /**
     * 에러 컨텍스트 설정
     * 예외 발생 시 호출
     */
    public static void setErrorContext(Exception exception) {
        if (exception instanceof BifException bifException) {
            MDC.put(ERROR_CODE, bifException.getErrorCode().getCode());
        } else {
            ErrorCode errorCode = ErrorCode.fromException(exception);
            MDC.put(ERROR_CODE, errorCode.getCode());
        }
    }

    /**
     * 실행 시간 측정 시작
     */
    public static void startTimer() {
        MDC.put("startTime", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 실행 시간 측정 종료 및 로깅
     */
    public static void endTimer(String operation) {
        String startTimeStr = MDC.get("startTime");
        if (startTimeStr != null) {
            long startTime = Long.parseLong(startTimeStr);
            long executionTime = System.currentTimeMillis() - startTime;
            MDC.put(EXECUTION_TIME, executionTime + "ms");

            if (executionTime > 1000) {
                log.warn("Slow operation detected: {} took {}ms", operation, executionTime);
            } else {
                log.debug("Operation completed: {} took {}ms", operation, executionTime);
            }

            MDC.remove("startTime");
        }
    }

    /**
     * 성공 로깅
     */
    public static void logSuccess(String message, Object... args) {
        log.info("✅ " + message, args);
    }

    /**
     * 에러 로깅 (사용자 친화적)
     */
    public static void logError(String operation, Exception exception) {
        setErrorContext(exception);

        if (exception instanceof BifException bifException) {
            log.error("❌ BIF Error in {}: {} (ErrorCode: {})",
                     operation,
                     bifException.getUserFriendlyMessage(),
                     bifException.getErrorCode().getCode(),
                     exception);
        } else {
            ErrorCode errorCode = ErrorCode.fromException(exception);
            log.error("❌ System Error in {}: {} (ErrorCode: {})",
                     operation,
                     errorCode.getMessage(),
                     errorCode.getCode(),
                     exception);
        }
    }

    /**
     * 비즈니스 로직 성능 로깅
     */
    public static void logPerformance(String operation, long executionTimeMs, Object result) {
        MDC.put(EXECUTION_TIME, executionTimeMs + "ms");

        if (executionTimeMs > 1000) {
            log.warn("⚠️ Slow operation: {} took {}ms", operation, executionTimeMs);
        } else {
            log.info("⚡ Operation completed: {} took {}ms", operation, executionTimeMs);
        }

        // 결과 크기 로깅 (컬렉션인 경우)
        if (result instanceof java.util.Collection<?> collection) {
            log.debug("Result size: {} items", collection.size());
        }
    }

    /**
     * BIF 사용자 행동 로깅
     * 사용자의 주요 행동을 추적하여 UX 개선에 활용
     */
    public static void logUserAction(String action, Object... context) {
        log.info("👤 User action: {} (context: {})", action, context);
    }

    /**
     * 보안 관련 로깅
     * 인증 실패, 권한 부족 등의 보안 이벤트
     */
    public static void logSecurityEvent(String event, String details) {
        log.warn("🔒 Security event: {} - {}", event, details);
    }

    /**
     * 요청 컨텍스트 정리
     * 요청 종료 시 호출하여 MDC 정리
     */
    public static void clearContext() {
        String traceId = MDC.get(TRACE_ID);
        String operation = MDC.get(OPERATION);
        String executionTime = MDC.get(EXECUTION_TIME);

        if (traceId != null) {
            log.debug("Request completed [{}] operation: {}, time: {}",
                     traceId, operation, executionTime);
        }

        MDC.clear();
    }

    /**
     * 현재 trace ID 반환
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * 디버그 정보 로깅 (개발 환경에서만)
     */
    public static void logDebugInfo(String message, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug("🔍 " + message, args);
        }
    }
}