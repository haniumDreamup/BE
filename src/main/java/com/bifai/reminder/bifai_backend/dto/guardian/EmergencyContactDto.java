package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;

/**
 * 긴급 연락처 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactDto {
  private Long id;
  private String name;
  private String relationship;
  private String phoneNumber;
  private boolean isPrimary;
}