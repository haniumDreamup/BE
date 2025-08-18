package com.bifai.reminder.bifai_backend.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmService {
  
  @Value("${fcm.key-path:firebase-service-account.json}")
  private String fcmKeyPath;
  
  @Value("${fcm.enabled:false}")
  private boolean fcmEnabled;
  
  @PostConstruct
  public void initialize() {
    if (!fcmEnabled) {
      log.info("FCM is disabled. Skipping initialization.");
      return;
    }
    
    try {
      GoogleCredentials googleCredentials = GoogleCredentials
          .fromStream(new ClassPathResource(fcmKeyPath).getInputStream());
      
      FirebaseOptions firebaseOptions = FirebaseOptions.builder()
          .setCredentials(googleCredentials)
          .build();
      
      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(firebaseOptions);
        log.info("Firebase Admin SDK 초기화 완료");
      }
    } catch (IOException e) {
      log.error("Firebase Admin SDK 초기화 실패", e);
    }
  }
  
  /**
   * 단일 디바이스에 푸시 알림 전송
   */
  public void sendPushNotification(String fcmToken, String title, String body, Map<String, String> data) {
    if (!fcmEnabled || fcmToken == null) {
      log.debug("FCM disabled or no token. Skipping notification.");
      return;
    }
    
    try {
      Message message = Message.builder()
          .setToken(fcmToken)
          .setNotification(Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build())
          .setAndroidConfig(AndroidConfig.builder()
              .setPriority(AndroidConfig.Priority.HIGH)
              .setNotification(AndroidNotification.builder()
                  .setSound("default")
                  .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                  .build())
              .build())
          .setApnsConfig(ApnsConfig.builder()
              .setAps(Aps.builder()
                  .setSound("default")
                  .build())
              .build())
          .putAllData(data != null ? data : new HashMap<>())
          .build();
      
      String response = FirebaseMessaging.getInstance().send(message);
      log.info("푸시 알림 전송 성공: {}", response);
      
    } catch (FirebaseMessagingException e) {
      log.error("푸시 알림 전송 실패: {}", e.getMessage());
      handleFcmError(e, fcmToken);
    }
  }
  
  /**
   * 여러 디바이스에 푸시 알림 전송
   */
  public void sendMulticastNotification(List<String> fcmTokens, String title, String body, Map<String, String> data) {
    if (!fcmEnabled || fcmTokens == null || fcmTokens.isEmpty()) {
      return;
    }
    
    try {
      MulticastMessage message = MulticastMessage.builder()
          .addAllTokens(fcmTokens)
          .setNotification(Notification.builder()
              .setTitle(title)
              .setBody(body)
              .build())
          .setAndroidConfig(AndroidConfig.builder()
              .setPriority(AndroidConfig.Priority.HIGH)
              .setNotification(AndroidNotification.builder()
                  .setSound("default")
                  .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                  .build())
              .build())
          .putAllData(data != null ? data : new HashMap<>())
          .build();
      
      BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
      log.info("멀티캐스트 알림 전송: 성공 {}, 실패 {}", 
          response.getSuccessCount(), response.getFailureCount());
      
      // 실패한 토큰 처리
      if (response.getFailureCount() > 0) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
          if (!responses.get(i).isSuccessful()) {
            log.error("토큰 {} 전송 실패: {}", 
                fcmTokens.get(i), responses.get(i).getException());
          }
        }
      }
    } catch (FirebaseMessagingException e) {
      log.error("멀티캐스트 알림 전송 실패", e);
    }
  }
  
  /**
   * 약물 복용 알림
   */
  public void sendMedicationReminder(String fcmToken, String medicationName, String time) {
    String title = "💊 약 먹을 시간이에요!";
    String body = String.format("%s을(를) 드실 시간이에요 (%s)", medicationName, time);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "MEDICATION_REMINDER");
    data.put("medicationName", medicationName);
    data.put("time", time);
    data.put("timestamp", LocalDateTime.now().toString());
    
    sendPushNotification(fcmToken, title, body, data);
  }
  
  /**
   * 일정 알림
   */
  public void sendScheduleReminder(String fcmToken, String scheduleName, String time, String location) {
    String title = "📅 일정 알림";
    String body = String.format("%s - %s", scheduleName, time);
    if (location != null && !location.isEmpty()) {
      body += String.format(" (%s)", location);
    }
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "SCHEDULE_REMINDER");
    data.put("scheduleName", scheduleName);
    data.put("time", time);
    data.put("location", location != null ? location : "");
    data.put("timestamp", LocalDateTime.now().toString());
    
    sendPushNotification(fcmToken, title, body, data);
  }
  
  /**
   * 긴급 알림 (보호자에게)
   */
  public void sendEmergencyAlert(List<String> guardianTokens, String userName, String message, 
                                  Double latitude, Double longitude) {
    String title = "🚨 긴급 알림";
    String body = String.format("%s님이 도움을 요청했어요: %s", userName, message);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "EMERGENCY_ALERT");
    data.put("userName", userName);
    data.put("message", message);
    if (latitude != null && longitude != null) {
      data.put("latitude", latitude.toString());
      data.put("longitude", longitude.toString());
    }
    data.put("timestamp", LocalDateTime.now().toString());
    
    sendMulticastNotification(guardianTokens, title, body, data);
  }
  
  /**
   * 활동 완료 알림
   */
  public void sendActivityCompletionNotification(String fcmToken, String activityType, String details) {
    String title = "✅ 잘 하셨어요!";
    String body = details;
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "ACTIVITY_COMPLETION");
    data.put("activityType", activityType);
    data.put("details", details);
    data.put("timestamp", LocalDateTime.now().toString());
    
    sendPushNotification(fcmToken, title, body, data);
  }
  
  /**
   * 일일 요약 알림
   */
  public void sendDailySummary(String fcmToken, int medicationsTaken, int schedulesCompleted) {
    String title = "🌟 오늘 하루 수고하셨어요!";
    String body = String.format("약 %d개 복용, 일정 %d개 완료했어요", 
        medicationsTaken, schedulesCompleted);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "DAILY_SUMMARY");
    data.put("medicationsTaken", String.valueOf(medicationsTaken));
    data.put("schedulesCompleted", String.valueOf(schedulesCompleted));
    data.put("timestamp", LocalDateTime.now().toString());
    
    sendPushNotification(fcmToken, title, body, data);
  }
  
  /**
   * FCM 에러 처리
   */
  private void handleFcmError(FirebaseMessagingException e, String fcmToken) {
    if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
        e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
      // 유효하지 않은 토큰 처리
      log.warn("Invalid FCM token: {}. Should be removed from database.", fcmToken);
      // TODO: 데이터베이스에서 토큰 제거
    }
  }
  
  /**
   * 토큰 유효성 검증
   */
  public boolean validateToken(String fcmToken) {
    if (!fcmEnabled || fcmToken == null || fcmToken.isEmpty()) {
      return false;
    }
    
    try {
      // 드라이런으로 토큰 검증
      Message message = Message.builder()
          .setToken(fcmToken)
          .setNotification(Notification.builder()
              .setTitle("Test")
              .setBody("Test")
              .build())
          .build();
      
      FirebaseMessaging.getInstance().send(message, true); // dryRun = true
      return true;
    } catch (FirebaseMessagingException e) {
      log.warn("Invalid FCM token: {}", fcmToken);
      return false;
    }
  }
}