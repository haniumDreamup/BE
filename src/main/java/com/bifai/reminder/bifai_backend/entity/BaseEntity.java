package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 기본 엔티티 - 등록일, 수정일, 등록자, 수정자 자동 관리
 * 
 * <p>모든 주요 엔티티가 상속받는 기본 클래스로, JPA Auditing을 통해
 * 생성/수정 시간과 생성자/수정자 정보를 자동으로 관리합니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 * @see BaseTimeEntity
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity extends BaseTimeEntity {
    
    /**
     * 엔티티 생성자
     * 
     * <p>Spring Security Context에서 자동으로 설정됩니다.
     * 최초 생성 후에는 수정되지 않습니다.</p>
     */
    @CreatedBy
    @Column(updatable = false, length = 100)
    private String createdBy;
    
    /**
     * 엔티티 수정자
     * 
     * <p>마지막으로 엔티티를 수정한 사용자 정보를 저장합니다.
     * 엔티티가 수정될 때마다 자동으로 업데이트됩니다.</p>
     */
    @LastModifiedBy
    @Column(length = 100)
    private String modifiedBy;
} 