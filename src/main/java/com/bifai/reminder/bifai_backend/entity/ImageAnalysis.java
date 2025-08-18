package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이미지 분석 엔티티
 * 웨어러블 카메라로 캡처한 이미지의 AI 분석 결과 저장
 */
@Entity
@Table(name = "image_analyses", indexes = {
    @Index(name = "idx_analysis_user", columnList = "user_id"),
    @Index(name = "idx_analysis_created", columnList = "created_at"),
    @Index(name = "idx_analysis_type", columnList = "analysis_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageAnalysis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "analysis_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "image_url", nullable = false, length = 500)
  private String imageUrl; // S3 URL

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Column(name = "file_size")
  private Long fileSize; // bytes

  @Column(name = "image_width")
  private Integer imageWidth;

  @Column(name = "image_height")
  private Integer imageHeight;

  @Column(name = "analysis_type", length = 50)
  @Enumerated(EnumType.STRING)
  private AnalysisType analysisType;

  @Column(name = "location_latitude")
  private Double latitude;

  @Column(name = "location_longitude")
  private Double longitude;

  @Column(name = "location_address", length = 500)
  private String address;

  // 객체 인식 결과 (YOLOv8)
  @Column(name = "detected_objects", columnDefinition = "JSON")
  private String detectedObjects; // [{"label":"person","confidence":0.95,"bbox":[x,y,w,h]}]

  @Column(name = "object_count")
  private Integer objectCount;

  // 텍스트 추출 결과 (OCR)
  @Column(name = "extracted_text", columnDefinition = "TEXT")
  private String extractedText;

  @Column(name = "text_language", length = 10)
  private String textLanguage;

  // 상황 해석 (OpenAI)
  @Column(name = "situation_description", columnDefinition = "TEXT")
  private String situationDescription; // 사용자에게 전달할 상황 설명

  @Column(name = "action_suggestion", columnDefinition = "TEXT")
  private String actionSuggestion; // 추천 행동

  @Column(name = "safety_level", length = 20)
  @Enumerated(EnumType.STRING)
  private SafetyLevel safetyLevel;

  @Column(name = "requires_attention")
  private Boolean requiresAttention;

  @Column(name = "emergency_detected")
  private Boolean emergencyDetected;

  // 분석 메타데이터
  @Column(name = "processing_time_ms")
  private Long processingTimeMs; // 처리 시간 (ms)

  @Column(name = "ai_confidence_score")
  private Float aiConfidenceScore;

  @Column(name = "analysis_status", length = 30)
  @Enumerated(EnumType.STRING)
  private AnalysisStatus analysisStatus;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "guardian_notified")
  private Boolean guardianNotified;

  @Column(name = "voice_guidance_sent")
  private Boolean voiceGuidanceSent;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "analyzed_at")
  private LocalDateTime analyzedAt;

  /**
   * 분석 타입
   */
  public enum AnalysisType {
    PERIODIC("주기적"),      // 정기 캡처
    ON_DEMAND("요청"),     // 사용자 요청
    EMERGENCY("긴급"),      // 긴급 상황
    NAVIGATION("길안내"),   // 내비게이션 중
    MEDICATION("약품인식"), // 약 확인
    TEXT_READING("텍스트"); // 문서 읽기

    private final String description;

    AnalysisType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 안전 수준
   */
  public enum SafetyLevel {
    SAFE("안전"),
    CAUTION("주의"),
    WARNING("경고"),
    DANGER("위험");

    private final String description;

    SafetyLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 분석 상태
   */
  public enum AnalysisStatus {
    UPLOADED("업로드됨"),
    QUEUED("대기중"),
    PROCESSING("처리중"),
    COMPLETED("완료"),
    FAILED("실패"),
    PARTIAL("부분완료");

    private final String description;

    AnalysisStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (analysisStatus == null) {
      analysisStatus = AnalysisStatus.UPLOADED;
    }
    if (requiresAttention == null) {
      requiresAttention = false;
    }
    if (emergencyDetected == null) {
      emergencyDetected = false;
    }
    if (guardianNotified == null) {
      guardianNotified = false;
    }
    if (voiceGuidanceSent == null) {
      voiceGuidanceSent = false;
    }
  }

  /**
   * 분석 시작
   */
  public void startProcessing() {
    this.analysisStatus = AnalysisStatus.PROCESSING;
  }

  /**
   * 분석 완료
   */
  public void completeAnalysis() {
    this.analysisStatus = AnalysisStatus.COMPLETED;
    this.analyzedAt = LocalDateTime.now();
    if (this.createdAt != null) {
      this.processingTimeMs = 
          java.time.Duration.between(this.createdAt, this.analyzedAt).toMillis();
    }
  }

  /**
   * 분석 실패
   */
  public void failAnalysis(String error) {
    this.analysisStatus = AnalysisStatus.FAILED;
    this.errorMessage = error;
  }

  /**
   * 긴급 상황 표시
   */
  public void markAsEmergency() {
    this.emergencyDetected = true;
    this.requiresAttention = true;
    this.safetyLevel = SafetyLevel.DANGER;
  }
}