package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 긴급 연락처 엔티티
 * BIF 사용자의 보호자 및 의료진 연락처 관리
 */
@Entity
@Table(name = "emergency_contacts", indexes = {
    @Index(name = "idx_emergency_contact_user_id", columnList = "user_id"),
    @Index(name = "idx_emergency_contact_priority", columnList = "priority"),
    @Index(name = "idx_emergency_contact_type", columnList = "contact_type"),
    @Index(name = "idx_emergency_contact_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contact_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "relationship", nullable = false, length = 50)
  private String relationship;

  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "contact_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private ContactType contactType;

  @Column(name = "priority", nullable = false)
  private Integer priority;

  @Column(name = "is_primary")
  private Boolean isPrimary;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "can_receive_alerts")
  private Boolean canReceiveAlerts;

  @Column(name = "can_access_location")
  private Boolean canAccessLocation;

  @Column(name = "can_access_health_data")
  private Boolean canAccessHealthData;

  @Column(name = "can_make_decisions")
  private Boolean canMakeDecisions;

  @Column(name = "available_start_time")
  private LocalTime availableStartTime;

  @Column(name = "available_end_time")
  private LocalTime availableEndTime;

  @Column(name = "available_days", length = 50)
  private String availableDays;

  @Column(name = "preferred_contact_method", length = 20)
  @Enumerated(EnumType.STRING)
  private ContactMethod preferredContactMethod;

  @Column(name = "language_preference", length = 10)
  private String languagePreference;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "medical_professional")
  private Boolean isMedicalProfessional;

  @Column(name = "specialization", length = 100)
  private String specialization;

  @Column(name = "hospital_name", length = 200)
  private String hospitalName;

  @Column(name = "license_number", length = 50)
  private String licenseNumber;

  @Column(name = "last_contacted_at")
  private LocalDateTime lastContactedAt;

  @Column(name = "contact_count")
  private Integer contactCount;

  @Column(name = "response_rate")
  private Double responseRate;

  @Column(name = "average_response_time_minutes")
  private Integer averageResponseTimeMinutes;

  @Column(name = "verified")
  private Boolean verified;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verification_code", length = 10)
  private String verificationCode;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  /**
   * 연락처 유형
   */
  public enum ContactType {
    FAMILY("가족"),
    GUARDIAN("보호자"),
    DOCTOR("의사"),
    NURSE("간호사"),
    CAREGIVER("요양보호사"),
    EMERGENCY_SERVICE("응급서비스"),
    FRIEND("친구"),
    NEIGHBOR("이웃"),
    OTHER("기타");

    private final String description;

    ContactType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 선호 연락 방법
   */
  public enum ContactMethod {
    PHONE("전화"),
    SMS("문자"),
    EMAIL("이메일"),
    APP("앱 알림"),
    ALL("모두");

    private final String description;

    ContactMethod(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (isPrimary == null) {
      isPrimary = false;
    }
    if (isActive == null) {
      isActive = true;
    }
    if (canReceiveAlerts == null) {
      canReceiveAlerts = true;
    }
    if (canAccessLocation == null) {
      canAccessLocation = false;
    }
    if (canAccessHealthData == null) {
      canAccessHealthData = false;
    }
    if (canMakeDecisions == null) {
      canMakeDecisions = false;
    }
    if (preferredContactMethod == null) {
      preferredContactMethod = ContactMethod.PHONE;
    }
    if (languagePreference == null) {
      languagePreference = "ko";
    }
    if (isMedicalProfessional == null) {
      isMedicalProfessional = false;
    }
    if (verified == null) {
      verified = false;
    }
    if (contactCount == null) {
      contactCount = 0;
    }
    if (responseRate == null) {
      responseRate = 0.0;
    }
  }

  /**
   * 현재 시간에 연락 가능한지 확인
   */
  public boolean isAvailable() {
    if (!isActive || !canReceiveAlerts) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    
    // 요일 확인
    if (availableDays != null && !availableDays.isEmpty()) {
      String dayOfWeek = now.getDayOfWeek().name().substring(0, 3).toUpperCase();
      if (!availableDays.contains(dayOfWeek)) {
        return false;
      }
    }

    // 시간 확인
    if (availableStartTime != null && availableEndTime != null) {
      LocalTime currentTime = now.toLocalTime();
      if (availableStartTime.isBefore(availableEndTime)) {
        return !currentTime.isBefore(availableStartTime) && !currentTime.isAfter(availableEndTime);
      } else {
        // 자정을 넘는 경우
        return !currentTime.isBefore(availableStartTime) || !currentTime.isAfter(availableEndTime);
      }
    }

    return true;
  }

  /**
   * 연락 기록 업데이트
   */
  public void updateContactRecord(boolean responded, long responseTimeMinutes) {
    this.lastContactedAt = LocalDateTime.now();
    this.contactCount = (this.contactCount == null ? 0 : this.contactCount) + 1;
    
    if (responded) {
      double currentResponseRate = this.responseRate == null ? 0 : this.responseRate;
      int currentContactCount = this.contactCount;
      
      // 응답률 재계산
      this.responseRate = ((currentResponseRate * (currentContactCount - 1)) + 1) / currentContactCount;
      
      // 평균 응답 시간 재계산
      if (this.averageResponseTimeMinutes == null) {
        this.averageResponseTimeMinutes = (int) responseTimeMinutes;
      } else {
        this.averageResponseTimeMinutes = 
            (int) ((this.averageResponseTimeMinutes * (currentContactCount - 1) + responseTimeMinutes) / currentContactCount);
      }
    } else {
      // 응답하지 않은 경우 응답률만 업데이트
      double currentResponseRate = this.responseRate == null ? 0 : this.responseRate;
      int currentContactCount = this.contactCount;
      this.responseRate = (currentResponseRate * (currentContactCount - 1)) / currentContactCount;
    }
  }

  /**
   * 의료진 여부 확인
   */
  public boolean isMedicalStaff() {
    return contactType == ContactType.DOCTOR || 
           contactType == ContactType.NURSE || 
           Boolean.TRUE.equals(isMedicalProfessional);
  }

  /**
   * 주요 연락처 여부 확인
   */
  public boolean isPrimaryContact() {
    return Boolean.TRUE.equals(isPrimary) || priority == 1;
  }

  /**
   * 권한 수준 문자열로 반환
   */
  public String getPermissionSummary() {
    StringBuilder permissions = new StringBuilder();
    
    if (Boolean.TRUE.equals(canReceiveAlerts)) {
      permissions.append("알림 수신, ");
    }
    if (Boolean.TRUE.equals(canAccessLocation)) {
      permissions.append("위치 확인, ");
    }
    if (Boolean.TRUE.equals(canAccessHealthData)) {
      permissions.append("건강 정보 열람, ");
    }
    if (Boolean.TRUE.equals(canMakeDecisions)) {
      permissions.append("의사결정 권한, ");
    }
    
    if (permissions.length() > 0) {
      return permissions.substring(0, permissions.length() - 2);
    }
    return "권한 없음";
  }
}