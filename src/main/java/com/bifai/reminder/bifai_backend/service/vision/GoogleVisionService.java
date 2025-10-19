package com.bifai.reminder.bifai_backend.service.vision;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * GPT-4o Vision 기반 이미지 분석 서비스
 * RestTemplate 직접 사용 (Spring AI ChatClient 타임아웃 이슈 해결)
 */
@Slf4j
@Service
@org.springframework.context.annotation.Profile("!test")
public class GoogleVisionService {

  private final RestTemplate restTemplate;

  @Value("${spring.ai.openai.api-key}")
  private String openaiApiKey;

  @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
  private String model;

  public GoogleVisionService(RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))  // Vision 분석은 최대 60초
        .build();
  }

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
      log.info("GPT-4o-mini Vision 분석 시작 - 파일: {}, 크기: {}bytes",
          imageFile.getOriginalFilename(), imageFile.getSize());

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

      // 이미지 최적화 및 Base64 인코딩
      log.debug("이미지 처리 중...");
      byte[] imageBytes = imageFile.getBytes();
      long originalSize = imageBytes.length;

      // 적응적 리사이즈 (이미지 크기에 따라 다른 해상도 적용)
      if (originalSize > 10 * 1024 * 1024) {
        // 10MB 이상: 2048x2048 (고화질 유지하면서 압축)
        log.info("매우 큰 이미지 ({}MB). 2048x2048로 리사이즈 중...", originalSize / 1024 / 1024);
        imageBytes = resizeImage(imageBytes, 2048, 2048);
      } else if (originalSize > 5 * 1024 * 1024) {
        // 5-10MB: 1536x1536
        log.info("큰 이미지 ({}MB). 1536x1536로 리사이즈 중...", originalSize / 1024 / 1024);
        imageBytes = resizeImage(imageBytes, 1536, 1536);
      } else if (originalSize > 2 * 1024 * 1024) {
        // 2-5MB: 1024x1024
        log.info("중간 크기 이미지 ({}MB). 1024x1024로 리사이즈 중...", originalSize / 1024 / 1024);
        imageBytes = resizeImage(imageBytes, 1024, 1024);
      }

      if (imageBytes.length != originalSize) {
        log.info("리사이즈 완료: {}MB → {}KB",
            originalSize / 1024 / 1024, imageBytes.length / 1024);
      }

      String base64Image = Base64.getEncoder().encodeToString(imageBytes);

      // Base64 인코딩 후 크기 체크 (OpenAI API는 약 20MB 제한)
      long base64Size = base64Image.length();
      if (base64Size > 15 * 1024 * 1024) {  // 15MB로 안전하게 제한
        log.warn("Base64 인코딩 후 이미지가 너무 큽니다 ({}MB). 추가 압축 중...", base64Size / 1024 / 1024);
        // 재귀적으로 더 작게 리사이즈
        imageBytes = resizeImage(imageBytes, 800, 800);
        base64Image = Base64.getEncoder().encodeToString(imageBytes);
        log.info("추가 압축 완료: {}KB", imageBytes.length / 1024);
      }

      // OpenAI API 요청 본문 구성
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("model", model);
      requestBody.put("max_tokens", 1000);

      List<Map<String, Object>> messages = new ArrayList<>();
      Map<String, Object> message = new HashMap<>();
      message.put("role", "user");

      List<Object> content = new ArrayList<>();

      // 텍스트 파트
      Map<String, String> textPart = new HashMap<>();
      textPart.put("type", "text");
      textPart.put("text", prompt);
      content.add(textPart);

      // 이미지 파트
      Map<String, Object> imagePart = new HashMap<>();
      imagePart.put("type", "image_url");
      Map<String, String> imageUrl = new HashMap<>();
      imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
      imagePart.put("image_url", imageUrl);
      content.add(imagePart);

      message.put("content", content);
      messages.add(message);
      requestBody.put("messages", messages);

      // HTTP 헤더 설정
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Authorization", "Bearer " + openaiApiKey);

      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

      // OpenAI API 호출
      log.info("OpenAI API 호출 중... (모델: {})", model);
      ResponseEntity<Map> response = restTemplate.postForEntity(
          "https://api.openai.com/v1/chat/completions",
          request,
          Map.class
      );

      long duration = System.currentTimeMillis() - startTime;

      // 응답 파싱
      Map<String, Object> responseBody = response.getBody();
      if (responseBody == null) {
        throw new IOException("OpenAI API 응답이 비어있습니다");
      }

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
      if (choices == null || choices.isEmpty()) {
        throw new IOException("OpenAI API 응답에 choices가 없습니다");
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> firstChoice = choices.get(0);
      @SuppressWarnings("unchecked")
      Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
      String gptDescription = (String) messageObj.get("content");

      log.info("✅ GPT-4o-mini Vision 분석 완료 - 소요시간: {}ms, 응답길이: {}자",
          duration, gptDescription != null ? gptDescription.length() : 0);

      // 기존 VisionAnalysisResult 형식으로 변환
      return VisionAnalysisResult.builder()
          .simpleDescription(gptDescription)
          .objects(new ArrayList<>())
          .labels(new ArrayList<>())
          .faces(new ArrayList<>())
          .build();

    } catch (Exception e) {
      log.error("❌ GPT-4o-mini Vision 분석 실패 - 파일: {}, 에러: {}",
          imageFile.getOriginalFilename(), e.getMessage(), e);

      // 상세 에러 정보 로깅
      if (e.getCause() != null) {
        log.error("원인: {}", e.getCause().getMessage());
      }

      throw new IOException("이미지 분석 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 이미지 리사이즈 (성능 최적화)
   */
  private byte[] resizeImage(byte[] imageBytes, int maxWidth, int maxHeight) throws IOException {
    try {
      // BufferedImage로 변환
      java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
      java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(bais);

      if (originalImage == null) {
        log.warn("이미지를 읽을 수 없습니다. 원본 사용");
        return imageBytes;
      }

      int width = originalImage.getWidth();
      int height = originalImage.getHeight();

      // 이미 작으면 리사이즈 안함
      if (width <= maxWidth && height <= maxHeight) {
        return imageBytes;
      }

      // 비율 유지하면서 리사이즈
      double ratio = Math.min((double) maxWidth / width, (double) maxHeight / height);
      int newWidth = (int) (width * ratio);
      int newHeight = (int) (height * ratio);

      log.debug("이미지 리사이즈: {}x{} -> {}x{}", width, height, newWidth, newHeight);

      java.awt.Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH);
      java.awt.image.BufferedImage bufferedResized = new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D g2d = bufferedResized.createGraphics();
      g2d.drawImage(resizedImage, 0, 0, null);
      g2d.dispose();

      // JPEG로 변환
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      javax.imageio.ImageIO.write(bufferedResized, "jpg", baos);
      return baos.toByteArray();

    } catch (Exception e) {
      log.error("이미지 리사이즈 실패: {}", e.getMessage());
      return imageBytes;  // 실패시 원본 사용
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
