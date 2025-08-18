# BIF-AI Flutter & Spring Boot 연동 가이드

## 시작하기

### 1. 전체 시스템 실행
```bash
# 프로젝트 루트에서
./run-app-with-backend.sh
```

### 2. 개별 실행

#### 백엔드 서버
```bash
./gradlew bootRun
```
- URL: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

#### Flutter 앱
```bash
cd bifai_app
flutter run
```

## API 연동 확인

### 1. 테스트 계정
```
이메일: test@bifai.com
비밀번호: Test1234!
```

### 2. 로그인 테스트
1. Flutter 앱 실행
2. 로그인 화면에서 테스트 계정 입력
3. 로그인 성공 시 홈 화면으로 이동

### 3. API 엔드포인트

#### 인증
- POST `/api/v1/mobile/auth/login` - 로그인
- POST `/api/v1/mobile/auth/logout` - 로그아웃  
- POST `/api/v1/mobile/auth/refresh` - 토큰 갱신

#### 홈
- GET `/api/v1/mobile/home/dashboard` - 대시보드

#### 약물
- GET `/api/v1/mobile/medications/today` - 오늘의 약물
- POST `/api/v1/mobile/medications/{id}/take` - 약물 복용

#### 일정
- GET `/api/v1/mobile/schedules/today` - 오늘의 일정
- PUT `/api/v1/mobile/schedules/{id}/complete` - 일정 완료

## 디버깅

### 백엔드 로그 확인
```bash
tail -f logs/bifai.log
```

### Flutter 디버그 모드
```bash
flutter run --debug
```

### 네트워크 요청 확인
Flutter 앱의 `lib/core/network/api_client.dart`에서 `PrettyDioLogger`가 활성화되어 있으면 콘솔에서 모든 API 요청/응답을 확인할 수 있습니다.

## 트러블슈팅

### 1. 연결 실패
- 백엔드 서버가 실행 중인지 확인
- `.env` 파일의 `API_BASE_URL` 확인
- iOS Simulator의 경우 `localhost` 대신 호스트 머신 IP 사용

### 2. 인증 오류
- JWT 토큰 만료 시간 확인 (기본 1시간)
- 리프레시 토큰 자동 갱신 동작 확인

### 3. CORS 오류 (웹)
```yaml
# application.yml에 추가
cors:
  allowed-origins: 
    - http://localhost:3000
    - http://localhost:*
```

## 환경 설정

### 개발 환경
```bash
# bifai_app/.env
API_BASE_URL=http://localhost:8080/api/v1
ENVIRONMENT=development
ENABLE_LOGGING=true
```

### 프로덕션 환경
```bash
# bifai_app/.env.prod
API_BASE_URL=https://api.bifai.com/api/v1
ENVIRONMENT=production
ENABLE_LOGGING=false
```

## 주요 기능 테스트

### 1. 자동 로그인
1. 로그인 후 앱 종료
2. 앱 재실행
3. 자동으로 홈 화면으로 이동 확인

### 2. 토큰 갱신
1. 액세스 토큰 만료 대기 (1시간)
2. API 요청 시 자동 갱신 확인

### 3. 오프라인 모드
1. 네트워크 차단
2. 캐시된 데이터 표시 확인
3. 네트워크 복구 시 자동 동기화

## 성능 최적화

### 이미지 캐싱
- `cached_network_image` 패키지 사용
- 최대 캐시 크기: 100MB

### API 응답 캐싱
- GET 요청 1시간 캐싱
- 오프라인 시 캐시 데이터 사용

### 로딩 최적화
- 레이지 로딩
- 페이지네이션 (20개씩)
- Shimmer 효과로 로딩 표시