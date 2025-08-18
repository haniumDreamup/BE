package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.*;
import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.MediaFile.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.MediaFileRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 미디어 파일 서비스
 * 
 * 이미지/비디오 업로드 및 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {
  
  private final MediaFileRepository mediaFileRepository;
  private final UserRepository userRepository;
  private final MobileS3Service s3Service;
  private final ObjectMapper objectMapper;
  
  @Value("${aws.s3.bucket-name:bifai-media}")
  private String bucketName;
  
  @Value("${aws.cloudfront.url:}")
  private String cloudFrontUrl;
  
  private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
  private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
  private static final long MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB
  private static final int PART_SIZE = 5 * 1024 * 1024; // 5MB per part
  
  /**
   * Presigned URL 생성 (단일 업로드)
   * 
   * @param userId 사용자 ID
   * @param request 업로드 요청
   * @return Presigned URL 응답
   */
  public PresignedUrlResponse generatePresignedUrl(Long userId, MediaUploadRequest request) {
    log.info("Generating presigned URL: userId={}, fileName={}, fileSize={}", 
        userId, request.getFileName(), request.getFileSize());
    
    // 파일 크기 검증
    validateFileSize(request);
    
    // 사용자 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없어요"));
    
    // 업로드 제한 확인
    checkUploadLimits(userId);
    
    // 미디어 파일 엔티티 생성
    String mediaId = generateMediaId();
    String uploadId = generateUploadId();
    String s3Key = generateS3Key(userId, request.getUploadType(), request.getFileName());
    
    MediaFile mediaFile = MediaFile.builder()
        .mediaId(mediaId)
        .user(user)
        .uploadType(request.getUploadType())
        .fileName(request.getFileName())
        .originalName(request.getFileName())
        .mimeType(request.getFileType())
        .fileSize(request.getFileSize())
        .s3Key(s3Key)
        .s3Bucket(bucketName)
        .url(s3Service.generatePublicUrl(s3Key))
        .uploadStatus(UploadStatus.PENDING)
        .uploadId(uploadId)
        .metadata(convertMetadataToJson(request.getMetadata()))
        .build();
    
    mediaFileRepository.save(mediaFile);
    
    // Presigned URL 생성
    Map<String, String> s3Metadata = new HashMap<>();
    s3Metadata.put("userId", userId.toString());
    s3Metadata.put("mediaId", mediaId);
    s3Metadata.put("uploadType", request.getUploadType().name());
    
    URL presignedUrl = s3Service.generatePresignedPutUrl(s3Key, request.getFileType(), s3Metadata);
    
    // 응답 생성
    return PresignedUrlResponse.builder()
        .uploadUrl(presignedUrl.toString())
        .uploadId(uploadId)
        .mediaId(mediaId)
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .maxSize(getMaxFileSize(request))
        .uploadMethod("PUT")
        .headers(Map.of("Content-Type", request.getFileType()))
        .build();
  }
  
  /**
   * 멀티파트 업로드 시작
   * 
   * @param userId 사용자 ID
   * @param request 업로드 요청
   * @return Presigned URL 응답 (파트 정보 포함)
   */
  public PresignedUrlResponse initiateMultipartUpload(Long userId, MediaUploadRequest request) {
    log.info("Initiating multipart upload: userId={}, fileName={}, fileSize={}", 
        userId, request.getFileName(), request.getFileSize());
    
    // 파일 크기 검증
    validateFileSize(request);
    
    // 사용자 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없어요"));
    
    // S3 키 생성
    String mediaId = generateMediaId();
    String s3Key = generateS3Key(userId, request.getUploadType(), request.getFileName());
    
    // 멀티파트 업로드 시작
    Map<String, String> s3Metadata = new HashMap<>();
    s3Metadata.put("userId", userId.toString());
    s3Metadata.put("mediaId", mediaId);
    
    String uploadId = s3Service.initiateMultipartUpload(s3Key, request.getFileType(), s3Metadata);
    
    // 미디어 파일 엔티티 생성
    MediaFile mediaFile = MediaFile.builder()
        .mediaId(mediaId)
        .user(user)
        .uploadType(request.getUploadType())
        .fileName(request.getFileName())
        .originalName(request.getFileName())
        .mimeType(request.getFileType())
        .fileSize(request.getFileSize())
        .s3Key(s3Key)
        .s3Bucket(bucketName)
        .url(s3Service.generatePublicUrl(s3Key))
        .uploadStatus(UploadStatus.UPLOADING)
        .uploadId(uploadId)
        .metadata(convertMetadataToJson(request.getMetadata()))
        .build();
    
    mediaFileRepository.save(mediaFile);
    
    // 파트 정보 생성
    int totalParts = (int) Math.ceil((double) request.getFileSize() / PART_SIZE);
    List<PresignedUrlResponse.PartUploadInfo> parts = new ArrayList<>();
    
    for (int i = 1; i <= totalParts; i++) {
      long startByte = (i - 1) * PART_SIZE;
      long endByte = Math.min(i * PART_SIZE - 1, request.getFileSize() - 1);
      
      URL partUrl = s3Service.generatePresignedUploadPartUrl(s3Key, uploadId, i);
      
      parts.add(PresignedUrlResponse.PartUploadInfo.builder()
          .partNumber(i)
          .uploadUrl(partUrl.toString())
          .startByte(startByte)
          .endByte(endByte)
          .build());
    }
    
    return PresignedUrlResponse.builder()
        .uploadId(uploadId)
        .mediaId(mediaId)
        .partSize(PART_SIZE)
        .totalParts(totalParts)
        .parts(parts)
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .build();
  }
  
  /**
   * 업로드 완료 확인
   * 
   * @param userId 사용자 ID
   * @param mediaId 미디어 ID
   * @param uploadId 업로드 ID
   * @param etag ETag
   * @return 미디어 응답
   */
  public MediaResponse completeUpload(Long userId, String mediaId, String uploadId, String etag) {
    log.info("Completing upload: userId={}, mediaId={}, uploadId={}", userId, mediaId, uploadId);
    
    MediaFile mediaFile = mediaFileRepository.findByMediaIdAndUser_UserId(mediaId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("파일을 찾을 수 없어요"));
    
    if (!uploadId.equals(mediaFile.getUploadId())) {
      throw new UnauthorizedException("업로드 정보가 일치하지 않아요");
    }
    
    // 업로드 완료 처리
    mediaFile.markAsUploaded(etag);
    
    // CDN URL 설정
    if (!cloudFrontUrl.isEmpty()) {
      mediaFile.setCdnUrl(cloudFrontUrl + "/" + mediaFile.getS3Key());
    }
    
    // 썸네일 처리 (비동기로 처리하거나 Lambda 트리거)
    if (mediaFile.isImage()) {
      processImageThumbnail(mediaFile);
    } else if (mediaFile.isVideo()) {
      processVideoThumbnail(mediaFile);
    }
    
    mediaFile.markAsProcessed();
    mediaFileRepository.save(mediaFile);
    
    return convertToResponse(mediaFile);
  }
  
  /**
   * 멀티파트 업로드 완료
   * 
   * @param userId 사용자 ID
   * @param uploadId 업로드 ID
   * @param parts 완료된 파트 목록
   * @return 미디어 응답
   */
  public MediaResponse completeMultipartUpload(Long userId, String uploadId, 
                                              List<Map<String, Object>> parts) {
    log.info("Completing multipart upload: userId={}, uploadId={}, parts={}", 
        userId, uploadId, parts.size());
    
    MediaFile mediaFile = mediaFileRepository.findByUploadId(uploadId)
        .orElseThrow(() -> new ResourceNotFoundException("업로드를 찾을 수 없어요"));
    
    if (!mediaFile.getUser().getUserId().equals(userId)) {
      throw new UnauthorizedException("권한이 없어요");
    }
    
    // S3 파트 목록 생성
    List<CompletedPart> completedParts = parts.stream()
        .map(part -> CompletedPart.builder()
            .partNumber((Integer) part.get("partNumber"))
            .eTag((String) part.get("etag"))
            .build())
        .collect(Collectors.toList());
    
    // 멀티파트 업로드 완료
    String etag = s3Service.completeMultipartUpload(
        mediaFile.getS3Key(), uploadId, completedParts);
    
    // 업로드 완료 처리
    mediaFile.markAsUploaded(etag);
    mediaFile.markAsProcessed();
    mediaFileRepository.save(mediaFile);
    
    return convertToResponse(mediaFile);
  }
  
  /**
   * 미디어 목록 조회
   * 
   * @param userId 사용자 ID
   * @param uploadType 업로드 타입 (선택)
   * @param pageable 페이징
   * @return 미디어 목록
   */
  @Transactional(readOnly = true)
  public Page<MediaResponse> getMediaList(Long userId, UploadType uploadType, Pageable pageable) {
    Page<MediaFile> mediaFiles;
    
    if (uploadType != null) {
      mediaFiles = mediaFileRepository.findByUser_UserIdAndUploadTypeAndIsDeletedFalse(
          userId, uploadType, pageable);
    } else {
      mediaFiles = mediaFileRepository.findByUser_UserIdAndIsDeletedFalse(userId, pageable);
    }
    
    return mediaFiles.map(this::convertToResponse);
  }
  
  /**
   * 미디어 삭제
   * 
   * @param userId 사용자 ID
   * @param mediaId 미디어 ID
   */
  public void deleteMedia(Long userId, String mediaId) {
    log.info("Deleting media: userId={}, mediaId={}", userId, mediaId);
    
    MediaFile mediaFile = mediaFileRepository.findByMediaIdAndUser_UserId(mediaId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("파일을 찾을 수 없어요"));
    
    // 소프트 삭제
    mediaFile.softDelete();
    mediaFileRepository.save(mediaFile);
    
    // S3에서는 30일 후 실제 삭제 (배치 작업)
    log.info("Media soft deleted: mediaId={}", mediaId);
  }
  
  /**
   * 파일 크기 검증
   */
  private void validateFileSize(MediaUploadRequest request) {
    if (request.getFileSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("파일이 너무 커요 (최대 100MB)");
    }
    
    if (request.isImage() && request.getFileSize() > MAX_IMAGE_SIZE) {
      throw new IllegalArgumentException("이미지가 너무 커요 (최대 10MB)");
    }
  }
  
  /**
   * 업로드 제한 확인
   */
  private void checkUploadLimits(Long userId) {
    // 시간당 업로드 제한
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    Long recentUploads = mediaFileRepository.countUploadsSince(userId, oneHourAgo);
    
    if (recentUploads >= 100) {
      throw new IllegalStateException("잠시 후 다시 시도해주세요 (시간당 100개 제한)");
    }
    
    // 전체 용량 제한 (1GB)
    Long totalSize = mediaFileRepository.getTotalFileSizeByUser(userId);
    if (totalSize > 1024 * 1024 * 1024) {
      throw new IllegalStateException("저장 공간이 부족해요");
    }
  }
  
  /**
   * 미디어 ID 생성
   */
  private String generateMediaId() {
    return "media_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
  
  /**
   * 업로드 ID 생성
   */
  private String generateUploadId() {
    return "upload_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
  
  /**
   * S3 키 생성
   */
  private String generateS3Key(Long userId, UploadType type, String fileName) {
    String extension = "";
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex > 0) {
      extension = fileName.substring(dotIndex);
    }
    
    return String.format("user/%d/%s/%s/%s%s",
        userId,
        type.name().toLowerCase(),
        LocalDateTime.now().toLocalDate(),
        UUID.randomUUID().toString(),
        extension);
  }
  
  /**
   * 최대 파일 크기 조회
   */
  private long getMaxFileSize(MediaUploadRequest request) {
    if (request.isImage()) {
      return MAX_IMAGE_SIZE;
    }
    return MAX_FILE_SIZE;
  }
  
  /**
   * 메타데이터를 JSON으로 변환
   */
  private String convertMetadataToJson(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (Exception e) {
      log.error("Failed to convert metadata to JSON: {}", e.getMessage());
      return null;
    }
  }
  
  /**
   * JSON을 메타데이터로 변환
   */
  private Map<String, String> convertJsonToMetadata(String json) {
    if (json == null || json.isEmpty()) {
      return new HashMap<>();
    }
    
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (Exception e) {
      log.error("Failed to convert JSON to metadata: {}", e.getMessage());
      return new HashMap<>();
    }
  }
  
  /**
   * 이미지 썸네일 처리
   */
  private void processImageThumbnail(MediaFile mediaFile) {
    // TODO: Lambda 함수 또는 비동기 처리로 썸네일 생성
    String thumbnailKey = mediaFile.getS3Key().replace(".", "_thumb.");
    mediaFile.setThumbnailUrl(s3Service.generatePublicUrl(thumbnailKey));
  }
  
  /**
   * 비디오 썸네일 처리
   */
  private void processVideoThumbnail(MediaFile mediaFile) {
    // TODO: Lambda 함수 또는 비동기 처리로 첫 프레임 추출
    String thumbnailKey = mediaFile.getS3Key().replace(".", "_thumb.jpg");
    mediaFile.setThumbnailUrl(s3Service.generatePublicUrl(thumbnailKey));
  }
  
  /**
   * MediaFile을 MediaResponse로 변환
   */
  private MediaResponse convertToResponse(MediaFile mediaFile) {
    return MediaResponse.builder()
        .mediaId(mediaFile.getMediaId())
        .type(mediaFile.getUploadType())
        .url(mediaFile.getUrl())
        .thumbnailUrl(mediaFile.getThumbnailUrl())
        .cdnUrl(mediaFile.getCdnUrl())
        .fileName(mediaFile.getFileName())
        .fileSize(mediaFile.getFileSize())
        .mimeType(mediaFile.getMimeType())
        .width(mediaFile.getWidth())
        .height(mediaFile.getHeight())
        .duration(mediaFile.getDuration())
        .uploadedAt(mediaFile.getUploadedAt())
        .processedAt(mediaFile.getProcessedAt())
        .metadata(convertJsonToMetadata(mediaFile.getMetadata()))
        .build();
  }
}