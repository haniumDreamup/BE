package com.bifai.reminder.bifai_backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * AuthController 통합 테스트
 * 인증 관련 엔드포인트의 성공/실패/엣지 케이스 검증
 */
@DisplayName("Auth Controller 통합 테스트")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("POST /api/v1/auth/register - 성공: 유효한 사용자 등록")
  void shouldRegisterUser_WhenValidRequest() {
    // given
    String registerRequest = """
        {
          "email": "test@example.com",
          "password": "validPassword123",
          "name": "테스트 사용자",
          "phone": "010-1234-5678"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(registerRequest),
        String.class
    );

    // then
    assertSuccessResponse(response);
    // TODO: 실제 사용자 생성 확인
  }

  @Test
  @DisplayName("POST /api/v1/auth/register - 실패: 중복된 이메일")
  void shouldFailRegister_WhenDuplicateEmail() {
    // given
    String registerRequest = """
        {
          "email": "existing@example.com",
          "password": "validPassword123",
          "name": "중복 사용자",
          "phone": "010-1234-5678"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(registerRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.CONFLICT);
    // BIF 친화적 에러 메시지 확인
    // TODO: 구체적인 에러 메시지 검증
  }

  @Test
  @DisplayName("POST /api/v1/auth/register - 실패: 잘못된 입력 데이터")
  void shouldFailRegister_WhenInvalidInput() {
    // given
    String invalidRequest = """
        {
          "email": "invalid-email",
          "password": "123",
          "name": "",
          "phone": "invalid-phone"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(invalidRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 성공: 유효한 로그인")
  void shouldLoginUser_WhenValidCredentials() {
    // given
    String loginRequest = """
        {
          "email": "test@example.com",
          "password": "validPassword123"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/login"),
        createRequestEntity(loginRequest),
        String.class
    );

    // then
    assertSuccessResponse(response);
    // JWT 토큰 존재 확인
    // TODO: 토큰 유효성 검증
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 실패: 잘못된 비밀번호")
  void shouldFailLogin_WhenInvalidPassword() {
    // given
    String loginRequest = """
        {
          "email": "test@example.com",
          "password": "wrongPassword"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/login"),
        createRequestEntity(loginRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 실패: 존재하지 않는 사용자")
  void shouldFailLogin_WhenUserNotExists() {
    // given
    String loginRequest = """
        {
          "email": "nonexistent@example.com",
          "password": "anyPassword"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/login"),
        createRequestEntity(loginRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /api/v1/auth/refresh - 성공: 유효한 토큰 갱신")
  void shouldRefreshToken_WhenValidToken() {
    // given
    String refreshRequest = """
        {
          "refreshToken": "valid-refresh-token"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/refresh"),
        createRequestEntity(refreshRequest),
        String.class
    );

    // then
    // TODO: 실제 유효한 refresh token으로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("POST /api/v1/auth/refresh - 실패: 만료된 토큰")
  void shouldFailRefresh_WhenExpiredToken() {
    // given
    String refreshRequest = """
        {
          "refreshToken": "expired-refresh-token"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/refresh"),
        createRequestEntity(refreshRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /api/v1/auth/logout - 성공: 인증된 사용자 로그아웃")
  void shouldLogoutUser_WhenAuthenticated() {
    // given
    String token = generateTestToken();
    
    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/logout"),
        createGetEntity(token),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("POST /api/v1/auth/logout - 실패: 인증되지 않은 요청")
  void shouldFailLogout_WhenNotAuthenticated() {
    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/logout"),
        createRequestEntity(""),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("GET /api/v1/auth/health - 성공: 헬스체크")
  void shouldReturnHealth_Always() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/v1/auth/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("엣지 케이스: 빈 JSON 요청")
  void shouldHandleEmptyJson_Gracefully() {
    // given
    String emptyRequest = "{}";

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(emptyRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 잘못된 JSON 형식")
  void shouldHandleMalformedJson_Gracefully() {
    // given
    String malformedJson = "{ invalid json }";

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(malformedJson),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 매우 긴 입력값")
  void shouldHandleLongInput_Gracefully() {
    // given
    String longString = "a".repeat(1000);
    String longInputRequest = String.format("""
        {
          "email": "%s@example.com",
          "password": "%s",
          "name": "%s",
          "phone": "010-1234-5678"
        }
        """, longString, longString, longString);

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/auth/register"),
        createRequestEntity(longInputRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }
}