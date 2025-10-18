package com.bifai.reminder.bifai_backend.service.vision;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Cloud Vision API 서비스
 * 이미지 분석 및 BIF 사용자를 위한 설명 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(com.google.cloud.vision.v1.ImageAnnotatorClient.class)
public class GoogleVisionService {
  
  private final ImageAnnotatorClient visionClient;
  
  @Value("${google.cloud.vision.max-results:10}")
  private int maxResults;
  
  @Value("${google.cloud.vision.confidence-threshold:0.6}")
  private float confidenceThreshold;

  @Value("${google.cloud.vision.high-confidence-threshold:0.85}")
  private float highConfidenceThreshold;
  
  /**
   * 이미지 종합 분석
   */
  public VisionAnalysisResult analyzeImage(MultipartFile imageFile) throws IOException {
    if (visionClient == null) {
      log.error("Google Vision API 클라이언트가 초기화되지 않았습니다");
      throw new IllegalStateException("이미지 분석 서비스를 사용할 수 없습니다");
    }
    
    if (imageFile == null || imageFile.isEmpty()) {
      log.warn("Empty or null image file provided");
      throw new IllegalArgumentException("이미지 파일이 필요합니다");
    }
    
    if (imageFile.getSize() > 20 * 1024 * 1024) { // 20MB 제한
      log.warn("Image file too large: {} bytes", imageFile.getSize());
      throw new IllegalArgumentException("이미지 파일이 너무 큽니다 (최대 20MB)");
    }
    
    try {
      // 이미지 준비
      ByteString imgBytes = ByteString.copyFrom(imageFile.getBytes());
      Image image = Image.newBuilder().setContent(imgBytes).build();
      
      // 분석 요청 구성
      List<AnnotateImageRequest> requests = new ArrayList<>();
      
      // 1. 객체 감지
      requests.add(AnnotateImageRequest.newBuilder()
          .addFeatures(Feature.newBuilder()
              .setType(Feature.Type.OBJECT_LOCALIZATION)
              .setMaxResults(maxResults))
          .setImage(image)
          .build());
      
      // 2. 라벨 감지 (장면 이해)
      requests.add(AnnotateImageRequest.newBuilder()
          .addFeatures(Feature.newBuilder()
              .setType(Feature.Type.LABEL_DETECTION)
              .setMaxResults(maxResults))
          .setImage(image)
          .build());
      
      // 3. 텍스트 감지
      requests.add(AnnotateImageRequest.newBuilder()
          .addFeatures(Feature.newBuilder()
              .setType(Feature.Type.TEXT_DETECTION))
          .setImage(image)
          .build());
      
      // 4. 안전성 감지
      requests.add(AnnotateImageRequest.newBuilder()
          .addFeatures(Feature.newBuilder()
              .setType(Feature.Type.SAFE_SEARCH_DETECTION))
          .setImage(image)
          .build());
      
      // 5. 얼굴 감지
      requests.add(AnnotateImageRequest.newBuilder()
          .addFeatures(Feature.newBuilder()
              .setType(Feature.Type.FACE_DETECTION)
              .setMaxResults(maxResults))
          .setImage(image)
          .build());
      
      // API 호출
      BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();
      
      // 결과 파싱
      VisionAnalysisResult result = new VisionAnalysisResult();
      
      // 객체 정보
      if (!responses.get(0).getLocalizedObjectAnnotationsList().isEmpty()) {
        result.setObjects(parseObjects(responses.get(0)));
      }
      
      // 라벨 정보
      if (!responses.get(1).getLabelAnnotationsList().isEmpty()) {
        result.setLabels(parseLabels(responses.get(1)));
      }
      
      // 텍스트 정보
      if (!responses.get(2).getTextAnnotationsList().isEmpty()) {
        result.setText(parseText(responses.get(2)));
      }
      
      // 안전성 정보
      if (responses.get(3).hasSafeSearchAnnotation()) {
        result.setSafetyInfo(parseSafety(responses.get(3)));
      }
      
      // 얼굴 정보
      if (!responses.get(4).getFaceAnnotationsList().isEmpty()) {
        result.setFaces(parseFaces(responses.get(4)));
      }
      
      // BIF 사용자를 위한 간단한 설명 생성
      result.setSimpleDescription(generateSimpleDescription(result));
      
      return result;
      
    } catch (Exception e) {
      log.error("이미지 분석 중 오류 발생", e);
      throw new IOException("이미지 분석 실패: " + e.getMessage());
    }
  }
  
  /**
   * 객체 정보 파싱
   */
  private List<DetectedObject> parseObjects(AnnotateImageResponse response) {
    return response.getLocalizedObjectAnnotationsList().stream()
        .filter(obj -> obj.getScore() >= confidenceThreshold)
        .map(obj -> DetectedObject.builder()
            .name(translateToKorean(obj.getName()))
            .confidence(obj.getScore())
            .boundingBox(BoundingBox.from(obj.getBoundingPoly()))
            .build())
        .collect(Collectors.toList());
  }
  
  /**
   * 라벨 정보 파싱
   */
  private List<Label> parseLabels(AnnotateImageResponse response) {
    return response.getLabelAnnotationsList().stream()
        .filter(label -> label.getScore() >= confidenceThreshold)
        .map(label -> Label.builder()
            .description(translateToKorean(label.getDescription()))
            .confidence(label.getScore())
            .build())
        .collect(Collectors.toList());
  }
  
  /**
   * 텍스트 정보 파싱
   */
  private String parseText(AnnotateImageResponse response) {
    if (response.getTextAnnotationsList().isEmpty()) {
      return null;
    }
    // 첫 번째 텍스트 어노테이션은 전체 텍스트
    return response.getTextAnnotations(0).getDescription();
  }
  
  /**
   * 안전성 정보 파싱
   */
  private SafetyInfo parseSafety(AnnotateImageResponse response) {
    SafeSearchAnnotation safety = response.getSafeSearchAnnotation();
    return SafetyInfo.builder()
        .adult(safety.getAdult().name())
        .violence(safety.getViolence().name())
        .medical(safety.getMedical().name())
        .build();
  }
  
  /**
   * 얼굴 정보 파싱
   */
  private List<FaceInfo> parseFaces(AnnotateImageResponse response) {
    return response.getFaceAnnotationsList().stream()
        .map(face -> FaceInfo.builder()
            .joy(face.getJoyLikelihood().name())
            .sorrow(face.getSorrowLikelihood().name())
            .anger(face.getAngerLikelihood().name())
            .surprise(face.getSurpriseLikelihood().name())
            .confidence(face.getDetectionConfidence())
            .build())
        .collect(Collectors.toList());
  }
  
  /**
   * BIF 사용자를 위한 간단한 설명 생성
   */
  private String generateSimpleDescription(VisionAnalysisResult result) {
    StringBuilder description = new StringBuilder();

    // 1. 신뢰도별로 객체 분류
    List<DetectedObject> highConfidence = result.getObjects().stream()
        .filter(obj -> obj.getConfidence() >= highConfidenceThreshold)
        .collect(Collectors.toList());

    List<DetectedObject> mediumConfidence = result.getObjects().stream()
        .filter(obj -> obj.getConfidence() >= confidenceThreshold
            && obj.getConfidence() < highConfidenceThreshold)
        .collect(Collectors.toList());

    // 2. 확실하게 보이는 것 (85% 이상)
    if (!highConfidence.isEmpty()) {
      Map<String, Integer> counts = new HashMap<>();
      for (DetectedObject obj : highConfidence) {
        counts.merge(obj.getName(), 1, Integer::sum);
      }

      description.append("✓ 확실히 보여요: ");
      List<String> items = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : counts.entrySet()) {
        if (entry.getValue() == 1) {
          items.add(entry.getKey());
        } else {
          items.add(entry.getKey() + " " + entry.getValue() + "개");
        }
      }
      description.append(String.join(", ", items)).append("\n");
    }

    // 3. 아마도 있을 것 같은 것 (60-85%)
    if (!mediumConfidence.isEmpty()) {
      Map<String, Integer> counts = new HashMap<>();
      for (DetectedObject obj : mediumConfidence) {
        counts.merge(obj.getName(), 1, Integer::sum);
      }

      description.append("? 아마도 있을 것 같아요: ");
      List<String> items = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : counts.entrySet()) {
        if (entry.getValue() == 1) {
          items.add(entry.getKey());
        } else {
          items.add(entry.getKey() + " " + entry.getValue() + "개");
        }
      }
      description.append(String.join(", ", items)).append("\n");
    }

    // 4. 사람 정보 + 감정 (있다면)
    if (!result.getFaces().isEmpty()) {
      description.append("👤 사람 ").append(result.getFaces().size()).append("명");

      // 첫 번째 사람의 감정이 뚜렷하면 추가
      FaceInfo mainFace = result.getFaces().get(0);
      if ("VERY_LIKELY".equals(mainFace.getJoy())) {
        description.append(" (웃고 있어요 😊)");
      } else if ("LIKELY".equals(mainFace.getJoy())) {
        description.append(" (기분이 좋아 보여요)");
      } else if ("VERY_LIKELY".equals(mainFace.getSorrow())) {
        description.append(" (슬퍼 보여요 😢)");
      } else if ("VERY_LIKELY".equals(mainFace.getAnger())) {
        description.append(" (화나 보여요 😠)");
      } else if ("VERY_LIKELY".equals(mainFace.getSurprise())) {
        description.append(" (놀란 것 같아요 😮)");
      }
      description.append("\n");
    }

    // 5. 텍스트가 있으면
    if (result.getText() != null && !result.getText().isEmpty()) {
      String text = result.getText().trim();
      if (text.length() > 50) {
        text = text.substring(0, 50) + "...";
      }
      description.append("📝 글자: ").append(text).append("\n");
    }

    // 6. ⚠️ 안전성 경고 (가장 중요하므로 마지막에 강조)
    if (result.getSafetyInfo() != null && isUnsafe(result.getSafetyInfo())) {
      description.append("\n⚠️ 주의: 조심해야 할 내용이 있어요!\n");
    }

    // 7. 아무것도 없으면
    if (description.length() == 0) {
      return "사진을 확인했지만 특별한 것을 찾지 못했어요";
    }

    return description.toString().trim();
  }
  
  /**
   * 안전성 체크
   */
  private boolean isUnsafe(SafetyInfo safety) {
    if (safety == null) {
      log.debug("Safety info is null, assuming safe");
      return false;
    }
    
    boolean adultUnsafe = isLikelihoodUnsafe(safety.getAdult());
    boolean violenceUnsafe = isLikelihoodUnsafe(safety.getViolence());
    
    log.debug("Safety check - Adult: {}, Violence: {}", safety.getAdult(), safety.getViolence());
    return adultUnsafe || violenceUnsafe;
  }
  
  /**
   * 위험도 수준 확인
   */
  private boolean isLikelihoodUnsafe(String likelihood) {
    if (likelihood == null) {
      return false;
    }
    
    return !likelihood.equals("VERY_UNLIKELY") && !likelihood.equals("UNLIKELY");
  }
  
  // 단어 사전을 클래스 레벨 상수로 이동하여 성능 개선
  private static final Map<String, String> KOREAN_DICTIONARY;
  
  static {
    Map<String, String> dictionary = new HashMap<>();

    // 🚨 응급/위험 관련 (최우선)
    dictionary.put("fire", "불");
    dictionary.put("smoke", "연기");
    dictionary.put("blood", "피");
    dictionary.put("injury", "부상");
    dictionary.put("ambulance", "구급차");
    dictionary.put("police car", "경찰차");
    dictionary.put("warning", "경고");
    dictionary.put("danger", "위험");
    dictionary.put("emergency", "응급");

    // 🚦 도로/교통 안전
    dictionary.put("road", "길");
    dictionary.put("street", "거리");
    dictionary.put("crosswalk", "횡단보도");
    dictionary.put("traffic light", "신호등");
    dictionary.put("stop sign", "정지 표지판");
    dictionary.put("sidewalk", "인도");
    dictionary.put("pedestrian", "보행자");

    // 👥 사람
    dictionary.put("person", "사람");
    dictionary.put("man", "남자");
    dictionary.put("woman", "여자");
    dictionary.put("child", "아이");
    dictionary.put("baby", "아기");
    dictionary.put("boy", "소년");
    dictionary.put("girl", "소녀");

    // 👔 의류
    dictionary.put("clothing", "옷");
    dictionary.put("suit", "정장");
    dictionary.put("jacket", "재킷");
    dictionary.put("coat", "코트");
    dictionary.put("shirt", "셔츠");
    dictionary.put("pants", "바지");
    dictionary.put("dress", "드레스");
    dictionary.put("shoe", "신발");
    dictionary.put("shoes", "신발");
    dictionary.put("hat", "모자");
    dictionary.put("tie", "넥타이");
    dictionary.put("glasses", "안경");
    dictionary.put("sunglasses", "선글라스");
    dictionary.put("glove", "장갑");
    dictionary.put("gloves", "장갑");

    // 🚗 교통수단
    dictionary.put("car", "자동차");
    dictionary.put("bus", "버스");
    dictionary.put("truck", "트럭");
    dictionary.put("bicycle", "자전거");
    dictionary.put("motorcycle", "오토바이");
    dictionary.put("taxi", "택시");
    dictionary.put("train", "기차");
    dictionary.put("subway", "지하철");

    // 🐾 동물
    dictionary.put("dog", "강아지");
    dictionary.put("cat", "고양이");
    dictionary.put("bird", "새");
    dictionary.put("fish", "물고기");
    dictionary.put("animal", "동물");

    // 🍽️ 음식
    dictionary.put("food", "음식");
    dictionary.put("fruit", "과일");
    dictionary.put("vegetable", "채소");
    dictionary.put("drink", "음료");
    dictionary.put("coffee", "커피");
    dictionary.put("tea", "차");
    dictionary.put("water", "물");
    dictionary.put("rice", "밥");
    dictionary.put("bread", "빵");
    dictionary.put("noodle", "면");
    dictionary.put("soup", "국");
    dictionary.put("meat", "고기");
    dictionary.put("chicken", "닭고기");
    dictionary.put("beef", "소고기");
    dictionary.put("pork", "돼지고기");

    // 🏢 가구/건물
    dictionary.put("tree", "나무");
    dictionary.put("building", "건물");
    dictionary.put("house", "집");
    dictionary.put("table", "테이블");
    dictionary.put("chair", "의자");
    dictionary.put("door", "문");
    dictionary.put("window", "창문");
    dictionary.put("bed", "침대");
    dictionary.put("sofa", "소파");
    dictionary.put("desk", "책상");

    // 📱 전자기기
    dictionary.put("phone", "전화기");
    dictionary.put("smartphone", "스마트폰");
    dictionary.put("computer", "컴퓨터");
    dictionary.put("laptop", "노트북");
    dictionary.put("tablet", "태블릿");
    dictionary.put("television", "TV");
    dictionary.put("tv", "TV");
    dictionary.put("camera", "카메라");

    // 🏪 장소
    dictionary.put("hospital", "병원");
    dictionary.put("pharmacy", "약국");
    dictionary.put("store", "가게");
    dictionary.put("restaurant", "식당");
    dictionary.put("bank", "은행");
    dictionary.put("post office", "우체국");
    dictionary.put("school", "학교");
    dictionary.put("office", "사무실");

    // 🌤️ 날씨/자연
    dictionary.put("sky", "하늘");
    dictionary.put("cloud", "구름");
    dictionary.put("rain", "비");
    dictionary.put("snow", "눈");
    dictionary.put("sun", "태양");
    dictionary.put("flower", "꽃");
    dictionary.put("grass", "풀");
    dictionary.put("mountain", "산");
    dictionary.put("river", "강");

    // 🏠 실내 물품
    dictionary.put("refrigerator", "냉장고");
    dictionary.put("microwave", "전자레인지");
    dictionary.put("sink", "싱크대");
    dictionary.put("toilet", "화장실");
    dictionary.put("shower", "샤워기");
    dictionary.put("mirror", "거울");
    dictionary.put("clock", "시계");
    dictionary.put("lamp", "램프");
    dictionary.put("light", "불빛");

    // 📚 기타 일상
    dictionary.put("book", "책");
    dictionary.put("bag", "가방");
    dictionary.put("umbrella", "우산");
    dictionary.put("watch", "시계");
    dictionary.put("bottle", "병");
    dictionary.put("cup", "컵");
    dictionary.put("plate", "접시");
    dictionary.put("spoon", "숟가락");
    dictionary.put("fork", "포크");
    dictionary.put("knife", "칼");
    dictionary.put("pen", "펜");
    dictionary.put("pencil", "연필");
    dictionary.put("paper", "종이");

    KOREAN_DICTIONARY = Collections.unmodifiableMap(dictionary);
  }
  
  /**
   * 영어를 한국어로 번역 (간단한 매핑)
   */
  private String translateToKorean(String english) {
    if (english == null || english.trim().isEmpty()) {
      log.debug("Empty or null English text provided for translation");
      return "알 수 없음";
    }
    
    String normalized = english.toLowerCase().trim();
    String korean = KOREAN_DICTIONARY.get(normalized);
    
    if (korean == null) {
      log.debug("No Korean translation found for: {}", english);
      return english; // 번역이 없으면 원본 반환
    }
    
    return korean;
  }
  
  /**
   * 분석 결과 DTO
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
    private String simpleDescription;
    
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
    
    public static BoundingBox from(BoundingPoly poly) {
      if (poly == null) {
        log.warn("BoundingPoly is null");
        return null;
      }
      
      if (poly.getNormalizedVerticesCount() < 2) {
        log.warn("BoundingPoly has insufficient vertices: {}", poly.getNormalizedVerticesCount());
        return createDefaultBoundingBox();
      }
      
      try {
        return BoundingBox.builder()
            .x1(poly.getNormalizedVertices(0).getX())
            .y1(poly.getNormalizedVertices(0).getY())
            .x2(poly.getNormalizedVertices(1).getX())
            .y2(poly.getNormalizedVertices(1).getY())
            .build();
      } catch (Exception e) {
        log.error("Failed to create BoundingBox from BoundingPoly", e);
        return createDefaultBoundingBox();
      }
    }
    
    private static BoundingBox createDefaultBoundingBox() {
      return BoundingBox.builder()
          .x1(0.0f)
          .y1(0.0f)
          .x2(1.0f)
          .y2(1.0f)
          .build();
    }
  }
}