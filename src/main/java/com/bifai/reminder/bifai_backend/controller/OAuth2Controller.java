package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 소셜 로그인 컨트롤러
 * 
 * <p>OAuth2 소셜 로그인 관련 API 엔드포인트를 제공합니다.
 * 현재는 각 제공자별 로그인 URL을 반환하는 기능을 제공합니다.</p>
 * 
 * <p>지원하는 OAuth2 제공자:</p>
 * <ul>
 *   <li>Kakao - 카카오 계정 로그인</li>
 *   <li>Naver - 네이버 계정 로그인</li>
 *   <li>Google - 구글 계정 로그인</li>
 * </ul>
 * 
 * <p>BIF 사용자를 위한 특징:</p>
 * <ul>
 *   <li>간단한 API 구조</li>
 *   <li>친근한 한국어 메시지</li>
 *   <li>명확한 제공자별 URL 제공</li>
 * </ul>
 * 
 * @since 1.0
 */
@Tag(name = "OAuth2", description = "OAuth2 소셜 로그인 API")
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

  /**
   * 각 OAuth2 제공자의 로그인 URL을 반환합니다.
   * 
   * <p>프론트엔드에서 이 API를 호출하여 각 제공자의 로그인 URL을 받아
   * 사용자를 해당 URL로 리다이렉트할 수 있습니다.</p>
   * 
   * <p>반환 형식:</p>
   * <pre>
   * {
   *   "success": true,
   *   "data": {
   *     "kakao": "/oauth2/authorization/kakao",
   *     "naver": "/oauth2/authorization/naver", 
   *     "google": "/oauth2/authorization/google"
   *   },
   *   "message": "소셜 로그인 주소를 가져왔습니다."
   * }
   * </pre>
   * 
   * <p>사용 예시:</p>
   * <pre>
   * // 프론트엔드에서 카카오 로그인 버튼 클릭 시
   * const response = await fetch('/api/v1/auth/oauth2/login-urls');
   * const data = await response.json();
   * window.location.href = data.data.kakao; // 카카오 로그인 페이지로 이동
   * </pre>
   * 
   * @return OAuth2 제공자별 로그인 URL 맵
   */
  @Operation(
    summary = "OAuth2 로그인 URL 조회",
    description = "각 소셜 로그인 제공자의 로그인 URL을 반환합니다. " +
                  "프론트엔드에서 이 URL로 사용자를 리다이렉트하면 소셜 로그인을 시작할 수 있습니다."
  )
  @ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "로그인 URL 조회 성공",
      content = @io.swagger.v3.oas.annotations.media.Content(
        mediaType = "application/json",
        examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
          value = "{"
            + "\"success\": true,"
            + "\"data\": {"
            + "  \"kakao\": \"/oauth2/authorization/kakao\","
            + "  \"naver\": \"/oauth2/authorization/naver\","
            + "  \"google\": \"/oauth2/authorization/google\""
            + "},"
            + "\"message\": \"소셜 로그인 주소를 가져왔습니다.\","
            + "\"timestamp\": \"2024-01-01T00:00:00Z\""
            + "}"
        )
      )
    )
  })
  @SecurityRequirements // 이 API는 인증이 필요하지 않음을 명시
  @GetMapping("/login-urls")
  public ApiResponse<Map<String, String>> getOAuth2LoginUrls() {
    // 각 OAuth2 제공자의 로그인 URL 맵 생성
    Map<String, String> loginUrls = new HashMap<>();
    
    // Spring Security OAuth2 클라이언트가 처리하는 표준 URL 패턴
    // /oauth2/authorization/{registrationId} 형식
    loginUrls.put("kakao", "/oauth2/authorization/kakao");
    loginUrls.put("naver", "/oauth2/authorization/naver");
    loginUrls.put("google", "/oauth2/authorization/google");
    
    // BIF 사용자를 위한 친근한 메시지와 함께 반환
    return ApiResponse.success(loginUrls, "소셜 로그인 주소를 가져왔습니다.");
  }
}