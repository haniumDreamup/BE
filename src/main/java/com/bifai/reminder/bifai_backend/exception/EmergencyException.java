package com.bifai.reminder.bifai_backend.exception;

/**
 * 긴급 상황 예외
 * BIF 사용자가 위험에 처했거나 즉각적인 도움이 필요한 상황
 */
public class EmergencyException extends BifException {
    
    private final Long userId;
    private final EmergencyType type;
    private final Double latitude;
    private final Double longitude;
    
    public enum EmergencyType {
        FALL_DETECTED("낙상 감지"),
        LOCATION_LOST("위치 추적 불가"),
        NO_ACTIVITY("장시간 활동 없음"),
        PANIC_BUTTON("긴급 버튼 활성화"),
        LOW_BATTERY_CRITICAL("배터리 위급"),
        MEDICATION_MISSED("중요 약물 미복용");
        
        private final String description;
        
        EmergencyType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public EmergencyException(Long userId, EmergencyType type, Double latitude, Double longitude) {
        super(
            String.format("Emergency situation for user %d: %s", userId, type.getDescription()),
            "긴급 상황이 발생했어요! 보호자에게 알림을 보냈어요.",
            "안전한 곳에서 기다려 주세요. 곧 도움이 올 거예요."
        );
        this.userId = userId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public EmergencyType getType() {
        return type;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
}