package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleRequest;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleResponse;
import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.Schedule.CreatorType;
import com.bifai.reminder.bifai_backend.entity.Schedule.RecurrenceType;
import com.bifai.reminder.bifai_backend.entity.Schedule.ScheduleType;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.ScheduleRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ScheduleService 단위 테스트
 * Mockito를 사용한 서비스 레이어 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {

  @Mock
  private ScheduleRepository scheduleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private ScheduleService scheduleService;

  private User testUser;
  private Schedule testSchedule;
  private ScheduleRequest validRequest;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .userId(1L)
        .email("test@example.com")
        .fullName("테스트 사용자")
        .build();

    // 테스트 일정 생성
    testSchedule = Schedule.builder()
        .id(1L)
        .user(testUser)
        .title("약 먹기")
        .description("아침 약 복용")
        .scheduleType(ScheduleType.MEDICATION)
        .recurrenceType(RecurrenceType.DAILY)
        .executionTime(LocalTime.of(9, 0))
        .startDate(LocalDateTime.now())
        .isActive(true)
        .priority(2)
        .createdByType(CreatorType.USER)
        .build();

    // 유효한 요청 생성
    validRequest = ScheduleRequest.builder()
        .title("약 먹기")
        .description("아침 약 복용")
        .scheduleType(ScheduleType.MEDICATION)
        .recurrenceType(RecurrenceType.DAILY)
        .executionTime(LocalTime.of(9, 0))
        .startDate(LocalDateTime.now())
        .priority(2)
        .isActive(true)
        .build();

    // Security Context 설정
    BifUserDetails userDetails = new BifUserDetails(testUser);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
  }

  // ===========================================================================================
  // CRUD 기본 테스트
  // ===========================================================================================

  @Test
  @DisplayName("일정 생성 성공")
  void createSchedule_Success() {
    // Given
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.createSchedule(validRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getTitle()).isEqualTo("약 먹기");
    assertThat(response.getScheduleType()).isEqualTo(ScheduleType.MEDICATION);
    assertThat(response.getRecurrenceType()).isEqualTo(RecurrenceType.DAILY);
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 조회 성공")
  void getSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

    // When
    ScheduleResponse response = scheduleService.getSchedule(scheduleId);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("약 먹기");
    verify(scheduleRepository, times(1)).findById(scheduleId);
  }

  @Test
  @DisplayName("존재하지 않는 일정 조회 시 예외 발생")
  void getSchedule_NotFound_ThrowsException() {
    // Given
    Long scheduleId = 999L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> scheduleService.getSchedule(scheduleId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("일정을 찾을 수 없습니다");
  }

  @Test
  @DisplayName("타인의 일정 조회 시 권한 오류")
  void getSchedule_UnauthorizedAccess_ThrowsException() {
    // Given
    Long scheduleId = 1L;
    User otherUser = User.builder().userId(2L).build();
    testSchedule.setUser(otherUser);

    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

    // When & Then
    assertThatThrownBy(() -> scheduleService.getSchedule(scheduleId))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("접근할 권한이 없습니다");
  }

  @Test
  @DisplayName("모든 일정 조회 성공 (페이징)")
  void getAllSchedules_Success() {
    // Given
    Pageable pageable = PageRequest.of(0, 20);
    List<Schedule> schedules = Arrays.asList(testSchedule);
    Page<Schedule> schedulePage = new PageImpl<>(schedules, pageable, 1);

    when(scheduleRepository.findActiveSchedulesByUser(testUser, pageable)).thenReturn(schedulePage);

    // When
    Page<ScheduleResponse> response = scheduleService.getAllSchedules(pageable);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).hasSize(1);
    assertThat(response.getTotalElements()).isEqualTo(1);
    verify(scheduleRepository, times(1)).findActiveSchedulesByUser(testUser, pageable);
  }

  @Test
  @DisplayName("일정 수정 성공")
  void updateSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    ScheduleRequest updateRequest = ScheduleRequest.builder()
        .title("약 먹기 - 수정됨")
        .description("저녁 약 복용")
        .scheduleType(ScheduleType.MEDICATION)
        .recurrenceType(RecurrenceType.DAILY)
        .executionTime(LocalTime.of(21, 0))
        .startDate(LocalDateTime.now())
        .priority(3)
        .isActive(true)
        .build();

    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.updateSchedule(scheduleId, updateRequest);

    // Then
    assertThat(response).isNotNull();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 삭제 성공")
  void deleteSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    doNothing().when(scheduleRepository).delete(testSchedule);

    // When
    scheduleService.deleteSchedule(scheduleId);

    // Then
    verify(scheduleRepository, times(1)).delete(testSchedule);
  }

  // ===========================================================================================
  // 조회 필터 테스트
  // ===========================================================================================

  @Test
  @DisplayName("오늘의 일정 조회 성공")
  void getTodaySchedules_Success() {
    // Given
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

    List<Schedule> schedules = Arrays.asList(testSchedule, testSchedule);
    when(scheduleRepository.findTodaySchedules(testUser, startOfDay, endOfDay))
        .thenReturn(schedules);

    // When
    List<ScheduleResponse> response = scheduleService.getTodaySchedules();

    // Then
    assertThat(response).hasSize(2);
    verify(scheduleRepository, times(1)).findTodaySchedules(testUser, startOfDay, endOfDay);
  }

  @Test
  @DisplayName("다가오는 일정 조회 성공")
  void getUpcomingSchedules_Success() {
    // Given
    int days = 7;
    List<Schedule> schedules = List.of(testSchedule);
    when(scheduleRepository.findUpcomingSchedules(eq(testUser), any(), any()))
        .thenReturn(schedules);

    // When
    List<ScheduleResponse> response = scheduleService.getUpcomingSchedules(days);

    // Then
    assertThat(response).hasSize(1);
    verify(scheduleRepository, times(1)).findUpcomingSchedules(eq(testUser), any(), any());
  }

  @Test
  @DisplayName("특정 날짜 일정 조회 성공")
  void getSchedulesByDate_Success() {
    // Given
    LocalDate date = LocalDate.now();
    List<Schedule> schedules = List.of(testSchedule);
    when(scheduleRepository.findSchedulesBetweenDates(eq(testUser), any(), any()))
        .thenReturn(schedules);

    // When
    List<ScheduleResponse> response = scheduleService.getSchedulesByDate(date);

    // Then
    assertThat(response).hasSize(1);
    verify(scheduleRepository, times(1)).findSchedulesBetweenDates(eq(testUser), any(), any());
  }

  @Test
  @DisplayName("기간별 일정 조회 성공")
  void getSchedulesByDateRange_Success() {
    // Given
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = startDate.plusDays(7);
    List<Schedule> schedules = Arrays.asList(testSchedule, testSchedule);
    when(scheduleRepository.findSchedulesBetweenDates(eq(testUser), any(), any()))
        .thenReturn(schedules);

    // When
    List<ScheduleResponse> response = scheduleService.getSchedulesByDateRange(startDate, endDate);

    // Then
    assertThat(response).hasSize(2);
    verify(scheduleRepository, times(1)).findSchedulesBetweenDates(eq(testUser), any(), any());
  }

  // ===========================================================================================
  // 상태 관리 테스트
  // ===========================================================================================

  @Test
  @DisplayName("일정 완료 처리 성공")
  void completeSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.completeSchedule(scheduleId);

    // Then
    assertThat(response).isNotNull();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 완료 취소 성공")
  void uncompleteSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.uncompleteSchedule(scheduleId);

    // Then
    assertThat(response).isNotNull();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 활성화 성공")
  void activateSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    testSchedule.setIsActive(false);
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.activateSchedule(scheduleId);

    // Then
    assertThat(response).isNotNull();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("일정 비활성화 성공")
  void deactivateSchedule_Success() {
    // Given
    Long scheduleId = 1L;
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.deactivateSchedule(scheduleId);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getIsActive()).isFalse();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  // ===========================================================================================
  // 반복 일정 테스트
  // ===========================================================================================

  @Test
  @DisplayName("다음 실행 건너뛰기 성공")
  void skipNextExecution_Success() {
    // Given
    Long scheduleId = 1L;
    testSchedule.setNextExecutionTime(LocalDateTime.now().plusDays(1));
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));
    when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

    // When
    ScheduleResponse response = scheduleService.skipNextExecution(scheduleId);

    // Then
    assertThat(response).isNotNull();
    verify(scheduleRepository, times(1)).save(any(Schedule.class));
  }

  @Test
  @DisplayName("반복 일정 목록 조회 성공")
  void getScheduleOccurrences_Success() {
    // Given
    Long scheduleId = 1L;
    int count = 5;
    testSchedule.setNextExecutionTime(LocalDateTime.now().plusDays(1));
    testSchedule.setIntervalValue(1);
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

    // When
    List<LocalDateTime> occurrences = scheduleService.getScheduleOccurrences(scheduleId, count);

    // Then
    assertThat(occurrences).isNotEmpty();
    assertThat(occurrences).hasSizeLessThanOrEqualTo(count);
  }

  @Test
  @DisplayName("다음 실행 시간이 없는 일정의 반복 목록 조회 시 빈 리스트 반환")
  void getScheduleOccurrences_NoNextExecution_ReturnsEmptyList() {
    // Given
    Long scheduleId = 1L;
    int count = 5;
    testSchedule.setNextExecutionTime(null);
    when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(testSchedule));

    // When
    List<LocalDateTime> occurrences = scheduleService.getScheduleOccurrences(scheduleId, count);

    // Then
    assertThat(occurrences).isEmpty();
  }
}
