package com.bifai.reminder.bifai_backend.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Resilience4j 설정
 * Circuit Breaker, Rate Limiter, Bulkhead, Retry 패턴 구현
 */
@Configuration
@Slf4j
public class ResilienceConfig {
  
  /**
   * Circuit Breaker 설정
   */
  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    // 기본 Circuit Breaker 설정
    CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
      .slidingWindowSize(100) // 100개 호출 기준
      .failureRateThreshold(50) // 50% 실패율
      .waitDurationInOpenState(Duration.ofSeconds(30)) // Open 상태 유지 시간
      .slowCallRateThreshold(80) // 80% 느린 호출
      .slowCallDurationThreshold(Duration.ofSeconds(3)) // 3초 이상이면 느린 호출
      .permittedNumberOfCallsInHalfOpenState(10) // Half-Open에서 허용 호출 수
      .automaticTransitionFromOpenToHalfOpenEnabled(true)
      .recordExceptions(IOException.class, TimeoutException.class)
      .ignoreExceptions(IllegalArgumentException.class)
      .build();
    
    // 외부 API용 Circuit Breaker 설정
    CircuitBreakerConfig externalApiConfig = CircuitBreakerConfig.custom()
      .slidingWindowSize(50)
      .failureRateThreshold(60)
      .waitDurationInOpenState(Duration.ofSeconds(60))
      .slowCallDurationThreshold(Duration.ofSeconds(5))
      .build();
    
    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
    
    // 특정 서비스별 설정 등록
    registry.addConfiguration("external-api", externalApiConfig);
    
    // 이벤트 리스너 등록
    registry.getEventPublisher()
      .onEntryAdded(entryAddedEvent -> {
        CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
        circuitBreaker.getEventPublisher()
          .onStateTransition(event -> 
            log.warn("Circuit Breaker {} 상태 변경: {} -> {}", 
              event.getCircuitBreakerName(), 
              event.getStateTransition().getFromState(),
              event.getStateTransition().getToState()))
          .onSlowCallRateExceeded(event ->
            log.warn("Circuit Breaker {} 느린 호출률 초과: {}%", 
              event.getCircuitBreakerName(), 
              event.getSlowCallRate()));
      });
    
    log.info("Circuit Breaker Registry 설정 완료");
    return registry;
  }
  
  /**
   * Rate Limiter 설정 (요청 속도 제한)
   */
  @Bean
  public RateLimiterRegistry rateLimiterRegistry() {
    // 기본 Rate Limiter 설정
    RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
      .limitForPeriod(100) // 주기당 100개 요청
      .limitRefreshPeriod(Duration.ofSeconds(1)) // 1초마다 갱신
      .timeoutDuration(Duration.ofSeconds(5)) // 대기 타임아웃
      .build();
    
    // API별 설정
    RateLimiterConfig apiConfig = RateLimiterConfig.custom()
      .limitForPeriod(50)
      .limitRefreshPeriod(Duration.ofSeconds(1))
      .timeoutDuration(Duration.ofSeconds(2))
      .build();
    
    RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
    registry.addConfiguration("api-limiter", apiConfig);
    
    log.info("Rate Limiter Registry 설정 완료");
    return registry;
  }
  
  /**
   * Bulkhead 설정 (동시 실행 제한)
   */
  @Bean
  public BulkheadRegistry bulkheadRegistry() {
    // 기본 Bulkhead 설정
    BulkheadConfig defaultConfig = BulkheadConfig.custom()
      .maxConcurrentCalls(25) // 최대 동시 호출
      .maxWaitDuration(Duration.ofMillis(500)) // 최대 대기 시간
      .build();
    
    // CPU 집약적 작업용 설정
    BulkheadConfig cpuIntensiveConfig = BulkheadConfig.custom()
      .maxConcurrentCalls(5) // CPU 작업은 적게
      .maxWaitDuration(Duration.ofMillis(100))
      .build();
    
    BulkheadRegistry registry = BulkheadRegistry.of(defaultConfig);
    registry.addConfiguration("cpu-intensive", cpuIntensiveConfig);
    
    log.info("Bulkhead Registry 설정 완료");
    return registry;
  }
  
  /**
   * Retry 설정 (재시도 정책)
   */
  @Bean
  public RetryRegistry retryRegistry() {
    // 기본 Retry 설정
    RetryConfig defaultConfig = RetryConfig.custom()
      .maxAttempts(3) // 최대 3회 시도
      .waitDuration(Duration.ofMillis(500)) // 재시도 간격
      .retryExceptions(IOException.class, TimeoutException.class)
      .ignoreExceptions(IllegalArgumentException.class)
      .build();
    
    // 외부 API용 Retry 설정
    RetryConfig externalApiConfig = RetryConfig.custom()
      .maxAttempts(5)
      .waitDuration(Duration.ofSeconds(1))
      .retryOnException(throwable -> 
        throwable instanceof IOException || throwable instanceof TimeoutException)
      .build();
    
    RetryRegistry registry = RetryRegistry.of(defaultConfig);
    registry.addConfiguration("external-api", externalApiConfig);
    
    log.info("Retry Registry 설정 완료");
    return registry;
  }
  
  /**
   * 커스텀 Circuit Breaker 이벤트 리스너
   */
  @Bean
  public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
    return new RegistryEventConsumer<CircuitBreaker>() {
      @Override
      public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
        log.info("Circuit Breaker 추가: {}", entryAddedEvent.getAddedEntry().getName());
      }
      
      @Override
      public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
        log.info("Circuit Breaker 제거: {}", entryRemoveEvent.getRemovedEntry().getName());
      }
      
      @Override
      public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
        log.info("Circuit Breaker 교체: {}", entryReplacedEvent.getNewEntry().getName());
      }
    };
  }
}