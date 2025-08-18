package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정
 * 
 * S3 클라이언트 및 Presigner 구성
 */
@Slf4j
@Configuration
public class S3Config {
  
  @Value("${aws.s3.region:ap-northeast-2}")
  private String region;
  
  @Value("${aws.s3.bucket-name:bifai-media}")
  private String bucketName;
  
  @Value("${aws.s3.access-key:}")
  private String accessKey;
  
  @Value("${aws.s3.secret-key:}")
  private String secretKey;
  
  @Value("${aws.s3.endpoint:}")
  private String endpoint;
  
  /**
   * AWS 자격 증명 제공자
   */
  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
      log.info("Using static AWS credentials");
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey)
      );
    }
    
    log.info("Using default AWS credentials provider chain");
    return DefaultCredentialsProvider.create();
  }
  
  /**
   * S3 동기 클라이언트
   */
  @Bean
  public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
    var builder = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider);
    
    if (!endpoint.isEmpty()) {
      builder.endpointOverride(java.net.URI.create(endpoint));
    }
    
    return builder.build();
  }
  
  /**
   * S3 비동기 클라이언트
   */
  @Bean
  public S3AsyncClient s3AsyncClient(AwsCredentialsProvider credentialsProvider) {
    var builder = S3AsyncClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider);
    
    if (!endpoint.isEmpty()) {
      builder.endpointOverride(java.net.URI.create(endpoint));
    }
    
    return builder.build();
  }
  
  /**
   * S3 Presigner for generating presigned URLs
   */
  @Bean
  public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
    var builder = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider);
    
    if (!endpoint.isEmpty()) {
      builder.endpointOverride(java.net.URI.create(endpoint));
    }
    
    return builder.build();
  }
  
  /**
   * Get configured bucket name
   */
  public String getBucketName() {
    return bucketName;
  }
}