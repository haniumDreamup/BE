package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.dashboard.WeeklySummaryDto;
import com.bifai.reminder.bifai_backend.dto.dashboard.WeeklySummaryDto.*;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 주간 요약 리포트 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WeeklySummaryService {
  
  private final UserRepository userRepository;
  private final MedicationRepository medicationRepository;
  private final MedicationAdherenceRepository adherenceRepository;
  private final LocationHistoryRepository locationRepository;
  private final ScheduleRepository scheduleRepository;
  private final ActivityLogRepository activityLogRepository;
  private final GuardianRelationshipRepository relationshipRepository;
  
  /**
   * 주간 요약 조회
   */
  @Cacheable(value = "weeklySummary", key = "#userId + ':' + #guardianId + ':' + #weekOffset")
  public WeeklySummaryDto getWeeklySummary(Long userId, Long guardianId, int weekOffset) {
    log.info("주간 요약 조회 - 사용자: {}, 보호자: {}, 주 오프셋: {}", userId, guardianId, weekOffset);
    
    // 권한 확인
    if (!hasViewPermission(guardianId, userId)) {
      throw new IllegalArgumentException("조회 권한이 없습니다");
    }
    
    // 사용자 정보 조회
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 주간 기간 계산
    LocalDate today = LocalDate.now();
    LocalDate weekStart = today.minusWeeks(weekOffset).with(DayOfWeek.MONDAY);
    LocalDate weekEnd = weekStart.plusDays(6);
    
    // 각 요약 정보 수집
    MedicationWeeklySummary medicationSummary = getMedicationWeeklySummary(userId, weekStart, weekEnd);
    ActivityWeeklySummary activitySummary = getActivityWeeklySummary(userId, weekStart, weekEnd);
    LocationWeeklySummary locationSummary = getLocationWeeklySummary(userId, weekStart, weekEnd);
    ScheduleWeeklySummary scheduleSummary = getScheduleWeeklySummary(userId, weekStart, weekEnd);
    
    // 주간 트렌드 분석
    String weeklyTrend = analyzeWeeklyTrend(medicationSummary, activitySummary);
    List<String> concerns = identifyConcerns(medicationSummary, activitySummary, locationSummary);
    List<String> achievements = identifyAchievements(medicationSummary, activitySummary);
    
    return WeeklySummaryDto.builder()
      .userId(userId)
      .userName(user.getUsername())
      .weekStartDate(weekStart)
      .weekEndDate(weekEnd)
      .medicationSummary(medicationSummary)
      .activitySummary(activitySummary)
      .locationSummary(locationSummary)
      .scheduleSummary(scheduleSummary)
      .weeklyTrend(weeklyTrend)
      .concerns(concerns)
      .achievements(achievements)
      .build();
  }
  
  /**
   * 복약 주간 요약
   */
  private MedicationWeeklySummary getMedicationWeeklySummary(Long userId, LocalDate weekStart, LocalDate weekEnd) {
    Map<LocalDate, Double> dailyRates = new HashMap<>();
    Map<String, Integer> missedMedicationCount = new HashMap<>();
    int totalMedications = 0;
    int totalMissed = 0;
    
    double bestRate = 0;
    String bestDay = null;
    double worstRate = 100;
    String worstDay = null;
    
    // 일별 복약률 계산
    for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
      
      List<Medication> dayMedications = medicationRepository
        .findByUserIdAndScheduledTimeBetween(userId, dayStart, dayEnd);
      
      int dayTotal = dayMedications.size();
      int dayTaken = 0;
      
      for (Medication medication : dayMedications) {
        totalMedications++;
        
        Optional<MedicationAdherence> adherence = adherenceRepository
          .findByMedicationIdAndScheduledDate(medication.getMedicationId(), date);
        
        if (adherence.isPresent() && adherence.get().getTaken()) {
          dayTaken++;
        } else {
          totalMissed++;
          missedMedicationCount.merge(medication.getMedicationName(), 1, Integer::sum);
        }
      }
      
      if (dayTotal > 0) {
        double dayRate = (double) dayTaken / dayTotal * 100;
        dailyRates.put(date, dayRate);
        
        String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        
        if (dayRate > bestRate) {
          bestRate = dayRate;
          bestDay = dayName;
        }
        if (dayRate < worstRate) {
          worstRate = dayRate;
          worstDay = dayName;
        }
      } else {
        dailyRates.put(date, 100.0); // 복약 일정이 없는 날은 100%로 처리
      }
    }
    
    // 자주 놓치는 약 목록 (상위 3개)
    List<String> frequentlyMissed = missedMedicationCount.entrySet().stream()
      .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
      .limit(3)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
    
    double overallRate = totalMedications > 0 ? 
      (double) (totalMedications - totalMissed) / totalMedications * 100 : 100.0;
    
    return MedicationWeeklySummary.builder()
      .overallCompletionRate(Math.round(overallRate * 10) / 10.0)
      .dailyRates(dailyRates)
      .totalMedications(totalMedications)
      .missedMedications(totalMissed)
      .frequentlyMissed(frequentlyMissed)
      .bestDay(bestDay)
      .worstDay(worstDay)
      .build();
  }
  
  /**
   * 활동 주간 요약
   */
  private ActivityWeeklySummary getActivityWeeklySummary(Long userId, LocalDate weekStart, LocalDate weekEnd) {
    Map<LocalDate, Integer> dailyActivity = new HashMap<>();
    int totalActiveMinutes = 0;
    int inactiveDays = 0;
    
    int maxActivity = 0;
    String mostActiveDay = null;
    int minActivity = Integer.MAX_VALUE;
    String leastActiveDay = null;
    
    // 일별 활동 시간 계산
    for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
      
      List<ActivityLog> dayActivities = activityLogRepository
        .findByUserUserIdAndTimestampBetween(userId, dayStart, dayEnd);
      
      // 활동 시간 추정 (로그 수 * 5분)
      int dayMinutes = dayActivities.size() * 5;
      dailyActivity.put(date, dayMinutes);
      totalActiveMinutes += dayMinutes;
      
      if (dayMinutes < 30) {
        inactiveDays++;
      }
      
      String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
      
      if (dayMinutes > maxActivity) {
        maxActivity = dayMinutes;
        mostActiveDay = dayName;
      }
      if (dayMinutes < minActivity) {
        minActivity = dayMinutes;
        leastActiveDay = dayName;
      }
    }
    
    double dailyAverage = totalActiveMinutes / 7.0;
    
    // 활동 패턴 분석
    List<String> patterns = analyzeActivityPatterns(userId, weekStart, weekEnd);
    
    return ActivityWeeklySummary.builder()
      .totalActiveMinutes(totalActiveMinutes)
      .dailyAverageMinutes(Math.round(dailyAverage * 10) / 10.0)
      .dailyActivity(dailyActivity)
      .mostActiveDay(mostActiveDay)
      .leastActiveDay(leastActiveDay)
      .inactiveDays(inactiveDays)
      .activityPatterns(patterns)
      .build();
  }
  
  /**
   * 위치 주간 요약
   */
  private LocationWeeklySummary getLocationWeeklySummary(Long userId, LocalDate weekStart, LocalDate weekEnd) {
    LocalDateTime weekStartTime = weekStart.atStartOfDay();
    LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);
    
    List<LocationHistory> weekLocations = locationRepository
      .findByUserUserIdAndTimestampBetween(userId, weekStartTime, weekEndTime);
    
    // 자주 방문한 장소 집계
    Map<String, Integer> locationFrequency = new HashMap<>();
    int safeZoneExits = 0;
    Set<String> uniquePlaces = new HashSet<>();
    
    for (LocationHistory location : weekLocations) {
      String place = location.getAddress() != null ? location.getAddress() : "알 수 없는 위치";
      locationFrequency.merge(place, 1, Integer::sum);
      uniquePlaces.add(place);
      
      if (Boolean.FALSE.equals(location.getIsInSafeZone())) {
        safeZoneExits++;
      }
    }
    
    // 가장 많이 방문한 장소
    String mostVisited = locationFrequency.entrySet().stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .orElse("없음");
    
    // 평소와 다른 장소 (1번만 방문한 곳들)
    List<String> unusualLocations = locationFrequency.entrySet().stream()
      .filter(entry -> entry.getValue() == 1)
      .map(Map.Entry::getKey)
      .limit(3)
      .collect(Collectors.toList());
    
    return LocationWeeklySummary.builder()
      .frequentLocations(locationFrequency)
      .safeZoneExits(safeZoneExits)
      .mostVisitedPlace(mostVisited)
      .uniquePlacesVisited(uniquePlaces.size())
      .unusualLocations(unusualLocations)
      .build();
  }
  
  /**
   * 일정 주간 요약
   */
  private ScheduleWeeklySummary getScheduleWeeklySummary(Long userId, LocalDate weekStart, LocalDate weekEnd) {
    LocalDateTime weekStartTime = weekStart.atStartOfDay();
    LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);
    
    List<Schedule> weekSchedules = scheduleRepository
      .findByUserUserIdAndScheduledTimeBetween(userId, weekStartTime, weekEndTime);
    
    Map<LocalDate, Integer> dailySchedules = new HashMap<>();
    List<String> missedImportant = new ArrayList<>();
    int completed = 0;
    
    for (Schedule schedule : weekSchedules) {
      LocalDate scheduleDate = schedule.getScheduledTime().toLocalDate();
      dailySchedules.merge(scheduleDate, 1, Integer::sum);
      
      if (Boolean.TRUE.equals(schedule.getCompleted())) {
        completed++;
      } else if ("HIGH".equals(schedule.getPriority())) {
        missedImportant.add(schedule.getTitle());
      }
    }
    
    double completionRate = weekSchedules.size() > 0 ? 
      (double) completed / weekSchedules.size() * 100 : 100.0;
    
    return ScheduleWeeklySummary.builder()
      .totalSchedules(weekSchedules.size())
      .completedSchedules(completed)
      .completionRate(Math.round(completionRate * 10) / 10.0)
      .dailySchedules(dailySchedules)
      .missedImportantSchedules(missedImportant)
      .build();
  }
  
  /**
   * 활동 패턴 분석
   */
  private List<String> analyzeActivityPatterns(Long userId, LocalDate weekStart, LocalDate weekEnd) {
    List<String> patterns = new ArrayList<>();
    
    // 간단한 패턴 분석 (실제로는 더 복잡한 로직 필요)
    LocalDateTime weekStartTime = weekStart.atStartOfDay();
    LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);
    
    List<ActivityLog> activities = activityLogRepository
      .findByUserUserIdAndTimestampBetween(userId, weekStartTime, weekEndTime);
    
    // 아침형/저녁형 판단
    long morningActivities = activities.stream()
      .filter(a -> a.getTimestamp().getHour() < 12)
      .count();
    
    long eveningActivities = activities.stream()
      .filter(a -> a.getTimestamp().getHour() >= 18)
      .count();
    
    if (morningActivities > eveningActivities * 1.5) {
      patterns.add("아침형");
    } else if (eveningActivities > morningActivities * 1.5) {
      patterns.add("저녁형");
    } else {
      patterns.add("규칙적");
    }
    
    return patterns;
  }
  
  /**
   * 주간 트렌드 분석
   */
  private String analyzeWeeklyTrend(MedicationWeeklySummary medication, ActivityWeeklySummary activity) {
    // 간단한 트렌드 분석
    if (medication.getOverallCompletionRate() < 70 || activity.getInactiveDays() > 3) {
      return "DECLINING";
    } else if (medication.getOverallCompletionRate() > 90 && activity.getInactiveDays() < 2) {
      return "IMPROVING";
    } else {
      return "STABLE";
    }
  }
  
  /**
   * 주의사항 식별
   */
  private List<String> identifyConcerns(
      MedicationWeeklySummary medication, 
      ActivityWeeklySummary activity, 
      LocationWeeklySummary location) {
    
    List<String> concerns = new ArrayList<>();
    
    if (medication.getMissedMedications() > 5) {
      concerns.add(String.format("이번 주 %d개의 약을 놓쳤습니다", medication.getMissedMedications()));
    }
    
    if (activity.getInactiveDays() > 3) {
      concerns.add(String.format("%d일 동안 활동이 적었습니다", activity.getInactiveDays()));
    }
    
    if (location.getSafeZoneExits() > 2) {
      concerns.add(String.format("안전 구역을 %d번 벗어났습니다", location.getSafeZoneExits()));
    }
    
    return concerns;
  }
  
  /**
   * 성과 식별
   */
  private List<String> identifyAchievements(
      MedicationWeeklySummary medication, 
      ActivityWeeklySummary activity) {
    
    List<String> achievements = new ArrayList<>();
    
    if (medication.getOverallCompletionRate() > 90) {
      achievements.add("복약률 90% 이상 달성");
    }
    
    if (activity.getDailyAverageMinutes() > 120) {
      achievements.add("매일 2시간 이상 활동");
    }
    
    if (medication.getBestDay() != null) {
      achievements.add(medication.getBestDay() + "에 모든 약 복용");
    }
    
    return achievements;
  }
  
  /**
   * 권한 확인
   */
  private boolean hasViewPermission(Long guardianId, Long userId) {
    return relationshipRepository.hasViewPermission(guardianId, userId);
  }
}