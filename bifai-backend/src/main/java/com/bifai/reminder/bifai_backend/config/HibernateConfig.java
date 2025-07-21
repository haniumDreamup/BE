package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.config.database.QueryPerformanceInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.util.Map;

/**
 * Hibernate 최적화 설정
 * BIF 서비스를 위한 고성능 ORM 구성
 */
@Configuration
public class HibernateConfig {
    
    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:25}")
    private int batchSize;
    
    @Value("${spring.jpa.properties.hibernate.jdbc.fetch_size:100}")
    private int fetchSize;
    
    @Value("${spring.jpa.properties.hibernate.query.plan_cache_max_size:2048}")
    private int planCacheSize;
    
    @Value("${spring.jpa.properties.hibernate.query.plan_parameter_metadata_max_size:128}")
    private int planParameterMetadataSize;
    
    @Autowired(required = false)
    private QueryPerformanceInterceptor queryPerformanceInterceptor;
    
    /**
     * Hibernate 속성 커스터마이저
     * 성능 최적화를 위한 추가 설정
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // 배치 처리 최적화
            hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, batchSize);
            hibernateProperties.put(AvailableSettings.BATCH_VERSIONED_DATA, "true");
            hibernateProperties.put(AvailableSettings.ORDER_INSERTS, "true");
            hibernateProperties.put(AvailableSettings.ORDER_UPDATES, "true");
            
            // 페치 최적화
            hibernateProperties.put(AvailableSettings.STATEMENT_FETCH_SIZE, fetchSize);
            hibernateProperties.put(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 16);
            
            // 쿼리 플랜 캐시
            hibernateProperties.put(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, planCacheSize);
            hibernateProperties.put(AvailableSettings.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, planParameterMetadataSize);
            
            // 2차 레벨 캐시 (추후 Redis 연동 시 활성화)
            hibernateProperties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false");
            hibernateProperties.put(AvailableSettings.USE_QUERY_CACHE, "false");
            
            // 지연 로딩 최적화
            hibernateProperties.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "false");
            // DEFAULT_ENTITY_MODE는 Hibernate 6에서 제거됨
            
            // 통계 수집 (개발/테스트 환경에서만 활성화)
            hibernateProperties.put(AvailableSettings.GENERATE_STATISTICS, 
                "${hibernate.generate_statistics:false}");
            
            // ID 생성 최적화
            // USE_NEW_ID_GENERATOR_MAPPINGS는 Hibernate 6에서 제거됨 (기본값이 true)
            hibernateProperties.put(AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "pooled-lo");
            
            // 쿼리 인터셉터 설정
            if (queryPerformanceInterceptor != null) {
                hibernateProperties.put(AvailableSettings.STATEMENT_INSPECTOR, queryPerformanceInterceptor);
            }
            
            // 기타 성능 최적화
            // USE_REFLECTION_OPTIMIZER는 Hibernate 6에서 제거됨
            hibernateProperties.put(AvailableSettings.USE_GET_GENERATED_KEYS, "true");
            hibernateProperties.put(AvailableSettings.STATEMENT_FETCH_SIZE, fetchSize);
            
            // BIF 서비스 특화 설정
            configureBifSpecificSettings(hibernateProperties);
        };
    }
    
    /**
     * BIF 서비스 특화 Hibernate 설정
     */
    private void configureBifSpecificSettings(Map<String, Object> properties) {
        // 위치 기반 쿼리를 위한 공간 데이터 지원 (추후 확장 가능)
        properties.put("hibernate.spatial.dialect", "org.hibernate.spatial.dialect.mysql.MySQL8SpatialDialect");
        
        // 다국어 지원을 위한 설정
        properties.put("hibernate.jdbc.charset", "UTF-8");
        properties.put("hibernate.connection.characterEncoding", "UTF-8");
        properties.put("hibernate.connection.useUnicode", "true");
        
        // 시간대 설정 (한국 시간 기준)
        properties.put("hibernate.jdbc.time_zone", "Asia/Seoul");
        
        // 자동 스키마 검증
        properties.put("hibernate.hbm2ddl.auto", "${spring.jpa.hibernate.ddl-auto:validate}");
        
        // 쿼리 주석 추가 (디버깅용)
        properties.put(AvailableSettings.USE_SQL_COMMENTS, 
            "${hibernate.use_sql_comments:false}");
    }
    
    /**
     * JPA 설정 후처리기
     * EntityManagerFactory 생성 후 추가 설정
     */
    @Bean
    public EntityManagerFactoryPostProcessor entityManagerFactoryPostProcessor() {
        return new EntityManagerFactoryPostProcessor();
    }
    
    /**
     * EntityManagerFactory 후처리기
     */
    public static class EntityManagerFactoryPostProcessor {
        public void postProcess(LocalContainerEntityManagerFactoryBean factory) {
            // 추가 설정이 필요한 경우 여기에 구현
        }
    }
}