package com.bifai.reminder.bifai_backend.common;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;

import java.util.stream.Collectors;

/**
 * 기본 컨트롤러 추상 클래스
 * 
 * <p>모든 컨트롤러에서 공통으로 사용하는 메서드들을 제공합니다.
 * 중복 코드를 줄이고 일관된 응답 처리를 보장합니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public abstract class BaseController {

  @Autowired
  protected UserRepository userRepository;

  /**
   * 현재 인증된 사용자 정보를 조회합니다.
   * 
   * @return 현재 로그인한 사용자 엔티티
   * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
   */
  protected User getCurrentUser() {
    BifUserDetails userDetails = (BifUserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    
    return userRepository.findById(userDetails.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
  }

  /**
   * 현재 인증된 사용자의 ID를 조회합니다.
   * 
   * @return 현재 로그인한 사용자의 ID
   */
  protected Long getCurrentUserId() {
    BifUserDetails userDetails = (BifUserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    return userDetails.getUserId();
  }

  /**
   * 검증 오류가 있는지 확인하고 오류 메시지를 생성합니다.
   * 
   * @param bindingResult Spring 검증 결과
   * @return 오류 메시지 문자열, 오류가 없으면 null
   */
  protected String getValidationErrorMessage(BindingResult bindingResult) {
    if (!bindingResult.hasErrors()) {
      return null;
    }
    
    return bindingResult.getFieldErrors().stream()
        .map(error -> error.getDefaultMessage())
        .collect(Collectors.joining(", "));
  }

  /**
   * 검증 오류 응답을 생성합니다.
   *
   * @param bindingResult Spring 검증 결과
   * @return 400 Bad Request 응답, 오류가 없으면 null
   */
  protected ResponseEntity<ApiResponse<?>> createValidationErrorResponse(BindingResult bindingResult) {
    String errorMessage = getValidationErrorMessage(bindingResult);
    if (errorMessage == null) {
      return null;
    }

    return ResponseEntity.badRequest()
        .body(ApiResponse.error("VALIDATION_ERROR", errorMessage));
  }

  /**
   * 성공 응답을 생성합니다.
   * 
   * @param data 응답 데이터
   * @param message 성공 메시지
   * @return 200 OK 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createSuccessResponse(T data, String message) {
    return ResponseEntity.ok(ApiResponse.success(data, message));
  }

  /**
   * 생성 성공 응답을 생성합니다.
   * 
   * @param data 생성된 리소스 데이터
   * @param message 성공 메시지
   * @return 201 Created 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createCreatedResponse(T data, String message) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(data, message));
  }

  /**
   * 오류 응답을 생성합니다.
   *
   * @param status HTTP 상태 코드
   * @param code 오류 코드
   * @param message 오류 메시지
   * @return 오류 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createErrorResponse(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status)
        .body(ApiResponse.error(code, message));
  }

  /**
   * Not Found 응답을 생성합니다.
   *
   * @param message 오류 메시지
   * @return 404 Not Found 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createNotFoundResponse(String message) {
    return createErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
  }

  /**
   * Bad Request 응답을 생성합니다.
   *
   * @param message 오류 메시지
   * @return 400 Bad Request 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createBadRequestResponse(String message) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
  }

  /**
   * Internal Server Error 응답을 생성합니다.
   *
   * @param message 오류 메시지
   * @return 500 Internal Server Error 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> createInternalErrorResponse(String message) {
    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message);
  }

  /**
   * 예외를 로깅하고 오류 응답을 생성합니다.
   * 
   * @param message 로그 메시지
   * @param e 예외
   * @param userMessage 사용자에게 보여줄 메시지
   * @return 500 Internal Server Error 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> handleException(String message, Exception e, String userMessage) {
    log.error(message, e);
    return createInternalErrorResponse(userMessage);
  }

  /**
   * 리소스를 찾을 수 없는 예외를 처리합니다.
   * 
   * @param message 로그 메시지
   * @param e 예외
   * @param userMessage 사용자에게 보여줄 메시지
   * @return 404 Not Found 응답
   */
  protected <T> ResponseEntity<ApiResponse<T>> handleNotFoundException(String message, Exception e, String userMessage) {
    log.error(message, e);
    return createNotFoundResponse(userMessage);
  }
}