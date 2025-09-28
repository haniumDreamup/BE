package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 설정
 * 엔티티의 생성/수정 시간과 생성자/수정자 정보 자동 관리
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class JpaAuditingConfig {

}