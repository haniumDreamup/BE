package com.bifai.reminder.bifai_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

/**
 * JPA EntityManager 명시적 설정
 * Spring Boot 3.x에서 엔티티 스캔 문제 해결을 위한 설정
 */
@Configuration
public class JpaEntityManagerConfig {
    
    @Autowired
    private DataSource dataSource;
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.bifai.reminder.bifai_backend.entity")
                .persistenceUnit("default")
                .build();
    }
    
    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        adapter.setGenerateDdl(false);
        return adapter;
    }
}