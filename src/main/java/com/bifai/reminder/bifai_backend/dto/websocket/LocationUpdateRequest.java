package com.bifai.reminder.bifai_backend.dto.websocket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 위치 업데이트 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {
  
  @NotNull(message = "위도는 필수입니다")
  private Double latitude;
  
  @NotNull(message = "경도는 필수입니다")
  private Double longitude;
  
  private Float accuracy; // 위치 정확도 (미터)
  
  private Float speed; // 이동 속도 (m/s)
  
  private Float heading; // 방향 (0-360도)
  
  private Long timestamp; // 클라이언트 타임스탬프
  
  private String activityType; // 활동 유형 (WALKING, DRIVING, STATIONARY 등)
}