package com.bifai.reminder.bifai_backend.dto.pose;

import lombok.*;

/**
 * Pose 데이터 처리 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseResponseDto {
  
  private String sessionId;
  private Integer frameCount;
  private Boolean fallDetected;
  private Float confidenceScore;
  private String severity;
  private String message;
  private Long fallEventId;
}