package com.bifai.reminder.bifai_backend.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Google Cloud Vision API 설정
 */
@Slf4j
@Configuration
public class GoogleCloudConfig {
  
  @Value("${google.cloud.credentials.path:}")
  private String credentialsPath;
  
  @Value("${google.cloud.project-id:}")
  private String projectId;
  
  @Value("${google.cloud.vision.enabled:false}")
  private boolean visionEnabled;
  
  /**
   * Google Cloud 인증 정보 제공자
   */
  @Bean
  public CredentialsProvider credentialsProvider() throws IOException {
    if (!visionEnabled || credentialsPath.isEmpty()) {
      log.warn("Google Cloud Vision API가 비활성화되었거나 인증 정보가 없습니다");
      return FixedCredentialsProvider.create(null);
    }
    
    try {
      GoogleCredentials credentials = GoogleCredentials.fromStream(
          getClass().getResourceAsStream(credentialsPath)
      );
      log.info("Google Cloud 인증 정보 로드 성공");
      return FixedCredentialsProvider.create(credentials);
    } catch (Exception e) {
      log.error("Google Cloud 인증 정보 로드 실패", e);
      return FixedCredentialsProvider.create(null);
    }
  }
  
  /**
   * Vision API 클라이언트
   */
  @Bean
  public ImageAnnotatorClient imageAnnotatorClient(CredentialsProvider credentialsProvider) {
    if (!visionEnabled) {
      log.info("Google Cloud Vision API가 비활성화되어 있습니다");
      return null;
    }
    
    try {
      ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
          .setCredentialsProvider(credentialsProvider)
          .build();
      
      ImageAnnotatorClient client = ImageAnnotatorClient.create(settings);
      log.info("Google Cloud Vision API 클라이언트 초기화 성공");
      return client;
    } catch (Exception e) {
      log.error("Google Cloud Vision API 클라이언트 초기화 실패", e);
      return null;
    }
  }
}