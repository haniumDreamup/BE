package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Notification;
import com.bifai.reminder.bifai_backend.entity.Schedule;
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

/**
 * BIF 사용자의 알림 데이터 접근을 위한 Repository
 * 알림 상태, 채널, 우선순위 기반 조회 등 BIF 특화 기능 포함
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 읽지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.sendTime DESC")
    List<Notification> findUnreadNotificationsByUser(@Param("user") User user);

    /**
     * 사용자의 확인되지 않은 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isAcknowledged = false ORDER BY n.sendTime DESC")
    List<Notification> findUnacknowledgedNotificationsByUser(@Param("user") User user);

    /**
     * 사용자의 특정 상태 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.status = :status ORDER BY n.sendTime DESC")
    Page<Notification> findByUserAndStatus(
            @Param("user") User user,
            @Param("status") Notification.NotificationStatus status,
            Pageable pageable);

    /**
     * 전송 예정인 알림 조회 (배치 처리용)
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.sendTime <= :currentTime ORDER BY n.priority DESC, n.sendTime ASC")
    List<Notification> findNotificationsPendingSend(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 높은 우선순위 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.priority >= 3 ORDER BY n.priority DESC, n.sendTime DESC")
    List<Notification> findHighPriorityNotificationsByUser(@Param("user") User user);

    /**
     * 응급 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND (n.priority = 4 OR n.notificationType = 'EMERGENCY_ALERT') ORDER BY n.sendTime DESC")
    List<Notification> findEmergencyNotificationsByUser(@Param("user") User user);

    /**
     * 특정 타입의 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationType = :notificationType ORDER BY n.sendTime DESC")
    List<Notification> findByUserAndNotificationType(
            @Param("user") User user,
            @Param("notificationType") Notification.NotificationType notificationType);

    /**
     * 스케줄 관련 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.schedule = :schedule ORDER BY n.sendTime DESC")
    List<Notification> findBySchedule(@Param("schedule") Schedule schedule);

    /**
     * 재시도가 필요한 실패한 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries AND (n.expiresAt IS NULL OR n.expiresAt > :currentTime)")
    List<Notification> findNotificationsNeedingRetry(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 만료된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt <= :currentTime AND n.status NOT IN ('EXPIRED', 'ACKNOWLEDGED')")
    List<Notification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 오늘의 알림 개수 조회
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.sendTime >= :startOfDay AND n.sendTime < :endOfDay")
    long countTodayNotificationsByUser(
            @Param("user") User user,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 최근 N일간 읽지 않은 알림 개수
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false AND n.sendTime >= :sinceDate")
    long countUnreadNotificationsSince(@Param("user") User user, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * 알림 타입별 통계
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.user = :user AND n.sendTime >= :fromDate GROUP BY n.notificationType")
    List<Object[]> getNotificationTypeStatsByUser(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);

    /**
     * 알림 상태별 통계
     */
    @Query("SELECT n.status, COUNT(n) FROM Notification n WHERE n.user = :user AND n.sendTime >= :fromDate GROUP BY n.status")
    List<Object[]> getNotificationStatusStatsByUser(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);

    /**
     * 보호자에게도 전송할 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notifyGuardian = true AND n.status = 'PENDING'")
    List<Notification> findNotificationsForGuardian(@Param("user") User user);

    /**
     * 음성 안내가 활성화된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.voiceEnabled = true AND n.status = 'PENDING'")
    List<Notification> findVoiceEnabledNotificationsByUser(@Param("user") User user);

    /**
     * 특정 시각적 표시를 가진 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.visualIndicator = :indicator ORDER BY n.sendTime DESC")
    List<Notification> findByUserAndVisualIndicator(@Param("user") User user, @Param("indicator") String indicator);

    /**
     * 최근 성공적으로 전송된 알림 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.status IN ('SENT', 'DELIVERED', 'READ', 'ACKNOWLEDGED') ORDER BY n.sentAt DESC")
    List<Notification> findRecentSuccessfulNotificationsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 실패한 알림 조회 (분석용)
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.status = 'FAILED' ORDER BY n.sendTime DESC")
    List<Notification> findFailedNotificationsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readTime WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("readTime") LocalDateTime readTime);

    /**
     * 알림 확인 처리
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isAcknowledged = true, n.acknowledgedAt = :acknowledgedTime, n.userResponse = :response WHERE n.id = :notificationId")
    void markAsAcknowledged(
            @Param("notificationId") Long notificationId,
            @Param("acknowledgedTime") LocalDateTime acknowledgedTime,
            @Param("response") String response);

    /**
     * 만료된 알림 상태 업데이트
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'EXPIRED' WHERE n.id IN :notificationIds")
    void markAsExpired(@Param("notificationIds") List<Long> notificationIds);

    /**
     * 월별 알림 전송 통계
     */
    @Query("SELECT YEAR(n.sendTime), MONTH(n.sendTime), COUNT(n) FROM Notification n " +
           "WHERE n.user = :user AND n.sendTime IS NOT NULL " +
           "GROUP BY YEAR(n.sendTime), MONTH(n.sendTime) " +
           "ORDER BY YEAR(n.sendTime) DESC, MONTH(n.sendTime) DESC")
    List<Object[]> getMonthlyNotificationStatsByUser(@Param("user") User user);

    /**
     * 알림 응답률 통계
     */
    @Query("SELECT n.notificationType, " +
           "COUNT(CASE WHEN n.isAcknowledged = true THEN 1 END) * 100.0 / COUNT(n) as responseRate " +
           "FROM Notification n WHERE n.user = :user " +
           "GROUP BY n.notificationType")
    List<Object[]> getNotificationResponseRatesByUser(@Param("user") User user);

    /**
     * 알림 전송 성공률 통계
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN n.status IN ('SENT', 'DELIVERED', 'READ', 'ACKNOWLEDGED') THEN 1 END) * 100.0 / COUNT(n) as successRate " +
           "FROM Notification n WHERE n.user = :user")
    Double getNotificationSuccessRateByUser(@Param("user") User user);

    /**
     * 시간대별 알림 효과 분석
     */
    @Query("SELECT HOUR(n.sendTime) as hour, " +
           "COUNT(CASE WHEN n.isAcknowledged = true THEN 1 END) * 100.0 / COUNT(n) as acknowledgeRate " +
           "FROM Notification n WHERE n.user = :user AND n.sendTime >= :fromDate " +
           "GROUP BY HOUR(n.sendTime) " +
           "ORDER BY hour")
    List<Object[]> getHourlyAcknowledgeRatesByUser(@Param("user") User user, @Param("fromDate") LocalDateTime fromDate);

    /**
     * 사용자별 평균 알림 응답 시간
     * TODO: H2 호환성 문제로 인해 임시 비활성화 - 서비스 레이어에서 직접 계산 필요
     */
    default Double getAverageResponseTimeByUser(User user) {
        // 임시로 null 반환 - 실제 구현은 서비스 레이어에서 처리
        return null;
    }

    /**
     * 긴급하지 않은 대기 중인 알림을 나중으로 연기
     */
    @Modifying
    @Query("UPDATE Notification n SET n.sendTime = :newSendTime WHERE n.user = :user AND n.status = 'PENDING' AND n.priority < 3 AND n.sendTime <= :currentTime")
    int postponeNonUrgentNotifications(
            @Param("user") User user,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("newSendTime") LocalDateTime newSendTime);
} 