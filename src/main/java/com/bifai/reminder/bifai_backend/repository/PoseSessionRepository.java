package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.PoseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * PoseSession Repository
 */
@Repository
public interface PoseSessionRepository extends JpaRepository<PoseSession, Long> {
  
  /**
   * 세션 ID로 조회
   */
  Optional<PoseSession> findBySessionId(String sessionId);
  
  /**
   * 활성 세션 조회
   */
  Optional<PoseSession> findByUserIdAndStatus(Long userId, PoseSession.SessionStatus status);
  
  /**
   * 특정 기간의 세션 조회
   */
  List<PoseSession> findByUserIdAndStartTimeBetween(
      Long userId, LocalDateTime start, LocalDateTime end);
  
  /**
   * 최근 세션 조회
   */
  @Query("SELECT s FROM PoseSession s WHERE s.user.id = :userId " +
         "ORDER BY s.startTime DESC")
  List<PoseSession> findRecentSessions(@Param("userId") Long userId,
                                       org.springframework.data.domain.Pageable pageable);
}