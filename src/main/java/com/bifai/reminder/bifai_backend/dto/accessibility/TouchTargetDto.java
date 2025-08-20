package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

/**
 * 터치 타겟 정보 DTO
 */
@Data
@Builder
public class TouchTargetDto {
  
  private Integer minSize; // dp
  
  private Integer recommendedSize; // dp
  
  private Integer spacing; // dp
  
  private String deviceType;
  
  private Boolean wcagCompliant;
  
  private String guideline; // WCAG 2.1 AA
}