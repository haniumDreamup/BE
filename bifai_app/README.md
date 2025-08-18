# BIF-AI Flutter App

경계선 지적 기능(IQ 70-85) 성인을 위한 인지 지원 모바일 애플리케이션

## 프로젝트 개요

BIF-AI는 경계선 지적 기능을 가진 성인들이 일상생활을 독립적으로 수행할 수 있도록 돕는 모바일 앱입니다. 단순하고 직관적인 UI/UX를 통해 약물 복용, 일정 관리, 긴급 상황 대응 등을 지원합니다.

## 주요 기능

### 1. 인증 및 사용자 관리
- 간단한 로그인 프로세스
- 자동 로그인 지원
- 보호자 연동

### 2. 홈 대시보드
- 오늘의 중요 정보 한눈에 보기
- 날씨 및 일정 요약
- 빠른 실행 버튼

### 3. 약물 관리
- 시간별 약물 리스트
- 복용 체크 기능
- 시각적 표시 (색상, 아이콘)
- 간단한 설명

### 4. 일정 관리
- 오늘의 일정 확인
- 시각적 캘린더
- 위치 정보 표시
- 완료 체크

### 5. 긴급 상황
- SOS 버튼
- 빠른 연락처
- 보호자 자동 알림

## 기술 스택

- **Framework**: Flutter 3.x
- **State Management**: Provider
- **Navigation**: Go Router
- **Network**: Dio
- **Local Storage**: Shared Preferences, Flutter Secure Storage
- **UI**: Material Design 3

## 설치 및 실행

### 사전 요구사항
- Flutter SDK 3.0 이상
- Dart SDK 2.19 이상
- Android Studio / Xcode
- iOS Simulator / Android Emulator

### 설치 방법

1. 프로젝트 클론
```bash
cd bifai_app
```

2. 의존성 설치
```bash
flutter pub get
```

3. 환경 변수 설정
`.env` 파일에서 API URL 수정:
```
API_BASE_URL=http://your-api-url/api/v1
```

4. 실행
```bash
# iOS
flutter run -d ios

# Android
flutter run -d android

# 웹 (개발용)
flutter run -d chrome
```

## 프로젝트 구조

```
lib/
├── core/               # 핵심 유틸리티
│   ├── config/        # 앱 설정
│   ├── constants/     # 상수
│   ├── network/       # API 클라이언트
│   └── utils/         # 헬퍼 함수
├── data/              # 데이터 레이어
│   ├── models/        # 데이터 모델
│   └── repositories/  # 저장소
├── domain/            # 도메인 레이어
│   └── entities/      # 엔티티
├── presentation/      # UI 레이어
│   ├── providers/     # 상태 관리
│   ├── screens/       # 화면
│   ├── widgets/       # 위젯
│   └── routes/        # 라우팅
└── main.dart          # 앱 진입점
```

## API 연동

백엔드 API: `http://localhost:8080/api/v1/mobile`

### 주요 엔드포인트
- 인증: `/mobile/auth/*`
- 홈: `/mobile/home/*`
- 약물: `/mobile/medications/*`
- 일정: `/mobile/schedules/*`
- 긴급: `/mobile/guardians/emergency`

## 빌드

### Android APK
```bash
flutter build apk --release
```

### iOS IPA
```bash
flutter build ios --release
```
