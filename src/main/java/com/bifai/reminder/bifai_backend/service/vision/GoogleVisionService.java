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
    // 명시적 타임아웃 설정 (ClientHttpRequestFactory 사용)
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(15))   // 연결 타임아웃: 15초
        .setReadTimeout(Duration.ofSeconds(30))      // 읽기 타임아웃: 30초 (앱 타임아웃보다 짧게)
        .build();

    log.info("GoogleVisionService 초기화 - 타임아웃: 연결 15초, 읽기 30초");
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

      // OpenAI API 호출 (재시도 로직 포함)
      log.info("OpenAI API 호출 중... (모델: {})", model);
      ResponseEntity<Map> response = callOpenAIWithRetry(request, 3);

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
   * OpenAI API 호출 (재시도 로직)
   */
  private ResponseEntity<Map> callOpenAIWithRetry(HttpEntity<Map<String, Object>> request, int maxRetries) throws IOException {
    int attempt = 0;
    IOException lastException = null;

    while (attempt < maxRetries) {
      attempt++;
      try {
        log.info("OpenAI API 호출 시도 {}/{}", attempt, maxRetries);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.openai.com/v1/chat/completions",
            request,
            Map.class
        );

        // 성공
        log.info("✅ OpenAI API 호출 성공 (시도 {}회)", attempt);
        return response;

      } catch (org.springframework.web.client.HttpServerErrorException e) {
        // 5xx 에러 - 재시도 가능
        lastException = new IOException("OpenAI API 서버 오류: " + e.getMessage(), e);
        log.warn("⚠️ OpenAI API 서버 오류 (시도 {}/{}): {}", attempt, maxRetries, e.getStatusCode());

        if (attempt < maxRetries) {
          try {
            // 지수 백오프: 1초 → 2초 → 4초
            long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
            log.info("{}ms 후 재시도...", waitTime);
            Thread.sleep(waitTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("재시도 대기 중 인터럽트 발생", ie);
          }
        }

      } catch (org.springframework.web.client.HttpClientErrorException e) {
        // 4xx 에러 - 재시도 불가능 (인증, 요청 오류 등)
        log.error("❌ OpenAI API 클라이언트 오류 (재시도 불가): {}", e.getStatusCode());
        throw new IOException("OpenAI API 요청 오류: " + e.getMessage(), e);

      } catch (org.springframework.web.client.ResourceAccessException e) {
        // 타임아웃 또는 네트워크 오류
        lastException = new IOException("OpenAI API 타임아웃 또는 네트워크 오류: " + e.getMessage(), e);
        log.warn("⚠️ OpenAI API 타임아웃/네트워크 오류 (시도 {}/{}): {}", attempt, maxRetries, e.getMessage());

        if (attempt < maxRetries) {
          try {
            long waitTime = (long) Math.pow(2, attempt - 1) * 1000;
            log.info("{}ms 후 재시도...", waitTime);
            Thread.sleep(waitTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("재시도 대기 중 인터럽트 발생", ie);
          }
        }

      } catch (Exception e) {
        // 기타 오류
        log.error("❌ OpenAI API 호출 중 예상치 못한 오류: {}", e.getClass().getName() + " - " + e.getMessage());
        throw new IOException("OpenAI API 호출 실패: " + e.getMessage(), e);
      }
    }

    // 모든 재시도 실패
    log.error("❌ OpenAI API 호출 실패 - {}회 시도 모두 실패", maxRetries);
    throw lastException != null ? lastException : new IOException("OpenAI API 호출 실패");
  }

  /**
   * 1단계: 빠른 요약 프롬프트 (Chain-of-Thought)
   */
  private String getQuickAnalysisPrompt() {
    return """
        당신은 경계성 지능 사용자(IQ 70-85)의 눈입니다.
        단계별로 생각하고 명확하게 설명하세요.

        사고 과정:
        1. See (관찰): 무엇이 보이나요?
        2. Think (분석): 위험한 것은? 주요 상황은?
        3. Confirm (결론): 사용자가 알아야 할 핵심은?

        응답 형식 (이모티콘 없이, 반드시 이 순서로):

        긴급 (위험 있을 때만)
        위험: [위험물] ([위치])
        즉시 행동: [행동] ([이유])

        여기는
        [장소]. [주요 활동].
        [핵심 상황 1-2문장]

        할 일
        1. [행동] ([이유])
        2. [행동] ([이유])

        자세히
        [상세 설명: 물건, 색상, 위치 등]

        중요 원칙:
        1. 이모티콘 사용 금지 (TTS가 읽음)
        2. 위험 없으면 "긴급" 섹션 생략
        3. 맥락 간결하게 (1-2문장)
        4. 모든 행동에 이유 추가 ("~니까", "~으니")
        5. 구체적 수치 (50cm, 5분, 3개)
        6. 85% 이상 확신만
        7. 추측 금지 ("아마도", "~같아요" ❌)
        8. 안전 최우선
        9. 짧고 명확하게
        """;
  }

  /**
   * 2단계: 상세 분석 프롬프트 (CoT + Few-Shot)
   */
  private String getDetailedAnalysisPrompt() {
    return """
        당신은 경계선 지능 사용자(IQ 70-85)의 눈입니다.
        Be My AI, Google Lookout처럼 상세하고 정확하게 설명하세요.

        예시 1 - 주방 (위험 있음):

        긴급
        위험: 가스불 (왼쪽 50cm)
        즉시 행동: 1m 뒤로 물러나세요 (뜨거우니까)

        여기는
        주방입니다. 요리 중입니다.
        가스레인지에 냄비가 있고 김이 모락모락 나고 있어요.

        할 일
        1. 당근을 썰어서 넣으세요 (물이 충분히 끓었으니)
        2. 타이머 5분 맞추세요 (당근 익는 시간)

        자세히
        오른쪽 70cm에 나무 도마가 있고 당근 3개가 통째로 놓여 있습니다.
        은색 칼도 함께 있어요.
        사람은 없습니다.

        ---

        예시 2 - 횡단보도 (위험 있음):

        긴급
        위험: 신호등 빨간불
        즉시 행동: 멈추세요 (차가 지나가니까)

        여기는
        횡단보도 앞입니다.
        신호등이 빨간불이에요.
        오른쪽에서 차 2대가 지나가고 있습니다.

        할 일
        1. 초록불까지 기다리세요 (약 30초 걸려요)
        2. 초록불 되면 좌우 확인하세요 (차가 완전히 멈췄는지)
        3. 천천히 건너세요 (15초 정도 걸립니다)

        자세히
        횡단보도는 흰색 줄무늬이고 앞쪽 1미터에 있습니다.
        검은색 차와 흰색 차가 오른쪽에서 지나가고 있고
        옆에 다른 사람 한 명이 대기 중입니다.

        ---

        예시 3 - 공원 (위험 없음):

        여기는
        공원 산책로입니다. 날씨가 화창해요.
        앞쪽에 벤치가 2개 있고 주변에 나무들이 많습니다.

        할 일
        1. 산책로를 따라 직진하세요 (길이 넓고 평평하니까)
        2. 피곤하면 벤치에 앉으세요 (앞쪽 5미터에 있어요)

        자세히
        갈색 나무 벤치 2개가 앞쪽 5미터에 나란히 있습니다.
        주변에 큰 나무 5그루가 있고 산책로는 회색 돌로 되어 있어요.
        사람은 보이지 않습니다.

        ---

        이제 이 형식으로 사진을 분석하세요.

        중요 원칙:
        1. 이모티콘 사용 금지 (TTS가 읽음)
        2. 위험 없으면 "긴급" 섹션 생략
        3. 맥락 간결하게 (1-2문장)
        4. 모든 행동에 이유 추가 ("~니까", "~으니")
        5. 구체적 수치 (3개, 50cm, 5분)
        6. 공간별 정리 (앞/뒤/좌/우)
        7. 색상, 상태 필수
        8. 85% 이상 확신만 말하기
        9. 추측 금지 ("아마도", "~같아요" ❌)
        10. 안전이 최우선
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
