package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.MediaFile.*;
import com.bifai.reminder.bifai_backend.repository.MediaFileRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.LocalFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미디어 파일 서비스 (임시 스텁 버전)
 * 
 * S3 대신 로컬 파일 시스템 사용
 * TODO: 실제 로컬 파일 업로드 구현 필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {
  
  private final MediaFileRepository mediaFileRepository;
  private final UserRepository userRepository;
  private final LocalFileService localFileService;
  private final ObjectMapper objectMapper;
  
  @Value("${file.upload.url-prefix:/files}")
  private String urlPrefix;
  
  // TODO: S3를 사용하지 않으므로 모든 메소드를 LocalFileService 기반으로 재구현 필요
  // 현재는 컴파일 오류만 해결하기 위한 임시 스텁
}