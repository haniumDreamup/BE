package com.bifai.reminder.bifai_backend.exception;

import com.bifai.reminder.bifai_backend.constant.ErrorCode;

/**
 * BIF-AI 시스템 기본 예외 클래스
 * 모든 BIF 관련 예외의 부모 클래스
 *
 * <p>ErrorCode enum과 연동하여 일관된 에러 처리를 제공합니다.</p>
 * <p>BIF 사용자를 위한 친화적인 메시지와 행동 지침을 포함합니다.</p>
 *
 * @author BIF-AI 개발팀
 * @version 2.0
 * @since 2024-01-01
 */
public abstract class BifException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String userFriendlyMessage;
    private final String actionGuide;

    /**
     * ErrorCode를 사용한 생성자 (권장)
     */
    protected BifException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getMessage();
        this.actionGuide = errorCode.getUserAction();
    }

    /**
     * ErrorCode와 원인 예외를 사용한 생성자
     */
    protected BifException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = errorCode.getMessage();
        this.actionGuide = errorCode.getUserAction();
    }

    /**
     * 커스텀 메시지를 사용한 생성자 (레거시 호환)
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    protected BifException(String message, String userFriendlyMessage, String actionGuide) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.userFriendlyMessage = userFriendlyMessage;
        this.actionGuide = actionGuide;
    }

    /**
     * 커스텀 메시지와 원인 예외를 사용한 생성자 (레거시 호환)
     * @deprecated ErrorCode를 사용하는 생성자를 권장합니다
     */
    @Deprecated
    protected BifException(String message, String userFriendlyMessage, String actionGuide, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.userFriendlyMessage = userFriendlyMessage;
        this.actionGuide = actionGuide;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    public String getActionGuide() {
        return actionGuide;
    }
}