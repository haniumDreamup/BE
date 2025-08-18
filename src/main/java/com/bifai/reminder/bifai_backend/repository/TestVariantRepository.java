package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Experiment;
import com.bifai.reminder.bifai_backend.entity.TestVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테스트 변형 Repository
 */
@Repository
public interface TestVariantRepository extends JpaRepository<TestVariant, Long> {
  
  /**
   * 실험별 변형 조회
   */
  List<TestVariant> findByExperiment(Experiment experiment);
  
  /**
   * 실험별 활성 변형 조회
   */
  List<TestVariant> findByExperimentAndIsActiveTrue(Experiment experiment);
  
  /**
   * 실험과 변형 키로 조회
   */
  Optional<TestVariant> findByExperimentAndVariantKey(Experiment experiment, String variantKey);
  
  /**
   * 대조군 변형 조회
   */
  Optional<TestVariant> findByExperimentAndIsControlTrue(Experiment experiment);
  
  /**
   * 승자 변형 조회
   */
  Optional<TestVariant> findByExperimentAndIsWinnerTrue(Experiment experiment);
  
  /**
   * 통계적으로 유의한 변형 조회
   */
  @Query("SELECT v FROM TestVariant v WHERE v.experiment = :experiment " +
         "AND v.pValue < 0.05")
  List<TestVariant> findStatisticallySignificantVariants(@Param("experiment") Experiment experiment);
  
  /**
   * 최고 전환율 변형 조회
   */
  @Query("SELECT v FROM TestVariant v WHERE v.experiment = :experiment " +
         "ORDER BY v.conversionRate DESC")
  List<TestVariant> findTopPerformingVariants(@Param("experiment") Experiment experiment);
  
  /**
   * 성능 점수별 순위
   */
  @Query("SELECT v FROM TestVariant v WHERE v.experiment = :experiment " +
         "ORDER BY (COALESCE(v.conversionRate, 0) * 0.4 + " +
         "COALESCE(v.engagementScore, 0) * 0.3 + " +
         "(100 - COALESCE(v.errorRate, 100)) * 0.2 + " +
         "LEAST(COALESCE(v.avgSessionDuration, 0) / 60, 100) * 0.1) DESC")
  List<TestVariant> findByPerformanceScore(@Param("experiment") Experiment experiment);
  
  /**
   * Feature Flag 포함 변형 조회
   * JSON_CONTAINS_PATH는 MySQL 전용이므로 주석 처리
   */
  // @Query("SELECT v FROM TestVariant v WHERE v.experiment = :experiment " +
  //        "AND JSON_CONTAINS_PATH(v.featureFlags, 'one', :flagKey) = 1")
  // List<TestVariant> findVariantsWithFeatureFlag(
  //   @Param("experiment") Experiment experiment,
  //   @Param("flagKey") String flagKey);
}