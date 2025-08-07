package com.bifai.reminder.bifai_backend.entity;

import java.time.LocalDateTime;

/**
 * Soft Delete 기능을 제공하는 인터페이스
 * 
 * <p>BIF 사용자의 중요한 데이터는 물리적 삭제 대신 논리적 삭제를 사용하여
 * 데이터 복구 가능성을 보장하고 감사 추적을 가능하게 합니다.</p>
 * 
 * <p>이 인터페이스를 구현하는 엔티티는 다음 필드를 가져야 합니다:</p>
 * <ul>
 *   <li>deleted (Boolean) - 삭제 여부</li>
 *   <li>deletedAt (LocalDateTime) - 삭제 시간</li>
 *   <li>deletedBy (String) - 삭제자</li>
 * </ul>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
public interface SoftDeletable {
    
    /**
     * 삭제 여부를 반환합니다.
     * 
     * @return true: 삭제됨, false: 활성 상태
     */
    boolean isDeleted();
    
    /**
     * 삭제 시간을 반환합니다.
     * 
     * @return 삭제된 시간, 삭제되지 않았으면 null
     */
    LocalDateTime getDeletedAt();
    
    /**
     * 삭제자 정보를 반환합니다.
     * 
     * @return 삭제한 사용자 ID 또는 이름
     */
    String getDeletedBy();
    
    /**
     * 엔티티를 논리적으로 삭제합니다.
     * 
     * @param deletedBy 삭제를 수행하는 사용자 정보
     */
    void softDelete(String deletedBy);
    
    /**
     * 삭제된 엔티티를 복원합니다.
     */
    void restore();
}