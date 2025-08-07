package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.LocationHistory;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 위치 이력 Repository
 * BIF 사용자의 위치 추적 및 패턴 분석을 위한 데이터 접근 계층
 */
@Repository
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {
    
    /**
     * 사용자의 최신 위치 조회
     */
    @Query("SELECT l FROM LocationHistory l WHERE l.user.userId = :userId " +
           "ORDER BY l.capturedAt DESC")
    Optional<LocationHistory> findLatestByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 특정 기간 위치 이력 조회
     */
    List<LocationHistory> findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 사용자의 위치 이력 페이징 조회
     */
    Page<LocationHistory> findByUser_UserIdOrderByCapturedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 영역 내 위치 조회 (안전 구역 확인용)
     */
    @Query("SELECT l FROM LocationHistory l WHERE l.user.userId = :userId " +
           "AND CAST(l.latitude AS double) BETWEEN :minLat AND :maxLat " +
           "AND CAST(l.longitude AS double) BETWEEN :minLng AND :maxLng " +
           "AND l.capturedAt >= :since")
    List<LocationHistory> findWithinBounds(@Param("userId") Long userId,
                                         @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                         @Param("minLng") Double minLng, @Param("maxLng") Double maxLng,
                                         @Param("since") LocalDateTime since);
    
    /**
     * 사용자의 자주 방문하는 위치 분석
     */
    @Query(value = "SELECT ROUND(latitude, 4) as lat, ROUND(longitude, 4) as lng, COUNT(*) as visit_count " +
                   "FROM locations WHERE user_id = :userId " +
                   "AND captured_at >= :since " +
                   "GROUP BY lat, lng " +
                   "HAVING visit_count >= :minVisits " +
                   "ORDER BY visit_count DESC", 
           nativeQuery = true)
    List<Object[]> findFrequentLocations(@Param("userId") Long userId,
                                        @Param("since") LocalDateTime since,
                                        @Param("minVisits") Integer minVisits);
    
    /**
     * 디바이스별 최신 위치 조회
     */
    @Query("SELECT l FROM LocationHistory l WHERE l.device.id = :deviceId " +
           "ORDER BY l.capturedAt DESC")
    Optional<LocationHistory> findLatestByDeviceId(@Param("deviceId") Long deviceId);
    
    /**
     * 특정 위치 유형별 조회
     */
    List<LocationHistory> findByUser_UserIdAndLocationTypeOrderByCapturedAtDesc(
            Long userId, String locationType);
    
    /**
     * 오래된 위치 데이터 삭제용 조회
     */
    @Query("SELECT l FROM LocationHistory l WHERE l.capturedAt < :threshold")
    List<LocationHistory> findOldLocationData(@Param("threshold") LocalDateTime threshold);
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 사용자별 위치 이력 조회 (최신순)
     */
    List<LocationHistory> findByUserOrderByCapturedAtDesc(User user);
    
    /**
     * 사용자의 최신 위치 조회
     */
    Optional<LocationHistory> findFirstByUserOrderByCapturedAtDesc(User user);
    
    /**
     * 특정 기간 내 위치 이력 조회
     */
    List<LocationHistory> findByUserAndCapturedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    
    /**
     * 디바이스별 위치 이력 조회
     */
    List<LocationHistory> findByDevice(Device device);
    
    /**
     * 반경 내 위치 검색
     */
    @Query("SELECT l FROM LocationHistory l WHERE l.user = :user " +
           "AND (6371 * acos(cos(radians(CAST(:centerLat AS double))) * cos(radians(CAST(l.latitude AS double))) * " +
           "cos(radians(CAST(l.longitude AS double)) - radians(CAST(:centerLon AS double))) + sin(radians(CAST(:centerLat AS double))) * " +
           "sin(radians(CAST(l.latitude AS double))))) <= :radiusKm")
    List<LocationHistory> findLocationsWithinRadius(@Param("user") User user,
                                                   @Param("centerLat") BigDecimal centerLat,
                                                   @Param("centerLon") BigDecimal centerLon,
                                                   @Param("radiusKm") double radiusKm);
    
    /**
     * 주소로 위치 검색
     */
    List<LocationHistory> findByUserAndAddressContaining(User user, String addressPart);
    
    /**
     * 위치 타입별 조회
     */
    List<LocationHistory> findByUserAndLocationType(User user, LocationHistory.LocationType locationType);
    
    /**
     * 정확도 기준 위치 조회
     */
    List<LocationHistory> findByUserAndAccuracyLessThan(User user, BigDecimal accuracy);
    
    /**
     * 이동 속도가 있는 위치 조회
     */
    List<LocationHistory> findByUserAndSpeedGreaterThan(User user, BigDecimal speed);
    
    /**
     * 오래된 위치 이력 조회
     */
    List<LocationHistory> findByUserAndCapturedAtBefore(User user, LocalDateTime threshold);
    
    /**
     * 사용자의 최신 위치 조회 (Top 1)
     */
    Optional<LocationHistory> findTopByUserOrderByCapturedAtDesc(User user);
    
    /**
     * 특정 시간 이후 위치 이력 조회 (최신순)
     */
    List<LocationHistory> findByUserAndCapturedAtAfterOrderByCapturedAtDesc(User user, LocalDateTime after);
}