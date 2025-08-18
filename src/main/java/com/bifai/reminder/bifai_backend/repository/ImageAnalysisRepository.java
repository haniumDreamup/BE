package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ImageAnalysis;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.AnalysisStatus;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.AnalysisType;
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
 * 이미지 분석 리포지토리
 */
@Repository
public interface ImageAnalysisRepository extends JpaRepository<ImageAnalysis, Long> {

  /**
   * 사용자별 이미지 분석 조회
   */
  Page<ImageAnalysis> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  /**
   * 사용자별 특정 타입 분석 조회
   */
  List<ImageAnalysis> findByUserAndAnalysisType(User user, AnalysisType type);

  /**
   * 사용자별 특정 상태 분석 조회
   */
  List<ImageAnalysis> findByUserAndAnalysisStatus(User user, AnalysisStatus status);

  /**
   * 긴급 상황 분석 조회
   */
  @Query("SELECT ia FROM ImageAnalysis ia WHERE ia.user = :user AND ia.emergencyDetected = true " +
         "AND ia.createdAt >= :since ORDER BY ia.createdAt DESC")
  List<ImageAnalysis> findRecentEmergencies(@Param("user") User user, 
                                           @Param("since") LocalDateTime since);

  /**
   * 처리 중인 분석 조회
   */
  @Query("SELECT ia FROM ImageAnalysis ia WHERE ia.analysisStatus = 'PROCESSING' " +
         "AND ia.createdAt < :timeout")
  List<ImageAnalysis> findStuckProcessing(@Param("timeout") LocalDateTime timeout);

  /**
   * 사용자와 ID로 분석 조회 (보안)
   */
  Optional<ImageAnalysis> findByIdAndUser(Long id, User user);

  /**
   * 특정 기간 내 분석 개수
   */
  @Query("SELECT COUNT(ia) FROM ImageAnalysis ia WHERE ia.user = :user " +
         "AND ia.createdAt BETWEEN :start AND :end")
  Long countAnalysesByUserAndPeriod(@Param("user") User user,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

  /**
   * 위치 기반 분석 조회
   */
  @Query("SELECT ia FROM ImageAnalysis ia WHERE ia.user = :user " +
         "AND ia.latitude BETWEEN :minLat AND :maxLat " +
         "AND ia.longitude BETWEEN :minLon AND :maxLon " +
         "ORDER BY ia.createdAt DESC")
  List<ImageAnalysis> findByUserAndLocation(@Param("user") User user,
                                           @Param("minLat") Double minLat,
                                           @Param("maxLat") Double maxLat,
                                           @Param("minLon") Double minLon,
                                           @Param("maxLon") Double maxLon);

  /**
   * 최근 성공한 분석 조회
   */
  @Query("SELECT ia FROM ImageAnalysis ia WHERE ia.user = :user " +
         "AND ia.analysisStatus = 'COMPLETED' ORDER BY ia.analyzedAt DESC")
  List<ImageAnalysis> findRecentCompleted(@Param("user") User user, Pageable pageable);
}