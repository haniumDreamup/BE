package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection.RiskLevel;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection.WanderingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 배회 감지 레포지토리
 */
@Repository
public interface WanderingDetectionRepository extends JpaRepository<WanderingDetection, Long> {

  /**
   * 사용자의 활성 배회 감지 조회
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.user = :user " +
         "AND w.status NOT IN ('RESOLVED', 'FALSE_POSITIVE')")
  Optional<WanderingDetection> findActiveDetectionByUser(@Param("user") User user);

  /**
   * 모든 활성 배회 감지 조회
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.status NOT IN ('RESOLVED', 'FALSE_POSITIVE') " +
         "ORDER BY w.riskLevel DESC, w.detectedAt DESC")
  List<WanderingDetection> findActiveDetections();

  /**
   * 사용자별 배회 이력 조회
   */
  List<WanderingDetection> findByUserOrderByDetectedAtDesc(User user);

  /**
   * 위험 수준별 배회 감지 조회
   */
  List<WanderingDetection> findByRiskLevelAndStatus(RiskLevel riskLevel, WanderingStatus status);

  /**
   * 특정 기간 동안의 배회 감지
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.user = :user " +
         "AND w.detectedAt BETWEEN :startDate AND :endDate " +
         "ORDER BY w.detectedAt DESC")
  List<WanderingDetection> findByUserAndDateRange(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 개입이 필요한 배회 감지
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.status = 'INTERVENTION_NEEDED' " +
         "ORDER BY w.riskLevel DESC, w.detectedAt ASC")
  List<WanderingDetection> findDetectionsNeedingIntervention();

  /**
   * 보호자 알림이 필요한 감지
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.guardianNotified = false " +
         "AND w.riskLevel IN ('HIGH', 'CRITICAL') " +
         "AND w.status != 'RESOLVED'")
  List<WanderingDetection> findDetectionsNeedingGuardianNotification();

  /**
   * 반복 패턴 배회 조회
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.user = :user " +
         "AND w.recurringPattern = true " +
         "ORDER BY w.detectedAt DESC")
  List<WanderingDetection> findRecurringPatterns(@Param("user") User user);

  /**
   * 장시간 배회 감지
   */
  @Query("SELECT w FROM WanderingDetection w WHERE w.durationMinutes > :threshold " +
         "AND w.status NOT IN ('RESOLVED', 'FALSE_POSITIVE')")
  List<WanderingDetection> findLongDurationWanderings(@Param("threshold") Integer threshold);

  /**
   * 배회 통계
   */
  @Query("SELECT COUNT(w), AVG(w.durationMinutes), MAX(w.durationMinutes) " +
         "FROM WanderingDetection w WHERE w.user = :user " +
         "AND w.detectedAt > :since")
  Object getWanderingStatistics(
      @Param("user") User user,
      @Param("since") LocalDateTime since
  );
}