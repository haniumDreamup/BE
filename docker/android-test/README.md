# Docker Android Testing Environment

안드로이드 환경에서 Flutter 앱을 시각적으로 테스트하고 로그를 분석할 수 있는 Docker 환경입니다.

## 기능

- Android 에뮬레이터 (API 33, x86_64)
- Flutter 개발 환경
- VNC 원격 접속으로 시각적 확인
- 실시간 로그 모니터링
- Backend API 연동 테스트

## 사용 방법

### 1. 환경 구성

```bash
cd /Users/ihojun/Desktop/javaWorkSpace/BE/docker/android-test
```

### 2. Docker 이미지 빌드 및 실행

```bash
# 빌드 및 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

### 3. VNC로 시각적 접속

1. VNC 클라이언트 설치 (예: RealVNC Viewer)
2. 주소: `localhost:5901`
3. 비밀번호: 없음

### 4. Flutter 앱 테스트

Flutter 앱은 자동으로 `/app` 디렉토리에 마운트됩니다:

```bash
# 컨테이너 내부 접속
docker exec -it bifai-android-test bash

# Flutter 명령어 실행
flutter devices
flutter run -d android
```

### 5. 로그 확인

```bash
# 실시간 로그 확인
docker-compose logs -f android-test

# ADB 로그 확인
docker exec -it bifai-android-test adb logcat

# Flutter 로그 확인
docker exec -it bifai-android-test flutter logs
```

## 포트 설정

- `5901`: VNC 접속
- `5555`: ADB 디버깅
- `8080`: Backend API
- `3000`, `3004`, `3005`: Flutter 웹 개발 서버

## 문제 해결

### 에뮬레이터가 시작되지 않는 경우

```bash
# 컨테이너 재시작
docker-compose restart android-test

# 에뮬레이터 수동 시작
docker exec -it bifai-android-test emulator -avd test_android -no-window
```

### VNC 연결이 안 되는 경우

```bash
# X11VNC 상태 확인
docker exec -it bifai-android-test ps aux | grep x11vnc

# X11VNC 재시작
docker exec -it bifai-android-test supervisorctl restart x11vnc
```

### Flutter 앱이 실행되지 않는 경우

```bash
# Flutter doctor 확인
docker exec -it bifai-android-test flutter doctor

# 안드로이드 라이센스 확인
docker exec -it bifai-android-test yes | sdkmanager --licenses
```

## 환경 정리

```bash
# 컨테이너 중지 및 제거
docker-compose down

# 볼륨까지 삭제 (완전 초기화)
docker-compose down -v

# 이미지 삭제
docker rmi bifai-android-test_android-test
```

## 백엔드 연동 테스트

실제 배포된 프로덕션 서버(43.200.49.171:8080)로 API 요청을 보냅니다:

```bash
# 백엔드 연결 테스트
curl http://43.200.49.171:8080/api/v1/health

# Flutter 앱에서 API_BASE_URL 설정 (자동 설정됨)
export API_BASE_URL=http://43.200.49.171:8080

# 로컬 백엔드 테스트가 필요한 경우
export API_BASE_URL=http://host.docker.internal:8080
```

## 추가 기능

### 스크린샷 캡처

```bash
# 에뮬레이터 스크린샷
docker exec -it bifai-android-test adb shell screencap -p /sdcard/screenshot.png
docker exec -it bifai-android-test adb pull /sdcard/screenshot.png
```

### 앱 설치 및 테스트

```bash
# APK 설치
docker exec -it bifai-android-test adb install app.apk

# 앱 실행
docker exec -it bifai-android-test adb shell am start -n com.example.app/.MainActivity
```

이 환경을 통해 실제 안드로이드 디바이스 환경에서 Flutter 앱의 동작을 시각적으로 확인하고 API 호출 로그를 분석할 수 있습니다.