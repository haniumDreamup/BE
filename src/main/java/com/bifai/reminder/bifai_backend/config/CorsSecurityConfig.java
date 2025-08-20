package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 보안 설정
 * 허용된 도메인만 접근 가능하도록 제한
 */
@Configuration
@Slf4j
public class CorsSecurityConfig {
  
  @Value("${app.cors.allowed-origins:http://localhost:3000}")
  private String allowedOrigins;
  
  /**
   * CORS 설정 소스
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // 허용된 오리진만 설정 (프로덕션에서는 특정 도메인만)
    List<String> origins = Arrays.asList(allowedOrigins.split(","));
    configuration.setAllowedOrigins(origins);
    
    // 허용된 HTTP 메소드
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    
    // 허용된 헤더
    configuration.setAllowedHeaders(Arrays.asList(
      "Authorization",
      "Content-Type",
      "X-Requested-With",
      "Accept",
      "Origin",
      "Access-Control-Request-Method",
      "Access-Control-Request-Headers"
    ));
    
    // 노출할 헤더
    configuration.setExposedHeaders(Arrays.asList(
      "Access-Control-Allow-Origin",
      "Access-Control-Allow-Credentials",
      "Authorization"
    ));
    
    // 인증 정보 허용
    configuration.setAllowCredentials(true);
    
    // Preflight 캐시 시간 (1시간)
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    
    log.info("CORS 설정 완료 - 허용된 오리진: {}", origins);
    
    return source;
  }
}