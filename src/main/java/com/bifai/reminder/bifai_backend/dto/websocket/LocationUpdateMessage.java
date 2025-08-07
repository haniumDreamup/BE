package com.bifai.reminder.bifai_backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 위치 업데이트 브로드캐스트 메시지
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateMessage {
  
  private Long userId;
  
  private String username;
  
  private Double latitude;
  
  private Double longitude;
  
  private Float accuracy;
  
  private Float speed;
  
  private Float heading;
  
  private String activityType;
  
  private LocalDateTime timestamp;
  
  private String message; // 사용자 친화적 메시지
}