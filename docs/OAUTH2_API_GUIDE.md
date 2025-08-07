# BIF-AI OAuth2 API 가이드

## 구현된 OAuth2 엔드포인트

### 1. OAuth2 로그인 URL 조회
**GET** `/api/v1/auth/oauth2/login-urls`

OAuth2 제공자별 로그인 URL을 반환합니다.

```bash
curl -X GET http://localhost:8080/api/v1/auth/oauth2/login-urls
```

**응답 예시:**
```json
{
  "kakao": "/oauth2/authorization/kakao",
  "naver": "/oauth2/authorization/naver", 
  "google": "/oauth2/authorization/google"
}
```

### 2. OAuth2 로그인 시작
각 제공자별 OAuth2 로그인을 시작하려면 다음 URL로 리다이렉트:

- **카카오**: `/oauth2/authorization/kakao`
- **네이버**: `/oauth2/authorization/naver`
- **구글**: `/oauth2/authorization/google`

### 3. OAuth2 콜백 처리
Spring Security OAuth2 Client가 자동으로 처리:
- `/login/oauth2/code/{registrationId}`

### 4. 일반 인증 API

#### 회원가입
**POST** `/auth/register`
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

#### 로그인
**POST** `/auth/login`
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### 토큰 갱신
**POST** `/auth/refresh`
```json
{
  "refreshToken": "your-refresh-token"
}
```

#### 로그아웃
**POST** `/auth/logout`
헤더: `Authorization: Bearer {access-token}`

#### 헬스체크
**GET** `/auth/health`

## OAuth2 로그인 프로세스

1. 클라이언트가 `/api/v1/auth/oauth2/login-urls` 호출하여 로그인 URL 획득
2. 사용자를 선택한 제공자의 OAuth2 URL로 리다이렉트
3. 사용자가 제공자 사이트에서 로그인 및 권한 부여
4. 제공자가 콜백 URL로 리다이렉트
5. Spring Security가 OAuth2 사용자 정보 처리
6. `CustomOAuth2UserService`가 사용자 정보를 DB에 저장/업데이트
7. `OAuth2AuthenticationSuccessHandler`가 JWT 토큰 생성 및 반환

## 주요 컴포넌트

### OAuth2UserInfo 인터페이스
각 제공자별 사용자 정보를 표준화하는 인터페이스

### OAuth2UserInfoFactory
제공자별 구현체를 생성하는 팩토리 클래스

### CustomOAuth2UserService
OAuth2 로그인 성공 시 사용자 정보를 처리하는 서비스

### OAuth2AuthenticationSuccessHandler
로그인 성공 후 JWT 토큰을 생성하고 클라이언트로 리다이렉트

## 보안 고려사항

1. HTTPS 사용 필수
2. OAuth2 state 파라미터로 CSRF 방지
3. JWT 토큰 안전한 저장 (HttpOnly Cookie 권장)
4. 적절한 CORS 설정
5. Rate Limiting 적용

## 테스트 방법

1. 로컬에서 테스트 시 각 OAuth2 제공자의 리다이렉트 URI 설정 필요:
   - 카카오: http://localhost:8080/login/oauth2/code/kakao
   - 네이버: http://localhost:8080/login/oauth2/code/naver
   - 구글: http://localhost:8080/login/oauth2/code/google

2. 환경변수 설정:
   ```
   KAKAO_CLIENT_ID=your-kakao-client-id
   KAKAO_CLIENT_SECRET=your-kakao-client-secret
   NAVER_CLIENT_ID=your-naver-client-id
   NAVER_CLIENT_SECRET=your-naver-client-secret
   GOOGLE_CLIENT_ID=your-google-client-id
   GOOGLE_CLIENT_SECRET=your-google-client-secret
   ```

3. Swagger UI 사용:
   http://localhost:8080/swagger-ui.html

## 문제 해결

### Entity 인식 문제
현재 JPA 엔티티 스캔 문제로 애플리케이션 실행이 안 되는 경우:
1. `@EntityScan` 어노테이션 확인
2. 엔티티 클래스의 `@Entity` 어노테이션 확인
3. 패키지 구조 확인

### H2 데이터베이스 사용
개발/테스트 환경에서 H2 인메모리 DB 사용:
- H2 Console: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- Username: sa
- Password: (비어있음)