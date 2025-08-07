package com.bifai.reminder.bifai_backend.dto.location;

import com.bifai.reminder.bifai_backend.entity.Geofence.GeofenceType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;

/**
 * 안전 구역 생성/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceRequest {

  @NotBlank(message = "안전 구역 이름을 입력해주세요")
  @Size(max = 100, message = "이름은 100자 이내로 입력해주세요")
  private String name;

  @Size(max = 500, message = "설명은 500자 이내로 입력해주세요")
  private String description;

  @NotNull(message = "중심 위도를 입력해주세요")
  @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
  @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
  private Double centerLatitude;

  @NotNull(message = "중심 경도를 입력해주세요")
  @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
  @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
  private Double centerLongitude;

  @NotNull(message = "반경을 입력해주세요")
  @Min(value = 10, message = "반경은 최소 10미터 이상이어야 합니다")
  @Max(value = 10000, message = "반경은 최대 10km를 초과할 수 없습니다")
  private Integer radiusMeters;

  @Size(max = 500, message = "주소는 500자 이내로 입력해주세요")
  private String address;

  private GeofenceType type;

  private Boolean isActive;

  private Boolean alertOnEntry;

  private Boolean alertOnExit;

  private LocalTime startTime;

  private LocalTime endTime;

  @Pattern(regexp = "^[MON,TUE,WED,THU,FRI,SAT,SUN,]*$", 
           message = "요일은 MON,TUE,WED,THU,FRI,SAT,SUN 형식으로 입력해주세요")
  private String activeDays;

  @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
  @Max(value = 10, message = "우선순위는 10 이하여야 합니다")
  private Integer priority;
}