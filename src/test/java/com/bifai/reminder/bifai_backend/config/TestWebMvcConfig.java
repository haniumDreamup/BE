package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @WebMvcTest용 Security 설정
 * Controller 테스트에서 인증/인가를 간단하게 처리
 */
@TestConfiguration
@EnableWebSecurity
public class TestWebMvcConfig {

  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> 
            auth.anyRequest().permitAll())
        .build();
  }
}