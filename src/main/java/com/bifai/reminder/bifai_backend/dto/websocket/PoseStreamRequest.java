package com.bifai.reminder.bifai_backend.dto.websocket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 포즈 데이터 스트리밍 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseStreamRequest {
  
  @NotNull(message = "프레임 ID는 필수입니다")
  private Long frameId;
  
  @NotNull(message = "랜드마크 데이터는 필수입니다")
  private List<PoseLandmark> landmarks;
  
  private Float confidenceScore;
  
  private Long timestamp;
  
  private String sessionId;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PoseLandmark {
    private Integer id;
    private Float x;
    private Float y;
    private Float z;
    private Float visibility;
  }
}