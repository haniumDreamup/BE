# BIF-AI Flutter App Architecture

## 프로젝트 구조

```
lib/
├── core/
│   ├── config/
│   │   ├── app_config.dart
│   │   ├── api_config.dart
│   │   └── theme_config.dart
│   ├── constants/
│   │   ├── app_constants.dart
│   │   ├── api_endpoints.dart
│   │   └── storage_keys.dart
│   ├── errors/
│   │   ├── exceptions.dart
│   │   └── failures.dart
│   ├── network/
│   │   ├── api_client.dart
│   │   ├── interceptors/
│   │   └── response_models/
│   └── utils/
│       ├── validators.dart
│       ├── formatters.dart
│       └── helpers.dart
├── data/
│   ├── datasources/
│   │   ├── local/
│   │   └── remote/
│   ├── models/
│   └── repositories/
├── domain/
│   ├── entities/
│   ├── repositories/
│   └── usecases/
├── presentation/
│   ├── providers/
│   ├── screens/
│   │   ├── auth/
│   │   ├── home/
│   │   ├── medication/
│   │   ├── schedule/
│   │   └── emergency/
│   ├── widgets/
│   │   ├── common/
│   │   └── custom/
│   └── routes/
└── main.dart
```

## 아키텍처 패턴

### Clean Architecture + Provider
- **Presentation Layer**: UI 및 상태 관리 (Provider)
- **Domain Layer**: 비즈니스 로직 (Use Cases, Entities)
- **Data Layer**: 데이터 소스 및 저장소 구현

## 주요 패키지

### 상태 관리
- `provider`: ^6.1.1
- `flutter_riverpod`: ^2.4.9 (선택적)

### 네트워킹
- `dio`: ^5.4.0
- `retrofit`: ^4.0.3
- `pretty_dio_logger`: ^1.3.1

### 로컬 저장소
- `shared_preferences`: ^2.2.2
- `flutter_secure_storage`: ^9.0.0
- `hive_flutter`: ^1.1.0

### 네비게이션
- `go_router`: ^13.0.1
- `auto_route`: ^7.8.4 (선택적)

### UI/UX
- `flutter_screenutil`: ^5.9.0
- `flutter_svg`: ^2.0.9
- `lottie`: ^2.7.0
- `cached_network_image`: ^3.3.0

### 유틸리티
- `intl`: ^0.18.1
- `equatable`: ^2.0.5
- `freezed`: ^2.4.6
- `json_annotation`: ^4.8.1
- `flutter_dotenv`: ^5.1.0

### 디바이스 기능
- `permission_handler`: ^11.1.0
- `image_picker`: ^1.0.5
- `geolocator`: ^10.1.0
- `firebase_messaging`: ^14.7.6

## 설계 원칙

### 1. 인지 친화적 UI
- 큰 터치 타겟 (최소 48dp)
- 단순한 네비게이션 (최대 2단계)
- 명확한 시각적 피드백
- 고대비 색상 사용

### 2. 접근성
- 큰 폰트 크기 (최소 16sp)
- 음성 안내 지원
- 단순한 언어 사용 (5학년 수준)
- 아이콘과 텍스트 병행

### 3. 성능 최적화
- 이미지 캐싱
- 레이지 로딩
- 오프라인 모드 지원
- 빠른 응답 (< 3초)

### 4. 보안
- JWT 토큰 관리
- 민감 정보 암호화
- 자동 로그아웃
- 생체 인증 지원

## API 통신 전략

### 1. 에러 처리
- 사용자 친화적 메시지
- 자동 재시도 메커니즘
- 오프라인 큐잉

### 2. 데이터 캐싱
- 네트워크 우선 전략
- 로컬 데이터 폴백
- 자동 동기화

### 3. 상태 관리
- 로딩 상태 표시
- 에러 상태 처리
- 성공 피드백

## 테마 및 스타일

### 색상 팔레트
- Primary: #4ECDC4 (민트)
- Secondary: #FF6B6B (코랄)
- Success: #51CF66 (그린)
- Warning: #FFD93D (옐로우)
- Error: #FF6B6B (레드)
- Background: #F8F9FA
- Text: #212529

### 타이포그래피
- 제목: 24sp, Bold
- 부제목: 18sp, SemiBold
- 본문: 16sp, Regular
- 캡션: 14sp, Regular

### 간격 시스템
- xs: 4dp
- sm: 8dp
- md: 16dp
- lg: 24dp
- xl: 32dp