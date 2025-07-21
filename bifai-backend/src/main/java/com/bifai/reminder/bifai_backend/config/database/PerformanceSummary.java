package com.bifai.reminder.bifai_backend.config.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 쿼리 성능 요약 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSummary {
    
    /**
     * 전체 쿼리 수
     */
    private long totalQueries;
    
    /**
     * 슬로우 쿼리 수
     */
    private long slowQueries;
    
    /**
     * 슬로우 쿼리 임계값 (ms)
     */
    private long slowQueryThreshold;
    
    /**
     * 고유 쿼리 수
     */
    private int uniqueQueries;
    
    /**
     * 요약 생성 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 슬로우 쿼리 비율 계산
     */
    public double getSlowQueryRate() {
        if (totalQueries == 0) return 0;
        return (double) slowQueries / totalQueries * 100;
    }
    
    /**
     * 상태 확인
     */
    public boolean isHealthy() {
        return getSlowQueryRate() < 5.0; // 슬로우 쿼리가 5% 미만이면 건강
    }
    
    /**
     * 요약 문자열
     */
    public String getSummary() {
        return String.format(
            "전체: %d, 슬로우: %d (%.1f%%), 고유: %d, 임계값: %dms",
            totalQueries, slowQueries, getSlowQueryRate(), 
            uniqueQueries, slowQueryThreshold
        );
    }
}