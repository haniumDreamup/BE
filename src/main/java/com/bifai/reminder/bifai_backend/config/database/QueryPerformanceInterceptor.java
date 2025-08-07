package com.bifai.reminder.bifai_backend.config.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 쿼리 성능 모니터링 인터셉터
 * 슬로우 쿼리를 감지하고 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryPerformanceInterceptor implements StatementInspector {
    
    @Value("${bif.database.query-optimization.slow-query-threshold:1000}")
    private long slowQueryThresholdMs;
    
    // 쿼리 통계 저장
    private final Map<String, QueryStats> queryStatsMap = new ConcurrentHashMap<>();
    
    // 전체 쿼리 카운터
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    
    @Override
    public String inspect(String sql) {
        // 쿼리 시작 시간을 ThreadLocal에 저장
        QueryContext.startQuery(sql);
        return sql;
    }
    
    /**
     * 쿼리 실행 완료 시 호출
     */
    public void queryExecuted(String sql, long executionTimeMs) {
        totalQueries.incrementAndGet();
        
        // 슬로우 쿼리 체크
        if (executionTimeMs > slowQueryThresholdMs) {
            slowQueries.incrementAndGet();
            logSlowQuery(sql, executionTimeMs);
        }
        
        // 쿼리 통계 업데이트
        updateQueryStats(sql, executionTimeMs);
    }
    
    /**
     * 슬로우 쿼리 로깅
     */
    private void logSlowQuery(String sql, long executionTimeMs) {
        log.warn("슬로우 쿼리 감지 - 실행 시간: {}ms", executionTimeMs);
        log.warn("쿼리: {}", formatSql(sql));
        
        // 스택 트레이스에서 호출 위치 찾기
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith("com.bifai.reminder") && 
                !className.contains("QueryPerformanceInterceptor")) {
                log.warn("호출 위치: {}:{}", element.getClassName(), element.getLineNumber());
                break;
            }
        }
    }
    
    /**
     * SQL 포맷팅
     */
    private String formatSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        
        // 긴 쿼리는 축약
        if (sql.length() > 500) {
            return sql.substring(0, 500) + "... (truncated)";
        }
        
        return sql;
    }
    
    /**
     * 쿼리 통계 업데이트
     */
    private void updateQueryStats(String sql, long executionTimeMs) {
        // 쿼리 정규화 (파라미터 제거)
        String normalizedSql = normalizeSql(sql);
        
        queryStatsMap.compute(normalizedSql, (key, stats) -> {
            if (stats == null) {
                stats = new QueryStats(normalizedSql);
            }
            stats.recordExecution(executionTimeMs);
            return stats;
        });
    }
    
    /**
     * SQL 정규화 (파라미터를 ?로 변경)
     */
    private String normalizeSql(String sql) {
        if (sql == null) return null;
        
        // 숫자 파라미터를 ?로 변경
        String normalized = sql.replaceAll("\\b\\d+\\b", "?");
        
        // 문자열 파라미터를 ?로 변경
        normalized = normalized.replaceAll("'[^']*'", "?");
        
        // 연속된 공백을 하나로
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    /**
     * 쿼리 통계 조회
     */
    public Map<String, QueryStats> getQueryStats() {
        return new ConcurrentHashMap<>(queryStatsMap);
    }
    
    /**
     * 전체 통계 조회
     */
    public PerformanceSummary getPerformanceSummary() {
        return PerformanceSummary.builder()
                .totalQueries(totalQueries.get())
                .slowQueries(slowQueries.get())
                .slowQueryThreshold(slowQueryThresholdMs)
                .uniqueQueries(queryStatsMap.size())
                .build();
    }
    
    /**
     * 통계 초기화
     */
    public void resetStats() {
        queryStatsMap.clear();
        totalQueries.set(0);
        slowQueries.set(0);
        log.info("쿼리 성능 통계가 초기화되었습니다");
    }
    
    /**
     * 쿼리 실행 컨텍스트
     */
    private static class QueryContext {
        private static final ThreadLocal<QueryExecution> currentQuery = new ThreadLocal<>();
        
        static void startQuery(String sql) {
            currentQuery.set(new QueryExecution(sql, Instant.now()));
        }
        
        static QueryExecution endQuery() {
            QueryExecution execution = currentQuery.get();
            currentQuery.remove();
            return execution;
        }
    }
    
    /**
     * 쿼리 실행 정보
     */
    private static class QueryExecution {
        final String sql;
        final Instant startTime;
        
        QueryExecution(String sql, Instant startTime) {
            this.sql = sql;
            this.startTime = startTime;
        }
        
        long getDuration() {
            return Duration.between(startTime, Instant.now()).toMillis();
        }
    }
}