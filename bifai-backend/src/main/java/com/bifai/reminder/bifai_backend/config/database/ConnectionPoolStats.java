package com.bifai.reminder.bifai_backend.config.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커넥션 풀 통계 정보
 * HikariCP 풀의 현재 상태를 나타내는 스냅샷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionPoolStats {
    
    /**
     * 풀 이름
     */
    private String poolName;
    
    /**
     * 전체 커넥션 수
     */
    private int totalConnections;
    
    /**
     * 활성 커넥션 수
     */
    private int activeConnections;
    
    /**
     * 유휴 커넥션 수
     */
    private int idleConnections;
    
    /**
     * 커넥션 대기 중인 스레드 수
     */
    private int threadsAwaitingConnection;
    
    /**
     * 최대 풀 크기
     */
    private int maximumPoolSize;
    
    /**
     * 최소 유휴 커넥션 수
     */
    private int minimumIdle;
    
    /**
     * 통계 수집 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 사용률 계산 (%)
     */
    public double getUtilizationRate() {
        if (maximumPoolSize == 0) {
            return 0;
        }
        return (double) activeConnections / maximumPoolSize * 100;
    }
    
    /**
     * 풀 상태가 건강한지 확인
     */
    public boolean isHealthy() {
        return getUtilizationRate() < 90 && threadsAwaitingConnection == 0;
    }
    
    /**
     * 풀이 포화 상태인지 확인
     */
    public boolean isSaturated() {
        return activeConnections >= maximumPoolSize;
    }
    
    /**
     * 통계를 사용할 수 없는 경우의 기본값
     */
    public static ConnectionPoolStats unavailable() {
        return ConnectionPoolStats.builder()
                .poolName("UNAVAILABLE")
                .totalConnections(-1)
                .activeConnections(-1)
                .idleConnections(-1)
                .threadsAwaitingConnection(-1)
                .maximumPoolSize(-1)
                .minimumIdle(-1)
                .build();
    }
    
    /**
     * 요약 정보 문자열
     */
    public String getSummary() {
        return String.format("Pool: %s, Active: %d/%d (%.1f%%), Idle: %d, Waiting: %d",
                poolName, activeConnections, maximumPoolSize, getUtilizationRate(),
                idleConnections, threadsAwaitingConnection);
    }
}