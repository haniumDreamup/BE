package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.entity.listener.UserEntityListener;
import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 사용자 엔티티 - 경계성 지능 장애인을 위한 리마인더 시스템 사용자 정보
 */
@Entity
@Table(name = "users")
@EntityListeners(UserEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userId;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    
    @Column(name = "full_name", length = 100)
    private String fullName;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "gender", length = 10)
    private String gender;
    
    @Column(name = "cognitive_level", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CognitiveLevel cognitiveLevel = CognitiveLevel.MODERATE;
    
    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;
    
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;
    
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;
    
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Seoul";
    
    @Column(name = "language_preference", length = 10)
    @Builder.Default
    private String languagePreference = "ko";
    
    @Column(name = "language_preference_secondary", length = 10)
    private String languagePreferenceSecondary;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "emergency_mode_enabled")
    @Builder.Default
    private Boolean emergencyModeEnabled = false;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(name = "password_reset_token")
    private String passwordResetToken;
    
    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;
    
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;
    
    // 관계 매핑
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Guardian> guardians; // 이 사용자의 보호자들
    
    @OneToMany(mappedBy = "guardianUser", fetch = FetchType.LAZY)
    private List<Guardian> guardianFor; // 이 사용자가 보호자인 사용자들
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Device> devices;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Schedule> schedules;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserPreference userPreference;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationHistory> locationHistories;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activities;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Medication> medications;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthMetric> healthMetrics;
    
    // 사용자 권한 (다대다 관계)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        indexes = {
            @Index(name = "idx_user_roles_user", columnList = "user_id"),
            @Index(name = "idx_user_roles_role", columnList = "role_id")
        }
    )
    private Set<Role> roles;
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * 비밀번호 해시 업데이트
     */
    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    /**
     * 인지 수준 업데이트
     */
    public void updateCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }
    
    /**
     * 응급 모드 활성화/비활성화
     */
    public void setEmergencyMode(boolean enabled) {
        this.emergencyModeEnabled = enabled;
    }
    
    /**
     * 비밀번호 재설정 토큰 생성
     */
    public void createPasswordResetToken(String token, LocalDateTime expiresAt) {
        this.passwordResetToken = token;
        this.passwordResetExpiresAt = expiresAt;
    }
    
    /**
     * 비밀번호 재설정 토큰 초기화
     */
    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetExpiresAt = null;
    }
    
    /**
     * 이메일 인증 완료
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }
    
    /**
     * 전화번호 인증 완료
     */
    public void verifyPhone() {
        this.phoneVerified = true;
    }
    
    /**
     * 베스트 프랙티스: ID 기반 equals 구현 (null 안전)
     * - ID가 null인 경우 객체 참조 동등성 사용
     * - ID가 있는 경우 ID 값으로 동등성 판단
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        
        // ID가 null인 경우 (아직 영속화되지 않은 엔티티) 객체 참조로만 비교
        if (this.userId == null || other.userId == null) {
            return false;
        }
        
        return this.userId.equals(other.userId);
    }
    
    /**
     * 베스트 프랙티스: 안정적인 hashCode 구현
     * - ID가 null일 때는 클래스 기반 해시 사용
     * - ID가 있을 때는 ID 기반 해시 사용
     */
    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : getClass().hashCode();
    }
    
    /**
     * 베스트 프랙티스: 순환 참조 방지 toString
     * - 기본 필드만 포함, 연관관계는 제외
     */
    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', email='%s'}", 
                           userId, username, email);
    }
    
    
    /**
     * 인지 수준 - 경계성 지능 장애인의 인지 능력 수준을 분류
     */
    public enum CognitiveLevel {
        MILD("경미", "독립적 수행 가능"),
        MODERATE("중등도", "최소한의 지원 필요"),
        SEVERE("심각", "지속적인 지원 필요"),
        UNKNOWN("미정", "평가 필요");
        
        private final String displayName;
        private final String description;
        
        CognitiveLevel(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 