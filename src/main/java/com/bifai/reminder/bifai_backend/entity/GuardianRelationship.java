package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 보호자와 사용자 간의 관계를 정의하는 엔티티
 * 다중 보호자 지원 및 세밀한 권한 관리
 */
@Entity
@Table(name = "guardian_relationships", 
  indexes = {
    @Index(name = "idx_guardian_user", columnList = "guardian_id, user_id"),
    @Index(name = "idx_relationship_status", columnList = "status"),
    @Index(name = "idx_invitation_token", columnList = "invitation_token", unique = true)
  },
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"guardian_id", "user_id"})
  }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRelationship {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "relationship_id")
  private Long relationshipId;
  
  /**
   * 보호자 (Guardian 엔티티)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "guardian_id", nullable = false)
  private Guardian guardian;
  
  /**
   * 피보호자 (User 엔티티)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  /**
   * 관계 유형
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "relationship_type", nullable = false, length = 30)
  private RelationshipType relationshipType;
  
  /**
   * 권한 레벨
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "permission_level", nullable = false, length = 20)
  private PermissionLevel permissionLevel;
  
  /**
   * 관계 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RelationshipStatus status;
  
  /**
   * 초대 토큰 (보호자 초대 시 사용)
   */
  @Column(name = "invitation_token", length = 100)
  private String invitationToken;
  
  /**
   * 초대 만료 시간
   */
  @Column(name = "invitation_expires_at")
  private LocalDateTime invitationExpiresAt;
  
  /**
   * 관계 승인 날짜
   */
  @Column(name = "approved_at")
  private LocalDateTime approvedAt;
  
  /**
   * 관계 승인자 (사용자 또는 기존 보호자)
   */
  @Column(name = "approved_by", length = 50)
  private String approvedBy;
  
  /**
   * 권한 설정 (JSON)
   * 예: {"viewLocation": true, "viewHealth": true, "manageMedication": false, "viewActivity": true}
   */
  @Column(name = "permission_settings", columnDefinition = "JSON")
  private String permissionSettings;
  
  /**
   * 긴급 연락 우선순위 (1이 가장 높음)
   */
  @Column(name = "emergency_priority")
  private Integer emergencyPriority;
  
  /**
   * 알림 설정 (JSON)
   * 예: {"sms": true, "push": true, "email": false}
   */
  @Column(name = "notification_preferences", columnDefinition = "JSON")
  private String notificationPreferences;
  
  /**
   * 관계 설명 또는 메모
   */
  @Column(name = "notes", length = 500)
  private String notes;
  
  /**
   * 마지막 활동 시간 (대시보드 접근 등)
   */
  @Column(name = "last_active_at")
  private LocalDateTime lastActiveAt;
  
  /**
   * 관계 생성 날짜
   */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  
  /**
   * 관계 수정 날짜
   */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
  
  /**
   * 관계 종료 날짜 (관계가 종료된 경우)
   */
  @Column(name = "terminated_at")
  private LocalDateTime terminatedAt;
  
  /**
   * 관계 종료 사유
   */
  @Column(name = "termination_reason", length = 200)
  private String terminationReason;
  
  /**
   * 관계 유형 열거형
   */
  public enum RelationshipType {
    PARENT("부모"),
    SPOUSE("배우자"),
    CHILD("자녀"),
    SIBLING("형제자매"),
    RELATIVE("친척"),
    CAREGIVER("간병인"),
    PROFESSIONAL("전문 도우미"),
    FRIEND("친구"),
    OTHER("기타");
    
    private final String description;
    
    RelationshipType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 권한 레벨 열거형
   */
  public enum PermissionLevel {
    VIEW_ONLY("읽기 전용"),
    MANAGE("관리"),
    EMERGENCY("긴급 상황만"),
    FULL("전체 권한");
    
    private final String description;
    
    PermissionLevel(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 관계 상태 열거형
   */
  public enum RelationshipStatus {
    PENDING("대기 중"),
    ACTIVE("활성"),
    SUSPENDED("일시 중지"),
    REJECTED("거부됨"),
    EXPIRED("만료됨"),
    TERMINATED("종료됨");
    
    private final String description;
    
    RelationshipStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 활성 상태 확인
   */
  public boolean isActive() {
    return status == RelationshipStatus.ACTIVE;
  }
  
  /**
   * 긴급 상황 접근 권한 확인
   */
  public boolean hasEmergencyAccess() {
    return isActive() && 
      (permissionLevel == PermissionLevel.EMERGENCY || 
       permissionLevel == PermissionLevel.MANAGE || 
       permissionLevel == PermissionLevel.FULL);
  }
  
  /**
   * 관리 권한 확인
   */
  public boolean hasManagePermission() {
    return isActive() && 
      (permissionLevel == PermissionLevel.MANAGE || 
       permissionLevel == PermissionLevel.FULL);
  }
  
  /**
   * 전체 권한 확인
   */
  public boolean hasFullPermission() {
    return isActive() && permissionLevel == PermissionLevel.FULL;
  }
  
  /**
   * 초대 만료 확인
   */
  public boolean isInvitationExpired() {
    return invitationExpiresAt != null && 
      LocalDateTime.now().isAfter(invitationExpiresAt);
  }
  
  /**
   * 관계 활성화
   */
  public void activate(String approvedBy) {
    this.status = RelationshipStatus.ACTIVE;
    this.approvedAt = LocalDateTime.now();
    this.approvedBy = approvedBy;
    this.invitationToken = null;
    this.invitationExpiresAt = null;
  }
  
  /**
   * 관계 일시 중지
   */
  public void suspend() {
    if (status == RelationshipStatus.ACTIVE) {
      this.status = RelationshipStatus.SUSPENDED;
    }
  }
  
  /**
   * 관계 재활성화
   */
  public void reactivate() {
    if (status == RelationshipStatus.SUSPENDED) {
      this.status = RelationshipStatus.ACTIVE;
    }
  }
  
  /**
   * 관계 종료
   */
  public void terminate(String reason) {
    this.status = RelationshipStatus.TERMINATED;
    this.terminatedAt = LocalDateTime.now();
    this.terminationReason = reason;
  }
  
  /**
   * 활동 시간 업데이트
   */
  public void updateLastActiveTime() {
    this.lastActiveAt = LocalDateTime.now();
  }
}