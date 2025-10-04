package com.bifai.reminder.bifai_backend.service.mobile;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * FCM 푸시 알림 서비스
 *
 * Firebase Cloud Messaging을 통한 푸시 알림 전송 처리
 * BIF 사용자를 위한 간단하고 명확한 알림 메시지 제공
 */
@Slf4j
@Service
public class FcmService {

  private final FirebaseMessaging firebaseMessaging;

  public FcmService(@Autowired(required = false) FirebaseMessaging firebaseMessaging) {
    this.firebaseMessaging = firebaseMessaging;
    if (firebaseMessaging == null) {
      log.warn("FirebaseMessaging이 구성되지 않았습니다. FCM 알림이 비활성화됩니다.");
    }
  }
  
  // 알림 우선순위
  public enum Priority {
    HIGH("high"),      // 긴급 알림
    NORMAL("normal"),  // 일반 알림
    LOW("low");        // 낮은 우선순위
    
    private final String value;
    
    Priority(String value) {
      this.value = value;
    }
  }
  
  // 알림 카테고리
  public enum NotificationCategory {
    MEDICATION("약물", "💊"),
    SCHEDULE("일정", "📅"),
    EMERGENCY("긴급", "🚨"),
    REMINDER("알림", "🔔"),
    HEALTH("건강", "❤️"),
    LOCATION("위치", "📍");
    
    private final String korean;
    private final String emoji;
    
    NotificationCategory(String korean, String emoji) {
      this.korean = korean;
      this.emoji = emoji;
    }
    
    public String getTitle() {
      return emoji + " " + korean;
    }
  }
  
  /**
   * 단일 디바이스에 푸시 알림 전송
   * 
   * @param token FCM 토큰
   * @param title 알림 제목
   * @param body 알림 내용
   * @param data 추가 데이터
   * @param category 알림 카테고리
   * @param priority 우선순위
   * @return 전송 성공 여부
   */
  public boolean sendNotification(String token, String title, String body, 
                                 Map<String, String> data, 
                                 NotificationCategory category,
                                 Priority priority) {
    if (firebaseMessaging == null) {
      log.warn("FCM이 초기화되지 않았습니다. 알림을 전송할 수 없습니다.");
      return false;
    }
    
    try {
      // 알림 메시지 생성
      Notification notification = Notification.builder()
          .setTitle(simplifyTitle(title, category))
          .setBody(simplifyMessage(body))
          .build();
      
      // Android 설정
      AndroidConfig androidConfig = AndroidConfig.builder()
          .setPriority(mapToAndroidPriority(priority))
          .setNotification(AndroidNotification.builder()
              .setSound("default")
              .setChannelId(category.name().toLowerCase())
              .setTag(category.name())
              .build())
          .build();
      
      // iOS 설정
      ApnsConfig apnsConfig = ApnsConfig.builder()
          .setAps(Aps.builder()
              .setSound("default")
              .setCategory(category.name())
              .setThreadId(category.name())
              .build())
          .build();
      
      // 데이터 추가
      Map<String, String> messageData = new HashMap<>();
      if (data != null) {
        messageData.putAll(data);
      }
      messageData.put("category", category.name());
      messageData.put("timestamp", LocalDateTime.now().toString());
      
      // 메시지 빌드
      Message message = Message.builder()
          .setToken(token)
          .setNotification(notification)
          .setAndroidConfig(androidConfig)
          .setApnsConfig(apnsConfig)
          .putAllData(messageData)
          .build();
      
      // 전송
      String response = firebaseMessaging.send(message);
      log.info("FCM 알림 전송 성공: messageId={}, category={}", response, category.name());
      
      return true;
      
    } catch (FirebaseMessagingException e) {
      log.error("FCM 알림 전송 실패: token={}, error={}", token, e.getMessage());
      handleMessagingError(e, token);
      return false;
    } catch (Exception e) {
      log.error("FCM 알림 전송 중 오류: {}", e.getMessage());
      return false;
    }
  }
  
  /**
   * 여러 디바이스에 동시 전송 (배치)
   * 
   * @param tokens FCM 토큰 리스트
   * @param title 알림 제목
   * @param body 알림 내용
   * @param data 추가 데이터
   * @param category 알림 카테고리
   * @return 전송 결과
   */
  public BatchResponse sendBatchNotification(List<String> tokens, String title, String body,
                                            Map<String, String> data,
                                            NotificationCategory category) {
    if (firebaseMessaging == null || tokens == null || tokens.isEmpty()) {
      log.warn("FCM이 초기화되지 않았거나 토큰이 없습니다");
      return null;
    }
    
    try {
      // 알림 생성
      Notification notification = Notification.builder()
          .setTitle(simplifyTitle(title, category))
          .setBody(simplifyMessage(body))
          .build();
      
      // 메시지 리스트 생성 (최대 500개씩)
      List<Message> messages = tokens.stream()
          .limit(500) // FCM 제한
          .map(token -> Message.builder()
              .setToken(token)
              .setNotification(notification)
              .putAllData(data != null ? data : new HashMap<>())
              .build())
          .collect(Collectors.toList());
      
      // 배치 전송
      BatchResponse response = firebaseMessaging.sendAll(messages);
      
      log.info("FCM 배치 전송 완료: 성공={}, 실패={}", 
          response.getSuccessCount(), response.getFailureCount());
      
      // 실패한 토큰 처리
      if (response.getFailureCount() > 0) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
          if (!responses.get(i).isSuccessful()) {
            String failedToken = tokens.get(i);
            Exception error = responses.get(i).getException();
            log.error("토큰 전송 실패: token={}, error={}", failedToken, error.getMessage());
          }
        }
      }
      
      return response;
      
    } catch (FirebaseMessagingException e) {
      log.error("FCM 배치 전송 실패: {}", e.getMessage());
      return null;
    }
  }
  
  /**
   * 비동기 알림 전송
   * 
   * @param token FCM 토큰
   * @param title 제목
   * @param body 내용
   * @param category 카테고리
   * @return CompletableFuture
   */
  public CompletableFuture<Boolean> sendNotificationAsync(String token, String title, 
                                                          String body, NotificationCategory category) {
    return CompletableFuture.supplyAsync(() -> 
        sendNotification(token, title, body, null, category, Priority.NORMAL)
    );
  }
  
  /**
   * 약물 복용 알림
   * 
   * @param token FCM 토큰
   * @param medicationName 약물명
   * @param time 복용 시간
   * @return 전송 성공 여부
   */
  public boolean sendMedicationReminder(String token, String medicationName, String time) {
    String title = "약 먹을 시간이에요";
    String body = String.format("%s을(를) %s에 드세요", medicationName, time);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "MEDICATION");
    data.put("medicationName", medicationName);
    data.put("time", time);
    
    return sendNotification(token, title, body, data, 
        NotificationCategory.MEDICATION, Priority.HIGH);
  }
  
  /**
   * 일정 알림
   * 
   * @param token FCM 토큰
   * @param scheduleName 일정명
   * @param time 시간
   * @return 전송 성공 여부
   */
  public boolean sendScheduleReminder(String token, String scheduleName, String time) {
    String title = "일정 알림";
    String body = String.format("%s - %s에 있어요", scheduleName, time);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "SCHEDULE");
    data.put("scheduleName", scheduleName);
    data.put("time", time);
    
    return sendNotification(token, title, body, data,
        NotificationCategory.SCHEDULE, Priority.NORMAL);
  }
  
  /**
   * 긴급 알림
   * 
   * @param tokens 보호자 FCM 토큰들
   * @param userName 사용자 이름
   * @param location 현재 위치
   * @return 전송 결과
   */
  public BatchResponse sendEmergencyAlert(List<String> tokens, String userName, String location) {
    String title = "🚨 긴급 상황";
    String body = String.format("%s님이 도움이 필요해요. 위치: %s", userName, location);
    
    Map<String, String> data = new HashMap<>();
    data.put("type", "EMERGENCY");
    data.put("userName", userName);
    data.put("location", location);
    data.put("urgent", "true");
    
    return sendBatchNotification(tokens, title, body, data, NotificationCategory.EMERGENCY);
  }
  
  /**
   * 토큰 유효성 검증
   * 
   * @param token FCM 토큰
   * @return 유효 여부
   */
  public boolean validateToken(String token) {
    if (firebaseMessaging == null || token == null || token.isEmpty()) {
      return false;
    }
    
    try {
      // 드라이런으로 토큰 검증
      Message message = Message.builder()
          .setToken(token)
          .build();
      
      firebaseMessaging.send(message, true); // dryRun = true
      return true;
      
    } catch (Exception e) {
      log.debug("토큰 검증 실패: {}", e.getMessage());
      return false;
    }
  }
  
  /**
   * 메시지 단순화 (BIF 사용자용)
   * 
   * @param message 원본 메시지
   * @return 단순화된 메시지
   */
  private String simplifyMessage(String message) {
    if (message == null) return "";
    
    // 복잡한 단어를 쉬운 단어로 교체
    return message
        .replace("확인하세요", "봐주세요")
        .replace("필요합니다", "해야 해요")
        .replace("완료되었습니다", "끝났어요")
        .replace("시작됩니다", "시작해요");
  }
  
  /**
   * 제목 단순화
   * 
   * @param title 원본 제목
   * @param category 카테고리
   * @return 단순화된 제목
   */
  private String simplifyTitle(String title, NotificationCategory category) {
    if (title == null || title.isEmpty()) {
      return category.getTitle();
    }
    
    // 이모지가 없으면 추가
    if (!title.contains(category.emoji)) {
      return category.emoji + " " + title;
    }
    
    return title;
  }
  
  /**
   * Android 우선순위 매핑
   * 
   * @param priority 우선순위
   * @return Android 우선순위
   */
  private AndroidConfig.Priority mapToAndroidPriority(Priority priority) {
    switch (priority) {
      case HIGH:
        return AndroidConfig.Priority.HIGH;
      case LOW:
        return AndroidConfig.Priority.NORMAL; // Android는 LOW가 없음
      default:
        return AndroidConfig.Priority.NORMAL;
    }
  }
  
  /**
   * 메시징 에러 처리
   * 
   * @param e FirebaseMessagingException
   * @param token 실패한 토큰
   */
  private void handleMessagingError(FirebaseMessagingException e, String token) {
    MessagingErrorCode errorCode = e.getMessagingErrorCode();
    
    if (errorCode == MessagingErrorCode.UNREGISTERED || 
        errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
      // 토큰이 무효화됨 - DB에서 제거 필요
      log.warn("무효한 FCM 토큰 감지: token={}, errorCode={}", token, errorCode);
      // TODO: 토큰 제거 로직 추가
    } else if (errorCode == MessagingErrorCode.QUOTA_EXCEEDED) {
      log.error("FCM 할당량 초과");
    } else {
      log.error("FCM 에러: errorCode={}, message={}", errorCode, e.getMessage());
    }
  }
}