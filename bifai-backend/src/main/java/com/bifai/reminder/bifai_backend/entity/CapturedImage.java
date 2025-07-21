package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BIF 사용자가 촬영한 이미지 정보를 저장하는 엔티티
 * 개인정보 보호를 위해 soft delete 지원
 */
@Entity
@Table(name = "captured_images", indexes = {
    @Index(name = "idx_captured_image_user_id", columnList = "user_id"),
    @Index(name = "idx_captured_image_captured_at", columnList = "captured_at"),
    @Index(name = "idx_captured_image_image_type", columnList = "image_type"),
    @Index(name = "idx_captured_image_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user"})
public class CapturedImage extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이미지를 촬영한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    /**
     * S3에 저장된 원본 이미지 URL
     */
    @Column(name = "original_url", nullable = false, length = 1000)
    @NotBlank
    private String originalUrl;

    /**
     * S3에 저장된 썸네일 이미지 URL (BIF 사용자의 빠른 인식을 위해)
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * 이미지 촬영 위치 위도
     */
    @Column(name = "gps_latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * 이미지 촬영 위치 경도
     */
    @Column(name = "gps_longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * 이미지 촬영 시각
     */
    @Column(name = "captured_at", nullable = false)
    @NotNull
    private LocalDateTime capturedAt;

    /**
     * 이미지 타입 (약물, 음식, 메모 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    @NotNull
    private ImageType imageType;

    /**
     * 이미지 파일 크기 (바이트)
     */
    @Column(name = "file_size")
    @PositiveOrZero
    private Long fileSize;

    /**
     * MIME 타입 (image/jpeg, image/png 등)
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 이미지 가로 크기 (픽셀)
     */
    @Column(name = "width")
    @PositiveOrZero
    private Integer width;

    /**
     * 이미지 세로 크기 (픽셀)
     */
    @Column(name = "height")
    @PositiveOrZero
    private Integer height;

    /**
     * BIF 사용자를 위한 간단한 설명
     */
    @Column(name = "simple_description", length = 200)
    private String simpleDescription;

    /**
     * 이미지 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    /**
     * soft delete를 위한 플래그
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * 삭제 시간
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 이미지 타입 열거형
     */
    public enum ImageType {
        MEDICATION("약물"),
        FOOD("음식"),
        MEMO("메모"),
        EMERGENCY("응급상황"),
        DAILY_LIFE("일상생활"),
        EXERCISE("운동"),
        OTHER("기타");

        private final String description;

        ImageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 이미지 처리 상태 열거형
     */
    public enum ProcessingStatus {
        PENDING("처리 대기"),
        PROCESSING("처리 중"),
        COMPLETED("처리 완료"),
        FAILED("처리 실패");

        private final String description;

        ProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 이미지를 soft delete 처리
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 생성자 (필수 필드)
     */
    public CapturedImage(User user, String originalUrl, ImageType imageType, LocalDateTime capturedAt) {
        this.user = user;
        this.originalUrl = originalUrl;
        this.imageType = imageType;
        this.capturedAt = capturedAt;
    }
} 