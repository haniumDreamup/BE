package com.bifai.reminder.bifai_backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 활동 상태 브로드캐스트 메시지
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatusMessage {
  
  private Long userId;
  
  private String username;
  
  private String status;
  
  private String statusDescription;
  
  private Integer batteryLevel;
  
  private Integer heartRate;
  
  private Integer stepCount;
  
  private Double latitude;
  
  private Double longitude;
  
  private LocalDateTime timestamp;
  
  private String friendlyMessage; // 사용자 친화적 메시지
}