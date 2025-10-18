package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.common.BaseService;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleRequest;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleResponse;
import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.Schedule.CreatorType;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 관리 서비스
 *
 * <p>BIF 사용자의 일정 생성, 조회, 수정, 삭제를 담당합니다.
 * 반복 일정 계산 및 실행 시간 관리를 포함합니다.</p>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2025-10-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService extends BaseService {

  private final ScheduleRepository scheduleRepository;

  /**
   * 일정 생성
   *
   * @param request 일정 생성 요청
   * @return 생성된 일정 정보
   */
  @Transactional
  public ScheduleResponse createSchedule(ScheduleRequest request) {
    User user = getCurrentUser();

    Schedule schedule = Schedule.builder()
        .user(user)
        .title(request.getTitle())
        .description(request.getDescription())
        .scheduleType(request.getScheduleType())
        .recurrenceType(request.getRecurrenceType())
        .executionTime(request.getExecutionTime())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .selectedDays(request.getSelectedDays())
        .dayOfMonth(request.getDayOfMonth())
        .intervalValue(request.getIntervalValue() != null ? request.getIntervalValue() : 1)
        .isActive(request.getIsActive() != null ? request.getIsActive() : true)
        .priority(request.getPriority() != null ? request.getPriority() : 2)
        .visualIndicator(request.getVisualIndicator())
        .reminderMinutesBefore(request.getReminderMinutesBefore())
        .requiresConfirmation(request.getRequiresConfirmation() != null ? request.getRequiresConfirmation() : false)
        .createdByType(CreatorType.USER)
        .build();

    // 다음 실행 시간 계산
    schedule.calculateNextExecution();

    Schedule saved = scheduleRepository.save(schedule);

    logWithUser("일정 생성: scheduleId={}, type={}, recurrence={}, nextExecution={}",
        saved.getId(), saved.getScheduleType(), saved.getRecurrenceType(), saved.getNextExecutionTime());

    return ScheduleResponse.from(saved);
  }

  /**
   * 일정 상세 조회
   *
   * @param scheduleId 일정 ID
   * @return 일정 상세 정보
   */
  public ScheduleResponse getSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    return ScheduleResponse.from(schedule);
  }

  /**
   * 사용자의 모든 일정 조회 (페이징)
   *
   * @param pageable 페이지 정보
   * @return 일정 목록 (페이지)
   */
  public Page<ScheduleResponse> getAllSchedules(Pageable pageable) {
    User user = getCurrentUser();
    Page<Schedule> schedules = scheduleRepository.findActiveSchedulesByUser(user, pageable);

    return schedules.map(ScheduleResponse::from);
  }

  /**
   * 오늘의 일정 조회
   *
   * @return 오늘 실행될 일정 목록
   */
  public List<ScheduleResponse> getTodaySchedules() {
    User user = getCurrentUser();
    LocalDate today = LocalDate.now();
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

    List<Schedule> schedules = scheduleRepository.findTodaySchedules(user, startOfDay, endOfDay);

    logWithUser("오늘의 일정 조회: {} 건", schedules.size());

    return schedules.stream()
        .map(ScheduleResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 다가오는 일정 조회 (N일 이내)
   *
   * @param days 조회할 일수 (기본값: 7일)
   * @return 다가오는 일정 목록
   */
  public List<ScheduleResponse> getUpcomingSchedules(int days) {
    User user = getCurrentUser();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = now.plusDays(days);

    List<Schedule> schedules = scheduleRepository.findUpcomingSchedules(user, now, endTime);

    logWithUser("다가오는 일정 조회: {} 건 ({}일 이내)", schedules.size(), days);

    return schedules.stream()
        .map(ScheduleResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 특정 날짜의 일정 조회
   *
   * @param date 조회할 날짜
   * @return 해당 날짜의 일정 목록
   */
  public List<ScheduleResponse> getSchedulesByDate(LocalDate date) {
    User user = getCurrentUser();
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

    List<Schedule> schedules = scheduleRepository.findSchedulesBetweenDates(user, startOfDay, endOfDay);

    logWithUser("특정 날짜 일정 조회: {} 건 ({})", schedules.size(), date);

    return schedules.stream()
        .map(ScheduleResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 기간별 일정 조회
   *
   * @param startDate 시작 날짜
   * @param endDate   종료 날짜
   * @return 기간 내 일정 목록
   */
  public List<ScheduleResponse> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
    User user = getCurrentUser();
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Schedule> schedules = scheduleRepository.findSchedulesBetweenDates(user, start, end);

    logWithUser("기간별 일정 조회: {} 건 ({} ~ {})", schedules.size(), startDate, endDate);

    return schedules.stream()
        .map(ScheduleResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 일정 수정
   *
   * @param scheduleId 일정 ID
   * @param request    수정 요청
   * @return 수정된 일정 정보
   */
  @Transactional
  public ScheduleResponse updateSchedule(Long scheduleId, ScheduleRequest request) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    // 필드 업데이트
    schedule.setTitle(request.getTitle());
    schedule.setDescription(request.getDescription());
    schedule.setScheduleType(request.getScheduleType());
    schedule.setRecurrenceType(request.getRecurrenceType());
    schedule.setExecutionTime(request.getExecutionTime());
    schedule.setStartDate(request.getStartDate());
    schedule.setEndDate(request.getEndDate());
    schedule.setSelectedDays(request.getSelectedDays());
    schedule.setDayOfMonth(request.getDayOfMonth());
    schedule.setIntervalValue(request.getIntervalValue() != null ? request.getIntervalValue() : 1);

    if (request.getPriority() != null) {
      schedule.setPriority(request.getPriority());
    }
    if (request.getVisualIndicator() != null) {
      schedule.setVisualIndicator(request.getVisualIndicator());
    }
    if (request.getReminderMinutesBefore() != null) {
      schedule.setReminderMinutesBefore(request.getReminderMinutesBefore());
    }
    if (request.getRequiresConfirmation() != null) {
      schedule.setRequiresConfirmation(request.getRequiresConfirmation());
    }
    if (request.getIsActive() != null) {
      schedule.setIsActive(request.getIsActive());
    }

    // 다음 실행 시간 재계산
    schedule.calculateNextExecution();

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("일정 수정: scheduleId={}, nextExecution={}", updated.getId(), updated.getNextExecutionTime());

    return ScheduleResponse.from(updated);
  }

  /**
   * 일정 삭제
   *
   * @param scheduleId 일정 ID
   */
  @Transactional
  public void deleteSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    scheduleRepository.delete(schedule);

    logWithUser("일정 삭제: scheduleId={}, title={}", scheduleId, schedule.getTitle());
  }

  /**
   * 일정 완료 처리
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @Transactional
  public ScheduleResponse completeSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    // Lazy loading 초기화 (LazyInitializationException 방지)
    schedule.getSelectedDays().size();

    // 실행 완료 처리 (다음 실행 시간 자동 계산)
    schedule.markExecuted();

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("일정 완료: scheduleId={}, nextExecution={}", updated.getId(), updated.getNextExecutionTime());

    return ScheduleResponse.from(updated);
  }

  /**
   * 일정 완료 취소
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @Transactional
  public ScheduleResponse uncompleteSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    // 마지막 실행 시간 초기화하고 다음 실행 시간 재계산
    schedule.setLastExecutionTime(null);
    schedule.calculateNextExecution();

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("일정 완료 취소: scheduleId={}, nextExecution={}", updated.getId(), updated.getNextExecutionTime());

    return ScheduleResponse.from(updated);
  }

  /**
   * 일정 활성화
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @Transactional
  public ScheduleResponse activateSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    schedule.setIsActive(true);
    schedule.calculateNextExecution();

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("일정 활성화: scheduleId={}", scheduleId);

    return ScheduleResponse.from(updated);
  }

  /**
   * 일정 비활성화
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @Transactional
  public ScheduleResponse deactivateSchedule(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    schedule.setIsActive(false);
    schedule.setNextExecutionTime(null);

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("일정 비활성화: scheduleId={}", scheduleId);

    return ScheduleResponse.from(updated);
  }

  /**
   * 다음 실행 건너뛰기
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @Transactional
  public ScheduleResponse skipNextExecution(Long scheduleId) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    // 현재 nextExecutionTime을 lastExecutionTime으로 설정하고 다음 계산
    schedule.setLastExecutionTime(schedule.getNextExecutionTime());
    schedule.calculateNextExecution();

    Schedule updated = scheduleRepository.save(schedule);

    logWithUser("다음 실행 건너뛰기: scheduleId={}, skipped={}, nextExecution={}",
        scheduleId, schedule.getLastExecutionTime(), updated.getNextExecutionTime());

    return ScheduleResponse.from(updated);
  }

  /**
   * 반복 일정 목록 조회 (향후 N회)
   *
   * @param scheduleId 일정 ID
   * @param count      조회할 횟수 (기본값: 10)
   * @return 향후 실행 일정 목록
   */
  public List<LocalDateTime> getScheduleOccurrences(Long scheduleId, int count) {
    Schedule schedule = findScheduleById(scheduleId);
    validateScheduleAccess(schedule);

    List<LocalDateTime> occurrences = new java.util.ArrayList<>();
    LocalDateTime current = schedule.getNextExecutionTime();

    if (current == null) {
      return occurrences;
    }

    // 임시 스케줄 객체로 다음 실행 시간 계산
    Schedule tempSchedule = new Schedule();
    tempSchedule.setRecurrenceType(schedule.getRecurrenceType());
    tempSchedule.setExecutionTime(schedule.getExecutionTime());
    tempSchedule.setStartDate(schedule.getStartDate());
    tempSchedule.setEndDate(schedule.getEndDate());
    tempSchedule.setSelectedDays(schedule.getSelectedDays());
    tempSchedule.setDayOfMonth(schedule.getDayOfMonth());
    tempSchedule.setIntervalValue(schedule.getIntervalValue());
    tempSchedule.setIsActive(true);
    tempSchedule.setLastExecutionTime(current);

    occurrences.add(current);

    for (int i = 1; i < count; i++) {
      tempSchedule.calculateNextExecution();
      LocalDateTime next = tempSchedule.getNextExecutionTime();

      if (next == null) {
        break;
      }

      occurrences.add(next);
      tempSchedule.setLastExecutionTime(next);
    }

    logWithUser("반복 일정 목록 조회: scheduleId={}, count={}, found={}", scheduleId, count, occurrences.size());

    return occurrences;
  }

  /**
   * 일정 ID로 조회 (내부 메서드)
   *
   * @param scheduleId 일정 ID
   * @return Schedule 엔티티
   */
  private Schedule findScheduleById(Long scheduleId) {
    return scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다: scheduleId=" + scheduleId));
  }

  /**
   * 일정 접근 권한 검증
   * 본인의 일정인지 확인
   *
   * @param schedule Schedule 엔티티
   */
  private void validateScheduleAccess(Schedule schedule) {
    Long currentUserId = getCurrentUserId();
    if (!schedule.getUser().getId().equals(currentUserId)) {
      throw new UnauthorizedException("이 일정에 접근할 권한이 없습니다");
    }
  }
}
