package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 일상 대화 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationRequest {
  
  @NotBlank(message = "메시지는 필수입니다")
  @Size(max = 500, message = "메시지는 500자를 넘을 수 없습니다")
  private String message;
}