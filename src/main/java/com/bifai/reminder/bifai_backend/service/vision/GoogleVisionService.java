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
      log.warn("Google Vision API 클라이언트가 초기화되지 않았습니다");
      return VisionAnalysisResult.empty();
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
    return !safety.getAdult().equals("VERY_UNLIKELY") && !safety.getAdult().equals("UNLIKELY") ||
           !safety.getViolence().equals("VERY_UNLIKELY") && !safety.getViolence().equals("UNLIKELY");
  }
  
  /**
   * 영어를 한국어로 번역 (간단한 매핑)
   */
  private String translateToKorean(String english) {
    // 실제로는 번역 API나 사전을 사용해야 하지만, 여기서는 주요 단어만 매핑
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
    
    return dictionary.getOrDefault(english.toLowerCase(), english);
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
      if (poly.getNormalizedVerticesCount() >= 2) {
        return BoundingBox.builder()
            .x1(poly.getNormalizedVertices(0).getX())
            .y1(poly.getNormalizedVertices(0).getY())
            .x2(poly.getNormalizedVertices(1).getX())
            .y2(poly.getNormalizedVertices(1).getY())
            .build();
      }
      return null;
    }
  }
}