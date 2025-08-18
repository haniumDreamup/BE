package com.bifai.reminder.bifai_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Firebase Cloud Messaging 설정
 * 
 * 푸시 알림 전송을 위한 FCM SDK 초기화 및 설정
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true", matchIfMissing = false)
public class FcmConfig {
  
  @Value("${fcm.project-id:bifai-reminder}")
  private String projectId;
  
  @Value("${fcm.credentials-path:firebase-service-account.json}")
  private String credentialsPath;
  
  @Value("${fcm.enabled:true}")
  private boolean fcmEnabled;
  
  /**
   * Firebase 앱 초기화
   */
  @PostConstruct
  public void initialize() {
    if (!fcmEnabled) {
      log.info("FCM이 비활성화되어 있습니다");
      return;
    }
    
    try {
      if (FirebaseApp.getApps().isEmpty()) {
        GoogleCredentials googleCredentials = getGoogleCredentials();
        
        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(googleCredentials)
            .setProjectId(projectId)
            .build();
        
        FirebaseApp.initializeApp(options);
        log.info("Firebase 앱 초기화 완료: projectId={}", projectId);
      } else {
        log.info("Firebase 앱이 이미 초기화되어 있습니다");
      }
    } catch (Exception e) {
      log.error("Firebase 앱 초기화 실패: {}", e.getMessage());
      // 개발 환경에서는 FCM 없이도 실행 가능하도록 함
      if (isProductionProfile()) {
        throw new RuntimeException("Firebase 초기화 실패", e);
      }
    }
  }
  
  /**
   * FirebaseMessaging 빈 생성
   * 
   * @return FirebaseMessaging 인스턴스
   */
  @Bean
  public FirebaseMessaging firebaseMessaging() {
    if (!fcmEnabled || FirebaseApp.getApps().isEmpty()) {
      log.warn("Firebase가 초기화되지 않았습니다. Mock 객체를 반환합니다.");
      return null; // 개발 환경에서는 null 반환
    }
    
    return FirebaseMessaging.getInstance();
  }
  
  /**
   * Google 자격 증명 로드
   * 
   * @return GoogleCredentials
   * @throws IOException 파일 읽기 실패
   */
  private GoogleCredentials getGoogleCredentials() throws IOException {
    // 먼저 classpath에서 시도
    try {
      ClassPathResource resource = new ClassPathResource(credentialsPath);
      try (InputStream inputStream = resource.getInputStream()) {
        return GoogleCredentials.fromStream(inputStream);
      }
    } catch (Exception e) {
      log.debug("Classpath에서 자격 증명 파일을 찾을 수 없습니다: {}", e.getMessage());
    }
    
    // 환경 변수에서 시도 (프로덕션 환경)
    String fcmCredentials = System.getenv("FCM_CREDENTIALS_JSON");
    if (fcmCredentials != null && !fcmCredentials.isEmpty()) {
      return GoogleCredentials.fromStream(
          new java.io.ByteArrayInputStream(fcmCredentials.getBytes())
      );
    }
    
    // 기본 자격 증명 사용 (Google Cloud 환경)
    return GoogleCredentials.getApplicationDefault();
  }
  
  /**
   * 프로덕션 프로파일 확인
   * 
   * @return 프로덕션 환경 여부
   */
  private boolean isProductionProfile() {
    String profile = System.getProperty("spring.profiles.active", "");
    return profile.contains("prod") || profile.contains("production");
  }
}