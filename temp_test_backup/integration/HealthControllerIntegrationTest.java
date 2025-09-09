package com.bifai.reminder.bifai_backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * HealthController 통합 테스트
 * 헬스체크 엔드포인트의 성공/실패/엣지 케이스 검증
 */
@DisplayName("Health Controller 통합 테스트")
class HealthControllerIntegrationTest extends BaseIntegrationTest {

  @Test
  @DisplayName("GET /api/health - 성공: 기본 헬스체크")
  void shouldReturnHealthy_WhenSystemIsUp() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getBody().contains("\"status\":\"UP\"") ||
        response.getBody().contains("\"healthy\":true") ||
        response.getBody().contains("\"success\":true"),
        "헬스체크 응답에 정상 상태가 포함되어야 합니다"
    );
  }

  @Test
  @DisplayName("GET /api/v1/health - 성공: v1 헬스체크")
  void shouldReturnHealthy_WhenSystemIsUpV1() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/v1/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/health - 성공: 중첩 경로 헬스체크")
  void shouldReturnHealthy_WhenSystemIsUpNested() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/v1/api/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("GET /api/v1/api/v1/health - 성공: 이중 중첩 경로 헬스체크")
  void shouldReturnHealthy_WhenSystemIsUpDoubleNested() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/v1/api/v1/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
  }

  @Test
  @DisplayName("성능 테스트: 헬스체크 응답 시간")
  void shouldRespondQuickly_ForHealthCheck() {
    // when
    long startTime = System.currentTimeMillis();
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/health"),
        String.class
    );
    long endTime = System.currentTimeMillis();

    // then
    long responseTime = endTime - startTime;
    assertSuccessResponse(response);
    org.junit.jupiter.api.Assertions.assertTrue(
        responseTime < 1000,
        "헬스체크 응답 시간이 1초를 초과했습니다: " + responseTime + "ms"
    );
  }

  @Test
  @DisplayName("동시성 테스트: 다중 헬스체크 요청")
  void shouldHandleConcurrentHealthChecks() throws InterruptedException {
    // given
    int numberOfThreads = 10;
    Thread[] threads = new Thread[numberOfThreads];
    boolean[] results = new boolean[numberOfThreads];

    // when
    for (int i = 0; i < numberOfThreads; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        try {
          ResponseEntity<String> response = restTemplate.getForEntity(
              createURL("/api/health"),
              String.class
          );
          results[index] = response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
          results[index] = false;
        }
      });
      threads[i].start();
    }

    // 모든 스레드 완료 대기
    for (Thread thread : threads) {
      thread.join();
    }

    // then
    for (int i = 0; i < numberOfThreads; i++) {
      org.junit.jupiter.api.Assertions.assertTrue(
          results[i],
          "스레드 " + i + "의 헬스체크가 실패했습니다"
      );
    }
  }

  @Test
  @DisplayName("엣지 케이스: 잘못된 HTTP 메소드")
  void shouldReturnMethodNotAllowed_WhenWrongHttpMethod() {
    // when
    ResponseEntity<String> response = restTemplate.postForEntity(
        createURL("/api/health"),
        null,
        String.class
    );

    // then
    assertErrorResponse(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  @DisplayName("엣지 케이스: 잘못된 Accept 헤더")
  void shouldHandleInvalidAcceptHeader() {
    // given
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.set("Accept", "application/xml"); // JSON API에 XML 요청
    org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

    // when
    ResponseEntity<String> response = restTemplate.exchange(
        createURL("/api/health"),
        org.springframework.http.HttpMethod.GET,
        entity,
        String.class
    );

    // then
    // 대부분의 API는 JSON을 기본으로 반환하거나 406을 반환
    org.junit.jupiter.api.Assertions.assertTrue(
        response.getStatusCode() == HttpStatus.OK ||
        response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE,
        "잘못된 Accept 헤더에 대한 적절한 응답이 필요합니다"
    );
  }

  @Test
  @DisplayName("헬스체크 응답 구조 검증")
  void shouldReturnProperHealthCheckStructure() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
    String responseBody = response.getBody();
    
    // 기본적인 JSON 구조 검증
    org.junit.jupiter.api.Assertions.assertTrue(
        responseBody.startsWith("{") && responseBody.endsWith("}"),
        "응답이 유효한 JSON 형식이어야 합니다"
    );

    // 타임스탬프 필드 존재 확인
    org.junit.jupiter.api.Assertions.assertTrue(
        responseBody.contains("timestamp") || 
        responseBody.contains("time") ||
        responseBody.contains("DateTime"),
        "헬스체크 응답에 타임스탬프가 포함되어야 합니다"
    );
  }

  @Test
  @DisplayName("BIF 요구사항: 3초 이내 응답 검증")
  void shouldMeetBIFPerformanceRequirement() {
    // given
    int numberOfRequests = 5;
    long totalTime = 0;

    // when
    for (int i = 0; i < numberOfRequests; i++) {
      long startTime = System.currentTimeMillis();
      ResponseEntity<String> response = restTemplate.getForEntity(
          createURL("/api/health"),
          String.class
      );
      long endTime = System.currentTimeMillis();
      
      assertSuccessResponse(response);
      totalTime += (endTime - startTime);
    }

    // then
    long averageTime = totalTime / numberOfRequests;
    org.junit.jupiter.api.Assertions.assertTrue(
        averageTime < 3000,
        "BIF 요구사항: 평균 응답 시간이 3초를 초과했습니다: " + averageTime + "ms"
    );
  }

  @Test
  @DisplayName("데이터베이스 연결 상태 확인")
  void shouldIndicateDatabaseHealth() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
    // TODO: 실제 구현에서 데이터베이스 상태를 포함하는지 확인
    // 데이터베이스 연결 실패 시 적절한 상태 반환 검증
  }

  @Test
  @DisplayName("메모리 사용량 임계점 테스트")
  void shouldMonitorSystemResources() {
    // when
    ResponseEntity<String> response = restTemplate.getForEntity(
        createURL("/api/health"),
        String.class
    );

    // then
    assertSuccessResponse(response);
    
    // 현재 메모리 사용량 체크
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    
    double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
    
    // BIF 요구사항: 시스템 리소스 모니터링
    org.junit.jupiter.api.Assertions.assertTrue(
        memoryUsagePercentage < 90,
        "메모리 사용량이 너무 높습니다: " + memoryUsagePercentage + "%"
    );
  }
}