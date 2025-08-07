package com.bifai.reminder.bifai_backend.dto.ai;

import lombok.*;

/**
 * 텍스트 간소화 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimplifyTextResponse {
  
  private String originalText;
  
  private String simplifiedText;
  
  private int originalLength;
  
  private int simplifiedLength;
}