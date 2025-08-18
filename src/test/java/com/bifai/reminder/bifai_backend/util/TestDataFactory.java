package com.bifai.reminder.bifai_backend.util;

import com.bifai.reminder.bifai_backend.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

/**
 * 테스트 데이터 생성 팩토리
 * 일관된 테스트 데이터 생성을 위한 유틸리티
 */
public class TestDataFactory {
  
  private static int counter = 0;
  
  /**
   * 고유한 카운터 생성
   */
  private static synchronized int nextCounter() {
    return ++counter;
  }
  
  /**
   * Role 생성
   */
  public static Role createRole() {
    return createRole("ROLE_USER");
  }
  
  public static Role createRole(String name) {
    return Role.builder()
      .name(name)
      .koreanName("사용자")
      .description("테스트 역할")
      .isActive(true)
      .build();
  }
  
  /**
   * User 생성 (Role 없이)
   */
  public static User createUser() {
    int id = nextCounter();
    return User.builder()
      .username("testuser_" + id)
      .email("test" + id + "@example.com")
      .name("테스트사용자" + id)
      .passwordHash("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
      .phoneNumber("010-1234-" + String.format("%04d", id))
      .isActive(true)
      .lastLoginAt(LocalDateTime.now())
      .roles(new HashSet<>())
      .build();
  }
  
  /**
   * User 생성 (Role 포함)
   */
  public static User createUserWithRole(Role role) {
    User user = createUser();
    if (role != null) {
      user.getRoles().add(role);
    }
    return user;
  }
  
  /**
   * Device 생성
   */
  public static Device createDevice(User user) {
    int id = nextCounter();
    String uuid = UUID.randomUUID().toString();
    return Device.builder()
      .user(user)
      .deviceId("DEV_ID_" + uuid) // 필수 필드 추가
      .deviceIdentifier("DEVICE_" + uuid)
      .deviceName("테스트기기" + id)
      .deviceType("WEARABLE")
      .deviceModel("TestWatch " + id)
      .deviceSerialNumber("SN-" + String.format("%05d", id))
      .osVersion("1.0." + id)
      .appVersion("1.0.0")
      .batteryLevel(85)
      .isActive(true)
      .lastSyncAt(LocalDateTime.now())
      .build();
  }
  
  /**
   * Guardian 생성
   */
  public static Guardian createGuardian(User user, User guardianUser) {
    return Guardian.builder()
      .user(user)
      .guardianUser(guardianUser)
      .relationshipType(Guardian.RelationshipType.PARENT)
      .canViewLocation(true)
      .canReceiveAlerts(true)
      .canModifySettings(false)
      .isActive(true)
      .build();
  }
  
  /**
   * Schedule 생성
   */
  // Schedule은 @Builder가 없어서 주석 처리
  /*
  public static Schedule createSchedule(User user) {
    int id = nextCounter();
    return Schedule.builder()
      .user(user)
      .title("일정" + id)
      .description("테스트 일정 " + id)
      .scheduleType("REMINDER")
      .startTime(LocalDateTime.now().plusHours(1))
      .endTime(LocalDateTime.now().plusHours(2))
      .isAllDay(false)
      .isRecurring(false)
      .reminderMinutesBefore(30)
      .priority("MEDIUM")
      .isActive(true)
      .createdAt(LocalDateTime.now())
      .build();
  }
  */
  
  // Medication도 @Builder가 없어서 주석 처리
  /*
  public static Medication createMedication(User user) {
    int id = nextCounter();
    return Medication.builder()
      .user(user)
      .medicationName("약물" + id)
      .dosage("1정")
      .frequency("하루 3번")
      .instructions("식후 30분")
      .startDate(LocalDateTime.now().toLocalDate())
      .endDate(LocalDateTime.now().plusDays(30).toLocalDate())
      .isActive(true)
      .createdAt(LocalDateTime.now())
      .build();
  }
  */
  
  /**
   * LocationHistory 생성
   */
  public static LocationHistory createLocationHistory(User user, Device device) {
    int id = nextCounter();
    return LocationHistory.builder()
      .user(user)
      .device(device)
      .latitude(BigDecimal.valueOf(37.5665 + (id * 0.001)))
      .longitude(BigDecimal.valueOf(126.9780 + (id * 0.001)))
      .accuracy(BigDecimal.valueOf(10.0))
      .altitude(BigDecimal.valueOf(50.0))
      .speed(BigDecimal.valueOf(0.0))
      .heading(BigDecimal.valueOf(0.0))
      .address("서울시 중구 테스트동 " + id)
      .locationType(LocationHistory.LocationType.OUTDOOR)
      .build();
  }
  
  /**
   * EmergencyContact 생성
   */
  public static EmergencyContact createEmergencyContact(User user) {
    int id = nextCounter();
    return EmergencyContact.builder()
      .user(user)
      .name("긴급연락처" + id)
      .phoneNumber("010-9999-" + String.format("%04d", id))
      .relationship("가족")
      .isPrimary(id == 1)
      .priority(id)
      .isActive(true)
      .build();
  }
  
  /**
   * 테스트 카운터 리셋
   */
  public static void resetCounter() {
    counter = 0;
  }
}