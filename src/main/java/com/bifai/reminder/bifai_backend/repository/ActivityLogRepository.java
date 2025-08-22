package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ActivityLog;
import com.bifai.reminder.bifai_backend.entity.User;
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
 * ActivityLogRepository - BIF User Activity Tracking Repository
 * 
 * BIF 사용자의 활동 로그를 관리하는 리포지토리입니다.
 * 활동 추적, 패턴 분석, 성과 측정 등의 기능을 제공합니다.
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    // 기본 조회 메서드
    List<ActivityLog> findByUserOrderByActivityDateDesc(User user);
    
    List<ActivityLog> findByUserOrderByCreatedAtDesc(User user);
    
    Page<ActivityLog> findByUserOrderByActivityDateDesc(User user, Pageable pageable);
    
    List<ActivityLog> findByUser_UserIdOrderByActivityDateDesc(Long userId);
    
    // 활동 유형별 조회
    List<ActivityLog> findByUserAndActivityTypeOrderByActivityDateDesc(
        User user, ActivityLog.ActivityType activityType);
    
    Page<ActivityLog> findByUserAndActivityTypeOrderByActivityDateDesc(
        User user, ActivityLog.ActivityType activityType, Pageable pageable);
    
    // 날짜 범위별 조회
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "AND al.activityDate >= :startDate AND al.activityDate <= :endDate " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findByUserAndActivityDateBetween(
        @Param("user") User user,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT al FROM ActivityLog al WHERE al.user.userId = :userId " +
           "AND al.activityDate >= :startDate AND al.activityDate <= :endDate " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findByUser_UserIdAndActivityDateBetween(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // 성공 상태별 조회
    List<ActivityLog> findByUserAndSuccessStatusOrderByActivityDateDesc(
        User user, ActivityLog.SuccessStatus successStatus);
    
    Page<ActivityLog> findByUserAndSuccessStatusOrderByActivityDateDesc(
        User user, ActivityLog.SuccessStatus successStatus, Pageable pageable);
    
    // 오늘의 활동 조회
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "AND CAST(al.activityDate AS DATE) = CURRENT_DATE " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findTodayActivitiesByUser(@Param("user") User user);
    
    // 최근 활동 조회
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "ORDER BY al.activityDate DESC LIMIT :limit")
    List<ActivityLog> findRecentActivitiesByUser(@Param("user") User user, @Param("limit") int limit);
    
    // 보호자 알림이 필요한 활동들
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "AND (al.successStatus = 'FAILED' OR al.helpNeeded = true OR " +
           "al.activityType IN ('EMERGENCY', 'HELP_REQUEST')) " +
           "AND al.guardianNotified = false " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findActivitiesNeedingGuardianAttention(@Param("user") User user);
    
    // 활동 통계 쿼리들
    @Query("SELECT al.activityType, COUNT(al) FROM ActivityLog al " +
           "WHERE al.user = :user AND al.activityDate >= :startDate " +
           "GROUP BY al.activityType ORDER BY COUNT(al) DESC")
    List<Object[]> getActivityCountByType(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT al.successStatus, COUNT(al) FROM ActivityLog al " +
           "WHERE al.user = :user AND al.activityDate >= :startDate " +
           "GROUP BY al.successStatus")
    List<Object[]> getSuccessRateStatistics(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 성공률 계산
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE al.user = :user " +
           "AND al.activityType = :activityType " +
           "AND al.successStatus IN ('SUCCESS', 'PARTIAL_SUCCESS') " +
           "AND al.activityDate >= :startDate")
    Long countSuccessfulActivitiesByType(
        @Param("user") User user,
        @Param("activityType") ActivityLog.ActivityType activityType,
        @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE al.user = :user " +
           "AND al.activityType = :activityType " +
           "AND al.activityDate >= :startDate")
    Long countTotalActivitiesByType(
        @Param("user") User user,
        @Param("activityType") ActivityLog.ActivityType activityType,
        @Param("startDate") LocalDateTime startDate);
    
    // 난이도별 성과 분석
    @Query("SELECT al.difficultyLevel, AVG(al.confidenceScore) FROM ActivityLog al " +
           "WHERE al.user = :user AND al.confidenceScore IS NOT NULL " +
           "AND al.activityDate >= :startDate " +
           "GROUP BY al.difficultyLevel")
    List<Object[]> getConfidenceByDifficultyLevel(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 월별 활동 요약
    @Query("SELECT YEAR(al.activityDate), MONTH(al.activityDate), " +
           "COUNT(al), " +
           "SUM(CASE WHEN al.successStatus IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN 1 ELSE 0 END) " +
           "FROM ActivityLog al WHERE al.user = :user " +
           "GROUP BY YEAR(al.activityDate), MONTH(al.activityDate) " +
           "ORDER BY YEAR(al.activityDate), MONTH(al.activityDate)")
    List<Object[]> getMonthlyActivitySummary(@Param("user") User user);
    
    // 기분 변화 추적
    @Query("SELECT al.activityDate, al.moodBefore, al.moodAfter FROM ActivityLog al " +
           "WHERE al.user = :user AND al.moodBefore IS NOT NULL AND al.moodAfter IS NOT NULL " +
           "AND al.activityDate >= :startDate " +
           "ORDER BY al.activityDate")
    List<Object[]> getMoodProgressData(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 활동 지속 시간 통계
    @Query("SELECT al.activityType, AVG(al.durationMinutes), MAX(al.durationMinutes), MIN(al.durationMinutes) " +
           "FROM ActivityLog al WHERE al.user = :user AND al.durationMinutes IS NOT NULL " +
           "AND al.activityDate >= :startDate " +
           "GROUP BY al.activityType")
    List<Object[]> getDurationStatisticsByActivityType(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 장소별 활동 분석
    @Query("SELECT al.locationDescription, COUNT(al), " +
           "SUM(CASE WHEN al.successStatus IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN 1 ELSE 0 END) " +
           "FROM ActivityLog al WHERE al.user = :user " +
           "AND al.locationDescription IS NOT NULL " +
           "AND al.activityDate >= :startDate " +
           "GROUP BY al.locationDescription")
    List<Object[]> getActivityStatsByLocation(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 동기화 상태 관련
    List<ActivityLog> findByUserAndSyncStatus(User user, String syncStatus);
    
    @Query("SELECT COUNT(al) FROM ActivityLog al WHERE al.user = :user AND al.syncStatus = 'PENDING'")
    Long countPendingSyncByUser(@Param("user") User user);
    
    // 시간대별 활동 패턴
    @Query("SELECT HOUR(al.activityDate), COUNT(al), " +
           "SUM(CASE WHEN al.successStatus IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN 1 ELSE 0 END) " +
           "FROM ActivityLog al WHERE al.user = :user " +
           "AND al.activityDate >= :startDate " +
           "GROUP BY HOUR(al.activityDate) " +
           "ORDER BY HOUR(al.activityDate)")
    List<Object[]> getActivityPatternByHour(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 최근 실패한 활동들 (개선 포인트 찾기)
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "AND al.successStatus = 'FAILED' " +
           "AND al.activityDate >= :recentDate " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findRecentFailedActivities(
        @Param("user") User user, @Param("recentDate") LocalDateTime recentDate);
    
    // 특정 활동의 최근 기록
    Optional<ActivityLog> findTopByUserAndActivityTypeOrderByActivityDateDesc(
        User user, ActivityLog.ActivityType activityType);
    
    // 도움이 필요했던 활동들
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user " +
           "AND al.helpNeeded = true " +
           "AND al.activityDate >= :startDate " +
           "ORDER BY al.activityDate DESC")
    List<ActivityLog> findActivitiesRequiringHelp(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
} 