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
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.mockito.Mockito;

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
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    return Mockito.mock(CircuitBreakerRegistry.class);
  }
  
  @Bean
  @Primary
  public RateLimiterRegistry rateLimiterRegistry() {
    RateLimiterRegistry registry = Mockito.mock(RateLimiterRegistry.class);
    RateLimiter rateLimiter = Mockito.mock(RateLimiter.class);
    
    // 모든 요청을 허용하도록 설정
    when(rateLimiter.acquirePermission()).thenReturn(true);
    when(registry.rateLimiter(anyString(), anyString())).thenReturn(rateLimiter);
    when(registry.rateLimiter(anyString())).thenReturn(rateLimiter);
    
    return registry;
  }
}