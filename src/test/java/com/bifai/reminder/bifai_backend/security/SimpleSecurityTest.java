package com.bifai.reminder.bifai_backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 간단한 보안 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("간단한 보안 테스트")
class SimpleSecurityTest {
  
  @Autowired
  private TestRestTemplate restTemplate;
  
  @Test
  @DisplayName("인증되지 않은 요청은 401을 반환한다")
  void testUnauthorizedAccess() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/users", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
  
  @Test
  @DisplayName("헬스체크 엔드포인트는 인증 없이 접근 가능하다")
  void testHealthCheckEndpoint() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/health", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
  
  @Test
  @DisplayName("잘못된 URL은 404를 반환한다")
  void testNotFoundEndpoint() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/nonexistent", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}