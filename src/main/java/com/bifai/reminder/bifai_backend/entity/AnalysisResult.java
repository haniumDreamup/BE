package com.bifai.reminder.bifai_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 이미지 분석 결과를 저장하는 엔티티
 * BIF 사용자를 위한 간소화된 결과와 상세 결과를 모두 저장
 */
@Entity
@Table(name = "analysis_results", indexes = {
    @Index(name = "idx_analysis_result_image_id", columnList = "captured_image_id"),
    @Index(name = "idx_analysis_result_analysis_type", columnList = "analysis_type"),
    @Index(name = "idx_analysis_result_confidence", columnList = "confidence_score"),
    @Index(name = "idx_analysis_result_analyzed_at", columnList = "analyzed_at")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"capturedImage"})
public class AnalysisResult extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 분석 대상 이미지
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_image_id", nullable = false)
    @NotNull
    private CapturedImage capturedImage;

    /**
     * 분석 타입 (약물 인식, 음식 분석, 텍스트 추출 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 50)
    @NotNull
    private AnalysisType analysisType;

    /**
     * BIF 사용자를 위한 간단하고 명확한 결과 요약
     */
    @Column(name = "simplified_result", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String simplifiedResult;

    /**
     * AI 모델이 제공한 상세 분석 결과 (JSON 형태)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_result", columnDefinition = "JSON")
    private Map<String, Object> detailedResult;

    /**
     * 분석 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "confidence_score", precision = 5)
    @PositiveOrZero
    private BigDecimal confidenceScore;

    /**
     * 분석 완료 시간
     */
    @Column(name = "analyzed_at", nullable = false)
    @NotNull
    private LocalDateTime analyzedAt;

    /**
     * 사용된 AI 모델 정보
     */
    @Column(name = "model_version", length = 100)
    private String modelVersion;

    /**
     * 분석 처리 시간 (밀리초)
     */
    @Column(name = "processing_time_ms")
    @PositiveOrZero
    private Long processingTimeMs;

    /**
     * 오류 발생 시 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * BIF 사용자를 위한 권장 행동
     */
    @Column(name = "recommended_action", length = 500)
    private String recommendedAction;

    /**
     * 분석 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnalysisStatus status = AnalysisStatus.PROCESSING;

    /**
     * 분석 타입 열거형
     */
    public enum AnalysisType {
        MEDICATION_RECOGNITION("약물 인식"),
        FOOD_ANALYSIS("음식 분석"),
        TEXT_EXTRACTION("텍스트 추출"),
        OBJECT_DETECTION("물체 인식"),
        SCENE_ANALYSIS("상황 분석"),
        EMERGENCY_DETECTION("응급상황 감지"),
        EXERCISE_TRACKING("운동 추적");

        private final String description;

        AnalysisType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 분석 상태 열거형
     */
    public enum AnalysisStatus {
        PROCESSING("분석 중"),
        COMPLETED("분석 완료"),
        FAILED("분석 실패"),
        REQUIRES_REVIEW("검토 필요");

        private final String description;

        AnalysisStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public AnalysisResult(CapturedImage capturedImage, AnalysisType analysisType, 
                         String simplifiedResult, LocalDateTime analyzedAt) {
        this.capturedImage = capturedImage;
        this.analysisType = analysisType;
        this.simplifiedResult = simplifiedResult;
        this.analyzedAt = analyzedAt;
    }

    /**
     * 분석 성공 시 결과 설정
     */
    public void setSuccessResult(String simplifiedResult, Map<String, Object> detailedResult, 
                                BigDecimal confidenceScore, String recommendedAction) {
        this.simplifiedResult = simplifiedResult;
        this.detailedResult = detailedResult;
        this.confidenceScore = confidenceScore;
        this.recommendedAction = recommendedAction;
        this.status = AnalysisStatus.COMPLETED;
        this.analyzedAt = LocalDateTime.now();
    }

    /**
     * 분석 실패 시 오류 설정
     */
    public void setFailureResult(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = AnalysisStatus.FAILED;
        this.analyzedAt = LocalDateTime.now();
    }

    /**
     * 높은 신뢰도 여부 확인 (0.8 이상)
     */
    public boolean isHighConfidence() {
        return confidenceScore != null && 
               confidenceScore.compareTo(new BigDecimal("0.8")) >= 0;
    }

    /**
     * 낮은 신뢰도 여부 확인 (0.5 미만)
     */
    public boolean isLowConfidence() {
        return confidenceScore != null && 
               confidenceScore.compareTo(new BigDecimal("0.5")) < 0;
    }
} 