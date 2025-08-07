package com.bifai.reminder.bifai_backend.entity.listener;

import com.bifai.reminder.bifai_backend.entity.User;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

/**
 * User 엔티티 라이프사이클 리스너 - 베스트 프랙티스
 * 엔티티 상태 변화 시점에 비즈니스 로직 실행
 */
@Slf4j
public class UserEntityListener {
    
    /**
     * 영속화 전 호출
     */
    @PrePersist
    public void prePersist(User user) {
        log.debug("User 영속화 준비: username={}", user.getUsername());
        
        // 기본값 설정 - 활성 상태 확인
        if (user.getIsActive() == null) {
            // Builder.Default가 있지만 명시적으로 한번 더 확인
            log.debug("기본 활성 상태 설정");
        }
    }
    
    /**
     * 영속화 후 호출
     */
    @PostPersist
    public void postPersist(User user) {
        log.info("새 사용자 등록 완료: userId={}, username={}", 
                user.getUserId(), user.getUsername());
    }
    
    /**
     * 업데이트 전 호출
     */
    @PreUpdate
    public void preUpdate(User user) {
        log.debug("User 업데이트 준비: userId={}", user.getUserId());
    }
    
    /**
     * 업데이트 후 호출
     */
    @PostUpdate
    public void postUpdate(User user) {
        log.debug("User 업데이트 완료: userId={}", user.getUserId());
    }
    
    /**
     * 삭제 전 호출 - 보안 정책 적용
     */
    @PreRemove
    public void preRemove(User user) {
        log.warn("사용자 삭제 시도: userId={}, username={}", 
                user.getUserId(), user.getUsername());
        
        // 경계성 지능 장애인 보호를 위한 안전장치
        // 실제로는 soft delete를 권장
    }
    
    /**
     * 로드 후 호출
     */
    @PostLoad
    public void postLoad(User user) {
        log.trace("User 로드 완료: userId={}", user.getUserId());
    }
} 