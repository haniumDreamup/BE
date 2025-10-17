package com.bifai.reminder.bifai_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private LocalDateTime timestamp;
    
    // Error response용 추가 필드
    private ErrorDetail error;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
        private String userAction;
    }
    
    // 성공 응답 생성 헬퍼 메소드
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // 메시지 없는 성공 응답 생성 헬퍼 메소드
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("요청이 성공적으로 처리되었습니다")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // 에러 응답 생성 헬퍼 메소드
    public static <T> ApiResponse<T> error(String code, String message, String userAction) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .userAction(userAction)
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    // 간단한 에러 응답 생성 헬퍼 메소드
    public static <T> ApiResponse<T> error(String message) {
        return error("VALIDATION_ERROR", message, "입력값을 확인해주세요");
    }

    // code + message만으로 에러 응답 생성 (userAction 없음)
    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, "다시 시도해주세요");
    }
}