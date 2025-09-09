package com.bifai.reminder.bifai_backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * EmergencyController 통합 테스트
 * 긴급 상황 처리 엔드포인트의 성공/실패/엣지 케이스 검증
 */
@DisplayName("Emergency Controller 통합 테스트")
class EmergencyControllerIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("POST /api/v1/api/v1/emergency/alert - 성공: 긴급 상황 알림 생성")
  void shouldCreateEmergencyAlert_WhenValidRequest() {
    // given
    String token = generateTestToken();
    String emergencyRequest = """
        {
          "type": "SOS",
          "latitude": 37.5665,
          "longitude": 126.9780,
          "description": "도움이 필요합니다",
          "severity": "HIGH"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(emergencyRequest, token),
        String.class
    );

    // then
    assertSuccessResponse(response);
    // TODO: 긴급 상황 생성 확인 및 알림 전송 검증
  }

  @Test
  @DisplayName("POST /api/v1/api/v1/emergency/alert - 실패: 인증되지 않은 요청")
  void shouldFailEmergencyAlert_WhenNotAuthenticated() {
    // given
    String emergencyRequest = """
        {
          "type": "SOS",
          "latitude": 37.5665,
          "longitude": 126.9780,
          "description": "도움이 필요합니다"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(emergencyRequest),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("POST /api/v1/api/v1/emergency/alert - 실패: 필수 필드 누락")
  void shouldFailEmergencyAlert_WhenMissingRequiredFields() {
    // given
    String token = generateTestToken();
    String invalidRequest = """
        {
          "description": "도움이 필요합니다"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(invalidRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/v1/api/v1/emergency/fall-detection - 성공: 낙상 감지 처리")
  void shouldHandleFallDetection_WhenValidRequest() {
    // given
    String token = generateTestToken();
    String fallDetectionRequest = """
        {
          "confidence": 0.95,
          "latitude": 37.5665,
          "longitude": 126.9780,
          "timestamp": "2024-01-01T12:00:00Z",
          "sensorData": {
            "acceleration": [1.2, -9.8, 0.3],
            "gyroscope": [0.1, 0.2, 0.1]
          }
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/fall-detection"),
        createRequestEntity(fallDetectionRequest, token),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("POST /api/v1/api/v1/emergency/fall-detection - 실패: 낮은 신뢰도")
  void shouldRejectFallDetection_WhenLowConfidence() {
    // given
    String token = generateTestToken();
    String lowConfidenceRequest = """
        {
          "confidence": 0.3,
          "latitude": 37.5665,
          "longitude": 126.9780,
          "timestamp": "2024-01-01T12:00:00Z"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/fall-detection"),
        createRequestEntity(lowConfidenceRequest, token),
        String.class
    );

    // then
    // 낮은 신뢰도는 처리하지만 긴급 상황은 생성하지 않을 수 있음
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/emergency/status/{emergencyId} - 성공: 긴급 상황 상태 조회")
  void shouldGetEmergencyStatus_WhenValidId() {
    // given
    String token = generateTestToken();
    Long emergencyId = 1L;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/status/" + emergencyId),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    // TODO: 실제 긴급 상황 데이터로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/emergency/status/{emergencyId} - 실패: 존재하지 않는 ID")
  void shouldFailGetEmergencyStatus_WhenInvalidId() {
    // given
    String token = generateTestToken();
    Long invalidId = 99999L;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/status/" + invalidId),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/emergency/history/{userId} - 성공: 사용자 긴급 상황 이력 조회")
  void shouldGetEmergencyHistory_WhenValidUserId() {
    // given
    String token = generateTestToken();
    Long userId = 1L;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/history/" + userId),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/emergency/active - 성공: 활성 긴급 상황 조회")
  void shouldGetActiveEmergencies_WhenAuthenticated() {
    // given
    String token = generateTestToken();

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/active"),
        org.springframework.http.HttpMethod.GET,
        createGetEntity(token),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/emergency/{emergencyId}/resolve - 성공: 긴급 상황 해결")
  void shouldResolveEmergency_WhenValidRequest() {
    // given
    String token = generateTestToken();
    Long emergencyId = 1L;
    String resolveRequest = """
        {
          "resolution": "안전하게 해결됨",
          "resolvedBy": "사용자 본인",
          "timestamp": "2024-01-01T12:30:00Z"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/" + emergencyId + "/resolve"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(resolveRequest, token),
        String.class
    );

    // then
    // TODO: 실제 긴급 상황 데이터로 테스트
    // assertSuccessResponse(response);
  }

  @Test
  @DisplayName("PUT /api/v1/api/v1/emergency/{emergencyId}/resolve - 실패: 이미 해결된 긴급 상황")
  void shouldFailResolveEmergency_WhenAlreadyResolved() {
    // given
    String token = generateTestToken();
    Long emergencyId = 1L;
    String resolveRequest = """
        {
          "resolution": "안전하게 해결됨",
          "resolvedBy": "사용자 본인"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/v1/api/v1/emergency/" + emergencyId + "/resolve"),
        org.springframework.http.HttpMethod.PUT,
        createRequestEntity(resolveRequest, token),
        String.class
    );

    // then
    // TODO: 실제 이미 해결된 긴급 상황으로 테스트
    assertErrorResponse(response, HttpStatus.CONFLICT);
  }

  @Test
  @DisplayName("엣지 케이스: 잘못된 위치 좌표")
  void shouldHandleInvalidCoordinates_Gracefully() {
    // given
    String token = generateTestToken();
    String invalidCoordinatesRequest = """
        {
          "type": "SOS",
          "latitude": 999.999,
          "longitude": 999.999,
          "description": "잘못된 위치"
        }
        """;

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(invalidCoordinatesRequest, token),
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 매우 긴 설명 문자열")
  void shouldHandleLongDescription_Gracefully() {
    // given
    String token = generateTestToken();
    String longDescription = "도움이 필요합니다. ".repeat(100);
    String longDescriptionRequest = String.format("""
        {
          "type": "SOS",
          "latitude": 37.5665,
          "longitude": 126.9780,
          "description": "%s"
        }
        """, longDescription);

    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(longDescriptionRequest, token),
        String.class
    );

    // then
    // 설명이 너무 길면 잘라내거나 에러 처리
    assertErrorResponse(response, HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("엣지 케이스: 동시 다중 긴급 상황 생성")
  void shouldHandleConcurrentEmergencies_Gracefully() {
    // given
    String token = generateTestToken();
    String emergencyRequest1 = """
        {
          "type": "SOS",
          "latitude": 37.5665,
          "longitude": 126.9780,
          "description": "첫 번째 긴급 상황"
        }
        """;
    
    String emergencyRequest2 = """
        {
          "type": "FALL",
          "latitude": 37.5666,
          "longitude": 126.9781,
          "description": "두 번째 긴급 상황"
        }
        """;

    // when
    ResponseEntity<String> response1 = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(emergencyRequest1, token),
        String.class
    );
    
    ResponseEntity<String> response2 = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(emergencyRequest2, token),
        String.class
    );

    // then
    // 두 번째 요청이 중복으로 처리되지 않거나 적절히 병합되어야 함
    assertSuccessResponse(response1);
    // response2는 성공하거나 중복으로 인한 에러일 수 있음
  }

  @Test
  @DisplayName("성능 테스트: 긴급 상황 생성 응답 시간")
  void shouldCreateEmergencyWithinTimeLimit() {
    // given
    String token = generateTestToken();
    String emergencyRequest = """
        {
          "type": "SOS",
          "latitude": 37.5665,
          "longitude": 126.9780,
          "description": "응답 시간 테스트"
        }
        """;

    // when
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/v1/api/v1/emergency/alert"),
        createRequestEntity(emergencyRequest, token),
        String.class
    );
    long endTime = System.currentTimeMillis();

    // then
    long responseTime = endTime - startTime;
    org.junit.jupiter.api.Assertions.assertTrue(
        responseTime < 3000,
        "긴급 상황 생성 응답 시간이 3초를 초과했습니다: " + responseTime + "ms"
    );
  }
}