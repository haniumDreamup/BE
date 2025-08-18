package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.pose.PoseDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 테스트 자동 설정
 * 모든 테스트에서 자동으로 로드되도록 설정
 */
@TestConfiguration
@Profile("test")
public class TestAutoConfiguration {

  // ============== Redis Mock 설정 ==============
  
  @Bean
  @Primary
  public RedisConnectionFactory testRedisConnectionFactory() {
    return mock(LettuceConnectionFactory.class);
  }

  @Bean
  @Primary
  public RedisTemplate<String, Object> testRedisTemplate() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    
    // 기본 동작 Mock
    org.springframework.data.redis.core.ValueOperations valueOps = mock(org.springframework.data.redis.core.ValueOperations.class);
    org.springframework.data.redis.core.HashOperations hashOps = mock(org.springframework.data.redis.core.HashOperations.class);
    org.springframework.data.redis.core.ListOperations listOps = mock(org.springframework.data.redis.core.ListOperations.class);
    org.springframework.data.redis.core.SetOperations setOps = mock(org.springframework.data.redis.core.SetOperations.class);
    
    when(template.opsForValue()).thenReturn(valueOps);
    when(template.opsForHash()).thenReturn(hashOps);
    when(template.opsForList()).thenReturn(listOps);
    when(template.opsForSet()).thenReturn(setOps);
    when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(template.hasKey(anyString())).thenReturn(false);
    when(template.delete(anyString())).thenReturn(true);
    
    // ValueOperations Mock
    when(valueOps.get(anyString())).thenReturn(null);
    when(valueOps.setIfAbsent(anyString(), any())).thenReturn(true);
    
    return template;
  }

  @Bean
  @Primary
  public StringRedisTemplate testStringRedisTemplate() {
    StringRedisTemplate template = mock(StringRedisTemplate.class);
    
    org.springframework.data.redis.core.ValueOperations<String, String> valueOps = mock(org.springframework.data.redis.core.ValueOperations.class);
    
    when(template.opsForValue()).thenReturn(valueOps);
    when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(template.hasKey(anyString())).thenReturn(false);
    when(valueOps.get(anyString())).thenReturn(null);
    
    return template;
  }

  @Bean
  @Primary
  public RedisTemplate<String, String> testStringStringRedisTemplate() {
    return mock(RedisTemplate.class);
  }

  // ============== Redis Service Mock ==============
  
  @Bean
  @Primary
  public RefreshTokenService testRefreshTokenService() {
    RefreshTokenService service = mock(RefreshTokenService.class);
    
    // validateRefreshToken은 Long을 반환
    when(service.validateRefreshToken(anyString())).thenReturn(1L);
    when(service.getRefreshToken(anyLong())).thenReturn("mock-refresh-token");
    when(service.hasRefreshToken(anyLong())).thenReturn(true);
    
    // void 메소드들은 doNothing() 사용
    org.mockito.Mockito.doNothing().when(service).saveRefreshToken(anyLong(), anyString(), anyLong());
    org.mockito.Mockito.doNothing().when(service).deleteRefreshToken(anyLong());
    org.mockito.Mockito.doNothing().when(service).rotateRefreshToken(anyString(), anyString(), anyLong(), anyLong());
    
    return service;
  }

  @Bean
  @Primary
  public RedisCacheService testRedisCacheService() {
    RedisCacheService service = mock(RedisCacheService.class);
    
    when(service.get(anyString())).thenReturn(null);
    when(service.hasKey(anyString())).thenReturn(false);
    
    // void 메소드들
    org.mockito.Mockito.doNothing().when(service).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    org.mockito.Mockito.doNothing().when(service).delete(anyString());
    
    return service;
  }

  // ============== ObjectMapper ==============
  
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public ObjectMapper testObjectMapper() {
    return new ObjectMapper();
  }
  
  // ============== Google Vision Mock ==============
  
  @Bean
  @Primary
  public ImageAnnotatorClient testImageAnnotatorClient() {
    return mock(ImageAnnotatorClient.class);
  }
  
  // ============== FCM Mock ==============
  
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public FirebaseMessaging testFirebaseMessaging() {
    FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    
    // Mock 메소드들
    try {
      when(messaging.send(any())).thenReturn("mock-message-id");
      when(messaging.sendAll(any())).thenReturn(null);
    } catch (Exception e) {
      // Mock 설정 중 예외 무시
    }
    
    return messaging;
  }
  
  // ============== S3 Mock ==============
  
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public S3Client testS3Client() {
    return mock(S3Client.class);
  }
  
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public S3AsyncClient testS3AsyncClient() {
    return mock(S3AsyncClient.class);
  }
  
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public S3Presigner testS3Presigner() {
    return mock(S3Presigner.class);
  }
}