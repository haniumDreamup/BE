package com.bifai.reminder.bifai_backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 데이터소스 및 커넥션 풀 최적화 설정
 * 100+ 동시 사용자 지원을 위한 HikariCP 튜닝
 */
@Configuration
@Slf4j
public class DataSourceConfig {
  
  @Value("${spring.datasource.url}")
  private String jdbcUrl;
  
  @Value("${spring.datasource.username}")
  private String username;
  
  @Value("${spring.datasource.password}")
  private String password;
  
  @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
  private String driverClassName;
  
  /**
   * 최적화된 HikariCP 데이터소스
   */
  @Bean
  @Primary
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    
    // 기본 연결 설정
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driverClassName);
    
    // 커넥션 풀 크기 설정 (100명 동시 사용자 기준)
    config.setMaximumPoolSize(30); // 최대 커넥션 수
    config.setMinimumIdle(10); // 최소 유휴 커넥션
    config.setIdleTimeout(300000); // 5분 (유휴 커넥션 타임아웃)
    config.setMaxLifetime(1800000); // 30분 (커넥션 최대 수명)
    config.setConnectionTimeout(20000); // 20초 (커넥션 획득 타임아웃)
    
    // 커넥션 검증
    config.setConnectionTestQuery("SELECT 1");
    config.setValidationTimeout(5000); // 5초
    
    // 성능 최적화 설정
    config.setAutoCommit(false); // 명시적 트랜잭션 관리
    config.setPoolName("BifAI-HikariCP");
    config.setRegisterMbeans(true); // JMX 모니터링 활성화
    
    // 커넥션 풀 동작 최적화
    config.setLeakDetectionThreshold(60000); // 1분 (커넥션 누수 감지)
    config.setIsolateInternalQueries(true); // 내부 쿼리 격리
    
    // MySQL 특화 설정
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("useLocalSessionState", "true");
    config.addDataSourceProperty("rewriteBatchedStatements", "true");
    config.addDataSourceProperty("cacheResultSetMetadata", "true");
    config.addDataSourceProperty("cacheServerConfiguration", "true");
    config.addDataSourceProperty("elideSetAutoCommits", "true");
    config.addDataSourceProperty("maintainTimeStats", "false");
    
    // 성능 관련 추가 설정
    config.addDataSourceProperty("useSSL", "false"); // SSL 비활성화 (내부망인 경우)
    config.addDataSourceProperty("useUnicode", "true");
    config.addDataSourceProperty("characterEncoding", "UTF-8");
    config.addDataSourceProperty("serverTimezone", "Asia/Seoul");
    
    log.info("HikariCP 커넥션 풀 설정 완료 - 최대: {}, 최소: {}", 
      config.getMaximumPoolSize(), config.getMinimumIdle());
    
    return new HikariDataSource(config);
  }
  
  /**
   * 읽기 전용 데이터소스 (선택적)
   */
  @Bean
  @ConditionalOnProperty(name = "spring.datasource.read-only.enabled", havingValue = "true")
  public DataSource readOnlyDataSource(
      @Value("${spring.datasource.read-only.url}") String readOnlyUrl) {
    
    HikariConfig config = new HikariConfig();
    
    config.setJdbcUrl(readOnlyUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driverClassName);
    
    // 읽기 전용은 더 많은 커넥션 허용
    config.setMaximumPoolSize(40);
    config.setMinimumIdle(15);
    config.setReadOnly(true);
    config.setAutoCommit(true);
    config.setPoolName("BifAI-ReadOnly-HikariCP");
    
    // 읽기 최적화
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "500");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    
    log.info("읽기 전용 HikariCP 커넥션 풀 설정 완료");
    
    return new HikariDataSource(config);
  }
}