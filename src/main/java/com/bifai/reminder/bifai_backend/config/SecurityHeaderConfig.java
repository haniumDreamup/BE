package com.bifai.reminder.bifai_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 보안 헤더 설정
 * OWASP 권장 사항 적용
 */
@Configuration
@Slf4j
public class SecurityHeaderConfig {
  
  /**
   * 보안 헤더 필터
   */
  @Component
  public static class SecurityHeaderFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      
      // XSS 방지
      httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
      httpResponse.setHeader("X-Content-Type-Options", "nosniff");
      
      // Clickjacking 방지
      httpResponse.setHeader("X-Frame-Options", "DENY");
      
      // HTTPS 강제
      httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
      
      // Content Security Policy
      httpResponse.setHeader("Content-Security-Policy", 
        "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self' data:; " +
        "connect-src 'self' https://api.openai.com https://maps.googleapis.com");
      
      // Referrer Policy
      httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
      
      // Permissions Policy (구 Feature Policy)
      httpResponse.setHeader("Permissions-Policy", 
        "geolocation=(self), microphone=(), camera=()");
      
      chain.doFilter(request, response);
    }
  }
  
  /**
   * Spring Security 보안 설정
   */
  @Bean
  public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
      // XSS 보호
      .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
      
      // Content Type Options
      .contentTypeOptions(contentType -> {})
      
      // Frame Options
      .frameOptions(frame -> frame.deny())
      
      // HSTS
      .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
      
      // CSP
      .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
      
      // Cache Control
      .cacheControl(cache -> {})
      
      // 커스텀 헤더
      .addHeaderWriter(new StaticHeadersWriter("X-Custom-Security", "BIF-AI"))
    );
    
    return http.build();
  }
}