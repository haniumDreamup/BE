# 통일된 API 응답 구조

## 📋 개요

**변경일**: 2025-10-16
**목적**: 프론트엔드-백엔드 간 일관된 응답 구조 제공

---

## 🎯 변경 사항

### 삭제된 파일
- ❌ `ResponseOptimizationAdvice.java` (자동 래핑 제거)
- ❌ `OptimizedApiResponse.java` (짧은 키 이름 제거)

### 이유
1. **BIF 사용자 요구사항**: 명확하고 이해하기 쉬운 응답 필요
2. **일관성**: `GlobalExceptionHandler`의 `ProblemDetail`과 통일
3. **유지보수성**: 단일 응답 구조로 프론트엔드 파싱 로직 단순화
4. **과도한 최적화**: 짧은 키 이름의 성능 이득 < 가독성 손실

---

## ✅ 통일된 응답 구조

### 1. 성공 응답

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동"
  },
  "message": "사용자 정보를 가져왔습니다",
  "timestamp": "2025-10-16T10:30:00"
}
```

**Java 코드**:
```java
@GetMapping("/users/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
  UserResponse user = userService.getUser(id);
  return ResponseEntity.ok(
    ApiResponse.success(user, "사용자 정보를 가져왔습니다")
  );
}
```

**프론트엔드 (Dart)**:
```dart
final response = await dio.get('/api/users/1');
if (response.data['success'] == true) {
  final user = UserModel.fromJson(response.data['data']);
  return user;
}
```

---

### 2. 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다"
  },
  "timestamp": "2025-10-16T10:30:00"
}
```

**Java 코드**:
```java
// BaseController 헬퍼 메서드 사용
return createNotFoundResponse("사용자를 찾을 수 없습니다");

// 직접 호출
return ResponseEntity.status(HttpStatus.NOT_FOUND)
  .body(ApiResponse.error("NOT_FOUND", "사용자를 찾을 수 없습니다"));
```

**프론트엔드 (Dart)**:
```dart
if (response.data['success'] == false) {
  final errorCode = response.data['error']['code'];
  final errorMsg = response.data['error']['message'];
  throw ApiException(errorCode, errorMsg);
}
```

---

### 3. 검증 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "이메일 형식이 올바르지 않습니다, 비밀번호는 8자 이상이어야 합니다"
  },
  "timestamp": "2025-10-16T10:30:00"
}
```

**Java 코드**:
```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<?>> register(
    @Valid @RequestBody RegisterRequest request,
    BindingResult bindingResult) {

  // BaseController 헬퍼 메서드 사용
  ResponseEntity<ApiResponse<?>> errorResponse =
    createValidationErrorResponse(bindingResult);
  if (errorResponse != null) {
    return errorResponse;
  }

  // 정상 처리...
}
```

---

## 📦 에러 코드 표준

### 인증/권한 관련
- `UNAUTHORIZED` - 인증 필요 (401)
- `FORBIDDEN` - 권한 없음 (403)

### 리소스 관련
- `NOT_FOUND` - 리소스 없음 (404)
- `CONFLICT` - 중복 리소스 (409)

### 검증 관련
- `VALIDATION_ERROR` - 입력값 검증 실패 (400)
- `BAD_REQUEST` - 잘못된 요청 (400)

### 서버 관련
- `INTERNAL_ERROR` - 서버 내부 오류 (500)
- `OPERATION_FAILED` - 작업 실패 (500)

---

## 🔄 마이그레이션 가이드

### 백엔드

**이전**:
```java
// ❌ 응답이 OptimizedApiResponse로 자동 래핑됨
@GetMapping("/schedules")
public List<Schedule> getSchedules() {
  return scheduleService.getAll();
}
```

**현재**:
```java
// ✅ ApiResponse로 명시적 반환
@GetMapping("/schedules")
public ResponseEntity<ApiResponse<List<Schedule>>> getSchedules() {
  List<Schedule> schedules = scheduleService.getAll();
  return ResponseEntity.ok(
    ApiResponse.success(schedules, "일정 목록을 가져왔습니다")
  );
}
```

### 프론트엔드

**이전 (혼재)**:
```dart
// ScheduleService - OptimizedApiResponse 대응
final schedules = (response.data['d'] as List)
  .map((e) => Schedule.fromJson(e))
  .toList();

// GuardianService - ApiResponse 대응
if (response.data['success'] == true) {
  final guardians = (response.data['data'] as List)
    .map((e) => Guardian.fromJson(e))
    .toList();
}
```

**현재 (통일)**:
```dart
// 모든 서비스에서 동일
if (response.data['success'] == true) {
  final items = (response.data['data'] as List)
    .map((e) => Model.fromJson(e))
    .toList();
  return items;
}
```

---

## 🧪 테스트 가이드

### 컨트롤러 테스트

```java
@Test
void getUserSuccess() {
  mockMvc.perform(get("/api/users/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.id").value(1))
    .andExpect(jsonPath("$.message").exists());
}

@Test
void getUserNotFound() {
  mockMvc.perform(get("/api/users/999"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.success").value(false))
    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
    .andExpect(jsonPath("$.error.message").exists());
}
```

---

## 📊 BaseController 헬퍼 메서드

```java
// 성공 응답
protected <T> ResponseEntity<ApiResponse<T>> createSuccessResponse(T data, String message)

// 생성 성공
protected <T> ResponseEntity<ApiResponse<T>> createCreatedResponse(T data, String message)

// 에러 응답
protected <T> ResponseEntity<ApiResponse<T>> createErrorResponse(HttpStatus status, String code, String message)
protected <T> ResponseEntity<ApiResponse<T>> createNotFoundResponse(String message)
protected <T> ResponseEntity<ApiResponse<T>> createBadRequestResponse(String message)
protected <T> ResponseEntity<ApiResponse<T>> createInternalErrorResponse(String message)

// 검증 에러
protected ResponseEntity<ApiResponse<?>> createValidationErrorResponse(BindingResult bindingResult)
```

---

## ⚠️ 주의사항

### 1. 에러 응답 시 항상 code 포함
```java
// ❌ 나쁜 예
return ResponseEntity.badRequest()
  .body(ApiResponse.error("입력값이 잘못되었습니다"));

// ✅ 좋은 예
return ResponseEntity.badRequest()
  .body(ApiResponse.error("VALIDATION_ERROR", "입력값이 잘못되었습니다"));
```

### 2. BaseController 헬퍼 메서드 활용
```java
// ❌ 수동으로 작성
return ResponseEntity.status(HttpStatus.NOT_FOUND)
  .body(ApiResponse.error("NOT_FOUND", "사용자를 찾을 수 없습니다"));

// ✅ 헬퍼 메서드 사용
return createNotFoundResponse("사용자를 찾을 수 없습니다");
```

### 3. 프론트엔드 파싱 순서
```dart
// 1. success 필드 체크
if (response.data['success'] == true) {
  // 2. data 필드에서 실제 데이터 추출
  return Model.fromJson(response.data['data']);
} else {
  // 3. error 필드에서 에러 정보 추출
  final errorCode = response.data['error']['code'];
  final errorMsg = response.data['error']['message'];
  throw ApiException(errorCode, errorMsg);
}
```

---

## 📚 관련 파일

### 백엔드
- [ApiResponse.java](../src/main/java/com/bifai/reminder/bifai_backend/dto/response/ApiResponse.java)
- [BaseController.java](../src/main/java/com/bifai/reminder/bifai_backend/common/BaseController.java)
- [GlobalExceptionHandler.java](../src/main/java/com/bifai/reminder/bifai_backend/exception/GlobalExceptionHandler.java)

### 프론트엔드
- `lib/services/*_service.dart` - 모든 API 서비스
- `lib/core/errors/api_exception.dart` - 에러 처리

---

## 🔗 참고

- [CLAUDE.md](../CLAUDE.md) - 프로젝트 규칙
- [API 규약](../CLAUDE.md#api-conventions) - 응답 포맷 명세
