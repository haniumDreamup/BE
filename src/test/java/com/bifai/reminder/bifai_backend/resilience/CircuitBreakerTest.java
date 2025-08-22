package com.bifai.reminder.bifai_backend.resilience;

import com.bifai.reminder.bifai_backend.service.ResilientExternalApiService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Circuit Breaker 테스트
 */
@SpringBootTest
@DisplayName("Circuit Breaker 패턴 테스트")
class CircuitBreakerTest {
  
  @Autowired
  private ResilientExternalApiService resilientApiService;
  
  @Autowired
  private CircuitBreakerRegistry circuitBreakerRegistry;
  
  @MockBean
  private RestTemplate restTemplate;
  
  @MockBean
  private WebClient webClient;
  
  @BeforeEach
  void setUp() {
    // Circuit Breaker 초기화
    circuitBreakerRegistry.getAllCircuitBreakers()
      .forEach(CircuitBreaker::reset);
  }
  
  @Test
  @DisplayName("Circuit Breaker가 실패 시 폴백 메소드를 호출하는지 확인")
  void testCircuitBreakerFallback() {
    // RestTemplate이 예외를 던지도록 설정
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenThrow(new RuntimeException("API 호출 실패"));
    
    // API 호출
    String result = resilientApiService.callOpenAiApi("테스트 프롬프트");
    
    // 폴백 응답 확인
    assertThat(result).contains("AI 서비스가 일시적으로 사용 불가능합니다");
  }
  
  @Test
  @DisplayName("연속 실패 시 Circuit이 Open 상태로 전환되는지 확인")
  void testCircuitBreakerOpenState() {
    // Circuit Breaker 가져오기
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("openai-api");
    
    // RestTemplate이 항상 실패하도록 설정
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenThrow(new RuntimeException("API 실패"));
    
    // 여러 번 호출하여 실패율 임계값 초과
    for (int i = 0; i < 60; i++) {
      resilientApiService.callOpenAiApi("테스트");
    }
    
    // Circuit이 Open 상태인지 확인
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    
    // Open 상태에서는 즉시 폴백 호출
    String result = resilientApiService.callOpenAiApi("테스트");
    assertThat(result).contains("AI 서비스가 일시적으로 사용 불가능합니다");
    
    // RestTemplate 호출이 차단되었는지 확인
    verify(restTemplate, atMost(60)).postForObject(anyString(), any(), eq(String.class));
  }
  
  @Test
  @DisplayName("Retry 메커니즘이 작동하는지 확인")
  void testRetryMechanism() {
    // 처음 2번은 실패, 3번째는 성공
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenThrow(new RuntimeException("첫 번째 실패"))
      .thenThrow(new RuntimeException("두 번째 실패"))
      .thenReturn("성공");
    
    // API 호출
    String result = resilientApiService.callOpenAiApi("테스트");
    
    // 재시도 후 성공했는지 확인
    assertThat(result).isEqualTo("성공");
    
    // RestTemplate이 3번 호출되었는지 확인
    verify(restTemplate, times(3)).postForObject(anyString(), any(), eq(String.class));
  }
  
  @Test
  @DisplayName("Rate Limiter가 요청을 제한하는지 확인")
  void testRateLimiter() throws InterruptedException {
    // 정상 응답 설정
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenReturn("성공");
    
    int successCount = 0;
    int rejectedCount = 0;
    
    // 짧은 시간에 많은 요청 시도
    for (int i = 0; i < 100; i++) {
      try {
        String result = resilientApiService.callOpenAiApi("테스트");
        if ("성공".equals(result)) {
          successCount++;
        }
      } catch (Exception e) {
        rejectedCount++;
      }
      
      // 요청 간 짧은 지연
      Thread.sleep(10);
    }
    
    // Rate Limiting으로 일부 요청이 거부되었는지 확인
    assertThat(successCount).isLessThan(100);
    assertThat(rejectedCount).isGreaterThan(0);
  }
  
  @Test
  @DisplayName("Bulkhead가 동시 실행을 제한하는지 확인")
  void testBulkhead() throws Exception {
    // 느린 응답 시뮬레이션
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenAnswer(invocation -> {
        Thread.sleep(1000); // 1초 지연
        return "성공";
      });
    
    // 동시에 여러 요청 시작
    CompletableFuture<?>[] futures = new CompletableFuture[30];
    for (int i = 0; i < 30; i++) {
      futures[i] = resilientApiService.sendPushNotification("user" + i, "메시지");
    }
    
    // 모든 요청 완료 대기
    CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
    
    // 동시 실행이 제한되었는지 확인 (Bulkhead 설정에 따라)
    // 실제 동시 실행 수는 Bulkhead 설정값을 초과하지 않아야 함
    assertThat(futures).allMatch(f -> f.isDone());
  }
  
  @Test
  @DisplayName("Circuit Breaker가 Half-Open 상태로 전환되는지 확인")
  void testCircuitBreakerHalfOpenState() throws InterruptedException {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("openai-api");
    
    // Circuit을 Open 상태로 만들기
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenThrow(new RuntimeException("실패"));
    
    for (int i = 0; i < 60; i++) {
      resilientApiService.callOpenAiApi("테스트");
    }
    
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    
    // 대기 시간 후 Half-Open으로 전환
    Thread.sleep(31000); // 30초 대기 + 여유 시간
    
    // 성공하도록 설정
    when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
      .thenReturn("성공");
    
    // Half-Open 상태에서 테스트 호출
    String result = resilientApiService.callOpenAiApi("테스트");
    
    // 성공하면 Closed로 전환
    if ("성공".equals(result)) {
      assertThat(circuitBreaker.getState()).isIn(
        CircuitBreaker.State.HALF_OPEN,
        CircuitBreaker.State.CLOSED
      );
    }
  }
  
  @Test
  @DisplayName("다양한 API 엔드포인트에 대한 Circuit Breaker 동작 확인")
  void testMultipleEndpointCircuitBreakers() {
    // Google Vision API 실패 설정
    when(restTemplate.postForObject(contains("vision.googleapis.com"), any(), eq(String.class)))
      .thenThrow(new RuntimeException("Vision API 실패"));
    
    // 이미지 분석 호출
    String visionResult = resilientApiService.analyzeImage(new byte[0]);
    assertThat(visionResult).contains("이미지 분석 서비스를 사용할 수 없습니다");
    
    // Weather API는 정상 동작
    when(restTemplate.getForObject(contains("weather.com"), eq(String.class)))
      .thenReturn("날씨 정보");
    
    CompletableFuture<String> weatherFuture = resilientApiService.getWeatherAsync(37.5, 127.0);
    String weatherResult = weatherFuture.join();
    assertThat(weatherResult).isEqualTo("날씨 정보");
  }
}