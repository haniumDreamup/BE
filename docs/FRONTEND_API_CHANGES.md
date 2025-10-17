# 프론트엔드 API 변경사항 안내

**배포일**: 2025-10-17
**변경 내용**: API 응답 구조 통일

---

## 🔧 변경 사항 요약

모든 API가 **동일한 응답 구조**를 사용하도록 통일했습니다.

### Before (혼재)
```dart
// ❌ 일부 API (ScheduleService 등)
final schedules = (response.data['d'] as List)  // 짧은 키

// ✅ 일부 API (GuardianService 등)
if (response.data['success'] == true) {
  final data = response.data['data'];
}
```

### After (통일)
```dart
// ✅ 모든 API
if (response.data['success'] == true) {
  final data = response.data['data'];
  return Model.fromJson(data);
}
```

---

## 📦 통일된 응답 구조

### ✅ 성공 응답
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동"
  },
  "message": "요청이 성공했습니다",
  "timestamp": "2025-10-17T15:30:00"
}
```

**Dart 파싱**:
```dart
if (response.data['success'] == true) {
  final data = response.data['data'];
  return UserModel.fromJson(data);
}
```

---

### ❌ 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다",
    "userAction": "다시 시도해주세요"
  },
  "timestamp": "2025-10-17T15:30:00"
}
```

**Dart 파싱**:
```dart
if (response.data['success'] == false) {
  final errorCode = response.data['error']['code'];
  final errorMsg = response.data['error']['message'];
  final userAction = response.data['error']['userAction'];

  throw ApiException(errorCode, errorMsg, userAction);
}
```

---

## 🔑 표준 에러 코드

| 코드 | 의미 | HTTP 상태 |
|-----|------|-----------|
| `UNAUTHORIZED` | 인증 필요 | 401 |
| `VALIDATION_ERROR` | 입력값 검증 실패 | 400 |
| `NOT_FOUND` | 리소스 없음 | 404 |
| `OPERATION_FAILED` | 작업 실패 | 500 |
| `INTERNAL_ERROR` | 서버 내부 오류 | 500 |

---

## 🛠️ 수정 필요한 파일

### 1. ScheduleService
**파일**: `lib/data/services/schedule_service.dart`

**Before**:
```dart
final schedules = (response.data['d'] as List)
  .map((e) => ScheduleModel.fromJson(e))
  .toList();
```

**After**:
```dart
if (response.data['success'] == true) {
  final schedules = (response.data['data'] as List)
    .map((e) => ScheduleModel.fromJson(e))
    .toList();
  return schedules;
}
```

---

### 2. 모든 서비스 통일

**통일된 패턴**:
```dart
Future<T> apiCall() async {
  try {
    final response = await dio.get('/api/endpoint');

    if (response.data['success'] == true) {
      // ✅ 성공
      return Model.fromJson(response.data['data']);
    } else {
      // ❌ 에러
      throw ApiException(
        response.data['error']['code'],
        response.data['error']['message'],
      );
    }
  } on DioException catch (e) {
    // 네트워크 에러 처리
    throw NetworkException(e.message);
  }
}
```

---

## ✅ 수정 완료 확인

다음 API들이 이미 올바르게 작성되어 있습니다:
- ✅ `guardian_service.dart`
- ✅ `image_analysis_service.dart`
- ✅ `emergency_service.dart`
- ✅ `user_service.dart`

다음 API만 수정하세요:
- ⚠️ `schedule_service.dart` - `['d']` → `['data']` 변경

---

## 📝 에러 처리 예제

### ApiException 클래스
```dart
class ApiException implements Exception {
  final String code;
  final String message;
  final String? userAction;

  ApiException(this.code, this.message, [this.userAction]);

  @override
  String toString() => 'ApiException: $code - $message';
}
```

### 사용 예제
```dart
try {
  final user = await userService.getUser(userId);
} on ApiException catch (e) {
  if (e.code == 'NOT_FOUND') {
    // 404 처리
    showDialog('사용자를 찾을 수 없습니다');
  } else if (e.code == 'UNAUTHORIZED') {
    // 401 처리
    navigateToLogin();
  } else {
    // 기타 에러
    showError(e.message);
  }
}
```

---

## 🔍 Vision API 추가 정보

### 엔드포인트
```
POST /api/v1/images/vision-analyze
```

### 요청
```dart
FormData formData = FormData.fromMap({
  'imageFile': await MultipartFile.fromFile(
    imagePath,
    filename: 'image.jpg',
  ),
});

final response = await dio.post(
  '/api/v1/images/vision-analyze',
  data: formData,
);
```

### 응답
```json
{
  "success": true,
  "data": {
    "objects": [
      {"name": "Person", "confidence": 0.95}
    ],
    "labels": [
      {"description": "Person", "score": 0.98}
    ],
    "text": "인식된 텍스트"
  },
  "message": "이미지 분석이 완료되었습니다"
}
```

---

## 🚀 배포 완료

- ✅ 서버 배포 완료: 2025-10-17 15:45
- ✅ Vision API 활성화
- ✅ 응답 구조 통일

---

## 📞 문의

백엔드 관련 문의사항은 슬랙 `#backend` 채널로 연락주세요!
