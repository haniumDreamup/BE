package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * 권한 엔티티 - 사용자 역할 및 권한 관리
 * BIF 시스템의 계층적 권한 구조
 */
@Entity
@Table(name = "roles",
       indexes = {
           @Index(name = "idx_role_name", columnList = "name", unique = true)
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(length = 255)
    private String description;
    
    @Column(name = "korean_name", length = 100)
    private String koreanName;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    // 사용자와의 다대다 관계 - 양방향 관계 제거 (순환 참조 방지)
    // @ManyToMany(mappedBy = "roles")
    // private Set<User> users;
    
    /**
     * 권한 활성화/비활성화
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return id != null && id.equals(role.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", koreanName='" + koreanName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
    
    /**
     * 시스템 기본 권한
     */
    public enum SystemRole {
        USER("ROLE_USER", "사용자", "BIF 일반 사용자"),
        GUARDIAN("ROLE_GUARDIAN", "보호자", "BIF 사용자의 보호자"),
        MEDICAL_STAFF("ROLE_MEDICAL_STAFF", "의료진", "의료 전문가"),
        ADMIN("ROLE_ADMIN", "관리자", "시스템 관리자"),
        SUPER_ADMIN("ROLE_SUPER_ADMIN", "최고 관리자", "모든 권한을 가진 관리자");
        
        private final String roleName;
        private final String koreanName;
        private final String description;
        
        SystemRole(String roleName, String koreanName, String description) {
            this.roleName = roleName;
            this.koreanName = koreanName;
            this.description = description;
        }
        
        public String getRoleName() {
            return roleName;
        }
        
        public String getKoreanName() {
            return koreanName;
        }
        
        public String getDescription() {
            return description;
        }
    }
}