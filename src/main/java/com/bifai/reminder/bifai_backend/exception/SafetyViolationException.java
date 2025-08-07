package com.bifai.reminder.bifai_backend.exception;

/**
 * 안전 규칙 위반 예외
 * BIF 사용자가 안전 구역을 벗어났거나 위험한 행동을 했을 때
 */
public class SafetyViolationException extends BifException {
    
    private final Long userId;
    private final ViolationType type;
    private final String safeZoneName;
    
    public enum ViolationType {
        OUT_OF_SAFE_ZONE("안전 구역 이탈"),
        DANGEROUS_TIME("위험한 시간대 활동"),
        UNAUTHORIZED_LOCATION("허가되지 않은 장소"),
        MISSED_CHECK_IN("체크인 누락"),
        DEVICE_DISCONNECTED("기기 연결 끊김");
        
        private final String description;
        
        ViolationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public SafetyViolationException(Long userId, ViolationType type, String safeZoneName) {
        super(
            String.format("Safety violation for user %d: %s", userId, type.getDescription()),
            getSafetyMessage(type),
            getSafetyAction(type)
        );
        this.userId = userId;
        this.type = type;
        this.safeZoneName = safeZoneName;
    }
    
    private static String getSafetyMessage(ViolationType type) {
        switch (type) {
            case OUT_OF_SAFE_ZONE:
                return "안전한 곳을 벗어났어요.";
            case DANGEROUS_TIME:
                return "지금은 밖에 나가기 위험한 시간이에요.";
            case DEVICE_DISCONNECTED:
                return "안전 장치가 꺼졌어요.";
            default:
                return "안전하지 않은 상황이에요.";
        }
    }
    
    private static String getSafetyAction(ViolationType type) {
        switch (type) {
            case OUT_OF_SAFE_ZONE:
                return "집으로 돌아가거나 보호자에게 연락하세요.";
            case DANGEROUS_TIME:
                return "집에서 기다려 주세요.";
            case DEVICE_DISCONNECTED:
                return "기기를 다시 켜주세요.";
            default:
                return "안전한 곳으로 이동하세요.";
        }
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public ViolationType getType() {
        return type;
    }
    
    public String getSafeZoneName() {
        return safeZoneName;
    }
}