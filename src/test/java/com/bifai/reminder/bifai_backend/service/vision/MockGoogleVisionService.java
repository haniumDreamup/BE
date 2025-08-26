package com.bifai.reminder.bifai_backend.service.vision;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 테스트용 Mock Google Vision Service
 * 실제 Google Cloud Vision API 호출 없이 테스트 가능
 */
@Service
@Profile("test")
@Primary
public class MockGoogleVisionService extends GoogleVisionService {
  
  public MockGoogleVisionService() {
    super(null); // Mock이므로 실제 클라이언트 불필요
  }
  
  @Override
  public VisionAnalysisResult analyzeImage(MultipartFile imageFile) throws IOException {
    // Mock 분석 결과 반환
    VisionAnalysisResult result = VisionAnalysisResult.builder()
      .objects(new ArrayList<>())
      .labels(new ArrayList<>())
      .text("테스트 텍스트")
      .safetyInfo(SafetyInfo.builder()
        .adult("VERY_UNLIKELY")
        .violence("VERY_UNLIKELY")
        .medical("VERY_UNLIKELY")
        .build())
      .faces(new ArrayList<>())
      .simpleDescription("테스트 이미지입니다.")
      .build();
    
    // 테스트용 객체 추가
    result.getObjects().add(DetectedObject.builder()
      .name("테이블")
      .confidence(0.95f)
      .boundingBox(null)
      .build());
    
    result.getObjects().add(DetectedObject.builder()
      .name("의자")
      .confidence(0.90f)
      .boundingBox(null)
      .build());
    
    // 테스트용 라벨 추가
    result.getLabels().add(Label.builder()
      .description("furniture")
      .confidence(0.92f)
      .build());
    
    return result;
  }
}