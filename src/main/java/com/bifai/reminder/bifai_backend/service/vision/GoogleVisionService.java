package com.bifai.reminder.bifai_backend.service.vision;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * GPT-4o Vision 기반 이미지 분석 서비스
 * (기존 GoogleVisionService를 GPT-4o로 교체)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class GoogleVisionService {

  private final ChatClient.Builder chatClientBuilder;

  @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
  private String model;

  /**
   * 이미지 종합 분석 (기존 API 호환)
   */
  public VisionAnalysisResult analyzeImage(MultipartFile imageFile) throws IOException {
    if (imageFile == null || imageFile.isEmpty()) {
      log.warn("Empty or null image file provided");
      throw new IllegalArgumentException("이미지 파일이 필요합니다");
    }

    if (imageFile.getSize() > 20 * 1024 * 1024) {
      log.warn("Image file too large: {} bytes", imageFile.getSize());
      throw new IllegalArgumentException("이미지 파일이 너무 큽니다 (최대 20MB)");
    }

    long startTime = System.currentTimeMillis();

    try {
      String prompt = """
          당신은 인지 능력이 낮은 사용자(IQ 70-85)를 돕는 AI 비서입니다.

          이 사진을 분석하고 아래 형식으로 설명해주세요:

          **🚨 위험 상황** (있을 경우만)
          - 불, 연기, 피, 날카로운 물건 등 발견시
          - "⚠️⚠️⚠️ 위험할 수 있어요! [발견된 것]: 안전한 곳으로 이동하세요!"

          **📍 지금 상황**
          - 한 문장으로 무슨 상황인지 (예: "실내에서 식사 중이에요", "길을 건널 준비를 하고 있어요")

          **✓ 확실히 보이는 것**
          - 85% 이상 확신하는 물건들 쉼표로 나열 (예: "테이블, 의자, 사람 2명")

          **? 아마도 있는 것**
          - 60-85% 확신하는 것들 (예: "컵, 접시")

          **👤 사람 정보**
          - 몇 명인지 + 표정/감정 (예: "사람 2명 (웃고 있어요 😊)")

          **📝 글자** (사진에 텍스트가 있으면)
          - 50자 이내로 요약

          **💡 도움말** (유용한 조언이 있으면)
          - 예: "빨간불이에요. 초록불을 기다리세요"
          - 예: "횡단보도가 보여요. 좌우를 살펴보세요"

          규칙:
          - 초등학교 5학년이 이해할 수 있는 쉬운 말
          - 한 문장은 15단어 이하
          - 위험은 최상단에 ⚠️로 강조
          - 없는 섹션은 생략
          - 이모지 사용 (😊😢😠😮 등)
          """;

      // 이미지를 Resource로 변환
      byte[] imageBytes = imageFile.getBytes();
      ByteArrayResource imageResource = new ByteArrayResource(imageBytes);

      // ChatClient로 요청 (Spring AI 1.0.0-M7 방식)
      ChatClient chatClient = chatClientBuilder.build();
      String gptDescription = chatClient.prompt()
          .user(u -> u.text(prompt)
              .media(MimeTypeUtils.IMAGE_JPEG, imageResource))
          .call()
          .content();

      long duration = System.currentTimeMillis() - startTime;

      log.info("GPT-4o Vision 분석 완료 - 소요시간: {}ms", duration);

      // 기존 VisionAnalysisResult 형식으로 변환
      return VisionAnalysisResult.builder()
          .simpleDescription(gptDescription)
          .objects(new ArrayList<>())  // GPT는 구조화된 객체 리스트 안줌
          .labels(new ArrayList<>())
          .faces(new ArrayList<>())
          .build();

    } catch (Exception e) {
      log.error("GPT-4o Vision 분석 중 오류 발생", e);
      throw new IOException("이미지 분석 실패: " + e.getMessage());
    }
  }

  /**
   * 기존 DTO 호환성 유지
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class VisionAnalysisResult {
    @Builder.Default
    private List<DetectedObject> objects = new ArrayList<>();
    @Builder.Default
    private List<Label> labels = new ArrayList<>();
    private String text;
    private SafetyInfo safetyInfo;
    @Builder.Default
    private List<FaceInfo> faces = new ArrayList<>();
    private String simpleDescription;  // GPT-4o 생성 설명

    public static VisionAnalysisResult empty() {
      return VisionAnalysisResult.builder()
          .simpleDescription("이미지를 분석할 수 없습니다")
          .build();
    }
  }

  @lombok.Data
  @lombok.Builder
  public static class DetectedObject {
    private String name;
    private float confidence;
    private BoundingBox boundingBox;
  }

  @lombok.Data
  @lombok.Builder
  public static class Label {
    private String description;
    private float confidence;
  }

  @lombok.Data
  @lombok.Builder
  public static class SafetyInfo {
    private String adult;
    private String violence;
    private String medical;
  }

  @lombok.Data
  @lombok.Builder
  public static class FaceInfo {
    private String joy;
    private String sorrow;
    private String anger;
    private String surprise;
    private float confidence;
  }

  @lombok.Data
  @lombok.Builder
  public static class BoundingBox {
    private float x1, y1, x2, y2;
  }
}
