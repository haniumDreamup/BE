package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Google Cloud Vision API 통합 테스트
 *
 * 실행 조건:
 * - GOOGLE_VISION_ENABLED=true
 * - google-cloud-credentials.json 파일 존재
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "GOOGLE_VISION_ENABLED", matches = "true")
class GoogleVisionIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(GoogleVisionIntegrationTest.class);

  @Autowired(required = false)
  private GoogleVisionService googleVisionService;

  @Test
  void testGoogleVisionService_빈이_생성됨() {
    // Given & When
    log.info("Google Vision Service 빈 확인");

    // Then
    if (System.getenv("GOOGLE_VISION_ENABLED") != null &&
        System.getenv("GOOGLE_VISION_ENABLED").equals("true")) {
      assertThat(googleVisionService).isNotNull();
      log.info("✅ Google Vision Service 빈이 정상적으로 생성됨");
    } else {
      log.info("⚠️ GOOGLE_VISION_ENABLED가 false이므로 빈이 생성되지 않음 (정상)");
    }
  }

  @Test
  void testAnalyzeImage_간단한_이미지() throws IOException {
    // Given
    if (googleVisionService == null) {
      log.info("⚠️ Google Vision Service가 비활성화되어 테스트 스킵");
      return;
    }

    // 테스트용 이미지 생성 (1x1 흰색 PNG)
    byte[] imageBytes = createSimpleTestImage();
    MockMultipartFile imageFile = new MockMultipartFile(
        "image",
        "test.png",
        "image/png",
        imageBytes
    );

    // When
    log.info("이미지 분석 시작");
    var result = googleVisionService.analyzeImage(imageFile);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSimpleDescription()).isNotNull();

    log.info("✅ Vision API 분석 완료");
    log.info("간단한 설명: {}", result.getSimpleDescription());
    log.info("객체 수: {}", result.getObjects().size());
    log.info("라벨 수: {}", result.getLabels().size());
  }

  /**
   * 테스트용 간단한 이미지 생성 (1x1 흰색 PNG)
   */
  private byte[] createSimpleTestImage() {
    // PNG 헤더 + 1x1 흰색 픽셀
    return new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 dimension
        0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
        0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
        0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xFF, (byte) 0xFF, 0x3F,
        0x00, 0x05, (byte) 0xFE, 0x02, (byte) 0xFE, (byte) 0xDC, (byte) 0xCC,
        0x59, (byte) 0xE7,
        0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND chunk
        (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };
  }
}
