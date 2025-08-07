package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 템플릿 엔티티
 * 상황별 맞춤 알림 메시지 템플릿 관리
 */
@Entity
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_type", columnList = "template_type"),
    @Index(name = "idx_template_event", columnList = "event_type"),
    @Index(name = "idx_template_active", columnList = "is_active"),
    @Index(name = "idx_template_language", columnList = "language_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "template_id")
  private Long id;

  @Column(name = "template_code", nullable = false, unique = true, length = 50)
  private String templateCode;

  @Column(name = "template_name", nullable = false, length = 100)
  private String templateName;

  @Column(name = "template_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private TemplateType templateType;

  @Column(name = "event_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private EventType eventType;

  @Column(name = "severity_level", length = 20)
  @Enumerated(EnumType.STRING)
  private SeverityLevel severityLevel;

  @Column(name = "language_code", nullable = false, length = 10)
  private String languageCode;

  @Column(name = "title_template", nullable = false, length = 200)
  private String titleTemplate;

  @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
  private String bodyTemplate;

  @Column(name = "sms_template", columnDefinition = "TEXT")
  private String smsTemplate;

  @Column(name = "email_subject_template", length = 200)
  private String emailSubjectTemplate;

  @Column(name = "email_body_template", columnDefinition = "TEXT")
  private String emailBodyTemplate;

  @Column(name = "push_title_template", length = 100)
  private String pushTitleTemplate;

  @Column(name = "push_body_template", length = 500)
  private String pushBodyTemplate;

  @Column(name = "action_url", length = 500)
  private String actionUrl;

  @Column(name = "action_label", length = 50)
  private String actionLabel;

  @Column(name = "icon_type", length = 30)
  private String iconType;

  @Column(name = "sound_type", length = 30)
  private String soundType;

  @Column(name = "priority", nullable = false)
  private Integer priority;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "requires_immediate_action")
  private Boolean requiresImmediateAction;

  @Column(name = "escalation_minutes")
  private Integer escalationMinutes;

  @Column(name = "max_retry_count")
  private Integer maxRetryCount;

  @Column(name = "retry_interval_seconds")
  private Integer retryIntervalSeconds;

  @Column(name = "variables", columnDefinition = "JSON")
  private String variables; // JSON 형태로 필요한 변수 목록 저장

  @Column(name = "tags", length = 200)
  private String tags;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "usage_count")
  private Long usageCount;

  @Column(name = "last_used_at")
  private LocalDateTime lastUsedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  /**
   * 템플릿 타입
   */
  public enum TemplateType {
    SMS("문자"),
    EMAIL("이메일"),
    PUSH("푸시"),
    IN_APP("인앱"),
    MULTI_CHANNEL("멀티채널");

    private final String description;

    TemplateType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 이벤트 타입
   */
  public enum EventType {
    // 긴급 상황
    FALL_DETECTED("낙상 감지"),
    EMERGENCY_TRIGGERED("긴급 상황 발생"),
    PANIC_BUTTON("긴급 버튼"),
    NO_RESPONSE("응답 없음"),
    EMERGENCY_RESOLVED("긴급 상황 해결"),
    
    // 위치 관련
    GEOFENCE_ENTRY("안전구역 진입"),
    GEOFENCE_EXIT("안전구역 이탈"),
    LOCATION_UNKNOWN("위치 불명"),
    WANDERING_DETECTED("배회 감지"),
    
    // 건강 관련
    MEDICATION_REMINDER("약 복용 알림"),
    MEDICATION_MISSED("약 복용 놓침"),
    HEALTH_CHECK_REMINDER("건강 체크 알림"),
    ABNORMAL_VITAL("비정상 바이탈"),
    
    // 일정 관련
    APPOINTMENT_REMINDER("일정 알림"),
    SCHEDULE_CHANGE("일정 변경"),
    DAILY_ROUTINE("일상 루틴"),
    
    // 시스템
    DEVICE_LOW_BATTERY("배터리 부족"),
    DEVICE_DISCONNECTED("기기 연결 끊김"),
    SYSTEM_ALERT("시스템 알림"),
    
    // 보호자 관련
    GUARDIAN_REQUEST("보호자 요청"),
    GUARDIAN_APPROVED("보호자 승인"),
    GUARDIAN_MESSAGE("보호자 메시지"),
    
    // 기타
    DAILY_REPORT("일일 리포트"),
    WEEKLY_SUMMARY("주간 요약"),
    CUSTOM("사용자 정의");

    private final String description;

    EventType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 심각도 레벨
   */
  public enum SeverityLevel {
    CRITICAL("매우 위급"),
    HIGH("높음"),
    MEDIUM("중간"),
    LOW("낮음"),
    INFO("정보");

    private final String description;

    SeverityLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (isActive == null) {
      isActive = true;
    }
    if (priority == null) {
      priority = 5;
    }
    if (languageCode == null) {
      languageCode = "ko";
    }
    if (usageCount == null) {
      usageCount = 0L;
    }
    if (maxRetryCount == null) {
      maxRetryCount = 3;
    }
    if (retryIntervalSeconds == null) {
      retryIntervalSeconds = 60;
    }
    if (requiresImmediateAction == null) {
      requiresImmediateAction = false;
    }
  }

  /**
   * 템플릿 사용 기록
   */
  public void recordUsage() {
    this.usageCount = (this.usageCount == null ? 0L : this.usageCount) + 1;
    this.lastUsedAt = LocalDateTime.now();
  }

  /**
   * 변수 치환된 메시지 생성
   */
  public String renderMessage(String template, Map<String, String> variables) {
    if (template == null) return "";
    
    String rendered = template;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      rendered = rendered.replace(placeholder, entry.getValue());
    }
    return rendered;
  }

  /**
   * 제목 렌더링
   */
  public String renderTitle(Map<String, String> variables) {
    return renderMessage(titleTemplate, variables);
  }

  /**
   * 본문 렌더링
   */
  public String renderBody(Map<String, String> variables) {
    return renderMessage(bodyTemplate, variables);
  }

  /**
   * SMS 메시지 렌더링
   */
  public String renderSms(Map<String, String> variables) {
    if (smsTemplate != null) {
      return renderMessage(smsTemplate, variables);
    }
    // SMS 템플릿이 없으면 기본 템플릿 사용
    return renderMessage(bodyTemplate, variables);
  }

  /**
   * 이메일 제목 렌더링
   */
  public String renderEmailSubject(Map<String, String> variables) {
    if (emailSubjectTemplate != null) {
      return renderMessage(emailSubjectTemplate, variables);
    }
    return renderTitle(variables);
  }

  /**
   * 이메일 본문 렌더링
   */
  public String renderEmailBody(Map<String, String> variables) {
    if (emailBodyTemplate != null) {
      return renderMessage(emailBodyTemplate, variables);
    }
    return renderBody(variables);
  }

  /**
   * 푸시 알림 제목 렌더링
   */
  public String renderPushTitle(Map<String, String> variables) {
    if (pushTitleTemplate != null) {
      return renderMessage(pushTitleTemplate, variables);
    }
    return renderTitle(variables);
  }

  /**
   * 푸시 알림 본문 렌더링
   */
  public String renderPushBody(Map<String, String> variables) {
    if (pushBodyTemplate != null) {
      return renderMessage(pushBodyTemplate, variables);
    }
    return renderBody(variables);
  }

  /**
   * 즉시 처리 필요 여부
   */
  public boolean isUrgent() {
    return severityLevel == SeverityLevel.CRITICAL || 
           Boolean.TRUE.equals(requiresImmediateAction);
  }

  /**
   * 에스컬레이션 필요 여부
   */
  public boolean needsEscalation() {
    return escalationMinutes != null && escalationMinutes > 0;
  }
}