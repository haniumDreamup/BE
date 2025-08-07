package com.bifai.reminder.bifai_backend.dto.emergency;

import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencyType;
import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencySeverity;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 긴급 상황 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyRequest {

  @NotNull(message = "긴급 상황 유형을 선택해주세요")
  private EmergencyType type;

  @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
  @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
  private Double latitude;

  @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
  @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
  private Double longitude;

  @Size(max = 500, message = "주소는 500자 이내로 입력해주세요")
  private String address;

  @Size(max = 1000, message = "설명은 1000자 이내로 입력해주세요")
  private String description;

  private EmergencySeverity severity;

  @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다")
  private String imageUrl;

  @Min(value = 0, message = "낙상 신뢰도는 0 이상이어야 합니다")
  @Max(value = 100, message = "낙상 신뢰도는 100 이하여야 합니다")
  private Double fallConfidence;
}