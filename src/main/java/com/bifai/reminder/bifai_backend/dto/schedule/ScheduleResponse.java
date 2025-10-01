package com.bifai.reminder.bifai_backend.dto.schedule;

import com.bifai.reminder.bifai_backend.entity.Schedule;
import com.bifai.reminder.bifai_backend.entity.Schedule.CreatorType;
import com.bifai.reminder.bifai_backend.entity.Schedule.RecurrenceType;
import com.bifai.reminder.bifai_backend.entity.Schedule.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * 일정 응답 DTO
 *
 * <p>BIF 사용자에게 반환되는 일정 정보입니다.
 * 한글 설명과 함께 이해하기 쉬운 형태로 제공됩니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

  /**
   * 일정 ID
   */
  private Long id;

  /**
   * 사용자 ID
   */
  private Long userId;

  /**
   * 사용자 이름
   */
  private String userName;

  /**
   * 일정 제목
   */
  private String title;

  /**
   * 일정 상세 설명
   */
  private String description;

  /**
   * 일정 유형
   */
  private ScheduleType scheduleType;

  /**
   * 일정 유형 한글 설명
   */
  private String scheduleTypeDescription;

  /**
   * 반복 패턴
   */
  private RecurrenceType recurrenceType;

  /**
   * 반복 패턴 한글 설명
   */
  private String recurrenceTypeDescription;

  /**
   * 실행 시간
   */
  private LocalTime executionTime;

  /**
   * 다음 실행 예정 시간 (계산된 값)
   */
  private LocalDateTime nextExecutionTime;

  /**
   * 마지막 실행 시간
   */
  private LocalDateTime lastExecutionTime;

  /**
   * 시작 날짜
   */
  private LocalDateTime startDate;

  /**
   * 종료 날짜
   */
  private LocalDateTime endDate;

  /**
   * 선택된 요일 (주간 반복의 경우)
   */
  private Set<DayOfWeek> selectedDays;

  /**
   * 월간 반복의 경우 날짜 (1-31)
   */
  private Integer dayOfMonth;

  /**
   * 반복 간격
   */
  private Integer intervalValue;

  /**
   * 활성화 상태
   */
  private Boolean isActive;

  /**
   * 우선순위 (1=낮음, 2=보통, 3=높음, 4=매우 높음)
   */
  private Integer priority;

  /**
   * 시각적 표시 (색상, 아이콘 등)
   */
  private String visualIndicator;

  /**
   * 미리 알림 시간 (분 단위)
   */
  private Integer reminderMinutesBefore;

  /**
   * 완료 확인 필요 여부
   */
  private Boolean requiresConfirmation;

  /**
   * 생성자 타입 (USER, GUARDIAN, SYSTEM)
   */
  private CreatorType createdByType;

  /**
   * 생성자 타입 한글 설명
   */
  private String createdByTypeDescription;

  /**
   * 생성 일시
   */
  private LocalDateTime createdAt;

  /**
   * 수정 일시
   */
  private LocalDateTime updatedAt;

  /**
   * 곧 실행될 일정인지 여부 (1시간 이내)
   */
  private Boolean isDueSoon;

  /**
   * 높은 우선순위 여부
   */
  private Boolean isHighPriority;

  /**
   * 간단한 설명 (BIF 사용자용)
   */
  private String simpleDescription;

  /**
   * Entity를 Response DTO로 변환
   *
   * @param schedule Schedule 엔티티
   * @return ScheduleResponse DTO
   */
  public static ScheduleResponse from(Schedule schedule) {
    if (schedule == null) {
      return null;
    }

    return ScheduleResponse.builder()
        .id(schedule.getId())
        .userId(schedule.getUser().getId())
        .userName(schedule.getUser().getFullName())
        .title(schedule.getTitle())
        .description(schedule.getDescription())
        .scheduleType(schedule.getScheduleType())
        .scheduleTypeDescription(schedule.getScheduleType().getDescription())
        .recurrenceType(schedule.getRecurrenceType())
        .recurrenceTypeDescription(schedule.getRecurrenceType().getDescription())
        .executionTime(schedule.getExecutionTime())
        .nextExecutionTime(schedule.getNextExecutionTime())
        .lastExecutionTime(schedule.getLastExecutionTime())
        .startDate(schedule.getStartDate())
        .endDate(schedule.getEndDate())
        .selectedDays(schedule.getSelectedDays())
        .dayOfMonth(schedule.getDayOfMonth())
        .intervalValue(schedule.getIntervalValue())
        .isActive(schedule.getIsActive())
        .priority(schedule.getPriority())
        .visualIndicator(schedule.getVisualIndicator())
        .reminderMinutesBefore(schedule.getReminderMinutesBefore())
        .requiresConfirmation(schedule.getRequiresConfirmation())
        .createdByType(schedule.getCreatedByType())
        .createdByTypeDescription(schedule.getCreatedByType().getDescription())
        .createdAt(schedule.getCreatedAt())
        .updatedAt(schedule.getUpdatedAt())
        .isDueSoon(schedule.isDueSoon())
        .isHighPriority(schedule.isHighPriority())
        .simpleDescription(schedule.getSimpleDescription())
        .build();
  }
}
