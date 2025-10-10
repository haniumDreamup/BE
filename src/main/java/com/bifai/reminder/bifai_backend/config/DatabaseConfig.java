package com.bifai.reminder.bifai_backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 데이터베이스 설정
 * HikariCP를 사용한 고성능 커넥션 풀 구성
 * BIF 서비스의 안정적인 데이터베이스 연결 관리
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.H2Dialect}")
    private String hibernateDialect;

    @Value("${spring.jpa.show-sql:false}")
    private String showSql;

    @Value("${spring.jpa.properties.hibernate.format_sql:true}")
    private String formatSql;

    /**
     * HikariCP DataSource 설정
     * BIF 서비스를 위한 최적화된 커넥션 풀 구성
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        
        // 기본 데이터베이스 연결 정보
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUsername);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setDriverClassName(driverClassName);
        
        // 커넥션 풀 설정
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        
        // 성능 최적화 설정
        hikariConfig.setPoolName("BifHikariCP");
        hikariConfig.setRegisterMbeans(true);
        hikariConfig.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // 커넥션 테스트 쿼리 (데이터베이스별로 다름)
        if (driverClassName.contains("h2")) {
            hikariConfig.setConnectionTestQuery("SELECT 1");
        } else if (driverClassName.contains("mysql")) {
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
            hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        } else if (driverClassName.contains("postgresql")) {
            hikariConfig.setConnectionTestQuery("SELECT 1");
            hikariConfig.addDataSourceProperty("prepareThreshold", "3");
            hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "250");
            hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        }
        
        log.info("HikariCP 데이터소스 초기화 - URL: {}, Pool Name: {}", dbUrl, "BifHikariCP");
        
        return new HikariDataSource(hikariConfig);
    }

    /**
     * JPA EntityManagerFactory 설정
     * Hibernate 최적화 설정 포함
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bifai.reminder.bifai_backend.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties jpaProperties = new Properties();

        // Hibernate 기본 설정
        jpaProperties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        jpaProperties.setProperty("hibernate.dialect", hibernateDialect);
        jpaProperties.setProperty("hibernate.show_sql", showSql);
        jpaProperties.setProperty("hibernate.format_sql", formatSql);

        log.info("Hibernate 설정 - DDL Auto: {}, Dialect: {}", ddlAuto, hibernateDialect);
        
        // 성능 최적화 설정
        jpaProperties.setProperty("hibernate.jdbc.batch_size", "25");
        jpaProperties.setProperty("hibernate.order_inserts", "true");
        jpaProperties.setProperty("hibernate.order_updates", "true");
        jpaProperties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        
        // 2차 캐시 설정 (추후 Redis 연동 가능)
        jpaProperties.setProperty("hibernate.cache.use_second_level_cache", "false");
        jpaProperties.setProperty("hibernate.cache.use_query_cache", "false");
        
        // 통계 및 모니터링
        jpaProperties.setProperty("hibernate.generate_statistics", "${hibernate.generate_statistics:false}");
        jpaProperties.setProperty("hibernate.session.events.log", "false");
        
        // 쿼리 플랜 캐시
        jpaProperties.setProperty("hibernate.query.plan_cache_max_size", "2048");
        jpaProperties.setProperty("hibernate.query.plan_parameter_metadata_max_size", "128");
        
        em.setJpaProperties(jpaProperties);
        
        return em;
    }

    /**
     * 트랜잭션 매니저 설정
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    /**
     * 데이터베이스 상태 로깅
     * 애플리케이션 시작 시 커넥션 풀 상태 출력
     */
    @Bean
    @ConditionalOnProperty(name = "database.config.log-startup-info", havingValue = "true", matchIfMissing = true)
    public DatabaseStartupLogger databaseStartupLogger(DataSource dataSource) {
        return new DatabaseStartupLogger(dataSource);
    }

    /**
     * 데이터베이스 시작 로거
     */
    public static class DatabaseStartupLogger {
        public DatabaseStartupLogger(DataSource dataSource) {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                log.info("===== BIF 데이터베이스 연결 정보 =====");
                log.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
                log.info("풀 이름: {}", hikariDataSource.getPoolName());
                log.info("최대 풀 크기: {}", hikariDataSource.getMaximumPoolSize());
                log.info("최소 유휴 연결: {}", hikariDataSource.getMinimumIdle());
                log.info("연결 타임아웃: {}ms", hikariDataSource.getConnectionTimeout());
                log.info("유휴 타임아웃: {}ms", hikariDataSource.getIdleTimeout());
                log.info("최대 수명: {}ms", hikariDataSource.getMaxLifetime());
                log.info("누수 감지 임계값: {}ms", hikariDataSource.getLeakDetectionThreshold());
                log.info("=====================================");
            }
        }
    }
}