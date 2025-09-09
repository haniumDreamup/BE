package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 모바일 API 컨트롤러 베이스 클래스
 * 
 * 모든 모바일 컨트롤러가 상속받는 기본 클래스로,
 * 공통 기능과 유틸리티 메서드를 제공합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class MobileBaseController {
  
  protected final UserRepository userRepository;
  
  /**
   * 현재 인증된 사용자 정보 조회
   * 
   * @return 현재 로그인한 사용자
   * @throws UnauthorizedException 인증되지 않은 경우
   */
  protected User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("인증되지 않은 요청 시도");
      throw new UnauthorizedException("다시 로그인해 주세요");
    }
    
    String username = authentication.getName();
    return userRepository.findByUsernameOrEmail(username)
        .orElseThrow(() -> {
          log.error("사용자를 찾을 수 없음: {}", username);
          return new UnauthorizedException("사용자 정보를 찾을 수 없어요");
        });
  }
  
  /**
   * 현재 사용자 ID 조회
   * 
   * @return 사용자 ID
   */
  protected Long getCurrentUserId() {
    return getCurrentUser().getUserId();
  }
  
  /**
   * 성공 응답 생성
   * 
   * @param data 응답 데이터
   * @param message 사용자 메시지
   * @return 모바일 API 응답
   */
  protected <T> MobileApiResponse<T> success(T data, String message) {
    return MobileApiResponse.success(data, message);
  }
  
  /**
   * 성공 응답 생성 (데이터만)
   * 
   * @param data 응답 데이터
   * @return 모바일 API 응답
   */
  protected <T> MobileApiResponse<T> success(T data) {
    return MobileApiResponse.success(data, "성공적으로 처리되었어요");
  }
  
  /**
   * 에러 응답 생성
   * 
   * @param code 에러 코드
   * @param message 에러 메시지
   * @param userAction 사용자 액션 가이드
   * @return 모바일 API 응답
   */
  protected <T> MobileApiResponse<T> error(String code, String message, String userAction) {
    return MobileApiResponse.error(code, message, userAction);
  }
  
  /**
   * 사용자의 인지 수준에 맞는 메시지 조정
   * 
   * @param message 원본 메시지
   * @param user 사용자
   * @return 조정된 메시지
   */
  protected String adjustMessageForUser(String message, User user) {
    if (user.getCognitiveLevel() == User.CognitiveLevel.SEVERE) {
      // 더 간단한 메시지로 변환
      return simplifyMessage(message);
    }
    return message;
  }
  
  /**
   * 메시지 단순화
   * 
   * @param message 원본 메시지
   * @return 단순화된 메시지
   */
  private String simplifyMessage(String message) {
    // 복잡한 단어를 쉬운 단어로 교체
    return message
        .replace("성공적으로", "잘")
        .replace("완료되었습니다", "됐어요")
        .replace("처리되었습니다", "됐어요")
        .replace("실패하였습니다", "안 됐어요");
  }
}