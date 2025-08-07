package com.bifai.reminder.bifai_backend.dto.websocket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개인 메시지 전송 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalMessageRequest {
  
  @NotNull(message = "수신자 ID는 필수입니다")
  private Long targetUserId;
  
  @NotBlank(message = "메시지 내용은 필수입니다")
  private String content;
  
  private MessageType messageType;
  
  private Integer priority; // 1-5 우선순위
  
  private Boolean requiresAcknowledgment;
  
  public enum MessageType {
    TEXT("텍스트"),
    REMINDER("리마인더"),
    INSTRUCTION("지시사항"),
    ENCOURAGEMENT("격려"),
    CHECK_IN("안부 확인");
    
    private final String description;
    
    MessageType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
}