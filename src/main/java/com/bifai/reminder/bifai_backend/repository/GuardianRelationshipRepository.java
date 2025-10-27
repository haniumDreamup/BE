package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.GuardianRelationship;
import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.RelationshipStatus;
import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 보호자 관계 레포지토리
 */
@Repository
public interface GuardianRelationshipRepository extends JpaRepository<GuardianRelationship, Long> {
  
  /**
   * 특정 보호자와 사용자 간의 관계 조회 (단일)
   */
  Optional<GuardianRelationship> findByGuardian_IdAndUser_UserId(Long guardianId, Long userId);

  /**
   * 특정 보호자와 사용자 간의 모든 관계 조회 (중복 체크용)
   */
  List<GuardianRelationship> findAllByGuardian_IdAndUser_UserId(Long guardianId, Long userId);
  
  /**
   * 특정 사용자의 모든 보호자 관계 조회
   */
  List<GuardianRelationship> findByUserUserIdOrderByEmergencyPriority(Long userId);
  
  /**
   * 특정 사용자의 활성 보호자 관계 조회
   */
  List<GuardianRelationship> findByUserUserIdAndStatus(Long userId, RelationshipStatus status);
  
  /**
   * 특정 보호자의 모든 피보호자 관계 조회
   */
  List<GuardianRelationship> findByGuardian_IdAndStatus(Long guardianId, RelationshipStatus status);
  
  /**
   * 초대 토큰으로 관계 조회
   */
  Optional<GuardianRelationship> findByInvitationToken(String invitationToken);
  
  /**
   * 만료된 초대 조회
   */
  @Query("SELECT gr FROM GuardianRelationship gr " +
         "WHERE gr.status = 'PENDING' " +
         "AND gr.invitationExpiresAt < :now")
  List<GuardianRelationship> findExpiredInvitations(@Param("now") LocalDateTime now);
  
  /**
   * 사용자의 긴급 연락 보호자 조회 (우선순위 순)
   */
  @Query("SELECT gr FROM GuardianRelationship gr " +
         "WHERE gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE' " +
         "AND gr.permissionLevel IN ('EMERGENCY', 'MANAGE', 'FULL') " +
         "ORDER BY gr.emergencyPriority ASC NULLS LAST")
  List<GuardianRelationship> findEmergencyGuardians(@Param("userId") Long userId);
  
  /**
   * 특정 권한을 가진 보호자 조회
   */
  @Query("SELECT gr FROM GuardianRelationship gr " +
         "WHERE gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE' " +
         "AND gr.permissionLevel = :permissionLevel")
  List<GuardianRelationship> findByUserAndPermissionLevel(
    @Param("userId") Long userId, 
    @Param("permissionLevel") PermissionLevel permissionLevel
  );
  
  /**
   * 보호자의 피보호자 수 카운트
   */
  @Query("SELECT COUNT(gr) FROM GuardianRelationship gr " +
         "WHERE gr.guardian.id = :guardianId " +
         "AND gr.status = 'ACTIVE'")
  Long countActiveRelationshipsByGuardian(@Param("guardianId") Long guardianId);
  
  /**
   * 사용자의 활성 보호자 수 카운트
   */
  @Query("SELECT COUNT(gr) FROM GuardianRelationship gr " +
         "WHERE gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE'")
  Long countActiveGuardiansByUser(@Param("userId") Long userId);
  
  /**
   * 최근 활동한 보호자 관계 조회
   */
  @Query("SELECT gr FROM GuardianRelationship gr " +
         "WHERE gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE' " +
         "AND gr.lastActiveAt > :since " +
         "ORDER BY gr.lastActiveAt DESC")
  List<GuardianRelationship> findRecentlyActiveGuardians(
    @Param("userId") Long userId, 
    @Param("since") LocalDateTime since
  );
  
  /**
   * 관계 유형별 통계
   */
  @Query("SELECT gr.relationshipType, COUNT(gr) FROM GuardianRelationship gr " +
         "WHERE gr.status = 'ACTIVE' " +
         "GROUP BY gr.relationshipType")
  List<Object[]> getRelationshipTypeStatistics();
  
  /**
   * 중복 관계 확인
   */
  boolean existsByGuardian_IdAndUser_UserIdAndStatusNot(
    Long guardianId, 
    Long userId, 
    RelationshipStatus status
  );
  
  /**
   * 보호자의 관리 권한 확인
   */
  @Query("SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END " +
         "FROM GuardianRelationship gr " +
         "WHERE gr.guardian.id = :guardianId " +
         "AND gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE' " +
         "AND gr.permissionLevel IN ('MANAGE', 'FULL')")
  boolean hasManagePermission(@Param("guardianId") Long guardianId, @Param("userId") Long userId);
  
  /**
   * 보호자의 조회 권한 확인
   */
  @Query("SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END " +
         "FROM GuardianRelationship gr " +
         "WHERE gr.guardian.id = :guardianId " +
         "AND gr.user.userId = :userId " +
         "AND gr.status = 'ACTIVE'")
  boolean hasViewPermission(@Param("guardianId") Long guardianId, @Param("userId") Long userId);
}