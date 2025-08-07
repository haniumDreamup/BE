package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.GeofenceEvent;
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
 * 지오펜스 이벤트 레포지토리
 * 안전 구역 진입/이탈 이벤트 데이터 접근
 */
@Repository
public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, Long> {

  /**
   * 사용자별 이벤트 조회
   */
  Page<GeofenceEvent> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  /**
   * 지오펜스별 이벤트 조회
   */
  List<GeofenceEvent> findByGeofenceOrderByCreatedAtDesc(Geofence geofence);

  /**
   * 사용자와 이벤트 타입별 조회
   */
  List<GeofenceEvent> findByUserAndEventType(User user, GeofenceEvent.EventType eventType);

  /**
   * 기간별 이벤트 조회
   */
  List<GeofenceEvent> findByUserAndCreatedAtBetween(
      User user,
      LocalDateTime startTime,
      LocalDateTime endTime
  );

  /**
   * 미확인 알림 조회
   */
  List<GeofenceEvent> findByUserAndAcknowledgedFalse(User user);

  /**
   * 고위험 이벤트 조회
   */
  @Query("SELECT e FROM GeofenceEvent e WHERE e.user = :user " +
         "AND e.riskLevel IN ('HIGH', 'CRITICAL') " +
         "AND e.acknowledged = false ORDER BY e.createdAt DESC")
  List<GeofenceEvent> findUnacknowledgedHighRiskEvents(@Param("user") User user);

  /**
   * 최근 이벤트 조회
   */
  Optional<GeofenceEvent> findTopByUserAndGeofenceOrderByCreatedAtDesc(
      User user,
      Geofence geofence
  );

  /**
   * 특정 위치 근처의 이벤트 조회
   */
  @Query(value = "SELECT * FROM geofence_events WHERE user_id = :userId " +
         "AND ST_Distance_Sphere(POINT(longitude, latitude), POINT(:lon, :lat)) <= :radius " +
         "AND created_at >= :since ORDER BY created_at DESC",
         nativeQuery = true)
  List<GeofenceEvent> findNearbyEvents(
      @Param("userId") Long userId,
      @Param("lat") Double latitude,
      @Param("lon") Double longitude,
      @Param("radius") Integer radiusMeters,
      @Param("since") LocalDateTime since
  );

  /**
   * 알림이 전송되지 않은 이벤트 조회
   */
  List<GeofenceEvent> findByNotificationSentFalseAndCreatedAtAfter(LocalDateTime after);

  /**
   * 이벤트 타입별 통계
   */
  @Query("SELECT e.eventType, COUNT(e) FROM GeofenceEvent e " +
         "WHERE e.user = :user AND e.createdAt >= :since " +
         "GROUP BY e.eventType")
  List<Object[]> getEventTypeStatistics(
      @Param("user") User user,
      @Param("since") LocalDateTime since
  );

  /**
   * 지오펜스별 이벤트 개수
   */
  long countByGeofenceAndCreatedAtAfter(Geofence geofence, LocalDateTime after);

  /**
   * 위험 레벨별 이벤트 개수
   */
  @Query("SELECT e.riskLevel, COUNT(e) FROM GeofenceEvent e " +
         "WHERE e.user = :user GROUP BY e.riskLevel")
  List<Object[]> countByRiskLevel(@Param("user") User user);

  /**
   * 체류 시간이 긴 이벤트 조회
   */
  @Query("SELECT e FROM GeofenceEvent e WHERE e.user = :user " +
         "AND e.eventType = 'DWELL' AND e.durationSeconds > :seconds " +
         "ORDER BY e.durationSeconds DESC")
  List<GeofenceEvent> findLongDwellEvents(
      @Param("user") User user,
      @Param("seconds") Long seconds
  );

  /**
   * 특정 지오펜스에서의 마지막 진입 이벤트
   */
  Optional<GeofenceEvent> findTopByUserAndGeofenceAndEventTypeOrderByCreatedAtDesc(
      User user,
      Geofence geofence,
      GeofenceEvent.EventType eventType
  );

  /**
   * 사용자의 현재 위치 상태 확인
   */
  @Query("SELECT e FROM GeofenceEvent e WHERE e.user = :user " +
         "AND e.eventType IN ('ENTRY', 'EXIT') " +
         "ORDER BY e.createdAt DESC LIMIT 1")
  Optional<GeofenceEvent> findLastLocationEvent(@Param("user") User user);

  /**
   * 경고 이벤트 조회
   */
  List<GeofenceEvent> findByUserAndEventTypeAndCreatedAtAfter(
      User user,
      GeofenceEvent.EventType eventType,
      LocalDateTime after
  );
}