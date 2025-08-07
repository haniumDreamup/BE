package com.bifai.reminder.bifai_backend.security.oauth2;

import org.springframework.security.core.AuthenticationException;

/**
 * OAuth2 인증 처리 중 발생하는 예외
 * 
 * <p>OAuth2 로그인 과정에서 발생할 수 있는 다양한 문제를 나타냅니다.
 * Spring Security의 {@link AuthenticationException}을 확장하여
 * 인증 실패로 처리됩니다.</p>
 * 
 * <p>주요 사용 케이스:</p>
 * <ul>
 *   <li>지원하지 않는 OAuth2 제공자</li>
 *   <li>필수 사용자 정보 누락</li>
 *   <li>OAuth2 제공자 API 오류</li>
 * </ul>
 * 
 * @see OAuth2AuthenticationFailureHandler
 * @since 1.0
 */
public class OAuth2AuthenticationProcessingException extends AuthenticationException {
  
  /**
   * 예외 메시지와 함께 OAuth2AuthenticationProcessingException을 생성합니다.
   * 
   * <p>BIF 사용자를 위해 친근한 메시지를 사용하는 것이 권장됩니다.</p>
   * 
   * @param msg 예외 메시지
   */
  public OAuth2AuthenticationProcessingException(String msg) {
    super(msg);
  }
}