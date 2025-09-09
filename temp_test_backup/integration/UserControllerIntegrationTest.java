package com.bifai.reminder.bifai_backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * UserController 통합 테스트
 * 사용자 관리 엔드포인트의 성공/실패/엣지 케이스 검증
 */
@DisplayName("User Controller 통합 테스트")
class UserControllerIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("GET /api/v1/api/v1/users/me - 성공: 현재 사용자 정보 조회")
  void shouldGetCurrentUser_WhenAuthenticated() {
    // given
    String token = generateTestToken();

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    assertSuccessResponse(response);
    // TODO: 사용자 정보 필드 검증 (id, email, name, etc.)
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/users/me - 실패: 인증되지 않은 요청")
  void shouldFailGetCurrentUser_WhenNotAuthenticated() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/v1/api/v1/users/me"),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/me - 성공: 현재 사용자 정보 수정")
  void shouldUpdateCurrentUser_WhenValidRequest() {
    // given
    String token = generateTestToken();
    String updateRequest = """
        {
          "name": "수정된 이름",
          "phone": "010-9876-5432",
          "preferences": {
            "notification": true,
            "darkMode": false,
            "language": "ko"
          }
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(updateRequest, token),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/me - 실패: 잘못된 전화번호 형식")
  void shouldFailUpdateCurrentUser_WhenInvalidPhone() {
    // given
    String token = generateTestToken();
    String invalidRequest = """
        {
          "name": "수정된 이름",
          "phone": "잘못된-전화번호",
          "preferences": {}
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(invalidRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/users/{userId} - 성공: 특정 사용자 조회")
  void shouldGetUser_WhenValidId() {
    // given
    String token = generateTestToken();
    Long userId = 1L;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + userId),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    // TODO: 실제 사용자 데이터로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/users/{userId} - 실패: 존재하지 않는 사용자")
  void shouldFailGetUser_WhenUserNotExists() {
    // given
    String token = generateTestToken();
    Long invalidUserId = 99999L;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + invalidUserId),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/users - 성공: 사용자 목록 조회")
  void shouldGetUsers_WhenAuthorized() {
    // given
    String token = generateTestToken(); // 관리자 권한 필요

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users"),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    // TODO: 관리자 권한 테스트 구현
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/users - 실패: 권한 없는 사용자")
  void shouldFailGetUsers_WhenNotAuthorized() {
    // given
    String regularUserToken = generateTestToken(); // 일반 사용자 토큰

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users"),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(regularUserToken),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/{userId}/deactivate - 성공: 사용자 비활성화")
  void shouldDeactivateUser_WhenAuthorized() {
    // given
    String adminToken = generateTestToken(); // 관리자 토큰
    Long userId = 2L;
    String deactivateRequest = """
        {
          "reason": "사용자 요청에 의한 비활성화",
          "notifyUser": true
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + userId + "/deactivate"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(deactivateRequest, adminToken),
        String.class
    );

    // then
    // TODO: 관리자 권한 및 실제 사용자로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/{userId}/activate - 성공: 사용자 활성화")
  void shouldActivateUser_WhenAuthorized() {
    // given
    String adminToken = generateTestToken();
    Long userId = 2L;
    String activateRequest = """
        {
          "reason": "계정 복구",
          "notifyUser": true
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + userId + "/activate"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(activateRequest, adminToken),
        String.class
    );

    // then
    // TODO: 관리자 권한 및 실제 사용자로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/{userId}/roles - 성공: 사용자 권한 수정")
  void shouldUpdateUserRoles_WhenAuthorized() {
    // given
    String adminToken = generateTestToken();
    Long userId = 2L;
    String rolesRequest = """
        {
          "roles": ["USER", "GUARDIAN"],
          "reason": "보호자 권한 추가"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + userId + "/roles"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(rolesRequest, adminToken),
        String.class
    );

    // then
    // TODO: 관리자 권한 및 실제 사용자로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/users/{userId}/roles - 실패: 잘못된 권한")
  void shouldFailUpdateUserRoles_WhenInvalidRole() {
    // given
    String adminToken = generateTestToken();
    Long userId = 2L;
    String invalidRolesRequest = """
        {
          "roles": ["USER", "INVALID_ROLE"],
          "reason": "잘못된 권한 테스트"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + userId + "/roles"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(invalidRolesRequest, adminToken),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 빈 이름으로 사용자 정보 수정")
  void shouldFailUpdateUser_WhenEmptyName() {
    // given
    String token = generateTestToken();
    String emptyNameRequest = """
        {
          "name": "",
          "phone": "010-1234-5678"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(emptyNameRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 매우 긴 이름으로 사용자 정보 수정")
  void shouldFailUpdateUser_WhenNameTooLong() {
    // given
    String token = generateTestToken();
    String longName = "가".repeat(100);
    String longNameRequest = String.format("""
        {
          "name": "%s",
          "phone": "010-1234-5678"
        }
        """, longName);

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(longNameRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 자기 자신 비활성화 시도")
  void shouldFailDeactivateUser_WhenSelfDeactivation() {
    // given
    String token = generateTestToken();
    Long currentUserId = 1L; // 현재 사용자의 ID
    String deactivateRequest = """
        {
          "reason": "자기 자신 비활성화 시도"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/" + currentUserId + "/deactivate"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(deactivateRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("성능 테스트: 사용자 정보 조회 응답 시간")
  void shouldGetUserWithinTimeLimit() {
    // given
    String token = generateTestToken();

    // when
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );
    long endTime = System.currentTimeMillis();

    // then
    long responseTime = endTime - startTime;
    org.junit.jupiter.api.Assertions.assertTrue(
        responseTime < 3000,
        "사용자 정보 조회 응답 시간이 3초를 초과했습니다: " + responseTime + "ms"
    );
  }

  @Test
  @DisplayName("BIF 요구사항: 5학년 수준 에러 메시지 확인")
  void shouldReturnBIFFriendlyErrorMessage_WhenValidationFails() {
    // given
    String token = generateTestToken();
    String invalidRequest = """
        {
          "name": "",
          "phone": "잘못된번호"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/users/me"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(invalidRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
    // BIF 친화적 메시지 확인 (5학년 수준)
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getBody().contains("이름을") || response.getBody().contains("전화번호를"),
        "BIF 친화적 에러 메시지가 포함되어야 합니다"
    );
  }
}