package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * OAuth2 사용자 정보 처리 서비스
 * 
 * <p>OAuth2 로그인 성공 후 사용자 정보를 받아서 처리하는 핵심 서비스입니다.
 * Spring Security의 {@link DefaultOAuth2UserService}를 확장하여 구현합니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>OAuth2 제공자로부터 사용자 정보 로드</li>
 *   <li>신규 사용자 자동 등록</li>
 *   <li>기존 사용자 정보 업데이트</li>
 *   <li>USER 권한 자동 부여</li>
 * </ul>
 * 
 * @see OAuth2UserInfoFactory
 * @see OAuth2UserPrincipal
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  /**
   * OAuth2 사용자 정보를 로드하고 처리합니다.
   * 
   * <p>처리 과정:</p>
   * <ol>
   *   <li>OAuth2 제공자로부터 사용자 정보 가져오기</li>
   *   <li>제공자별 사용자 정보 파싱</li>
   *   <li>데이터베이스에 사용자 저장 또는 업데이트</li>
   *   <li>OAuth2UserPrincipal 객체 생성 및 반환</li>
   * </ol>
   * 
   * @param userRequest OAuth2 인증 요청 정보
   * @return 인증된 OAuth2 사용자 정보
   * @throws OAuth2AuthenticationException OAuth2 인증 실패 시
   */
  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    // 1. 기본 OAuth2UserService를 통해 사용자 정보 로드
    OAuth2User oauth2User = super.loadUser(userRequest);
    
    // 2. OAuth2 제공자 식별 (kakao, naver, google)
    String provider = userRequest.getClientRegistration().getRegistrationId();
    
    // 3. 제공자별 사용자 정보 파싱
    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(provider, oauth2User.getAttributes());
    
    // 4. 사용자 정보 저장 또는 업데이트
    User user = saveOrUpdateUser(userInfo, provider);
    
    // 5. Spring Security에서 사용할 Principal 객체 생성
    return new OAuth2UserPrincipal(user, oauth2User.getAttributes());
  }

  /**
   * OAuth2 사용자 정보를 데이터베이스에 저장하거나 업데이트합니다.
   * 
   * <p>처리 로직:</p>
   * <ul>
   *   <li>기존 사용자: 이름과 프로필 이미지만 업데이트</li>
   *   <li>신규 사용자: 전체 정보 저장 및 USER 권한 부여</li>
   * </ul>
   * 
   * <p>BIF 사용자 처리 특징:</p>
   * <ul>
   *   <li>간단한 가입 프로세스 (자동 가입)</li>
   *   <li>필수 정보만 수집</li>
   *   <li>기본 USER 권한 자동 부여</li>
   * </ul>
   * 
   * @param userInfo OAuth2 제공자로부터 받은 사용자 정보
   * @param provider OAuth2 제공자 이름 (kakao, naver, google)
   * @return 저장된 사용자 엔티티
   */
  private User saveOrUpdateUser(OAuth2UserInfo userInfo, String provider) {
    // 이메일로 기존 사용자 검색
    Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());
    
    User user;
    if (existingUser.isPresent()) {
      // 기존 사용자의 경우: 프로필 정보만 업데이트
      user = existingUser.get();
      user.setName(userInfo.getName());
      user.setProfileImageUrl(userInfo.getImageUrl());
      // 마지막 로그인 시간 업데이트
      user.updateLastLogin();
      
      log.info("Existing user logged in via OAuth2: {} ({})", user.getEmail(), provider);
    } else {
      // 신규 사용자의 경우: 새로운 계정 생성
      // USER 권한 검색
      Role userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("User role not found"));
      
      // 새 사용자 객체 생성
      user = User.builder()
        .email(userInfo.getEmail())
        .name(userInfo.getName())
        .username(userInfo.getEmail()) // 이메일을 username으로 사용
        .provider(provider)
        .providerId(userInfo.getId())
        .profileImageUrl(userInfo.getImageUrl())
        .isActive(true)
        .emailVerified(true) // OAuth2 로그인은 이미 이메일 인증됨
        .roles(Set.of(userRole))
        .build();
      
      log.info("New user registered via OAuth2: {} ({})", user.getEmail(), provider);
    }
    
    // 데이터베이스에 저장
    return userRepository.save(user);
  }
}