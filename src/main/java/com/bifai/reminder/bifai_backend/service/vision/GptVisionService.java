package com.bifai.reminder.bifai_backend.service.vision;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * GPT-4o Vision 서비스
 * BIF 사용자를 위한 고품질 이미지 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GptVisionService {

  private final ChatClient.Builder chatClientBuilder;

  @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
  private String model;

  /**
   * 일반 이미지 분석 (BIF 친화적)
   */
  public GptVisionResult analyzeImage(MultipartFile imageFile) throws IOException {
    log.info("GPT-4o Vision 분석 시작 - 파일: {}", imageFile.getOriginalFilename());

    String prompt = """
        당신은 인지 능력이 낮은 사용자(IQ 70-85)를 돕는 AI 비서입니다.

        이 사진을 보고 아래 형식으로 설명해주세요:

        **📍 지금 상황**
        - 한 문장으로 지금 무슨 상황인지 설명 (예: "실내에서 식사 중이에요")

        **✓ 확실히 보이는 것**
        - 85% 이상 확신하는 물건들을 쉼표로 나열 (예: "테이블, 의자, 사람 2명")

        **? 아마도 있는 것**
        - 60-85% 확신하는 것들 (예: "컵, 접시")

        **👤 사람 정보**
        - 사람이 몇 명 보이는지
        - 표정이나 감정 (예: "웃고 있어요 😊", "피곤해 보여요")

        **⚠️ 주의사항** (위험한 것이 있을 때만)
        - 위험 요소와 어떻게 해야 하는지 (예: "불이 보여요! 안전한 곳으로 이동하세요!")

        **💡 도움말** (필요하면)
        - 지금 상황에서 유용한 조언 (예: "신호등이 빨간불이에요. 초록불을 기다리세요")

        규칙:
        - 초등학교 5학년이 이해할 수 있는 쉬운 말로 작성
        - 한 문장은 15단어 이하로
        - 복잡한 단어 사용 금지
        - 위험 상황은 ⚠️ 이모지로 강조
        - 없는 것은 "없음" 또는 섹션 생략
        """;

    return analyzeWithPrompt(imageFile, prompt, "GENERAL");
  }

  /**
   * 응급 상황 분석 (위험 요소 집중)
   */
  public GptVisionResult analyzeEmergency(MultipartFile imageFile) throws IOException {
    log.warn("GPT-4o Vision 응급 분석 시작");

    String prompt = """
        당신은 긴급 상황을 판단하는 AI입니다.
        사진에서 위험한 것이 있는지 빠르게 확인해주세요.

        **🚨 위험 수준**
        - 위험함 / 조금 위험함 / 안전함 중 하나 선택

        **⚠️ 발견된 위험**
        - 불, 연기, 피, 날카로운 물건, 차량 등
        - 각 위험 요소를 한 줄로 설명

        **✅ 지금 해야 할 일**
        1. 첫 번째 행동 (가장 중요)
        2. 두 번째 행동
        3. 세 번째 행동 (선택사항)

        **📞 연락처** (심각하면)
        - 119 (화재/응급)
        - 112 (범죄/사고)

        규칙:
        - 매우 간단하고 직접적으로
        - 위험하지 않으면 "안전합니다"만 출력
        - 행동 지침은 구체적으로 (예: "문을 열고 밖으로 나가세요" ✅, "조심하세요" ❌)
        """;

    return analyzeWithPrompt(imageFile, prompt, "EMERGENCY");
  }

  /**
   * 도로 횡단 안내
   */
  public GptVisionResult analyzeRoadCrossing(MultipartFile imageFile) throws IOException {
    log.info("GPT-4o Vision 도로 횡단 분석 시작");

    String prompt = """
        당신은 시각장애인과 인지장애인을 돕는 보행 안내 AI입니다.

        이 사진을 보고 길을 건널 수 있는지 판단해주세요:

        **🚦 신호 상태**
        - 빨간불 / 초록불 / 신호등 없음

        **🚗 차량 상황**
        - 차량이 지나가는 중 / 차량 없음 / 잘 안 보임

        **📍 현재 위치**
        - 횡단보도 / 일반 도로 / 인도

        **✅ 지금 행동**
        예시:
        - "빨간불이에요. 초록불까지 기다리세요"
        - "차가 지나가고 있어요. 조금만 기다리세요"
        - "안전해요. 좌우를 보고 건너세요"

        규칙:
        - 한 문장으로 간단명료하게
        - 확실하지 않으면 "기다리세요"라고 안내
        - 안전이 최우선
        """;

    return analyzeWithPrompt(imageFile, prompt, "ROAD_CROSSING");
  }

  /**
   * 공통 분석 로직
   */
  private GptVisionResult analyzeWithPrompt(MultipartFile imageFile, String prompt, String analysisType)
      throws IOException {

    long startTime = System.currentTimeMillis();

    try {
      // 이미지를 Resource로 변환
      byte[] imageBytes = imageFile.getBytes();
      ByteArrayResource imageResource = new ByteArrayResource(imageBytes);

      // Media 객체 생성
      Media media = new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);

      // UserMessage 생성 (텍스트 + 이미지)
      UserMessage userMessage = new UserMessage(prompt, List.of(media));

      // ChatClient로 요청
      ChatClient chatClient = chatClientBuilder.build();

      ChatResponse response = chatClient.prompt()
          .messages(userMessage)
          .call()
          .chatResponse();

      String content = response.getResult().getOutput().getContent();

      long duration = System.currentTimeMillis() - startTime;

      log.info("GPT-4o Vision 분석 완료 - 타입: {}, 소요시간: {}ms", analysisType, duration);

      return GptVisionResult.builder()
          .analysisType(analysisType)
          .description(content)
          .model(model)
          .tokensUsed(extractTokenUsage(response))
          .processingTimeMs(duration)
          .success(true)
          .build();

    } catch (Exception e) {
      log.error("GPT-4o Vision 분석 실패", e);

      return GptVisionResult.builder()
          .analysisType(analysisType)
          .description("이미지 분석 중 오류가 발생했습니다. 다시 시도해주세요.")
          .success(false)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  /**
   * 토큰 사용량 추출
   */
  private Integer extractTokenUsage(ChatResponse response) {
    try {
      var usage = response.getMetadata().getUsage();
      if (usage != null) {
        return (int) (usage.getPromptTokens() + usage.getGenerationTokens());
      }
    } catch (Exception e) {
      log.debug("토큰 사용량 추출 실패", e);
    }
    return null;
  }

  /**
   * GPT Vision 분석 결과 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class GptVisionResult {
    private String analysisType;      // GENERAL, EMERGENCY, ROAD_CROSSING
    private String description;        // GPT-4o가 생성한 설명
    private String model;              // 사용된 모델 (gpt-4o)
    private Integer tokensUsed;        // 사용된 토큰 수
    private Long processingTimeMs;     // 처리 시간 (밀리초)
    private boolean success;           // 성공 여부
    private String errorMessage;       // 오류 메시지 (실패시)

    /**
     * 위험 수준 판단 (description 텍스트 분석)
     */
    public String getDangerLevel() {
      if (description == null) return "UNKNOWN";

      String lowerDesc = description.toLowerCase();
      if (lowerDesc.contains("위험함") || lowerDesc.contains("🚨")) {
        return "HIGH";
      } else if (lowerDesc.contains("조금 위험") || lowerDesc.contains("⚠️")) {
        return "MEDIUM";
      } else if (lowerDesc.contains("안전")) {
        return "SAFE";
      }
      return "UNKNOWN";
    }
  }
}
