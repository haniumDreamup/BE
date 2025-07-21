package com.bifai.reminder.bifai_backend.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * JwtTokenProvider 단위 테스트
 * JWT 토큰 생성, 검증, 파싱 기능 테스트
 */
@DisplayName("JWT Token Provider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private Authentication authentication;
    private UserDetails userDetails;

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hs512-algorithm-requirements-64-bytes-minimum";
    private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15분
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7일
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                ACCESS_TOKEN_EXPIRATION,
                REFRESH_TOKEN_EXPIRATION
        );

        userDetails = User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }

    @Nested
    @DisplayName("토큰 생성 테스트")
    class TokenGenerationTests {

        @Test
        @DisplayName("Access Token 생성 성공")
        void generateAccessToken_Success() {
            // when
            String token = jwtTokenProvider.generateAccessToken(authentication);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT 구조: header.payload.signature
        }

        @Test
        @DisplayName("Refresh Token 생성 성공")
        void generateRefreshToken_Success() {
            // when
            String token = jwtTokenProvider.generateRefreshToken(authentication);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("생성된 토큰의 타입 확인")
        void tokenType_Check() {
            // when
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // then
            assertThat(jwtTokenProvider.getTokenType(accessToken)).isEqualTo("access");
            assertThat(jwtTokenProvider.getTokenType(refreshToken)).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("토큰 검증 테스트")
    class TokenValidationTests {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateToken_ValidToken_Success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(authentication);

            // when & then
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("잘못된 서명 토큰 검증 실패")
        void validateToken_InvalidSignature_Fail() {
            // given
            String token = jwtTokenProvider.generateAccessToken(authentication);
            String tamperedToken = token.substring(0, token.length() - 10) + "tamperedXYZ";

            // when & then
            assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("형식이 잘못된 토큰 검증 실패")
        void validateToken_MalformedToken_Fail() {
            // given
            String malformedToken = "invalid.token.format";

            // when & then
            assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();
        }

        @Test
        @DisplayName("빈 토큰 검증 실패")
        void validateToken_EmptyToken_Fail() {
            // when & then
            assertThat(jwtTokenProvider.validateToken("")).isFalse();
            assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("토큰 파싱 테스트")
    class TokenParsingTests {

        @Test
        @DisplayName("토큰에서 사용자명 추출 성공")
        void getUsernameFromToken_Success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(authentication);

            // when
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // then
            assertThat(username).isEqualTo(TEST_USERNAME);
        }

        @Test
        @DisplayName("토큰에서 만료시간 추출 성공")
        void getExpirationFromToken_Success() {
            // given
            String token = jwtTokenProvider.generateAccessToken(authentication);
            Date beforeGeneration = new Date();

            // when
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);

            // then
            assertThat(expiration).isNotNull();
            assertThat(expiration).isAfter(beforeGeneration);
            
            // Access token은 15분 후 만료
            long expectedExpiration = beforeGeneration.getTime() + ACCESS_TOKEN_EXPIRATION;
            assertThat(expiration.getTime()).isBetween(
                    expectedExpiration - 1000, // 1초 오차 허용
                    expectedExpiration + 1000
            );
        }

        @Test
        @DisplayName("잘못된 토큰에서 사용자명 추출 실패")
        void getUsernameFromToken_InvalidToken_ReturnsNull() {
            // given
            String invalidToken = "invalid.token.format";

            // when
            String username = jwtTokenProvider.getUsernameFromToken(invalidToken);

            // then
            assertThat(username).isNull();
        }
    }

    @Nested
    @DisplayName("토큰 만료 테스트")
    class TokenExpirationTests {

        @Test
        @DisplayName("새로 생성된 토큰은 만료되지 않음")
        void isTokenExpired_NewToken_False() {
            // given
            String token = jwtTokenProvider.generateAccessToken(authentication);

            // when & then
            assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰 검증")
        void validateToken_ExpiredToken() {
            // given - 만료된 토큰을 시뮬레이션하기 위해 매우 짧은 만료시간 사용
            JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
                    TEST_SECRET,
                    1L, // 1ms로 설정하여 즉시 만료
                    REFRESH_TOKEN_EXPIRATION
            );
            
            String token = shortExpirationProvider.generateAccessToken(authentication);
            
            // 토큰이 만료될 때까지 잠시 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when & then
            assertThat(shortExpirationProvider.validateToken(token)).isFalse();
            assertThat(shortExpirationProvider.isTokenExpired(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("설정값 테스트")
    class ConfigurationTests {

        @Test
        @DisplayName("Access Token 만료시간 확인")
        void getAccessTokenExpirationMs() {
            assertThat(jwtTokenProvider.getAccessTokenExpirationMs()).isEqualTo(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("Refresh Token 만료시간 확인")
        void getRefreshTokenExpirationMs() {
            assertThat(jwtTokenProvider.getRefreshTokenExpirationMs()).isEqualTo(REFRESH_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("짧은 Secret Key로 생성 시 예외 발생")
        void constructor_ShortSecretKey_ThrowsException() {
            // given
            String shortSecret = "short"; // 64바이트 미만

            // when & then
            assertThatThrownBy(() -> new JwtTokenProvider(shortSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JWT secret key must be at least 64 bytes for HS512");
        }
    }

    @Nested
    @DisplayName("보안 테스트")
    class SecurityTests {

        @Test
        @DisplayName("서로 다른 시크릿으로 생성된 토큰은 상호 검증 불가")
        void validateToken_DifferentSecret_Fail() {
            // given
            String differentSecret = "different-secret-key-that-is-also-long-enough-for-hs512-algorithm-requirements-64-bytes";
            JwtTokenProvider differentProvider = new JwtTokenProvider(
                    differentSecret,
                    ACCESS_TOKEN_EXPIRATION,
                    REFRESH_TOKEN_EXPIRATION
            );

            String tokenFromOriginal = jwtTokenProvider.generateAccessToken(authentication);

            // when & then
            assertThat(differentProvider.validateToken(tokenFromOriginal)).isFalse();
        }

        @Test
        @DisplayName("토큰 변조 감지")
        void validateToken_TamperedToken_Fail() {
            // given
            String originalToken = jwtTokenProvider.generateAccessToken(authentication);
            
            // 토큰의 페이로드 부분을 변조
            String[] parts = originalToken.split("\\.");
            String tamperedPayload = parts[1] + "XYZ"; // 페이로드에 임의 문자 추가
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            // when & then
            assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
        }
    }
} 