package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

/**
 * 간소화된 텍스트 응답 DTO
 */
@Data
@Builder
public class SimplifiedTextResponse {
  
  private String originalText;
  
  private String simplifiedText;
  
  private String readingLevel;
  
  private Integer wordCount;
  
  private Integer sentenceCount;
  
  private Double readabilityScore;
}