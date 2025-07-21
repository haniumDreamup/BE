package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
@EntityScan(basePackages = "com.bifai.reminder.bifai_backend.entity")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        // 현재는 기본값 반환, 나중에 Security Context에서 사용자 정보 가져오도록 수정
        return () -> Optional.of("system");
    }
}