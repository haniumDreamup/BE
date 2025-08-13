package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 행동 로그 엔티티
 * BIF 사용자의 모든 인터랙션을 추적하고 분석하기 위한 데이터를 저장
 */
@Entity
@Table(name = "user_behavior_logs", indexes = {
  @Index(name = "idx_user_behavior_log_user_id", columnList = "user_id"),
  @Index(name = "idx_user_behavior_log_session_id", columnList = "session_id"),
  @Index(name = "idx_user_behavior_log_timestamp", columnList = "timestamp"),
  @Index(name = "idx_user_behavior_log_action_type", columnList = "action_type"),
  @Index(name = "idx_user_behavior_log_composite", columnList = "user_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehaviorLog {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "session_id", nullable = false, length = 100)
  private String sessionId;
  
  @Column(name = "action_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ActionType actionType;
  
  @Column(name = "action_detail", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> actionDetail = new HashMap<>();
  
  @Column(name = "device_info", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> deviceInfo = new HashMap<>();
  
  @Column(name = "page_url", length = 500)
  private String pageUrl;
  
  @Column(name = "referrer_url", length = 500)
  private String referrerUrl;
  
  @Column(name = "response_time_ms")
  private Integer responseTimeMs;
  
  @Column(name = "ip_address", length = 45)
  private String ipAddress;
  
  @Column(name = "user_agent", length = 500)
  private String userAgent;
  
  @CreationTimestamp
  @Column(name = "timestamp", nullable = false, updatable = false)
  private LocalDateTime timestamp;
  
  @Column(name = "log_level", length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private LogLevel logLevel = LogLevel.INFO;
  
  /**
   * 액션 타입 열거형
   */
  public enum ActionType {
    // 페이지 관련
    PAGE_VIEW("페이지 조회"),
    PAGE_EXIT("페이지 이탈"),
    
    // 클릭 관련
    BUTTON_CLICK("버튼 클릭"),
    LINK_CLICK("링크 클릭"),
    IMAGE_CLICK("이미지 클릭"),
    
    // 폼 관련
    FORM_START("폼 작성 시작"),
    FORM_SUBMIT("폼 제출"),
    FORM_ERROR("폼 오류"),
    FORM_ABANDON("폼 이탈"),
    
    // 내비게이션
    NAVIGATION("내비게이션"),
    SEARCH("검색"),
    FILTER("필터링"),
    SORT("정렬"),
    
    // 미디어
    VIDEO_PLAY("비디오 재생"),
    VIDEO_PAUSE("비디오 일시정지"),
    VIDEO_COMPLETE("비디오 완료"),
    AUDIO_PLAY("오디오 재생"),
    
    // 인터랙션
    SCROLL("스크롤"),
    SWIPE("스와이프"),
    LONG_PRESS("롱프레스"),
    DOUBLE_TAP("더블탭"),
    
    // 시스템
    APP_START("앱 시작"),
    APP_BACKGROUND("백그라운드 전환"),
    APP_FOREGROUND("포그라운드 전환"),
    APP_CRASH("앱 크래시"),
    
    // 기능 사용
    FEATURE_USE("기능 사용"),
    SETTING_CHANGE("설정 변경"),
    NOTIFICATION_RECEIVE("알림 수신"),
    NOTIFICATION_CLICK("알림 클릭"),
    
    // 오류
    ERROR("오류 발생"),
    WARNING("경고 발생");
    
    private final String description;
    
    ActionType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 로그 레벨 열거형
   */
  public enum LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
  }
  
  /**
   * 민감 정보 마스킹
   */
  public void maskSensitiveData() {
    if (this.ipAddress != null && this.ipAddress.length() > 0) {
      // IP 주소 마지막 옥텟 마스킹
      String[] parts = this.ipAddress.split("\\.");
      if (parts.length == 4) {
        this.ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + ".xxx";
      }
    }
    
    if (this.actionDetail != null) {
      // 민감한 필드 제거
      this.actionDetail.remove("password");
      this.actionDetail.remove("email");
      this.actionDetail.remove("phoneNumber");
      this.actionDetail.remove("address");
    }
  }
}