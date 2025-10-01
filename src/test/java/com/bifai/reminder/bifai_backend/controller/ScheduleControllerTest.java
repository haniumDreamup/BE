package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleRequest;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleResponse;
import com.bifai.reminder.bifai_backend.entity.Schedule.RecurrenceType;
import com.bifai.reminder.bifai_backend.entity.Schedule.ScheduleType;
import com.bifai.reminder.bifai_backend.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ScheduleController 단위 테스트
 * MockMvc를 사용한 컨트롤러 레이어 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleController 단위 테스트")
class ScheduleControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private ScheduleService scheduleService;

  @InjectMocks
  private ScheduleController scheduleController;

  private ScheduleRequest createValidRequest() {
    return ScheduleRequest.builder()
        .title("약 먹기")
        .description("아침 약 복용")
        .scheduleType(ScheduleType.MEDICATION)
        .recurrenceType(RecurrenceType.DAILY)
        .executionTime(LocalTime.of(9, 0))
        .startDate(LocalDateTime.now())
        .priority(2)
        .isActive(true)
        .build();
  }

  private ScheduleResponse createMockResponse() {
    return ScheduleResponse.builder()
        .id(1L)
        .userId(1L)
        .userName("테스트 사용자")
        .title("약 먹기")
        .description("아침 약 복용")
        .scheduleType(ScheduleType.MEDICATION)
        .scheduleTypeDescription("약물 복용")
        .recurrenceType(RecurrenceType.DAILY)
        .recurrenceTypeDescription("매일")
        .executionTime(LocalTime.of(9, 0))
        .nextExecutionTime(LocalDateTime.now().plusDays(1))
        .isActive(true)
        .priority(2)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    mockMvc = MockMvcBuilders.standaloneSetup(scheduleController).build();
  }

  // ===========================================================================================
  // CRUD 기본 테스트
  // ===========================================================================================

  @Test
  @DisplayName("POST /api/v1/schedules - 일정 생성 성공")
  void createSchedule_Success() throws Exception {
    // Given
    ScheduleRequest request = createValidRequest();
    ScheduleResponse response = createMockResponse();

    when(scheduleService.createSchedule(any(ScheduleRequest.class))).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(1L))
        .andExpect(jsonPath("$.data.title").value("약 먹기"))
        .andExpect(jsonPath("$.message").value("일정이 등록되었습니다"));
  }

  @Test
  @DisplayName("POST /api/v1/schedules - 제목 누락 시 검증 오류")
  void createSchedule_ValidationError_MissingTitle() throws Exception {
    // Given
    ScheduleRequest request = createValidRequest();
    request.setTitle(null);

    // When & Then
    mockMvc.perform(post("/api/v1/schedules")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/v1/schedules/{scheduleId} - 일정 조회 성공")
  void getSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();

    when(scheduleService.getSchedule(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/{scheduleId}", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(1L))
        .andExpect(jsonPath("$.data.title").value("약 먹기"))
        .andExpect(jsonPath("$.message").value("일정 정보를 가져왔습니다"));
  }

  @Test
  @DisplayName("GET /api/v1/schedules - 일정 목록 조회 성공")
  void getAllSchedules_Success() throws Exception {
    // Given
    ScheduleResponse response = createMockResponse();
    Page<ScheduleResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

    when(scheduleService.getAllSchedules(any())).thenReturn(page);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].id").value(1L))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.message").value("일정 목록을 가져왔습니다"));
  }

  @Test
  @DisplayName("PUT /api/v1/schedules/{scheduleId} - 일정 수정 성공")
  void updateSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleRequest request = createValidRequest();
    request.setTitle("약 먹기 - 수정됨");

    ScheduleResponse response = createMockResponse();
    response.setTitle("약 먹기 - 수정됨");

    when(scheduleService.updateSchedule(eq(scheduleId), any(ScheduleRequest.class)))
        .thenReturn(response);

    // When & Then
    mockMvc.perform(put("/api/v1/schedules/{scheduleId}", scheduleId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.title").value("약 먹기 - 수정됨"))
        .andExpect(jsonPath("$.message").value("일정이 수정되었습니다"));
  }

  @Test
  @DisplayName("DELETE /api/v1/schedules/{scheduleId} - 일정 삭제 성공")
  void deleteSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    doNothing().when(scheduleService).deleteSchedule(scheduleId);

    // When & Then
    mockMvc.perform(delete("/api/v1/schedules/{scheduleId}", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다"));
  }

  // ===========================================================================================
  // 조회 필터 테스트
  // ===========================================================================================

  @Test
  @DisplayName("GET /api/v1/schedules/today - 오늘의 일정 조회 성공")
  void getTodaySchedules_Success() throws Exception {
    // Given
    List<ScheduleResponse> schedules = Arrays.asList(createMockResponse(), createMockResponse());
    when(scheduleService.getTodaySchedules()).thenReturn(schedules);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/today"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.message").value("오늘의 일정 2건을 가져왔습니다"));
  }

  @Test
  @DisplayName("GET /api/v1/schedules/upcoming - 다가오는 일정 조회 성공")
  void getUpcomingSchedules_Success() throws Exception {
    // Given
    int days = 7;
    List<ScheduleResponse> schedules = List.of(createMockResponse());
    when(scheduleService.getUpcomingSchedules(days)).thenReturn(schedules);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/upcoming")
            .param("days", String.valueOf(days)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.message").value("7일 이내 일정 1건을 가져왔습니다"));
  }

  @Test
  @DisplayName("GET /api/v1/schedules/date - 특정 날짜 일정 조회 성공")
  void getSchedulesByDate_Success() throws Exception {
    // Given
    LocalDate date = LocalDate.now();
    List<ScheduleResponse> schedules = List.of(createMockResponse());
    when(scheduleService.getSchedulesByDate(date)).thenReturn(schedules);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/date")
            .param("date", date.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1));
  }

  @Test
  @DisplayName("GET /api/v1/schedules/range - 기간별 일정 조회 성공")
  void getSchedulesByDateRange_Success() throws Exception {
    // Given
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = startDate.plusDays(7);
    List<ScheduleResponse> schedules = Arrays.asList(createMockResponse(), createMockResponse());
    when(scheduleService.getSchedulesByDateRange(startDate, endDate)).thenReturn(schedules);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/range")
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.message").value("기간 내 일정 2건을 가져왔습니다"));
  }

  // ===========================================================================================
  // 상태 관리 테스트
  // ===========================================================================================

  @Test
  @DisplayName("POST /api/v1/schedules/{scheduleId}/complete - 일정 완료 처리 성공")
  void completeSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();
    when(scheduleService.completeSchedule(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/schedules/{scheduleId}/complete", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("일정이 완료 처리되었습니다"));
  }

  @Test
  @DisplayName("POST /api/v1/schedules/{scheduleId}/uncomplete - 일정 완료 취소 성공")
  void uncompleteSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();
    when(scheduleService.uncompleteSchedule(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/schedules/{scheduleId}/uncomplete", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("일정 완료가 취소되었습니다"));
  }

  @Test
  @DisplayName("PUT /api/v1/schedules/{scheduleId}/activate - 일정 활성화 성공")
  void activateSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();
    when(scheduleService.activateSchedule(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(put("/api/v1/schedules/{scheduleId}/activate", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("일정이 활성화되었습니다"));
  }

  @Test
  @DisplayName("PUT /api/v1/schedules/{scheduleId}/deactivate - 일정 비활성화 성공")
  void deactivateSchedule_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();
    response.setIsActive(false);
    when(scheduleService.deactivateSchedule(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(put("/api/v1/schedules/{scheduleId}/deactivate", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("일정이 비활성화되었습니다"));
  }

  // ===========================================================================================
  // 반복 일정 테스트
  // ===========================================================================================

  @Test
  @DisplayName("POST /api/v1/schedules/{scheduleId}/skip-next - 다음 실행 건너뛰기 성공")
  void skipNextExecution_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    ScheduleResponse response = createMockResponse();
    when(scheduleService.skipNextExecution(scheduleId)).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/schedules/{scheduleId}/skip-next", scheduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("다음 실행이 건너뛰어졌습니다"));
  }

  @Test
  @DisplayName("GET /api/v1/schedules/{scheduleId}/occurrences - 반복 일정 목록 조회 성공")
  void getScheduleOccurrences_Success() throws Exception {
    // Given
    Long scheduleId = 1L;
    int count = 10;
    List<LocalDateTime> occurrences = Arrays.asList(
        LocalDateTime.now().plusDays(1),
        LocalDateTime.now().plusDays(2),
        LocalDateTime.now().plusDays(3)
    );
    when(scheduleService.getScheduleOccurrences(scheduleId, count)).thenReturn(occurrences);

    // When & Then
    mockMvc.perform(get("/api/v1/schedules/{scheduleId}/occurrences", scheduleId)
            .param("count", String.valueOf(count)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.message").value("향후 3회 실행 시간을 가져왔습니다"));
  }
}
