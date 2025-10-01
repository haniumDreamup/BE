package com.bifai.reminder.bifai_backend.dto.schedule;

import com.bifai.reminder.bifai_backend.entity.Schedule.RecurrenceType;
import com.bifai.reminder.bifai_backend.entity.Schedule.ScheduleType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * 일정 생성/수정 요청 DTO
 *
 * <p>BIF 사용자를 위한 간단하고 직관적인 일정 등록 요청입니다.
 * 복잡한 Cron 표현식 대신 사용자 친화적인 반복 패턴을 제공합니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

  /**
   * 일정 제목 (필수)
   */
  @NotBlank(message = "일정 제목을 입력해 주세요")
  @Size(max = 100, message = "제목은 100자 이내로 입력해 주세요")
  private String title;

  /**
   * 일정 상세 설명 (선택)
   */
  @Size(max = 1000, message = "설명은 1000자 이내로 입력해 주세요")
  private String description;

  /**
   * 일정 유형 (필수)
   * MEDICATION, MEAL, EXERCISE, APPOINTMENT, REMINDER 등
   */
  @NotNull(message = "일정 유형을 선택해 주세요")
  private ScheduleType scheduleType;

  /**
   * 반복 패턴 (필수)
   * ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM_DAYS 등
   */
  @NotNull(message = "반복 패턴을 선택해 주세요")
  private RecurrenceType recurrenceType;

  /**
   * 실행 시간 (필수)
   * 예: 09:00, 14:30
   */
  @NotNull(message = "실행 시간을 입력해 주세요")
  private LocalTime executionTime;

  /**
   * 시작 날짜 (필수)
   */
  @NotNull(message = "시작 날짜를 입력해 주세요")
  private LocalDateTime startDate;

  /**
   * 종료 날짜 (선택)
   * null이면 무기한 반복
   */
  private LocalDateTime endDate;

  /**
   * 요일 선택 (WEEKLY 또는 CUSTOM_DAYS인 경우 사용)
   * 예: [MONDAY, WEDNESDAY, FRIDAY]
   */
  private Set<DayOfWeek> selectedDays;

  /**
   * 월간 반복의 경우 날짜 (1-31)
   * MONTHLY인 경우 사용
   */
  @Min(value = 1, message = "날짜는 1일부터 31일까지만 가능합니다")
  @Max(value = 31, message = "날짜는 1일부터 31일까지만 가능합니다")
  private Integer dayOfMonth;

  /**
   * 반복 간격 (매 N일, 매 N주 등)
   * 기본값: 1
   */
  @Min(value = 1, message = "반복 간격은 1 이상이어야 합니다")
  @Max(value = 365, message = "반복 간격은 365 이하여야 합니다")
  private Integer intervalValue;

  /**
   * 우선순위 (1=낮음, 2=보통, 3=높음, 4=매우 높음)
   * 기본값: 2
   */
  @Min(value = 1, message = "우선순위는 1부터 4까지만 가능합니다")
  @Max(value = 4, message = "우선순위는 1부터 4까지만 가능합니다")
  private Integer priority;

  /**
   * 시각적 표시 (색상 코드, 아이콘 등)
   * 예: "blue", "pill-icon", "#FF5733"
   */
  @Size(max = 50, message = "시각적 표시는 50자 이내로 입력해 주세요")
  private String visualIndicator;

  /**
   * 미리 알림 시간 (분 단위)
   * 예: 10, 30, 60
   */
  @Min(value = 0, message = "미리 알림 시간은 0분 이상이어야 합니다")
  @Max(value = 1440, message = "미리 알림 시간은 24시간(1440분) 이하여야 합니다")
  private Integer reminderMinutesBefore;

  /**
   * 완료 확인 필요 여부
   * true: 사용자가 직접 완료 버튼을 눌러야 함
   */
  private Boolean requiresConfirmation;

  /**
   * 활성화 상태
   * 기본값: true
   */
  private Boolean isActive;
}
