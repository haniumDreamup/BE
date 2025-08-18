package com.bifai.reminder.bifai_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * S3 서비스
 * AWS S3 파일 업로드 및 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

  @Value("${aws.s3.bucket:bifai-images}")
  private String bucketName;

  @Value("${aws.s3.region:ap-northeast-2}")
  private String region;

  /**
   * S3에 파일 업로드
   */
  public String uploadFile(MultipartFile file, String key) throws IOException {
    log.info("S3 업로드: {}", key);
    
    // TODO: 실제 AWS SDK를 사용한 S3 업로드 구현
    // S3Client s3Client = S3Client.builder().region(Region.of(region)).build();
    // PutObjectRequest request = PutObjectRequest.builder()
    //     .bucket(bucketName)
    //     .key(key)
    //     .build();
    // s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    
    // 임시 URL 반환
    return String.format("https://%s.s3.%s.amazonaws.com/%s", 
        bucketName, region, key);
  }

  /**
   * S3에서 파일 삭제
   */
  public void deleteFile(String key) {
    log.info("S3 삭제: {}", key);
    // TODO: 실제 S3 파일 삭제 구현
  }

  /**
   * Pre-signed URL 생성
   */
  public String generatePresignedUrl(String key, int expirationMinutes) {
    log.info("Pre-signed URL 생성: {}", key);
    // TODO: 실제 Pre-signed URL 생성 구현
    return String.format("https://%s.s3.%s.amazonaws.com/%s?token=temp", 
        bucketName, region, key);
  }
}