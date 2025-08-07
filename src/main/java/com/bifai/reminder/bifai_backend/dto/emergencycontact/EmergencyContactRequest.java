package com.bifai.reminder.bifai_backend.dto.emergencycontact;

import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 긴급 연락처 등록/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactRequest {

  @NotBlank(message = "이름을 입력해주세요")
  @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
  private String name;

  @NotBlank(message = "관계를 입력해주세요")
  @Size(max = 50, message = "관계는 50자를 초과할 수 없습니다")
  private String relationship;

  @NotBlank(message = "전화번호를 입력해주세요")
  @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", 
           message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
  private String phoneNumber;

  @Email(message = "올바른 이메일 형식이 아닙니다")
  @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
  private String email;

  @NotBlank(message = "연락처 유형을 선택해주세요")
  private EmergencyContact.ContactType contactType;

  private Integer priority;
  
  private Boolean isPrimary;
  
  private Boolean canReceiveAlerts;
  
  private Boolean canAccessLocation;
  
  private Boolean canAccessHealthData;
  
  private Boolean canMakeDecisions;

  @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", 
           message = "시간 형식이 올바르지 않습니다 (HH:MM)")
  private String availableStartTime;

  @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", 
           message = "시간 형식이 올바르지 않습니다 (HH:MM)")
  private String availableEndTime;

  @Pattern(regexp = "^(MON|TUE|WED|THU|FRI|SAT|SUN)(,(MON|TUE|WED|THU|FRI|SAT|SUN))*$", 
           message = "요일 형식이 올바르지 않습니다")
  private String availableDays;

  private EmergencyContact.ContactMethod preferredContactMethod;

  @Size(max = 10, message = "언어 설정은 10자를 초과할 수 없습니다")
  private String languagePreference;

  @Size(max = 500, message = "메모는 500자를 초과할 수 없습니다")
  private String notes;

  private Boolean isMedicalProfessional;

  @Size(max = 100, message = "전문 분야는 100자를 초과할 수 없습니다")
  private String specialization;

  @Size(max = 200, message = "병원명은 200자를 초과할 수 없습니다")
  private String hospitalName;

  @Size(max = 50, message = "면허번호는 50자를 초과할 수 없습니다")
  private String licenseNumber;

  private Boolean sendVerification;
}