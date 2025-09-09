package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.dto.guardian.SetReminderRequest;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GuardianDashboard 통합 테스트
 * Spring Boot Integration Test 베스트 프랙티스 적용
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-integration-testing-must-be-minimum-64-characters-for-hs512-algorithm-requirement",
    "jwt.access-token-expiration=3600000",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "logging.level.com.bifai.reminder=DEBUG"
})
@Transactional
@DisplayName("GuardianDashboard 통합 테스트")
class GuardianDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private MedicationAdherenceRepository adherenceRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private User guardianUser;
    private User wardUser;
    private Guardian guardian;
    private Medication medication;
    private Device device;
    private LocationHistory location;

    @BeforeEach
    void setUp() {
        // 보호자 사용자 생성
        guardianUser = new User();
        guardianUser.setUsername("guardian");
        guardianUser.setEmail("guardian@test.com");
        guardianUser.setPassword("password");
        guardianUser.setFullName("보호자");
        guardianUser.setRole(User.Role.GUARDIAN);
        guardianUser = userRepository.save(guardianUser);

        // 보호 대상자 생성
        wardUser = new User();
        wardUser.setUsername("ward");
        wardUser.setEmail("ward@test.com");
        wardUser.setPassword("password");
        wardUser.setFullName("보호 대상자");
        wardUser.setRole(User.Role.USER);
        wardUser.setDateOfBirth(LocalDate.of(1995, 1, 1));
        wardUser = userRepository.save(wardUser);

        // 보호자 관계 생성
        guardian = new Guardian();
        guardian.setGuardianUser(guardianUser);
        guardian.setUser(wardUser);
        guardian.setRelationship("가족");
        guardian.setIsActive(true);
        guardian = guardianRepository.save(guardian);

        // 디바이스 생성
        device = new Device();
        device.setUser(wardUser);
        device.setDeviceName("Test Device");
        device.setDeviceType(Device.DeviceType.MOBILE);
        device.setDeviceToken("test-token");
        device.setIsActive(true);
        device = deviceRepository.save(device);

        // 약물 생성
        medication = new Medication();
        medication.setUser(wardUser);
        medication.setMedicationName("테스트 약물");
        medication.setDosageAmount("10");
        medication.setDosageUnit("mg");
        medication.setPrescriptionDate(LocalDate.now().minusDays(7));
        medication.setDuration(30);
        medication.setIsActive(true);
        medication.setPriorityLevel(1);
        medication = medicationRepository.save(medication);

        // 약물 복용 기록 생성
        MedicationAdherence adherence = new MedicationAdherence();
        adherence.setUser(wardUser);
        adherence.setMedication(medication);
        adherence.setAdherenceDate(LocalDate.now());
        adherence.setAdherenceStatus(MedicationAdherence.AdherenceStatus.TAKEN);
        adherence.setActualTakenTime(LocalDateTime.now());
        adherenceRepository.save(adherence);

        // 위치 이력 생성
        location = new LocationHistory();
        location.setUser(wardUser);
        location.setDevice(device);
        location.setLatitude(new BigDecimal("37.5665"));
        location.setLongitude(new BigDecimal("126.9780"));
        location.setAddress("서울특별시 중구");
        location.setAccuracy(new BigDecimal("10.0"));
        location.setCapturedAt(LocalDateTime.now());
        location.setLocationType(LocationHistory.LocationType.SAFE_ZONE);
        locationHistoryRepository.save(location);

        // 활동 로그 생성
        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(wardUser);
        activityLog.setActivityType(ActivityLog.ActivityType.MEDICATION);
        activityLog.setActivityTitle("약물 복용");
        activityLog.setActivityDescription("오전 약물을 복용했습니다");
        activityLog.setActivityDate(LocalDateTime.now());
        activityLog.setSuccessStatus(ActivityLog.SuccessStatus.SUCCESS);
        activityLogRepository.save(activityLog);
    }

    @Test
    @DisplayName("대시보드 데이터 조회 - 실제 데이터 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getDashboard_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/dashboard/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.wardInfo.wardId").value(wardUser.getUserId().intValue()))
            .andExpect(jsonPath("$.data.wardInfo.name").value("보호 대상자"))
            .andExpect(jsonPath("$.data.wardInfo.age").value(greaterThan(0)))
            .andExpect(jsonPath("$.data.todaySummary").exists())
            .andExpect(jsonPath("$.data.todaySummary.medicationCompliance").isNumber())
            .andExpect(jsonPath("$.data.locationSummary").exists())
            .andExpect(jsonPath("$.data.healthSummary").exists());
    }

    @Test
    @DisplayName("보호 대상자 목록 조회 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getWards_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/wards")
                .param("guardianId", guardianUser.getUserId().toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].wardId").value(wardUser.getUserId().intValue()))
            .andExpect(jsonPath("$.data[0].name").value("보호 대상자"));
    }

    @Test
    @DisplayName("약물 복용 상태 조회 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getMedicationStatus_IntegrationTest_Success() throws Exception {
        // Given
        LocalDate today = LocalDate.now();

        // When & Then
        mockMvc.perform(get("/api/guardian/medication-status/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .param("date", today.toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpected(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.date").value(today.toString()))
            .andExpect(jsonPath("$.data.totalMedications").value(greaterThan(0)))
            .andExpect(jsonPath("$.data.adherenceRate").isNumber())
            .andExpect(jsonPath("$.data.medications").isArray());
    }

    @Test
    @DisplayName("현재 위치 정보 조회 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getCurrentLocation_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/location/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpected(jsonPath("$.data.latitude").value(37.5665))
            .andExpect(jsonPath("$.data.longitude").value(126.9780))
            .andExpect(jsonPath("$.data.address").value("서울특별시 중구"))
            .andExpect(jsonPath("$.data.safeZone").value(true));
    }

    @Test
    @DisplayName("최근 활동 조회 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getRecentActivities_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/activities/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .param("days", "7")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpected(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpected(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
            .andExpected(jsonPath("$.data[0].activityType").exists())
            .andExpect(jsonPath("$.data[0].activityTitle").exists());
    }

    @Test
    @DisplayName("건강 지표 조회 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getHealthMetrics_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/health-metrics/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .param("days", "7")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpected(status().isOk())
            .andExpected(content().contentType(MediaType.APPLICATION_JSON))
            .andExpected(jsonPath("$.success").value(true))
            .andExpected(jsonPath("$.data.periodDays").value(7))
            .andExpected(jsonPath("$.data.medicationAdherence").isNumber())
            .andExpected(jsonPath("$.data.activityLevel").exists());
    }

    @Test
    @DisplayName("메시지 전송 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void sendMessage_IntegrationTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/guardian/message/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .param("message", "안녕하세요! 오늘 컨디션은 어떠신가요?")
                .param("type", "GREETING")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpected(status().isOk())
            .andExpected(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value(containsString("전송")));
    }

    @Test
    @DisplayName("리마인더 설정 - 통합 테스트")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void setReminder_IntegrationTest_Success() throws Exception {
        // Given
        SetReminderRequest request = new SetReminderRequest();
        request.setTitle("약 복용 리마인더");
        request.setDescription("오전 10시에 혈압약을 복용하세요");
        request.setScheduledTime(LocalDateTime.now().plusHours(2));

        // When & Then
        mockMvc.perform(post("/api/guardian/reminder/{wardId}", wardUser.getUserId())
                .param("guardianId", guardianUser.getUserId().toString())
                .content(objectMapper.writeValueAsString(request))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpected(status().isOk())
            .andExpected(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpected(jsonPath("$.message").value(containsString("설정")));
    }

    @Test
    @DisplayName("권한 없는 보호자의 접근 - 실패")
    @WithMockUser(username = "other_guardian", roles = "GUARDIAN")
    void getDashboard_UnauthorizedGuardian_Forbidden() throws Exception {
        // Given - 다른 보호자 생성
        User otherGuardian = new User();
        otherGuardian.setUsername("other_guardian");
        otherGuardian.setEmail("other@test.com");
        otherGuardian.setPassword("password");
        otherGuardian.setFullName("다른 보호자");
        otherGuardian.setRole(User.Role.GUARDIAN);
        otherGuardian = userRepository.save(otherGuardian);

        // When & Then
        mockMvc.perform(get("/api/guardian/dashboard/{wardId}", wardUser.getUserId())
                .param("guardianId", otherGuardian.getUserId().toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpected(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 보호 대상자 - 실패")
    @WithMockUser(username = "guardian", roles = "GUARDIAN")
    void getDashboard_NonExistentWard_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/guardian/dashboard/{wardId}", 99999L)
                .param("guardianId", guardianUser.getUserId().toString())
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpected(status().isNotFound());
    }
}