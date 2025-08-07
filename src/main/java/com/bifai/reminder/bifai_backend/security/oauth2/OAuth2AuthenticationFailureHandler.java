package com.bifai.reminder.bifai_backend.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 인증 실패 핸들러
 * 
 * <p>OAuth2 로그인이 실패했을 때 호출되며,
 * BIF 사용자를 위한 친근한 오류 메시지와 함께
 * 프론트엔드로 리다이렉트합니다.</p>
 * 
 * <p>오류 처리 특징:</p>
 * <ul>
 *   <li>5학년 수준의 친근한 오류 메시지</li>
 *   <li>구체적인 행동 지침 제공</li>
 *   <li>오류 코드로 문제 분류</li>
 * </ul>
 * 
 * @see OAuth2AuthenticationSuccessHandler
 * @since 1.0
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  /**
   * OAuth2 인증 실패 후 프론트엔드로 리다이렉트할 URI
   * 
   * <p>기본값: http://localhost:3000/auth/callback</p>
   * <p>환경변수: app.oauth2.redirect-uri</p>
   */
  @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
  private String redirectUri;

  /**
   * OAuth2 인증 실패 시 호출되는 메소드
   * 
   * <p>BIF 사용자를 위해 친근한 오류 메시지와 함께
   * 프론트엔드로 리다이렉트합니다.</p>
   * 
   * <p>리다이렉트 URL 형식:</p>
   * <pre>
   * http://localhost:3000/auth/callback?
   *   error={ERROR_CODE}&
   *   message={FRIENDLY_MESSAGE}&
   *   action={USER_ACTION}
   * </pre>
   * 
   * <p>오류 코드 분류:</p>
   * <ul>
   *   <li>AUTH_FAILED: 일반적인 인증 실패</li>
   *   <li>EMAIL_REQUIRED: 이메일 정보 누락</li>
   *   <li>SYSTEM_ERROR: 시스템 오류</li>
   * </ul>
   * 
   * @param request HTTP 요청
   * @param response HTTP 응답
   * @param exception 인증 실패 예외
   * @throws IOException I/O 예외
   * @throws ServletException 서블릿 예외
   */
  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
    
    // BIF 사용자를 위한 친근하고 구체적인 에러 메시지
    String errorMessage = "로그인이 잘 안됐어요 😢";
    String errorCode = "AUTH_FAILED";
    
    // 예외 메시지에 따른 구체적인 오류 메시지 설정
    if (exception.getMessage().contains("email")) {
      errorMessage = "이메일 정보를 받을 수 없어요. 소셜 계정 설정을 확인해 주세요.";
      errorCode = "EMAIL_REQUIRED";
    } else if (exception.getMessage().contains("User role not found")) {
      errorMessage = "시스템 오류가 있어요. 잠시 후 다시 시도해 주세요.";
      errorCode = "SYSTEM_ERROR";
    }
    
    // 로그에 오류 기록
    log.error("OAuth2 로그인 실패: {}", exception.getMessage());
    
    // 프론트엔드로 리다이렉트할 URL 구성
    String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
      .queryParam("error", errorCode)
      .queryParam("message", errorMessage)
      .queryParam("action", "다시 로그인 버튼을 눌러주세요")
      .build().toUriString();
    
    // 프론트엔드로 리다이렉트
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}