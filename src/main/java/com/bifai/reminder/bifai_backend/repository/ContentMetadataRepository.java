package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ContentMetadata;
import com.bifai.reminder.bifai_backend.entity.CapturedImage;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 메타데이터 데이터 접근을 위한 Repository
 * BIF 사용자를 위한 이미지 품질 관리 및 메타데이터 분석 기능 포함
 */
@Repository
public interface ContentMetadataRepository extends JpaRepository<ContentMetadata, Long> {

    /**
     * 특정 이미지의 메타데이터 조회
     */
    Optional<ContentMetadata> findByCapturedImage(CapturedImage capturedImage);

    /**
     * 사용자의 모든 메타데이터 조회
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findByUser(@Param("user") User user);

    /**
     * 추출 상태별 메타데이터 조회
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.extractionStatus = :status ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findByUserAndExtractionStatus(
            @Param("user") User user,
            @Param("status") ContentMetadata.ExtractionStatus status);

    /**
     * 추출 대기 중인 메타데이터 조회 (처리 대상)
     */
    @Query("SELECT cm FROM ContentMetadata cm WHERE cm.extractionStatus = 'PENDING' ORDER BY cm.createdAt ASC")
    List<ContentMetadata> findPendingExtractions();

    /**
     * 추출 실패한 메타데이터 조회 (재처리 대상)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.extractionStatus = 'FAILED' ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findFailedExtractionsByUser(@Param("user") User user);

    /**
     * 좋은 품질의 이미지 메타데이터 조회 (품질 점수 70 이상)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.qualityScore >= 70 ORDER BY cm.qualityScore DESC")
    List<ContentMetadata> findGoodQualityMetadataByUser(@Param("user") User user);

    /**
     * 낮은 품질의 이미지 메타데이터 조회 (품질 점수 50 미만)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.qualityScore < 50 ORDER BY cm.qualityScore ASC")
    List<ContentMetadata> findPoorQualityMetadataByUser(@Param("user") User user);

    /**
     * 흐린 이미지 메타데이터 조회 (흐림 정도 0.7 이상)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.blurLevel >= 0.7 ORDER BY cm.blurLevel DESC")
    List<ContentMetadata> findBlurryImagesByUser(@Param("user") User user);

    /**
     * 어두운 이미지 메타데이터 조회 (밝기 0.3 미만)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.brightnessLevel < 0.3 ORDER BY cm.brightnessLevel ASC")
    List<ContentMetadata> findDarkImagesByUser(@Param("user") User user);

    /**
     * 너무 밝은 이미지 메타데이터 조회 (밝기 0.9 초과)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.brightnessLevel > 0.9 ORDER BY cm.brightnessLevel DESC")
    List<ContentMetadata> findOverbrightImagesByUser(@Param("user") User user);

    /**
     * 특정 기기 모델의 메타데이터 조회 (기기별 품질 분석용)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.deviceModel = :deviceModel ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findByUserAndDeviceModel(
            @Param("user") User user,
            @Param("deviceModel") String deviceModel);

    /**
     * GPS 정보가 있는 메타데이터 조회
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.hasGpsInfo = true ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findWithGpsInfoByUser(@Param("user") User user);

    /**
     * 플래시 사용한 이미지 메타데이터 조회
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.flashUsed = true ORDER BY cm.createdAt DESC")
    List<ContentMetadata> findFlashUsedByUser(@Param("user") User user);

    /**
     * 사용자별 평균 이미지 품질 점수 조회
     */
    @Query("SELECT AVG(cm.qualityScore) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.qualityScore IS NOT NULL")
    Optional<Double> getAverageQualityScoreByUser(@Param("user") User user);

    /**
     * 사용자별 평균 흐림 정도 조회
     */
    @Query("SELECT AVG(cm.blurLevel) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.blurLevel IS NOT NULL")
    Optional<Double> getAverageBlurLevelByUser(@Param("user") User user);

    /**
     * 사용자별 평균 밝기 수준 조회
     */
    @Query("SELECT AVG(cm.brightnessLevel) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.brightnessLevel IS NOT NULL")
    Optional<Double> getAverageBrightnessLevelByUser(@Param("user") User user);

    /**
     * 기기 모델별 품질 통계 조회
     */
    @Query("SELECT cm.deviceModel, COUNT(cm), AVG(cm.qualityScore) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.deviceModel IS NOT NULL AND cm.qualityScore IS NOT NULL GROUP BY cm.deviceModel")
    List<Object[]> getQualityStatsByDeviceModel(@Param("user") User user);

    /**
     * 월별 이미지 품질 통계 조회
     */
    @Query("SELECT YEAR(cm.createdAt), MONTH(cm.createdAt), AVG(cm.qualityScore), COUNT(cm) FROM ContentMetadata cm JOIN cm.capturedImage ci " +
           "WHERE ci.user = :user AND cm.qualityScore IS NOT NULL " +
           "GROUP BY YEAR(cm.createdAt), MONTH(cm.createdAt) " +
           "ORDER BY YEAR(cm.createdAt) DESC, MONTH(cm.createdAt) DESC")
    List<Object[]> getMonthlyQualityStatsByUser(@Param("user") User user);

    /**
     * 오늘 추출된 메타데이터 개수 조회
     */
    @Query("SELECT COUNT(cm) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.createdAt >= :startOfDay AND cm.createdAt < :endOfDay AND cm.extractionStatus = 'COMPLETED'")
    long countTodayMetadataByUser(@Param("user") User user, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 품질 개선이 필요한 이미지 개수 조회 (BIF 사용자 가이드용)
     */
    @Query("SELECT COUNT(cm) FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND (cm.qualityScore < 50 OR cm.blurLevel >= 0.7 OR cm.brightnessLevel < 0.3 OR cm.brightnessLevel > 0.9)")
    long countImagesNeedingQualityImprovement(@Param("user") User user);

    /**
     * 최근 품질이 좋은 이미지들 조회 (BIF 사용자 격려용)
     */
    @Query("SELECT cm FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user AND cm.qualityScore >= 80 AND cm.createdAt >= :sinceDate ORDER BY cm.qualityScore DESC, cm.createdAt DESC")
    List<ContentMetadata> findRecentHighQualityImages(@Param("user") User user, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * 사용자별 촬영 기술 개선 통계 (시간별 품질 변화)
     */
    @Query("SELECT FUNCTION('DATE', cm.createdAt), AVG(cm.qualityScore) FROM ContentMetadata cm JOIN cm.capturedImage ci " +
           "WHERE ci.user = :user AND cm.qualityScore IS NOT NULL AND cm.createdAt >= :sinceDate " +
           "GROUP BY FUNCTION('DATE', cm.createdAt) " +
           "ORDER BY FUNCTION('DATE', cm.createdAt) DESC")
    List<Object[]> getQualityTrendByUser(@Param("user") User user, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * 메타데이터 추출 성공률 조회 (전체)
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN cm.extractionStatus = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(cm) " +
           "FROM ContentMetadata cm")
    Optional<Double> getExtractionSuccessRate();

    /**
     * 사용자별 메타데이터 추출 성공률 조회
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN cm.extractionStatus = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(cm) " +
           "FROM ContentMetadata cm JOIN cm.capturedImage ci WHERE ci.user = :user")
    Optional<Double> getExtractionSuccessRateByUser(@Param("user") User user);
} 