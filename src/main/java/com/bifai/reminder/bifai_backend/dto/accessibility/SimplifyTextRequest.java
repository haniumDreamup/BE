package com.bifai.reminder.bifai_backend.dto.accessibility;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 텍스트 간소화 요청 DTO
 */
@Data
public class SimplifyTextRequest {
  
  @NotBlank(message = "텍스트는 필수입니다")
  private String text;
  
  private String targetLevel; // grade3, grade5, grade7, adult
}