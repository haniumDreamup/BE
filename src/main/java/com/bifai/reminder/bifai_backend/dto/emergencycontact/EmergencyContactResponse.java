package com.bifai.reminder.bifai_backend.dto.emergencycontact;

import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 긴급 연락처 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactResponse {

  private Long id;
  
  private String name;
  
  private String relationship;
  
  private String phoneNumber;
  
  private String email;
  
  private EmergencyContact.ContactType contactType;
  
  private Integer priority;
  
  private Boolean isPrimary;
  
  private Boolean isActive;
  
  private Boolean canReceiveAlerts;
  
  private Boolean canAccessLocation;
  
  private Boolean canAccessHealthData;
  
  private Boolean canMakeDecisions;
  
  private String availableStartTime;
  
  private String availableEndTime;
  
  private String availableDays;
  
  private EmergencyContact.ContactMethod preferredContactMethod;
  
  private String languagePreference;
  
  private String notes;
  
  private Boolean isMedicalProfessional;
  
  private String specialization;
  
  private String hospitalName;
  
  private String licenseNumber;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lastContactedAt;
  
  private Integer contactCount;
  
  private Double responseRate;
  
  private Integer averageResponseTimeMinutes;
  
  private Boolean verified;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime verifiedAt;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;
  
  private boolean isAvailable;
  
  private boolean isMedicalStaff;
  
  private String permissionSummary;
}