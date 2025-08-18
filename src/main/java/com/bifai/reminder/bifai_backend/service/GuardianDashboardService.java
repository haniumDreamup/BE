package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.*;
import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.ScheduleRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 보호자 대시보드 서비스 (임시 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuardianDashboardService {
  
  private final UserRepository userRepository;
  private final ScheduleRepository scheduleRepository;
  
  @Transactional(readOnly = true)
  public GuardianDashboardDto getDashboard(Long guardianId, Long wardId) {
    log.info("대시보드 조회 - guardian: {}, ward: {}", guardianId, wardId);
    return GuardianDashboardDto.builder().build();
  }
  
  @Transactional(readOnly = true)
  public List<WardSummaryDto> getWardList(Long guardianId) {
    log.info("보호 대상자 목록 조회 - guardian: {}", guardianId);
    return new ArrayList<>();
  }
  
  @Transactional(readOnly = true)
  public List<RecentActivityDto> getRecentActivities(Long guardianId, Long wardId) {
    log.info("최근 활동 조회 - guardian: {}, ward: {}", guardianId, wardId);
    return new ArrayList<>();
  }
  
  @Transactional(readOnly = true)
  public List<MedicationStatusDto> getMedicationStatus(Long guardianId, Long wardId, LocalDate date) {
    log.info("복약 상태 조회 - guardian: {}, ward: {}, date: {}", guardianId, wardId, date);
    return new ArrayList<>();
  }
  
  @Transactional(readOnly = true)
  public LocationInfoDto getLocationInfo(Long guardianId, Long wardId) {
    log.info("위치 정보 조회 - guardian: {}, ward: {}", guardianId, wardId);
    return LocationInfoDto.builder().build();
  }
  
  @Transactional(readOnly = true)
  public HealthMetricsDto getHealthMetrics(Long guardianId, Long wardId, int days) {
    return HealthMetricsDto.builder()
        .periodDays(days)
        .medicationAdherence(0.85)
        .averageStepCount(5000)
        .averageHeartRate(75)
        .sleepQualityScore(7.5)
        .activityLevel("MODERATE")
        .build();
  }
  
  @Transactional
  public void sendMessage(Long guardianId, Long wardId, String message, String type) {
    log.info("메시지 전송 - from: {}, to: {}, type: {}", guardianId, wardId, type);
  }
  
  @Transactional
  public void setReminder(Long guardianId, Long wardId, SetReminderRequest request) {
    log.info("리마인더 설정 - guardian: {}, ward: {}, title: {}", guardianId, wardId, request.getTitle());
    
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
  }
  
  @Transactional(readOnly = true)
  public List<EmergencyContactDto> getEmergencyContacts(Long guardianId, Long wardId) {
    log.info("긴급 연락처 조회 - guardian: {}, ward: {}", guardianId, wardId);
    return new ArrayList<>();
  }
  
  @Transactional
  public void requestSafetyCheck(Long guardianId, Long wardId) {
    log.info("안부 확인 요청 - guardian: {}, ward: {}", guardianId, wardId);
  }
  
  @Transactional(readOnly = true)
  public DailyReportDto getDailyReport(Long guardianId, Long wardId, LocalDate date) {
    return DailyReportDto.builder()
        .date(date)
        .completedTasks(10)
        .totalTasks(12)
        .medicationAdherence(0.85)
        .stepCount(5000)
        .summary("일일 활동 요약")
        .build();
  }
  
  @Transactional(readOnly = true)
  public WeeklyReportDto getWeeklyReport(Long guardianId, Long wardId, LocalDate startDate) {
    LocalDate endDate = startDate.plusDays(6);
    return WeeklyReportDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .averageMedicationAdherence(0.90)
        .totalActivities(75)
        .dailyReports(new ArrayList<>())
        .weekSummary("주간 활동 요약")
        .build();
  }
  
  @Transactional
  public GuardianSettingsDto updateSettings(Long guardianId, GuardianSettingsDto settings) {
    log.info("보호자 설정 업데이트 - guardian: {}", guardianId);
    return settings;
  }
}