package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 로컬 파일 저장소 설정
 * EC2 서버의 로컬 디스크를 파일 저장소로 사용
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class LocalFileConfig implements WebMvcConfigurer {
  
  @Value("${file.upload.dir:/home/ec2-user/bifai-files}")
  private String uploadDir;
  
  @Value("${file.upload.url-prefix:/files}")
  private String urlPrefix;
  
  /**
   * 업로드 디렉토리 초기화
   */
  @PostConstruct
  public void init() {
    try {
      Path uploadPath = Paths.get(uploadDir);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
        log.info("업로드 디렉토리 생성: {}", uploadPath);
      }
      
      // 카테고리별 하위 디렉토리 생성
      String[] categories = {"profile", "medication", "document", "temp"};
      for (String category : categories) {
        Path categoryPath = uploadPath.resolve(category);
        if (!Files.exists(categoryPath)) {
          Files.createDirectories(categoryPath);
          log.info("카테고리 디렉토리 생성: {}", categoryPath);
        }
      }
    } catch (Exception e) {
      log.warn("업로드 디렉토리 초기화 실패 - 테스트 환경에서는 무시합니다: {}", e.getMessage());
      // 테스트 환경에서는 파일 시스템 오류를 무시
    }
  }
  
  /**
   * 정적 리소스 핸들러 설정
   * 업로드된 파일을 URL로 접근 가능하도록 설정
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler(urlPrefix + "/**")
        .addResourceLocations("file:" + uploadDir + "/")
        .setCachePeriod(3600);
    
    log.info("파일 리소스 핸들러 등록: {} -> {}", urlPrefix, uploadDir);
  }
}