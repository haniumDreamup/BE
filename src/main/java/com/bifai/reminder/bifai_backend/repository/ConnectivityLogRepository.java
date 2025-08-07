package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ConnectivityLog;
import com.bifai.reminder.bifai_backend.entity.ConnectivityLog.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 연결 로그 Repository
 * BIF 사용자 디바이스의 네트워크 연결 상태 추적을 위한 데이터 접근 계층
 */
@Repository
public interface ConnectivityLogRepository extends JpaRepository<ConnectivityLog, Long> {
    
    /**
     * 디바이스의 현재 연결 상태 조회
     */
    @Query("SELECT c FROM ConnectivityLog c WHERE c.device.id = :deviceId " +
           "AND c.connectionStatus = 'CONNECTED' " +
           "ORDER BY c.timestamp DESC")
    List<ConnectivityLog> findActiveConnections(@Param("deviceId") Long deviceId);
    
    /**
     * 디바이스의 특정 기간 연결 로그 조회
     */
    @Query("SELECT c FROM ConnectivityLog c WHERE c.device.id = :deviceId " +
           "AND c.timestamp BETWEEN :start AND :end " +
           "ORDER BY c.timestamp DESC")
    List<ConnectivityLog> findByDeviceIdAndPeriod(@Param("deviceId") Long deviceId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
    
    /**
     * 연결 유형별 로그 조회
     */
    List<ConnectivityLog> findByDevice_IdAndNetworkTypeOrderByTimestampDesc(
            Long deviceId, NetworkType networkType);
    
    /**
     * 연결 끊김 이벤트 조회
     */
    @Query("SELECT c FROM ConnectivityLog c WHERE c.device.id = :deviceId " +
           "AND c.connectionStatus = 'DISCONNECTED' " +
           "AND c.timestamp >= :since " +
           "ORDER BY c.timestamp DESC")
    List<ConnectivityLog> findDisconnectionEvents(@Param("deviceId") Long deviceId,
                                                 @Param("since") LocalDateTime since);
    
    /**
     * 약한 신호 강도 이벤트 조회
     */
    @Query("SELECT c FROM ConnectivityLog c WHERE c.device.id = :deviceId " +
           "AND c.signalStrength < :threshold " +
           "AND c.timestamp >= :since")
    List<ConnectivityLog> findWeakSignalEvents(@Param("deviceId") Long deviceId,
                                              @Param("threshold") Integer threshold,
                                              @Param("since") LocalDateTime since);
    
    /**
     * 사용자의 모든 디바이스 연결 상태 요약
     */
    @Query("SELECT c.device.id, c.networkType, c.connectionStatus, MAX(c.timestamp) " +
           "FROM ConnectivityLog c " +
           "WHERE c.device.user.userId = :userId " +
           "GROUP BY c.device.id, c.networkType, c.connectionStatus")
    List<Object[]> findConnectionSummaryByUserId(@Param("userId") Long userId);
    
    /**
     * 네트워크별 연결 시간 통계
     */
    @Query("SELECT c.networkType, COUNT(c), AVG(c.durationSeconds) " +
           "FROM ConnectivityLog c " +
           "WHERE c.device.id = :deviceId " +
           "AND c.durationSeconds IS NOT NULL " +
           "AND c.timestamp >= :since " +
           "GROUP BY c.networkType")
    List<Object[]> findNetworkUsageStats(@Param("deviceId") Long deviceId,
                                        @Param("since") LocalDateTime since);
}