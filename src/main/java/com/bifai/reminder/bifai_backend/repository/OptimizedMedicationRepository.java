package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.entity.MedicationAdherence;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 최적화된 복약 정보 조회 리포지토리
 * N+1 문제 해결 및 쿼리 최적화
 */
@Repository
public interface OptimizedMedicationRepository extends JpaRepository<Medication, Long> {
  
  /**
   * 대시보드용 사용자 복약 정보 조회 (한 번의 쿼리로 모든 연관 데이터 로드)
   */
  @Query("SELECT DISTINCT m FROM Medication m " +
         "LEFT JOIN FETCH m.user u " +
         "LEFT JOIN FETCH m.intakeTimes " +
         "WHERE m.user.userId = :userId " +
         "AND m.isActive = true " +
         "AND m.scheduledTime BETWEEN :startTime AND :endTime " +
         "ORDER BY m.scheduledTime, m.priorityLevel DESC")
  List<Medication> findDashboardMedications(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);
  
  /**
   * 복약 순응도와 함께 조회 (JOIN FETCH로 N+1 해결)
   */
  @Query("SELECT DISTINCT m FROM Medication m " +
         "LEFT JOIN FETCH m.user u " +
         "LEFT JOIN FETCH m.adherenceRecords a " +
         "WHERE m.user.userId = :userId " +
         "AND a.scheduledDate BETWEEN :startDate AND :endDate " +
         "ORDER BY a.scheduledDate DESC, m.priorityLevel DESC")
  List<Medication> findMedicationsWithAdherence(
    @Param("userId") Long userId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
  
  /**
   * 오늘의 복약 일정 (배치 페칭 활용)
   */
  @EntityGraph(attributePaths = {"user", "intakeTimes"})
  @Query("SELECT m FROM Medication m " +
         "WHERE m.user.userId = :userId " +
         "AND m.isActive = true " +
         "AND m.medicationStatus = 'ACTIVE' " +
         "AND (m.endDate IS NULL OR m.endDate >= :today) " +
         "AND m.startDate <= :today " +
         "ORDER BY m.priorityLevel DESC")
  List<Medication> findTodayMedicationsOptimized(
    @Param("userId") Long userId, 
    @Param("today") LocalDate today);
  
  /**
   * 주간 복약 요약 데이터 (단일 쿼리로 집계)
   */
  @Query("SELECT NEW com.bifai.reminder.bifai_backend.dto.dashboard.MedicationWeeklyStats(" +
         "m.medicationId, m.medicationName, m.priorityLevel, " +
         "COUNT(a), " +
         "SUM(CASE WHEN a.taken = true THEN 1 ELSE 0 END), " +
         "AVG(a.delayMinutes)) " +
         "FROM Medication m " +
         "LEFT JOIN MedicationAdherence a ON a.medicationId = m.medicationId " +
         "WHERE m.user.userId = :userId " +
         "AND a.scheduledDate BETWEEN :startDate AND :endDate " +
         "GROUP BY m.medicationId, m.medicationName, m.priorityLevel " +
         "ORDER BY m.priorityLevel DESC")
  List<Object> getMedicationWeeklyStats(
    @Param("userId") Long userId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
  
  /**
   * 시간대별 복약 일정 (인덱스 활용)
   */
  @Query(value = "SELECT /*+ INDEX(medications idx_medications_user_time) */ " +
                 "m.* FROM medications m " +
                 "WHERE m.user_id = :userId " +
                 "AND m.scheduled_time BETWEEN :startTime AND :endTime " +
                 "AND m.is_active = true " +
                 "ORDER BY m.scheduled_time",
         nativeQuery = true)
  List<Medication> findByTimeRangeOptimized(
    @Param("userId") Long userId,
    @Param("startTime") LocalTime startTime,
    @Param("endTime") LocalTime endTime);
  
  /**
   * 복약 알림이 필요한 약물들 (배치 처리용)
   */
  @Query("SELECT m FROM Medication m " +
         "WHERE m.isActive = true " +
         "AND m.medicationStatus = 'ACTIVE' " +
         "AND EXISTS (" +
         "  SELECT 1 FROM m.intakeTimes it " +
         "  WHERE it = :notificationTime" +
         ") " +
         "ORDER BY m.user.userId, m.priorityLevel DESC")
  List<Medication> findMedicationsForNotification(@Param("notificationTime") LocalTime notificationTime);
  
  /**
   * 보호자 알림 대상 약물들 (JOIN 최소화)
   */
  @Query("SELECT m.medicationId, m.medicationName, m.user.userId, m.user.username " +
         "FROM Medication m " +
         "WHERE m.guardianAlertNeeded = true " +
         "AND m.isActive = true " +
         "AND m.priorityLevel IN ('CRITICAL', 'HIGH') " +
         "AND EXISTS (" +
         "  SELECT 1 FROM MedicationAdherence a " +
         "  WHERE a.medicationId = m.medicationId " +
         "  AND a.taken = false " +
         "  AND a.scheduledDate = :date" +
         ")")
  List<Object[]> findMedicationsNeedingGuardianAlert(@Param("date") LocalDate date);
}