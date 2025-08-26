package com.bifai.reminder.bifai_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 파일 저장소 서비스
 * EC2 서버의 로컬 디스크에 파일을 저장하고 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileService {

  @Value("${file.upload.dir:/home/ec2-user/bifai-files}")
  private String uploadDir;

  @Value("${file.upload.url-prefix:/files}")
  private String urlPrefix;

  /**
   * 파일 업로드
   */
  public String uploadFile(MultipartFile file, String category) throws IOException {
    // 카테고리별 디렉토리 생성
    Path categoryDir = Paths.get(uploadDir, category);
    if (!Files.exists(categoryDir)) {
      Files.createDirectories(categoryDir);
      log.info("디렉토리 생성: {}", categoryDir);
    }

    // 고유한 파일명 생성
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String newFilename = UUID.randomUUID().toString() + extension;
    
    // 파일 저장
    Path targetPath = categoryDir.resolve(newFilename);
    Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    
    log.info("파일 저장 완료: {}", targetPath);
    
    // URL 경로 반환
    return urlPrefix + "/" + category + "/" + newFilename;
  }

  /**
   * 파일 삭제
   */
  public void deleteFile(String filePath) {
    try {
      // URL 경로에서 실제 파일 경로 추출
      String relativePath = filePath.replace(urlPrefix + "/", "");
      Path path = Paths.get(uploadDir, relativePath);
      
      if (Files.exists(path)) {
        Files.delete(path);
        log.info("파일 삭제 완료: {}", path);
      }
    } catch (IOException e) {
      log.error("파일 삭제 실패: {}", filePath, e);
    }
  }

  /**
   * 파일 존재 여부 확인
   */
  public boolean fileExists(String filePath) {
    String relativePath = filePath.replace(urlPrefix + "/", "");
    Path path = Paths.get(uploadDir, relativePath);
    return Files.exists(path);
  }

  /**
   * 파일 경로 가져오기
   */
  public Path getFilePath(String filePath) {
    String relativePath = filePath.replace(urlPrefix + "/", "");
    return Paths.get(uploadDir, relativePath);
  }
}