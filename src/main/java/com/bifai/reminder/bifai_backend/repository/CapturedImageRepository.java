package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.CapturedImage;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BIF 사용자의 촬영 이미지 데이터 접근을 위한 Repository
 * soft delete를 고려한 조회 메서드들과 BIF 특화 기능 포함
 */
@Repository
public interface CapturedImageRepository extends JpaRepository<CapturedImage, Long> {

    /**
     * soft delete되지 않은 이미지 조회
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.deleted = false AND ci.id = :id")
    Optional<CapturedImage> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 사용자의 삭제되지 않은 이미지 목록 조회 (페이징)
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    Page<CapturedImage> findByUserAndNotDeleted(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 특정 타입 이미지 목록 조회
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.imageType = :imageType AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    Page<CapturedImage> findByUserAndImageTypeAndNotDeleted(
            @Param("user") User user, 
            @Param("imageType") CapturedImage.ImageType imageType, 
            Pageable pageable);

    /**
     * 날짜 범위로 이미지 조회
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.capturedAt BETWEEN :startDate AND :endDate AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findByUserAndCapturedAtBetweenAndNotDeleted(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 처리 상태별 이미지 조회
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.processingStatus = :status AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findByUserAndProcessingStatusAndNotDeleted(
            @Param("user") User user,
            @Param("status") CapturedImage.ProcessingStatus status);

    /**
     * 오늘 촬영한 이미지 개수 조회 (BIF 사용자의 일일 활동 추적용)
     */
    @Query("SELECT COUNT(ci) FROM CapturedImage ci WHERE ci.user = :user AND ci.capturedAt >= :startOfDay AND ci.capturedAt < :endOfDay AND ci.deleted = false")
    long countTodayImagesByUser(@Param("user") User user, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 최근 N일간 이미지 개수 조회
     */
    @Query("SELECT COUNT(ci) FROM CapturedImage ci WHERE ci.user = :user AND ci.capturedAt >= :sinceDate AND ci.deleted = false")
    long countRecentImagesByUser(@Param("user") User user, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * 특정 이미지 타입의 최근 이미지 조회 (BIF 사용자의 패턴 분석용)
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.imageType = :imageType AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findTopByUserAndImageTypeAndNotDeleted(
            @Param("user") User user,
            @Param("imageType") CapturedImage.ImageType imageType,
            Pageable pageable);

    /**
     * 처리 완료된 이미지 중 가장 최근 것 조회
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.processingStatus = 'COMPLETED' AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findLatestProcessedImageByUser(@Param("user") User user, Pageable pageable);

    /**
     * 응급상황 이미지 조회 (우선순위 처리용)
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.imageType = 'EMERGENCY' AND ci.deleted = false ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findEmergencyImagesByUser(@Param("user") User user);

    /**
     * 특정 위치 반경 내의 이미지 조회 (GPS 활용)
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.user = :user AND ci.latitude IS NOT NULL AND ci.longitude IS NOT NULL AND ci.deleted = false " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(ci.latitude)) * " +
           "cos(radians(ci.longitude) - radians(:longitude)) + " +
           "sin(radians(:latitude)) * sin(radians(ci.latitude)))) <= :radiusKm " +
           "ORDER BY ci.capturedAt DESC")
    List<CapturedImage> findByUserAndLocationWithinRadius(
            @Param("user") User user,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm);

    /**
     * 이미지 soft delete 처리
     */
    @Modifying
    @Query("UPDATE CapturedImage ci SET ci.deleted = true, ci.deletedAt = CURRENT_TIMESTAMP WHERE ci.id = :id")
    void softDeleteById(@Param("id") Long id);

    /**
     * 사용자의 모든 이미지 soft delete 처리 (계정 삭제 시)
     */
    @Modifying
    @Query("UPDATE CapturedImage ci SET ci.deleted = true, ci.deletedAt = CURRENT_TIMESTAMP WHERE ci.user = :user")
    void softDeleteAllByUser(@Param("user") User user);

    /**
     * 사용자별 이미지 타입 통계 조회 (BIF 사용자의 활동 패턴 분석용)
     */
    @Query("SELECT ci.imageType, COUNT(ci) FROM CapturedImage ci WHERE ci.user = :user AND ci.deleted = false GROUP BY ci.imageType")
    List<Object[]> getImageTypeStatsByUser(@Param("user") User user);

    /**
     * 월별 이미지 촬영 개수 통계 (BIF 사용자의 장기 활동 추이)
     */
    @Query("SELECT YEAR(ci.capturedAt), MONTH(ci.capturedAt), COUNT(ci) FROM CapturedImage ci " +
           "WHERE ci.user = :user AND ci.deleted = false " +
           "GROUP BY YEAR(ci.capturedAt), MONTH(ci.capturedAt) " +
           "ORDER BY YEAR(ci.capturedAt) DESC, MONTH(ci.capturedAt) DESC")
    List<Object[]> getMonthlyImageStatsByUser(@Param("user") User user);

    /**
     * 오래된 삭제된 이미지 조회 (물리적 삭제 대상)
     */
    @Query("SELECT ci FROM CapturedImage ci WHERE ci.deleted = true AND ci.deletedAt < :cutoffDate")
    List<CapturedImage> findOldDeletedImages(@Param("cutoffDate") LocalDateTime cutoffDate);
} 