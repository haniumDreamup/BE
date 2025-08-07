package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.HealthMetric;
import com.bifai.reminder.bifai_backend.entity.User;
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
 * HealthMetricRepository - BIF User Health Indicator Repository
 * 
 * BIF 사용자의 건강 지표를 관리하는 리포지토리입니다.
 * 건강 데이터 추적, 트렌드 분석, 경고 관리 등의 기능을 제공합니다.
 */
@Repository
public interface HealthMetricRepository extends JpaRepository<HealthMetric, Long> {

    // 기본 조회 메서드
    List<HealthMetric> findByUserOrderByMeasuredAtDesc(User user);
    
    Page<HealthMetric> findByUserOrderByMeasuredAtDesc(User user, Pageable pageable);
    
    List<HealthMetric> findByUser_UserIdOrderByMeasuredAtDesc(Long userId);
    
    // 건강 지표 유형별 조회
    List<HealthMetric> findByUserAndMetricTypeOrderByMeasuredAtDesc(
        User user, HealthMetric.MetricType metricType);
    
    Page<HealthMetric> findByUserAndMetricTypeOrderByMeasuredAtDesc(
        User user, HealthMetric.MetricType metricType, Pageable pageable);
    
    // 날짜 범위별 조회
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measuredAt >= :startDate AND hm.measuredAt <= :endDate " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findByUserAndMeasuredAtBetween(
        @Param("user") User user,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measuredAt >= :startDate AND hm.measuredAt <= :endDate " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findByUserAndMetricTypeAndMeasuredAtBetween(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    // 오늘의 건강 지표
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND CAST(hm.measuredAt AS DATE) = CURRENT_DATE " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findTodayMetricsByUser(@Param("user") User user);
    
    // 경고 수준별 조회
    List<HealthMetric> findByUserAndAlertLevelOrderByMeasuredAtDesc(
        User user, HealthMetric.AlertLevel alertLevel);
    
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.alertLevel IN ('CRITICAL', 'HIGH') " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findCriticalAndHighAlertMetricsByUser(@Param("user") User user);
    
    // 측정 상태별 조회
    List<HealthMetric> findByUserAndMeasurementStatusOrderByMeasuredAtDesc(
        User user, HealthMetric.MeasurementStatus measurementStatus);
    
    // 보호자 알림이 필요한 지표들
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND (hm.alertLevel IN ('CRITICAL', 'HIGH') OR hm.doctorConsultationNeeded = true) " +
           "AND hm.guardianNotified = false " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findMetricsNeedingGuardianAlert(@Param("user") User user);
    
    // 의사 상담이 필요한 지표들
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.doctorConsultationNeeded = true " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findMetricsNeedingDoctorConsultation(@Param("user") User user);
    
    // 최근 측정값 (지표별 최신 기록)
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "ORDER BY hm.measuredAt DESC LIMIT 1")
    Optional<HealthMetric> findLatestMetricByType(
        @Param("user") User user, @Param("metricType") HealthMetric.MetricType metricType);
    
    // 각 지표별 최신 측정값들
    @Query("SELECT hm1 FROM HealthMetric hm1 WHERE hm1.user = :user " +
           "AND hm1.measuredAt = (SELECT MAX(hm2.measuredAt) FROM HealthMetric hm2 " +
           "WHERE hm2.user = :user AND hm2.metricType = hm1.metricType) " +
           "ORDER BY hm1.metricType")
    List<HealthMetric> findLatestMetricsForAllTypes(@Param("user") User user);
    
    // 트렌드 분석을 위한 시계열 데이터
    @Query("SELECT hm.measuredAt, hm.value, hm.secondaryValue FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.measuredAt >= :startDate " +
           "ORDER BY hm.measuredAt")
    List<Object[]> getMetricTrendData(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 평균값 계산
    @Query("SELECT AVG(hm.value) FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measuredAt >= :startDate " +
           "AND hm.measurementStatus = 'COMPLETED'")
    Double getAverageValueByMetricType(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 최고/최저값
    @Query("SELECT MAX(hm.value), MIN(hm.value) FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measuredAt >= :startDate " +
           "AND hm.measurementStatus = 'COMPLETED'")
    List<Object[]> getMaxMinValueByMetricType(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 월별 통계
    @Query("SELECT YEAR(hm.measuredAt), MONTH(hm.measuredAt), " +
           "AVG(hm.value), MAX(hm.value), MIN(hm.value), COUNT(hm) " +
           "FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "GROUP BY YEAR(hm.measuredAt), MONTH(hm.measuredAt) " +
           "ORDER BY YEAR(hm.measuredAt), MONTH(hm.measuredAt)")
    List<Object[]> getMonthlyStatisticsByMetricType(
        @Param("user") User user, @Param("metricType") HealthMetric.MetricType metricType);
    
    // 주별 통계
    @Query("SELECT YEAR(hm.measuredAt), WEEK(hm.measuredAt), " +
           "AVG(hm.value), MAX(hm.value), MIN(hm.value), COUNT(hm) " +
           "FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY YEAR(hm.measuredAt), WEEK(hm.measuredAt) " +
           "ORDER BY YEAR(hm.measuredAt), WEEK(hm.measuredAt)")
    List<Object[]> getWeeklyStatisticsByMetricType(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 일별 통계
    @Query("SELECT CAST(hm.measuredAt AS DATE), AVG(hm.value), MAX(hm.value), MIN(hm.value), COUNT(hm) " +
           "FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY CAST(hm.measuredAt AS DATE) " +
           "ORDER BY CAST(hm.measuredAt AS DATE)")
    List<Object[]> getDailyStatisticsByMetricType(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 측정 방법별 통계
    @Query("SELECT hm.measurementMethod, COUNT(hm), AVG(hm.value) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY hm.measurementMethod ORDER BY COUNT(hm) DESC")
    List<Object[]> getStatisticsByMeasurementMethod(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 시간대별 측정 패턴
    @Query("SELECT HOUR(hm.measuredAt), COUNT(hm), AVG(hm.value) " +
           "FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY HOUR(hm.measuredAt) " +
           "ORDER BY HOUR(hm.measuredAt)")
    List<Object[]> getMeasurementPatternByHour(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 측정 맥락별 통계
    @Query("SELECT hm.timingContext, COUNT(hm), AVG(hm.value) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.timingContext IS NOT NULL " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY hm.timingContext ORDER BY COUNT(hm) DESC")
    List<Object[]> getStatisticsByTimingContext(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 주관적 컨디션과 객관적 지표 상관관계
    @Query("SELECT hm.subjectiveFeeling, AVG(hm.value), COUNT(hm) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.subjectiveFeeling IS NOT NULL " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY hm.subjectiveFeeling ORDER BY hm.subjectiveFeeling")
    List<Object[]> getCorrelationWithSubjectiveFeeling(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 경고 수준별 통계
    @Query("SELECT hm.alertLevel, COUNT(hm) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.measuredAt >= :startDate " +
           "GROUP BY hm.alertLevel")
    List<Object[]> getAlertLevelStatistics(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 지표별 총 개수 통계
    @Query("SELECT hm.metricType, COUNT(hm) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.measuredAt >= :startDate " +
           "GROUP BY hm.metricType ORDER BY COUNT(hm) DESC")
    List<Object[]> getMetricCountByType(
        @Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    // 정상 범위 내 측정값 비율
    @Query("SELECT COUNT(CASE WHEN hm.value BETWEEN hm.referenceMin AND hm.referenceMax THEN 1 END), " +
           "COUNT(hm) FROM HealthMetric hm " +
           "WHERE hm.user = :user AND hm.metricType = :metricType " +
           "AND hm.referenceMin IS NOT NULL AND hm.referenceMax IS NOT NULL " +
           "AND hm.measurementStatus = 'COMPLETED' " +
           "AND hm.measuredAt >= :startDate")
    List<Object[]> getNormalRangeComplianceRate(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("startDate") LocalDateTime startDate);
    
    // 최근 측정값들 (지표별 최근 N개)
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.metricType = :metricType " +
           "ORDER BY hm.measuredAt DESC LIMIT :limit")
    List<HealthMetric> findRecentMetricsByType(
        @Param("user") User user,
        @Param("metricType") HealthMetric.MetricType metricType,
        @Param("limit") int limit);
    
    // 증상이 있는 측정 기록들
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.symptoms IS NOT NULL AND hm.symptoms != '' " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findMetricsWithSymptoms(@Param("user") User user);
    
    // 특정 기기로 측정한 데이터
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measurementDevice = :deviceName " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findMetricsByDevice(
        @Param("user") User user, @Param("deviceName") String deviceName);
    
    // 동기화 상태 관련
    List<HealthMetric> findByUserAndSyncStatus(User user, String syncStatus);
    
    @Query("SELECT COUNT(hm) FROM HealthMetric hm WHERE hm.user = :user AND hm.syncStatus = 'PENDING'")
    Long countPendingSyncByUser(@Param("user") User user);
    
    // 측정 실패한 기록들
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measurementStatus IN ('FAILED', 'DEVICE_ERROR') " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findFailedMeasurements(@Param("user") User user);
    
    // 수동 입력된 기록들
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measurementStatus = 'MANUAL_INPUT' " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findManuallyInputMetrics(@Param("user") User user);
    
    // 최근 N일 동안의 모든 지표
    @Query("SELECT hm FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measuredAt >= :since " +
           "ORDER BY hm.measuredAt DESC")
    List<HealthMetric> findRecentMetrics(
        @Param("user") User user, @Param("since") LocalDateTime since);
    
    // 사용자별 총 측정 횟수
    @Query("SELECT COUNT(hm) FROM HealthMetric hm WHERE hm.user = :user " +
           "AND hm.measurementStatus = 'COMPLETED'")
    Long countTotalMeasurementsByUser(@Param("user") User user);
} 