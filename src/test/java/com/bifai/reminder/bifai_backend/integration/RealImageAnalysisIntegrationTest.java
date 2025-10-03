package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.service.OpenAIService;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 이미지를 사용한 통합 테스트
 * Google Vision API + OpenAI 전체 플로우 검증
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "GOOGLE_VISION_ENABLED", matches = "true")
class RealImageAnalysisIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(RealImageAnalysisIntegrationTest.class);

  @Autowired(required = false)
  private GoogleVisionService googleVisionService;

  @Autowired(required = false)
  private OpenAIService openAIService;

  @Test
  void test_실제_이미지로_Vision_API_테스트() throws IOException {
    // Given
    if (googleVisionService == null) {
      log.info("⚠️ Google Vision Service가 비활성화되어 테스트 스킵");
      return;
    }

    // 테스트용 간단한 이미지 생성 (텍스트 포함)
    MockMultipartFile imageFile = createTestImageWithText("EXIT");

    // When
    log.info("🖼️ Google Vision API로 이미지 분석 시작");
    var result = googleVisionService.analyzeImage(imageFile);

    // Then
    assertThat(result).isNotNull();
    log.info("✅ Vision API 분석 완료");
    log.info("📊 결과:");
    log.info("  - 간단한 설명: {}", result.getSimpleDescription());
    log.info("  - 발견된 객체: {}개", result.getObjects().size());
    log.info("  - 라벨: {}개", result.getLabels().size());
    log.info("  - 추출된 텍스트: {}", result.getText());
    log.info("  - 얼굴: {}명", result.getFaces().size());

    // 상세 정보 출력
    if (!result.getObjects().isEmpty()) {
      log.info("📦 객체 상세:");
      result.getObjects().forEach(obj ->
          log.info("    - {}: {}", obj.getName(), obj.getConfidence())
      );
    }

    if (!result.getLabels().isEmpty()) {
      log.info("🏷️ 라벨 상세:");
      result.getLabels().forEach(label ->
          log.info("    - {}: {}", label.getDescription(), label.getConfidence())
      );
    }
  }

  @Test
  void test_Vision_결과를_OpenAI로_해석() throws IOException {
    // Given
    if (googleVisionService == null || openAIService == null) {
      log.info("⚠️ Vision 또는 OpenAI 서비스가 비활성화되어 테스트 스킵");
      return;
    }

    // 1단계: Vision API로 이미지 분석
    MockMultipartFile imageFile = createTestImageWithText("DANGER");
    var visionResult = googleVisionService.analyzeImage(imageFile);

    // 2단계: Vision 결과를 Map으로 변환
    List<Map<String, Object>> objects = visionResult.getObjects().stream()
        .map(obj -> {
          Map<String, Object> map = new HashMap<>();
          map.put("label", obj.getName());
          map.put("confidence", obj.getConfidence());
          return map;
        })
        .toList();

    String extractedText = visionResult.getText() != null ? visionResult.getText() : "";

    // When
    log.info("🤖 OpenAI로 상황 해석 시작");
    var interpretation = openAIService.interpretSituation(
        objects,
        extractedText,
        "이게 뭐예요?",
        "길을 걷고 있어요"
    );

    // Then
    assertThat(interpretation).isNotNull();
    assertThat(interpretation).containsKeys("description", "action", "safety");

    log.info("✅ OpenAI 상황 해석 완료");
    log.info("📝 결과:");
    log.info("  - 설명: {}", interpretation.get("description"));
    log.info("  - 행동: {}", interpretation.get("action"));
    log.info("  - 안전도: {}", interpretation.get("safety"));
  }

  @Test
  void test_전체_플로우_이미지_업로드부터_AI_해석까지() throws IOException {
    // Given
    if (googleVisionService == null || openAIService == null) {
      log.info("⚠️ 서비스가 비활성화되어 테스트 스킵");
      return;
    }

    log.info("🚀 전체 AI 파이프라인 테스트 시작");
    log.info("=====================================");

    // 1. 이미지 생성
    log.info("1️⃣ 테스트 이미지 생성");
    MockMultipartFile imageFile = createTestImageWithText("HELP");

    // 2. Vision API 분석
    log.info("2️⃣ Google Vision API 분석");
    var visionResult = googleVisionService.analyzeImage(imageFile);
    log.info("   ✓ 객체: {}개", visionResult.getObjects().size());
    log.info("   ✓ 라벨: {}개", visionResult.getLabels().size());
    log.info("   ✓ 텍스트: {}", visionResult.getText());

    // 3. 데이터 변환
    log.info("3️⃣ 데이터 변환");
    List<Map<String, Object>> objects = visionResult.getObjects().stream()
        .map(obj -> Map.<String, Object>of(
            "label", obj.getName(),
            "confidence", obj.getConfidence()
        ))
        .toList();

    // 4. OpenAI 해석
    log.info("4️⃣ OpenAI 상황 해석");
    var interpretation = openAIService.interpretSituation(
        objects,
        visionResult.getText(),
        "도움이 필요해요",
        null
    );

    // 5. 결과 검증
    log.info("5️⃣ 결과 검증");
    assertThat(visionResult).isNotNull();
    assertThat(interpretation).isNotNull();
    assertThat(interpretation.get("description")).isNotNull();

    // 최종 결과
    log.info("=====================================");
    log.info("✅ 전체 AI 파이프라인 성공!");
    log.info("📊 최종 결과:");
    log.info("  Vision: {}", visionResult.getSimpleDescription());
    log.info("  OpenAI: {}", interpretation.get("description"));
    log.info("=====================================");
  }

  /**
   * 텍스트가 포함된 테스트 이미지 생성
   */
  private MockMultipartFile createTestImageWithText(String text) throws IOException {
    // 200x100 이미지 생성
    BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // 배경 흰색
    g2d.setColor(Color.WHITE);
    g2d.fillRect(0, 0, 200, 100);

    // 텍스트 검은색
    g2d.setColor(Color.BLACK);
    g2d.setFont(new Font("Arial", Font.BOLD, 24));
    g2d.drawString(text, 50, 60);

    // 빨간 사각형 추가 (객체 인식용)
    g2d.setColor(Color.RED);
    g2d.fillRect(10, 10, 30, 30);

    g2d.dispose();

    // PNG로 변환
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    byte[] imageBytes = baos.toByteArray();

    return new MockMultipartFile(
        "image",
        "test-image.png",
        "image/png",
        imageBytes
    );
  }
}
