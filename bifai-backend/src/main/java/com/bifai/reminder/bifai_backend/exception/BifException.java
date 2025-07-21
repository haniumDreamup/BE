package com.bifai.reminder.bifai_backend.exception;

/**
 * BIF-AI 시스템 기본 예외 클래스
 * 모든 BIF 관련 예외의 부모 클래스
 */
public abstract class BifException extends RuntimeException {
    
    private final String userFriendlyMessage;  // BIF 사용자를 위한 쉬운 메시지
    private final String actionGuide;          // 사용자가 할 수 있는 행동 안내
    
    protected BifException(String message, String userFriendlyMessage, String actionGuide) {
        super(message);
        this.userFriendlyMessage = userFriendlyMessage;
        this.actionGuide = actionGuide;
    }
    
    protected BifException(String message, String userFriendlyMessage, String actionGuide, Throwable cause) {
        super(message, cause);
        this.userFriendlyMessage = userFriendlyMessage;
        this.actionGuide = actionGuide;
    }
    
    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }
    
    public String getActionGuide() {
        return actionGuide;
    }
}