package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Location;
import com.bifai.reminder.bifai_backend.entity.Location.LocationType;
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
 * 위치 정보 리포지토리
 * 위치 추적 데이터 접근을 위한 인터페이스
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

  /**
   * 사용자의 최신 위치 조회
   */
  Optional<Location> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

  /**
   * 사용자의 위치 이력 조회 (페이징)
   */
  Page<Location> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * 특정 기간 동안의 위치 이력 조회
   */
  List<Location> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      Long userId,
      LocalDateTime startTime,
      LocalDateTime endTime
  );

  /**
   * 특정 유형의 위치 정보 조회
   */
  List<Location> findByUserIdAndLocationTypeOrderByCreatedAtDesc(
      Long userId,
      LocationType locationType
  );

  /**
   * 안전 구역 이탈 위치 조회
   */
  @Query("SELECT l FROM Location l WHERE l.user.id = :userId " +
         "AND l.isInSafeZone = false AND l.createdAt >= :since " +
         "ORDER BY l.createdAt DESC")
  List<Location> findOutOfSafeZoneLocations(
      @Param("userId") Long userId,
      @Param("since") LocalDateTime since
  );

  /**
   * 특정 Geofence 내의 위치 조회
   */
  List<Location> findByCurrentGeofenceIdOrderByCreatedAtDesc(Long geofenceId);

  /**
   * 배터리 부족 상태의 최신 위치 조회
   */
  @Query("SELECT l FROM Location l WHERE l.user.id = :userId " +
         "AND l.batteryLevel < :threshold " +
         "ORDER BY l.createdAt DESC")
  List<Location> findLowBatteryLocations(
      @Param("userId") Long userId,
      @Param("threshold") Integer threshold,
      Pageable pageable
  );

  /**
   * 특정 활동 유형의 위치 조회
   */
  List<Location> findByUserIdAndActivityTypeAndCreatedAtAfter(
      Long userId,
      Location.ActivityType activityType,
      LocalDateTime after
  );

  /**
   * 사용자별 위치 통계
   */
  @Query("SELECT l.locationType, COUNT(l) FROM Location l " +
         "WHERE l.user.id = :userId AND l.createdAt >= :startDate " +
         "GROUP BY l.locationType")
  List<Object[]> getLocationStatsByUser(
      @Param("userId") Long userId,
      @Param("startDate") LocalDateTime startDate
  );

  /**
   * 특정 디바이스의 최신 위치
   */
  Optional<Location> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);

  /**
   * 근처 사용자 조회 (반경 내)
   */
  @Query("SELECT DISTINCT l.user FROM Location l WHERE " +
         "(6371 * acos(cos(radians(:latitude)) * cos(radians(l.latitude)) * " +
         "cos(radians(l.longitude) - radians(:longitude)) + " +
         "sin(radians(:latitude)) * sin(radians(l.latitude)))) < :radiusKm " +
         "AND l.createdAt >= :since AND l.user.id != :excludeUserId")
  List<Object> findNearbyUsers(
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusKm") Double radiusKm,
      @Param("since") LocalDateTime since,
      @Param("excludeUserId") Long excludeUserId
  );

  /**
   * 오래된 위치 데이터 삭제
   */
  void deleteByCreatedAtBefore(LocalDateTime before);

  /**
   * 사용자의 이동 경로 조회 (시간순)
   */
  @Query("SELECT l FROM Location l WHERE l.user.id = :userId " +
         "AND l.createdAt BETWEEN :startTime AND :endTime " +
         "ORDER BY l.createdAt ASC")
  List<Location> findUserPath(
      @Param("userId") Long userId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime
  );
}