package com.bifai.reminder.bifai_backend.dto.guardian;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 보호자 권한 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianPermissionRequest {
    
    @NotNull(message = "위치 조회 권한 설정을 선택해주세요")
    private Boolean canViewLocation;
    
    @NotNull(message = "설정 수정 권한 설정을 선택해주세요")
    private Boolean canModifySettings;
    
    @NotNull(message = "알림 수신 권한 설정을 선택해주세요")
    private Boolean canReceiveAlerts;
    
    private Integer emergencyPriority;  // 1-5, 낮을수록 우선순위 높음
}