package com.bifai.reminder.bifai_backend.config.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터베이스 상태 점검 인디케이터
 * 데이터베이스 연결 상태와 성능을 모니터링
 */
@Slf4j
@Component("databaseHealth")
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    private static final int WARNING_RESPONSE_TIME_MS = 100;
    private static final int CRITICAL_RESPONSE_TIME_MS = 1000;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        Map<String, Object> details = new HashMap<>();
        
        try {
            // 데이터베이스 연결 테스트 및 메타데이터 수집
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                // 연결 유효성 검사
                if (!connection.isValid(5)) {
                    return builder.down()
                            .withDetail("error", "Invalid database connection")
                            .build();
                }
                
                // 응답 시간 측정
                long connectionTime = System.currentTimeMillis() - startTime;
                details.put("connectionTimeMs", connectionTime);
                
                // 데이터베이스 메타데이터 수집
                DatabaseMetaData metaData = connection.getMetaData();
                details.put("database", getDatabaseInfo(metaData));
                
                // 테이블 존재 확인 (BIF 핵심 테이블)
                checkCriticalTables(connection, details);
                
                // 쿼리 성능 테스트
                testQueryPerformance(connection, details);
                
                // 디스크 공간 확인 (H2 데이터베이스의 경우)
                checkDiskSpace(connection, metaData, details);
                
                // 상태 결정
                Status status = determineStatus(connectionTime, details);
                builder.status(status);
                
                // 상세 정보 추가
                details.forEach(builder::withDetail);
                
                // 추가 경고 메시지
                if (connectionTime > WARNING_RESPONSE_TIME_MS) {
                    builder.withDetail("warning", 
                        String.format("Slow database response: %dms", connectionTime));
                }
                
            }
            
        } catch (SQLException e) {
            log.error("데이터베이스 헬스 체크 실패", e);
            return builder.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
        
        return builder.build();
    }
    
    /**
     * 데이터베이스 정보 수집
     */
    private Map<String, Object> getDatabaseInfo(DatabaseMetaData metaData) throws SQLException {
        Map<String, Object> info = new HashMap<>();
        info.put("productName", metaData.getDatabaseProductName());
        info.put("productVersion", metaData.getDatabaseProductVersion());
        info.put("driverName", metaData.getDriverName());
        info.put("driverVersion", metaData.getDriverVersion());
        info.put("url", sanitizeUrl(metaData.getURL()));
        return info;
    }
    
    /**
     * URL에서 민감한 정보 제거
     */
    private String sanitizeUrl(String url) {
        if (url == null) return "unknown";
        // 비밀번호 제거
        return url.replaceAll("password=[^&]*", "password=***")
                  .replaceAll(":[^:@]*@", ":***@");
    }
    
    /**
     * BIF 서비스 핵심 테이블 존재 확인
     */
    private void checkCriticalTables(Connection connection, Map<String, Object> details) {
        String[] criticalTables = {"users", "reminders", "locations", "schedules"};
        Map<String, Boolean> tableStatus = new HashMap<>();
        
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            
            for (String tableName : criticalTables) {
                try (ResultSet rs = metaData.getTables(null, null, 
                        tableName.toUpperCase(), new String[]{"TABLE"})) {
                    boolean exists = rs.next();
                    if (!exists) {
                        // 소문자로도 확인
                        try (ResultSet rs2 = metaData.getTables(null, null, 
                                tableName.toLowerCase(), new String[]{"TABLE"})) {
                            exists = rs2.next();
                        }
                    }
                    tableStatus.put(tableName, exists);
                }
            }
            
            details.put("criticalTables", tableStatus);
            
            // 누락된 테이블이 있으면 경고
            long missingTables = tableStatus.values().stream()
                    .filter(exists -> !exists)
                    .count();
            
            if (missingTables > 0) {
                details.put("missingTablesCount", missingTables);
            }
            
        } catch (SQLException e) {
            log.warn("테이블 확인 중 오류 발생", e);
            details.put("tableCheckError", e.getMessage());
        }
    }
    
    /**
     * 쿼리 성능 테스트
     */
    private void testQueryPerformance(Connection connection, Map<String, Object> details) {
        String testQuery = "SELECT 1";
        long startTime = System.currentTimeMillis();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(testQuery)) {
            
            if (rs.next()) {
                long queryTime = System.currentTimeMillis() - startTime;
                details.put("testQueryTimeMs", queryTime);
                
                if (queryTime > 50) {
                    details.put("queryPerformanceWarning", 
                        "Test query took " + queryTime + "ms");
                }
            }
            
        } catch (SQLException e) {
            details.put("queryTestError", e.getMessage());
        }
    }
    
    /**
     * 디스크 공간 확인 (H2 데이터베이스용)
     */
    private void checkDiskSpace(Connection connection, DatabaseMetaData metaData, 
                               Map<String, Object> details) {
        try {
            String productName = metaData.getDatabaseProductName();
            
            if ("H2".equalsIgnoreCase(productName)) {
                // H2 데이터베이스의 경우 파일 크기 확인
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS " +
                         "WHERE SETTING_NAME = 'info.FILE_WRITE'")) {
                    
                    if (rs.next()) {
                        details.put("h2FileWriteInfo", rs.getString(1));
                    }
                }
            }
            
        } catch (SQLException e) {
            // 디스크 공간 확인은 선택적이므로 오류 무시
            log.debug("디스크 공간 확인 실패", e);
        }
    }
    
    /**
     * 헬스 상태 결정
     */
    private Status determineStatus(long connectionTime, Map<String, Object> details) {
        // 심각한 응답 시간
        if (connectionTime > CRITICAL_RESPONSE_TIME_MS) {
            return Status.DOWN;
        }
        
        // 누락된 테이블 확인
        Object missingTables = details.get("missingTablesCount");
        if (missingTables instanceof Long && (Long) missingTables > 0) {
            return new Status("WARNING");
        }
        
        // 느린 응답 시간
        if (connectionTime > WARNING_RESPONSE_TIME_MS) {
            return new Status("WARNING");
        }
        
        return Status.UP;
    }
}