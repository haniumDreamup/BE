package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.net.URL;
import java.time.Duration;
import java.util.*;

/**
 * AWS S3 서비스
 * 
 * S3 파일 업로드/다운로드 및 Presigned URL 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileS3Service {
  
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Config s3Config;
  
  private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(15);
  private static final long MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB
  private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB per part
  
  /**
   * Presigned PUT URL 생성 (단일 업로드)
   * 
   * @param key S3 객체 키
   * @param contentType 콘텐츠 타입
   * @param metadata 메타데이터
   * @return Presigned URL
   */
  public URL generatePresignedPutUrl(String key, String contentType, Map<String, String> metadata) {
    log.info("Generating presigned PUT URL: key={}, contentType={}", key, contentType);
    
    try {
      // PUT 요청 빌드
      PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .contentType(contentType);
      
      // 메타데이터 추가
      if (metadata != null && !metadata.isEmpty()) {
        requestBuilder.metadata(metadata);
      }
      
      // Presigned 요청 생성
      PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
          .signatureDuration(PRESIGNED_URL_DURATION)
          .putObjectRequest(requestBuilder.build())
          .build();
      
      PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
      
      return presignedRequest.url();
      
    } catch (Exception e) {
      log.error("Failed to generate presigned PUT URL: {}", e.getMessage());
      throw new RuntimeException("Presigned URL 생성 실패", e);
    }
  }
  
  /**
   * Presigned GET URL 생성 (다운로드)
   * 
   * @param key S3 객체 키
   * @return Presigned URL
   */
  public URL generatePresignedGetUrl(String key) {
    log.info("Generating presigned GET URL: key={}", key);
    
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .build();
      
      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(PRESIGNED_URL_DURATION)
          .getObjectRequest(getObjectRequest)
          .build();
      
      PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
      
      return presignedRequest.url();
      
    } catch (Exception e) {
      log.error("Failed to generate presigned GET URL: {}", e.getMessage());
      throw new RuntimeException("Presigned URL 생성 실패", e);
    }
  }
  
  /**
   * 멀티파트 업로드 시작
   * 
   * @param key S3 객체 키
   * @param contentType 콘텐츠 타입
   * @param metadata 메타데이터
   * @return 업로드 ID
   */
  public String initiateMultipartUpload(String key, String contentType, Map<String, String> metadata) {
    log.info("Initiating multipart upload: key={}", key);
    
    try {
      CreateMultipartUploadRequest.Builder requestBuilder = CreateMultipartUploadRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .contentType(contentType);
      
      if (metadata != null && !metadata.isEmpty()) {
        requestBuilder.metadata(metadata);
      }
      
      CreateMultipartUploadResponse response = s3Client.createMultipartUpload(requestBuilder.build());
      
      log.info("Multipart upload initiated: uploadId={}", response.uploadId());
      return response.uploadId();
      
    } catch (Exception e) {
      log.error("Failed to initiate multipart upload: {}", e.getMessage());
      throw new RuntimeException("멀티파트 업로드 시작 실패", e);
    }
  }
  
  /**
   * 멀티파트 업로드용 Presigned URL 생성
   * 
   * @param key S3 객체 키
   * @param uploadId 업로드 ID
   * @param partNumber 파트 번호
   * @return Presigned URL
   */
  public URL generatePresignedUploadPartUrl(String key, String uploadId, int partNumber) {
    log.debug("Generating presigned URL for part: key={}, partNumber={}", key, partNumber);
    
    try {
      UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .uploadId(uploadId)
          .partNumber(partNumber)
          .build();
      
      UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
          .signatureDuration(PRESIGNED_URL_DURATION)
          .uploadPartRequest(uploadPartRequest)
          .build();
      
      PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);
      
      return presignedRequest.url();
      
    } catch (Exception e) {
      log.error("Failed to generate presigned upload part URL: {}", e.getMessage());
      throw new RuntimeException("파트 업로드 URL 생성 실패", e);
    }
  }
  
  /**
   * 멀티파트 업로드 완료
   * 
   * @param key S3 객체 키
   * @param uploadId 업로드 ID
   * @param parts 완료된 파트 목록
   * @return ETag
   */
  public String completeMultipartUpload(String key, String uploadId, List<CompletedPart> parts) {
    log.info("Completing multipart upload: key={}, uploadId={}, parts={}", key, uploadId, parts.size());
    
    try {
      // 파트 번호 순으로 정렬
      parts.sort(Comparator.comparing(CompletedPart::partNumber));
      
      CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .uploadId(uploadId)
          .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
          .build();
      
      CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);
      
      log.info("Multipart upload completed: etag={}", response.eTag());
      return response.eTag();
      
    } catch (Exception e) {
      log.error("Failed to complete multipart upload: {}", e.getMessage());
      throw new RuntimeException("멀티파트 업로드 완료 실패", e);
    }
  }
  
  /**
   * 멀티파트 업로드 취소
   * 
   * @param key S3 객체 키
   * @param uploadId 업로드 ID
   */
  public void abortMultipartUpload(String key, String uploadId) {
    log.info("Aborting multipart upload: key={}, uploadId={}", key, uploadId);
    
    try {
      AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .uploadId(uploadId)
          .build();
      
      s3Client.abortMultipartUpload(abortRequest);
      log.info("Multipart upload aborted");
      
    } catch (Exception e) {
      log.error("Failed to abort multipart upload: {}", e.getMessage());
    }
  }
  
  /**
   * 파일 삭제
   * 
   * @param key S3 객체 키
   */
  public void deleteObject(String key) {
    log.info("Deleting object: key={}", key);
    
    try {
      DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .build();
      
      s3Client.deleteObject(deleteRequest);
      log.info("Object deleted successfully");
      
    } catch (Exception e) {
      log.error("Failed to delete object: {}", e.getMessage());
      throw new RuntimeException("파일 삭제 실패", e);
    }
  }
  
  /**
   * 파일 존재 여부 확인
   * 
   * @param key S3 객체 키
   * @return 존재 여부
   */
  public boolean doesObjectExist(String key) {
    try {
      HeadObjectRequest headRequest = HeadObjectRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .build();
      
      s3Client.headObject(headRequest);
      return true;
      
    } catch (NoSuchKeyException e) {
      return false;
    } catch (Exception e) {
      log.error("Failed to check object existence: {}", e.getMessage());
      return false;
    }
  }
  
  /**
   * 파일 메타데이터 조회
   * 
   * @param key S3 객체 키
   * @return 메타데이터
   */
  public Map<String, String> getObjectMetadata(String key) {
    try {
      HeadObjectRequest headRequest = HeadObjectRequest.builder()
          .bucket(s3Config.getBucketName())
          .key(key)
          .build();
      
      HeadObjectResponse response = s3Client.headObject(headRequest);
      
      Map<String, String> metadata = new HashMap<>();
      metadata.put("contentType", response.contentType());
      metadata.put("contentLength", String.valueOf(response.contentLength()));
      metadata.put("eTag", response.eTag());
      metadata.put("lastModified", response.lastModified().toString());
      
      if (response.metadata() != null) {
        metadata.putAll(response.metadata());
      }
      
      return metadata;
      
    } catch (Exception e) {
      log.error("Failed to get object metadata: {}", e.getMessage());
      throw new RuntimeException("메타데이터 조회 실패", e);
    }
  }
  
  /**
   * 파일 복사
   * 
   * @param sourceKey 원본 키
   * @param destKey 대상 키
   */
  public void copyObject(String sourceKey, String destKey) {
    log.info("Copying object: source={}, dest={}", sourceKey, destKey);
    
    try {
      CopyObjectRequest copyRequest = CopyObjectRequest.builder()
          .sourceBucket(s3Config.getBucketName())
          .sourceKey(sourceKey)
          .destinationBucket(s3Config.getBucketName())
          .destinationKey(destKey)
          .build();
      
      s3Client.copyObject(copyRequest);
      log.info("Object copied successfully");
      
    } catch (Exception e) {
      log.error("Failed to copy object: {}", e.getMessage());
      throw new RuntimeException("파일 복사 실패", e);
    }
  }
  
  /**
   * S3 URL 생성
   * 
   * @param key S3 객체 키
   * @return 공개 URL
   */
  public String generatePublicUrl(String key) {
    return String.format("https://%s.s3.%s.amazonaws.com/%s",
        s3Config.getBucketName(),
        "ap-northeast-2", // TODO: region from config
        key);
  }
}