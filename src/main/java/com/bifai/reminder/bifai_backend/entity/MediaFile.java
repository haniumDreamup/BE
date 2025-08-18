package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 미디어 파일 엔티티
 * 
 * 사용자가 업로드한 이미지/비디오 파일 정보 관리
 */
@Entity
@Table(name = "media_files", indexes = {
    @Index(name = "idx_media_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_media_type_created", columnList = "upload_type, created_at DESC"),
    @Index(name = "idx_media_id", columnList = "media_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MediaFile extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(name = "media_id", nullable = false, unique = true, length = 50)
  private String mediaId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "upload_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private UploadType uploadType;
  
  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;
  
  @Column(name = "original_name", length = 255)
  private String originalName;
  
  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;
  
  @Column(name = "file_size", nullable = false)
  private Long fileSize;
  
  @Column(name = "s3_key", nullable = false, length = 500)
  private String s3Key;
  
  @Column(name = "s3_bucket", nullable = false, length = 100)
  private String s3Bucket;
  
  @Column(name = "url", nullable = false, length = 1000)
  private String url;
  
  @Column(name = "thumbnail_url", length = 1000)
  private String thumbnailUrl;
  
  @Column(name = "cdn_url", length = 1000)
  private String cdnUrl;
  
  // 이미지 메타데이터
  @Column(name = "width")
  private Integer width;
  
  @Column(name = "height")
  private Integer height;
  
  // 비디오 메타데이터
  @Column(name = "duration")
  private Integer duration; // 초 단위
  
  @Column(name = "frame_rate")
  private Float frameRate;
  
  @Column(name = "upload_status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private UploadStatus uploadStatus = UploadStatus.PENDING;
  
  @Column(name = "upload_id", length = 100)
  private String uploadId;
  
  @Column(name = "uploaded_at")
  private LocalDateTime uploadedAt;
  
  @Column(name = "processed_at")
  private LocalDateTime processedAt;
  
  @Column(name = "etag", length = 100)
  private String etag;
  
  @Column(columnDefinition = "JSON")
  private String metadata; // 추가 메타데이터 JSON
  
  @Column(name = "is_deleted")
  @Builder.Default
  private Boolean isDeleted = false;
  
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
  
  // 관련 엔티티 참조
  @Column(name = "related_entity_id")
  private Long relatedEntityId;
  
  @Column(name = "related_entity_type", length = 50)
  private String relatedEntityType;
  
  /**
   * 업로드 타입
   */
  public enum UploadType {
    PROFILE("프로필 사진"),
    MEDICATION("약물 사진"),
    ACTIVITY("활동 사진/영상"),
    DOCUMENT("문서"),
    HEALTH("건강 기록"),
    EMERGENCY("긴급 상황");
    
    private final String description;
    
    UploadType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 업로드 상태
   */
  public enum UploadStatus {
    PENDING("대기중"),
    UPLOADING("업로드중"),
    UPLOADED("업로드됨"),
    PROCESSING("처리중"),
    COMPLETED("완료"),
    FAILED("실패"),
    DELETED("삭제됨");
    
    private final String description;
    
    UploadStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 업로드 완료 처리
   */
  public void markAsUploaded(String etag) {
    this.uploadStatus = UploadStatus.UPLOADED;
    this.uploadedAt = LocalDateTime.now();
    this.etag = etag;
  }
  
  /**
   * 처리 완료
   */
  public void markAsProcessed() {
    this.uploadStatus = UploadStatus.COMPLETED;
    this.processedAt = LocalDateTime.now();
  }
  
  /**
   * 실패 처리
   */
  public void markAsFailed() {
    this.uploadStatus = UploadStatus.FAILED;
  }
  
  /**
   * 소프트 삭제
   */
  public void softDelete() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
    this.uploadStatus = UploadStatus.DELETED;
  }
  
  /**
   * 이미지 파일 여부
   */
  public boolean isImage() {
    return mimeType != null && mimeType.startsWith("image/");
  }
  
  /**
   * 비디오 파일 여부
   */
  public boolean isVideo() {
    return mimeType != null && mimeType.startsWith("video/");
  }
}