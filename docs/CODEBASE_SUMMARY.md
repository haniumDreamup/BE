# BIF-AI Backend 코드베이스 요약

## 프로젝트 개요

**BIF-AI**는 경계선 지적 기능(Borderline Intellectual Functioning, IQ 70-85) 성인을 위한 실시간 인지 지원 시스템입니다.

### 핵심 목표
- 일상생활에서의 어려움을 줄이고 독립적인 삶 지원
- 실시간 상황 인식 기반 인지 지원
- 5학년 수준의 쉬운 언어로 단계별 안내
- 보호자와의 안전한 연계 시스템

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 17
- **Database**: MySQL 8.0 (주 DB), Redis (캐싱)
- **Security**: Spring Security + JWT + OAuth2
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Build Tool**: Gradle
- **Cloud**: AWS (EC2, RDS, S3)

### 주요 의존성
- Spring Data JPA (ORM)
- Flyway (DB 마이그레이션)
- Lombok (보일러플레이트 감소)
- MapStruct (DTO 매핑)
- JWT (인증 토큰)
- AWS SDK (클라우드 서비스)
- Micrometer (모니터링)

## 아키텍처

### 계층 구조
```
┌─────────────────────────────────────┐
│        Controller Layer             │ ← REST API 엔드포인트
├─────────────────────────────────────┤
│         Service Layer               │ ← 비즈니스 로직
├─────────────────────────────────────┤
│        Repository Layer             │ ← 데이터 접근
├─────────────────────────────────────┤
│      Entity/Domain Layer            │ ← 도메인 모델
└─────────────────────────────────────┘
```

### 주요 컴포넌트

#### 1. 엔티티 (25개)
- **핵심 엔티티**: User, Guardian, Emergency, Location, Geofence
- **지원 엔티티**: Device, Medication, Reminder, ActivityLog, HealthMetric
- **관계**: 다대다 (User ↔ Guardian), 일대다 (User → Location/Emergency)

#### 2. 컨트롤러 (7개)
- **AuthController**: 인증/인가 (회원가입, 로그인, 토큰 갱신)
- **UserController**: 사용자 관리
- **GuardianController**: 보호자 관리
- **EmergencyController**: 긴급상황 처리
- **OAuth2Controller**: 소셜 로그인
- **AdminController**: 관리자 기능
- **HealthController**: 헬스체크

#### 3. 서비스
- **AuthService**: 인증 로직
- **UserService**: 사용자 비즈니스 로직
- **GuardianService**: 보호자 관계 관리
- **EmergencyService**: 긴급상황 처리
- **NotificationService**: 알림 발송

#### 4. 보안
- JWT 기반 인증 (액세스 + 리프레시 토큰)
- OAuth2 소셜 로그인 (Google, Kakao, Naver)
- Role 기반 권한 관리 (USER, GUARDIAN, ADMIN)
- CORS 설정 및 CSRF 보호

## API 구조

### 기본 경로: `/api/v1`

### 주요 엔드포인트
```
POST   /auth/register        - 회원가입
POST   /auth/login          - 로그인
POST   /auth/refresh        - 토큰 갱신
POST   /auth/logout         - 로그아웃

GET    /users/profile       - 프로필 조회
PUT    /users/profile       - 프로필 수정
POST   /users/location      - 위치 업데이트

GET    /guardians/users     - 관리 중인 사용자
POST   /guardians/request   - 보호자 요청
PUT    /guardians/permissions - 권한 설정

POST   /emergency/alert     - 긴급 알림
POST   /emergency/fall-detection - 낙상 감지
```

### 응답 형식
```json
{
  "success": true,
  "data": {},
  "message": "작업이 완료되었습니다",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "USER_FRIENDLY_CODE",
    "message": "쉽게 이해할 수 있는 설명",
    "userAction": "사용자가 할 수 있는 조치"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

## BIF 사용자를 위한 특별 기능

### 1. 인지 수준 맞춤형 UI/UX
- **MILD**: 독립적 수행 가능 → 기본 인터페이스
- **MODERATE**: 중등도 지원 필요 → 단순화된 인터페이스
- **SEVERE**: 지속적 지원 필요 → 최대한 단순한 인터페이스

### 2. 안전 기능
- 실시간 위치 추적 (GPS)
- 안전 구역 이탈 알림 (Geofence)
- 낙상 감지 (MediaPipe Pose 예정)
- 긴급 호출 버튼

### 3. 일상 지원
- 약물 복용 알림
- 일정 관리 (패턴 학습)
- 단계별 작업 안내
- 음성/시각적 알림

## 개발 현황

### 완료된 기능 ✅
- 기본 인증 시스템 (JWT)
- OAuth2 소셜 로그인
- 사용자/보호자 관리
- 긴급상황 처리 API
- 위치 추적 기능
- Redis 캐싱
- Swagger API 문서화

### 개발 예정 🔄
- AI 상황 인식 (OpenAI API)
- 실시간 비디오 스트리밍 (WebRTC)
- 낙상 감지 (MediaPipe)
- WebSocket 실시간 통신
- 푸시 알림 (FCM)
- 음성 인터페이스

## 개발 환경 설정

### 필수 환경변수
```properties
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bifai_db
DB_USER=bifai_user
DB_PASSWORD=<secure>

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=<secret>
JWT_EXPIRATION=86400000

# OAuth2
GOOGLE_CLIENT_ID=<id>
GOOGLE_CLIENT_SECRET=<secret>
```

### 실행 명령어
```bash
# 개발 환경 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 테스트 실행
./gradlew test

# 빌드
./gradlew clean build

# DB 마이그레이션
./gradlew flywayMigrate
```

## 문서화

### JavaDoc
- 모든 public 클래스와 메소드에 JavaDoc 작성
- BIF 사용자 특성을 고려한 쉬운 설명
- 예제 코드 포함

### Swagger UI
- URL: http://localhost:8080/swagger-ui/index.html
- 모든 API 엔드포인트 문서화
- 요청/응답 예시 제공
- 인증 방법 안내

## 성능 목표
- API 응답시간: < 500ms
- AI 분석시간: < 3초
- 동시 사용자: 100+
- 시스템 가용성: 99.5%+

## 보안 고려사항
- 모든 API는 인증 필요 (헬스체크 제외)
- 민감 데이터 암호화
- SQL Injection 방지
- XSS/CSRF 보호
- 로그 마스킹

## 향후 계획
1. 마이크로서비스 아키텍처 전환
2. 쿠버네티스 배포
3. GraphQL API 추가
4. 실시간 분석 대시보드
5. 다국어 지원