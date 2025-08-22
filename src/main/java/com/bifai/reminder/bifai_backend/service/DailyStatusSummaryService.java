package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.dashboard.DailyStatusSummaryDto;
import com.bifai.reminder.bifai_backend.dto.dashboard.DailyStatusSummaryDto.*;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 오늘의 상태 요약 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DailyStatusSummaryService {
  
  private final UserRepository userRepository;
  private final MedicationRepository medicationRepository;
  private final MedicationAdherenceRepository adherenceRepository;
  private final LocationHistoryRepository locationRepository;
  private final ScheduleRepository scheduleRepository;
  private final ActivityLogRepository activityLogRepository;
  private final GuardianRelationshipRepository relationshipRepository;
  
  /**
   * 오늘의 상태 요약 조회
   */
  @Cacheable(value = "dailySummary", key = "#userId + ':' + #guardianId + ':' + T(java.time.LocalDate).now()")
  public DailyStatusSummaryDto getDailySummary(Long userId, Long guardianId) {
    log.info("오늘의 상태 요약 조회 - 사용자: {}, 보호자: {}", userId, guardianId);
    
    // 권한 확인
    if (!hasViewPermission(guardianId, userId)) {
      throw new IllegalArgumentException("조회 권한이 없습니다");
    }
    
    // 사용자 정보 조회
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
    
    // 각 상태 정보 수집
    MedicationStatus medicationStatus = getMedicationStatus(userId, startOfDay, endOfDay);
    LocationStatus locationStatus = getLocationStatus(userId);
    ActivityStatus activityStatus = getActivityStatus(userId, startOfDay, endOfDay);
    ScheduleStatus scheduleStatus = getScheduleStatus(userId, startOfDay, endOfDay);
    
    // 전체 상태 평가
    String overallStatus = evaluateOverallStatus(
      medicationStatus, locationStatus, activityStatus, scheduleStatus);
    String statusMessage = generateStatusMessage(
      user.getUsername(), overallStatus, medicationStatus, activityStatus);
    
    return DailyStatusSummaryDto.builder()
      .userId(userId)
      .userName(user.getUsername())
      .summaryDate(LocalDateTime.now())
      .medicationStatus(medicationStatus)
      .locationStatus(locationStatus)
      .activityStatus(activityStatus)
      .scheduleStatus(scheduleStatus)
      .overallStatus(overallStatus)
      .statusMessage(statusMessage)
      .build();
  }
  
  /**
   * 복약 상태 조회
   */
  private MedicationStatus getMedicationStatus(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
    // 오늘의 복약 일정 조회
    // TODO: 실제 스케줄 기반 조회로 변경 필요
    List<Medication> todayMedications = medicationRepository
      .findByUserId(userId).stream()
      .filter(m -> m.getIsActive())
      .collect(Collectors.toList());
    
    int totalMedications = todayMedications.size();
    int takenMedications = 0;
    int missedMedications = 0;
    
    LocalDateTime now = LocalDateTime.now();
    String nextMedicationTime = null;
    String nextMedicationName = null;
    LocalDateTime earliestUpcoming = null;
    
    for (Medication medication : todayMedications) {
      // 복약 여부 확인
      Optional<MedicationAdherence> adherence = adherenceRepository
        .findByMedication_IdAndAdherenceDate(medication.getId(), LocalDate.now());
      
      if (adherence.isPresent() && 
          (adherence.get().getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN ||
           adherence.get().getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_EARLY ||
           adherence.get().getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_LATE)) {
        takenMedications++;
      } else if (now.isAfter(now.minusHours(1))) { // TODO: 실제 스케줄 시간 확인 필요
        missedMedications++;
      } else {
        // 다음 복약 시간 확인
        // TODO: 실제 스케줄 시간 기반으로 수정 필요
        if (earliestUpcoming == null) {
          earliestUpcoming = now.plusHours(1);
          nextMedicationTime = formatTime(now.plusHours(1));
          nextMedicationName = medication.getMedicationName();
        }
      }
    }
    
    double completionRate = totalMedications > 0 ? 
      (double) takenMedications / totalMedications * 100 : 100.0;
    
    return MedicationStatus.builder()
      .totalMedications(totalMedications)
      .takenMedications(takenMedications)
      .missedMedications(missedMedications)
      .completionRate(Math.round(completionRate * 10) / 10.0)
      .nextMedicationTime(nextMedicationTime)
      .nextMedicationName(nextMedicationName)
      .build();
  }
  
  /**
   * 위치 상태 조회
   */
  private LocationStatus getLocationStatus(Long userId) {
    // 최근 위치 정보 조회
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return LocationStatus.builder()
        .currentLocation("위치 정보 없음")
        .isInSafeZone(true)
        .minutesSinceUpdate(0)
        .build();
    }
    
    List<LocationHistory> locations = locationRepository
      .findByUserOrderByCreatedAtDesc(user);
    
    if (locations.isEmpty()) {
      return LocationStatus.builder()
        .currentLocation("위치 정보 없음")
        .isInSafeZone(true)
        .minutesSinceUpdate(0)
        .build();
    }
    
    LocationHistory location = locations.get(0);
    
    LocalDateTime now = LocalDateTime.now();
    long minutesSinceUpdate = ChronoUnit.MINUTES.between(location.getCapturedAt(), now);
    
    // 주소 또는 장소명 생성
    String locationName = location.getAddress();
    if (locationName == null || locationName.isEmpty()) {
      locationName = String.format("위도 %.4f, 경도 %.4f", 
        location.getLatitude(), location.getLongitude());
    }
    
    return LocationStatus.builder()
      .currentLocation(locationName)
      .lastUpdated(location.getCapturedAt())
      .isInSafeZone(location.getInSafeZone() != null ? location.getInSafeZone() : true)
      .minutesSinceUpdate((int) minutesSinceUpdate)
      .build();
  }
  
  /**
   * 활동 상태 조회
   */
  private ActivityStatus getActivityStatus(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
    // 오늘의 활동 로그 조회
    List<ActivityLog> todayActivities = activityLogRepository
      .findByUser_UserIdAndActivityDateBetween(userId, startOfDay, endOfDay);
    
    LocalDateTime lastActiveTime = null;
    int totalActiveMinutes = 0;
    int screenTimeMinutes = 0;
    
    if (!todayActivities.isEmpty()) {
      // 마지막 활동 시간
      lastActiveTime = todayActivities.stream()
        .map(ActivityLog::getActivityDate)
        .max(LocalDateTime::compareTo)
        .orElse(null);
      
      // 활동 시간 계산 (간단히 로그 수 * 5분으로 추정)
      totalActiveMinutes = todayActivities.size() * 5;
      
      // 화면 사용 시간 (앱 사용 로그만 계산)
      screenTimeMinutes = (int) todayActivities.stream()
        .filter(log -> log.getActivityType() == ActivityLog.ActivityType.APP_USAGE)
        .count() * 5;
    }
    
    // 활동 레벨 평가
    String activityLevel;
    if (totalActiveMinutes > 180) {
      activityLevel = "HIGH";
    } else if (totalActiveMinutes > 60) {
      activityLevel = "NORMAL";
    } else {
      activityLevel = "LOW";
    }
    
    // 현재 활동 중 여부 (30분 이내 활동이 있으면 활동 중)
    boolean isCurrentlyActive = lastActiveTime != null && 
      ChronoUnit.MINUTES.between(lastActiveTime, LocalDateTime.now()) < 30;
    
    return ActivityStatus.builder()
      .lastActiveTime(lastActiveTime)
      .totalActiveMinutes(totalActiveMinutes)
      .screenTimeMinutes(screenTimeMinutes)
      .activityLevel(activityLevel)
      .isCurrentlyActive(isCurrentlyActive)
      .build();
  }
  
  /**
   * 일정 상태 조회
   */
  private ScheduleStatus getScheduleStatus(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
    // 오늘의 일정 조회
    List<Schedule> todaySchedules = scheduleRepository
      .findByUser_UserIdAndNextExecutionTimeBetween(userId, startOfDay, endOfDay);
    
    int totalSchedules = todaySchedules.size();
    int completedSchedules = 0;
    int upcomingSchedules = 0;
    
    LocalDateTime now = LocalDateTime.now();
    String nextScheduleTime = null;
    String nextScheduleTitle = null;
    LocalDateTime earliestUpcoming = null;
    
    for (Schedule schedule : todaySchedules) {
      // 마지막 실행 시간이 오늘인 경우 완료로 간주
      if (schedule.getLastExecutionTime() != null && 
          schedule.getLastExecutionTime().toLocalDate().equals(LocalDate.now())) {
        completedSchedules++;
      } else if (schedule.getNextExecutionTime() != null && schedule.getNextExecutionTime().isAfter(now)) {
        upcomingSchedules++;
        
        // 다음 일정 확인
        if (earliestUpcoming == null || schedule.getNextExecutionTime().isBefore(earliestUpcoming)) {
          earliestUpcoming = schedule.getNextExecutionTime();
          nextScheduleTime = formatTime(schedule.getNextExecutionTime());
          nextScheduleTitle = schedule.getTitle();
        }
      }
    }
    
    return ScheduleStatus.builder()
      .totalSchedules(totalSchedules)
      .completedSchedules(completedSchedules)
      .upcomingSchedules(upcomingSchedules)
      .nextScheduleTime(nextScheduleTime)
      .nextScheduleTitle(nextScheduleTitle)
      .build();
  }
  
  /**
   * 전체 상태 평가
   */
  private String evaluateOverallStatus(
      MedicationStatus medication, 
      LocationStatus location, 
      ActivityStatus activity, 
      ScheduleStatus schedule) {
    
    // 위험 신호 체크
    if (medication.getMissedMedications() > 2 || 
        location.getMinutesSinceUpdate() > 240 ||
        "LOW".equals(activity.getActivityLevel()) && !activity.isCurrentlyActive()) {
      return "WARNING";
    }
    
    // 주의 필요 신호
    if (medication.getMissedMedications() > 0 ||
        location.getMinutesSinceUpdate() > 120 ||
        medication.getCompletionRate() < 70) {
      return "ATTENTION_NEEDED";
    }
    
    return "GOOD";
  }
  
  /**
   * 상태 메시지 생성
   */
  private String generateStatusMessage(
      String userName, 
      String overallStatus, 
      MedicationStatus medication,
      ActivityStatus activity) {
    
    switch (overallStatus) {
      case "GOOD":
        return String.format("%s님은 오늘 잘 지내고 있습니다", userName);
      
      case "ATTENTION_NEEDED":
        if (medication.getMissedMedications() > 0) {
          return String.format("%s님이 약 %d개를 놓쳤습니다. 확인이 필요합니다", 
            userName, medication.getMissedMedications());
        }
        if ("LOW".equals(activity.getActivityLevel())) {
          return String.format("%s님의 활동이 평소보다 적습니다", userName);
        }
        return String.format("%s님의 상태를 확인해주세요", userName);
      
      case "WARNING":
        return String.format("%s님의 상태가 걱정됩니다. 연락해보세요", userName);
      
      default:
        return String.format("%s님의 오늘 상태입니다", userName);
    }
  }
  
  /**
   * 권한 확인
   */
  private boolean hasViewPermission(Long guardianId, Long userId) {
    return relationshipRepository.hasViewPermission(guardianId, userId);
  }
  
  /**
   * 시간 포맷팅
   */
  private String formatTime(LocalDateTime dateTime) {
    return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
  }
}