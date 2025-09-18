package com.bifai.reminder.bifai_backend.config;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.firebase.messaging.FirebaseMessaging;
import com.bifai.reminder.bifai_backend.service.GoogleTtsService;
import com.bifai.reminder.bifai_backend.service.OpenAIService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.RetryConfig;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 외부 서비스 Mock 설정
 * Spring Boot 테스트 베스트 프랙티스에 따라 외부 서비스를 Mock으로 대체
 */
@Configuration
@Profile("test")
public class TestExternalServicesConfig {
  
  @Bean
  @Primary
  public ImageAnnotatorClient imageAnnotatorClient() {
    return Mockito.mock(ImageAnnotatorClient.class);
  }
  
  @Bean
  @Primary
  public TextToSpeechClient textToSpeechClient() {
    return Mockito.mock(TextToSpeechClient.class);
  }
  
  @Bean 
  @Primary
  public GoogleTtsService googleTtsService() {
    return Mockito.mock(GoogleTtsService.class);
  }
  
  @Bean
  @Primary
  public ChatClient chatClient() {
    return Mockito.mock(ChatClient.class);
  }
  
  @Bean
  @Primary
  public OpenAIService openAIService() {
    return Mockito.mock(OpenAIService.class);
  }
  
  @Bean
  @Primary
  public FirebaseMessaging firebaseMessaging() {
    return Mockito.mock(FirebaseMessaging.class);
  }
  
  @Bean
  @Primary
  public JavaMailSender javaMailSender() {
    return Mockito.mock(JavaMailSender.class);
  }
  
  @Bean
  @Primary
  public RestTemplate restTemplate() {
    return Mockito.mock(RestTemplate.class);
  }

  @Bean
  @Primary
  public WebClient webClient() {
    return Mockito.mock(WebClient.class);
  }

  // Resilience4j 테스트 설정 추가
  @Bean
  @Primary
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
      .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
      .slidingWindowSize(5) // 테스트용으로 작은 크기
      .minimumNumberOfCalls(3) // 최소 3번 호출 후 판단
      .failureRateThreshold(50) // 50% 실패율
      .waitDurationInOpenState(Duration.ofMillis(500)) // 테스트용으로 짧은 시간
      .slowCallDurationThreshold(Duration.ofMillis(500))
      .permittedNumberOfCallsInHalfOpenState(2)
      .automaticTransitionFromOpenToHalfOpenEnabled(true)
      .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

    // 테스트용 circuit breaker들 추가
    registry.circuitBreaker("openai-api", config);
    registry.circuitBreaker("google-vision", config);
    registry.circuitBreaker("weather-api", config);
    registry.circuitBreaker("map-api", config);
    registry.circuitBreaker("health-check", config);

    return registry;
  }

  @Bean
  @Primary
  public RateLimiterRegistry rateLimiterRegistry() {
    RateLimiterConfig config = RateLimiterConfig.custom()
      .limitForPeriod(5) // 테스트용으로 작은 수
      .limitRefreshPeriod(Duration.ofSeconds(1))
      .timeoutDuration(Duration.ofMillis(100))
      .build();

    RateLimiterRegistry registry = RateLimiterRegistry.of(config);
    registry.rateLimiter("openai-api", config);
    registry.rateLimiter("map-api", config);

    return registry;
  }

  @Bean
  @Primary
  public BulkheadRegistry bulkheadRegistry() {
    BulkheadConfig config = BulkheadConfig.custom()
      .maxConcurrentCalls(3) // 테스트용으로 작은 수
      .maxWaitDuration(Duration.ofMillis(100))
      .build();

    BulkheadRegistry registry = BulkheadRegistry.of(config);
    registry.bulkhead("google-vision", config);
    registry.bulkhead("push-notification", config);

    return registry;
  }

  @Bean
  @Primary
  public RetryRegistry retryRegistry() {
    RetryConfig config = RetryConfig.custom()
      .maxAttempts(3)
      .waitDuration(Duration.ofMillis(100))
      .build();

    RetryRegistry registry = RetryRegistry.of(config);
    registry.retry("openai-api", config);
    registry.retry("weather-api", config);
    registry.retry("push-notification", config);

    return registry;
  }
}