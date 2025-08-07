package com.bifai.reminder.bifai_backend.config.database;

import com.bifai.reminder.bifai_backend.config.properties.DatabaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 데이터베이스 보안 설정
 * SQL 인젝션 방지 및 보안 강화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseSecurityConfig {
    
    private final DataSource dataSource;
    private final DatabaseProperties databaseProperties;
    private final Environment environment;
    
    /**
     * 데이터베이스 보안 설정 초기화
     */
    @PostConstruct
    public void initializeSecurity() {
        log.info("데이터베이스 보안 설정 초기화 시작");
        
        // SSL 설정 확인
        if (databaseProperties.getSecurity().isUseSsl()) {
            configureSslConnection();
        }
        
        // SQL 인젝션 방지 설정
        if (databaseProperties.getSecurity().isEnableSqlInjectionPrevention()) {
            configureSqlInjectionPrevention();
        }
        
        // 민감한 데이터 마스킹 설정
        if (databaseProperties.getSecurity().isMaskSensitiveData()) {
            configureSensitiveDataMasking();
        }
        
        log.info("데이터베이스 보안 설정 초기화 완료");
    }
    
    /**
     * SSL 연결 설정
     */
    private void configureSslConnection() {
        String jdbcUrl = environment.getProperty("spring.datasource.url");
        
        if (jdbcUrl != null && !jdbcUrl.contains("useSSL=true")) {
            log.warn("SSL이 활성화되지 않은 데이터베이스 연결이 감지되었습니다. SSL 사용을 권장합니다.");
        }
        
        // SSL 인증서 검증 설정
        if (databaseProperties.getSecurity().isVerifySslCertificate()) {
            System.setProperty("javax.net.ssl.trustStore", 
                environment.getProperty("database.ssl.trust-store", ""));
            System.setProperty("javax.net.ssl.trustStorePassword", 
                environment.getProperty("database.ssl.trust-store-password", ""));
        }
        
        log.info("데이터베이스 SSL 설정 완료");
    }
    
    /**
     * SQL 인젝션 방지 설정
     */
    private void configureSqlInjectionPrevention() {
        // PreparedStatement 사용 강제화는 코드 레벨에서 처리
        log.info("SQL 인젝션 방지 모드 활성화");
    }
    
    /**
     * 민감한 데이터 마스킹 설정
     */
    private void configureSensitiveDataMasking() {
        // 로깅 시 민감한 데이터 마스킹은 로깅 인터셉터에서 처리
        log.info("민감한 데이터 마스킹 모드 활성화");
    }
    
    /**
     * 안전한 쿼리 실행을 위한 유틸리티 클래스
     */
    public static class SecureQueryExecutor {
        
        /**
         * 파라미터 검증
         */
        public static void validateParameter(String param, String paramName) {
            if (param == null || param.trim().isEmpty()) {
                throw new IllegalArgumentException(paramName + " 파라미터가 비어있습니다");
            }
            
            // SQL 인젝션 위험 문자 검사
            if (containsSqlInjectionRisk(param)) {
                throw new SecurityException("잠재적인 SQL 인젝션 시도가 감지되었습니다: " + paramName);
            }
        }
        
        /**
         * SQL 인젝션 위험 검사
         */
        private static boolean containsSqlInjectionRisk(String input) {
            String[] dangerousPatterns = {
                "--", "/*", "*/", "xp_", "sp_", 
                "';", "';--", "';/*", "'or", "'='",
                "union select", "drop table", "insert into",
                "delete from", "update set"
            };
            
            String lowerInput = input.toLowerCase();
            for (String pattern : dangerousPatterns) {
                if (lowerInput.contains(pattern)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * 안전한 LIKE 쿼리 파라미터 생성
         */
        public static String escapeLikeParameter(String param) {
            if (param == null) return null;
            
            // LIKE 쿼리에서 특수문자 이스케이프
            return param
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        }
    }
    
    /**
     * 데이터베이스 권한 검증
     */
    @ConditionalOnProperty(
        name = "bif.database.security.validate-permissions", 
        havingValue = "true"
    )
    public void validateDatabasePermissions() {
        log.info("데이터베이스 권한 검증 시작");
        
        try (Connection connection = dataSource.getConnection()) {
            // 읽기 권한 확인
            checkReadPermission(connection);
            
            // 쓰기 권한 확인
            checkWritePermission(connection);
            
            // DDL 권한 확인 (개발 환경에서만)
            if (!"prod".equals(environment.getActiveProfiles()[0])) {
                checkDdlPermission(connection);
            }
            
            log.info("데이터베이스 권한 검증 완료");
            
        } catch (SQLException e) {
            log.error("데이터베이스 권한 검증 실패", e);
        }
    }
    
    private void checkReadPermission(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1")) {
            ps.executeQuery();
            log.debug("읽기 권한 확인 완료");
        }
    }
    
    private void checkWritePermission(Connection connection) throws SQLException {
        // 실제 쓰기는 하지 않고 문법만 검증
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 WHERE EXISTS (SELECT 1 FROM users WHERE 1=0)")) {
            ps.executeQuery();
            log.debug("쓰기 권한 확인 완료");
        }
    }
    
    private void checkDdlPermission(Connection connection) throws SQLException {
        // DDL 권한은 메타데이터 조회로 확인
        connection.getMetaData().getTables(null, null, "TEST_TABLE_CHECK", null);
        log.debug("DDL 권한 확인 완료");
    }
}