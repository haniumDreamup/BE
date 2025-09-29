package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Flyway 데이터베이스 마이그레이션 설정
 * BIF 서비스의 데이터베이스 스키마 버전 관리
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {
    
    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;
    
    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;
    
    @Value("${spring.flyway.baseline-version:1}")
    private String baselineVersion;
    
    @Value("${spring.flyway.validate-on-migrate:true}")
    private boolean validateOnMigrate;
    
    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;
    
    @Value("${spring.flyway.out-of-order:false}")
    private boolean outOfOrder;
    
    @Value("${spring.flyway.placeholder-replacement:true}")
    private boolean placeholderReplacement;
    
    /**
     * Flyway 설정 빈
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("Flyway 마이그레이션 설정 시작");
        
        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(validateOnMigrate)
                .cleanDisabled(cleanDisabled)
                .outOfOrder(outOfOrder)
                .encoding(StandardCharsets.UTF_8)
                .placeholderReplacement(placeholderReplacement);
        
        // 플레이스홀더 설정
        configuration.placeholders(java.util.Map.of(
                "schema_name", "bifai",
                "default_user", "bif_user",
                "table_prefix", "bif_"
        ));
        
        // 콜백 설정
        configuration.callbacks(new FlywayMigrationCallback());
        
        Flyway flyway = configuration.load();
        
        // 마이그레이션 실행
        int pendingMigrations = flyway.info().pending().length;
        log.info("대기 중인 마이그레이션: {} 개", pendingMigrations);
        
        if (pendingMigrations > 0) {
            int applied = flyway.migrate().migrationsExecuted;
            log.info("적용된 마이그레이션: {} 개", applied);
        } else {
            log.info("적용할 마이그레이션이 없습니다");
        }
        
        return flyway;
    }
    
    /**
     * Flyway 마이그레이션 콜백
     * 마이그레이션 전후 작업 처리
     */
    private static class FlywayMigrationCallback implements org.flywaydb.core.api.callback.Callback {
        
        @Override
        public boolean supports(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            return event == org.flywaydb.core.api.callback.Event.BEFORE_MIGRATE ||
                   event == org.flywaydb.core.api.callback.Event.AFTER_MIGRATE ||
                   event == org.flywaydb.core.api.callback.Event.AFTER_MIGRATE_ERROR;
        }
        
        @Override
        public boolean canHandleInTransaction(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            return true;
        }
        
        @Override
        public void handle(org.flywaydb.core.api.callback.Event event, org.flywaydb.core.api.callback.Context context) {
            switch (event) {
                case BEFORE_MIGRATE:
                    log.info("마이그레이션 시작 - 버전: {}", 
                        context.getMigrationInfo() != null ? context.getMigrationInfo().getVersion() : "unknown");
                    break;
                    
                case AFTER_MIGRATE:
                    log.info("마이그레이션 완료 - 버전: {}, 실행 시간: {}ms", 
                        context.getMigrationInfo() != null ? context.getMigrationInfo().getVersion() : "unknown",
                        context.getMigrationInfo() != null ? context.getMigrationInfo().getExecutionTime() : "unknown");
                    break;
                    
                case AFTER_MIGRATE_ERROR:
                    log.error("마이그레이션 실패 - 버전: {}", 
                        context.getMigrationInfo() != null ? context.getMigrationInfo().getVersion() : "unknown");
                    break;
                    
                default:
                    break;
            }
        }
        
        @Override
        public String getCallbackName() {
            return "BIF Migration Callback";
        }
    }
}