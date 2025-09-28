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
@Getter
public abstract class BaseEntity extends BaseTimeEntity {

} 