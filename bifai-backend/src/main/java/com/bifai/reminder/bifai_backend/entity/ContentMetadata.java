package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 이미지의 메타데이터를 저장하는 엔티티 (EXIF 데이터 등)
 * BIF 사용자의 개인정보 보호를 위해 필요한 메타데이터만 선별적으로 저장
 */
@Entity
@Table(name = "content_metadata", indexes = {
    @Index(name = "idx_content_metadata_image_id", columnList = "captured_image_id"),
    @Index(name = "idx_content_metadata_device_model", columnList = "device_model"),
    @Index(name = "idx_content_metadata_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"capturedImage"})
public class ContentMetadata extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 이미지
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_image_id", nullable = false)
    @NotNull
    private CapturedImage capturedImage;

    /**
     * 촬영 기기 제조사
     */
    @Column(name = "device_make", length = 100)
    private String deviceMake;

    /**
     * 촬영 기기 모델명
     */
    @Column(name = "device_model", length = 100)
    private String deviceModel;

    /**
     * 촬영 소프트웨어 (카메라 앱)
     */
    @Column(name = "software", length = 100)
    private String software;

    /**
     * 원본 촬영 시간 (EXIF에서 추출)
     */
    @Column(name = "original_datetime")
    private LocalDateTime originalDateTime;

    /**
     * GPS 정보 유무 (개인정보 보호를 위한 플래그)
     */
    @Column(name = "has_gps_info")
    private Boolean hasGpsInfo = false;

    /**
     * 이미지 방향 정보 (회전)
     */
    @Column(name = "orientation")
    private Integer orientation;

    /**
     * 플래시 사용 여부
     */
    @Column(name = "flash_used")
    private Boolean flashUsed;

    /**
     * 화이트 밸런스 설정
     */
    @Column(name = "white_balance", length = 50)
    private String whiteBalance;

    /**
     * 색상 공간 정보
     */
    @Column(name = "color_space", length = 50)
    private String colorSpace;

    /**
     * BIF 사용자에게 안전한 추가 메타데이터 (개인정보 제외)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_metadata", columnDefinition = "JSON")
    private Map<String, Object> safeMetadata;

    /**
     * 이미지 품질 점수 (1-100, BIF 사용자의 이해를 위해)
     */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /**
     * 흐림 정도 (0.0-1.0, 0에 가까울수록 선명)
     */
    @Column(name = "blur_level")
    private Double blurLevel;

    /**
     * 밝기 수준 (0.0-1.0)
     */
    @Column(name = "brightness_level")
    private Double brightnessLevel;

    /**
     * 대비 수준 (0.0-1.0)
     */
    @Column(name = "contrast_level")
    private Double contrastLevel;

    /**
     * 메타데이터 추출 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 30)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    /**
     * 메타데이터 추출 시 발생한 오류 메시지
     */
    @Column(name = "extraction_error", columnDefinition = "TEXT")
    private String extractionError;

    /**
     * 메타데이터 추출 상태 열거형
     */
    public enum ExtractionStatus {
        PENDING("추출 대기"),
        PROCESSING("추출 중"),
        COMPLETED("추출 완료"),
        FAILED("추출 실패"),
        NO_METADATA("메타데이터 없음");

        private final String description;

        ExtractionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public ContentMetadata(CapturedImage capturedImage) {
        this.capturedImage = capturedImage;
    }

    /**
     * 메타데이터 추출 성공 시 결과 설정
     */
    public void setExtractionSuccess() {
        this.extractionStatus = ExtractionStatus.COMPLETED;
    }

    /**
     * 메타데이터 추출 실패 시 오류 설정
     */
    public void setExtractionFailure(String errorMessage) {
        this.extractionStatus = ExtractionStatus.FAILED;
        this.extractionError = errorMessage;
    }

    /**
     * 좋은 품질의 이미지인지 확인 (품질 점수 70 이상)
     */
    public boolean isGoodQuality() {
        return qualityScore != null && qualityScore >= 70;
    }

    /**
     * 이미지가 너무 흐린지 확인 (흐림 정도 0.7 이상)
     */
    public boolean isTooBlurry() {
        return blurLevel != null && blurLevel >= 0.7;
    }

    /**
     * 이미지가 너무 어두운지 확인 (밝기 0.3 미만)
     */
    public boolean isTooDark() {
        return brightnessLevel != null && brightnessLevel < 0.3;
    }

    /**
     * 이미지가 너무 밝은지 확인 (밝기 0.9 초과)
     */
    public boolean isTooBright() {
        return brightnessLevel != null && brightnessLevel > 0.9;
    }

    /**
     * BIF 사용자를 위한 이미지 품질 피드백 생성
     */
    public String getQualityFeedbackForBifUser() {
        if (isGoodQuality()) {
            return "좋은 품질의 사진입니다.";
        }
        
        if (isTooBlurry()) {
            return "사진이 흐려요. 다시 찍어보세요.";
        }
        
        if (isTooDark()) {
            return "사진이 너무 어둡습니다. 밝은 곳에서 찍어보세요.";
        }
        
        if (isTooBright()) {
            return "사진이 너무 밝습니다. 조명을 조절해보세요.";
        }
        
        return "사진을 더 선명하게 찍어보세요.";
    }
} 