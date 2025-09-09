package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security 비활성화 설정
 */
@Configuration
@EnableWebSecurity
@Profile("no-security")  // 보안을 비활성화하고 싶을 때만 활성화
@org.springframework.core.annotation.Order(2)  // 낮은 우선순위
public class NoSecurityConfig {
  
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll()
      );
    
    return http.build();
  }
  
  @Bean
  public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
  }
  
  @Bean
  public org.springframework.security.authentication.AuthenticationManager authenticationManager(
      org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}