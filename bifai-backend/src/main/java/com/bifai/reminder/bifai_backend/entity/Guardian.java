package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

/**
 * 보호자 엔티티 - BIF 사용자의 보호자 정보 관리
 * 경계성 지능 장애인의 안전과 지원을 위한 보호자 시스템
 */
@Entity
@Table(name = "guardians", 
       indexes = {
           @Index(name = "idx_guardian_user", columnList = "user_id"),
           @Index(name = "idx_guardian_guardian_user", columnList = "guardian_user_id"),
           @Index(name = "idx_guardian_relationship", columnList = "relationship_type"),
           @Index(name = "idx_guardian_approval", columnList = "approval_status")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_guardian", columnNames = {"user_id", "guardian_user_id"})
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Guardian extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    // BIF 사용자 (보호를 받는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // 보호자 사용자 (보호를 제공하는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_user_id", nullable = false)
    private User guardianUser;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 50, nullable = false)
    private String relationship; // 자유 텍스트 관계 설명
    
    @Column(name = "relationship_type", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RelationshipType relationshipType = RelationshipType.OTHER;
    
    @Column(name = "primary_phone", nullable = false, length = 20)
    private String primaryPhone;
    
    @Column(name = "secondary_phone", length = 20)
    private String secondaryPhone;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 500)
    private String address;
    
    @Column(name = "emergency_priority")
    @Builder.Default
    private Integer emergencyPriority = 1;
    
    @Column(name = "notification_preferences", columnDefinition = "TEXT")
    private String notificationPreferences; // JSON
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "can_modify_settings")
    @Builder.Default
    private Boolean canModifySettings = false;
    
    @Column(name = "can_view_location")
    @Builder.Default
    private Boolean canViewLocation = true;
    
    @Column(name = "can_receive_alerts")
    @Builder.Default
    private Boolean canReceiveAlerts = true;
    
    @Column(name = "approval_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;
    
    @Column(name = "approval_note", columnDefinition = "TEXT")
    private String approvalNote;
    
    @Column(name = "last_activity_at")
    private java.time.LocalDateTime lastActivityAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 보호자 정보 업데이트
     */
    public void updateContactInfo(String primaryPhone, String secondaryPhone, String email) {
        this.primaryPhone = primaryPhone;
        this.secondaryPhone = secondaryPhone;
        this.email = email;
    }
    
    /**
     * 권한 설정 업데이트
     */
    public void updatePermissions(boolean canModifySettings, boolean canViewLocation, boolean canReceiveAlerts) {
        this.canModifySettings = canModifySettings;
        this.canViewLocation = canViewLocation;
        this.canReceiveAlerts = canReceiveAlerts;
    }
    
    /**
     * 주 보호자 설정
     */
    public void setPrimaryGuardian(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    /**
     * 승인 처리
     */
    public void approve(String approvalNote) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedAt = java.time.LocalDateTime.now();
        this.approvalNote = approvalNote;
    }
    
    /**
     * 거부 처리
     */
    public void reject(String rejectionReason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approvalNote = rejectionReason;
    }
    
    /**
     * 활동 시간 업데이트
     */
    public void updateLastActivity() {
        this.lastActivityAt = java.time.LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guardian)) return false;
        Guardian guardian = (Guardian) o;
        return id != null && id.equals(guardian.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Guardian{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", relationshipType=" + relationshipType +
                ", isPrimary=" + isPrimary +
                ", approvalStatus=" + approvalStatus +
                '}';
    }
    
    /**
     * 승인 상태
     */
    public enum ApprovalStatus {
        PENDING("대기"),
        APPROVED("승인"),
        REJECTED("거부");
        
        private final String description;
        
        ApprovalStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 보호자와 사용자의 관계 유형
     */
    public enum RelationshipType {
        PARENT("부모"),
        SIBLING("형제자매"),
        CAREGIVER("돌봄제공자"),
        DOCTOR("의사"),
        OTHER("기타");
        
        private final String description;
        
        RelationshipType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
} 