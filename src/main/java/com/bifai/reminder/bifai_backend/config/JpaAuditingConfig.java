package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 설정
 * 엔티티의 생성/수정 시간과 생성자/수정자 정보 자동 관리
 */
@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditingConfig {

  private final JwtAuthUtils jwtAuthUtils;

  /**
   * 현재 사용자 정보를 제공하는 AuditorAware 구현체
   */
  @Bean
  public AuditorAware<Long> auditorProvider() {
    return new SpringSecurityAuditorAware();
  }

  /**
   * Spring Security 기반 감사자 제공자
   */
  private class SpringSecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
      try {
        Long userId = jwtAuthUtils.getCurrentUserId();
        if (userId != null) {
          log.debug("JPA Auditing - 현재 사용자 ID: {}", userId);
          return Optional.of(userId);
        }
        
        log.debug("JPA Auditing - 인증되지 않은 사용자, 시스템 사용자로 설정");
        return Optional.of(-1L); // 시스템 사용자 ID
        
      } catch (Exception e) {
        log.warn("JPA Auditing - 사용자 정보 추출 실패, 시스템 사용자로 설정: {}", e.getMessage());
        return Optional.of(-1L); // 시스템 사용자 ID
      }
    }
  }
}