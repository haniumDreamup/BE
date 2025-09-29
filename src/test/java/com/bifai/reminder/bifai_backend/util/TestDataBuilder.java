package com.bifai.reminder.bifai_backend.util;

import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.entity.User.CognitiveLevel;
import com.bifai.reminder.bifai_backend.entity.LocationHistory.LocationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 테스트용 데이터 빌더
 * BIF 사용자 관련 엔티티의 테스트 데이터 생성
 */
public class TestDataBuilder {
    
    /**
     * 기본 BIF 사용자 생성
     */
    public static User createUser() {
        return User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .name("김철수")
                .fullName("김철수")
                .nickname("철수")
                .phoneNumber("010-1234-5678")
                .cognitiveLevel(CognitiveLevel.MODERATE)
                .timezone("Asia/Seoul")
                .languagePreference("ko")
                .isActive(true)
                .build();
    }
    
    /**
     * 특정 이메일로 사용자 생성
     */
    public static User createUserWithEmail(String email) {
        User user = createUser();
        user = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .passwordHash(user.getPasswordHash())
                .name(user.getName())
                .fullName(user.getFullName())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .cognitiveLevel(user.getCognitiveLevel())
                .timezone(user.getTimezone())
                .languagePreference(user.getLanguagePreference())
                .isActive(user.getIsActive())
                .build();
        return user;
    }
    
    /**
     * 디바이스 생성
     */
    public static Device createDevice(User user) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return Device.builder()
                .user(user)
                .deviceId("DEV-ID-" + timestamp)  // 필수 필드 추가
                .deviceIdentifier("MAC-" + timestamp)
                .deviceSerialNumber("SN-12345")
                .deviceName("철수의 스마트워치")
                .deviceType("WEARABLE")
                .manufacturer("Samsung")
                .model("Galaxy Watch 5")
                .osType("WearOS")
                .osVersion("3.0")
                .appVersion("1.0.0")
                .pushToken("push-token-" + timestamp)
                .batteryLevel(85)
                .isActive(true)
                .lastSyncAt(LocalDateTime.now())
                .registeredAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 보호자 생성
     */
    public static Guardian createGuardian(User user, User guardianUser) {
        return Guardian.builder()
                .user(user)
                .guardianUser(guardianUser)
                .name(guardianUser.getName() != null ? guardianUser.getName() : "김보호")
                .relationship("부모")
                .primaryPhone(guardianUser.getPhoneNumber() != null ? guardianUser.getPhoneNumber() : "010-1111-2222")
                .email(guardianUser.getEmail())
                .isPrimary(true)
                .canViewLocation(true)
                .canModifySettings(true)
                .canReceiveAlerts(true)
                .emergencyPriority(1)
                .approvalStatus(Guardian.ApprovalStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }
    
    /**
     * 위치 이력 생성
     */
    public static LocationHistory createLocationHistory(User user, Device device) {
        return LocationHistory.builder()
                .user(user)
                .device(device)
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .altitude(BigDecimal.valueOf(50.5))
                .accuracy(BigDecimal.valueOf(10.0))
                .speed(BigDecimal.valueOf(5.0))
                .locationType(LocationHistory.LocationType.OUTDOOR)
                .address("서울특별시 중구 세종대로 110")
                .capturedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 일정 생성
     */
    public static Schedule createSchedule(User user) {
        Schedule schedule = new Schedule();
        schedule.setUser(user);
        schedule.setTitle("병원 방문");
        schedule.setDescription("정기 검진");
        schedule.setScheduleType(Schedule.ScheduleType.APPOINTMENT);
        schedule.setRecurrenceType(Schedule.RecurrenceType.ONCE);
        schedule.setExecutionTime(LocalTime.of(10, 0));
        schedule.setStartDate(LocalDateTime.now().plusDays(1));
        schedule.setIsActive(true);
        schedule.setPriority(3); // HIGH priority
        schedule.setReminderMinutesBefore(30);
        return schedule;
    }
    
    /**
     * 약물 정보 생성
     */
    public static Medication createMedication(User user) {
        Medication medication = new Medication();
        medication.setUser(user);
        medication.setMedicationName("혈압약");
        medication.setGenericName("암로디핀");
        medication.setDosageAmount(BigDecimal.valueOf(5));
        medication.setDosageUnit("mg");
        medication.setDailyFrequency(1);
        medication.setSimpleDescription("혈압을 낮춰주는 약이에요");
        medication.setStartDate(LocalDate.now().minusMonths(6));
        medication.setMedicationType(Medication.MedicationType.BLOOD_PRESSURE);
        medication.setDosageForm(Medication.DosageForm.TABLET);
        medication.setIsActive(true);
        medication.setPriorityLevel(Medication.PriorityLevel.MEDIUM);
        medication.setMedicationStatus(Medication.MedicationStatus.ACTIVE);
        medication.setPrescribingDoctor("김의사");
        medication.setPharmacyName("동네약국");
        medication.setSideEffects("어지러움 가능");
        return medication;
    }
    
    /**
     * 사용자 설정 생성
     */
    public static UserPreference createUserPreference(User user) {
        return UserPreference.builder()
                .user(user)
                .notificationEnabled(true)
                .voiceGuidanceEnabled(true)
                .textSize(UserPreference.TextSize.LARGE)
                .uiComplexityLevel(UserPreference.UiComplexityLevel.SIMPLE)
                .languageCode("ko-KR")
                .themePreference(UserPreference.ThemePreference.LIGHT)
                .reminderFrequency(3)
                .emergencyAutoCall(false)
                .build();
    }
}