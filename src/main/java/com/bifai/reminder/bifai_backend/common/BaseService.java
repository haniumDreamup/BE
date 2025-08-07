package com.bifai.reminder.bifai_backend.common;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 기본 서비스 추상 클래스
 * 
 * <p>모든 서비스에서 공통으로 사용하는 메서드들을 제공합니다.
 * 사용자 조회, 권한 확인 등의 기본 기능을 포함합니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public abstract class BaseService {

  @Autowired
  protected UserRepository userRepository;

  /**
   * 현재 인증된 사용자 정보를 조회합니다.
   * 
   * @return 현재 로그인한 사용자 엔티티
   * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
   */
  protected User getCurrentUser() {
    BifUserDetails userDetails = getCurrentUserDetails();
    
    return userRepository.findById(userDetails.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
  }

  /**
   * 현재 인증된 사용자의 상세 정보를 조회합니다.
   * 
   * @return BifUserDetails 객체
   */
  protected BifUserDetails getCurrentUserDetails() {
    return (BifUserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
  }

  /**
   * 현재 인증된 사용자의 ID를 조회합니다.
   * 
   * @return 현재 로그인한 사용자의 ID
   */
  protected Long getCurrentUserId() {
    return getCurrentUserDetails().getUserId();
  }

  /**
   * 사용자 ID로 사용자를 조회합니다.
   * 
   * @param userId 사용자 ID
   * @return 사용자 엔티티
   * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
   */
  protected User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: userId=" + userId));
  }

  /**
   * 현재 사용자가 특정 사용자와 동일한지 확인합니다.
   * 
   * @param userId 확인할 사용자 ID
   * @return 동일 여부
   */
  protected boolean isCurrentUser(Long userId) {
    return getCurrentUserId().equals(userId);
  }

  /**
   * 로그 메시지에 사용자 정보를 포함하여 출력합니다.
   * 
   * @param message 로그 메시지
   * @param args 추가 인자
   */
  protected void logWithUser(String message, Object... args) {
    Long userId = getCurrentUserId();
    log.info("[userId={}] " + message, userId, args);
  }

  /**
   * 경고 로그 메시지에 사용자 정보를 포함하여 출력합니다.
   * 
   * @param message 로그 메시지
   * @param args 추가 인자
   */
  protected void logWarnWithUser(String message, Object... args) {
    Long userId = getCurrentUserId();
    log.warn("[userId={}] " + message, userId, args);
  }

  /**
   * 오류 로그 메시지에 사용자 정보를 포함하여 출력합니다.
   * 
   * @param message 로그 메시지
   * @param e 예외
   */
  protected void logErrorWithUser(String message, Exception e) {
    Long userId = getCurrentUserId();
    log.error("[userId={}] " + message, userId, e);
  }

  /**
   * 디버그 로그 메시지에 사용자 정보를 포함하여 출력합니다.
   * 
   * @param message 로그 메시지
   * @param args 추가 인자
   */
  protected void logDebugWithUser(String message, Object... args) {
    Long userId = getCurrentUserId();
    log.debug("[userId={}] " + message, userId, args);
  }
}