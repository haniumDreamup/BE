package com.bifai.reminder.bifai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * BIF-AI API 통합 응답 형식
 * 모든 API 응답은 이 형식을 따름
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BifApiResponse<T> {

  private boolean success;
  
  private T data;
  
  private String message;
  
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp;
  
  private ErrorDetail error;
  
  /**
   * 에러 상세 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ErrorDetail {
    private String code;
    private String message;
    private String userAction;
    private String field;
  }
  
  /**
   * 성공 응답 생성
   */
  public static <T> BifApiResponse<T> success(T data, String message) {
    return BifApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }
  
  /**
   * 성공 응답 생성 (메시지 없음)
   */
  public static <T> BifApiResponse<T> success(T data) {
    return success(data, "요청이 성공적으로 처리되었어요");
  }
  
  /**
   * 실패 응답 생성
   */
  public static <T> BifApiResponse<T> error(String code, String message, String userAction) {
    return BifApiResponse.<T>builder()
        .success(false)
        .error(ErrorDetail.builder()
            .code(code)
            .message(message)
            .userAction(userAction)
            .build())
        .timestamp(LocalDateTime.now())
        .build();
  }
  
  /**
   * 실패 응답 생성 (필드 에러)
   */
  public static <T> BifApiResponse<T> fieldError(String field, String message) {
    return BifApiResponse.<T>builder()
        .success(false)
        .error(ErrorDetail.builder()
            .code("VALIDATION_ERROR")
            .message(message)
            .field(field)
            .userAction("입력한 정보를 다시 확인해주세요")
            .build())
        .timestamp(LocalDateTime.now())
        .build();
  }
}