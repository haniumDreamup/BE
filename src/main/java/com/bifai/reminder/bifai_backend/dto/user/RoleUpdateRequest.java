package com.bifai.reminder.bifai_backend.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 사용자 역할 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    
    @NotEmpty(message = "최소 하나 이상의 역할을 선택해주세요")
    private Set<Long> roleIds;
}