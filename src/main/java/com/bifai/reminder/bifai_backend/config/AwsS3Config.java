package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * AWS S3 설정
 * 개발 환경에서는 LocalStack, 운영 환경에서는 실제 AWS S3 사용
 */
@Slf4j
@Configuration
public class AwsS3Config {
  
  @Value("${aws.s3.region:ap-northeast-2}")
  private String region;
  
  @Value("${aws.s3.access-key:}")
  private String accessKey;
  
  @Value("${aws.s3.secret-key:}")
  private String secretKey;
  
  @Value("${aws.s3.endpoint:}")
  private String endpoint;
  
  @Value("${aws.s3.bucket-name:bifai-media}")
  private String bucketName;
  
  @Bean
  public S3Client s3Client() {
    var builder = S3Client.builder()
        .region(Region.of(region));
    
    // LocalStack이나 커스텀 엔드포인트 설정
    if (endpoint != null && !endpoint.isEmpty()) {
      log.info("Using custom S3 endpoint: {}", endpoint);
      builder.endpointOverride(URI.create(endpoint))
          .serviceConfiguration(S3Configuration.builder()
              .pathStyleAccessEnabled(true) // LocalStack에서 필요
              .build());
    }
    
    // 자격 증명 설정
    if (accessKey != null && !accessKey.isEmpty() && 
        secretKey != null && !secretKey.isEmpty()) {
      log.info("Using provided AWS credentials for S3");
      AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else {
      log.info("Using default AWS credentials provider for S3");
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }
    
    S3Client s3Client = builder.build();
    
    log.info("S3Client configured - Region: {}, Bucket: {}, Endpoint: {}", 
        region, bucketName, endpoint.isEmpty() ? "default" : endpoint);
    
    return s3Client;
  }
  
  @Bean
  public String s3BucketName() {
    return bucketName;
  }
}