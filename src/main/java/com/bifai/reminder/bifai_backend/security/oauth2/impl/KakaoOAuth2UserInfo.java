package com.bifai.reminder.bifai_backend.security.oauth2.impl;

import com.bifai.reminder.bifai_backend.security.oauth2.OAuth2UserInfo;

import java.util.Map;

/**
 * 카카오 OAuth2 사용자 정보 구현체
 * 
 * <p>카카오 OAuth2 API로부터 받은 사용자 정보를 파싱하여
 * {@link OAuth2UserInfo} 인터페이스에 맞게 변환합니다.</p>
 * 
 * <p>카카오 API 응답 구조:</p>
 * <pre>
 * {
 *   "id": 123456789,
 *   "kakao_account": {
 *     "email": "user@example.com",
 *     "has_email": true,
 *     "is_email_valid": true,
 *     "is_email_verified": true
 *   },
 *   "properties": {
 *     "nickname": "홍길동",
 *     "profile_image": "http://k.kakaocdn.net/...",
 *     "thumbnail_image": "http://k.kakaocdn.net/..."
 *   }
 * }
 * </pre>
 * 
 * @see OAuth2UserInfo
 * @since 1.0
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

  private Map<String, Object> attributes;

  /**
   * KakaoOAuth2UserInfo 생성자
   * 
   * @param attributes 카카오 OAuth2 API로부터 받은 원본 사용자 정보
   */
  public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  /**
   * 카카오 사용자 고유 ID를 반환합니다.
   * 
   * <p>카카오는 숫자형 ID를 사용하며, 이를 문자열로 변환하여 반환합니다.</p>
   * 
   * @return 카카오 사용자 ID (예: "123456789")
   */
  @Override
  public String getId() {
    return String.valueOf(attributes.get("id"));
  }

  /**
   * 카카오 계정의 이메일 주소를 반환합니다.
   * 
   * <p>이메일은 kakao_account 객체 내에 있으며,
   * 사용자가 이메일 제공에 동의한 경우에만 값이 존재합니다.</p>
   * 
   * <p>주의사항:</p>
   * <ul>
   *   <li>이메일 수집 권한이 필요합니다 (scope: account_email)</li>
   *   <li>사용자가 동의하지 않은 경우 null을 반환합니다</li>
   * </ul>
   * 
   * @return 이메일 주소, 없거나 동의하지 않은 경우 null
   */
  @Override
  public String getEmail() {
    // kakao_account 객체에서 이메일 정보 추출
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    if (kakaoAccount != null) {
      return (String) kakaoAccount.get("email");
    }
    return null;
  }

  /**
   * 카카오 프로필 닉네임을 반환합니다.
   * 
   * <p>닉네임은 properties 객체 내에 있으며,
   * 카카오에서는 실명 대신 닉네임을 기본 이름으로 사용합니다.</p>
   * 
   * <p>주의사항:</p>
   * <ul>
   *   <li>프로필 정보 권한이 필요합니다 (scope: profile_nickname)</li>
   *   <li>BIF 사용자를 위해 친근한 닉네임을 사용하는 것이 권장됩니다</li>
   * </ul>
   * 
   * @return 사용자 닉네임, 없으면 null
   */
  @Override
  public String getName() {
    // properties 객체에서 닉네임 정보 추출
    Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
    if (properties != null) {
      return (String) properties.get("nickname");
    }
    return null;
  }

  /**
   * 카카오 프로필 이미지 URL을 반환합니다.
   * 
   * <p>프로필 이미지는 properties 객체 내에 있으며,
   * 카카오 CDN에서 제공하는 이미지 URL입니다.</p>
   * 
   * <p>이미지 관련 정보:</p>
   * <ul>
   *   <li>profile_image: 원본 크기 이미지</li>
   *   <li>thumbnail_image: 썸네일 크기 이미지 (110x110 or 100x100)</li>
   *   <li>카카오 CDN URL은 영구적이지 않을 수 있으므로 별도 저장 권장</li>
   * </ul>
   * 
   * @return 프로필 이미지 URL, 설정하지 않은 경우 null
   */
  @Override
  public String getImageUrl() {
    // properties 객체에서 프로필 이미지 URL 추출
    Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
    if (properties != null) {
      return (String) properties.get("profile_image");
    }
    return null;
  }
}