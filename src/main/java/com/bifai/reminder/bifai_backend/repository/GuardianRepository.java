package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guardian Repository - 보호자 데이터 접근 계층
 * JPA 기능을 최대한 활용한 Repository 패턴 구현
 */
@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    
    /**
     * Soft Delete를 고려한 기본 조회 메서드
     */
    @Query("SELECT g FROM Guardian g WHERE g.isActive = true")
    List<Guardian> findAllActive();
    
    @Query("SELECT g FROM Guardian g WHERE g.isActive = true")
    Page<Guardian> findAllActive(Pageable pageable);
    
    @Query("SELECT g FROM Guardian g WHERE g.id = :id AND g.isActive = true")
    Optional<Guardian> findActiveById(@Param("id") Long id);
    
    /**
     * 연락처 기반 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.primaryPhone = :phoneNumber AND g.isActive = true")
    Optional<Guardian> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    @Query("SELECT g FROM Guardian g WHERE g.email = :email AND g.isActive = true")
    Optional<Guardian> findByEmail(@Param("email") String email);
    
    /**
     * 이름으로 보호자 검색 (부분 매치)
     */
    @Query("SELECT g FROM Guardian g WHERE g.name LIKE %:name% AND g.isActive = true ORDER BY g.name")
    List<Guardian> findByNameContaining(@Param("name") String name);
    
    /**
     * 주 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.isPrimary = true AND g.isActive = true")
    List<Guardian> findPrimaryGuardians();
    
    /**
     * 활성 상태의 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.isActive = true ORDER BY g.isPrimary DESC, g.name")
    List<Guardian> findActiveGuardians();
    
    /**
     * 관계 유형별 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.relationshipType = :relationshipType AND g.isActive = true")
    List<Guardian> findByRelationshipType(@Param("relationshipType") Guardian.RelationshipType relationshipType);
    
    /**
     * 승인 상태별 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.approvalStatus = :approvalStatus AND g.isActive = true")
    List<Guardian> findByApprovalStatus(@Param("approvalStatus") Guardian.ApprovalStatus approvalStatus);
    
    /**
     * 특정 사용자의 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId AND g.isActive = true AND g.user.isActive = true")
    List<Guardian> findByUser_UserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 주 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId AND g.isPrimary = true AND g.isActive = true AND g.user.isActive = true")
    Optional<Guardian> findPrimaryGuardianByUserId(@Param("userId") Long userId);
    
    /**
     * 연락처 중복 체크
     */
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.primaryPhone = :phoneNumber AND g.id != :excludeId AND g.isActive = true")
    boolean existsByPhoneNumberAndNotId(@Param("phoneNumber") String phoneNumber, @Param("excludeId") Long excludeId);
    
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.email = :email AND g.id != :excludeId AND g.isActive = true")
    boolean existsByEmailAndNotId(@Param("email") String email, @Param("excludeId") Long excludeId);
    
    /**
     * 관리 중인 사용자가 있는 보호자 조회
     */
    @Query("SELECT DISTINCT g FROM Guardian g WHERE g.user IS NOT NULL AND g.user.isActive = true AND g.isActive = true")
    List<Guardian> findGuardiansWithActiveUsers();
    
    /**
     * 통계 쿼리 - 보호자별 관리 사용자 수
     */
    @Query("SELECT g.id, g.name, g.relationshipType, COUNT(DISTINCT g.user) FROM Guardian g WHERE g.isActive = true GROUP BY g.id, g.name, g.relationshipType")
    List<Object[]> getGuardianStatistics();
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 사용자의 보호자 목록 조회
     */
    List<Guardian> findByUser(User user);
    
    /**
     * 보호자 사용자별 피보호자 조회
     */
    List<Guardian> findByGuardianUser(User guardianUser);
    
    /**
     * 승인 상태별 보호자 조회
     */
    List<Guardian> findByUserAndApprovalStatus(User user, Guardian.ApprovalStatus approvalStatus);
    
    /**
     * 주 보호자 조회
     */
    Optional<Guardian> findByUserAndIsPrimaryTrue(User user);
    
    /**
     * 알림 수신 가능한 보호자 조회
     */
    List<Guardian> findByUserAndCanReceiveAlertsTrue(User user);
    
    /**
     * 긴급 연락 우선순위별 조회
     */
    List<Guardian> findByUserOrderByEmergencyPriority(User user);
    
    /**
     * 설정 수정 권한이 있는 보호자 조회
     */
    List<Guardian> findByUserAndCanModifySettingsTrue(User user);
    
    /**
     * 위치 조회 권한이 있는 보호자 조회
     */
    List<Guardian> findByUserAndCanViewLocationTrue(User user);
    
    /**
     * 활성 보호자 관계 조회
     */
    List<Guardian> findByUserAndIsActiveTrue(User user);
    
    /**
     * 특정 사용자와 보호자 관계 조회
     * @param userId BIF 사용자 ID
     * @param guardianUserId 보호자 사용자 ID
     * @return 보호자 관계 리스트
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId AND g.guardianUser.userId = :guardianUserId")
    List<Guardian> findByUserIdAndGuardianUserId(@Param("userId") Long userId, @Param("guardianUserId") Long guardianUserId);
    
    /**
     * 보호자가 관리하는 활성 사용자 목록 조회
     */
    List<Guardian> findByGuardianUserAndIsActiveTrue(User guardianUser);
    
    /**
     * 사용자와 보호자 관계 존재 여부 확인
     */
    boolean existsByUserAndGuardianUser(User user, User guardianUser);
    
    /**
     * 사용자, 보호자, 활성 상태, 승인 상태로 존재 여부 확인
     */
    boolean existsByUserAndGuardianUserAndIsActiveTrueAndApprovalStatus(User user, User guardianUser, Guardian.ApprovalStatus approvalStatus);
    
    /**
     * 특정 사용자의 활성 보호자 목록 조회 (긴급 상황 알림용)
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId AND g.isActive = true " +
           "AND g.approvalStatus = 'APPROVED' AND g.canReceiveAlerts = true " +
           "ORDER BY g.emergencyPriority ASC, g.isPrimary DESC")
    List<Guardian> findActiveGuardiansByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자와 보호자 관계 존재 여부 확인
     */
    @Query("SELECT COUNT(g) > 0 FROM Guardian g WHERE g.user.userId = :userId " +
           "AND g.guardianUser.userId = :guardianUserId AND g.isActive = true " +
           "AND g.approvalStatus = 'APPROVED'")
    boolean existsByUserIdAndGuardianUserId(@Param("userId") Long userId, @Param("guardianUserId") Long guardianUserId);
    
    /**
     * WebSocket 위치 공유를 위한 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId " +
           "AND g.canViewLocation = :canViewLocation AND g.isActive = :isActive " +
           "AND g.approvalStatus = 'APPROVED'")
    List<Guardian> findByUserIdAndCanViewLocationAndIsActive(@Param("userId") Long userId, 
                                                            @Param("canViewLocation") Boolean canViewLocation, 
                                                            @Param("isActive") Boolean isActive);
    
    /**
     * WebSocket 긴급 알림을 위한 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId " +
           "AND g.canReceiveAlerts = :canReceiveAlerts AND g.isActive = :isActive " +
           "AND g.approvalStatus = 'APPROVED' ORDER BY g.emergencyPriority ASC")
    List<Guardian> findByUserIdAndCanReceiveAlertsAndIsActive(@Param("userId") Long userId, 
                                                             @Param("canReceiveAlerts") Boolean canReceiveAlerts, 
                                                             @Param("isActive") Boolean isActive);
    
    /**
     * WebSocket 활동 상태 업데이트를 위한 보호자 조회
     */
    @Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId " +
           "AND g.isActive = :isActive AND g.approvalStatus = 'APPROVED'")
    List<Guardian> findByUserIdAndIsActive(@Param("userId") Long userId, 
                                          @Param("isActive") Boolean isActive);
} 