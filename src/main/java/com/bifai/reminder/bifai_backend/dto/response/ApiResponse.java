package com.bifai.reminder.bifai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 표준 API 응답 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  
  private boolean success;
  private T data;
  private String message;
  private LocalDateTime timestamp;
  private ErrorInfo error;
  
  /**
   * 성공 응답 생성
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
      .success(true)
      .data(data)
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 메시지와 함께 성공 응답
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
      .success(true)
      .data(data)
      .message(message)
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 에러 응답
   */
  public static <T> ApiResponse<T> error(String code, String message) {
    return ApiResponse.<T>builder()
      .success(false)
      .error(ErrorInfo.builder()
        .code(code)
        .message(message)
        .build())
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 에러 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ErrorInfo {
    private String code;
    private String message;
  }
}