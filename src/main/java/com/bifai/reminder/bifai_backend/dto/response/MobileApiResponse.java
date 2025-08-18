package com.bifai.reminder.bifai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모바일 API 공통 응답 DTO
 * 
 * BIF 사용자를 위해 간단하고 일관된 응답 구조를 제공합니다.
 * 불필요한 중첩을 최소화하고 명확한 메시지를 전달합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileApiResponse<T> {
  
  /**
   * 요청 성공 여부
   */
  private boolean success;
  
  /**
   * 응답 데이터
   */
  private T data;
  
  /**
   * 사용자에게 보여줄 메시지 (5학년 수준 한국어)
   */
  private String message;
  
  /**
   * 에러 정보 (실패시에만 포함)
   */
  private ErrorInfo error;
  
  /**
   * 응답 시간
   */
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();
  
  /**
   * 성공 응답 생성
   */
  public static <T> MobileApiResponse<T> success(T data, String message) {
    return MobileApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .build();
  }
  
  /**
   * 성공 응답 생성 (기본 메시지)
   */
  public static <T> MobileApiResponse<T> success(T data) {
    return success(data, "성공적으로 처리되었어요");
  }
  
  /**
   * 에러 응답 생성
   */
  public static <T> MobileApiResponse<T> error(String code, String message, String userAction) {
    return MobileApiResponse.<T>builder()
        .success(false)
        .error(ErrorInfo.builder()
            .code(code)
            .message(message)
            .userAction(userAction)
            .build())
        .build();
  }
  
  /**
   * 에러 정보 내부 클래스
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ErrorInfo {
    /**
     * 에러 코드 (예: AUTH_001, DATA_001)
     */
    private String code;
    
    /**
     * 에러 메시지 (사용자가 이해하기 쉬운 설명)
     */
    private String message;
    
    /**
     * 사용자가 해야 할 행동 안내
     */
    private String userAction;
  }
  
  /**
   * 페이징 정보 내부 클래스
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PageInfo {
    /**
     * 현재 페이지 (1부터 시작)
     */
    private int current;
    
    /**
     * 전체 페이지 수
     */
    private int total;
    
    /**
     * 페이지 크기
     */
    private int size;
    
    /**
     * 전체 요소 개수
     */
    private long totalElements;
    
    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;
    
    /**
     * 이전 페이지 존재 여부
     */
    private boolean hasPrevious;
  }
}