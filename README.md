# BIF-AI Reminder - Backend

> 경계선 지능(IQ 70-85) 대상자를 위한 AI 기반 인지 보조 시스템

## 📋 프로젝트 개요

| **항목** | **내용** |
| --- | --- |
| **개발 기간** | 2025.05 ~ 2026.01 (8개월) |
| **팀 구성** | Backend 1명 (본인), Frontend 1명 |
| **배포 환경** | AWS EC2, RDS, S3 |
| **서비스 대상** | 경계선 지능 대상자 190만 명 (전체 인구 3.7%) |

한국 내 약 190만 명(전체 인구의 3.7%)에 해당하는 경계선 지능 대상자들의 일상생활을 지원하는 **Spring Boot 기반 백엔드 시스템**입니다.

### 기능 구현

• GPT-4o Vision 이미지 분석 시스템 (실시간 상황 인식, 위험 감지, 음성 가이드)

• JWT + OAuth2 인증 시스템 (Google/Kakao/Naver 소셜 로그인, Refresh Token)

• 낙상 감지 및 긴급 알림 시스템 (WebSocket + FCM, 실시간 보호자 알림)

• Geofence 안전 구역 모니터링 (Redis GeoHash, 구역 이탈 감지)

• 패턴 학습 리마인더 (Spring Batch, 행동 패턴 분석)

• 보호자 대시보드 (활동/건강/위치 실시간 모니터링)

• Google TTS 음성 안내 기능 (5학년 수준 간단한 언어)

• Spring Security 역할 기반 접근 제어 (USER/GUARDIAN/ADMIN)

• Flyway 데이터베이스 마이그레이션 및 스키마 관리

• Docker Compose 기반 로컬 개발 환경 구축

• GitHub Actions CI/CD 파이프라인 구축

### 성능 최적화 & 트러블슈팅

• **N+1 쿼리 최적화** (JOIN FETCH + @EntityGraph 적용, 쿼리 응답 170ms, TPS 109)

• **보호자 초대 동시성 제어** (synchronized + DB UNIQUE 제약, 100 VU 동시 요청 시 중복 0건)

• **Vision API 타임아웃 문제 해결** (이미지 리사이즈 + 타임아웃 설정, 3MB → 300KB 압축)

• **Circuit Breaker 패턴 적용** (Resilience4j, 외부 API 장애 격리 및 폴백 처리)

• **Event-Driven 아키텍처** (사용자 행동 로깅 비동기 처리, 메인 로직 영향 0ms)

• **Guardian 중복 생성 방지** (synchronized 블록 + DB 제약 조건, 중복 등록 0건)

• **Redis 다계층 캐싱** (자주 접근하는 데이터 사전 로드, 응답 속도 개선)

## 🛠️ 기술 스택

```
Backend    : Spring Boot 3.5.3, Java 17
Database   : MySQL 8.0, Redis 7
Cloud      : AWS (EC2, RDS, S3), Docker
Security   : Spring Security, JWT, OAuth2
AI/ML      : OpenAI GPT-4o Vision API
Monitoring : Firebase Cloud Messaging (FCM)
CI/CD      : GitHub Actions, AWS ECR
Testing    : JUnit 5, Mockito, Spring Boot Test
```

## 🚀 시작하기

### 필수 요구사항
- Java 17+
- Docker & Docker Compose
- MySQL 8.0+
- Redis

### 환경 변수 설정

```bash
# .env 파일 생성
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bifai_db
DB_USER=bifai_user
DB_PASSWORD=your_password

REDIS_HOST=localhost
REDIS_PORT=6379

AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=bifai-images

OPENAI_API_KEY=your_openai_api_key
JWT_SECRET=your_jwt_secret_minimum_64_characters
```

### 로컬 실행

```bash
# Docker로 MySQL, Redis 실행
docker-compose up -d

# Gradle 빌드 및 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 서비스 레이어 테스트만
./gradlew test --tests *ServiceTest

# 테스트 커버리지 리포트
./gradlew jacocoTestReport
```

## 📊 성능 최적화 결과

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| **AI 이미지 분석 응답 시간** | 5.2초 | 2.1초 | **70% 단축** |
| **TPS** | 11 | 211 | **1,818% 향상** |
| **메모리 사용량** | 15MB/요청 | 2MB/요청 | **87% 감소** |
| **중복 등록 오류** | 18건/일 | 0건/일 | **100% 해결** |

## 🏗️ 프로젝트 구조

```
src/
├── main/
│   ├── java/com/bifai/reminder/bifai_backend/
│   │   ├── controller/      # REST API 컨트롤러
│   │   ├── service/          # 비즈니스 로직
│   │   ├── repository/       # JPA 레포지토리
│   │   ├── entity/           # JPA 엔티티
│   │   ├── dto/              # 데이터 전송 객체
│   │   ├── config/           # Spring 설정
│   │   └── security/         # 인증/인가
│   └── resources/
│       ├── application.yml   # 애플리케이션 설정
│       └── db/migration/     # Flyway 마이그레이션
└── test/                     # 테스트 코드

scripts/                      # DB 스크립트
docker/                       # Docker 설정
local-only/                   # Git 미추적 로컬 파일
```

## 📝 API 문서

서버 실행 후 다음 URL에서 확인:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

## 🔒 보안

- JWT 기반 인증 (HS512 알고리즘)
- CORS 설정으로 Flutter 앱과 안전한 통신
- Spring Security를 통한 엔드포인트 보호
- Refresh Token은 Redis에 저장 (TTL 7일)

## 🚢 배포

### GitHub Actions CI/CD

```bash
# main 브랜치에 push하면 자동 배포
git push origin main
```

자동 실행:
1. 테스트 실행
2. Docker 이미지 빌드
3. AWS ECR에 푸시
4. EC2 인스턴스에 배포

### 수동 배포

```bash
# 프로덕션 빌드
./gradlew build -x test

# Docker 이미지 빌드
docker build -t bifai-backend .

# AWS ECR 푸시
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <account>.dkr.ecr.ap-northeast-2.amazonaws.com
docker push <account>.dkr.ecr.ap-northeast-2.amazonaws.com/bifai-backend:latest
```

## 📚 주요 의사결정

### MySQL + Redis 조합
- **MySQL**: ACID 보장, 복잡한 쿼리 지원
- **Redis**: In-Memory 캐싱, GeoHash, TTL 자동 관리

### GPT-4o Vision 선택
- Google Vision API 대비 **70% 빠른 응답 속도**
- **안정적인 SLA** 제공
- 한국어 객체 인식 우수

### Spring Security + JWT
- Stateless 인증으로 수평 확장 용이
- Refresh Token으로 보안 강화

## 🤝 기여

버그 리포트나 기능 제안은 Issues를 통해 제출해주세요.

## 📄 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

---

**개발자**: 이호준
**이메일**: ihojun@example.com
**GitHub**: [BIF-AI-Reminder](https://github.com/yourusername/bifai-backend)
