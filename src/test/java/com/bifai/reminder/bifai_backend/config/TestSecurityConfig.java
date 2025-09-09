package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthenticationFilter;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * 보안 관련 테스트 설정
 * Spring Boot 테스트 베스트 프랙티스에 따라 보안 컴포넌트 제공
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test") 
public class TestSecurityConfig {
  
  @MockBean
  private BifUserDetailsService userDetailsService;
  
  @MockBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;
  
  @MockBean
  private RefreshTokenService refreshTokenService;
  
  @MockBean
  private RedisCacheService redisCacheService;
  
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
  
  @Bean
  public JwtTokenProvider jwtTokenProvider() {
    return new JwtTokenProvider(
      "test-jwt-secret-key-for-bifai-backend-application-test-environment-only-with-minimum-64-bytes-requirement",
      900000L,
      604800000L
    );
  }
  
  /**
   * AuthenticationManager 빈 제공
   * Spring Security 테스트에서 필수적인 컴포넌트
   */
  @Bean
  @Primary
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
  
  
  /**
   * ObjectPostProcessor 빈 제공
   * Spring Security 내부에서 필요
   */
  @Bean
  public org.springframework.security.config.ObjectPostProcessor<Object> objectPostProcessor() {
    return new org.springframework.security.config.ObjectPostProcessor<Object>() {
      @Override
      public <T> T postProcess(T object) {
        return object;
      }
    };
  }
  
  /**
   * SecurityFilterChain 빈 제공
   * 테스트 환경용 기본 보안 설정
   */
  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        );
    
    return http.build();
  }
}