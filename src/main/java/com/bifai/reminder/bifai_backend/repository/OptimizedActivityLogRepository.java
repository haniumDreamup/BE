package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 최적화된 활동 로그 조회 리포지토리
 * 대시보드 성능 최적화
 */
@Repository
public interface OptimizedActivityLogRepository extends JpaRepository<ActivityLog, Long> {
  
  /**
   * 주간 활동 요약 (단일 쿼리로 집계)
   */
  @Query("SELECT DATE(a.activityDate) as activityDate, " +
         "COUNT(a) as activityCount, " +
         "a.activityType, " +
         "MIN(a.activityDate) as firstActivity, " +
         "MAX(a.activityDate) as lastActivity " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.activityDate BETWEEN :startTime AND :endTime " +
         "GROUP BY DATE(a.activityDate), a.activityType " +
         "ORDER BY DATE(a.activityDate) DESC")
  List<Object[]> getWeeklyActivitySummary(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);
  
  /**
   * 시간대별 활동 패턴 분석 (인덱스 활용)
   */
  @Query(value = "SELECT /*+ INDEX(activity_logs idx_activity_user_activity_date) */ " +
                 "HOUR(activity_date) as hour, " +
                 "COUNT(*) as count, " +
                 "activity_type " +
                 "FROM activity_logs " +
                 "WHERE user_id = :userId " +
                 "AND activity_date >= :startTime " +
                 "GROUP BY HOUR(activity_date), activity_type " +
                 "ORDER BY hour",
         nativeQuery = true)
  List<Object[]> getHourlyActivityPattern(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime);
  
  /**
   * 활동 강도 계산 (배치 처리)
   */
  @Query("SELECT CAST(a.activityDate AS date) as date, " +
         "COUNT(a) as count, " +
         "COALESCE(SUM(a.durationMinutes), 0L) as duration " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.activityDate BETWEEN :startTime AND :endTime " +
         "GROUP BY CAST(a.activityDate AS date) " +
         "ORDER BY CAST(a.activityDate AS date)")
  List<Object[]> getDailyActivityIntensity(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);
  
  /**
   * 최근 활동 조회 (페이징 최적화)
   */
  @Query(value = "SELECT * FROM activity_logs " +
                 "WHERE user_id = :userId " +
                 "ORDER BY activity_date DESC " +
                 "LIMIT :limit",
         nativeQuery = true)
  List<ActivityLog> findRecentActivities(
    @Param("userId") Long userId,
    @Param("limit") int limit);
  
  /**
   * 비활동 시간 감지
   */
  @Query("SELECT a1.activityDate, a2.activityDate " +
         "FROM ActivityLog a1, ActivityLog a2 " +
         "WHERE a1.user.userId = :userId " +
         "AND a2.user.userId = :userId " +
         "AND a2.activityDate = (" +
         "  SELECT MIN(a3.activityDate) " +
         "  FROM ActivityLog a3 " +
         "  WHERE a3.user.userId = :userId " +
         "  AND a3.activityDate > a1.activityDate" +
         ") " +
         "AND TIMESTAMPDIFF(HOUR, a1.activityDate, a2.activityDate) > :inactiveHours " +
         "ORDER BY a1.activityDate")
  List<Object[]> findInactivePeriods(
    @Param("userId") Long userId,
    @Param("inactiveHours") int inactiveHours);
  
  /**
   * 활동 유형별 통계 (캐시 활용)
   */
  @Query("SELECT a.activityType, COUNT(a), SUM(a.durationMinutes) " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.activityDate >= :startTime " +
         "GROUP BY a.activityType")
  List<Object[]> getActivityTypeStatistics(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime);
}