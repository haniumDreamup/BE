package com.bifai.reminder.bifai_backend.exception;

import com.bifai.reminder.bifai_backend.constant.ErrorCode;

/**
 * 입력 검증 관련 예외
 *
 * <p>사용자 입력 데이터의 유효성 검증 실패 시 발생하는 예외를 처리합니다.</p>
 *
 * <p>주요 사용 사례:</p>
 * <ul>
 *   <li>필수 필드 누락</li>
 *   <li>잘못된 형식의 이메일, 전화번호</li>
 *   <li>값의 범위 초과</li>
 *   <li>잘못된 데이터 형식</li>
 * </ul>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
public class ValidationException extends BifException {

    /**
     * 필수 필드 누락
     */
    public static ValidationException requiredFieldMissing() {
        return new ValidationException(ErrorCode.VALIDATION_002);
    }

    /**
     * 잘못된 입력 형식
     */
    public static ValidationException invalidInputFormat() {
        return new ValidationException(ErrorCode.VALIDATION_001);
    }

    /**
     * 잘못된 이메일 형식
     */
    public static ValidationException invalidEmailFormat() {
        return new ValidationException(ErrorCode.VALIDATION_003);
    }

    /**
     * 잘못된 전화번호 형식
     */
    public static ValidationException invalidPhoneFormat() {
        return new ValidationException(ErrorCode.VALIDATION_004);
    }

    /**
     * 커스텀 검증 실패 (필드명과 함께)
     */
    public static ValidationException invalidField(String fieldName, String reason) {
        ValidationException exception = new ValidationException(ErrorCode.VALIDATION_001);
        // 로깅을 위한 추가 정보 저장 가능
        return exception;
    }

    private ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ValidationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}