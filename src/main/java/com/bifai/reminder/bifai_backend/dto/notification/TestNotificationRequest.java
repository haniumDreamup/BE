package com.bifai.reminder.bifai_backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestNotificationRequest {
  
  @NotBlank(message = "제목은 필수입니다")
  private String title;
  
  @NotBlank(message = "내용은 필수입니다")
  private String body;
}