package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 텍스트 간소화 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimplifyTextRequest {
  
  @NotBlank(message = "텍스트는 필수입니다")
  @Size(max = 5000, message = "텍스트는 5000자를 넘을 수 없습니다")
  private String text;
}