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
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
@Profile("!test")
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
   * Long 타입으로 사용자 ID를 반환하여 BaseEntity의 createdBy, updatedBy 필드와 호환
   */
  private class SpringSecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
      try {
        // JwtAuthUtils에서 현재 사용자 ID 추출
        Long userId = jwtAuthUtils.getCurrentUserId();
        if (userId != null && userId > 0) {
          log.debug("JPA Auditing - 현재 인증된 사용자 ID: {}", userId);
          return Optional.of(userId);
        }

        // 인증되지 않은 요청의 경우 시스템 사용자 ID로 설정
        log.debug("JPA Auditing - 인증되지 않은 사용자, 시스템 사용자(-1L)로 설정");
        return Optional.of(-1L);

      } catch (Exception e) {
        // 예외 발생 시 시스템 사용자 ID로 fallback
        log.warn("JPA Auditing - 사용자 정보 추출 실패, 시스템 사용자로 fallback: {}", e.getMessage());
        return Optional.of(-1L);
      }
    }
  }
}