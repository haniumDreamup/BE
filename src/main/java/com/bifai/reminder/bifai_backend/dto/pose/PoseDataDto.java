package com.bifai.reminder.bifai_backend.dto.pose;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MediaPipe Pose 데이터 전송 DTO
 * 33개의 랜드마크 포인트를 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseDataDto {
  
  @NotNull(message = "사용자 ID는 필수입니다")
  private Long userId;
  
  @NotNull(message = "타임스탬프는 필수입니다")
  private LocalDateTime timestamp;
  
  @NotNull(message = "포즈 랜드마크는 필수입니다")
  @Size(min = 33, max = 33, message = "33개의 랜드마크가 필요합니다")
  @Valid
  private List<LandmarkDto> landmarks;
  
  @Min(value = 0, message = "프레임 번호는 0 이상이어야 합니다")
  private Integer frameNumber;
  
  @DecimalMin(value = "0.0", message = "신뢰도는 0 이상이어야 합니다")
  @DecimalMax(value = "1.0", message = "신뢰도는 1 이하여야 합니다")
  private Float overallConfidence;
  
  @Size(max = 255, message = "세션 ID는 255자를 초과할 수 없습니다")
  private String sessionId;
  
  /**
   * 랜드마크 데이터 DTO
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LandmarkDto {
    
    @NotNull(message = "랜드마크 타입은 필수입니다")
    private LandmarkType type;
    
    @NotNull(message = "X 좌표는 필수입니다")
    @DecimalMin(value = "0.0", message = "X 좌표는 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "X 좌표는 1 이하여야 합니다")
    private Float x;
    
    @NotNull(message = "Y 좌표는 필수입니다")
    @DecimalMin(value = "0.0", message = "Y 좌표는 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "Y 좌표는 1 이하여야 합니다")
    private Float y;
    
    @NotNull(message = "Z 좌표는 필수입니다")
    @DecimalMin(value = "-1.0", message = "Z 좌표는 -1 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "Z 좌표는 1 이하여야 합니다")
    private Float z;
    
    @NotNull(message = "가시성은 필수입니다")
    @DecimalMin(value = "0.0", message = "가시성은 0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "가시성은 1 이하여야 합니다")
    private Float visibility;
  }
  
  /**
   * MediaPipe Pose 랜드마크 타입 (33개)
   */
  public enum LandmarkType {
    NOSE(0),
    LEFT_EYE_INNER(1),
    LEFT_EYE(2),
    LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4),
    RIGHT_EYE(5),
    RIGHT_EYE_OUTER(6),
    LEFT_EAR(7),
    RIGHT_EAR(8),
    MOUTH_LEFT(9),
    MOUTH_RIGHT(10),
    LEFT_SHOULDER(11),
    RIGHT_SHOULDER(12),
    LEFT_ELBOW(13),
    RIGHT_ELBOW(14),
    LEFT_WRIST(15),
    RIGHT_WRIST(16),
    LEFT_PINKY(17),
    RIGHT_PINKY(18),
    LEFT_INDEX(19),
    RIGHT_INDEX(20),
    LEFT_THUMB(21),
    RIGHT_THUMB(22),
    LEFT_HIP(23),
    RIGHT_HIP(24),
    LEFT_KNEE(25),
    RIGHT_KNEE(26),
    LEFT_ANKLE(27),
    RIGHT_ANKLE(28),
    LEFT_HEEL(29),
    RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31),
    RIGHT_FOOT_INDEX(32);
    
    private final int index;
    
    LandmarkType(int index) {
      this.index = index;
    }
    
    public int getIndex() {
      return index;
    }
    
    public static LandmarkType fromIndex(int index) {
      for (LandmarkType type : values()) {
        if (type.index == index) {
          return type;
        }
      }
      throw new IllegalArgumentException("Invalid landmark index: " + index);
    }
  }
}