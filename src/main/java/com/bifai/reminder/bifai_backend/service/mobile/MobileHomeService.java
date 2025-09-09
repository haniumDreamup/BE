package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 모바일 홈 서비스
 */
@Service
@RequiredArgsConstructor
public class MobileHomeService {
    
    public MobileDashboardResponse getDashboard(Long userId) {
        // 임시 구현
        return MobileDashboardResponse.success("대시보드 데이터");
    }
}