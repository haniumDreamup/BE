package com.bifai.reminder.bifai_backend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileApiResponse<T> {
  private boolean success;
  private T data;
  private String message;
  private MobileError error;
  private LocalDateTime timestamp;
  
  // Success response
  public static <T> MobileApiResponse<T> success(T data) {
    return MobileApiResponse.<T>builder()
        .success(true)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }
  
  // Success response with message
  public static <T> MobileApiResponse<T> success(T data, String message) {
    return MobileApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }
  
  // Error response
  public static <T> MobileApiResponse<T> error(String code, String message, String userAction) {
    return MobileApiResponse.<T>builder()
        .success(false)
        .error(MobileError.builder()
            .code(code)
            .message(message)
            .userAction(userAction)
            .build())
        .timestamp(LocalDateTime.now())
        .build();
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MobileError {
    private String code;
    private String message;
    private String userAction;
  }
}