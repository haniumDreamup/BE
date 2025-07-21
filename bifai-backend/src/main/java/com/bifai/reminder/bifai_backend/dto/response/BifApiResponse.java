package com.bifai.reminder.bifai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BIF 사용자를 위한 표준 API 응답 포맷
 * - 5학년 수준의 쉬운 메시지 사용
 * - 명확한 성공/실패 표시
 * - 사용자가 할 수 있는 행동 안내
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BifApiResponse<T> {
    
    /**
     * 성공 여부 - true: 성공, false: 실패
     */
    private boolean success;
    
    /**
     * 응답 데이터
     */
    private T data;
    
    /**
     * 사용자 친화적 메시지 (5학년 수준)
     */
    private String message;
    
    /**
     * 사용자가 할 수 있는 행동 안내
     */
    private String userAction;
    
    /**
     * 에러 코드 (실패 시에만)
     */
    private String errorCode;
    
    /**
     * 응답 시간
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 성공 응답 생성
     */
    public static <T> BifApiResponse<T> success(T data) {
        return BifApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("요청이 성공적으로 처리되었습니다.")
                .build();
    }
    
    /**
     * 메시지와 함께 성공 응답 생성
     */
    public static <T> BifApiResponse<T> success(T data, String message) {
        return BifApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }
    
    /**
     * 실패 응답 생성
     */
    public static <T> BifApiResponse<T> error(String errorCode, String message, String userAction) {
        return BifApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .userAction(userAction)
                .build();
    }
    
    /**
     * 간단한 실패 응답 생성
     */
    public static <T> BifApiResponse<T> error(String message) {
        return BifApiResponse.<T>builder()
                .success(false)
                .message(message)
                .userAction("문제가 계속되면 보호자에게 도움을 요청하세요.")
                .build();
    }
}