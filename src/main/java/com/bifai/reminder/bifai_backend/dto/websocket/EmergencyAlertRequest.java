package com.bifai.reminder.bifai_backend.dto.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 긴급 알림 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlertRequest {
  
  @NotNull(message = "알림 유형은 필수입니다")
  private AlertType alertType;
  
  @NotBlank(message = "알림 메시지는 필수입니다")
  private String message;
  
  private Double latitude;
  
  private Double longitude;
  
  private String locationDescription; // 위치 설명 (예: "집 근처", "공원")
  
  private Integer severityLevel; // 1-5 심각도
  
  private Boolean requiresImmediateAction;
  
  public enum AlertType {
    FALL_DETECTED("낙상 감지"),
    SOS("SOS 요청"),
    MEDICATION_MISSED("약 복용 누락"),
    WANDERING("배회 감지"),
    HEALTH_EMERGENCY("건강 응급상황"),
    DEVICE_ISSUE("기기 문제");
    
    private final String description;
    
    AlertType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
}