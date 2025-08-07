package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.FallEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FallEvent Repository
 */
@Repository
public interface FallEventRepository extends JpaRepository<FallEvent, Long> {
  
  /**
   * 사용자별 최근 낙상 이벤트 조회
   */
  List<FallEvent> findByUserIdOrderByDetectedAtDesc(Long userId);
  
  /**
   * 특정 기간의 낙상 이벤트 조회
   */
  List<FallEvent> findByUserIdAndDetectedAtBetween(
      Long userId, LocalDateTime start, LocalDateTime end);
  
  /**
   * 미해결 낙상 이벤트 조회
   */
  List<FallEvent> findByUserIdAndStatus(Long userId, FallEvent.EventStatus status);
  
  /**
   * 심각도별 낙상 이벤트 개수
   */
  @Query("SELECT f.severity, COUNT(f) FROM FallEvent f " +
         "WHERE f.user.id = :userId AND f.detectedAt BETWEEN :start AND :end " +
         "GROUP BY f.severity")
  List<Object[]> countBySeverity(@Param("userId") Long userId,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
  
  /**
   * 오탐지가 아닌 낙상 이벤트 조회
   */
  @Query("SELECT f FROM FallEvent f WHERE f.user.id = :userId " +
         "AND (f.falsePositive IS NULL OR f.falsePositive = false) " +
         "ORDER BY f.detectedAt DESC")
  List<FallEvent> findValidFallEvents(@Param("userId") Long userId);
  
  /**
   * 특정 시간 이후의 낙상 이벤트 조회 (중복 체크용)
   */
  List<FallEvent> findByUserIdAndDetectedAtAfter(Long userId, LocalDateTime after);
}