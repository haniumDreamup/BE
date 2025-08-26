package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 사용자 행동 로그 Repository
 */
@Repository
public interface UserBehaviorLogRepository extends JpaRepository<UserBehaviorLog, Long> {
  
  // 사용자별 로그 조회
  Page<UserBehaviorLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);
  
  // 세션별 로그 조회
  List<UserBehaviorLog> findBySessionIdOrderByTimestamp(String sessionId);
  
  // 액션 타입별 로그 조회
  Page<UserBehaviorLog> findByActionTypeOrderByTimestampDesc(ActionType actionType, Pageable pageable);
  
  // 기간별 로그 조회
  List<UserBehaviorLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
  
  // 사용자와 기간별 로그 조회
  List<UserBehaviorLog> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);
  
  // 액션 타입별 카운트
  @Query("SELECT l.actionType, COUNT(l) FROM UserBehaviorLog l " +
         "WHERE l.user = :user AND l.timestamp BETWEEN :start AND :end " +
         "GROUP BY l.actionType")
  List<Object[]> countActionsByTypeForUser(@Param("user") User user, 
                                           @Param("start") LocalDateTime start, 
                                           @Param("end") LocalDateTime end);
  
  // 시간대별 로그 카운트
  @Query(value = "SELECT HOUR(timestamp) as hour, COUNT(*) as count " +
                 "FROM user_behavior_logs " +
                 "WHERE user_id = :userId AND DATE(timestamp) = :date " +
                 "GROUP BY HOUR(timestamp) " +
                 "ORDER BY hour", nativeQuery = true)
  List<Map<String, Object>> countLogsByHour(@Param("userId") Long userId, 
                                            @Param("date") String date);
  
  // 페이지별 방문 횟수
  @Query("SELECT l.pageUrl, COUNT(l) FROM UserBehaviorLog l " +
         "WHERE l.actionType = 'PAGE_VIEW' AND l.timestamp BETWEEN :start AND :end " +
         "GROUP BY l.pageUrl " +
         "ORDER BY COUNT(l) DESC")
  List<Object[]> getMostVisitedPages(@Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end);
  
  // 평균 응답 시간
  @Query("SELECT AVG(l.responseTimeMs) FROM UserBehaviorLog l " +
         "WHERE l.user = :user AND l.responseTimeMs IS NOT NULL " +
         "AND l.timestamp BETWEEN :start AND :end")
  Double getAverageResponseTime(@Param("user") User user, 
                                @Param("start") LocalDateTime start, 
                                @Param("end") LocalDateTime end);
  
  // 오류 로그 조회
  @Query("SELECT l FROM UserBehaviorLog l " +
         "WHERE l.logLevel = 'ERROR' AND l.timestamp > :since " +
         "ORDER BY l.timestamp DESC")
  Page<UserBehaviorLog> findRecentErrors(@Param("since") LocalDateTime since, Pageable pageable);
  
  // 오래된 로그 삭제
  @Modifying
  @Transactional
  @Query("DELETE FROM UserBehaviorLog l WHERE l.timestamp < :before")
  int deleteLogsOlderThan(@Param("before") LocalDateTime before);
  
  // 특정 사용자의 마지막 활동 시간
  @Query("SELECT MAX(l.timestamp) FROM UserBehaviorLog l WHERE l.user = :user")
  LocalDateTime findLastActivityTime(@Param("user") User user);
  
  // 고유 세션 수 카운트
  @Query("SELECT COUNT(DISTINCT l.sessionId) FROM UserBehaviorLog l " +
         "WHERE l.user = :user AND l.timestamp BETWEEN :start AND :end")
  Long countUniqueSessionsForUser(@Param("user") User user, 
                                  @Param("start") LocalDateTime start, 
                                  @Param("end") LocalDateTime end);
  
  // 세션별 평균 지속 시간
  @Query(value = "SELECT AVG(session_duration) FROM (" +
                 "SELECT session_id, TIMESTAMPDIFF(SECOND, MIN(timestamp), MAX(timestamp)) as session_duration " +
                 "FROM user_behavior_logs " +
                 "WHERE user_id = :userId AND timestamp BETWEEN :start AND :end " +
                 "GROUP BY session_id" +
                 ") as session_stats", nativeQuery = true)
  Double getAverageSessionDuration(@Param("userId") Long userId, 
                                   @Param("start") LocalDateTime start, 
                                   @Param("end") LocalDateTime end);
  
  // 사용자와 세션 ID로 로그 조회
  List<UserBehaviorLog> findByUserAndSessionId(User user, String sessionId);
}