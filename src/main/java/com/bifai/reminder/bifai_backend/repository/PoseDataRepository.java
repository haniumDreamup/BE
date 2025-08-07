package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.PoseData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PoseData Repository
 */
@Repository
public interface PoseDataRepository extends JpaRepository<PoseData, Long> {
  
  /**
   * 특정 사용자의 최근 Pose 데이터 조회
   */
  List<PoseData> findByUserIdAndTimestampBetweenOrderByTimestampDesc(
      Long userId, LocalDateTime start, LocalDateTime end);
  
  /**
   * 세션별 Pose 데이터 조회
   */
  List<PoseData> findByPoseSessionIdOrderByTimestamp(Long sessionId);
  
  /**
   * 특정 시간 범위의 Pose 데이터 개수
   */
  long countByUserIdAndTimestampBetween(
      Long userId, LocalDateTime start, LocalDateTime end);
  
  /**
   * 최근 N개의 Pose 데이터 조회
   */
  @Query("SELECT p FROM PoseData p WHERE p.user.id = :userId ORDER BY p.timestamp DESC")
  List<PoseData> findRecentByUserId(@Param("userId") Long userId, 
                                    org.springframework.data.domain.Pageable pageable);
  
  /**
   * 이전 프레임 데이터 조회 (속도 계산용)
   */
  @Query("SELECT p FROM PoseData p WHERE p.user.id = :userId " +
         "AND p.timestamp < :currentTime " +
         "ORDER BY p.timestamp DESC")
  List<PoseData> findPreviousFrame(@Param("userId") Long userId,
                                  @Param("currentTime") LocalDateTime currentTime,
                                  org.springframework.data.domain.Pageable pageable);
}