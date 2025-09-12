package com.bifai.reminder.bifai_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 테스트 프로파일 전용 설정
 * 운영환경에서 @Profile("!test")로 제외된 빈들을 테스트용으로 제공
 */
@Configuration
@Profile("test")
@EnableCaching
public class TestProfileConfig {

  /**
   * 테스트용 캐시 매니저
   * Redis 대신 메모리 기반 캐시 사용
   */
  @Bean
  @Primary
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(
        "users", "medications", "schedules", "emergencies", 
        "geofences", "dashboard", "statistics", "notifications",
        "accessibility", "voiceGuidance", "imageAnalysis",
        "poseData", "deviceStatus", "guardianRelations",
        "userProfiles", "systemHealth"
    );
  }

  /**
   * 테스트용 패스워드 인코더
   */
  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 테스트용 인증 매니저
   */
  @Bean
  @Primary
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}