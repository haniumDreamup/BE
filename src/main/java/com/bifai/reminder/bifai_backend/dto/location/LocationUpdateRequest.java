package com.bifai.reminder.bifai_backend.dto.location;

import com.bifai.reminder.bifai_backend.entity.Location.ActivityType;
import com.bifai.reminder.bifai_backend.entity.Location.LocationType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 위치 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateRequest {

  @NotNull(message = "위도를 입력해주세요")
  @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
  @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
  private Double latitude;

  @NotNull(message = "경도를 입력해주세요")
  @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
  @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
  private Double longitude;

  private Double altitude;

  @Min(value = 0, message = "정확도는 0 이상이어야 합니다")
  private Double accuracy;

  @Min(value = 0, message = "속도는 0 이상이어야 합니다")
  private Double speed;

  @Min(value = 0, message = "방향은 0도 이상이어야 합니다")
  @Max(value = 360, message = "방향은 360도 이하여야 합니다")
  private Double heading;

  @Size(max = 500, message = "주소는 500자 이내로 입력해주세요")
  private String address;

  private LocationType locationType;

  @Size(max = 100, message = "디바이스 ID는 100자 이내여야 합니다")
  private String deviceId;

  @Min(value = 0, message = "배터리 레벨은 0 이상이어야 합니다")
  @Max(value = 100, message = "배터리 레벨은 100 이하여야 합니다")
  private Integer batteryLevel;

  private Boolean isCharging;

  @Size(max = 20, message = "네트워크 타입은 20자 이내여야 합니다")
  private String networkType;

  @Size(max = 50, message = "위치 제공자는 50자 이내여야 합니다")
  private String provider;

  private ActivityType activityType;

  @Min(value = 0, message = "활동 신뢰도는 0 이상이어야 합니다")
  @Max(value = 100, message = "활동 신뢰도는 100 이하여야 합니다")
  private Integer activityConfidence;
}