package com.bifai.reminder.bifai_backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포즈 데이터 브로드캐스트 메시지
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseStreamMessage {
  
  private Long userId;
  
  private Long frameId;
  
  private String sessionId;
  
  private Boolean fallDetected;
  
  private String fallSeverity;
  
  private Float confidenceScore;
  
  private LocalDateTime timestamp;
  
  private String analysisResult; // 포즈 분석 결과
}