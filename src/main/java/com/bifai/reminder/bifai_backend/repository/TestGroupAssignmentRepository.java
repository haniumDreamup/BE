package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.*;
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
 * 테스트 그룹 할당 Repository
 */
@Repository
public interface TestGroupAssignmentRepository extends JpaRepository<TestGroupAssignment, Long> {
  
  /**
   * 사용자와 실험으로 할당 조회
   */
  Optional<TestGroupAssignment> findByUserAndExperiment(User user, Experiment experiment);
  
  /**
   * 사용자별 할당 조회
   */
  List<TestGroupAssignment> findByUser(User user);
  
  /**
   * 사용자의 활성 할당 조회
   */
  @Query("SELECT a FROM TestGroupAssignment a WHERE a.user.userId = :userId " +
         "AND a.isActive = true AND a.optedOut = false")
  List<TestGroupAssignment> findActiveAssignmentsByUserId(@Param("userId") Long userId);
  
  /**
   * 실험별 할당 조회
   */
  Page<TestGroupAssignment> findByExperiment(Experiment experiment, Pageable pageable);
  
  /**
   * 테스트 그룹별 할당 조회
   */
  List<TestGroupAssignment> findByTestGroup(TestGroup testGroup);
  
  /**
   * 변형별 할당 조회
   */
  List<TestGroupAssignment> findByVariant(TestVariant variant);
  
  /**
   * 전환 달성한 할당
   */
  @Query("SELECT a FROM TestGroupAssignment a WHERE a.experiment = :experiment " +
         "AND a.conversionAchieved = true")
  List<TestGroupAssignment> findConvertedAssignments(@Param("experiment") Experiment experiment);
  
  /**
   * 그룹별 전환 수 카운트
   */
  @Query("SELECT COUNT(a) FROM TestGroupAssignment a WHERE a.testGroup = :testGroup " +
         "AND a.conversionAchieved = true")
  Long countConversionsByTestGroup(@Param("testGroup") TestGroup testGroup);
  
  /**
   * 변형별 전환 수 카운트
   */
  @Query("SELECT COUNT(a) FROM TestGroupAssignment a WHERE a.variant = :variant " +
         "AND a.conversionAchieved = true")
  Long countConversionsByVariant(@Param("variant") TestVariant variant);
  
  /**
   * 노출 기간별 할당 조회
   */
  @Query("SELECT a FROM TestGroupAssignment a WHERE a.experiment = :experiment " +
         "AND a.firstExposureAt BETWEEN :startDate AND :endDate")
  List<TestGroupAssignment> findByExposurePeriod(
    @Param("experiment") Experiment experiment,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 최소 노출 횟수 이상 할당
   */
  @Query("SELECT a FROM TestGroupAssignment a WHERE a.experiment = :experiment " +
         "AND a.exposureCount >= :minExposures")
  List<TestGroupAssignment> findByMinExposures(
    @Param("experiment") Experiment experiment,
    @Param("minExposures") Integer minExposures);
  
  /**
   * 제외된 사용자 카운트
   */
  @Query("SELECT COUNT(a) FROM TestGroupAssignment a WHERE a.experiment = :experiment " +
         "AND a.optedOut = true")
  Long countOptedOutUsers(@Param("experiment") Experiment experiment);
  
  /**
   * 평균 전환 값
   */
  @Query("SELECT AVG(a.conversionValue) FROM TestGroupAssignment a " +
         "WHERE a.testGroup = :testGroup AND a.conversionAchieved = true")
  Double getAverageConversionValue(@Param("testGroup") TestGroup testGroup);
  
  /**
   * 만족도 점수 평균
   */
  @Query("SELECT AVG(a.satisfactionScore) FROM TestGroupAssignment a " +
         "WHERE a.experiment = :experiment AND a.satisfactionScore IS NOT NULL")
  Double getAverageSatisfactionScore(@Param("experiment") Experiment experiment);
  
  /**
   * 할당 해시로 조회
   */
  Optional<TestGroupAssignment> findByAssignmentHash(String assignmentHash);
}