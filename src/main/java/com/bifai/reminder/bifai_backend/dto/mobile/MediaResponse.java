package com.bifai.reminder.bifai_backend.dto.mobile;

import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 미디어 파일 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {
  
  private String mediaId;
  private UploadType type;
  private String url;
  private String thumbnailUrl;
  private String cdnUrl;
  private String fileName;
  private Long fileSize;
  private String mimeType;
  
  // 이미지 정보
  private Integer width;
  private Integer height;
  
  // 비디오 정보
  private Integer duration; // 초 단위
  
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private LocalDateTime uploadedAt;
  
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  private LocalDateTime processedAt;
  
  private Map<String, String> metadata;
  
  /**
   * 파일 크기를 읽기 쉬운 형식으로 변환
   */
  public String getReadableFileSize() {
    if (fileSize == null) return "0 B";
    
    final String[] units = {"B", "KB", "MB", "GB"};
    int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
    
    return String.format("%.1f %s", 
        fileSize / Math.pow(1024, digitGroups), 
        units[digitGroups]);
  }
}