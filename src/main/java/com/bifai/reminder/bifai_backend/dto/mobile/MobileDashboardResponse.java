package com.bifai.reminder.bifai_backend.dto.mobile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모바일 대시보드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileDashboardResponse {
    
    private String message;
    private Object data;
    
    public static MobileDashboardResponse success(Object data) {
        return MobileDashboardResponse.builder()
            .message("대시보드 조회 성공")
            .data(data)
            .build();
    }
}