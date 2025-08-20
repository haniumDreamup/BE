package com.bifai.reminder.bifai_backend.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 외부 API 호출 서비스
 * Circuit Breaker 패턴 적용으로 장애 격리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientExternalApiService {
  
  private final RestTemplate restTemplate;
  private final WebClient webClient;
  
  /**
   * OpenAI API 호출 (Circuit Breaker + Retry)
   */
  @CircuitBreaker(name = "openai-api", fallbackMethod = "openAiFallback")
  @Retry(name = "openai-api")
  @RateLimiter(name = "openai-api")
  public String callOpenAiApi(String prompt) {
    log.debug("OpenAI API 호출: {}", prompt);
    
    // 실제 OpenAI API 호출 로직
    Map<String, Object> request = new HashMap<>();
    request.put("prompt", prompt);
    request.put("max_tokens", 150);
    
    return restTemplate.postForObject(
      "https://api.openai.com/v1/completions",
      request,
      String.class
    );
  }
  
  /**
   * OpenAI API 폴백 메소드
   */
  public String openAiFallback(String prompt, Exception ex) {
    log.error("OpenAI API 호출 실패, 폴백 실행: {}", ex.getMessage());
    return "AI 서비스가 일시적으로 사용 불가능합니다. 잠시 후 다시 시도해주세요.";
  }
  
  /**
   * Google Vision API 호출 (Circuit Breaker + Bulkhead)
   */
  @CircuitBreaker(name = "google-vision", fallbackMethod = "googleVisionFallback")
  @Bulkhead(name = "google-vision", type = Bulkhead.Type.SEMAPHORE)
  public String analyzeImage(byte[] imageData) {
    log.debug("Google Vision API 호출");
    
    // Google Vision API 호출 로직
    Map<String, Object> request = new HashMap<>();
    request.put("image", imageData);
    request.put("features", new String[]{"LABEL_DETECTION", "TEXT_DETECTION"});
    
    return restTemplate.postForObject(
      "https://vision.googleapis.com/v1/images:annotate",
      request,
      String.class
    );
  }
  
  /**
   * Google Vision API 폴백 메소드
   */
  public String googleVisionFallback(byte[] imageData, Exception ex) {
    log.error("Google Vision API 호출 실패: {}", ex.getMessage());
    return "이미지 분석 서비스를 사용할 수 없습니다.";
  }
  
  /**
   * 외부 날씨 API 호출 (비동기 + Circuit Breaker)
   */
  @CircuitBreaker(name = "weather-api", fallbackMethod = "weatherFallback")
  @Retry(name = "weather-api")
  public CompletableFuture<String> getWeatherAsync(double latitude, double longitude) {
    log.debug("날씨 API 비동기 호출: {}, {}", latitude, longitude);
    
    return CompletableFuture.supplyAsync(() -> {
      String url = String.format(
        "https://api.weather.com/v1/location?lat=%f&lon=%f",
        latitude, longitude
      );
      
      return restTemplate.getForObject(url, String.class);
    });
  }
  
  /**
   * 날씨 API 폴백
   */
  public CompletableFuture<String> weatherFallback(double latitude, double longitude, Exception ex) {
    log.warn("날씨 API 폴백 실행: {}", ex.getMessage());
    return CompletableFuture.completedFuture("날씨 정보를 가져올 수 없습니다.");
  }
  
  /**
   * 지도 API 호출 (WebClient + Circuit Breaker)
   */
  @CircuitBreaker(name = "map-api", fallbackMethod = "mapApiFallback")
  @RateLimiter(name = "map-api")
  public Mono<String> getMapData(String address) {
    log.debug("지도 API 호출: {}", address);
    
    return webClient.get()
      .uri("https://maps.api.com/geocode?address={address}", address)
      .retrieve()
      .bodyToMono(String.class);
  }
  
  /**
   * 지도 API 폴백
   */
  public Mono<String> mapApiFallback(String address, Exception ex) {
    log.error("지도 API 호출 실패: {}", ex.getMessage());
    return Mono.just("위치 정보를 찾을 수 없습니다.");
  }
  
  /**
   * 푸시 알림 서비스 호출 (Bulkhead로 동시 호출 제한)
   */
  @Bulkhead(name = "push-notification", type = Bulkhead.Type.THREADPOOL)
  @Retry(name = "push-notification")
  public CompletableFuture<Boolean> sendPushNotification(String userId, String message) {
    return CompletableFuture.supplyAsync(() -> {
      log.debug("푸시 알림 전송: {} -> {}", userId, message);
      
      try {
        // FCM 또는 APNs 호출
        Map<String, String> notification = new HashMap<>();
        notification.put("to", userId);
        notification.put("message", message);
        
        restTemplate.postForObject(
          "https://fcm.googleapis.com/fcm/send",
          notification,
          Void.class
        );
        
        return true;
      } catch (Exception e) {
        log.error("푸시 알림 전송 실패: {}", e.getMessage());
        return false;
      }
    });
  }
  
  /**
   * 헬스체크 (Circuit Breaker 상태 확인용)
   */
  @CircuitBreaker(name = "health-check")
  public String healthCheck(String serviceName) {
    log.debug("헬스체크: {}", serviceName);
    
    String url = String.format("https://%s/health", serviceName);
    return restTemplate.getForObject(url, String.class);
  }
}