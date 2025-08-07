package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.MovementPattern;
import com.bifai.reminder.bifai_backend.entity.MovementPattern.PatternType;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이동 패턴 레포지토리
 */
@Repository
public interface MovementPatternRepository extends JpaRepository<MovementPattern, Long> {

  /**
   * 사용자의 활성 패턴 조회
   */
  List<MovementPattern> findByUserAndIsActiveTrue(User user);

  /**
   * 사용자의 최근 패턴 조회
   */
  @Query("SELECT p FROM MovementPattern p WHERE p.user = :user " +
         "AND p.lastOccurred > :since " +
         "ORDER BY p.lastOccurred DESC")
  List<MovementPattern> findRecentPatternsByUser(
      @Param("user") User user,
      @Param("since") LocalDateTime since
  );

  /**
   * 특정 요일의 패턴 조회
   */
  List<MovementPattern> findByUserAndDayOfWeek(User user, String dayOfWeek);

  /**
   * 특정 시간대의 패턴 조회
   */
  List<MovementPattern> findByUserAndTimeOfDay(User user, String timeOfDay);

  /**
   * 패턴 타입별 조회
   */
  List<MovementPattern> findByUserAndPatternType(User user, PatternType patternType);

  /**
   * 높은 확신도 패턴 조회
   */
  @Query("SELECT p FROM MovementPattern p WHERE p.user = :user " +
         "AND p.confidenceScore >= :minConfidence " +
         "AND p.isActive = true " +
         "ORDER BY p.confidenceScore DESC")
  List<MovementPattern> findHighConfidencePatterns(
      @Param("user") User user,
      @Param("minConfidence") Float minConfidence
  );

  /**
   * 안전한 패턴 조회
   */
  List<MovementPattern> findByUserAndIsSafeTrue(User user);

  /**
   * 편차 경고가 설정된 패턴
   */
  List<MovementPattern> findByUserAndAlertOnDeviationTrue(User user);

  /**
   * 특정 목적의 패턴 조회
   */
  List<MovementPattern> findByUserAndPurpose(User user, String purpose);

  /**
   * 사용 빈도가 높은 패턴
   */
  @Query("SELECT p FROM MovementPattern p WHERE p.user = :user " +
         "AND p.occurrenceCount >= :minCount " +
         "ORDER BY p.occurrenceCount DESC")
  List<MovementPattern> findFrequentPatterns(
      @Param("user") User user,
      @Param("minCount") Integer minCount
  );
}