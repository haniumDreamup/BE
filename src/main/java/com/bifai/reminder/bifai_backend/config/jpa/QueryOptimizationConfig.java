package com.bifai.reminder.bifai_backend.config.jpa;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA 쿼리 최적화 설정
 * 김영한님의 JPA 베스트 프랙티스 적용
 */
@Configuration
public class QueryOptimizationConfig {

  /**
   * Hibernate 배치 처리 및 쿼리 최적화 설정
   */
  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
    return hibernateProperties -> {
      // N+1 문제 해결을 위한 배치 페치 설정
      hibernateProperties.put("hibernate.default_batch_fetch_size", "100");
      
      // IN 절 파라미터 패딩 최적화 (2의 제곱수로 패딩)
      hibernateProperties.put("hibernate.query.in_clause_parameter_padding", "true");
      
      // 배치 INSERT/UPDATE 최적화
      hibernateProperties.put("hibernate.jdbc.batch_size", "20");
      hibernateProperties.put("hibernate.order_inserts", "true");
      hibernateProperties.put("hibernate.order_updates", "true");
      
      // 2차 캐시 설정 (필요시 활성화)
      // hibernateProperties.put("hibernate.cache.use_second_level_cache", "true");
      // hibernateProperties.put("hibernate.cache.use_query_cache", "true");
      
      // 통계 정보 (개발 환경에서만 사용)
      hibernateProperties.put("hibernate.generate_statistics", "false");
      
      // 쿼리 실행 계획 캐시 크기
      hibernateProperties.put("hibernate.query.plan_cache_max_size", "2048");
      hibernateProperties.put("hibernate.query.plan_parameter_metadata_max_size", "128");
    };
  }
}