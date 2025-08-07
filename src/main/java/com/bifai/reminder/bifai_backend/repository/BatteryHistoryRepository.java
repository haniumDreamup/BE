package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.BatteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 배터리 이력 Repository
 * BIF 사용자 디바이스의 배터리 상태 추적을 위한 데이터 접근 계층
 */
@Repository
public interface BatteryHistoryRepository extends JpaRepository<BatteryHistory, Long> {
    
    /**
     * 디바이스의 최신 배터리 상태 조회
     */
    @Query("SELECT b FROM BatteryHistory b WHERE b.device.id = :deviceId " +
           "ORDER BY b.recordedAt DESC")
    Optional<BatteryHistory> findLatestByDeviceId(@Param("deviceId") Long deviceId);
    
    /**
     * 디바이스의 특정 기간 배터리 이력 조회
     */
    List<BatteryHistory> findByDevice_IdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long deviceId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 낮은 배터리 상태 이력 조회
     */
    @Query("SELECT b FROM BatteryHistory b WHERE b.device.id = :deviceId " +
           "AND b.batteryLevel <= :threshold " +
           "AND b.recordedAt >= :since " +
           "ORDER BY b.recordedAt DESC")
    List<BatteryHistory> findLowBatteryEvents(@Param("deviceId") Long deviceId,
                                            @Param("threshold") Integer threshold,
                                            @Param("since") LocalDateTime since);
    
    /**
     * 사용자의 모든 디바이스 최신 배터리 상태
     */
    @Query("SELECT b FROM BatteryHistory b " +
           "WHERE b.device.user.userId = :userId " +
           "AND b.recordedAt = (SELECT MAX(b2.recordedAt) FROM BatteryHistory b2 " +
           "                    WHERE b2.device.id = b.device.id)")
    List<BatteryHistory> findLatestBatteryStatusByUserId(@Param("userId") Long userId);
    
    /**
     * 충전 패턴 분석을 위한 충전 시작/종료 이벤트 조회
     */
    @Query("SELECT b FROM BatteryHistory b WHERE b.device.id = :deviceId " +
           "AND b.recordedAt >= :since " +
           "AND ((b.isCharging = true AND EXISTS (SELECT 1 FROM BatteryHistory b2 " +
           "     WHERE b2.device.id = b.device.id AND b2.recordedAt < b.recordedAt " +
           "     AND b2.isCharging = false ORDER BY b2.recordedAt DESC LIMIT 1)) " +
           "OR (b.isCharging = false AND EXISTS (SELECT 1 FROM BatteryHistory b3 " +
           "     WHERE b3.device.id = b.device.id AND b3.recordedAt < b.recordedAt " +
           "     AND b3.isCharging = true ORDER BY b3.recordedAt DESC LIMIT 1)))")
    List<BatteryHistory> findChargingEvents(@Param("deviceId") Long deviceId,
                                          @Param("since") LocalDateTime since);
    
    /**
     * 평균 배터리 소모율 계산을 위한 데이터 조회
     */
    @Query("SELECT AVG(b.batteryLevel) FROM BatteryHistory b " +
           "WHERE b.device.id = :deviceId " +
           "AND b.recordedAt BETWEEN :start AND :end")
    Double calculateAverageBatteryLevel(@Param("deviceId") Long deviceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}