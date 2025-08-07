package com.bifai.reminder.bifai_backend.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개인 메시지 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalMessage {
  
  private Long messageId;
  
  private Long fromUserId;
  
  private String fromUsername;
  
  private Long toUserId;
  
  private String content;
  
  private String messageType;
  
  private Integer priority;
  
  private LocalDateTime timestamp;
  
  private Boolean delivered;
  
  private Boolean read;
  
  private String formattedMessage; // BIF 사용자를 위한 형식화된 메시지
}