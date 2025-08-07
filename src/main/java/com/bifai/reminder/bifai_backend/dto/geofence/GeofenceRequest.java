package com.bifai.reminder.bifai_backend.dto.geofence;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지오펜스 생성/수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceRequest {

  @NotBlank(message = "안전 구역 이름을 입력해주세요")
  @Size(min = 1, max = 100, message = "이름은 1~100자 사이로 입력해주세요")
  private String name;

  @Size(max = 500, message = "설명은 500자까지 입력할 수 있어요")
  private String description;

  @NotNull(message = "중심 위도를 입력해주세요")
  @DecimalMin(value = "-90.0", message = "위도는 -90도에서 90도 사이여야 해요")
  @DecimalMax(value = "90.0", message = "위도는 -90도에서 90도 사이여야 해요")
  private Double centerLatitude;

  @NotNull(message = "중심 경도를 입력해주세요")
  @DecimalMin(value = "-180.0", message = "경도는 -180도에서 180도 사이여야 해요")
  @DecimalMax(value = "180.0", message = "경도는 -180도에서 180도 사이여야 해요")
  private Double centerLongitude;

  @NotNull(message = "반경을 입력해주세요")
  @Min(value = 10, message = "반경은 최소 10미터 이상이어야 해요")
  @Max(value = 5000, message = "반경은 최대 5000미터까지 설정할 수 있어요")
  private Integer radiusMeters;

  @Size(max = 500, message = "주소는 500자까지 입력할 수 있어요")
  private String address;

  private Geofence.GeofenceType type;

  private Boolean isActive;

  private Boolean alertOnEntry;

  private Boolean alertOnExit;

  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", 
          message = "시작 시간은 HH:mm 형식으로 입력해주세요 (예: 09:00)")
  private String startTime;

  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", 
          message = "종료 시간은 HH:mm 형식으로 입력해주세요 (예: 18:00)")
  private String endTime;

  @Pattern(regexp = "^(MON|TUE|WED|THU|FRI|SAT|SUN)(,(MON|TUE|WED|THU|FRI|SAT|SUN))*$", 
          message = "요일은 MON,TUE,WED 형식으로 입력해주세요")
  private String activeDays;

  @Min(value = 1, message = "우선순위는 1 이상이어야 해요")
  @Max(value = 10, message = "우선순위는 10 이하여야 해요")
  private Integer priority;
}