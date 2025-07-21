package com.bifai.reminder.bifai_backend.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 데이터베이스 관련 설정 속성
 * application.yml의 데이터베이스 설정을 타입 세이프하게 관리
 */
@Data
@Component
@ConfigurationProperties(prefix = "bif.database")
public class DatabaseProperties {
    
    /**
     * 커넥션 풀 설정
     */
    private ConnectionPool connectionPool = new ConnectionPool();
    
    /**
     * 쿼리 최적화 설정
     */
    private QueryOptimization queryOptimization = new QueryOptimization();
    
    /**
     * 모니터링 설정
     */
    private Monitoring monitoring = new Monitoring();
    
    /**
     * 보안 설정
     */
    private Security security = new Security();
    
    @Data
    public static class ConnectionPool {
        /**
         * 최대 풀 크기
         */
        private int maximumPoolSize = 10;
        
        /**
         * 최소 유휴 연결 수
         */
        private int minimumIdle = 5;
        
        /**
         * 유휴 타임아웃 (밀리초)
         */
        private long idleTimeout = 600000; // 10분
        
        /**
         * 연결 타임아웃 (밀리초)
         */
        private long connectionTimeout = 30000; // 30초
        
        /**
         * 최대 수명 (밀리초)
         */
        private long maxLifetime = 1800000; // 30분
        
        /**
         * 누수 감지 임계값 (밀리초)
         */
        private long leakDetectionThreshold = 60000; // 1분
        
        /**
         * 유효성 검사 타임아웃 (밀리초)
         */
        private long validationTimeout = 5000; // 5초
        
        /**
         * 초기화 실패 타임아웃 (밀리초)
         */
        private long initializationFailTimeout = 60000; // 1분
    }
    
    @Data
    public static class QueryOptimization {
        /**
         * 배치 크기
         */
        private int batchSize = 25;
        
        /**
         * 페치 크기
         */
        private int fetchSize = 100;
        
        /**
         * 쿼리 타임아웃 (초)
         */
        private int queryTimeout = 30;
        
        /**
         * 슬로우 쿼리 임계값 (밀리초)
         */
        private long slowQueryThreshold = 1000; // 1초
        
        /**
         * 쿼리 플랜 캐시 크기
         */
        private int queryPlanCacheSize = 2048;
        
        /**
         * 준비된 문장 캐시 크기
         */
        private int preparedStatementCacheSize = 250;
    }
    
    @Data
    public static class Monitoring {
        /**
         * 통계 활성화
         */
        private boolean enableStatistics = false;
        
        /**
         * 슬로우 쿼리 로깅
         */
        private boolean logSlowQueries = true;
        
        /**
         * 연결 상태 로깅
         */
        private boolean logConnectionState = true;
        
        /**
         * 메트릭 수집 간격 (초)
         */
        private int metricsCollectionInterval = 60;
        
        /**
         * JMX 활성화
         */
        private boolean enableJmx = true;
    }
    
    @Data
    public static class Security {
        /**
         * SSL 사용 여부
         */
        private boolean useSsl = false;
        
        /**
         * SSL 인증서 검증
         */
        private boolean verifySslCertificate = true;
        
        /**
         * 연결 암호화
         */
        private boolean encryptConnection = false;
        
        /**
         * SQL 인젝션 방지
         */
        private boolean enableSqlInjectionPrevention = true;
        
        /**
         * 민감한 데이터 마스킹
         */
        private boolean maskSensitiveData = true;
    }
}