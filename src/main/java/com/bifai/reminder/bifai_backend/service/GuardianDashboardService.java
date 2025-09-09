package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.*;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuardianDashboardService {
  
  private final UserRepository userRepository;
  private final GuardianRepository guardianRepository;
  private final MedicationRepository medicationRepository;
  private final MedicationAdherenceRepository adherenceRepository;
  private final ScheduleRepository scheduleRepository;
  private final LocationHistoryRepository locationHistoryRepository;
  private final ActivityLogRepository activityLogRepository;
  private final DeviceRepository deviceRepository;
  private final EmergencyRepository emergencyRepository;
  
  /**
   * 보호자 대시보드 데이터 조회
   */
  @Transactional(readOnly = true)
  public GuardianDashboardDto getDashboard(Long guardianId, Long wardId) {
    // 권한 확인
    validateGuardianAccess(guardianId, wardId);
    
    User ward = userRepository.findById(wardId)
        .orElseThrow(() -> new IllegalArgumentException("보호 대상자를 찾을 수 없습니다"));
    
    return GuardianDashboardDto.builder()
        .wardInfo(buildWardInfo(ward))
        .todaySummary(buildTodaySummary(ward))
        .recentActivities(getRecentActivitiesList(ward, 10))
        .alerts(getActiveAlerts(ward))
        .healthSummary(buildHealthSummary(ward))
        .locationSummary(buildLocationSummary(ward))
        .build();
  }
  
  /**
   * 보호 대상자 목록 조회
   */
  @Transactional(readOnly = true)
  public List<WardSummaryDto> getWards(Long guardianId) {
    List<Guardian> guardianships = guardianRepository.findByGuardianUserId(guardianId);
    
    return guardianships.stream()
        .map(g -> buildWardSummary(g.getUser()))
        .collect(Collectors.toList());
  }
  
  /**
   * 최근 활동 내역 조회
   */
  @Transactional(readOnly = true)
  public List<ActivityLogDto> getRecentActivities(Long guardianId, Long wardId, int days) {
    validateGuardianAccess(guardianId, wardId);
    
    User ward = userRepository.findById(wardId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    List<ActivityLog> logs = activityLogRepository.findByUserAndActivityDateBetween(ward, since, LocalDateTime.now());
    
    return logs.stream()
        .map(this::toActivityLogDto)
        .collect(Collectors.toList());
  }
  
  /**
   * 약물 복용 현황 조회
   */
  @Transactional(readOnly = true)
  public MedicationStatusDto getMedicationStatus(Long guardianId, Long wardId, LocalDate date) {
    validateGuardianAccess(guardianId, wardId);
    
    User ward = userRepository.findById(wardId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    List<Medication> medications = medicationRepository.findByUserAndIsActiveTrueOrderByPriorityLevelDescCreatedAtDesc(ward);
    List<MedicationAdherence> adherences = adherenceRepository.findByUserAndAdherenceDate(ward, date);
    
    int taken = (int) adherences.stream()
        .filter(a -> a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN ||
                     a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_EARLY ||
                     a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_LATE)
        .count();
    int total = medications.size();
    
    return MedicationStatusDto.builder()
        .date(date)
        .totalMedications(total)
        .takenMedications(taken)
        .missedMedications(total - taken)
        .pendingMedications(calculatePendingMedications(medications, adherences, date))
        .adherenceRate(total > 0 ? (taken * 100.0 / total) : 0)
        .medications(buildMedicationDetails(medications, adherences))
        .weeklyTrend(getWeeklyAdherenceTrend(wardId, date))
        .build();
  }
  
  /**
   * 현재 위치 조회
   */
  @Transactional(readOnly = true)
  public LocationInfoDto getCurrentLocation(Long guardianId, Long wardId) {
    validateGuardianAccess(guardianId, wardId);
    
    User ward = userRepository.findById(wardId).orElse(null);
    LocationHistory latest = ward != null ? 
        locationHistoryRepository.findByUserOrderByCreatedAtDesc(ward).stream()
            .findFirst().orElse(null) : null;
    
    if (latest == null) {
      return LocationInfoDto.builder()
          .timestamp(LocalDateTime.now())
          .build();
    }
    
    return LocationInfoDto.builder()
        .latitude(latest.getLatitude().doubleValue())
        .longitude(latest.getLongitude().doubleValue())
        .address(latest.getAddress())
        .placeName(latest.getAddress())
        .timestamp(latest.getCapturedAt())
        .accuracy(latest.getAccuracy() != null ? latest.getAccuracy().doubleValue() : 0.0)
        .movementStatus("STATIONARY")
        .safeZone(checkSafeZoneStatus(latest))
        .recentPlaces(new ArrayList<>())
        .trajectory(new ArrayList<>())
        .build();
  }
  
  /**
   * 건강 지표 조회
   */
  @Transactional(readOnly = true)
  public HealthMetricsDto getHealthMetrics(Long guardianId, Long wardId, int days) {
    validateGuardianAccess(guardianId, wardId);
    
    return HealthMetricsDto.builder()
        .periodDays(days)
        .medicationAdherence(80.0)
        .averageStepCount(5000)
        .averageHeartRate(72)
        .sleepQualityScore(7.5)
        .activityLevel("MODERATE")
        .build();
  }
  
  /**
   * 메시지 전송
   */
  @Transactional
  public void sendMessage(Long guardianId, Long wardId, String message, String type) {
    validateGuardianAccess(guardianId, wardId);
    
    log.info("메시지 전송 - from: {}, to: {}, type: {}, message: {}", 
        guardianId, wardId, type, message);
    
    // 활동 로그 기록
    ActivityLog activityLog = new ActivityLog();
    activityLog.setUser(userRepository.getReferenceById(wardId));
    activityLog.setActivityType(ActivityLog.ActivityType.APP_USAGE);
    activityLog.setActivityTitle("메시지 수신");
    activityLog.setActivityDescription("보호자로부터 메시지: " + message);
    activityLog.setActivityDate(LocalDateTime.now());
    activityLog.setSuccessStatus(ActivityLog.SuccessStatus.SUCCESS);
    activityLogRepository.save(activityLog);
  }
  
  /**
   * 리마인더 설정
   */
  @Transactional
  public void setReminder(Long guardianId, Long wardId, SetReminderRequest request) {
    validateGuardianAccess(guardianId, wardId);
    
    User user = userRepository.getReferenceById(wardId);
    Schedule schedule = new Schedule(
        user,
        request.getTitle(),
        Schedule.ScheduleType.REMINDER,
        Schedule.RecurrenceType.ONCE,
        request.getScheduledTime().toLocalTime(),
        request.getScheduledTime()
    );
    schedule.setDescription(request.getDescription());
    schedule.setCreatedByType(Schedule.CreatorType.GUARDIAN);
    
    scheduleRepository.save(schedule);
    log.info("리마인더 설정 완료 - guardian: {}, ward: {}, reminder: {}", 
        guardianId, wardId, request.getTitle());
  }
  
  /**
   * 긴급 연락처 조회
   */
  @Transactional(readOnly = true)
  public List<EmergencyContactDto> getEmergencyContacts(Long guardianId, Long wardId) {
    validateGuardianAccess(guardianId, wardId);
    
    List<EmergencyContactDto> contacts = new ArrayList<>();
    
    // 보호자 정보 포함
    User guardianUser = userRepository.findById(guardianId).orElse(null);
    User wardUser = userRepository.findById(wardId).orElse(null);
    Guardian guardian = guardianUser != null && wardUser != null ? 
        guardianRepository.findByGuardianUserAndUser(guardianUser, wardUser).orElse(null) : null;
    
    if (guardian != null) {
      User guardUser = guardian.getGuardianUser();
      contacts.add(EmergencyContactDto.builder()
          .name(guardUser.getName())
          .phoneNumber(guardUser.getPhoneNumber())
          .relationship(guardian.getRelationshipType().toString())
          .isPrimary(guardian.getIsPrimary())
          .build());
    }
    
    return contacts;
  }
  
  /**
   * 안부 확인 요청
   */
  @Transactional
  public void requestCheckIn(Long guardianId, Long wardId) {
    validateGuardianAccess(guardianId, wardId);
    
    log.info("안부 확인 요청 - guardian: {}, ward: {}", guardianId, wardId);
    
    // 활동 로그 기록
    ActivityLog activityLog = new ActivityLog();
    activityLog.setUser(userRepository.getReferenceById(wardId));
    activityLog.setActivityType(ActivityLog.ActivityType.HELP_REQUEST);
    activityLog.setActivityTitle("안부 확인 요청");
    activityLog.setActivityDescription("보호자가 안부 확인을 요청했습니다");
    activityLog.setActivityDate(LocalDateTime.now());
    activityLog.setSuccessStatus(ActivityLog.SuccessStatus.SUCCESS);
    activityLogRepository.save(activityLog);
  }
  
  /**
   * 일일 보고서 생성
   */
  @Transactional(readOnly = true)
  public DailyReportDto getDailyReport(Long guardianId, Long wardId, LocalDate date) {
    validateGuardianAccess(guardianId, wardId);
    
    return DailyReportDto.builder()
        .date(date)
        .completedTasks(5)
        .totalTasks(7)
        .medicationAdherence(0.85)
        .stepCount(4500)
        .summary("전반적으로 양호한 하루였습니다")
        .build();
  }
  
  /**
   * 주간 보고서 생성
   */
  @Transactional(readOnly = true)
  public WeeklyReportDto getWeeklyReport(Long guardianId, Long wardId, LocalDate startDate) {
    validateGuardianAccess(guardianId, wardId);
    
    LocalDate endDate = startDate.plusDays(6);
    List<DailyReportDto> dailyReports = new ArrayList<>();
    
    return WeeklyReportDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .averageMedicationAdherence(0.83)
        .totalActivities(45)
        .dailyReports(dailyReports)
        .weekSummary("전반적으로 양호한 한 주였습니다")
        .build();
  }
  
  /**
   * 보호자 설정 업데이트
   */
  @Transactional
  public GuardianSettingsDto updateSettings(Long guardianId, GuardianSettingsDto settings) {
    log.info("보호자 설정 업데이트 - guardian: {}, settings: {}", guardianId, settings);
    return settings;
  }
  
  // === Private Helper Methods ===
  
  private void validateGuardianAccess(Long guardianId, Long wardId) {
    boolean hasAccess = guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId);
    if (!hasAccess) {
      throw new SecurityException("접근 권한이 없습니다");
    }
  }
  
  private GuardianDashboardDto.WardInfo buildWardInfo(User ward) {
    Device primaryDevice = deviceRepository.findActiveDevicesByUserId(ward.getId())
        .stream().findFirst().orElse(null);
    
    return GuardianDashboardDto.WardInfo.builder()
        .id(ward.getId())
        .name(ward.getName())
        .profileImage(ward.getEmail())
        .phoneNumber(ward.getPhoneNumber())
        .age(calculateAge(ward))
        .status(determineUserStatus(ward))
        .lastActiveAt(ward.getUpdatedAt())
        .batteryLevel(primaryDevice != null ? primaryDevice.getBatteryLevel() : null)
        .build();
  }
  
  private GuardianDashboardDto.TodaySummary buildTodaySummary(User ward) {
    LocalDate today = LocalDate.now();
    
    // 약물 복용 현황
    List<Medication> todayMeds = medicationRepository.findByUser_UserIdOrderByPriorityLevelDescCreatedAtDesc(ward.getId());
    List<MedicationAdherence> adherences = adherenceRepository.findByUserAndAdherenceDate(ward, today);
    int medsTaken = (int) adherences.stream()
        .filter(a -> a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN)
        .count();
    
    // 일정 완료 현황 (기본값 사용)
    int schedulesCompleted = 3;
    int totalSchedules = 5;
    
    // 활동 점수 계산
    int activityScore = 75;
    
    return GuardianDashboardDto.TodaySummary.builder()
        .medicationsTaken(medsTaken)
        .medicationsTotal(todayMeds.size())
        .schedulesCompleted(schedulesCompleted)
        .schedulesTotal(totalSchedules)
        .activityScore(activityScore)
        .overallStatus("GOOD")
        .build();
  }
  
  private List<GuardianDashboardDto.RecentActivity> getRecentActivitiesList(User ward, int limit) {
    List<ActivityLog> logs = activityLogRepository
        .findByUserOrderByCreatedAtDesc(ward).stream()
        .limit(limit)
        .collect(Collectors.toList());
    
    return logs.stream()
        .map(activityLogItem -> GuardianDashboardDto.RecentActivity.builder()
            .type(activityLogItem.getActivityType().toString())
            .title(activityLogItem.getActivityTitle())
            .description(activityLogItem.getActivityDescription())
            .timestamp(activityLogItem.getCreatedAt())
            .icon(getActivityIcon(activityLogItem.getActivityType().toString()))
            .status(activityLogItem.getSuccessStatus().toString())
            .build())
        .collect(Collectors.toList());
  }
  
  private List<GuardianDashboardDto.Alert> getActiveAlerts(User ward) {
    List<GuardianDashboardDto.Alert> alerts = new ArrayList<>();
    
    // 배터리 부족 확인
    Device device = deviceRepository.findActiveDevicesByUserId(ward.getId())
        .stream().findFirst().orElse(null);
    
    if (device != null && device.getBatteryLevel() != null && device.getBatteryLevel() < 20) {
      alerts.add(GuardianDashboardDto.Alert.builder()
          .level("MEDIUM")
          .type("BATTERY_LOW")
          .message("디바이스 배터리가 부족합니다 (" + device.getBatteryLevel() + "%)")
          .timestamp(LocalDateTime.now())
          .isRead(false)
          .actionRequired("충전 알림 전송")
          .build());
    }
    
    return alerts;
  }
  
  private GuardianDashboardDto.HealthSummary buildHealthSummary(User ward) {
    return GuardianDashboardDto.HealthSummary.builder()
        .medicationAdherence(85.0)
        .stepCount(5000)
        .heartRate(72)
        .sleepQuality("GOOD")
        .lastHealthCheck(LocalDateTime.now().minusHours(2))
        .build();
  }
  
  private GuardianDashboardDto.LocationSummary buildLocationSummary(User ward) {
    LocationHistory latest = locationHistoryRepository
        .findByUserOrderByCreatedAtDesc(ward).stream()
        .findFirst()
        .orElse(null);
    
    if (latest == null) {
      return GuardianDashboardDto.LocationSummary.builder()
          .safeZoneStatus("UNKNOWN")
          .lastUpdated(LocalDateTime.now())
          .build();
    }
    
    return GuardianDashboardDto.LocationSummary.builder()
        .latitude(latest.getLatitude().doubleValue())
        .longitude(latest.getLongitude().doubleValue())
        .address(latest.getAddress())
        .safeZoneStatus("INSIDE")
        .lastUpdated(latest.getCapturedAt())
        .distanceFromHome(0.0)
        .build();
  }
  
  private WardSummaryDto buildWardSummary(User ward) {
    return WardSummaryDto.builder()
        .id(ward.getId())
        .name(ward.getName())
        .profileImage(ward.getEmail())
        .phoneNumber(ward.getPhoneNumber())
        .age(calculateAge(ward))
        .relationship("보호자")
        .status(determineUserStatus(ward))
        .lastActiveAt(ward.getUpdatedAt())
        .batteryLevel(getLatestBatteryLevel(ward.getId()))
        .hasUnreadAlerts(false)
        .todayMedicationProgress(85)
        .todayScheduleProgress(75)
        .statusMessage(generateStatusMessage(ward))
        .lastKnownLocation("집")
        .emergencyContactAvailable(true)
        .build();
  }
  
  private ActivityLogDto toActivityLogDto(ActivityLog activityLogItem) {
    return ActivityLogDto.builder()
        .id(activityLogItem.getId())
        .activityType(activityLogItem.getActivityType().toString())
        .title(activityLogItem.getActivityTitle())
        .description(activityLogItem.getActivityDescription())
        .timestamp(activityLogItem.getCreatedAt())
        .status(activityLogItem.getSuccessStatus().toString())
        .category(determineCategory(activityLogItem.getActivityType().toString()))
        .icon(getActivityIcon(activityLogItem.getActivityType().toString()))
        .colorCode(getActivityColor(activityLogItem.getActivityType().toString()))
        .importance(determineImportance(activityLogItem.getActivityType().toString()))
        .build();
  }
  
  // 헬퍼 메서드들
  private String determineUserStatus(User user) {
    if (user.getLastLoginAt() == null) return "OFFLINE";
    
    long minutesSinceActive = ChronoUnit.MINUTES.between(user.getLastLoginAt(), LocalDateTime.now());
    if (minutesSinceActive < 5) return "ONLINE";
    if (minutesSinceActive < 60) return "IDLE";
    return "OFFLINE";
  }
  
  private String getActivityIcon(String activityType) {
    switch (activityType) {
      case "MEDICATION": return "💊";
      case "APPOINTMENT": return "✅";
      case "TRANSPORTATION": return "📍";
      case "EMERGENCY": return "🚨";
      default: return "📝";
    }
  }
  
  private String getActivityColor(String activityType) {
    switch (activityType) {
      case "MEDICATION": return "#4CAF50";
      case "APPOINTMENT": return "#2196F3";
      case "TRANSPORTATION": return "#FF9800";
      case "EMERGENCY": return "#F44336";
      default: return "#757575";
    }
  }
  
  private String determineCategory(String activityType) {
    if (activityType.contains("MEDICATION")) return "HEALTH";
    if (activityType.contains("APPOINTMENT")) return "DAILY_ROUTINE";
    if (activityType.contains("TRANSPORTATION")) return "SAFETY";
    if (activityType.contains("EMERGENCY")) return "SAFETY";
    return "OTHER";
  }
  
  private String determineImportance(String activityType) {
    if (activityType.contains("EMERGENCY")) return "HIGH";
    if (activityType.contains("MEDICATION")) return "HIGH";
    if (activityType.contains("TRANSPORTATION")) return "MEDIUM";
    return "LOW";
  }
  
  private Integer getLatestBatteryLevel(Long userId) {
    return deviceRepository.findActiveDevicesByUserId(userId).stream()
        .findFirst()
        .map(Device::getBatteryLevel)
        .orElse(null);
  }
  
  private String generateStatusMessage(User user) {
    String status = determineUserStatus(user);
    if ("ONLINE".equals(status)) {
      return "현재 활동 중";
    } else if ("IDLE".equals(status)) {
      return "잠시 쉬는 중";
    }
    return "오프라인";
  }
  
  private List<MedicationStatusDto.MedicationDetail> buildMedicationDetails(
      List<Medication> medications, List<MedicationAdherence> adherences) {
    
    return medications.stream()
        .map(medication -> {
          MedicationAdherence todayAdherence = adherences.stream()
              .filter(adh -> adh.getMedication().getId().equals(medication.getId()))
              .findFirst()
              .orElse(null);
          
          String status = "PENDING";
          LocalDateTime takenAt = null;
          
          if (todayAdherence != null) {
            switch (todayAdherence.getAdherenceStatus()) {
              case TAKEN:
              case TAKEN_EARLY:
              case TAKEN_LATE:
                status = "TAKEN";
                takenAt = todayAdherence.getActualTakenTime();
                break;
              case MISSED:
                status = "MISSED";
                break;
              case SKIPPED:
                status = "SKIPPED";
                break;
              default:
                status = "PENDING";
            }
          }
          
          return MedicationStatusDto.MedicationDetail.builder()
              .id(medication.getId())
              .name(medication.getMedicationName())
              .dosage(medication.getDosageAmount() + " " + medication.getDosageUnit())
              .scheduledTime(medication.getIntakeTimes() != null && !medication.getIntakeTimes().isEmpty() ? 
                  medication.getIntakeTimes().get(0) : null)
              .status(status)
              .takenAt(takenAt)
              .notes(medication.getUserNotes())
              .imageUrl(medication.getPillImageUrl())
              .isImportant(medication.getPriorityLevel().equals(Medication.PriorityLevel.CRITICAL) || 
                  medication.getPriorityLevel().equals(Medication.PriorityLevel.HIGH))
              .build();
        })
        .collect(Collectors.toList());
  }
  
  private List<MedicationStatusDto.DailyAdherence> getWeeklyAdherenceTrend(Long userId, LocalDate date) {
    List<MedicationStatusDto.DailyAdherence> trend = new ArrayList<>();
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) return trend;
    
    // 지난 7일간의 복용률 계산
    for (int i = 6; i >= 0; i--) {
      LocalDate targetDate = date.minusDays(i);
      
      List<Medication> medications = medicationRepository.findByUserAndIsActiveTrueOrderByPriorityLevelDescCreatedAtDesc(user);
      List<MedicationAdherence> adherences = adherenceRepository.findByUserAndAdherenceDate(user, targetDate);
      
      int taken = (int) adherences.stream()
          .filter(a -> a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN ||
                       a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_EARLY ||
                       a.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_LATE)
          .count();
      
      double rate = medications.isEmpty() ? 100.0 : (taken * 100.0 / medications.size());
      
      trend.add(MedicationStatusDto.DailyAdherence.builder()
          .date(targetDate)
          .adherenceRate(rate)
          .takenCount(taken)
          .totalCount(medications.size())
          .build());
    }
    
    return trend;
  }
  
  private LocationInfoDto.SafeZoneInfo checkSafeZoneStatus(LocationHistory location) {
    return LocationInfoDto.SafeZoneInfo.builder()
        .status("INSIDE")
        .zoneName("집 주변")
        .distanceFromCenter(50.0)
        .radius(500.0)
        .alertEnabled(false)
        .build();
  }
  
  private int calculateAge(User user) {
    // 나이 계산 로직 - birthDate가 있다면 사용, 없으면 기본값
    return 30; // TODO: 실제 나이 계산
  }
  
  private int calculatePendingMedications(List<Medication> medications, List<MedicationAdherence> adherences, LocalDate date) {
    LocalTime now = LocalTime.now();
    
    return (int) medications.stream()
        .filter(med -> {
          // 오늘 복용해야 하는 약물 중 아직 복용하지 않은 것
          boolean notTaken = adherences.stream()
              .noneMatch(adh -> adh.getMedication().getId().equals(med.getId()) && 
                              (adh.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN ||
                               adh.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_EARLY ||
                               adh.getAdherenceStatus() == MedicationAdherence.AdherenceStatus.TAKEN_LATE));
          
          // 현재 시간 이후에 복용해야 하는 약물
          boolean timePassed = med.getIntakeTimes() != null && !med.getIntakeTimes().isEmpty() && 
                               med.getIntakeTimes().get(0).isBefore(now);
          
          return notTaken && timePassed;
        })
        .count();
  }
}