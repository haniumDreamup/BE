package com.bifai.reminder.bifai_backend.dto.websocket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 활동 상태 업데이트 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatusRequest {
  
  @NotNull(message = "활동 상태는 필수입니다")
  private ActivityStatus status;
  
  private String description; // 추가 설명
  
  private Integer batteryLevel; // 배터리 잔량 (0-100)
  
  private Integer heartRate; // 심박수 (선택사항)
  
  private Integer stepCount; // 걸음 수
  
  private Double currentLatitude;
  
  private Double currentLongitude;
  
  public enum ActivityStatus {
    ACTIVE("활동 중"),
    RESTING("휴식 중"),
    SLEEPING("수면 중"),
    EATING("식사 중"),
    EXERCISING("운동 중"),
    TRAVELING("이동 중"),
    WORKING("작업 중"),
    SOCIALIZING("사교 활동 중");
    
    private final String description;
    
    ActivityStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
}