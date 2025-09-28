package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 인증 성공 핸들러
 * 
 * <p>OAuth2 로그인이 성공한 후 JWT 토큰을 생성하고
 * 프론트엔드로 리다이렉트하는 역할을 담당합니다.</p>
 * 
 * <p>처리 과정:</p>
 * <ol>
 *   <li>OAuth2 인증이 성공하면 호출됨</li>
 *   <li>인증된 사용자 정보로 JWT 토큰 생성</li>
 *   <li>토큰을 URL 파라미터로 포함하여 프론트엔드로 리다이렉트</li>
 * </ol>
 * 
 * <p>보안 고려사항:</p>
 * <ul>
 *   <li>URL 파라미터로 토큰 전달 시 보안 위험 존재</li>
 *   <li>BIF 사용자의 편의성을 위해 단순한 방식 사용</li>
 *   <li>프로덕션에서는 보다 안전한 방법 고려 필요</li>
 * </ul>
 * 
 * @see JwtTokenProvider
 * @see OAuth2UserPrincipal
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider tokenProvider;

  /**
   * OAuth2 인증 성공 후 프론트엔드로 리다이렉트할 URI
   *
   * <p>기본값: http://localhost:3004/auth/callback</p>
   * <p>환경변수: app.oauth2.redirect-uri</p>
   */
  @Value("${app.oauth2.redirect-uri:http://localhost:3004/auth/callback}")
  private String redirectUri;

  /**
   * 허용된 리다이렉트 URI 목록
   * 보안을 위해 화이트리스트 방식으로 검증
   */
  @Value("${app.oauth2.allowed-redirect-uris:http://localhost:3004/auth/callback}")
  private java.util.List<String> allowedRedirectUris;

  /**
   * OAuth2 인증 성공 시 호출되는 메소드
   * 
   * <p>JWT 토큰을 생성하고 프론트엔드로 리다이렉트합니다.
   * 현재는 단순화를 위해 URL 파라미터로 토큰을 전달하고 있습니다.</p>
   * 
   * <p>리다이렉트 URL 형식:</p>
   * <pre>
   * http://localhost:3000/auth/callback?
   *   accessToken={JWT_ACCESS_TOKEN}&
   *   refreshToken={JWT_REFRESH_TOKEN}
   * </pre>
   * 
   * <p>향후 개선 방향:</p>
   * <ul>
   *   <li>HttpOnly 쿠키 사용</li>
   *   <li>임시 코드 발급 후 토큰 교환</li>
   *   <li>state 파라미터를 통한 CSRF 방지</li>
   * </ul>
   * 
   * @param request HTTP 요청
   * @param response HTTP 응답
   * @param authentication 인증 정보
   * @throws IOException I/O 예외
   * @throws ServletException 서블릿 예외
   */
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

    // OAuth2 인증된 사용자 정보 추출
    OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

    // JWT 토큰 생성
    String accessToken = tokenProvider.createAccessToken(principal.getUser());
    String refreshToken = tokenProvider.createRefreshToken(principal.getUser());

    // 요청에서 redirect_uri 파라미터 확인 (프론트엔드가 지정한 경우)
    String requestedRedirectUri = request.getParameter("redirect_uri");
    String finalRedirectUri = redirectUri; // 기본값

    // 요청된 URI가 있고, 허용 목록에 포함되어 있으면 사용
    if (requestedRedirectUri != null && !requestedRedirectUri.isEmpty()) {
      boolean isAllowed = allowedRedirectUris.stream()
        .anyMatch(allowed -> requestedRedirectUri.startsWith(allowed.replace("/auth/callback", "")));

      if (isAllowed) {
        finalRedirectUri = requestedRedirectUri;
        log.info("Using requested redirect URI: {}", requestedRedirectUri);
      } else {
        log.warn("Requested redirect URI {} is not in allowed list, using default", requestedRedirectUri);
      }
    }

    // 프론트엔드로 리다이렉트할 URL 구성
    // 현재는 단순화를 위해 URL 파라미터로 토큰 전달
    // TODO: 프로덕션에서는 보다 안전한 방법 고려
    String targetUrl = UriComponentsBuilder.fromUriString(finalRedirectUri)
      .queryParam("accessToken", accessToken)
      .queryParam("refreshToken", refreshToken)
      .build().toUriString();

    log.info("OAuth2 authentication successful for user: {}, redirecting to: {}",
             principal.getUser().getEmail(), finalRedirectUri);

    // 프론트엔드로 리다이렉트
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}