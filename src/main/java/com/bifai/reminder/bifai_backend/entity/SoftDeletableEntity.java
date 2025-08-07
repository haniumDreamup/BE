package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Soft Delete 기능을 가진 엔티티의 추상 클래스
 * 
 * <p>물리적 삭제 대신 논리적 삭제를 사용해야 하는 중요한 엔티티들이 상속받습니다.
 * BaseEntity의 모든 기능과 함께 Soft Delete 기능을 제공합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <ul>
 *   <li>User - 사용자 정보는 삭제 후에도 기록 유지 필요</li>
 *   <li>Guardian - 보호자 관계 이력 추적 필요</li>
 *   <li>Device - 기기 사용 이력 감사 목적</li>
 * </ul>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 * @see BaseEntity
 * @see SoftDeletable
 */
@MappedSuperclass
@Getter
public abstract class SoftDeletableEntity extends BaseEntity implements SoftDeletable {
    
    /**
     * 삭제 여부
     * 
     * <p>true인 경우 논리적으로 삭제된 상태입니다.
     * 기본값은 false(활성 상태)입니다.</p>
     */
    @Column(name = "deleted")
    private Boolean deleted = false;
    
    /**
     * 삭제 시간
     * 
     * <p>엔티티가 삭제된 정확한 시간을 기록합니다.
     * 삭제되지 않은 경우 null입니다.</p>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 삭제자 정보
     * 
     * <p>엔티티를 삭제한 사용자의 ID 또는 이름을 저장합니다.
     * 감사 추적을 위해 사용됩니다.</p>
     */
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
    
    /**
     * 엔티티를 논리적으로 삭제합니다.
     * 
     * <p>실제로 데이터베이스에서 삭제하지 않고 deleted 플래그를 true로 설정합니다.
     * 삭제 시간과 삭제자 정보도 함께 기록됩니다.</p>
     * 
     * @param deletedBy 삭제를 수행하는 사용자 정보
     */
    @Override
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    /**
     * 삭제된 엔티티를 복원합니다.
     * 
     * <p>deleted 플래그를 false로 설정하고 삭제 관련 정보를 초기화합니다.
     * 복원 이력은 별도로 관리해야 합니다.</p>
     */
    @Override
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
    
    /**
     * 엔티티의 삭제 여부를 확인합니다.
     * 
     * @return true: 삭제됨, false: 활성 상태
     */
    @Override
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}