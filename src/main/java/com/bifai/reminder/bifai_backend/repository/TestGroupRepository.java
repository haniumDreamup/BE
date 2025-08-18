package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Experiment;
import com.bifai.reminder.bifai_backend.entity.TestGroup;
import com.bifai.reminder.bifai_backend.entity.TestGroup.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 테스트 그룹 Repository
 */
@Repository
public interface TestGroupRepository extends JpaRepository<TestGroup, Long> {
  
  /**
   * 실험별 그룹 조회
   */
  List<TestGroup> findByExperiment(Experiment experiment);
  
  /**
   * 실험별 활성 그룹 조회
   */
  List<TestGroup> findByExperimentAndIsActiveTrue(Experiment experiment);
  
  /**
   * 실험과 그룹명으로 조회
   */
  Optional<TestGroup> findByExperimentAndGroupName(Experiment experiment, String groupName);
  
  /**
   * 대조군 조회
   */
  Optional<TestGroup> findByExperimentAndIsControlTrue(Experiment experiment);
  
  /**
   * 그룹 타입별 조회
   */
  List<TestGroup> findByGroupType(GroupType groupType);
  
  /**
   * 가득 차지 않은 그룹 조회
   */
  @Query("SELECT g FROM TestGroup g WHERE g.experiment = :experiment " +
         "AND g.isActive = true " +
         "AND (g.maxSampleSize IS NULL OR g.currentSize < g.maxSampleSize)")
  List<TestGroup> findAvailableGroups(@Param("experiment") Experiment experiment);
  
  /**
   * 최소 샘플 크기 달성한 그룹
   */
  @Query("SELECT g FROM TestGroup g WHERE g.experiment = :experiment " +
         "AND (g.minSampleSize IS NULL OR g.currentSize >= g.minSampleSize)")
  List<TestGroup> findGroupsWithMinimumSample(@Param("experiment") Experiment experiment);
  
  /**
   * 그룹 활용률 통계
   */
  @Query("SELECT g.groupName, " +
         "CAST(g.currentSize AS DOUBLE) / g.maxSampleSize * 100 as utilization " +
         "FROM TestGroup g WHERE g.experiment = :experiment")
  List<Object[]> getGroupUtilization(@Param("experiment") Experiment experiment);
  
  /**
   * 실험별 총 참여자 수
   */
  @Query("SELECT SUM(g.currentSize) FROM TestGroup g WHERE g.experiment = :experiment")
  Long getTotalParticipants(@Param("experiment") Experiment experiment);
}