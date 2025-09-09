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
  
  @Value("${google.cloud.vision.confidence-threshold:0.7}")
  private float confidenceThreshold;
  
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
    
    // 주요 객체 설명
    if (!result.getObjects().isEmpty()) {
      Map<String, Integer> objectCounts = new HashMap<>();
      for (DetectedObject obj : result.getObjects()) {
        objectCounts.merge(obj.getName(), 1, Integer::sum);
      }
      
      description.append("발견한 것: ");
      List<String> items = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : objectCounts.entrySet()) {
        if (entry.getValue() == 1) {
          items.add(entry.getKey());
        } else {
          items.add(entry.getKey() + " " + entry.getValue() + "개");
        }
      }
      description.append(String.join(", ", items)).append("\n");
    }
    
    // 텍스트가 있으면
    if (result.getText() != null && !result.getText().isEmpty()) {
      description.append("글자가 있어요: ").append(result.getText().trim()).append("\n");
    }
    
    // 안전성 경고
    if (result.getSafetyInfo() != null) {
      if (isUnsafe(result.getSafetyInfo())) {
        description.append("⚠️ 주의가 필요한 내용이 있어요\n");
      }
    }
    
    // 사람 얼굴
    if (!result.getFaces().isEmpty()) {
      description.append("사람 ").append(result.getFaces().size()).append("명이 보여요\n");
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
    dictionary.put("person", "사람");
    dictionary.put("car", "자동차");
    dictionary.put("dog", "강아지");
    dictionary.put("cat", "고양이");
    dictionary.put("food", "음식");
    dictionary.put("tree", "나무");
    dictionary.put("building", "건물");
    dictionary.put("phone", "전화기");
    dictionary.put("computer", "컴퓨터");
    dictionary.put("book", "책");
    dictionary.put("table", "테이블");
    dictionary.put("chair", "의자");
    dictionary.put("door", "문");
    dictionary.put("window", "창문");
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