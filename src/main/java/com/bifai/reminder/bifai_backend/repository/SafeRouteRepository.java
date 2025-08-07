package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.SafeRoute;
import com.bifai.reminder.bifai_backend.entity.SafeRoute.DifficultyLevel;
import com.bifai.reminder.bifai_backend.entity.SafeRoute.RouteType;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 안전 경로 레포지토리
 */
@Repository
public interface SafeRouteRepository extends JpaRepository<SafeRoute, Long> {

  /**
   * 사용자의 활성 경로 조회
   */
  List<SafeRoute> findByUserAndIsActiveTrue(User user);

  /**
   * 사용자와 경로 타입별 조회
   */
  List<SafeRoute> findByUserAndRouteTypeAndIsActiveTrue(User user, RouteType routeType);

  /**
   * 주 경로 조회
   */
  List<SafeRoute> findByUserAndIsPrimaryTrue(User user);

  /**
   * 검증된 경로 조회
   */
  List<SafeRoute> findByUserAndValidatedTrue(User user);

  /**
   * 난이도별 경로 조회
   */
  List<SafeRoute> findByUserAndDifficultyLevel(User user, DifficultyLevel difficultyLevel);

  /**
   * 안전도가 높은 경로 조회
   */
  @Query("SELECT r FROM SafeRoute r WHERE r.user = :user " +
         "AND r.safetyScore >= :minScore " +
         "AND r.isActive = true " +
         "ORDER BY r.safetyScore DESC")
  List<SafeRoute> findSafeRoutes(
      @Param("user") User user,
      @Param("minScore") Float minScore
  );

  /**
   * 야간 안전 경로 조회
   */
  @Query("SELECT r FROM SafeRoute r WHERE r.user = :user " +
         "AND r.wellLit = true " +
         "AND r.safetyScore >= 0.7 " +
         "AND r.isActive = true")
  List<SafeRoute> findNightSafeRoutes(@Param("user") User user);

  /**
   * 자주 사용하는 경로 조회
   */
  @Query("SELECT r FROM SafeRoute r WHERE r.user = :user " +
         "AND r.usageCount > 0 " +
         "ORDER BY r.usageCount DESC, r.lastUsed DESC")
  List<SafeRoute> findFrequentlyUsedRoutes(@Param("user") User user);

  /**
   * 특정 목적지로의 경로 확인
   */
  @Query("SELECT r FROM SafeRoute r WHERE r.user = :user " +
         "AND r.endLatitude = :lat AND r.endLongitude = :lon " +
         "AND r.isActive = true")
  List<SafeRoute> findByDestination(
      @Param("user") User user,
      @Param("lat") Double latitude,
      @Param("lon") Double longitude
  );

  /**
   * 경로 타입별 통계
   */
  @Query("SELECT r.routeType, COUNT(r) FROM SafeRoute r " +
         "WHERE r.user = :user GROUP BY r.routeType")
  List<Object[]> getRouteTypeStatistics(@Param("user") User user);
}