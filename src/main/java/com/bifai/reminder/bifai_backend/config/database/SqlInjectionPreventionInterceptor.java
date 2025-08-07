package com.bifai.reminder.bifai_backend.config.database;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 인젝션 방지 인터셉터
 * Repository 메서드 호출 시 파라미터 검증
 */
@Slf4j
@Aspect
@Component
public class SqlInjectionPreventionInterceptor {
    
    // SQL 인젝션 위험 패턴
    private static final Set<Pattern> DANGEROUS_PATTERNS = new HashSet<>(Arrays.asList(
        Pattern.compile("(\\s|^)(union|select|insert|update|delete|drop|create|alter|exec|execute)(\\s|$)", 
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(--|/\\*|\\*/|xp_|sp_|0x)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("('\\s*or\\s*'|'\\s*=\\s*')", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(;\\s*(delete|drop|exec|insert|update))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\\\x[0-9a-fA-F]+|\\\\[0-9]+)", Pattern.CASE_INSENSITIVE)
    ));
    
    // 안전한 파라미터 타입
    private static final Set<Class<?>> SAFE_TYPES = new HashSet<>(Arrays.asList(
        Long.class, Integer.class, Double.class, Float.class,
        Boolean.class, Enum.class
    ));
    
    /**
     * Repository 메서드 실행 전 파라미터 검증
     */
    @Before("@within(org.springframework.stereotype.Repository)")
    public void validateParameters(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            
            if (arg == null) {
                continue;
            }
            
            // 문자열 파라미터 검증
            if (arg instanceof String) {
                String strArg = (String) arg;
                validateStringParameter(strArg, methodName, i);
            }
            
            // 컬렉션 내부 문자열 검증
            if (arg instanceof Iterable) {
                validateIterableParameter((Iterable<?>) arg, methodName, i);
            }
        }
    }
    
    /**
     * 문자열 파라미터 검증
     */
    private void validateStringParameter(String value, String methodName, int paramIndex) {
        // 빈 문자열은 통과
        if (value.trim().isEmpty()) {
            return;
        }
        
        // SQL 인젝션 패턴 검사
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                log.error("SQL 인젝션 시도 감지 - 메서드: {}, 파라미터 인덱스: {}, 값: {}", 
                    methodName, paramIndex, maskSensitiveData(value));
                throw new SecurityException("잠재적인 SQL 인젝션이 감지되었습니다");
            }
        }
        
        // 특수한 경우 추가 검증
        if (methodName.toLowerCase().contains("find") || 
            methodName.toLowerCase().contains("search")) {
            validateSearchParameter(value);
        }
    }
    
    /**
     * 검색 파라미터 추가 검증
     */
    private void validateSearchParameter(String value) {
        // 검색에서 허용되지 않는 특수문자
        String[] forbiddenChars = {"<", ">", "&", "\"", "'"};
        
        for (String forbidden : forbiddenChars) {
            if (value.contains(forbidden)) {
                log.warn("검색 파라미터에 특수문자 포함: {}", maskSensitiveData(value));
                // 검색의 경우 특수문자를 제거하거나 이스케이프 처리
            }
        }
    }
    
    /**
     * Iterable 파라미터 검증
     */
    private void validateIterableParameter(Iterable<?> iterable, String methodName, int paramIndex) {
        int itemIndex = 0;
        for (Object item : iterable) {
            if (item instanceof String) {
                validateStringParameter((String) item, 
                    methodName + "[" + paramIndex + "][" + itemIndex + "]", itemIndex);
            }
            itemIndex++;
        }
    }
    
    /**
     * 민감한 데이터 마스킹
     */
    private String maskSensitiveData(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        
        int visibleLength = Math.min(4, value.length() / 4);
        String visible = value.substring(0, visibleLength);
        return visible + "***";
    }
    
    /**
     * 안전한 SQL 파라미터인지 확인
     */
    public static boolean isSafeParameter(Object param) {
        if (param == null) {
            return true;
        }
        
        // 안전한 타입 확인
        if (SAFE_TYPES.contains(param.getClass())) {
            return true;
        }
        
        // Enum 타입 확인
        if (param.getClass().isEnum()) {
            return true;
        }
        
        // 문자열의 경우 추가 검증 필요
        if (param instanceof String) {
            String str = (String) param;
            for (Pattern pattern : DANGEROUS_PATTERNS) {
                if (pattern.matcher(str).find()) {
                    return false;
                }
            }
        }
        
        return true;
    }
}