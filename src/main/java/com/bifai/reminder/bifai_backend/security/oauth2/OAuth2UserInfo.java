package com.bifai.reminder.bifai_backend.security.oauth2;

/**
 * OAuth2 제공자로부터 받은 사용자 정보를 표준화하는 인터페이스
 * 
 * <p>각 OAuth2 제공자(Kakao, Naver, Google)는 서로 다른 형식의 사용자 정보를 반환합니다.
 * 이 인터페이스는 이러한 다양한 형식을 통일된 방식으로 처리할 수 있도록 합니다.</p>
 * 
 * <p>구현체:</p>
 * <ul>
 *   <li>{@link com.bifai.reminder.bifai_backend.security.oauth2.impl.KakaoOAuth2UserInfo} - 카카오 로그인</li>
 *   <li>{@link com.bifai.reminder.bifai_backend.security.oauth2.impl.NaverOAuth2UserInfo} - 네이버 로그인</li>
 *   <li>{@link com.bifai.reminder.bifai_backend.security.oauth2.impl.GoogleOAuth2UserInfo} - 구글 로그인</li>
 * </ul>
 * 
 * @see OAuth2UserInfoFactory
 * @since 1.0
 */
public interface OAuth2UserInfo {
  
  /**
   * OAuth2 제공자의 고유 사용자 식별자를 반환합니다.
   * 
   * <p>각 제공자별 ID 형식:</p>
   * <ul>
   *   <li>Kakao: 숫자형 ID (예: "123456789")</li>
   *   <li>Naver: 문자열 ID (예: "abcd1234")</li>
   *   <li>Google: 문자열 ID (예: "1234567890")</li>
   * </ul>
   * 
   * @return 제공자별 고유 사용자 ID
   */
  String getId();
  
  /**
   * 사용자의 이메일 주소를 반환합니다.
   * 
   * <p>주의사항:</p>
   * <ul>
   *   <li>일부 제공자는 이메일을 제공하지 않을 수 있습니다</li>
   *   <li>사용자가 이메일 제공에 동의하지 않은 경우 null일 수 있습니다</li>
   * </ul>
   * 
   * @return 이메일 주소, 없으면 null
   */
  String getEmail();
  
  /**
   * 사용자의 표시 이름을 반환합니다.
   * 
   * <p>각 제공자별 이름 우선순위:</p>
   * <ul>
   *   <li>Kakao: 프로필 닉네임</li>
   *   <li>Naver: 실명 또는 닉네임</li>
   *   <li>Google: 표시 이름</li>
   * </ul>
   * 
   * @return 사용자 이름, 없으면 null
   */
  String getName();
  
  /**
   * 사용자의 프로필 이미지 URL을 반환합니다.
   * 
   * <p>이미지 관련 주의사항:</p>
   * <ul>
   *   <li>URL은 시간이 지나면 만료될 수 있습니다</li>
   *   <li>BIF 시스템에서는 이미지를 별도로 저장해야 합니다</li>
   *   <li>사용자가 프로필 이미지를 설정하지 않은 경우 null일 수 있습니다</li>
   * </ul>
   * 
   * @return 프로필 이미지 URL, 없으면 null
   */
  String getImageUrl();
}