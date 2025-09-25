package com.bifai.reminder.bifai_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 기본 설정
 *
 * JPA Auditing은 JpaAuditingConfig에서 별도로 관리
 */
@Configuration
@EnableTransactionManagement
public class JpaConfig {
    // JPA Repository는 BifaiBackendApplication에서 @EnableJpaRepositories로 활성화
    // JPA Auditing은 JpaAuditingConfig에서 처리
}