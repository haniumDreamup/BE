# BIF-AI Backend 프로젝트 구조 분석

## 1. 프로젝트 개요

BIF-AI는 경계선 지적 기능(IQ 70-85) 성인을 위한 실시간 인지 지원 시스템입니다.

### 기술 스택
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Database**: MySQL 8.0, Redis (캐싱)
- **Security**: Spring Security + JWT + OAuth2
- **API Documentation**: Swagger (SpringDoc OpenAPI)
- **Build Tool**: Gradle
- **Cloud**: AWS (EC2, RDS, S3)

## 2. 프로젝트 구조

### 2.1 패키지 구조
```
com.bifai.reminder.bifai_backend/
├── config/                 # 설정 클래스
│   ├── SecurityConfig     # 보안 설정
│   ├── SwaggerConfig      # API 문서화 설정
│   ├── DatabaseConfig     # DB 연결 설정
│   ├── RedisConfig        # 캐시 설정
│   └── FlywayConfig       # DB 마이그레이션
├── controller/            # REST API 엔드포인트
│   ├── AuthController     # 인증 관련
│   ├── UserController     # 사용자 관리
│   ├── GuardianController # 보호자 관리
│   ├── EmergencyController# 긴급상황 처리
│   └── OAuth2Controller   # 소셜 로그인
├── entity/                # JPA 엔티티
│   ├── User              # 사용자
│   ├── Guardian          # 보호자
│   ├── Emergency         # 긴급상황
│   ├── Location          # 위치정보
│   └── Geofence          # 안전구역
├── service/              # 비즈니스 로직
│   ├── UserService       # 사용자 서비스
│   ├── GuardianService   # 보호자 서비스
│   ├── EmergencyService  # 긴급 서비스
│   └── NotificationService# 알림 서비스
├── repository/           # 데이터 접근 계층
├── dto/                  # 데이터 전송 객체
│   ├── auth/            # 인증 관련 DTO
│   ├── user/            # 사용자 DTO
│   ├── guardian/        # 보호자 DTO
│   └── emergency/       # 긴급상황 DTO
├── security/            # 보안 관련
│   ├── jwt/            # JWT 토큰 처리
│   └── oauth2/         # OAuth2 처리
└── exception/          # 예외 처리
```

### 2.2 주요 엔티티

#### 핵심 엔티티
1. **User**: BIF 사용자 정보
2. **Guardian**: 보호자 정보 및 권한
3. **Emergency**: 긴급상황 기록
4. **Location**: 실시간 위치 정보
5. **Geofence**: 안전 구역 설정

#### 지원 엔티티
- **Device**: 사용자 디바이스 정보
- **Medication**: 약물 정보
- **Reminder**: 일정 알림
- **ActivityLog**: 활동 기록
- **HealthMetric**: 건강 지표

### 2.3 주요 API 엔드포인트

#### 인증 API (`/api/auth`)
- POST `/login` - 로그인
- POST `/register` - 회원가입
- POST `/refresh` - 토큰 갱신
- POST `/logout` - 로그아웃

#### 사용자 API (`/api/users`)
- GET `/profile` - 프로필 조회
- PUT `/profile` - 프로필 수정
- GET `/location` - 현재 위치
- POST `/location` - 위치 업데이트

#### 보호자 API (`/api/guardians`)
- GET `/users` - 관리 중인 사용자 목록
- POST `/request` - 보호자 요청
- PUT `/permissions` - 권한 설정

#### 긴급상황 API (`/api/emergency`)
- POST `/alert` - 긴급 알림
- GET `/history` - 긴급상황 이력
- POST `/fall-detection` - 낙상 감지

### 2.4 보안 구조

#### 인증 방식
1. **JWT 기반 인증**: 액세스 토큰 + 리프레시 토큰
2. **OAuth2 소셜 로그인**: Google, Kakao, Naver
3. **Role 기반 권한**: USER, GUARDIAN, ADMIN

#### 보안 기능
- CORS 설정
- CSRF 보호
- XSS 방지
- SQL Injection 방지
- 민감 데이터 마스킹

## 3. 데이터베이스 구조

### 3.1 주요 테이블
- `users`: 사용자 정보
- `guardians`: 보호자 관계
- `emergencies`: 긴급상황 기록
- `locations`: 위치 정보
- `geofences`: 안전 구역

### 3.2 관계
- User ↔ Guardian: 다대다
- User → Location: 일대다
- User → Emergency: 일대다
- User → Geofence: 일대다

## 4. 주요 기능 구현 상태

### 완료된 기능
- ✅ 기본 인증 시스템 (JWT)
- ✅ OAuth2 소셜 로그인
- ✅ 사용자/보호자 관리
- ✅ 긴급상황 처리
- ✅ 위치 추적
- ✅ Redis 캐싱

### 개발 중/예정
- 🔄 AI 상황 인식 (OpenAI API)
- 🔄 실시간 비디오 스트리밍
- 🔄 낙상 감지 (MediaPipe)
- 🔄 WebSocket 실시간 통신
- 🔄 푸시 알림

## 5. 성능 및 확장성

### 성능 목표
- API 응답시간: < 500ms
- 동시 사용자: 100+
- 가용성: 99.5%+

### 확장성 고려사항
- 마이크로서비스 아키텍처 준비
- 수평적 확장 가능
- 캐싱 전략 구현
- 비동기 처리 준비

## 6. 개발 환경 설정

### 필수 환경변수
```properties
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bifai_db
DB_USER=bifai_user
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=
JWT_EXPIRATION=86400000

# OAuth2
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
```

### 실행 명령어
```bash
# 개발 환경 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트 실행
./gradlew test

# 빌드
./gradlew clean build
```

## 7. 다음 단계

1. JavaDoc 문서화 개선
2. Swagger API 문서 보완
3. 통합 테스트 추가
4. 성능 테스트
5. AI 기능 통합