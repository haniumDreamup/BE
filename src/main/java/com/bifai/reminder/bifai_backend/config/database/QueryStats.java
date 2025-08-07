package com.bifai.reminder.bifai_backend.config.database;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 개별 쿼리에 대한 통계 정보
 */
@Getter
public class QueryStats {
    
    private final String sql;
    private final AtomicLong executionCount = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> firstExecuted = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastExecuted = new AtomicReference<>();
    
    public QueryStats(String sql) {
        this.sql = sql;
    }
    
    /**
     * 쿼리 실행 기록
     */
    public void recordExecution(long executionTimeMs) {
        executionCount.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);
        
        // 최소/최대 시간 업데이트
        minExecutionTime.updateAndGet(current -> Math.min(current, executionTimeMs));
        maxExecutionTime.updateAndGet(current -> Math.max(current, executionTimeMs));
        
        // 처음 실행 시간 기록
        firstExecuted.compareAndSet(null, LocalDateTime.now());
        
        // 마지막 실행 시간 업데이트
        lastExecuted.set(LocalDateTime.now());
    }
    
    /**
     * 평균 실행 시간 계산
     */
    public double getAverageExecutionTime() {
        long count = executionCount.get();
        if (count == 0) return 0;
        return (double) totalExecutionTime.get() / count;
    }
    
    /**
     * 통계 요약 문자열
     */
    public String getSummary() {
        return String.format(
            "실행 횟수: %d, 평균: %.2fms, 최소: %dms, 최대: %dms",
            executionCount.get(),
            getAverageExecutionTime(),
            minExecutionTime.get() == Long.MAX_VALUE ? 0 : minExecutionTime.get(),
            maxExecutionTime.get()
        );
    }
}