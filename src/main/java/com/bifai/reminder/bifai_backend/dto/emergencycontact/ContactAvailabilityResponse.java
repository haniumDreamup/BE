package com.bifai.reminder.bifai_backend.dto.emergencycontact;

import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 연락처 가용성 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactAvailabilityResponse {

  private Long contactId;
  
  private String name;
  
  private String relationship;
  
  private String phoneNumber;
  
  private EmergencyContact.ContactMethod preferredContactMethod;
  
  private boolean isAvailable;
  
  private Double responseRate;
  
  private Integer averageResponseTimeMinutes;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lastContactedAt;
}