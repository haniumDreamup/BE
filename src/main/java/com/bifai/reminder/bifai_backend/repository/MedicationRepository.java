package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MedicationRepository - BIF User Medication Management Repository
 * 
 * BIF 사용자의 복약 정보를 관리하는 리포지토리입니다.
 * 약물 관리, 복약 일정, 우선순위 관리 등의 기능을 제공합니다.
 */
@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    // 기본 조회 메서드
    @EntityGraph(attributePaths = {"user"})
    List<Medication> findByUserOrderByPriorityLevelDescCreatedAtDesc(User user);
    
    @EntityGraph(attributePaths = {"user"})
    Page<Medication> findByUserOrderByPriorityLevelDescCreatedAtDesc(User user, Pageable pageable);
    
    List<Medication> findByUser_UserIdOrderByPriorityLevelDescCreatedAtDesc(Long userId);
    
    // 활성 약물 조회
    List<Medication> findByUserAndIsActiveTrueOrderByPriorityLevelDescCreatedAtDesc(User user);
    
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true AND m.medicationStatus = 'ACTIVE' " +
           "ORDER BY m.priorityLevel DESC, m.createdAt DESC")
    List<Medication> findActiveEnabledMedicationsByUser(@Param("user") User user);
    
    // 상태별 조회
    List<Medication> findByUserAndMedicationStatusOrderByPriorityLevelDescCreatedAtDesc(
        User user, Medication.MedicationStatus medicationStatus);
    
    // 우선순위별 조회
    List<Medication> findByUserAndPriorityLevelOrderByCreatedAtDesc(
        User user, Medication.PriorityLevel priorityLevel);
    
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.priorityLevel IN ('CRITICAL', 'HIGH') " +
           "AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC, m.createdAt DESC")
    List<Medication> findHighPriorityMedicationsByUser(@Param("user") User user);
    
    // 약물 유형별 조회
    List<Medication> findByUserAndMedicationTypeOrderByPriorityLevelDescCreatedAtDesc(
        User user, Medication.MedicationType medicationType);
    
    // 복용 시간별 조회
    @Query("SELECT m FROM Medication m JOIN m.intakeTimes it " +
           "WHERE m.user = :user AND m.isActive = true " +
           "AND it BETWEEN :startTime AND :endTime " +
           "ORDER BY it, m.priorityLevel DESC")
    List<Medication> findByUserAndIntakeTimeRange(
        @Param("user") User user,
        @Param("startTime") java.time.LocalTime startTime,
        @Param("endTime") java.time.LocalTime endTime);
    
    // 오늘 복용해야 할 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true AND m.medicationStatus = 'ACTIVE' " +
           "AND (m.endDate IS NULL OR m.endDate >= CURRENT_DATE) " +
           "AND m.startDate <= CURRENT_DATE " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findTodayMedicationsByUser(@Param("user") User user);
    
    // 만료 예정 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true " +
           "AND m.endDate IS NOT NULL " +
           "AND m.endDate BETWEEN CURRENT_DATE AND :endDate " +
           "ORDER BY m.endDate, m.priorityLevel DESC")
    List<Medication> findExpiringMedicationsByUser(
        @Param("user") User user, @Param("endDate") LocalDate endDate);
    
    // 만료된 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.endDate IS NOT NULL AND m.endDate < CURRENT_DATE " +
           "ORDER BY m.endDate DESC")
    List<Medication> findExpiredMedicationsByUser(@Param("user") User user);
    
    // 보호자 알림이 필요한 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND (m.guardianAlertNeeded = true OR m.priorityLevel = 'CRITICAL') " +
           "AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC, m.createdAt DESC")
    List<Medication> findMedicationsNeedingGuardianAlert(@Param("user") User user);
    
    // 약물명으로 검색
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND (LOWER(m.medicationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY m.priorityLevel DESC, m.createdAt DESC")
    List<Medication> findByUserAndMedicationNameContaining(
        @Param("user") User user, @Param("searchTerm") String searchTerm);
    
    // 의사별 처방 약물
    List<Medication> findByUserAndPrescribingDoctorOrderByPriorityLevelDescCreatedAtDesc(
        User user, String prescribingDoctor);
    
    // 복용량별 조회
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.dailyFrequency = :frequency " +
           "AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findByUserAndDailyFrequency(
        @Param("user") User user, @Param("frequency") Integer frequency);
    
    // 제형별 조회
    List<Medication> findByUserAndDosageFormOrderByPriorityLevelDescCreatedAtDesc(
        User user, Medication.DosageForm dosageForm);
    
    // 복용 시점별 조회
    List<Medication> findByUserAndTimingInstructionOrderByPriorityLevelDescCreatedAtDesc(
        User user, Medication.TimingInstruction timingInstruction);
    
    // 통계 및 분석 쿼리들
    @Query("SELECT m.medicationType, COUNT(m) FROM Medication m " +
           "WHERE m.user = :user AND m.isActive = true " +
           "GROUP BY m.medicationType ORDER BY COUNT(m) DESC")
    List<Object[]> getMedicationCountByType(@Param("user") User user);
    
    @Query("SELECT m.priorityLevel, COUNT(m) FROM Medication m " +
           "WHERE m.user = :user AND m.isActive = true " +
           "GROUP BY m.priorityLevel")
    List<Object[]> getMedicationCountByPriority(@Param("user") User user);
    
    @Query("SELECT m.dosageForm, COUNT(m) FROM Medication m " +
           "WHERE m.user = :user AND m.isActive = true " +
           "GROUP BY m.dosageForm")
    List<Object[]> getMedicationCountByDosageForm(@Param("user") User user);
    
    @Query("SELECT m.dailyFrequency, COUNT(m), SUM(m.dailyFrequency) FROM Medication m " +
           "WHERE m.user = :user AND m.isActive = true " +
           "GROUP BY m.dailyFrequency ORDER BY m.dailyFrequency")
    List<Object[]> getDailyFrequencyStatistics(@Param("user") User user);
    
    // 특정 날짜 범위의 활성 약물
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true " +
           "AND m.startDate <= :endDate " +
           "AND (m.endDate IS NULL OR m.endDate >= :startDate) " +
           "ORDER BY m.priorityLevel DESC, m.startDate")
    List<Medication> findActiveMedicationsInDateRange(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // 처방전 번호로 조회
    Optional<Medication> findByUserAndPrescriptionNumber(User user, String prescriptionNumber);
    
    // 약국별 처방 약물
    List<Medication> findByUserAndPharmacyNameOrderByCreatedAtDesc(User user, String pharmacyName);
    
    // 부작용 정보가 있는 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.sideEffects IS NOT NULL AND m.sideEffects != '' " +
           "AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findMedicationsWithSideEffects(@Param("user") User user);
    
    // 중요한 주의사항이 있는 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.importantNotes IS NOT NULL AND m.importantNotes != '' " +
           "AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findMedicationsWithImportantNotes(@Param("user") User user);
    
    // 알코올 금지 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.avoidAlcohol = true AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findAlcoholRestrictedMedications(@Param("user") User user);
    
    // 음식과 함께 복용해야 하는 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.requiresFood = true AND m.isActive = true " +
           "ORDER BY m.priorityLevel DESC")
    List<Medication> findMedicationsRequiringFood(@Param("user") User user);
    
    // 일시 중단된 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.medicationStatus = 'PAUSED' " +
           "ORDER BY m.updatedAt DESC")
    List<Medication> findPausedMedicationsByUser(@Param("user") User user);
    
    // 재고 없는 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.medicationStatus = 'OUT_OF_STOCK' " +
           "ORDER BY m.priorityLevel DESC, m.updatedAt DESC")
    List<Medication> findOutOfStockMedicationsByUser(@Param("user") User user);
    
    // 최근 추가된 약물들
    @Query("SELECT m FROM Medication m WHERE m.user = :user " +
           "AND m.createdAt >= :since " +
           "ORDER BY m.createdAt DESC")
    List<Medication> findRecentlyAddedMedications(
        @Param("user") User user, @Param("since") java.time.LocalDateTime since);
    
    // 사용자별 총 활성 약물 개수
    @Query("SELECT COUNT(m) FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true AND m.medicationStatus = 'ACTIVE'")
    Long countActiveMedicationsByUser(@Param("user") User user);
    
    // 사용자별 하루 총 복용 횟수
    @Query("SELECT SUM(m.dailyFrequency) FROM Medication m WHERE m.user = :user " +
           "AND m.isActive = true AND m.medicationStatus = 'ACTIVE'")
    Long getTotalDailyDosesByUser(@Param("user") User user);
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 사용자별 약물 목록 조회
     */
    List<Medication> findByUser(User user);
    
    /**
     * 활성 약물만 조회
     */
    List<Medication> findByUserAndIsActiveTrue(User user);
    
    /**
     * 종료일이 가까운 약물 조회
     */
    @Query("SELECT m FROM Medication m WHERE m.endDate <= :threshold AND m.isActive = true")
    List<Medication> findMedicationsEndingSoon(@Param("threshold") LocalDate threshold);
    
    /**
     * 처방의별 약물 조회
     */
    List<Medication> findByUserAndPrescribingDoctor(User user, String prescribingDoctor);
    
    /**
     * 특정 기간 시작된 약물 조회
     */
    List<Medication> findByUserAndStartDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    /**
     * 약국별 약물 조회
     */
    List<Medication> findByUserAndPharmacyName(User user, String pharmacyName);
    
    /**
     * 부작용 있는 약물 조회
     */
    List<Medication> findByUserAndSideEffectsIsNotNull(User user);
    
    /**
     * 약물 이름으로 검색
     */
    @Query("SELECT m FROM Medication m WHERE m.user = :user AND m.medicationName LIKE %:name%")
    List<Medication> searchByNameContaining(@Param("user") User user, @Param("name") String name);
    
    /**
     * 특정 시간에 복용해야 할 약물 조회 (NotificationScheduler에서 사용)
     */
    @Query("SELECT m FROM Medication m JOIN m.intakeTimes it " +
           "WHERE m.isActive = true AND m.medicationStatus = 'ACTIVE' " +
           "AND it = :time ORDER BY m.priorityLevel DESC")
    List<Medication> findByScheduleTime(@Param("time") java.time.LocalTime time);
} 