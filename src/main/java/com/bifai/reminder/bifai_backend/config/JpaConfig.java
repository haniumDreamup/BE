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
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
public class JpaConfig {
    // JPA Auditing은 JpaAuditingConfig에서 처리
}