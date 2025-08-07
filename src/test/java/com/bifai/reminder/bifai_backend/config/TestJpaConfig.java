package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

/**
 * 테스트용 JPA 설정
 * H2 데이터베이스에서 엔티티를 제대로 인식하도록 설정
 */
@TestConfiguration
@EntityScan(basePackages = "com.bifai.reminder.bifai_backend.entity")
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
@EnableJpaAuditing
public class TestJpaConfig {
    
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }
}