package com.bifai.reminder.bifai_backend.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 보호자 권한 검증 어노테이션
 * 메서드 레벨에서 보호자 권한을 확인하여 접근을 제어
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardianCheck {
    
    /**
     * 검증할 사용자 ID 파라미터명
     * 기본값: "userId"
     */
    String userIdParam() default "userId";
    
    /**
     * 본인도 접근 가능한지 여부
     * true: 본인 또는 보호자 접근 가능
     * false: 보호자만 접근 가능
     */
    boolean allowSelf() default true;
    
    /**
     * 주 보호자만 접근 가능한지 여부
     * true: 주 보호자만 접근 가능
     * false: 모든 승인된 보호자 접근 가능
     */
    boolean primaryOnly() default false;
    
    /**
     * 특정 권한이 필요한지
     * 예: canModifySettings, canViewLocation
     */
    String requiredPermission() default "";
}