package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAI ChatClient 통합 테스트
 *
 * 실행 조건:
 * - OPENAI_API_KEY 환경 변수 설정
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
class OpenAIChatClientIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(OpenAIChatClientIntegrationTest.class);

  @Autowired(required = false)
  private ChatClient chatClient;

  @Autowired(required = false)
  private OpenAIService openAIService;

  @Test
  void testChatClient_빈이_생성됨() {
    // Given & When
    log.info("ChatClient 빈 확인");

    // Then
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey != null && apiKey.startsWith("sk-")) {
      assertThat(chatClient).isNotNull();
      log.info("✅ ChatClient 빈이 정상적으로 생성됨");
    } else {
      log.info("⚠️ OPENAI_API_KEY가 없으므로 빈이 생성되지 않음 (정상)");
    }
  }

  @Test
  void testOpenAIService_빈이_생성됨() {
    // Given & When
    log.info("OpenAIService 빈 확인");

    // Then
    assertThat(openAIService).isNotNull();
    log.info("✅ OpenAIService 빈이 정상적으로 생성됨");
  }

  @Test
  void testInterpretSituation_간단한_상황_해석() {
    // Given
    if (openAIService == null) {
      log.info("⚠️ OpenAI Service가 비활성화되어 테스트 스킵");
      return;
    }

    List<Map<String, Object>> objects = List.of(
        Map.of("label", "사람", "confidence", 0.95f),
        Map.of("label", "자동차", "confidence", 0.88f)
    );
    String extractedText = "출구";
    String userQuestion = "이게 뭐예요?";

    // When
    log.info("상황 해석 시작");
    var result = openAIService.interpretSituation(objects, extractedText, userQuestion, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).containsKeys("description", "action", "safety");
    assertThat(result.get("description")).isNotNull();

    log.info("✅ OpenAI 상황 해석 완료");
    log.info("설명: {}", result.get("description"));
    log.info("행동: {}", result.get("action"));
    log.info("안전도: {}", result.get("safety"));
  }

  @Test
  void testGenerateMedicationReminder_약물_알림() {
    // Given
    if (openAIService == null) {
      log.info("⚠️ OpenAI Service가 비활성화되어 테스트 스킵");
      return;
    }

    // When
    log.info("약물 알림 메시지 생성 시작");
    String reminder = openAIService.generateMedicationReminder(
        "아스피린",
        "오전 8시",
        "1정"
    );

    // Then
    assertThat(reminder).isNotNull();
    assertThat(reminder).isNotEmpty();

    log.info("✅ 약물 알림 메시지 생성 완료");
    log.info("메시지: {}", reminder);
  }
}
