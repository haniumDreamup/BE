package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.*;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianDashboardService 테스트")
class GuardianDashboardServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private GuardianRepository guardianRepository;
  @Mock private MedicationRepository medicationRepository;
  @Mock private MedicationAdherenceRepository adherenceRepository;
  @Mock private ScheduleRepository scheduleRepository;
  @Mock private LocationHistoryRepository locationHistoryRepository;
  @Mock private ActivityLogRepository activityLogRepository;
  @Mock private DeviceRepository deviceRepository;
  @Mock private EmergencyRepository emergencyRepository;

  @InjectMocks
  private GuardianDashboardService guardianDashboardService;

  private User testUser;
  private User guardianUser;
  private Guardian testGuardian;
  private List<Medication> testMedications;
  private List<MedicationAdherence> testAdherences;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정 - Builder 패턴 사용
    testUser = User.builder()
        .userId(1L)
        .username("testUser")
        .name("테스트 사용자")
        .email("test@example.com")
        .phoneNumber("010-1234-5678")
        .lastLoginAt(LocalDateTime.now().minusMinutes(2))
        .isActive(true)
        .cognitiveLevel(User.CognitiveLevel.MODERATE)
        .build();

    // 보호자 사용자 설정 - Builder 패턴 사용
    guardianUser = User.builder()
        .userId(2L)
        .username("guardianUser")
        .name("보호자")
        .email("guardian@example.com")
        .isActive(true)
        .build();

    // 보호자 관계 설정 - Builder 패턴 사용
    testGuardian = Guardian.builder()
        .id(1L)
        .user(testUser)
        .guardianUser(guardianUser)
        .name("보호자")
        .relationship("가족")
        .relationshipType(Guardian.RelationshipType.CAREGIVER)
        .primaryPhone("010-9876-5432")
        .isPrimary(true)
        .isActive(true)
        .build();

    // 테스트 약물 설정
    testMedications = createTestMedications();
    testAdherences = createTestAdherences();
  }

  @Test
  @DisplayName("보호자 대시보드 데이터 조회 - 성공")
  void getDashboard_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.findById(wardId)).thenReturn(Optional.of(testUser));
    when(medicationRepository.findByUser_UserIdOrderByPriorityLevelDescCreatedAtDesc(testUser.getId()))
        .thenReturn(testMedications);
    when(adherenceRepository.findByUserAndAdherenceDate(eq(testUser), any(LocalDate.class)))
        .thenReturn(testAdherences);
    when(activityLogRepository.findByUserOrderByCreatedAtDesc(testUser))
        .thenReturn(createTestActivityLogs());
    when(deviceRepository.findActiveDevicesByUserId(wardId))
        .thenReturn(createTestDevices());

    // When
    GuardianDashboardDto result = guardianDashboardService.getDashboard(guardianId, wardId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getWardInfo()).isNotNull();
    assertThat(result.getWardInfo().getId()).isEqualTo(wardId);
    assertThat(result.getWardInfo().getName()).isEqualTo("테스트 사용자");
    
    assertThat(result.getTodaySummary()).isNotNull();
    assertThat(result.getTodaySummary().getMedicationsTotal()).isEqualTo(testMedications.size());
    
    assertThat(result.getRecentActivities()).hasSize(createTestActivityLogs().size());
    assertThat(result.getHealthSummary()).isNotNull();
    assertThat(result.getLocationSummary()).isNotNull();
  }

  @Test
  @DisplayName("보호자 대시보드 데이터 조회 - 권한 없음")
  void getDashboard_AccessDenied() {
    // Given
    Long guardianId = 999L;
    Long wardId = testUser.getUserId();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> guardianDashboardService.getDashboard(guardianId, wardId))
        .isInstanceOf(SecurityException.class)
        .hasMessage("접근 권한이 없습니다");
  }

  @Test
  @DisplayName("보호 대상자 목록 조회 - 성공")
  void getWards_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    
    when(guardianRepository.findByGuardianUserId(guardianId))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(createTestDevices());

    // When
    List<WardSummaryDto> result = guardianDashboardService.getWards(guardianId);

    // Then
    assertThat(result).hasSize(1);
    WardSummaryDto ward = result.get(0);
    assertThat(ward.getId()).isEqualTo(testUser.getUserId());
    assertThat(ward.getName()).isEqualTo("테스트 사용자");
    assertThat(ward.getStatus()).isEqualTo("ONLINE");
  }

  @Test
  @DisplayName("약물 복용 현황 조회 - 성공")
  void getMedicationStatus_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();
    LocalDate testDate = LocalDate.now();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.findById(wardId)).thenReturn(Optional.of(testUser));
    when(medicationRepository.findByUserAndIsActiveTrueOrderByPriorityLevelDescCreatedAtDesc(testUser))
        .thenReturn(testMedications);
    when(adherenceRepository.findByUserAndAdherenceDate(testUser, testDate))
        .thenReturn(testAdherences);

    // When
    MedicationStatusDto result = guardianDashboardService.getMedicationStatus(guardianId, wardId, testDate);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getDate()).isEqualTo(testDate);
    assertThat(result.getTotalMedications()).isEqualTo(2);
    assertThat(result.getTakenMedications()).isEqualTo(1);
    assertThat(result.getMissedMedications()).isEqualTo(1);
    assertThat(result.getAdherenceRate()).isEqualTo(50.0);
  }

  @Test
  @DisplayName("현재 위치 조회 - 성공")
  void getCurrentLocation_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.findById(wardId)).thenReturn(Optional.of(testUser));
    
    List<LocationHistory> locationHistory = createTestLocationHistory();
    when(locationHistoryRepository.findByUserOrderByCreatedAtDesc(testUser))
        .thenReturn(locationHistory);

    // When
    LocationInfoDto result = guardianDashboardService.getCurrentLocation(guardianId, wardId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getLatitude()).isCloseTo(37.5665, within(0.0001));
    assertThat(result.getLongitude()).isCloseTo(126.9780, within(0.0001));
    assertThat(result.getAddress()).isEqualTo("서울시 중구");
  }

  @Test
  @DisplayName("건강 지표 조회 - 성공")
  void getHealthMetrics_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();
    int days = 7;

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);

    // When
    HealthMetricsDto result = guardianDashboardService.getHealthMetrics(guardianId, wardId, days);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getPeriodDays()).isEqualTo(days);
    assertThat(result.getAverageStepCount()).isGreaterThan(0);
    assertThat(result.getAverageHeartRate()).isBetween(60, 100);
    assertThat(result.getSleepQualityScore()).isBetween(5.0, 10.0);
    assertThat(result.getActivityLevel()).isNotNull();
  }

  @Test
  @DisplayName("메시지 전송 - 성공")
  void sendMessage_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();
    String message = "안녕하세요! 오늘 하루 어떠세요?";
    String type = "GREETING";

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.getReferenceById(wardId)).thenReturn(testUser);

    // When & Then
    guardianDashboardService.sendMessage(guardianId, wardId, message, type);

    verify(activityLogRepository).save(any(ActivityLog.class));
  }

  @Test
  @DisplayName("리마인더 설정 - 성공")
  void setReminder_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();
    
    SetReminderRequest request = new SetReminderRequest();
    request.setTitle("약 먹기 알림");
    request.setDescription("혈압약 복용하세요");
    request.setScheduledTime(LocalDateTime.now().plusHours(2));

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.getReferenceById(wardId)).thenReturn(testUser);

    // When
    guardianDashboardService.setReminder(guardianId, wardId, request);

    // Then
    verify(scheduleRepository).save(any(Schedule.class));
  }

  @Test
  @DisplayName("긴급 연락처 조회 - 성공")
  void getEmergencyContacts_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.findById(guardianId)).thenReturn(Optional.of(guardianUser));
    when(userRepository.findById(wardId)).thenReturn(Optional.of(testUser));
    when(guardianRepository.findByGuardianUserAndUser(guardianUser, testUser))
        .thenReturn(Optional.of(testGuardian));

    // When
    List<EmergencyContactDto> result = guardianDashboardService.getEmergencyContacts(guardianId, wardId);

    // Then
    assertThat(result).isNotEmpty();
    EmergencyContactDto contact = result.get(0);
    assertThat(contact.getName()).isEqualTo("보호자");
    // RelationshipType.CAREGIVER가 Enum이므로 실제 관계 유형과 일치하도록 수정
    assertThat(contact.getRelationship()).isEqualTo("CAREGIVER");
    assertThat(contact.isPrimary()).isTrue();
  }

  @Test
  @DisplayName("안부 확인 요청 - 성공")
  void requestCheckIn_Success() {
    // Given
    Long guardianId = guardianUser.getUserId();
    Long wardId = testUser.getUserId();

    when(guardianRepository.existsByUserIdAndGuardianUserId(wardId, guardianId))
        .thenReturn(true);
    when(userRepository.getReferenceById(wardId)).thenReturn(testUser);

    // When
    guardianDashboardService.requestCheckIn(guardianId, wardId);

    // Then
    verify(activityLogRepository).save(any(ActivityLog.class));
  }

  // === Helper Methods ===

  private List<Medication> createTestMedications() {
    List<Medication> medications = new ArrayList<>();
    
    Medication med1 = new Medication();
    med1.setId(1L);
    med1.setUser(testUser);
    med1.setMedicationName("혈압약");
    med1.setGenericName("ACE억제제");
    med1.setSimpleDescription("혈압을 낮춰주는 약");
    med1.setMedicationType(Medication.MedicationType.BLOOD_PRESSURE);
    med1.setDosageForm(Medication.DosageForm.TABLET);
    med1.setDosageAmount(BigDecimal.valueOf(1.0));
    med1.setDosageUnit("정");
    med1.setDailyFrequency(1);
    med1.setIntakeTimes(List.of(LocalTime.of(8, 0)));
    med1.setTimingInstruction(Medication.TimingInstruction.AFTER_MEAL);
    med1.setStartDate(LocalDate.now().minusDays(30));
    med1.setEndDate(LocalDate.now().plusDays(30));
    med1.setTotalDays(60);
    med1.setPriorityLevel(Medication.PriorityLevel.HIGH);
    med1.setIsActive(true);
    med1.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
    medications.add(med1);

    Medication med2 = new Medication();
    med2.setId(2L);
    med2.setUser(testUser);
    med2.setMedicationName("비타민");
    med2.setGenericName("종합비타민");
    med2.setSimpleDescription("몸에 좋은 영양소를 보충하는 약");
    med2.setMedicationType(Medication.MedicationType.VITAMIN);
    med2.setDosageForm(Medication.DosageForm.TABLET);
    med2.setDosageAmount(BigDecimal.valueOf(2.0));
    med2.setDosageUnit("정");
    med2.setDailyFrequency(1);
    med2.setIntakeTimes(List.of(LocalTime.of(20, 0)));
    med2.setTimingInstruction(Medication.TimingInstruction.AFTER_MEAL);
    med2.setStartDate(LocalDate.now().minusDays(10));
    med2.setEndDate(LocalDate.now().plusDays(50));
    med2.setTotalDays(60);
    med2.setPriorityLevel(Medication.PriorityLevel.MEDIUM);
    med2.setIsActive(true);
    med2.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
    medications.add(med2);

    return medications;
  }

  private List<MedicationAdherence> createTestAdherences() {
    List<MedicationAdherence> adherences = new ArrayList<>();
    
    MedicationAdherence adherence1 = new MedicationAdherence();
    adherence1.setId(1L);
    adherence1.setUser(testUser);
    adherence1.setMedication(testMedications.get(0));
    adherence1.setAdherenceDate(LocalDate.now());
    adherence1.setScheduledTime(LocalTime.of(8, 0));
    adherence1.setActualTakenTime(LocalDateTime.now().minusHours(2));
    adherence1.setAdherenceStatus(MedicationAdherence.AdherenceStatus.TAKEN);
    adherence1.setActualDosage(BigDecimal.valueOf(1.0));
    adherence1.setDosageUnit("정");
    adherence1.setDelayMinutes(0);
    adherences.add(adherence1);

    // 두 번째 약은 복용하지 않음
    return adherences;
  }

  private List<Schedule> createTestSchedules() {
    List<Schedule> schedules = new ArrayList<>();
    
    Schedule schedule1 = new Schedule();
    schedule1.setId(1L);
    schedule1.setUser(testUser);
    schedule1.setTitle("병원 방문");
    schedule1.setDescription("정기 검진 예약");
    schedule1.setScheduleType(Schedule.ScheduleType.APPOINTMENT);
    schedule1.setRecurrenceType(Schedule.RecurrenceType.ONCE);
    schedule1.setExecutionTime(LocalTime.of(14, 0));
    schedule1.setNextExecutionTime(LocalDateTime.now().plusHours(2));
    schedule1.setPriority(3);
    schedule1.setIsActive(true);
    schedule1.setCreatedByType(Schedule.CreatorType.USER);
    schedules.add(schedule1);

    Schedule schedule2 = new Schedule();
    schedule2.setId(2L);
    schedule2.setUser(testUser);
    schedule2.setTitle("약 복용");
    schedule2.setDescription("혈압약 복용 시간");
    schedule2.setScheduleType(Schedule.ScheduleType.MEDICATION);
    schedule2.setRecurrenceType(Schedule.RecurrenceType.DAILY);
    schedule2.setExecutionTime(LocalTime.of(8, 0));
    schedule2.setNextExecutionTime(LocalDateTime.now().minusHours(2));
    schedule2.setLastExecutionTime(LocalDateTime.now().minusHours(2));
    schedule2.setPriority(4);
    schedule2.setIsActive(true);
    schedule2.setCreatedByType(Schedule.CreatorType.GUARDIAN);
    schedules.add(schedule2);

    return schedules;
  }

  private List<ActivityLog> createTestActivityLogs() {
    List<ActivityLog> logs = new ArrayList<>();
    
    ActivityLog log1 = new ActivityLog();
    log1.setId(1L);
    log1.setUser(testUser);
    log1.setActivityType(ActivityLog.ActivityType.MEDICATION);
    log1.setActivityTitle("혈압약 복용");
    log1.setActivityDescription("아침 8시 혈압약을 복용했습니다");
    log1.setActivityDate(LocalDateTime.now().minusHours(2));
    log1.setSuccessStatus(ActivityLog.SuccessStatus.SUCCESS);
    log1.setDurationMinutes(5);
    log1.setDifficultyLevel(ActivityLog.DifficultyLevel.EASY);
    log1.setMoodBefore("보통");
    log1.setMoodAfter("좋음");
    log1.setConfidenceScore(8);
    log1.setHelpNeeded(false);
    logs.add(log1);

    ActivityLog log2 = new ActivityLog();
    log2.setId(2L);
    log2.setUser(testUser);
    log2.setActivityType(ActivityLog.ActivityType.TRANSPORTATION);
    log2.setActivityTitle("위치 변경");
    log2.setActivityDescription("집에서 병원으로 이동");
    log2.setActivityDate(LocalDateTime.now().minusHours(1));
    log2.setSuccessStatus(ActivityLog.SuccessStatus.SUCCESS);
    log2.setDurationMinutes(30);
    log2.setDifficultyLevel(ActivityLog.DifficultyLevel.MODERATE);
    log2.setMoodBefore("보통");
    log2.setMoodAfter("보통");
    log2.setConfidenceScore(7);
    log2.setHelpNeeded(false);
    logs.add(log2);

    return logs;
  }

  private List<Device> createTestDevices() {
    List<Device> devices = new ArrayList<>();
    
    Device device = Device.builder()
        .user(testUser)
        .deviceId("test-device-001")
        .deviceIdentifier("MAC-ADDRESS-001")
        .deviceName("테스트 디바이스")
        .deviceType("MOBILE")
        .manufacturer("Samsung")
        .model("Galaxy S21")
        .osType("Android")
        .osVersion("12")
        .appVersion("1.0.0")
        .fcmToken("test-fcm-token")
        .batteryLevel(85)
        .isActive(true)
        .lastSeen(LocalDateTime.now().minusMinutes(5))
        .lastSyncAt(LocalDateTime.now().minusMinutes(5))
        .build();
    devices.add(device);

    return devices;
  }

  private List<LocationHistory> createTestLocationHistory() {
    List<LocationHistory> locations = new ArrayList<>();
    
    LocationHistory location = LocationHistory.builder()
        .user(testUser)
        .latitude(BigDecimal.valueOf(37.5665))
        .longitude(BigDecimal.valueOf(126.9780))
        .accuracy(BigDecimal.valueOf(10.0))
        .altitude(BigDecimal.valueOf(50.0))
        .speed(BigDecimal.valueOf(0.0))
        .heading(BigDecimal.valueOf(0.0))
        .locationType(LocationHistory.LocationType.HOME)
        .address("서울시 중구")
        .inSafeZone(true)
        .capturedAt(LocalDateTime.now().minusMinutes(5))
        .build();
    locations.add(location);

    return locations;
  }
}