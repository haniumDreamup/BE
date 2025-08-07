package com.bifai.reminder.bifai_backend.dto.emergency;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 낙상 감지 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FallDetectionRequest {

  @NotNull(message = "낙상 신뢰도를 입력해주세요")
  @DecimalMin(value = "0.0", message = "낙상 신뢰도는 0 이상이어야 합니다")
  @DecimalMax(value = "100.0", message = "낙상 신뢰도는 100 이하여야 합니다")
  private Double confidence;

  @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
  @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
  private Double latitude;

  @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
  @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
  private Double longitude;

  @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다")
  private String imageUrl;

  @Size(max = 1000, message = "추가 정보는 1000자 이내로 입력해주세요")
  private String additionalInfo;

  private String deviceId;
  
  private LocalDateTime timestamp;
  
  private Long userId;
  
  private Float bodyAngle;
  
  private Float velocity;
}