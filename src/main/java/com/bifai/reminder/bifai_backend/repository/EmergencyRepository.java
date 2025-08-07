package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencyStatus;
import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 긴급 상황 리포지토리
 * 긴급 상황 데이터 접근을 위한 인터페이스
 */
@Repository
public interface EmergencyRepository extends JpaRepository<Emergency, Long> {

  /**
   * 사용자의 활성 긴급 상황 조회
   */
  List<Emergency> findByUserIdAndStatus(Long userId, EmergencyStatus status);

  /**
   * 사용자의 모든 긴급 상황 조회 (최신순)
   */
  Page<Emergency> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * 특정 기간 동안의 긴급 상황 조회
   */
  List<Emergency> findByUserIdAndCreatedAtBetween(
      Long userId, 
      LocalDateTime startDate, 
      LocalDateTime endDate
  );

  /**
   * 활성 상태의 모든 긴급 상황 조회
   */
  @Query("SELECT e FROM Emergency e WHERE e.status IN :statuses ORDER BY e.createdAt DESC")
  List<Emergency> findActiveEmergencies(@Param("statuses") List<EmergencyStatus> statuses);

  /**
   * 사용자의 최근 긴급 상황 조회
   */
  Optional<Emergency> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

  /**
   * 특정 유형의 긴급 상황 개수 조회
   */
  Long countByUserIdAndTypeAndCreatedAtAfter(
      Long userId, 
      EmergencyType type, 
      LocalDateTime after
  );

  /**
   * 보호자에게 알림이 전송된 긴급 상황 조회
   */
  @Query("SELECT e FROM Emergency e WHERE e.notifiedGuardians LIKE %:guardianEmail%")
  List<Emergency> findByNotifiedGuardian(@Param("guardianEmail") String guardianEmail);

  /**
   * 해결되지 않은 긴급 상황 조회
   */
  @Query("SELECT e FROM Emergency e WHERE e.status != 'RESOLVED' AND e.status != 'FALSE_ALARM' " +
         "AND e.createdAt < :timeThreshold")
  List<Emergency> findUnresolvedEmergenciesOlderThan(@Param("timeThreshold") LocalDateTime timeThreshold);

  /**
   * 사용자별 긴급 상황 통계
   */
  @Query("SELECT e.type, COUNT(e) FROM Emergency e " +
         "WHERE e.user.id = :userId AND e.createdAt >= :startDate " +
         "GROUP BY e.type")
  List<Object[]> getEmergencyStatsByUser(
      @Param("userId") Long userId, 
      @Param("startDate") LocalDateTime startDate
  );

  /**
   * 위치 기반 긴급 상황 조회
   */
  @Query("SELECT e FROM Emergency e WHERE " +
         "(6371 * acos(cos(radians(:latitude)) * cos(radians(e.latitude)) * " +
         "cos(radians(e.longitude) - radians(:longitude)) + " +
         "sin(radians(:latitude)) * sin(radians(e.latitude)))) < :radiusKm " +
         "AND e.status = :status")
  List<Emergency> findNearbyEmergencies(
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusKm") Double radiusKm,
      @Param("status") EmergencyStatus status
  );
}