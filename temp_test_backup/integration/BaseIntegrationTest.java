package com.bifai.reminder.bifai_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * 통합 테스트 기본 클래스
 * 모든 통합 테스트에서 공통으로 사용할 설정과 유틸리티 제공
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
public abstract class BaseIntegrationTest {

  @LocalServerPort
  protected int port;

  @Autowired
  protected TestRestTemplate restTemplate;

  @Autowired
  protected WebApplicationContext webApplicationContext;

  @Autowired
  protected ObjectMapper objectMapper;

  protected MockMvc mockMvc;

  @BeforeEach
  void baseSetUp() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(webApplicationContext)
        .apply(springSecurity())
        .build();
  }

  /**
   * 기본 URL 생성
   */
  protected String createURL(String uri) {
    return "http://localhost:" + port + uri;
  }

  /**
   * JWT 토큰으로 인증된 헤더 생성
   */
  protected HttpHeaders createAuthenticatedHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    return headers;
  }

  /**
   * JSON 헤더 생성
   */
  protected HttpHeaders createJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * GET 요청 엔티티 생성
   */
  protected <T> HttpEntity<T> createGetEntity(String token) {
    return new HttpEntity<>(createAuthenticatedHeaders(token));
  }

  /**
   * POST/PUT 요청 엔티티 생성
   */
  protected <T> HttpEntity<T> createRequestEntity(T body, String token) {
    return new HttpEntity<>(body, createAuthenticatedHeaders(token));
  }

  /**
   * 인증 없는 POST/PUT 요청 엔티티 생성
   */
  protected <T> HttpEntity<T> createRequestEntity(T body) {
    return new HttpEntity<>(body, createJsonHeaders());
  }

  /**
   * 테스트용 JWT 토큰 생성 (실제 로그인 없이)
   */
  protected String generateTestToken() {
    // TODO: JWT 토큰 생성 로직 구현
    return "test-jwt-token";
  }

  /**
   * 응답 상태 코드 검증 헬퍼
   */
  protected void assertHttpStatus(ResponseEntity<?> response, HttpStatus expectedStatus) {
    org.junit.jupiter.api.Assertions.assertEquals(
        expectedStatus,
        response.getStatusCode(),
        "HTTP 상태 코드가 예상과 다릅니다"
    );
  }

  /**
   * 성공 응답 구조 검증 헬퍼
   */
  protected void assertSuccessResponse(ResponseEntity<String> response) {
    assertHttpStatus(response, HttpStatus.OK);
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getBody().contains("\"success\":true"),
        "성공 응답 형식이 올바르지 않습니다"
    );
  }

  /**
   * 에러 응답 구조 검증 헬퍼
   */
  protected void assertErrorResponse(ResponseEntity<String> response, HttpStatus expectedStatus) {
    assertHttpStatus(response, expectedStatus);
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getBody().contains("\"success\":false"),
        "에러 응답 형식이 올바르지 않습니다"
    );
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getBody().contains("\"error\""),
        "에러 정보가 포함되어야 합니다"
    );
  }
}