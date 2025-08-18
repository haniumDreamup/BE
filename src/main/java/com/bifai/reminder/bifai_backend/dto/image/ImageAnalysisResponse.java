package com.bifai.reminder.bifai_backend.dto.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이미지 분석 결과 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageAnalysisResponse {

  private Long analysisId;
  
  private String imageUrl;
  
  private String analysisStatus; // UPLOADED, PROCESSING, COMPLETED, FAILED
  
  // 상황 설명 (간단한 한국어로)
  private String situationDescription;
  
  // 추천 행동
  private String actionSuggestion;
  
  // 안전 수준
  private String safetyLevel; // SAFE, CAUTION, WARNING, DANGER
  
  // 객체 인식 결과
  private List<DetectedObject> detectedObjects;
  
  // 추출된 텍스트
  private String extractedText;
  
  // 긴급 상황 여부
  private Boolean emergencyDetected;
  
  // 음성 안내 전송 여부
  private Boolean voiceGuidanceSent;
  
  // 처리 시간
  private Long processingTimeMs;
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime analyzedAt;

  /**
   * 감지된 객체
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DetectedObject {
    private String label;        // 객체 이름 (한국어)
    private String englishLabel; // 객체 이름 (영어)
    private Float confidence;    // 확신도 (0.0 ~ 1.0)
    private BoundingBox bbox;    // 바운딩 박스
    private String description;  // 간단한 설명
  }

  /**
   * 바운딩 박스
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BoundingBox {
    private Integer x;      // 시작 X 좌표
    private Integer y;      // 시작 Y 좌표
    private Integer width;  // 너비
    private Integer height; // 높이
  }

  /**
   * 간단한 응답 생성
   */
  public static ImageAnalysisResponse simple(String description, String safetyLevel) {
    return ImageAnalysisResponse.builder()
        .situationDescription(description)
        .safetyLevel(safetyLevel)
        .analysisStatus("COMPLETED")
        .build();
  }
}