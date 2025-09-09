package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 지오펜스 레포지토리
 * 안전 구역 데이터 접근 계층
 */
@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

  /**
   * 사용자별 지오펜스 목록 조회
   */
  List<Geofence> findByUserOrderByPriorityDescCreatedAtDesc(User user);

  /**
   * 사용자별 활성화된 지오펜스 목록 조회
   */
  List<Geofence> findByUserAndIsActiveTrue(User user);

  /**
   * 사용자별 지오펜스 페이징 조회
   */
  Page<Geofence> findByUser(User user, Pageable pageable);

  /**
   * 사용자와 ID로 지오펜스 조회
   */
  Optional<Geofence> findByIdAndUser(Long id, User user);

  /**
   * 타입별 지오펜스 조회
   */
  List<Geofence> findByUserAndType(User user, Geofence.GeofenceType type);

  /**
   * 특정 좌표를 포함하는 활성화된 지오펜스 찾기
   */
  @Query("SELECT g FROM Geofence g WHERE g.user = :user AND g.isActive = true")
  List<Geofence> findActiveGeofencesByUser(@Param("user") User user);

  /**
   * 이름으로 지오펜스 존재 여부 확인
   */
  boolean existsByUserAndName(User user, String name);

  /**
   * 사용자별 지오펜스 개수
   */
  long countByUser(User user);

  /**
   * 사용자별 활성화된 지오펜스 개수
   */
  long countByUserAndIsActiveTrue(User user);

  /**
   * 위험 구역 조회
   */
  @Query("SELECT g FROM Geofence g WHERE g.user = :user " +
         "AND g.type = 'DANGER_ZONE' AND g.isActive = true")
  List<Geofence> findActiveDangerZones(@Param("user") User user);

  /**
   * 우선순위별 정렬된 활성 지오펜스
   */
  List<Geofence> findByUserAndIsActiveTrueOrderByPriorityDesc(User user);
  
  /**
   * 사용자 ID와 활성화 상태로 지오펜스 조회
   */
  List<Geofence> findByUserUserIdAndIsActive(Long userId, boolean isActive);
}