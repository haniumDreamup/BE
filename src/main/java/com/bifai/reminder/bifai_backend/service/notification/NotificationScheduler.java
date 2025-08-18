package com.bifai.reminder.bifai_backend.service.notification;

import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.Medication;
import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.DeviceRepository;
import com.bifai.reminder.bifai_backend.repository.MedicationRepository;
import com.bifai.reminder.bifai_backend.repository.ScheduleRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduler {
  
  private final FcmService fcmService;
  private final UserRepository userRepository;
  private final DeviceRepository deviceRepository;
  private final MedicationRepository medicationRepository;
  private final ScheduleRepository scheduleRepository;
  
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  
  /**
   * 매분마다 실행되어 약물 복용 알림을 확인하고 전송
   */
  @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
  @Transactional(readOnly = true)
  public void checkMedicationReminders() {
    LocalTime now = LocalTime.now().withSecond(0).withNano(0);
    log.debug("약물 복용 알림 체크: {}", now);
    
    // 현재 시간에 복용해야 할 약물 찾기
    List<Medication> medications = medicationRepository.findByScheduleTime(now);
    
    for (Medication medication : medications) {
      try {
        sendMedicationReminder(medication);
      } catch (Exception e) {
        log.error("약물 알림 전송 실패 - medication: {}, error: {}", 
            medication.getId(), e.getMessage());
      }
    }
  }
  
  /**
   * 매분마다 실행되어 일정 알림을 확인하고 전송
   */
  @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
  @Transactional(readOnly = true)
  public void checkScheduleReminders() {
    LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
    log.debug("일정 알림 체크: {}", now);
    
    // 현재 시간의 일정 찾기
    List<Schedule> schedules = scheduleRepository.findByScheduledTime(now);
    
    for (Schedule schedule : schedules) {
      try {
        sendScheduleReminder(schedule);
      } catch (Exception e) {
        log.error("일정 알림 전송 실패 - schedule: {}, error: {}", 
            schedule.getId(), e.getMessage());
      }
    }
  }
  
  /**
   * 매일 오후 9시에 일일 요약 전송
   */
  @Scheduled(cron = "0 0 21 * * *") // 매일 21:00:00에 실행
  @Transactional(readOnly = true)
  public void sendDailySummaries() {
    log.info("일일 요약 알림 시작");
    
    List<User> activeUsers = userRepository.findAllActiveUsers();
    LocalDate today = LocalDate.now();
    
    for (User user : activeUsers) {
      try {
        sendDailySummaryToUser(user, today);
      } catch (Exception e) {
        log.error("일일 요약 전송 실패 - user: {}, error: {}", 
            user.getId(), e.getMessage());
      }
    }
    
    log.info("일일 요약 알림 완료 - {} 명에게 전송", activeUsers.size());
  }
  
  /**
   * 약물 복용 알림 전송
   */
  private void sendMedicationReminder(Medication medication) {
    User user = medication.getUser();
    String fcmToken = getActiveFcmToken(user);
    
    if (fcmToken == null) {
      log.debug("FCM 토큰 없음 - user: {}", user.getId());
      return;
    }
    
    String medicationName = medication.getMedicationName();
    String time = medication.getIntakeTimes() != null && !medication.getIntakeTimes().isEmpty() 
        ? medication.getIntakeTimes().get(0).format(TIME_FORMATTER)
        : "09:00"; // 기본 시간
    
    fcmService.sendMedicationReminder(fcmToken, medicationName, time);
    
    log.info("약물 알림 전송 - user: {}, medication: {}", 
        user.getId(), medicationName);
  }
  
  /**
   * 일정 알림 전송
   */
  private void sendScheduleReminder(Schedule schedule) {
    User user = schedule.getUser();
    String fcmToken = getActiveFcmToken(user);
    
    if (fcmToken == null) {
      log.debug("FCM 토큰 없음 - user: {}", user.getId());
      return;
    }
    
    String scheduleName = schedule.getTitle();
    String time = schedule.getExecutionTime().format(
        DateTimeFormatter.ofPattern("HH:mm"));
    String location = schedule.getDescription(); // 위치 대신 설명 사용
    
    fcmService.sendScheduleReminder(fcmToken, scheduleName, time, location);
    
    log.info("일정 알림 전송 - user: {}, schedule: {}", 
        user.getId(), scheduleName);
  }
  
  /**
   * 일일 요약 전송
   */
  private void sendDailySummaryToUser(User user, LocalDate date) {
    String fcmToken = getActiveFcmToken(user);
    
    if (fcmToken == null) {
      return;
    }
    
    // 오늘의 통계 계산 - TODO: 실제 메서드 구현 필요
    int medicationsTaken = 0; // medicationRepository.countTakenMedications(user.getId(), date);
    int schedulesCompleted = 0; // scheduleRepository.countCompletedSchedules(user.getId(), date);
    
    fcmService.sendDailySummary(fcmToken, medicationsTaken, schedulesCompleted);
    
    log.info("일일 요약 전송 - user: {}, medications: {}, schedules: {}", 
        user.getId(), medicationsTaken, schedulesCompleted);
  }
  
  /**
   * 사용자의 활성 FCM 토큰 가져오기
   */
  private String getActiveFcmToken(User user) {
    // 사용자의 활성 디바이스 찾기
    List<Device> activeDevices = deviceRepository.findActiveDevicesByUserId(user.getId());
    
    if (activeDevices.isEmpty()) {
      return null;
    }
    
    // 가장 최근에 사용된 디바이스의 FCM 토큰 반환
    Device primaryDevice = activeDevices.stream()
        .filter(d -> d.getFcmToken() != null && !d.getFcmToken().isEmpty())
        .findFirst()
        .orElse(null);
    
    return primaryDevice != null ? primaryDevice.getFcmToken() : null;
  }
  
  /**
   * 특정 사용자에게 즉시 알림 전송 (테스트용)
   */
  public void sendTestNotification(Long userId, String title, String body) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    String fcmToken = getActiveFcmToken(user);
    
    if (fcmToken == null) {
      throw new IllegalStateException("활성 디바이스가 없습니다");
    }
    
    fcmService.sendPushNotification(fcmToken, title, body, null);
    log.info("테스트 알림 전송 - user: {}", userId);
  }
  
  /**
   * 보호자들에게 긴급 알림 전송
   */
  public void sendEmergencyToGuardians(Long userId, String message, 
                                        Double latitude, Double longitude) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 보호자들의 FCM 토큰 수집
    List<String> guardianTokens = user.getGuardians().stream()
        .map(guardian -> getActiveFcmToken(guardian.getGuardianUser()))
        .filter(token -> token != null)
        .collect(Collectors.toList());
    
    if (guardianTokens.isEmpty()) {
      log.warn("긴급 알림 전송 실패 - 보호자 FCM 토큰 없음: user={}", userId);
      return;
    }
    
    fcmService.sendEmergencyAlert(
        guardianTokens, 
        user.getName(), 
        message, 
        latitude, 
        longitude
    );
    
    log.info("긴급 알림 전송 - user: {}, guardians: {}", 
        userId, guardianTokens.size());
  }
  
  /**
   * 예약된 알림 생성
   */
  public void scheduleNotification(Long userId, LocalDateTime scheduledTime, 
                                    String title, String body) {
    // TODO: 데이터베이스에 예약 알림 저장
    // 별도의 ScheduledNotification 엔티티를 만들어 관리
    log.info("알림 예약 - user: {}, time: {}, title: {}", 
        userId, scheduledTime, title);
  }
  
  /**
   * 반복 알림 설정
   */
  public void setRecurringNotification(Long userId, String cronExpression, 
                                        String title, String body) {
    // TODO: 반복 알림 설정 저장
    log.info("반복 알림 설정 - user: {}, cron: {}, title: {}", 
        userId, cronExpression, title);
  }
}