package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리마인더 Repository
 * BIF 사용자의 일정 알림 관리를 위한 데이터 접근 계층
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    /**
     * 사용자의 활성 리마인더 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.schedule.user.userId = :userId " +
           "AND r.isActive = true " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 일정별 리마인더 조회
     */
    List<Reminder> findBySchedule_IdAndIsActiveTrueOrderByReminderTimeAsc(Long scheduleId);
    
    /**
     * 특정 시간대 예정된 리마인더 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.reminderTime BETWEEN :start AND :end " +
           "AND r.isActive = true " +
           "AND r.isSent = false " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findUpcomingReminders(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
    
    /**
     * 전송 대기 중인 리마인더 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.reminderTime <= :now " +
           "AND r.isActive = true " +
           "AND r.isSent = false " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findPendingReminders(@Param("now") LocalDateTime now);
    
    /**
     * 사용자의 특정 날짜 리마인더 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.schedule.user.userId = :userId " +
           "AND CAST(r.reminderTime AS DATE) = CAST(:date AS DATE) " +
           "AND r.isActive = true " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findByUserIdAndDate(@Param("userId") Long userId,
                                     @Param("date") LocalDateTime date);
    
    /**
     * 리마인더 유형별 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.schedule.user.userId = :userId " +
           "AND r.reminderType = :type " +
           "AND r.isActive = true " +
           "ORDER BY r.reminderTime ASC")
    List<Reminder> findByUserIdAndType(@Param("userId") Long userId,
                                     @Param("type") String type);
    
    /**
     * 반복 일정의 리마인더 조회
     */
    @Query("SELECT r FROM Reminder r WHERE r.schedule.user.userId = :userId " +
           "AND r.schedule.recurrenceType != 'ONCE' " +
           "AND r.isActive = true")
    List<Reminder> findRecurringByUserId(@Param("userId") Long userId);
    
    /**
     * 전송 완료된 리마인더 통계
     */
    @Query("SELECT CAST(r.sentAt AS DATE), COUNT(r) " +
           "FROM Reminder r " +
           "WHERE r.schedule.user.userId = :userId " +
           "AND r.isSent = true " +
           "AND r.sentAt BETWEEN :start AND :end " +
           "GROUP BY CAST(r.sentAt AS DATE)")
    List<Object[]> findSentReminderStats(@Param("userId") Long userId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
    
    /**
     * 놓친 리마인더 조회 (전송 시간이 지났지만 전송되지 않은)
     */
    @Query("SELECT r FROM Reminder r WHERE r.schedule.user.userId = :userId " +
           "AND r.reminderTime < :now " +
           "AND r.isSent = false " +
           "AND r.isActive = true " +
           "ORDER BY r.reminderTime DESC")
    List<Reminder> findMissedReminders(@Param("userId") Long userId,
                                     @Param("now") LocalDateTime now);
}