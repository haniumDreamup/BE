package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.entity.InteractionPattern.PatternType;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 인터랙션 패턴 Repository
 */
@Repository
public interface InteractionPatternRepository extends JpaRepository<InteractionPattern, Long> {
  
  /**
   * 사용자별 최근 패턴 조회
   */
  List<InteractionPattern> findByUserAndAnalysisDateAfterOrderByAnalysisDateDesc(
    User user, LocalDateTime date);
  
  /**
   * 사용자와 패턴 타입으로 조회
   */
  List<InteractionPattern> findByUserAndPatternTypeOrderByAnalysisDateDesc(
    User user, PatternType patternType);
  
  /**
   * 이상 패턴만 조회
   */
  @Query("SELECT ip FROM InteractionPattern ip WHERE ip.user = :user AND ip.isAnomaly = true " +
         "AND ip.analysisDate BETWEEN :startDate AND :endDate ORDER BY ip.anomalyScore DESC")
  List<InteractionPattern> findAnomalousPatterns(
    @Param("user") User user,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 패턴 타입별 평균 메트릭 계산
   */
  @Query("SELECT AVG(ip.clickFrequency) as avgClickFreq, " +
         "AVG(ip.avgSessionDuration) as avgSessionDur, " +
         "AVG(ip.errorRate) as avgErrorRate, " +
         "STDDEV(ip.clickFrequency) as stdClickFreq, " +
         "STDDEV(ip.avgSessionDuration) as stdSessionDur, " +
         "STDDEV(ip.errorRate) as stdErrorRate " +
         "FROM InteractionPattern ip " +
         "WHERE ip.user = :user AND ip.patternType = :patternType " +
         "AND ip.analysisDate BETWEEN :startDate AND :endDate")
  Object[] calculateBaselineMetrics(
    @Param("user") User user,
    @Param("patternType") PatternType patternType,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 사용자의 최근 패턴 페이징 조회
   */
  Page<InteractionPattern> findByUserOrderByAnalysisDateDesc(User user, Pageable pageable);
  
  /**
   * 특정 기간의 패턴 조회
   */
  @Query("SELECT ip FROM InteractionPattern ip WHERE ip.user = :user " +
         "AND ip.timeWindowStart >= :startDate AND ip.timeWindowEnd <= :endDate " +
         "ORDER BY ip.analysisDate DESC")
  List<InteractionPattern> findPatternsInTimeWindow(
    @Param("user") User user,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 가장 최근 분석 패턴 조회
   */
  Optional<InteractionPattern> findTopByUserAndPatternTypeOrderByAnalysisDateDesc(
    User user, PatternType patternType);
  
  /**
   * 이상 패턴 개수 카운트
   */
  @Query("SELECT COUNT(ip) FROM InteractionPattern ip WHERE ip.user = :user " +
         "AND ip.isAnomaly = true AND ip.analysisDate >= :since")
  Long countAnomalousPatterns(@Param("user") User user, @Param("since") LocalDateTime since);
  
  /**
   * 패턴 타입별 통계 조회
   */
  @Query("SELECT ip.patternType, COUNT(ip), AVG(ip.anomalyScore) " +
         "FROM InteractionPattern ip WHERE ip.user = :user " +
         "AND ip.analysisDate BETWEEN :startDate AND :endDate " +
         "GROUP BY ip.patternType")
  List<Object[]> getPatternStatistics(
    @Param("user") User user,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 높은 신뢰도 패턴 조회
   */
  @Query("SELECT ip FROM InteractionPattern ip WHERE ip.user = :user " +
         "AND ip.confidenceLevel >= :minConfidence " +
         "ORDER BY ip.confidenceLevel DESC, ip.analysisDate DESC")
  List<InteractionPattern> findHighConfidencePatterns(
    @Param("user") User user,
    @Param("minConfidence") Double minConfidence);
  
  /**
   * 배치 처리를 위한 벌크 삭제
   */
  void deleteByAnalysisDateBefore(LocalDateTime date);
}