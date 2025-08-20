package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

/**
 * ARIA 라벨 응답 DTO
 */
@Data
@Builder
public class AriaLabelResponse {
  
  private String label;
  
  private String elementType;
  
  private String hint;
}