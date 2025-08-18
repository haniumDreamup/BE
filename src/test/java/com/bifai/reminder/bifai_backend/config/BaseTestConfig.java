package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 기본 테스트 설정 어노테이션
 * 모든 통합 테스트에서 사용
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Import({TestAutoConfiguration.class, TestConfig.class})
@TestPropertySource(properties = {
    // Redis Mock 사용
    "redis.mock.enabled=true",
    
    // H2 데이터베이스
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    
    // Flyway 비활성화
    "spring.flyway.enabled=false",
    
    // Security 비활성화
    "spring.security.enabled=false",
    
    // JWT
    "app.jwt.secret=test-jwt-secret-key-for-bifai-backend-application-test-environment-only-with-minimum-64-bytes-requirement",
    "app.jwt.access-token-expiration-ms=900000",
    "app.jwt.refresh-token-expiration-ms=604800000",
    
    // FCM 비활성화
    "fcm.enabled=false",
    
    // AI 비활성화
    "spring.ai.openai.api-key=test-key",
    
    // 로깅 최소화
    "logging.level.root=WARN",
    "logging.level.com.bifai.reminder=INFO"
})
public @interface BaseTestConfig {
}