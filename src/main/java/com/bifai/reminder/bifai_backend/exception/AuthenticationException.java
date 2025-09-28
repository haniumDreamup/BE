package com.bifai.reminder.bifai_backend.exception;

import com.bifai.reminder.bifai_backend.constant.ErrorCode;

/**
 * 인증 관련 예외
 *
 * <p>로그인, 회원가입, 토큰 검증 등 인증 과정에서 발생하는 예외를 처리합니다.</p>
 *
 * <p>주요 사용 사례:</p>
 * <ul>
 *   <li>잘못된 로그인 정보</li>
 *   <li>만료된 토큰</li>
 *   <li>중복된 사용자 정보</li>
 *   <li>권한 부족</li>
 * </ul>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
public class AuthenticationException extends BifException {

    /**
     * 로그인 실패
     */
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException(ErrorCode.AUTH_002);
    }

    /**
     * 토큰 만료
     */
    public static AuthenticationException tokenExpired() {
        return new AuthenticationException(ErrorCode.AUTH_003);
    }

    /**
     * 로그인 필요
     */
    public static AuthenticationException loginRequired() {
        return new AuthenticationException(ErrorCode.AUTH_001);
    }

    /**
     * 중복된 이메일
     */
    public static AuthenticationException duplicateEmail(String email) {
        return new AuthenticationException(ErrorCode.AUTH_005);
    }

    /**
     * 중복된 사용자명
     */
    public static AuthenticationException duplicateUsername(String username) {
        return new AuthenticationException(ErrorCode.AUTH_006);
    }

    /**
     * 권한 부족
     */
    public static AuthenticationException insufficientPermissions() {
        return new AuthenticationException(ErrorCode.AUTH_004);
    }

    private AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    private AuthenticationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}