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
   * 1단계: 빠른 요약 분석 (3-5초)
   * Chain-of-Thought 방식으로 즉각적인 안전 정보 제공
   */
  public VisionAnalysisResult analyzeImageQuick(MultipartFile imageFile) throws IOException {
    return analyzeImageInternal(imageFile, false);
  }

  /**
   * 2단계: 상세 분석 (10-15초)
   * CoT + Few-Shot + Structured Output으로 완벽한 정보 제공
   */
  public VisionAnalysisResult analyzeImageDetailed(MultipartFile imageFile) throws IOException {
    return analyzeImageInternal(imageFile, true);
  }

  /**
   * 이미지 종합 분석 (기존 API 호환 - 1단계 빠른 분석)
   */
  public VisionAnalysisResult analyzeImage(MultipartFile imageFile) throws IOException {
    return analyzeImageQuick(imageFile);
  }

  /**
   * 내부 분석 메서드
   */
  private VisionAnalysisResult analyzeImageInternal(MultipartFile imageFile, boolean detailed) throws IOException {
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

      String prompt;

      if (!detailed) {
        // 1단계: 빠른 요약 (Chain-of-Thought)
        prompt = getQuickAnalysisPrompt();
      } else {
        // 2단계: 상세 분석 (CoT + Few-Shot + Structured Output)
        prompt = getDetailedAnalysisPrompt();
      }

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
   * 1단계: 빠른 요약 프롬프트 (Chain-of-Thought)
   */
  private String getQuickAnalysisPrompt() {
    return """
        당신은 경계선 지능 사용자(IQ 70-85)의 눈입니다.
        단계별로 생각하고 빠르게 요약하세요.

        사고 과정:
        1. See (관찰): 무엇이 보이나요?
        2. Think (분석): 위험한 것은? 주요 상황은?
        3. Confirm (결론): 사용자가 알아야 할 핵심은?

        응답 형식:

        📌 한 줄 요약
        [장소]. [주요 활동]

        ⚠️ 위험 (있으면)
        - [위험물] ([위치]): [즉시 행동]

        📍 주요 물건
        - [객체 3-5개] (위치, 색상, 상태)

        💬 지금 상황
        [2-3문장으로 전체 상황 설명]

        🎯 다음 행동
        [구체적 행동 1-2개]

        규칙:
        - 85% 이상 확신만
        - 추측 금지
        - 구체적 거리/시간
        - 안전 최우선
        - 짧고 명확하게
        """;
  }

  /**
   * 2단계: 상세 분석 프롬프트 (CoT + Few-Shot)
   */
  private String getDetailedAnalysisPrompt() {
    return """
        당신은 경계선 지능 사용자(IQ 70-85)의 눈입니다.
        Be My AI, Google Lookout처럼 상세하고 정확하게 설명하세요.

        예시 1 - 주방:
        📌 실내 주방. 요리 중

        ⚠️ 위험
        - 가스불 (왼쪽 50cm): 1m 떨어지세요
        - 뜨거운 김 (앞쪽 30cm): 얼굴 가까이 대지 마세요

        📍 있는 것
        앞쪽:
        - 조리대 1개 (깨끗함)

        왼쪽 (50cm):
        - 가스레인지 1개 (불 켜짐 🔥)
        - 냄비 1개 (검은색, 김 나옴)

        오른쪽 (70cm):
        - 도마 1개 (나무)
        - 당근 3개 (통째로)
        - 칼 1개 (은색)

        👤 사람
        사람 없음

        💬 지금 상황
        주방 조리대 앞입니다. 왼쪽 가스레인지에 검은 냄비가 있고 김이 모락모락 나고 있어요. 오른쪽에는 당근 3개가 도마 위에 준비되어 있습니다.

        🎯 다음 행동
        1. 물이 충분히 끓었어요
        2. 오른쪽 도마로 가세요 (70cm)
        3. 당근을 썰어서 냄비에 넣으세요 (2-3분)
        4. 타이머 5분 맞추세요

        ---

        예시 2 - 횡단보도:
        📌 횡단보도 앞. 신호 대기

        ⚠️ 위험
        - 빨간불: 멈추세요
        - 차 2대 (오른쪽): 지나갈 때까지 대기

        📍 있는 것
        앞쪽 (1m):
        - 횡단보도 (흰색 줄무늬)
        - 신호등 1개 (빨간불)

        오른쪽:
        - 차 2대 (검은색, 흰색)
        - 사람 1명 (대기 중)

        💬 지금 상황
        횡단보도 앞에 있습니다. 신호등이 빨간불이에요. 오른쪽에서 차 2대가 지나가고 있습니다.

        🎯 다음 행동
        1. 초록불 기다리세요 (약 30초)
        2. 초록불 되면 좌우 확인하세요
        3. 천천히 건너세요 (15초 걸려요)

        ---

        이제 이 형식으로 사진을 분석하세요.

        중요 원칙:
        1. 85% 이상 확신만 말하기
        2. 추측 금지 ("아마도", "~같아요" ❌)
        3. 구체적 수치 (3개, 50cm, 5분)
        4. 공간별 정리 (앞/뒤/좌/우)
        5. 색상, 상태 필수
        6. 단계별 행동 가이드
        7. 안전이 최우선
        """;
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
