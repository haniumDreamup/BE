package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.security.oauth2.impl.GoogleOAuth2UserInfo;
import com.bifai.reminder.bifai_backend.security.oauth2.impl.KakaoOAuth2UserInfo;
import com.bifai.reminder.bifai_backend.security.oauth2.impl.NaverOAuth2UserInfo;

import java.util.Map;

/**
 * OAuth2 사용자 정보 팩토리 클래스
 * 
 * <p>각 OAuth2 제공자별로 적절한 {@link OAuth2UserInfo} 구현체를 생성합니다.
 * 팩토리 패턴을 사용하여 제공자별 처리 로직을 캡슐화합니다.</p>
 * 
 * <p>지원하는 제공자:</p>
 * <ul>
 *   <li>Google - {@link GoogleOAuth2UserInfo}</li>
 *   <li>Kakao - {@link KakaoOAuth2UserInfo}</li>
 *   <li>Naver - {@link NaverOAuth2UserInfo}</li>
 * </ul>
 * 
 * @see OAuth2UserInfo
 * @see CustomOAuth2UserService
 * @since 1.0
 */
public class OAuth2UserInfoFactory {

  /**
   * 제공자 ID에 따라 적절한 OAuth2UserInfo 구현체를 생성합니다.
   * 
   * <p>각 OAuth2 제공자는 서로 다른 형식의 사용자 정보를 반환하므로,
   * 제공자별로 전용 파서를 사용합니다.</p>
   * 
   * <p>제공자별 특징:</p>
   * <ul>
   *   <li>Google: 표준 OAuth2 형식 사용</li>
   *   <li>Kakao: 중첩된 JSON 구조 (kakao_account, properties)</li>
   *   <li>Naver: response 객체 내에 사용자 정보 포함</li>
   * </ul>
   * 
   * @param registrationId OAuth2 제공자 ID (google, kakao, naver)
   * @param attributes OAuth2 제공자로부터 받은 원본 사용자 정보
   * @return 제공자에 맞는 OAuth2UserInfo 구현체
   * @throws OAuth2AuthenticationProcessingException 지원하지 않는 제공자인 경우
   */
  public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
    // 제공자 ID를 소문자로 통일하여 비교
    switch (registrationId.toLowerCase()) {
      case "google":
        return new GoogleOAuth2UserInfo(attributes);
      case "kakao":
        return new KakaoOAuth2UserInfo(attributes);
      case "naver":
        return new NaverOAuth2UserInfo(attributes);
      default:
        // BIF 사용자를 위한 친근한 오류 메시지
        throw new OAuth2AuthenticationProcessingException(
          registrationId + " 로그인은 아직 지원하지 않습니다. " +
          "Google, Kakao, Naver 로그인을 사용해주세요."
        );
    }
  }
}