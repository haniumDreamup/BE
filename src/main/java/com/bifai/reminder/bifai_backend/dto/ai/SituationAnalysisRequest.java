package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 상황 분석 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SituationAnalysisRequest {
  
  @NotBlank(message = "상황 설명은 필수입니다")
  @Size(max = 1000, message = "상황 설명은 1000자를 넘을 수 없습니다")
  private String situation;
  
  @NotBlank(message = "상황 유형은 필수입니다")
  private String situationType; // SCHEDULE, MEDICATION, EMERGENCY, NAVIGATION, DAILY
  
  private String additionalContext;
  
  private String location;
  
  private byte[] image; // 이미지 분석용 (선택)
}