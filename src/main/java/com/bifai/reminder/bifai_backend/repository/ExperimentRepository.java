package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Experiment;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentStatus;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentType;
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
 * 실험 Repository
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
  
  /**
   * 실험 키로 조회
   */
  Optional<Experiment> findByExperimentKey(String experimentKey);
  
  /**
   * 실험 키와 상태로 조회
   */
  Optional<Experiment> findByExperimentKeyAndStatus(String experimentKey, ExperimentStatus status);
  
  /**
   * 활성 실험 조회
   */
  List<Experiment> findByIsActiveTrue();
  
  /**
   * 상태별 실험 조회
   */
  List<Experiment> findByStatus(ExperimentStatus status);
  
  /**
   * 타입별 실험 조회
   */
  List<Experiment> findByExperimentType(ExperimentType type);
  
  /**
   * 특정 기간 내 시작된 실험
   */
  @Query("SELECT e FROM Experiment e WHERE e.startDate BETWEEN :startDate AND :endDate")
  List<Experiment> findExperimentsStartedBetween(
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate);
  
  /**
   * 완료 예정 실험
   */
  @Query("SELECT e FROM Experiment e WHERE e.status = 'ACTIVE' AND e.endDate <= :date")
  List<Experiment> findExperimentsToComplete(@Param("date") LocalDateTime date);
  
  /**
   * 시작 예정 실험
   */
  @Query("SELECT e FROM Experiment e WHERE e.status = 'SCHEDULED' AND e.startDate <= :date")
  List<Experiment> findExperimentsToStart(@Param("date") LocalDateTime date);
  
  /**
   * 진행률별 실험 조회
   */
  @Query("SELECT e FROM Experiment e WHERE " +
         "(CAST(e.currentParticipants AS DOUBLE) / e.sampleSizeTarget) >= :minProgress")
  List<Experiment> findByMinProgress(@Param("minProgress") double minProgress);
  
  /**
   * 생성자별 실험 조회
   */
  Page<Experiment> findByCreatedBy(String createdBy, Pageable pageable);
  
  /**
   * 키워드 검색
   */
  @Query("SELECT e FROM Experiment e WHERE " +
         "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
         "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  Page<Experiment> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
  
  /**
   * 활성 실험 수 카운트
   */
  long countByIsActiveTrue();
  
  /**
   * 상태별 실험 수 카운트
   */
  long countByStatus(ExperimentStatus status);
}