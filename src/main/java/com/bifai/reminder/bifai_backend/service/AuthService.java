package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.auth.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 서비스
 * BIF 사용자를 위한 회원가입, 로그인, 토큰 관리 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final BifUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 사용자 회원가입
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("새 사용자 회원가입 시도: {}", request.getEmail());

        // 입력 검증
        validateRegisterRequest(request);

        // 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다");
        }

        // 사용자 생성
        User user = createUserFromRequest(request);
        User savedUser = userRepository.save(user);

        log.info("새 사용자 등록 완료: userId={}, username={}", savedUser.getUserId(), savedUser.getUsername());

        // 자동 로그인 처리
        return loginUser(savedUser);
    }

    /**
     * 사용자 로그인
     */
    public AuthResponse login(LoginRequest request) {
        log.info("사용자 로그인 시도: {}", request.getUsernameOrEmail());

        // 인증 처리
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        // 인증 성공 시 컨텍스트에 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 사용자 정보 조회 및 로그인 시간 업데이트
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        user.updateLastLogin();
        userRepository.save(user);

        log.info("사용자 로그인 성공: userId={}, username={}", user.getUserId(), user.getUsername());

        return loginUser(user);
    }

    /**
     * 토큰 갱신
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다");
        }

        // Redis에서 Refresh Token 검증
        Long userId = refreshTokenService.validateRefreshToken(refreshToken);
        if (userId == null) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다");
        }

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .filter(u -> u.getIsActive())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        log.info("토큰 갱신: userId={}, username={}", user.getUserId(), user.getUsername());

        // 새로운 토큰 생성 (Token Rotation)
        AuthResponse response = loginUser(user);
        
        // 기존 Refresh Token 무효화 및 새 토큰 저장
        refreshTokenService.rotateRefreshToken(
            refreshToken, 
            response.getRefreshToken(), 
            user.getUserId(), 
            jwtTokenProvider.getRefreshTokenExpirationMs()
        );

        return response;
    }

    /**
     * 로그아웃
     */
    public void logout() {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsernameOrEmail(username)
                    .orElse(null);
            
            if (user != null) {
                // Redis에서 Refresh Token 삭제
                refreshTokenService.deleteRefreshToken(user.getUserId());
                log.info("사용자 로그아웃 완료: userId={}, username={}", user.getUserId(), user.getUsername());
            }
        }
        
        SecurityContextHolder.clearContext();
    }

    /**
     * 회원가입 요청 검증
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.isPasswordMatching()) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        if (request.getAgreeToTerms() == null || !request.getAgreeToTerms()) {
            throw new IllegalArgumentException("이용약관에 동의해주세요");
        }

        if (request.getAgreeToPrivacyPolicy() == null || !request.getAgreeToPrivacyPolicy()) {
            throw new IllegalArgumentException("개인정보 처리방침에 동의해주세요");
        }
    }

    /**
     * 회원가입 요청으로부터 User 엔티티 생성
     */
    private User createUserFromRequest(RegisterRequest request) {
        // Guardian 관계는 User 생성 후 별도로 처리해야 함 (새로운 스키마에 따라)
        
        // User 엔티티 생성
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getFullName())
                .fullName(request.getFullName())
                .cognitiveLevel(User.CognitiveLevel.MODERATE) // 기본값
                .isActive(true)
                .build();
                
        return user;
    }

    /**
     * 사용자 로그인 처리 (토큰 생성 및 응답 구성)
     */
    private AuthResponse loginUser(User user) {
        // BifUserDetails 래핑
        BifUserDetails userDetails = new BifUserDetails(user);
        
        // 인증 객체 생성 - Principal을 BifUserDetails로 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,  // UserDetails 객체를 principal로 사용
                null,
                userDetails.getAuthorities()  // BifUserDetails에서 권한 정보 가져오기
        );

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(
            user.getUserId(), 
            refreshToken, 
            jwtTokenProvider.getRefreshTokenExpirationMs()
        );

        // 응답 구성
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .refreshTokenExpiresIn(jwtTokenProvider.getRefreshTokenExpirationMs())
                .user(AuthResponse.UserInfo.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .build())
                .loginTime(LocalDateTime.now())
                .build();
    }
} 