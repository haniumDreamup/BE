package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 긴급 상황 안내 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyGuideRequest {
  
  @NotBlank(message = "상황 설명은 필수입니다")
  private String situation;
  
  private String location;
}