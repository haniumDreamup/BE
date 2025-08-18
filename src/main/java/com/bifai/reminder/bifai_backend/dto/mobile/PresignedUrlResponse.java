package com.bifai.reminder.bifai_backend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Presigned URL 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresignedUrlResponse {
  
  private String uploadUrl;
  private String uploadId;
  private String mediaId;
  
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private LocalDateTime expiresAt;
  
  private Long maxSize;
  private String uploadMethod; // PUT or POST
  private Map<String, String> headers;
  
  // 멀티파트 업로드용 추가 필드
  private Integer partSize;
  private Integer totalParts;
  private java.util.List<PartUploadInfo> parts;
  
  /**
   * 파트 업로드 정보
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PartUploadInfo {
    private Integer partNumber;
    private String uploadUrl;
    private Long startByte;
    private Long endByte;
  }
}