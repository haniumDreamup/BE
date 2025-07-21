package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BIF 사용자의 스케줄 데이터 접근을 위한 Repository
 * 반복 패턴, 실행 시간, 우선순위 기반 조회 등 BIF 특화 기능 포함
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 사용자의 활성화된 스케줄 조회 (페이징)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    Page<Schedule> findActiveSchedulesByUser(@Param("user") User user, Pageable pageable);

    /**
     * 사용자의 특정 타입 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleType = :scheduleType AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findByUserAndScheduleTypeAndActive(
            @Param("user") User user,
            @Param("scheduleType") Schedule.ScheduleType scheduleType);

    /**
     * 실행 예정인 스케줄 조회 (다음 N시간 이내)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.nextExecutionTime <= :endTime AND s.nextExecutionTime >= :startTime ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findUpcomingSchedules(
            @Param("user") User user,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 실행 시간이 지난 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.nextExecutionTime <= :currentTime ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findOverdueSchedules(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 높은 우선순위 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.priority >= 3 ORDER BY s.priority DESC, s.nextExecutionTime ASC")
    List<Schedule> findHighPrioritySchedulesByUser(@Param("user") User user);

    /**
     * 오늘의 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.nextExecutionTime >= :startOfDay AND s.nextExecutionTime < :endOfDay ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findTodaySchedules(
            @Param("user") User user,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 특정 날짜 범위의 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.nextExecutionTime BETWEEN :startDate AND :endDate ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findSchedulesBetweenDates(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 반복 타입별 스케줄 개수 조회
     */
    @Query("SELECT s.recurrenceType, COUNT(s) FROM Schedule s WHERE s.user = :user AND s.isActive = true GROUP BY s.recurrenceType")
    List<Object[]> getRecurrenceTypeStatsByUser(@Param("user") User user);

    /**
     * 사용자별 스케줄 타입 통계
     */
    @Query("SELECT s.scheduleType, COUNT(s) FROM Schedule s WHERE s.user = :user AND s.isActive = true GROUP BY s.scheduleType")
    List<Object[]> getScheduleTypeStatsByUser(@Param("user") User user);

    /**
     * 실행이 필요한 스케줄 조회 (시스템에서 알림 생성용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.isActive = true AND s.nextExecutionTime <= :currentTime AND s.nextExecutionTime IS NOT NULL")
    List<Schedule> findSchedulesNeedingExecution(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 생성자별 스케줄 조회 (사용자, 보호자, 시스템)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.createdByType = :creatorType AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findByUserAndCreatorType(
            @Param("user") User user,
            @Param("creatorType") Schedule.CreatorType creatorType);

    /**
     * 보호자가 생성한 스케줄 조회 (BIF 사용자 지원용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.createdByType = 'GUARDIAN' AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findGuardianCreatedSchedules(@Param("user") User user);

    /**
     * 최근 실행된 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.lastExecutionTime IS NOT NULL ORDER BY s.lastExecutionTime DESC")
    List<Schedule> findRecentlyExecutedSchedules(@Param("user") User user, Pageable pageable);

    /**
     * 실행되지 않은 스케줄 조회 (누락 추적용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.nextExecutionTime < :cutoffTime AND s.lastExecutionTime IS NULL")
    List<Schedule> findMissedSchedules(@Param("user") User user, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 스케줄의 다음 실행 시간 업데이트
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.nextExecutionTime = :nextExecutionTime WHERE s.id = :scheduleId")
    void updateNextExecutionTime(@Param("scheduleId") Long scheduleId, @Param("nextExecutionTime") LocalDateTime nextExecutionTime);

    /**
     * 스케줄 실행 완료 처리
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.lastExecutionTime = :executionTime WHERE s.id = :scheduleId")
    void markScheduleExecuted(@Param("scheduleId") Long scheduleId, @Param("executionTime") LocalDateTime executionTime);

    /**
     * 종료된 스케줄 비활성화
     */
    @Modifying
    @Query("UPDATE Schedule s SET s.isActive = false WHERE s.endDate IS NOT NULL AND s.endDate < :currentTime")
    int deactivateExpiredSchedules(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 사용자별 활성 스케줄 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.user = :user AND s.isActive = true")
    long countActiveSchedulesByUser(@Param("user") User user);

    /**
     * 한 번만 실행되는 완료된 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.recurrenceType = 'ONCE' AND s.lastExecutionTime IS NOT NULL")
    List<Schedule> findCompletedOneTimeSchedules(@Param("user") User user);

    /**
     * 응급 타입 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleType = 'EMERGENCY_CHECK' AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findEmergencySchedules(@Param("user") User user);

    /**
     * 약물 복용 스케줄 조회 (BIF 사용자의 복약 관리용)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.scheduleType = 'MEDICATION' AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findMedicationSchedules(@Param("user") User user);

    /**
     * 곧 실행될 스케줄 조회 (다음 1시간 이내)
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true AND s.nextExecutionTime BETWEEN :now AND :oneHourLater ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findSchedulesDueSoon(
            @Param("user") User user,
            @Param("now") LocalDateTime now,
            @Param("oneHourLater") LocalDateTime oneHourLater);

    /**
     * 특정 시각적 표시를 가진 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.visualIndicator = :indicator AND s.isActive = true")
    List<Schedule> findByUserAndVisualIndicator(@Param("user") User user, @Param("indicator") String indicator);

    /**
     * 확인이 필요한 스케줄 조회
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.requiresConfirmation = true AND s.isActive = true ORDER BY s.nextExecutionTime ASC")
    List<Schedule> findSchedulesRequiringConfirmation(@Param("user") User user);

    /**
     * 월별 스케줄 실행 통계
     */
    @Query("SELECT YEAR(s.lastExecutionTime), MONTH(s.lastExecutionTime), COUNT(s) FROM Schedule s " +
           "WHERE s.user = :user AND s.lastExecutionTime IS NOT NULL " +
           "GROUP BY YEAR(s.lastExecutionTime), MONTH(s.lastExecutionTime) " +
           "ORDER BY YEAR(s.lastExecutionTime) DESC, MONTH(s.lastExecutionTime) DESC")
    List<Object[]> getMonthlyExecutionStatsByUser(@Param("user") User user);

    /**
     * 스케줄 성공률 계산용 데이터
     */
    @Query("SELECT s.scheduleType, " +
           "COUNT(CASE WHEN s.lastExecutionTime IS NOT NULL THEN 1 END) as executed, " +
           "COUNT(s) as total " +
           "FROM Schedule s WHERE s.user = :user " +
           "GROUP BY s.scheduleType")
    List<Object[]> getScheduleSuccessRatesByUser(@Param("user") User user);
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 사용자별 일정 목록 조회
     */
    List<Schedule> findByUser(User user);
    
    /**
     * 특정 기간 일정 조회
     */
    List<Schedule> findByUserAndStartDateBetween(User user, LocalDateTime start, LocalDateTime end);
    
    /**
     * 활성 일정만 조회
     */
    List<Schedule> findByUserAndIsActiveTrue(User user);
    
    /**
     * 스케줄 타입별 일정 조회
     */
    List<Schedule> findByUserAndScheduleType(User user, Schedule.ScheduleType scheduleType);
    
    /**
     * 우선순위별 일정 조회
     */
    List<Schedule> findByUserAndPriority(User user, Integer priority);
    
    /**
     * 반복 일정 조회
     */
    List<Schedule> findByUserAndRecurrenceTypeNot(User user, Schedule.RecurrenceType recurrenceType);
    
    /**
     * 다가오는 일정 조회 (리마인더)
     */
    List<Schedule> findByUserAndNextExecutionTimeBetween(User user, LocalDateTime start, LocalDateTime end);
    
    // isAllDay와 category 필드가 Schedule 엔티티에 없으므로 제거
    
    /**
     * 충돌하는 일정 확인
     */
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isActive = true " +
           "AND s.startDate <= :end AND (s.endDate IS NULL OR s.endDate >= :start)")
    List<Schedule> findConflictingSchedules(@Param("user") User user, 
                                          @Param("start") LocalDateTime start, 
                                          @Param("end") LocalDateTime end);
} 