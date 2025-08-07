package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.AnalysisResult;
import com.bifai.reminder.bifai_backend.entity.CapturedImage;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 분석 결과 데이터 접근을 위한 Repository
 * BIF 사용자를 위한 분석 결과 조회 및 통계 기능 포함
 */
@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    /**
     * 특정 이미지의 분석 결과 조회
     */
    Optional<AnalysisResult> findByCapturedImage(CapturedImage capturedImage);

    /**
     * 사용자의 모든 분석 결과 조회 (페이징)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user ORDER BY ar.analyzedAt DESC")
    Page<AnalysisResult> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 특정 분석 타입 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analysisType = :analysisType ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findByUserAndAnalysisType(
            @Param("user") User user,
            @Param("analysisType") AnalysisResult.AnalysisType analysisType);

    /**
     * 높은 신뢰도 분석 결과 조회 (0.8 이상)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.confidenceScore >= 0.8 AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findHighConfidenceResultsByUser(@Param("user") User user);

    /**
     * 낮은 신뢰도 분석 결과 조회 (검토 필요, 0.5 미만)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.confidenceScore < 0.5 AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findLowConfidenceResultsByUser(@Param("user") User user);

    /**
     * 분석 상태별 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.status = :status ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findByUserAndStatus(
            @Param("user") User user,
            @Param("status") AnalysisResult.AnalysisStatus status);

    /**
     * 날짜 범위별 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analyzedAt BETWEEN :startDate AND :endDate ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findByUserAndAnalyzedAtBetween(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 약물 인식 결과만 조회 (BIF 사용자의 복약 관리용)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analysisType = 'MEDICATION_RECOGNITION' AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findMedicationAnalysesByUser(@Param("user") User user);

    /**
     * 음식 분석 결과만 조회 (BIF 사용자의 식단 관리용)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analysisType = 'FOOD_ANALYSIS' AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findFoodAnalysesByUser(@Param("user") User user);

    /**
     * 응급상황 감지 결과 조회 (우선순위 처리용)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analysisType = 'EMERGENCY_DETECTION' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findEmergencyDetectionsByUser(@Param("user") User user);

    /**
     * 검토가 필요한 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.status = 'REQUIRES_REVIEW' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findResultsRequiringReviewByUser(@Param("user") User user);

    /**
     * 최근 완료된 분석 결과 조회 (최신 N개)
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findRecentCompletedResultsByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 모델 버전의 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.modelVersion = :modelVersion ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findByUserAndModelVersion(
            @Param("user") User user,
            @Param("modelVersion") String modelVersion);

    /**
     * 평균 신뢰도 점수 조회 (사용자별)
     */
    @Query("SELECT AVG(ar.confidenceScore) FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.status = 'COMPLETED' AND ar.confidenceScore IS NOT NULL")
    Optional<BigDecimal> getAverageConfidenceScoreByUser(@Param("user") User user);

    /**
     * 분석 타입별 개수 통계 (사용자별)
     */
    @Query("SELECT ar.analysisType, COUNT(ar) FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.status = 'COMPLETED' GROUP BY ar.analysisType")
    List<Object[]> getAnalysisTypeStatsByUser(@Param("user") User user);

    /**
     * 월별 분석 결과 개수 통계
     */
    @Query("SELECT YEAR(ar.analyzedAt), MONTH(ar.analyzedAt), COUNT(ar) FROM AnalysisResult ar JOIN ar.capturedImage ci " +
           "WHERE ci.user = :user AND ar.status = 'COMPLETED' " +
           "GROUP BY YEAR(ar.analyzedAt), MONTH(ar.analyzedAt) " +
           "ORDER BY YEAR(ar.analyzedAt) DESC, MONTH(ar.analyzedAt) DESC")
    List<Object[]> getMonthlyAnalysisStatsByUser(@Param("user") User user);

    /**
     * 평균 처리 시간 조회 (성능 모니터링용)
     */
    @Query("SELECT AVG(ar.processingTimeMs) FROM AnalysisResult ar WHERE ar.status = 'COMPLETED' AND ar.processingTimeMs IS NOT NULL")
    Optional<Double> getAverageProcessingTime();

    /**
     * 분석 타입별 평균 처리 시간 조회
     */
    @Query("SELECT ar.analysisType, AVG(ar.processingTimeMs) FROM AnalysisResult ar WHERE ar.status = 'COMPLETED' AND ar.processingTimeMs IS NOT NULL GROUP BY ar.analysisType")
    List<Object[]> getAverageProcessingTimeByType();

    /**
     * 실패한 분석 결과 조회 (오류 분석용)
     */
    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.status = 'FAILED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findFailedAnalyses(Pageable pageable);

    /**
     * 오늘의 분석 결과 개수 조회 (사용자별)
     */
    @Query("SELECT COUNT(ar) FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.analyzedAt >= :startOfDay AND ar.analyzedAt < :endOfDay AND ar.status = 'COMPLETED'")
    long countTodayAnalysesByUser(@Param("user") User user, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 사용자의 권장 행동이 있는 분석 결과 조회
     */
    @Query("SELECT ar FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.recommendedAction IS NOT NULL AND ar.status = 'COMPLETED' ORDER BY ar.analyzedAt DESC")
    List<AnalysisResult> findResultsWithRecommendationsByUser(@Param("user") User user);

    /**
     * 특정 신뢰도 범위의 분석 결과 개수 조회
     */
    @Query("SELECT COUNT(ar) FROM AnalysisResult ar JOIN ar.capturedImage ci WHERE ci.user = :user AND ar.confidenceScore BETWEEN :minConfidence AND :maxConfidence AND ar.status = 'COMPLETED'")
    long countByUserAndConfidenceRange(
            @Param("user") User user,
            @Param("minConfidence") BigDecimal minConfidence,
            @Param("maxConfidence") BigDecimal maxConfidence);
} 