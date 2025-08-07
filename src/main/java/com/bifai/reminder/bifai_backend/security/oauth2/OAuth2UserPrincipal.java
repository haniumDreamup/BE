package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth2 인증된 사용자 Principal
 * 
 * <p>Spring Security에서 OAuth2 사용자를 나타내는 클래스입니다.
 * {@link OAuth2User}와 {@link UserDetails} 인터페이스를 모두 구현하여
 * OAuth2 인증과 JWT 인증에서 모두 사용할 수 있습니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>OAuth2 제공자로부터 받은 원본 사용자 정보 보관</li>
 *   <li>BIF 시스템 사용자 엔티티와 연결</li>
 *   <li>Spring Security 권한 처리 지원</li>
 * </ul>
 * 
 * @see OAuth2User
 * @see UserDetails
 * @since 1.0
 */
@Getter
public class OAuth2UserPrincipal implements OAuth2User, UserDetails {

  private final User user;
  private final Map<String, Object> attributes;

  /**
   * OAuth2UserPrincipal 생성자
   * 
   * @param user BIF 시스템 사용자 엔티티
   * @param attributes OAuth2 제공자로부터 받은 원본 사용자 정보
   */
  public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  /**
   * OAuth2 제공자로부터 받은 원본 사용자 정보를 반환합니다.
   * 
   * @return OAuth2 제공자의 원본 사용자 정보 Map
   */
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * 사용자의 권한 목록을 반환합니다.
   * 
   * <p>데이터베이스에 저장된 Role을 Spring Security의 GrantedAuthority로 변환합니다.
   * 모든 권한에는 "ROLE_" 접두사가 붙습니다.</p>
   * 
   * @return 사용자 권한 목록
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return user.getRoles().stream()
      .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
      .collect(Collectors.toList());
  }

  /**
   * OAuth2 사용자의 고유 식별자를 반환합니다.
   * 
   * @return 사용자 ID
   */
  @Override
  public String getName() {
    return user.getId().toString();
  }

  /**
   * 사용자명(로그인 ID)을 반환합니다.
   * 
   * <p>BIF 시스템에서는 이메일을 사용자명으로 사용합니다.</p>
   * 
   * @return 사용자 이메일
   */
  @Override
  public String getUsername() {
    return user.getEmail();
  }

  /**
   * 비밀번호를 반환합니다.
   * 
   * <p>OAuth2 로그인은 비밀번호를 사용하지 않으므로 null을 반환합니다.</p>
   * 
   * @return null
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * 계정 만료 여부를 확인합니다.
   * 
   * <p>현재는 계정 만료 기능을 사용하지 않으므로 항상 true를 반환합니다.</p>
   * 
   * @return true (만료되지 않음)
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 계정 잠김 여부를 확인합니다.
   * 
   * <p>사용자의 isActive 상태를 기반으로 판단합니다.</p>
   * 
   * @return 계정 활성 상태
   */
  @Override
  public boolean isAccountNonLocked() {
    return user.isActive();
  }

  /**
   * 인증 정보 만료 여부를 확인합니다.
   * 
   * <p>OAuth2 인증은 토큰 기반이므로 항상 true를 반환합니다.</p>
   * 
   * @return true (만료되지 않음)
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 계정 활성화 여부를 확인합니다.
   * 
   * <p>사용자의 isActive 상태를 기반으로 판단합니다.</p>
   * 
   * @return 계정 활성 상태
   */
  @Override
  public boolean isEnabled() {
    return user.isActive();
  }
}