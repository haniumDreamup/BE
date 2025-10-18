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
 * (GPT-4o로 교체하여 비활성화)
 */
@Slf4j
//@Configuration  // GPT-4o 사용으로 비활성화
@org.springframework.context.annotation.Profile("!test")
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
      GoogleCredentials credentials;

      // classpath 경로인지 파일 시스템 경로인지 확인
      if (credentialsPath.startsWith("/") || credentialsPath.contains(":")) {
        // 절대 경로 또는 Windows 경로 (C:)
        java.io.FileInputStream fileStream = new java.io.FileInputStream(credentialsPath);
        credentials = GoogleCredentials.fromStream(fileStream);
        log.info("Google Cloud 인증 정보 로드 성공 (파일 경로: {})", credentialsPath);
      } else {
        // classpath 경로
        credentials = GoogleCredentials.fromStream(
            getClass().getResourceAsStream(credentialsPath)
        );
        log.info("Google Cloud 인증 정보 로드 성공 (classpath: {})", credentialsPath);
      }

      return FixedCredentialsProvider.create(credentials);
    } catch (Exception e) {
      log.error("Google Cloud 인증 정보 로드 실패: {}", credentialsPath, e);
      return FixedCredentialsProvider.create(null);
    }
  }
  
  /**
   * Vision API 클라이언트
   */
  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      value = "google.cloud.vision.enabled", 
      havingValue = "true"
  )
  public ImageAnnotatorClient imageAnnotatorClient(CredentialsProvider credentialsProvider) {
    try {
      ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
          .setCredentialsProvider(credentialsProvider)
          .build();
      
      ImageAnnotatorClient client = ImageAnnotatorClient.create(settings);
      log.info("Google Cloud Vision API 클라이언트 초기화 성공");
      return client;
    } catch (Exception e) {
      log.error("Google Cloud Vision API 클라이언트 초기화 실패", e);
      throw new RuntimeException("Vision API 클라이언트 초기화 실패", e);
    }
  }
}