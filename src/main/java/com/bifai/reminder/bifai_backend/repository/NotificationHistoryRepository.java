package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.bifai.reminder.bifai_backend.entity.NotificationHistory;
import com.bifai.reminder.bifai_backend.entity.NotificationHistory.NotificationChannel;
import com.bifai.reminder.bifai_backend.entity.NotificationHistory.NotificationStatus;
import com.bifai.reminder.bifai_backend.entity.NotificationTemplate.EventType;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 발송 이력 레포지토리
 * 알림 추적, 통계 및 에스컬레이션 관리
 */
@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

  /**
   * 알림 ID로 조회
   */
  Optional<NotificationHistory> findByNotificationId(String notificationId);

  /**
   * 사용자별 알림 이력 조회
   */
  Page<NotificationHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  /**
   * 사용자와 상태로 조회
   */
  List<NotificationHistory> findByUserAndStatus(User user, NotificationStatus status);

  /**
   * 수신자별 알림 이력
   */
  Page<NotificationHistory> findByRecipientOrderByCreatedAtDesc(
      EmergencyContact recipient, 
      Pageable pageable
  );

  /**
   * 이벤트 타입별 알림 이력
   */
  List<NotificationHistory> findByEventTypeOrderByCreatedAtDesc(EventType eventType);

  /**
   * 특정 기간 동안의 알림 이력
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.user = :user " +
         "AND h.createdAt BETWEEN :startDate AND :endDate " +
         "ORDER BY h.createdAt DESC")
  List<NotificationHistory> findByUserAndDateRange(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 대기 중인 알림 조회
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.status = 'PENDING' " +
         "ORDER BY h.priority DESC, h.createdAt ASC")
  List<NotificationHistory> findPendingNotifications();

  /**
   * 재시도 가능한 알림 조회
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.status = 'FAILED' " +
         "AND h.retryCount < h.maxRetries " +
         "AND h.createdAt > :afterDate")
  List<NotificationHistory> findRetryableNotifications(@Param("afterDate") LocalDateTime afterDate);

  /**
   * 응답되지 않은 긴급 알림
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.severityLevel = 'CRITICAL' " +
         "AND h.status NOT IN ('RESPONDED', 'CANCELLED') " +
         "AND h.createdAt > :afterDate " +
         "ORDER BY h.createdAt ASC")
  List<NotificationHistory> findUnrespondedCriticalNotifications(
      @Param("afterDate") LocalDateTime afterDate
  );

  /**
   * 에스컬레이션이 필요한 알림
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.status IN ('SENT', 'DELIVERED') " +
         "AND h.respondedAt IS NULL " +
         "AND h.escalationLevel < 3 " +
         "AND TIMESTAMPDIFF(MINUTE, h.sentAt, CURRENT_TIMESTAMP) > :minutesThreshold")
  List<NotificationHistory> findNotificationsNeedingEscalation(
      @Param("minutesThreshold") Integer minutesThreshold
  );

  /**
   * 채널별 알림 통계
   */
  @Query("SELECT h.channel, COUNT(h), " +
         "SUM(CASE WHEN h.status IN ('DELIVERED', 'READ', 'RESPONDED') THEN 1 ELSE 0 END) " +
         "FROM NotificationHistory h " +
         "WHERE h.createdAt BETWEEN :startDate AND :endDate " +
         "GROUP BY h.channel")
  List<Object[]> getChannelStatistics(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 사용자별 알림 통계
   */
  @Query("SELECT COUNT(h), " +
         "SUM(CASE WHEN h.status = 'DELIVERED' THEN 1 ELSE 0 END), " +
         "SUM(CASE WHEN h.status = 'READ' THEN 1 ELSE 0 END), " +
         "SUM(CASE WHEN h.status = 'RESPONDED' THEN 1 ELSE 0 END), " +
         "AVG(h.responseTimeSeconds) " +
         "FROM NotificationHistory h WHERE h.user = :user")
  Object getUserNotificationStatistics(@Param("user") User user);

  /**
   * 평균 응답 시간 계산
   */
  @Query("SELECT AVG(h.responseTimeSeconds) FROM NotificationHistory h " +
         "WHERE h.user = :user AND h.responseTimeSeconds IS NOT NULL " +
         "AND h.createdAt BETWEEN :startDate AND :endDate")
  Double getAverageResponseTime(
      @Param("user") User user,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 실패한 알림 수
   */
  @Query("SELECT COUNT(h) FROM NotificationHistory h " +
         "WHERE h.status = 'FAILED' AND h.createdAt BETWEEN :startDate AND :endDate")
  Long countFailedNotifications(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 성공률 계산
   */
  @Query("SELECT " +
         "(COUNT(CASE WHEN h.status IN ('DELIVERED', 'READ', 'RESPONDED') THEN 1 END) * 100.0) / COUNT(*) " +
         "FROM NotificationHistory h " +
         "WHERE h.createdAt BETWEEN :startDate AND :endDate")
  Double getSuccessRate(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 알림 상태 업데이트
   */
  @Modifying
  @Query("UPDATE NotificationHistory h SET h.status = :status WHERE h.id = :id")
  void updateStatus(@Param("id") Long id, @Param("status") NotificationStatus status);

  /**
   * 전달 완료 처리
   */
  @Modifying
  @Query("UPDATE NotificationHistory h SET h.status = 'DELIVERED', h.deliveredAt = :deliveredAt " +
         "WHERE h.notificationId = :notificationId")
  void markAsDelivered(
      @Param("notificationId") String notificationId, 
      @Param("deliveredAt") LocalDateTime deliveredAt
  );

  /**
   * 읽음 처리
   */
  @Modifying
  @Query("UPDATE NotificationHistory h SET h.status = 'READ', h.readAt = :readAt " +
         "WHERE h.id = :id")
  void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

  /**
   * 응답 처리
   */
  @Modifying
  @Query("UPDATE NotificationHistory h SET h.status = 'RESPONDED', " +
         "h.respondedAt = :respondedAt, h.responseType = :responseType, " +
         "h.responseData = :responseData WHERE h.id = :id")
  void markAsResponded(
      @Param("id") Long id,
      @Param("respondedAt") LocalDateTime respondedAt,
      @Param("responseType") String responseType,
      @Param("responseData") String responseData
  );

  /**
   * 에스컬레이션 체인 조회
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.escalatedFromId = :originalId " +
         "ORDER BY h.escalationLevel")
  List<NotificationHistory> findEscalationChain(@Param("originalId") Long originalId);

  /**
   * 비용 통계
   */
  @Query("SELECT h.channel, SUM(h.cost) FROM NotificationHistory h " +
         "WHERE h.createdAt BETWEEN :startDate AND :endDate " +
         "AND h.cost IS NOT NULL " +
         "GROUP BY h.channel")
  List<Object[]> getCostByChannel(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 최근 알림 졸업 확인
   */
  boolean existsByUserAndChannelAndCreatedAtAfter(
      User user, 
      NotificationChannel channel, 
      LocalDateTime after
  );

  /**
   * 사용자의 읽지 않은 알림 수
   */
  @Query("SELECT COUNT(h) FROM NotificationHistory h WHERE h.user = :user " +
         "AND h.status IN ('SENT', 'DELIVERED')")
  Long countUnreadNotifications(@Param("user") User user);

  /**
   * 최근 긴급 알림 조회
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.user = :user " +
         "AND h.severityLevel IN ('CRITICAL', 'HIGH') " +
         "ORDER BY h.createdAt DESC")
  List<NotificationHistory> findRecentEmergencyNotifications(
      @Param("user") User user, 
      Pageable pageable
  );

  /**
   * 테스트 알림 제외 조회
   */
  @Query("SELECT h FROM NotificationHistory h WHERE h.isTest = false " +
         "AND h.user = :user ORDER BY h.createdAt DESC")
  Page<NotificationHistory> findRealNotifications(
      @Param("user") User user, 
      Pageable pageable
  );
}