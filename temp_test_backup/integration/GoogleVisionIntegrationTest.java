package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Google Vision API 통합 테스트
 * 실제 API를 호출하므로 환경 변수 설정이 필요합니다.
 * 
 * 실행 전 설정:
 * - GOOGLE_VISION_ENABLED=true
 * - GOOGLE_CLOUD_CREDENTIALS_PATH=/path/to/credentials.json
 * - GOOGLE_CLOUD_PROJECT_ID=your-project-id
 */
@SpringBootTest
@TestPropertySource(properties = {
    "google.cloud.vision.enabled=true",
    "google.cloud.vision.max-results=5",
    "google.cloud.vision.confidence-threshold=0.5"
})
@EnabledIfEnvironmentVariable(named = "GOOGLE_VISION_ENABLED", matches = "true")
class GoogleVisionIntegrationTest {
  
  @Autowired(required = false)
  private GoogleVisionService googleVisionService;
  
  private MockMultipartFile testImage;
  
  @BeforeEach
  void setUp() throws IOException {
    // 테스트용 이미지 파일 준비 (src/test/resources에 있어야 함)
    ClassPathResource imageResource = new ClassPathResource("test-images/sample.jpg");
    
    if (imageResource.exists()) {
      testImage = new MockMultipartFile(
          "image",
          "sample.jpg",
          "image/jpeg",
          imageResource.getInputStream()
      );
    } else {
      // 테스트 이미지가 없으면 간단한 이미지 생성
      testImage = new MockMultipartFile(
          "image",
          "test.jpg",
          "image/jpeg",
          generateTestImage()
      );
    }
  }
  
  @Test
  @DisplayName("실제 Google Vision API 호출 테스트")
  void testRealGoogleVisionApi() throws IOException {
    // Given
    assertThat(googleVisionService).isNotNull();
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSimpleDescription()).isNotEmpty();
    
    // 결과 출력 (디버깅용)
    System.out.println("=== Google Vision API 분석 결과 ===");
    System.out.println("간단한 설명: " + result.getSimpleDescription());
    
    if (!result.getObjects().isEmpty()) {
      System.out.println("\n감지된 객체:");
      result.getObjects().forEach(obj -> 
          System.out.printf("- %s (신뢰도: %.2f%%)\n", obj.getName(), obj.getConfidence() * 100)
      );
    }
    
    if (!result.getLabels().isEmpty()) {
      System.out.println("\n라벨:");
      result.getLabels().forEach(label -> 
          System.out.printf("- %s (신뢰도: %.2f%%)\n", label.getDescription(), label.getConfidence() * 100)
      );
    }
    
    if (result.getText() != null && !result.getText().isEmpty()) {
      System.out.println("\n감지된 텍스트: " + result.getText());
    }
    
    if (result.getSafetyInfo() != null) {
      System.out.println("\n안전성 정보:");
      System.out.println("- 성인 콘텐츠: " + result.getSafetyInfo().getAdult());
      System.out.println("- 폭력성: " + result.getSafetyInfo().getViolence());
      System.out.println("- 의료 관련: " + result.getSafetyInfo().getMedical());
    }
    
    if (!result.getFaces().isEmpty()) {
      System.out.println("\n얼굴 감지: " + result.getFaces().size() + "명");
    }
  }
  
  @Test
  @DisplayName("텍스트가 포함된 이미지 분석")
  void analyzeImageWithText() throws IOException {
    // 텍스트가 포함된 이미지로 테스트
    // 실제 테스트를 위해서는 텍스트가 포함된 이미지 파일이 필요합니다
    
    var result = googleVisionService.analyzeImage(testImage);
    
    if (result.getText() != null) {
      assertThat(result.getText()).isNotEmpty();
      assertThat(result.getSimpleDescription()).contains("글자가 있어요");
    }
  }
  
  @Test
  @DisplayName("안전성 검사")
  void checkSafety() throws IOException {
    var result = googleVisionService.analyzeImage(testImage);
    
    assertThat(result.getSafetyInfo()).isNotNull();
    // 대부분의 일반 이미지는 안전해야 함
    assertThat(result.getSafetyInfo().getAdult())
        .isIn("VERY_UNLIKELY", "UNLIKELY");
    assertThat(result.getSafetyInfo().getViolence())
        .isIn("VERY_UNLIKELY", "UNLIKELY");
  }
  
  /**
   * 간단한 테스트 이미지 생성 (1x1 흰색 픽셀)
   */
  private byte[] generateTestImage() {
    // 최소한의 JPEG 이미지 바이트
    return new byte[] {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
        0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
        (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
        0x06, 0x06, 0x07, 0x06, 0x05, 0x08, 0x07, 0x07,
        0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
        0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F,
        0x14, 0x1D, 0x1A, 0x1F, 0x1E, 0x1D, 0x1A, 0x1C,
        0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
        0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30,
        0x31, 0x34, 0x34, 0x34, 0x1F, 0x27, 0x39, 0x3D,
        0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32, (byte) 0xFF,
        (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00,
        0x01, 0x01, 0x01, 0x11, 0x00, (byte) 0xFF, (byte) 0xC4,
        0x00, 0x1F, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, (byte) 0xFF,
        (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02,
        0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05,
        0x04, 0x04, 0x00, 0x00, 0x01, 0x7D, 0x01, 0x02,
        0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31,
        0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71,
        0x14, 0x32, (byte) 0x81, (byte) 0x91, (byte) 0xA1,
        0x08, 0x23, 0x42, (byte) 0xB1, (byte) 0xC1, 0x15,
        0x52, (byte) 0xD1, (byte) 0xF0, 0x24, 0x33, 0x62,
        0x72, (byte) 0x82, 0x09, 0x0A, 0x16, 0x17, 0x18,
        0x19, 0x1A, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A,
        0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43,
        0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x53,
        0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63,
        0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x73,
        0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, (byte) 0x83,
        (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87,
        (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x92,
        (byte) 0x93, (byte) 0x94, (byte) 0x95, (byte) 0x96,
        (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A,
        (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
        (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9,
        (byte) 0xAA, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4,
        (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0xB8,
        (byte) 0xB9, (byte) 0xBA, (byte) 0xC2, (byte) 0xC3,
        (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7,
        (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xD2,
        (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6,
        (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA,
        (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4,
        (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
        (byte) 0xE9, (byte) 0xEA, (byte) 0xF1, (byte) 0xF2,
        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6,
        (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
        (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01,
        0x00, 0x00, 0x3F, 0x00, (byte) 0xFB, (byte) 0xD8,
        (byte) 0xA2, (byte) 0x8A, 0x28, (byte) 0xFF, (byte) 0xD9
    };
  }
}