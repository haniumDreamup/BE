package com.bifai.reminder.bifai_backend.dto.mobile;

import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

/**
 * 미디어 업로드 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadRequest {
  
  @NotBlank(message = "파일 이름이 필요해요")
  @Size(max = 255, message = "파일 이름이 너무 길어요")
  private String fileName;
  
  @NotBlank(message = "파일 종류를 알려주세요")
  @Pattern(
      regexp = "^(image|video)/(jpeg|jpg|png|gif|webp|mp4|mov|avi)$",
      message = "지원하지 않는 파일 형식이에요"
  )
  private String fileType;
  
  @NotNull(message = "파일 크기를 알려주세요")
  @Positive(message = "파일 크기가 올바르지 않아요")
  @Max(value = 104857600, message = "파일이 너무 커요 (최대 100MB)") // 100MB
  private Long fileSize;
  
  @NotNull(message = "업로드 종류를 선택해주세요")
  private UploadType uploadType;
  
  private Map<String, String> metadata;
  
  /**
   * 이미지 파일 여부
   */
  public boolean isImage() {
    return fileType != null && fileType.startsWith("image/");
  }
  
  /**
   * 비디오 파일 여부
   */
  public boolean isVideo() {
    return fileType != null && fileType.startsWith("video/");
  }
  
  /**
   * 멀티파트 업로드 필요 여부 (5MB 이상)
   */
  public boolean needsMultipartUpload() {
    return fileSize != null && fileSize > 5 * 1024 * 1024; // 5MB
  }
}