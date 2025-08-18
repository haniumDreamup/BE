# Firebase Cloud Messaging (FCM) 설정 가이드

## 1. Firebase 프로젝트 설정

### Firebase Console에서 프로젝트 생성
1. [Firebase Console](https://console.firebase.google.com/) 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름: `bif-ai-reminder` 입력
4. Google Analytics 설정 (선택사항)

### Android 앱 추가
1. Firebase Console > 프로젝트 설정 > 앱 추가
2. Android 패키지 이름: `com.bifai.reminder`
3. 앱 닉네임: `BIF-AI Reminder`
4. `google-services.json` 다운로드
5. 파일을 `/bifai_app/android/app/` 디렉토리에 복사

### iOS 앱 추가
1. Firebase Console > 프로젝트 설정 > 앱 추가
2. iOS 번들 ID: `com.bifai.reminder`
3. 앱 닉네임: `BIF-AI Reminder`
4. `GoogleService-Info.plist` 다운로드
5. 파일을 `/bifai_app/ios/Runner/` 디렉토리에 복사

### 서비스 계정 키 생성 (백엔드용)
1. Firebase Console > 프로젝트 설정 > 서비스 계정
2. "새 비공개 키 생성" 클릭
3. JSON 키 파일 다운로드
4. 파일명을 `firebase-service-account.json`으로 변경
5. `/src/main/resources/` 디렉토리에 복사

## 2. Flutter 앱 설정

### 필요한 패키지 설치
```bash
flutter pub add firebase_core firebase_messaging flutter_local_notifications
```

### Firebase 초기화
```dart
// main.dart
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  runApp(MyApp());
}
```

### Android 설정
`android/app/build.gradle`:
```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-messaging'
}
```

`android/build.gradle`:
```gradle
dependencies {
    classpath 'com.google.gms:google-services:4.4.0'
}
```

### iOS 설정
`ios/Runner/Info.plist`:
```xml
<key>UIBackgroundModes</key>
<array>
    <string>fetch</string>
    <string>remote-notification</string>
</array>
```

## 3. Spring Boot 백엔드 설정

### 의존성 추가
`build.gradle`:
```gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

### application.yml 설정
```yaml
fcm:
  key-path: firebase-service-account.json
  enabled: true
```

### FCM 서비스 초기화
```java
@PostConstruct
public void initialize() {
    GoogleCredentials googleCredentials = GoogleCredentials
        .fromStream(new ClassPathResource(fcmKeyPath).getInputStream());
    
    FirebaseOptions firebaseOptions = FirebaseOptions.builder()
        .setCredentials(googleCredentials)
        .build();
    
    FirebaseApp.initializeApp(firebaseOptions);
}
```

## 4. 푸시 알림 테스트

### 1. FCM 토큰 획득 (Flutter)
```dart
String? token = await FirebaseMessaging.instance.getToken();
print('FCM Token: $token');
```

### 2. 백엔드에 토큰 등록
```bash
curl -X POST http://localhost:8080/api/v1/notifications/fcm-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "device123",
    "fcmToken": "YOUR_FCM_TOKEN"
  }'
```

### 3. 테스트 알림 전송
```bash
curl -X POST http://localhost:8080/api/v1/notifications/test \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "테스트 알림",
    "body": "이것은 테스트 알림입니다"
  }'
```

## 5. 알림 종류

### 약물 복용 알림
- 예정된 시간에 자동 전송
- 복용 확인 액션 포함
- 놓친 약물 재알림

### 일정 알림
- 일정 30분 전 알림
- 위치 정보 포함
- 일정 상세 보기 링크

### 긴급 알림
- 보호자에게 즉시 전송
- 위치 정보 포함
- 최우선 순위로 표시

### 일일 요약
- 매일 오후 9시 전송
- 오늘의 성과 요약
- 내일 일정 미리보기

## 6. 문제 해결

### FCM 토큰이 null인 경우
1. 인터넷 연결 확인
2. Firebase 프로젝트 설정 확인
3. google-services.json/GoogleService-Info.plist 파일 확인

### 알림이 오지 않는 경우
1. 디바이스 알림 권한 확인
2. FCM 토큰 유효성 확인
3. 백그라운드 제한 설정 확인

### iOS에서 알림이 오지 않는 경우
1. APNs 인증서 설정 확인
2. Provisioning Profile 확인
3. Push Notification capability 활성화 확인

## 7. 보안 주의사항

- Firebase 서비스 계정 키는 절대 Git에 커밋하지 않기
- FCM 토큰은 개인정보이므로 로그에 남기지 않기
- 프로덕션 환경에서는 환경변수로 키 관리
- 정기적으로 무효한 토큰 정리

## 8. 모니터링

### Firebase Console에서 확인
- 전송된 메시지 수
- 전달 성공률
- 오류 통계

### 백엔드 로그 확인
```bash
tail -f logs/notification.log | grep FCM
```

### 주요 메트릭
- 토큰 갱신 빈도
- 알림 전송 성공률
- 평균 전달 시간
- 사용자별 알림 수신률