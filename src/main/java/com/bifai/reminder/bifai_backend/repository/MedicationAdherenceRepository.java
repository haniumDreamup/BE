package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.entity.MedicationAdherence;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MedicationAdherenceRepository - BIF User Medication Compliance Repository
 * 
 * BIF 사용자의 복약 순응도를 추적하는 리포지토리입니다.
 * 복약 기록, 순응률 계산, 패턴 분석 등의 기능을 제공합니다.
 */
@Repository
public interface MedicationAdherenceRepository extends JpaRepository<MedicationAdherence, Long> {

    // 기본 조회 메서드
    List<MedicationAdherence> findByUserOrderByAdherenceDateDescScheduledTimeDesc(User user);
    
    Page<MedicationAdherence> findByUserOrderByAdherenceDateDescScheduledTimeDesc(User user, Pageable pageable);
    
    List<MedicationAdherence> findByUser_UserIdOrderByAdherenceDateDescScheduledTimeDesc(Long userId);
    
    // 특정 약물의 복약 기록
    List<MedicationAdherence> findByUserAndMedicationOrderByAdherenceDateDescScheduledTimeDesc(
        User user, Medication medication);
    
    List<MedicationAdherence> findByMedicationIdOrderByAdherenceDateDescScheduledTimeDesc(Long medicationId);
    
    // 날짜별 조회
    List<MedicationAdherence> findByUserAndAdherenceDateOrderByScheduledTime(
        User user, LocalDate adherenceDate);
    
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceDate >= :startDate AND ma.adherenceDate <= :endDate " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findByUserAndAdherenceDateBetween(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // 오늘의 복약 기록
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceDate = CURRENT_DATE " +
           "ORDER BY ma.scheduledTime")
    List<MedicationAdherence> findTodayAdherenceByUser(@Param("user") User user);
    
    // 순응 상태별 조회
    List<MedicationAdherence> findByUserAndAdherenceStatusOrderByAdherenceDateDescScheduledTimeDesc(
        User user, MedicationAdherence.AdherenceStatus adherenceStatus);
    
    // 성공적인 복약 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findSuccessfulAdherenceByUser(@Param("user") User user);
    
    // 실패한 복약 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus IN ('SKIPPED', 'MISSED') " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findFailedAdherenceByUser(@Param("user") User user);
    
    // 늦게 복용한 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus = 'TAKEN_LATE' " +
           "AND ma.adherenceDate >= :startDate " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findLateAdherenceByUser(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 보호자 알림이 필요한 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND (ma.adherenceStatus IN ('MISSED', 'SKIPPED') OR ma.sideEffectReported = true " +
           "OR ma.delayMinutes > 180) " +
           "AND ma.guardianNotified = false " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findAdherenceNeedingGuardianAttention(@Param("user") User user);
    
    // 부작용 보고가 있는 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.sideEffectReported = true " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findAdherenceWithSideEffects(@Param("user") User user);
    
    // 순응률 계산 관련 쿼리들
    @Query("SELECT COUNT(ma) FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') " +
           "AND ma.adherenceDate >= :startDate AND ma.adherenceDate <= :endDate")
    Long countSuccessfulAdherence(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(ma) FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceDate >= :startDate AND ma.adherenceDate <= :endDate")
    Long countTotalAdherence(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // 특정 약물의 순응률
    @Query("SELECT COUNT(ma) FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.medication = :medication " +
           "AND ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') " +
           "AND ma.adherenceDate >= :startDate")
    Long countSuccessfulAdherenceByMedication(
        @Param("user") User user,
        @Param("medication") Medication medication,
        @Param("startDate") LocalDate startDate);
    
    @Query("SELECT COUNT(ma) FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.medication = :medication " +
           "AND ma.adherenceDate >= :startDate")
    Long countTotalAdherenceByMedication(
        @Param("user") User user,
        @Param("medication") Medication medication,
        @Param("startDate") LocalDate startDate);
    
    // 월별 순응률 통계
    @Query("SELECT YEAR(ma.adherenceDate), MONTH(ma.adherenceDate), " +
           "COUNT(ma), " +
           "SUM(CASE WHEN ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 ELSE 0 END) " +
           "FROM MedicationAdherence ma WHERE ma.user = :user " +
           "GROUP BY YEAR(ma.adherenceDate), MONTH(ma.adherenceDate) " +
           "ORDER BY YEAR(ma.adherenceDate), MONTH(ma.adherenceDate)")
    List<Object[]> getMonthlyAdherenceStatistics(@Param("user") User user);
    
    // 약물별 순응률 통계
    @Query("SELECT m.medicationName, COUNT(ma), " +
           "SUM(CASE WHEN ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 ELSE 0 END) " +
           "FROM MedicationAdherence ma JOIN ma.medication m " +
           "WHERE ma.user = :user AND ma.adherenceDate >= :startDate " +
           "GROUP BY m.id, m.medicationName " +
           "ORDER BY COUNT(ma) DESC")
    List<Object[]> getAdherenceStatisticsByMedication(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 지연 시간 통계
    @Query("SELECT AVG(ma.delayMinutes), MAX(ma.delayMinutes), MIN(ma.delayMinutes) " +
           "FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.delayMinutes IS NOT NULL " +
           "AND ma.adherenceDate >= :startDate")
    List<Object[]> getDelayStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 건너뛴 이유별 통계
    @Query("SELECT ma.skipReason, COUNT(ma) FROM MedicationAdherence ma " +
           "WHERE ma.user = :user AND ma.skipReason IS NOT NULL " +
           "AND ma.adherenceDate >= :startDate " +
           "GROUP BY ma.skipReason ORDER BY COUNT(ma) DESC")
    List<Object[]> getSkipReasonStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 난이도 점수 통계
    @Query("SELECT AVG(ma.difficultyScore), COUNT(ma) FROM MedicationAdherence ma " +
           "WHERE ma.user = :user AND ma.difficultyScore IS NOT NULL " +
           "AND ma.adherenceDate >= :startDate")
    List<Object[]> getDifficultyStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 만족도 점수 통계
    @Query("SELECT AVG(ma.satisfactionScore), COUNT(ma) FROM MedicationAdherence ma " +
           "WHERE ma.user = :user AND ma.satisfactionScore IS NOT NULL " +
           "AND ma.adherenceDate >= :startDate")
    List<Object[]> getSatisfactionStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 시간대별 복약 패턴
    @Query("SELECT HOUR(ma.scheduledTime), COUNT(ma), " +
           "SUM(CASE WHEN ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 ELSE 0 END) " +
           "FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceDate >= :startDate " +
           "GROUP BY HOUR(ma.scheduledTime) " +
           "ORDER BY HOUR(ma.scheduledTime)")
    List<Object[]> getAdherencePatternByHour(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 요일별 복약 패턴
    @Query("SELECT DAYOFWEEK(ma.adherenceDate), COUNT(ma), " +
           "SUM(CASE WHEN ma.adherenceStatus IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 ELSE 0 END) " +
           "FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceDate >= :startDate " +
           "GROUP BY DAYOFWEEK(ma.adherenceDate) " +
           "ORDER BY DAYOFWEEK(ma.adherenceDate)")
    List<Object[]> getAdherencePatternByDayOfWeek(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 예정된 복약 (아직 복용하지 않은)
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus = 'SCHEDULED' " +
           "AND ma.adherenceDate = CURRENT_DATE " +
           "ORDER BY ma.scheduledTime")
    List<MedicationAdherence> findScheduledAdherenceToday(@Param("user") User user);
    
    // 놓친 복약들 (예정 시간이 지났는데 복용하지 않은)
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "AND ma.adherenceStatus = 'SCHEDULED' " +
           "AND (ma.adherenceDate < CURRENT_DATE OR " +
           "(ma.adherenceDate = CURRENT_DATE AND ma.scheduledTime < CURRENT_TIME)) " +
           "ORDER BY ma.adherenceDate DESC, ma.scheduledTime DESC")
    List<MedicationAdherence> findMissedAdherence(@Param("user") User user);
    
    // 알림 횟수 통계
    @Query("SELECT AVG(ma.reminderCount), MAX(ma.reminderCount) FROM MedicationAdherence ma " +
           "WHERE ma.user = :user AND ma.reminderCount > 0 " +
           "AND ma.adherenceDate >= :startDate")
    List<Object[]> getReminderStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 확인 방법별 통계
    @Query("SELECT ma.confirmationMethod, COUNT(ma) FROM MedicationAdherence ma " +
           "WHERE ma.user = :user AND ma.confirmationMethod IS NOT NULL " +
           "AND ma.adherenceDate >= :startDate " +
           "GROUP BY ma.confirmationMethod ORDER BY COUNT(ma) DESC")
    List<Object[]> getConfirmationMethodStatistics(
        @Param("user") User user, @Param("startDate") LocalDate startDate);
    
    // 특정 기간의 연속 성공/실패 분석
    @Query("SELECT ma.adherenceDate, ma.adherenceStatus FROM MedicationAdherence ma " +
           "WHERE ma.user = :user " +
           "AND ma.adherenceDate >= :startDate AND ma.adherenceDate <= :endDate " +
           "ORDER BY ma.adherenceDate, ma.scheduledTime")
    List<Object[]> getAdherenceSequence(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    // 동기화 상태 관련
    List<MedicationAdherence> findByUserAndSyncStatus(User user, String syncStatus);
    
    @Query("SELECT COUNT(ma) FROM MedicationAdherence ma WHERE ma.user = :user AND ma.syncStatus = 'PENDING'")
    Long countPendingSyncByUser(@Param("user") User user);
    
    // 특정 날짜와 시간의 복약 기록 조회
    Optional<MedicationAdherence> findByUserAndMedicationAndAdherenceDateAndScheduledTime(
        User user, Medication medication, LocalDate adherenceDate, java.time.LocalTime scheduledTime);
    
    // 최근 복약 기록들
    @Query("SELECT ma FROM MedicationAdherence ma WHERE ma.user = :user " +
           "ORDER BY ma.recordedAt DESC LIMIT :limit")
    List<MedicationAdherence> findRecentAdherenceByUser(@Param("user") User user, @Param("limit") int limit);
    
    // 약물 ID와 날짜로 복약 기록 조회
    Optional<MedicationAdherence> findByMedication_IdAndAdherenceDate(Long medicationId, LocalDate adherenceDate);
    
    // 사용자와 날짜로 복약 기록 조회
    List<MedicationAdherence> findByUserAndAdherenceDate(User user, LocalDate adherenceDate);
} 