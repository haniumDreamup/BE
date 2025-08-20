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
  @Query("SELECT DATE(a.timestamp) as activityDate, " +
         "COUNT(a) as activityCount, " +
         "a.activityType, " +
         "MIN(a.timestamp) as firstActivity, " +
         "MAX(a.timestamp) as lastActivity " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.timestamp BETWEEN :startTime AND :endTime " +
         "GROUP BY DATE(a.timestamp), a.activityType " +
         "ORDER BY DATE(a.timestamp) DESC")
  List<Object[]> getWeeklyActivitySummary(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);
  
  /**
   * 시간대별 활동 패턴 분석 (인덱스 활용)
   */
  @Query(value = "SELECT /*+ INDEX(activity_logs idx_activity_user_timestamp) */ " +
                 "HOUR(timestamp) as hour, " +
                 "COUNT(*) as count, " +
                 "activity_type " +
                 "FROM activity_logs " +
                 "WHERE user_id = :userId " +
                 "AND timestamp >= :startTime " +
                 "GROUP BY HOUR(timestamp), activity_type " +
                 "ORDER BY hour",
         nativeQuery = true)
  List<Object[]> getHourlyActivityPattern(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime);
  
  /**
   * 활동 강도 계산 (배치 처리)
   */
  @Query("SELECT NEW com.bifai.reminder.bifai_backend.dto.dashboard.ActivityIntensity(" +
         "DATE(a.timestamp), " +
         "COUNT(a), " +
         "SUM(a.durationMinutes), " +
         "AVG(a.intensityScore)) " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.timestamp BETWEEN :startTime AND :endTime " +
         "GROUP BY DATE(a.timestamp) " +
         "ORDER BY DATE(a.timestamp)")
  List<Object> getDailyActivityIntensity(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime,
    @Param("endTime") LocalDateTime endTime);
  
  /**
   * 최근 활동 조회 (페이징 최적화)
   */
  @Query(value = "SELECT * FROM activity_logs " +
                 "WHERE user_id = :userId " +
                 "ORDER BY timestamp DESC " +
                 "LIMIT :limit",
         nativeQuery = true)
  List<ActivityLog> findRecentActivities(
    @Param("userId") Long userId,
    @Param("limit") int limit);
  
  /**
   * 비활동 시간 감지
   */
  @Query("SELECT a1.timestamp, a2.timestamp " +
         "FROM ActivityLog a1, ActivityLog a2 " +
         "WHERE a1.user.userId = :userId " +
         "AND a2.user.userId = :userId " +
         "AND a2.timestamp = (" +
         "  SELECT MIN(a3.timestamp) " +
         "  FROM ActivityLog a3 " +
         "  WHERE a3.user.userId = :userId " +
         "  AND a3.timestamp > a1.timestamp" +
         ") " +
         "AND TIMESTAMPDIFF(HOUR, a1.timestamp, a2.timestamp) > :inactiveHours " +
         "ORDER BY a1.timestamp")
  List<Object[]> findInactivePeriods(
    @Param("userId") Long userId,
    @Param("inactiveHours") int inactiveHours);
  
  /**
   * 활동 유형별 통계 (캐시 활용)
   */
  @Query("SELECT a.activityType, COUNT(a), SUM(a.durationMinutes) " +
         "FROM ActivityLog a " +
         "WHERE a.user.userId = :userId " +
         "AND a.timestamp >= :startTime " +
         "GROUP BY a.activityType")
  List<Object[]> getActivityTypeStatistics(
    @Param("userId") Long userId,
    @Param("startTime") LocalDateTime startTime);
}