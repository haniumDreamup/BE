package com.bifai.reminder.bifai_backend.config.database;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 데이터베이스 커넥션 풀 모니터
 * HikariCP 풀의 상태를 실시간으로 모니터링하고 문제 상황 감지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionPoolMonitor implements HealthIndicator {
    
    private final DataSource dataSource;
    
    /**
     * 커넥션 풀 상태 확인 (Health Check)
     */
    @Override
    public Health health() {
        if (!(dataSource instanceof HikariDataSource)) {
            return Health.up()
                    .withDetail("type", "Non-HikariCP DataSource")
                    .build();
        }
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
        
        if (poolMXBean == null) {
            return Health.down()
                    .withDetail("error", "Pool MXBean not available")
                    .build();
        }
        
        try {
            int totalConnections = poolMXBean.getTotalConnections();
            int activeConnections = poolMXBean.getActiveConnections();
            int idleConnections = poolMXBean.getIdleConnections();
            int threadsAwaitingConnection = poolMXBean.getThreadsAwaitingConnection();
            
            Health.Builder builder = Health.up();
            builder.withDetail("poolName", hikariDataSource.getPoolName())
                   .withDetail("totalConnections", totalConnections)
                   .withDetail("activeConnections", activeConnections)
                   .withDetail("idleConnections", idleConnections)
                   .withDetail("threadsAwaitingConnection", threadsAwaitingConnection)
                   .withDetail("maximumPoolSize", hikariDataSource.getMaximumPoolSize())
                   .withDetail("minimumIdle", hikariDataSource.getMinimumIdle());
            
            // 경고 상태 체크
            double utilizationRate = (double) activeConnections / hikariDataSource.getMaximumPoolSize();
            if (utilizationRate > 0.9) {
                builder.withDetail("warning", "High connection pool utilization: " + String.format("%.1f%%", utilizationRate * 100));
            }
            
            if (threadsAwaitingConnection > 0) {
                builder.withDetail("warning", "Threads waiting for connection: " + threadsAwaitingConnection);
            }
            
            // 연결 테스트
            testConnection(builder);
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    
    /**
     * 실제 데이터베이스 연결 테스트
     */
    private void testConnection(Health.Builder builder) {
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                long responseTime = System.currentTimeMillis() - startTime;
                builder.withDetail("connectionTestTime", responseTime + "ms");
                
                if (responseTime > 1000) {
                    builder.withDetail("warning", "Slow connection test: " + responseTime + "ms");
                }
            } else {
                builder.withDetail("error", "Invalid connection");
            }
        } catch (SQLException e) {
            builder.withDetail("connectionError", e.getMessage());
        }
    }
    
    /**
     * 주기적으로 커넥션 풀 상태 로깅 (1분마다)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void logPoolStatistics() {
        if (!(dataSource instanceof HikariDataSource)) {
            return;
        }
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
        
        if (poolMXBean != null) {
            int totalConnections = poolMXBean.getTotalConnections();
            int activeConnections = poolMXBean.getActiveConnections();
            int idleConnections = poolMXBean.getIdleConnections();
            int threadsAwaitingConnection = poolMXBean.getThreadsAwaitingConnection();
            
            double utilizationRate = (double) activeConnections / hikariDataSource.getMaximumPoolSize();
            
            log.info("BIF 커넥션 풀 통계 - 전체: {}, 활성: {}, 유휴: {}, 대기 스레드: {}, 사용률: {:.1f}%",
                    totalConnections, activeConnections, idleConnections, 
                    threadsAwaitingConnection, utilizationRate * 100);
            
            // 경고 상황 로깅
            if (utilizationRate > 0.9) {
                log.warn("커넥션 풀 사용률이 90%를 초과했습니다: {:.1f}%", utilizationRate * 100);
            }
            
            if (threadsAwaitingConnection > 0) {
                log.warn("{}개의 스레드가 커넥션을 대기 중입니다", threadsAwaitingConnection);
            }
            
            if (idleConnections == 0 && activeConnections == hikariDataSource.getMaximumPoolSize()) {
                log.error("커넥션 풀이 포화 상태입니다. 풀 크기 증가를 고려하세요.");
            }
        }
    }
    
    /**
     * 커넥션 풀 통계 조회
     */
    public ConnectionPoolStats getPoolStats() {
        if (!(dataSource instanceof HikariDataSource)) {
            return ConnectionPoolStats.unavailable();
        }
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
        
        if (poolMXBean == null) {
            return ConnectionPoolStats.unavailable();
        }
        
        return ConnectionPoolStats.builder()
                .poolName(hikariDataSource.getPoolName())
                .totalConnections(poolMXBean.getTotalConnections())
                .activeConnections(poolMXBean.getActiveConnections())
                .idleConnections(poolMXBean.getIdleConnections())
                .threadsAwaitingConnection(poolMXBean.getThreadsAwaitingConnection())
                .maximumPoolSize(hikariDataSource.getMaximumPoolSize())
                .minimumIdle(hikariDataSource.getMinimumIdle())
                .build();
    }
}