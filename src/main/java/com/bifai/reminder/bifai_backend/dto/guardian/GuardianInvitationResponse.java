package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 보호자 초대 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianInvitationResponse {
  
  private Long relationshipId;
  private String invitationToken;
  private LocalDateTime expiresAt;
  private String guardianEmail;
  private String status;
  private String invitationUrl;
}