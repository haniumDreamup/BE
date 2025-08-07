package com.bifai.reminder.bifai_backend.dto.emergency;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 긴급 상황 해결 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveEmergencyRequest {

  @Size(max = 1000, message = "해결 메모는 1000자 이내로 입력해주세요")
  private String resolutionNotes;

  @Size(max = 100, message = "해결자 이름은 100자 이내여야 합니다")
  private String resolvedBy;
}