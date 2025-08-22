package com.bifai.reminder.bifai_backend.security;

import com.bifai.reminder.bifai_backend.config.SecurityHeaderConfig;
import com.bifai.reminder.bifai_backend.config.InputValidationConfig;
import com.bifai.reminder.bifai_backend.config.RateLimitingConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 보안 설정 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
  "app.cors.allowed-origins=http://localhost:3000",
  "spring.security.enabled=true"
})
@DisplayName("보안 설정 테스트")
class SecurityConfigTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @MockBean
  private RateLimiterRegistry rateLimiterRegistry;
  
  @BeforeEach
  void setUp() {
    // Rate Limiter 설정
  }
  
  @Test
  @DisplayName("보안 헤더가 올바르게 설정되는지 확인")
  void testSecurityHeaders() throws Exception {
    mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(header().exists("X-Content-Type-Options"))
      .andExpect(header().string("X-Content-Type-Options", "nosniff"))
      .andExpect(header().exists("X-Frame-Options"))
      .andExpect(header().string("X-Frame-Options", "DENY"))
      .andExpect(header().exists("X-XSS-Protection"))
      .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
  }
  
  @Test
  @DisplayName("CORS 설정이 올바르게 작동하는지 확인")
  void testCorsConfiguration() throws Exception {
    // 허용된 오리진에서의 요청
    mockMvc.perform(options("/api/users")
        .header("Origin", "http://localhost:3000")
        .header("Access-Control-Request-Method", "GET"))
      .andExpect(status().isOk())
      .andExpect(header().exists("Access-Control-Allow-Origin"))
      .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    
    // 허용되지 않은 오리진에서의 요청
    mockMvc.perform(options("/api/users")
        .header("Origin", "http://malicious.com")
        .header("Access-Control-Request-Method", "GET"))
      .andExpect(status().isForbidden());
  }
  
  @Test
  @DisplayName("SQL Injection 패턴이 차단되는지 확인")
  void testSqlInjectionPrevention() throws Exception {
    // SQL Injection 시도
    mockMvc.perform(get("/api/users")
        .param("name", "admin'; DROP TABLE users; --"))
      .andExpect(status().isBadRequest());
    
    mockMvc.perform(get("/api/users")
        .param("id", "1 OR 1=1"))
      .andExpect(status().isBadRequest());
    
    // 정상적인 요청
    mockMvc.perform(get("/api/users")
        .param("name", "홍길동"))
      .andExpect(status().isOk());
  }
  
  @Test
  @DisplayName("XSS 공격 패턴이 차단되는지 확인")
  void testXssPrevention() throws Exception {
    // XSS 시도
    mockMvc.perform(post("/api/comments")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"text\":\"<script>alert('XSS')</script>\"}"))
      .andExpect(status().isBadRequest());
    
    mockMvc.perform(post("/api/comments")
        .param("text", "<img src=x onerror=alert('XSS')>"))
      .andExpect(status().isBadRequest());
    
    // 정상적인 요청
    mockMvc.perform(post("/api/comments")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"text\":\"안녕하세요\"}"))
      .andExpect(status().isOk());
  }
  
  @Test
  @DisplayName("Path Traversal 공격이 차단되는지 확인")
  void testPathTraversalPrevention() throws Exception {
    // Path Traversal 시도
    mockMvc.perform(get("/api/files/../../../etc/passwd"))
      .andExpect(status().isBadRequest());
    
    mockMvc.perform(get("/api/files")
        .param("path", "../../sensitive/data"))
      .andExpect(status().isBadRequest());
  }
  
  @Test
  @DisplayName("Rate Limiting이 작동하는지 확인")
  void testRateLimiting() throws Exception {
    // 연속 요청 시뮬레이션
    for (int i = 0; i < 50; i++) {
      MvcResult result = mockMvc.perform(get("/api/users"))
        .andReturn();
      
      if (i < 45) {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
      }
    }
    
    // Rate limit 초과 시
    mockMvc.perform(get("/api/users"))
      .andExpect(status().isTooManyRequests())
      .andExpect(header().exists("X-RateLimit-Retry-After"));
  }
  
  @Test
  @DisplayName("CSRF 보호가 활성화되어 있는지 확인")
  void testCsrfProtection() throws Exception {
    // CSRF 토큰 없이 POST 요청
    mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"test\"}"))
      .andExpect(status().isForbidden());
  }
  
  @Test
  @DisplayName("Content Security Policy가 설정되어 있는지 확인")
  void testContentSecurityPolicy() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(header().exists("Content-Security-Policy"))
      .andReturn();
    
    String csp = result.getResponse().getHeader("Content-Security-Policy");
    assertThat(csp).contains("default-src 'self'");
    assertThat(csp).contains("script-src 'self'");
  }
  
  @Test
  @DisplayName("HSTS 헤더가 설정되어 있는지 확인")
  void testHstsHeader() throws Exception {
    mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(header().exists("Strict-Transport-Security"))
      .andExpect(header().string("Strict-Transport-Security", 
        containsString("max-age=31536000")));
  }
}