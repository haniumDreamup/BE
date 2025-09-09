package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadType;
import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadStatus;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.MediaFileRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AWS S3 기반 미디어 파일 서비스
 * 이미지, 오디오, 비디오 파일 업로드 및 관리
 */
@Slf4j
@Service
@Transactional
public class MediaService {
  
  private final MediaFileRepository mediaFileRepository;
  private final UserRepository userRepository;
  private final S3Client s3Client;
  private final String bucketName;
  
  public MediaService(
      MediaFileRepository mediaFileRepository,
      UserRepository userRepository,
      S3Client s3Client,
      @Qualifier("s3BucketName") String bucketName) {
    this.mediaFileRepository = mediaFileRepository;
    this.userRepository = userRepository;
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }
  
  @Value("${aws.cloudfront.url:}")
  private String cloudFrontUrl;
  
  /**
   * 파일을 S3에 업로드하고 메타데이터를 저장
   */
  public MediaFile uploadFile(Long userId, MultipartFile file, UploadType uploadType) throws IOException {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    // 파일 검증
    validateFile(file, uploadType);
    
    // S3 키 생성
    String s3Key = generateS3Key(userId, file.getOriginalFilename(), uploadType);
    
    try {
      // S3에 파일 업로드
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Key)
          .contentType(file.getContentType())
          .contentLength(file.getSize())
          .build();
      
      PutObjectResponse response = s3Client.putObject(putRequest, 
          RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
      
      log.info("파일 S3 업로드 성공: 사용자 {}, 키 {}, ETag: {}", 
          userId, s3Key, response.eTag());
      
      // 미디어 파일 엔티티 생성 및 저장
      MediaFile mediaFile = MediaFile.builder()
          .user(user)
          .originalName(file.getOriginalFilename())
          .fileName(s3Key)
          .s3Key(s3Key)
          .s3Bucket(bucketName)
          .mimeType(file.getContentType())
          .fileSize(file.getSize())
          .uploadType(uploadType)
          .uploadStatus(UploadStatus.COMPLETED)
          .url(getFileUrl(s3Key))
          .build();
      
      return mediaFileRepository.save(mediaFile);
      
    } catch (S3Exception e) {
      log.error("S3 업로드 실패: 사용자 {}, 파일 {}", userId, file.getOriginalFilename(), e);
      throw new IOException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
    }
  }
  
  /**
   * 사용자의 미디어 파일 목록 조회
   */
  @Transactional(readOnly = true)
  public List<MediaFile> getUserMediaFiles(Long userId, UploadType uploadType) {
    if (uploadType != null) {
      return mediaFileRepository.findByUserUserIdAndUploadTypeOrderByCreatedAtDesc(userId, uploadType);
    }
    return mediaFileRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
  }
  
  /**
   * 미디어 파일 삭제 (S3와 DB에서 모두 삭제)
   */
  public void deleteFile(Long userId, Long mediaFileId) {
    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
        .orElseThrow(() -> new IllegalArgumentException("미디어 파일을 찾을 수 없습니다: " + mediaFileId));
    
    // 소유자 확인
    if (!mediaFile.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("파일 삭제 권한이 없습니다");
    }
    
    try {
      // S3에서 파일 삭제
      DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(mediaFile.getS3Key())
          .build();
      
      s3Client.deleteObject(deleteRequest);
      log.info("S3 파일 삭제 성공: 키 {}", mediaFile.getS3Key());
      
      // DB에서 삭제
      mediaFileRepository.delete(mediaFile);
      
    } catch (S3Exception e) {
      log.error("S3 파일 삭제 실패: 키 {}", mediaFile.getS3Key(), e);
      throw new RuntimeException("파일 삭제에 실패했습니다: " + e.getMessage(), e);
    }
  }
  
  /**
   * 미디어 파일 URL 생성 (CloudFront 또는 S3 직접 URL)
   */
  public String getFileUrl(MediaFile mediaFile) {
    return getFileUrl(mediaFile.getS3Key());
  }
  
  /**
   * S3 키로 파일 URL 생성
   */
  public String getFileUrl(String s3Key) {
    if (cloudFrontUrl != null && !cloudFrontUrl.isEmpty()) {
      return cloudFrontUrl + "/" + s3Key;
    }
    
    // S3 직접 URL 생성
    return String.format("https://%s.s3.%s.amazonaws.com/%s", 
        bucketName, "ap-northeast-2", s3Key);
  }
  
  /**
   * 임시 다운로드 URL 생성 (Presigned URL)
   */
  public String generatePresignedUrl(Long userId, Long mediaFileId, int expirationMinutes) {
    MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
        .orElseThrow(() -> new IllegalArgumentException("미디어 파일을 찾을 수 없습니다: " + mediaFileId));
    
    // 소유자 확인
    if (!mediaFile.getUser().getUserId().equals(userId)) {
      throw new IllegalArgumentException("파일 접근 권한이 없습니다");
    }
    
    try {
      // 현재는 실제 presigning 없이 CloudFront URL 또는 S3 URL 반환
      // TODO: 실제 S3Presigner를 사용한 presigned URL 생성 구현 필요
      log.info("Presigned URL 요청: 파일 {}, 만료시간 {}분", mediaFile.getS3Key(), expirationMinutes);
      return getFileUrl(mediaFile);
      
    } catch (Exception e) {
      log.error("Presigned URL 생성 실패: 키 {}", mediaFile.getS3Key(), e);
      throw new RuntimeException("다운로드 URL 생성에 실패했습니다: " + e.getMessage(), e);
    }
  }
  
  /**
   * S3 키 생성
   */
  private String generateS3Key(Long userId, String originalFilename, UploadType uploadType) {
    String timestamp = java.time.LocalDateTime.now().format(
        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String uuid = UUID.randomUUID().toString().substring(0, 8);
    String extension = getFileExtension(originalFilename);
    
    return String.format("media/%s/%d/%s-%s%s", 
        uploadType.name().toLowerCase(), userId, timestamp, uuid, extension);
  }
  
  /**
   * 파일 확장자 추출
   */
  private String getFileExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf(".")).toLowerCase();
  }
  
  /**
   * 파일 검증
   */
  private void validateFile(MultipartFile file, UploadType uploadType) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어있습니다");
    }
    
    // 파일 크기 제한 (50MB)
    long maxSize = 50 * 1024 * 1024;
    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("파일 크기는 50MB를 초과할 수 없습니다");
    }
    
    // MIME 타입 검증
    String contentType = file.getContentType();
    if (contentType == null) {
      throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다");
    }
    
    boolean validType = false;
    switch (uploadType) {
      case PROFILE:
      case MEDICATION:
      case ACTIVITY:
      case HEALTH:
        validType = contentType.startsWith("image/") || contentType.startsWith("video/");
        break;
      case DOCUMENT:
        validType = contentType.startsWith("application/") || 
                   contentType.startsWith("text/") || contentType.startsWith("image/");
        break;
      case EMERGENCY:
        validType = contentType.startsWith("image/") || contentType.startsWith("video/") || 
                   contentType.startsWith("audio/");
        break;
    }
    
    if (!validType) {
      throw new IllegalArgumentException(
          String.format("지원되지 않는 파일 형식입니다: %s (업로드 타입: %s)", 
              contentType, uploadType.getDescription()));
    }
  }
}