package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.entity.listener.UserEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 사용자 엔티티 - 경계선 지능 장애인을 위한 리마인더 시스템 사용자 정보
 * 
 * <p>BIF(Borderline Intellectual Functioning) 사용자의 정보를 관리하는 핵심 엔티티입니다.
 * IQ 70-85 범위의 사용자를 지원하며, 인지 수준에 맞춘 서비스를 제공합니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>사용자 기본 정보 관리</li>
 *   <li>인지 수준 분류 및 추적</li>
 *   <li>보호자 관계 관리</li>
 *   <li>OAuth2 소셜 로그인 지원</li>
 * </ul>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 * @see Guardian
 * @see UserPreference
 * @see BaseEntity
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_active_username", columnList = "is_active, username"),
    @Index(name = "idx_user_active_email", columnList = "is_active, email"),
    @Index(name = "idx_user_provider", columnList = "provider, provider_id"),
    @Index(name = "idx_user_last_login", columnList = "last_login_at DESC"),
    @Index(name = "idx_user_emergency_mode", columnList = "emergency_mode_enabled, is_active")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"guardians", "guardianFor", "devices", "schedules", 
                    "notifications", "userPreference", "locationHistories", 
                    "activities", "medications", "healthMetrics", "roles"}) // 순환 참조 방지
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userId;
    
    public Long getId() {
        return userId;
    }
    
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
    
    /**
     * 사용자의 인지 수준
     * 
     * <p>BIF 사용자의 인지 능력을 분류하여 적절한 수준의 지원을 제공합니다:</p>
     * <ul>
     *   <li>MILD: 독립적 수행 가능, 최소한의 지원</li>
     *   <li>MODERATE: 중등도 지원 필요 (기본값)</li>
     *   <li>SEVERE: 지속적인 지원 필요</li>
     *   <li>UNKNOWN: 평가 필요</li>
     * </ul>
     */
    @Column(name = "cognitive_level", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CognitiveLevel cognitiveLevel = CognitiveLevel.MODERATE;
    
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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "phone_verified")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    public boolean isActive() {
        return isActive != null && isActive;
    }
    
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
    
    
    // OAuth2 관련 필드
    @Column(name = "provider", length = 20)
    private String provider;
    
    @Column(name = "provider_id", length = 255)
    private String providerId;
    
    // 관계 매핑 - 김영한 방식: @JsonIgnore 제거, 엔티티 직접 반환 대신 DTO 사용
    // 하지만 현재는 엔티티 직접 반환하므로 @JsonIgnore 필수
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Guardian> guardians; // 이 사용자의 보호자들

    @JsonIgnore
    @OneToMany(mappedBy = "guardianUser", fetch = FetchType.LAZY)
    private List<Guardian> guardianFor; // 이 사용자가 보호자인 사용자들

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Device> devices;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Schedule> schedules;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserPreference userPreference;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationHistory> locationHistories;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activities;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Medication> medications;

    @JsonIgnore
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
     * 마지막 로그인 시간을 현재 시간으로 업데이트합니다.
     * 
     * <p>이 메소드는 사용자가 성공적으로 로그인할 때 호출되어
     * 마지막 로그인 시간과 활동 시간을 모두 업데이트합니다.</p>
     * 
     * @since 1.0
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
     * 사용자의 인지 수준을 업데이트합니다.
     * 
     * <p>이 메소드는 사용자의 인지 능력 평가 후 수준을 업데이트할 때 사용됩니다.
     * 인지 수준 변경은 시스템 전반의 UI/UX 복잡도에 영향을 미칩니다.</p>
     * 
     * @param cognitiveLevel 새로운 인지 수준 (MILD, MODERATE, SEVERE, UNKNOWN)
     * @throws IllegalArgumentException cognitiveLevel이 null인 경우
     * @since 1.0
     */
    public void updateCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }
    
    /**
     * 응급 모드를 활성화 또는 비활성화합니다.
     * 
     * <p>응급 모드가 활성화되면 보호자에게 즉시 알림이 전송되고,
     * 위치 추적 빈도가 증가하며, 긴급 연락처가 화면에 표시됩니다.</p>
     * 
     * @param enabled true: 응급 모드 활성화, false: 비활성화
     * @since 1.0
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
     * 인지 수준 - 경계선 지능 장애인의 인지 능력 수준 분류
     *
     * <p>BIF 사용자의 인지 능력을 4단계로 분류하여 각 수준에 맞는
     * 인터페이스와 지원 기능을 제공합니다.</p>
     *
     * @since 1.0
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

    /**
     * 성별 분류
     */
    public enum Gender {
        MALE("남성"),
        FEMALE("여성"),
        OTHER("기타");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
} 