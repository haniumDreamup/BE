package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 전송 이력 Repository
 * BIF 사용자에게 전송된 알림의 상태 추적을 위한 데이터 접근 계층
 */
@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    
    /**
     * 알림별 전송 이력 조회
     */
    List<NotificationDelivery> findByNotification_IdOrderByAttemptTimeDesc(Long notificationId);
    
    /**
     * 채널별 전송 이력 조회
     */
    List<NotificationDelivery> findByDeliveryChannelOrderByAttemptTimeDesc(String deliveryChannel);
    
    /**
     * 전송 상태별 조회
     */
    List<NotificationDelivery> findByNotification_IdAndDeliveryStatus(
            Long notificationId, NotificationDelivery.DeliveryStatus deliveryStatus);
    
    /**
     * 성공적으로 전송된 알림 조회
     */
    @Query("SELECT nd FROM NotificationDelivery nd " +
           "WHERE nd.notification.user.userId = :userId " +
           "AND nd.isSuccessful = true " +
           "AND nd.deliveryStatus = 'DELIVERED' " +
           "ORDER BY nd.attemptTime DESC")
    List<NotificationDelivery> findSuccessfulByUserId(@Param("userId") Long userId);
    
    /**
     * 전송 실패 알림 재전송 대상 조회
     */
    @Query("SELECT nd FROM NotificationDelivery nd " +
           "WHERE nd.deliveryStatus IN ('FAILED', 'PENDING_RETRY') " +
           "AND nd.attemptCount < :maxRetries " +
           "AND nd.attemptTime >= :since " +
           "ORDER BY nd.attemptTime ASC")
    List<NotificationDelivery> findFailedDeliveries(@Param("maxRetries") Integer maxRetries,
                                                   @Param("since") LocalDateTime since);
    
    /**
     * 사용자의 알림 수신 통계
     */
    @Query("SELECT nd.deliveryStatus, COUNT(nd) " +
           "FROM NotificationDelivery nd " +
           "WHERE nd.notification.user.userId = :userId " +
           "AND nd.attemptTime BETWEEN :start AND :end " +
           "GROUP BY nd.deliveryStatus")
    List<Object[]> findDeliveryStatsByUserId(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
    
    /**
     * 알림 전송 성공률 계산
     */
    @Query("SELECT COUNT(CASE WHEN nd.isSuccessful = true THEN 1 END) * 100.0 / COUNT(*) " +
           "FROM NotificationDelivery nd " +
           "WHERE nd.notification.user.userId = :userId " +
           "AND nd.attemptTime >= :since")
    Double calculateSuccessRate(@Param("userId") Long userId,
                           @Param("since") LocalDateTime since);
    
    /**
     * 채널별 최근 전송 성공 시간
     */
    @Query("SELECT nd.deliveryChannel, MAX(nd.completedTime) " +
           "FROM NotificationDelivery nd " +
           "WHERE nd.notification.user.userId = :userId " +
           "AND nd.deliveryStatus = 'DELIVERED' " +
           "GROUP BY nd.deliveryChannel")
    List<Object[]> findLastDeliveryTimeByChannel(@Param("userId") Long userId);
}