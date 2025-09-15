package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.dashboard.*;
import com.bifai.reminder.bifai_backend.entity.ActivityLog;
import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 최적화된 대시보드 서비스
 * 병렬 처리 및 캐싱으로 성능 향상
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class OptimizedDashboardService {
  
  private final OptimizedMedicationRepository medicationRepository;
  private final OptimizedActivityLogRepository activityLogRepository;
  private final LocationHistoryRepository locationRepository;
  private final ScheduleRepository scheduleRepository;
  
  @Autowired(required = false)
  private RedisTemplate<String, Object> redisTemplate;
  
  public OptimizedDashboardService(OptimizedMedicationRepository medicationRepository,
                                  OptimizedActivityLogRepository activityLogRepository,
                                  LocationHistoryRepository locationRepository,
                                  ScheduleRepository scheduleRepository) {
    this.medicationRepository = medicationRepository;
    this.activityLogRepository = activityLogRepository;
    this.locationRepository = locationRepository;
    this.scheduleRepository = scheduleRepository;
  }
  
  /**
   * 통합 대시보드 데이터 조회 (병렬 처리)
   */
  @Cacheable(value = "dashboard", key = "#userId + ':' + #date")
  public ComprehensiveDashboardDto getComprehensiveDashboard(Long userId, LocalDate date) {
    log.info("통합 대시보드 조회 시작 - 사용자: {}, 날짜: {}", userId, date);
    
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
    
    // 병렬로 데이터 조회
    CompletableFuture<List<Medication>> medicationsFuture = CompletableFuture.supplyAsync(() ->
      medicationRepository.findTodayMedicationsOptimized(userId, date)
    );
    
    CompletableFuture<List<Object[]>> activityFuture = CompletableFuture.supplyAsync(() ->
      activityLogRepository.getHourlyActivityPattern(userId, startOfDay)
    );
    
    CompletableFuture<LocationSummary> locationFuture = CompletableFuture.supplyAsync(() ->
      getLocationSummary(userId, startOfDay, endOfDay)
    );
    
    CompletableFuture<ScheduleSummary> scheduleFuture = CompletableFuture.supplyAsync(() ->
      getScheduleSummary(userId, startOfDay, endOfDay)
    );
    
    // 모든 비동기 작업 완료 대기
    CompletableFuture.allOf(medicationsFuture, activityFuture, locationFuture, scheduleFuture).join();
    
    try {
      // 결과 수집
      List<Medication> medications = medicationsFuture.get();
      List<Object[]> activityPattern = activityFuture.get();
      LocationSummary locationSummary = locationFuture.get();
      ScheduleSummary scheduleSummary = scheduleFuture.get();
      
      // 대시보드 DTO 생성
      return ComprehensiveDashboardDto.builder()
        .userId(userId)
        .date(date)
        .medicationSummary(createMedicationSummary(medications))
        .activityPattern(parseActivityPattern(activityPattern))
        .locationSummary(locationSummary)
        .scheduleSummary(scheduleSummary)
        .overallStatus(calculateOverallStatus(medications, activityPattern))
        .build();
        
    } catch (Exception e) {
      log.error("대시보드 데이터 조회 실패", e);
      throw new RuntimeException("대시보드 조회 중 오류 발생", e);
    }
  }
  
  /**
   * 주간 트렌드 분석 (캐싱 활용)
   */
  public WeeklyTrendDto getWeeklyTrend(Long userId, LocalDate endDate) {
    String cacheKey = "weekly_trend:" + userId + ":" + endDate;
    
    // Redis 캐시 확인
    if (redisTemplate != null) {
      WeeklyTrendDto cached = (WeeklyTrendDto) redisTemplate.opsForValue().get(cacheKey);
      if (cached != null) {
        return cached;
      }
    }
    
    LocalDate startDate = endDate.minusDays(6);
    
    // 주간 통계 조회
    List<Object> weeklyStats = medicationRepository.getMedicationWeeklyStats(
      userId, startDate, endDate
    );
    
    List<Object[]> activityIntensity = activityLogRepository.getDailyActivityIntensity(
      userId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)
    );
    
    WeeklyTrendDto trend = WeeklyTrendDto.builder()
      .medicationStats(parseMedicationStats(weeklyStats))
      .activityIntensity(parseActivityIntensity(activityIntensity))
      .trendDirection(calculateTrendDirection(weeklyStats, activityIntensity))
      .build();
    
    // 캐시 저장 (1시간)
    if (redisTemplate != null) {
      redisTemplate.opsForValue().set(cacheKey, trend, 1, TimeUnit.HOURS);
    }
    
    return trend;
  }
  
  /**
   * 실시간 알림 필요 항목 조회
   */
  public List<AlertItem> getRealtimeAlerts(Long userId) {
    List<AlertItem> alerts = new ArrayList<>();
    
    // 놓친 복약 확인
    LocalTime currentTime = LocalTime.now();
    List<Medication> missedMedications = medicationRepository.findByTimeRangeOptimized(
      userId, currentTime.minusHours(2), currentTime
    );
    
    for (Medication med : missedMedications) {
      if (!isMedicationTaken(med, LocalDate.now())) {
        alerts.add(AlertItem.builder()
          .type("MEDICATION_MISSED")
          .priority(med.getPriorityLevel().toString())
          .message(med.getMedicationName() + " 복용 시간이 지났습니다")
          .actionRequired("지금 복용하기")
          .build());
      }
    }
    
    // 비활동 시간 확인
    List<Object[]> inactivePeriods = activityLogRepository.findInactivePeriods(userId, 3);
    if (!inactivePeriods.isEmpty()) {
      alerts.add(AlertItem.builder()
        .type("INACTIVITY")
        .priority("MEDIUM")
        .message("3시간 이상 활동이 없습니다")
        .actionRequired("상태 확인 필요")
        .build());
    }
    
    return alerts;
  }
  
  /**
   * 복약 요약 생성
   */
  private MedicationSummary createMedicationSummary(List<Medication> medications) {
    int total = medications.size();
    int critical = 0;
    int high = 0;
    
    for (Medication med : medications) {
      if (med.getPriorityLevel() == Medication.PriorityLevel.CRITICAL) {
        critical++;
      } else if (med.getPriorityLevel() == Medication.PriorityLevel.HIGH) {
        high++;
      }
    }
    
    return MedicationSummary.builder()
      .totalMedications(total)
      .criticalMedications(critical)
      .highPriorityMedications(high)
      .medications(medications.stream()
        .limit(5) // 상위 5개만
        .map(this::toMedicationDto)
        .collect(Collectors.toList()))
      .build();
  }
  
  /**
   * 활동 패턴 파싱
   */
  private Map<Integer, Integer> parseActivityPattern(List<Object[]> pattern) {
    Map<Integer, Integer> result = new HashMap<>();
    for (Object[] row : pattern) {
      Integer hour = ((Number) row[0]).intValue();
      Integer count = ((Number) row[1]).intValue();
      result.put(hour, count);
    }
    return result;
  }
  
  /**
   * 위치 요약 조회
   */
  private LocationSummary getLocationSummary(Long userId, LocalDateTime start, LocalDateTime end) {
    // 위치 정보 조회 로직
    return LocationSummary.builder()
      .currentLocation("집")
      .safeZoneStatus(true)
      .lastUpdateTime(LocalDateTime.now())
      .build();
  }
  
  /**
   * 일정 요약 조회
   */
  private ScheduleSummary getScheduleSummary(Long userId, LocalDateTime start, LocalDateTime end) {
    // 일정 정보 조회 로직
    return ScheduleSummary.builder()
      .totalSchedules(5)
      .completedSchedules(3)
      .upcomingSchedules(2)
      .build();
  }
  
  /**
   * 전반적인 상태 계산
   */
  private String calculateOverallStatus(List<Medication> medications, List<Object[]> activity) {
    // 복약 상태와 활동 패턴을 고려한 상태 계산
    if (medications.isEmpty() || activity.size() < 3) {
      return "WARNING";
    }
    return "GOOD";
  }
  
  /**
   * 복약 여부 확인
   */
  private boolean isMedicationTaken(Medication medication, LocalDate date) {
    // Redis에서 복약 기록 확인
    if (redisTemplate == null) {
      return false; // Redis가 없으면 복용하지 않은 것으로 간주
    }
    String key = "medication_taken:" + medication.getId() + ":" + date;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
  
  /**
   * Medication Entity를 DTO로 변환
   */
  private MedicationDto toMedicationDto(Medication medication) {
    return MedicationDto.builder()
      .medicationId(medication.getId())
      .name(medication.getMedicationName())
      .priority(medication.getPriorityLevel().toString())
      // TODO: scheduled time은 medication 엔티티에 없음
      .scheduledTime(LocalDateTime.now())
      .build();
  }
  
  /**
   * 복약 통계 파싱
   */
  private List<MedicationWeeklyStats> parseMedicationStats(List<Object> stats) {
    // 통계 데이터 파싱
    return new ArrayList<>();
  }
  
  /**
   * 활동 강도 파싱
   */
  private List<ActivityIntensity> parseActivityIntensity(List<Object[]> intensity) {
    // 활동 강도 데이터 파싱
    return new ArrayList<>();
  }
  
  /**
   * 트렌드 방향 계산
   */
  private String calculateTrendDirection(List<Object> medicationStats, List<Object[]> activityStats) {
    // 주간 트렌드 방향 계산
    return "STABLE";
  }
  
  /**
   * 알림 항목 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class AlertItem {
    private String type;
    private String priority;
    private String message;
    private String actionRequired;
  }
  
  /**
   * 복약 요약 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class MedicationSummary {
    private int totalMedications;
    private int criticalMedications;
    private int highPriorityMedications;
    private List<MedicationDto> medications;
  }
  
  /**
   * 복약 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class MedicationDto {
    private Long medicationId;
    private String name;
    private String priority;
    private LocalDateTime scheduledTime;
  }
  
  /**
   * 위치 요약 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class LocationSummary {
    private String currentLocation;
    private boolean safeZoneStatus;
    private LocalDateTime lastUpdateTime;
  }
  
  /**
   * 일정 요약 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class ScheduleSummary {
    private int totalSchedules;
    private int completedSchedules;
    private int upcomingSchedules;
  }
}