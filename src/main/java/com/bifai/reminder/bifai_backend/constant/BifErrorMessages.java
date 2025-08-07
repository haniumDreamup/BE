package com.bifai.reminder.bifai_backend.constant;

/**
 * BIF 사용자를 위한 쉬운 에러 메시지
 * - 5학년 수준의 이해하기 쉬운 문장
 * - 긍정적이고 격려하는 톤
 * - 구체적인 행동 지침 제공
 */
public class BifErrorMessages {
    
    // 인증 관련 메시지
    public static final String LOGIN_REQUIRED = "먼저 로그인을 해주세요.";
    public static final String LOGIN_FAILED = "아이디나 비밀번호가 맞지 않아요. 다시 확인해 주세요.";
    public static final String TOKEN_EXPIRED = "로그인 시간이 지났어요. 다시 로그인해 주세요.";
    public static final String UNAUTHORIZED = "이 기능을 사용할 권한이 없어요.";
    
    // 입력 검증 메시지
    public static final String INVALID_INPUT = "입력한 정보가 올바르지 않아요. 다시 확인해 주세요.";
    public static final String REQUIRED_FIELD_MISSING = "필요한 정보가 빠졌어요. 모든 칸을 채워주세요.";
    public static final String INVALID_EMAIL = "이메일 주소가 올바르지 않아요. (예: name@example.com)";
    public static final String INVALID_PHONE = "전화번호가 올바르지 않아요. (예: 010-1234-5678)";
    
    // 리소스 관련 메시지
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없어요.";
    public static final String SCHEDULE_NOT_FOUND = "일정을 찾을 수 없어요.";
    public static final String REMINDER_NOT_FOUND = "알림을 찾을 수 없어요.";
    public static final String DEVICE_NOT_FOUND = "기기를 찾을 수 없어요.";
    
    // 중복 관련 메시지
    public static final String EMAIL_ALREADY_EXISTS = "이미 사용 중인 이메일이에요. 다른 이메일을 사용해 주세요.";
    public static final String USERNAME_ALREADY_EXISTS = "이미 사용 중인 아이디에요. 다른 아이디를 만들어 주세요.";
    
    // 네트워크/시스템 메시지
    public static final String NETWORK_ERROR = "인터넷 연결을 확인해 주세요.";
    public static final String SERVER_ERROR = "잠시 문제가 생겼어요. 조금 후에 다시 시도해 주세요.";
    public static final String TIMEOUT_ERROR = "시간이 너무 오래 걸려요. 다시 시도해 주세요.";
    
    // 위치 관련 메시지
    public static final String LOCATION_PERMISSION_REQUIRED = "위치 정보를 사용하려면 권한이 필요해요.";
    public static final String LOCATION_NOT_AVAILABLE = "현재 위치를 찾을 수 없어요.";
    
    // 긴급 상황 메시지
    public static final String EMERGENCY_ALERT_SENT = "긴급 알림을 보냈어요. 보호자가 곧 연락할 거예요.";
    public static final String EMERGENCY_MODE_ACTIVATED = "긴급 모드가 켜졌어요. 안전한 곳으로 이동하세요.";
    
    // 사용자 행동 지침
    public static final String ACTION_TRY_AGAIN = "다시 한 번 시도해 보세요.";
    public static final String ACTION_CHECK_INPUT = "입력한 내용을 다시 확인해 보세요.";
    public static final String ACTION_CONTACT_GUARDIAN = "문제가 계속되면 보호자에게 도움을 요청하세요.";
    public static final String ACTION_CHECK_INTERNET = "인터넷이 연결되어 있는지 확인해 보세요.";
    public static final String ACTION_WAIT_AND_RETRY = "잠시 기다렸다가 다시 시도해 보세요.";
    
    private BifErrorMessages() {
        // 상수 클래스이므로 인스턴스 생성 방지
    }
}