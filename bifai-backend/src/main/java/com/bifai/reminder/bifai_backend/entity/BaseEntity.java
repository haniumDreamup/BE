package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기본 엔티티 - 등록일, 수정일, 등록자, 수정자 자동 관리 + Soft Delete 지원
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity extends BaseTimeEntity {
    
    @CreatedBy
    @Column(updatable = false, length = 100)
    private String createdBy;
    
    @LastModifiedBy
    @Column(length = 100)
    private String modifiedBy;
    
    /**
     * Soft Delete 지원 - 베스트 프랙티스
     * 경계성 지능 장애인의 중요한 데이터는 물리적 삭제 대신 논리적 삭제 사용
     */
    @Column(name = "deleted")
    private Boolean deleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
    
    /**
     * 소프트 딜리트 실행
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
    
    /**
     * 소프트 딜리트 복원
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
    
    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
} 