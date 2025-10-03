# Schedule API 실제 응답 샘플

**생성일**: 2025-10-02
**용도**: 프론트엔드 개발 시 참고용 실제 응답 예시

---

## ✅ 성공 응답 샘플

### 1. 일정 생성 성공 (201 Created)

**Request:**
```bash
POST /api/v1/schedules
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "title": "약 먹기",
  "description": "혈압약 1알",
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00",
  "priority": 3,
  "requiresConfirmation": true
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "userName": "홍길동",
    "title": "약 먹기",
    "description": "혈압약 1알",
    "scheduleType": "MEDICATION",
    "scheduleTypeDescription": "약물 복용",
    "recurrenceType": "DAILY",
    "recurrenceTypeDescription": "매일",
    "executionTime": "09:00",
    "nextExecutionTime": "2025-10-02T09:00:00",
    "lastExecutionTime": null,
    "startDate": "2025-10-02T00:00:00",
    "endDate": null,
    "selectedDays": null,
    "dayOfMonth": null,
    "intervalValue": 1,
    "isActive": true,
    "priority": 3,
    "visualIndicator": null,
    "reminderMinutesBefore": null,
    "requiresConfirmation": true,
    "createdByType": "USER",
    "createdByTypeDescription": "사용자",
    "createdAt": "2025-10-02T10:30:00",
    "updatedAt": "2025-10-02T10:30:00",
    "isDueSoon": false,
    "isHighPriority": true,
    "simpleDescription": "매일 오전 9시에 약물 복용"
  },
  "message": "일정이 등록되었습니다",
  "timestamp": "2025-10-02T10:30:00"
}
```

---

### 2. 일정 목록 조회 성공 (200 OK)

**Request:**
```bash
GET /api/v1/schedules?page=0&size=10
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "약 먹기",
        "scheduleType": "MEDICATION",
        "nextExecutionTime": "2025-10-02T09:00:00",
        "priority": 3,
        "isActive": true
      },
      {
        "id": 2,
        "title": "산책하기",
        "scheduleType": "EXERCISE",
        "nextExecutionTime": "2025-10-02T07:00:00",
        "priority": 2,
        "isActive": true
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": false,
        "unsorted": true,
        "empty": true
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 15,
    "totalPages": 2,
    "last": false,
    "size": 10,
    "number": 0,
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "numberOfElements": 10,
    "first": true,
    "empty": false
  },
  "message": "일정 목록을 가져왔습니다",
  "timestamp": "2025-10-02T10:31:00"
}
```

---

### 3. 오늘의 일정 조회 성공 (200 OK)

**Request:**
```bash
GET /api/v1/schedules/today
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "약 먹기",
      "scheduleType": "MEDICATION",
      "scheduleTypeDescription": "약물 복용",
      "nextExecutionTime": "2025-10-02T09:00:00",
      "priority": 3,
      "isActive": true,
      "isDueSoon": true,
      "isHighPriority": true
    },
    {
      "id": 5,
      "title": "점심 식사",
      "scheduleType": "MEAL",
      "scheduleTypeDescription": "식사",
      "nextExecutionTime": "2025-10-02T12:00:00",
      "priority": 2,
      "isActive": true,
      "isDueSoon": false,
      "isHighPriority": false
    }
  ],
  "message": "오늘의 일정 2건을 가져왔습니다",
  "timestamp": "2025-10-02T10:32:00"
}
```

---

### 4. 일정 완료 처리 성공 (200 OK)

**Request:**
```bash
POST /api/v1/schedules/1/complete
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "약 먹기",
    "scheduleType": "MEDICATION",
    "recurrenceType": "DAILY",
    "lastExecutionTime": "2025-10-02T09:15:00",
    "nextExecutionTime": "2025-10-03T09:00:00",
    "isActive": true
  },
  "message": "일정이 완료 처리되었습니다",
  "timestamp": "2025-10-02T09:15:00"
}
```

---

### 5. 일정 삭제 성공 (200 OK)

**Request:**
```bash
DELETE /api/v1/schedules/1
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": null,
  "message": "일정이 삭제되었습니다",
  "timestamp": "2025-10-02T10:35:00"
}
```

---

### 6. 반복 일정 목록 조회 성공 (200 OK)

**Request:**
```bash
GET /api/v1/schedules/1/occurrences?count=5
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": [
    "2025-10-02T09:00:00",
    "2025-10-03T09:00:00",
    "2025-10-04T09:00:00",
    "2025-10-05T09:00:00",
    "2025-10-06T09:00:00"
  ],
  "message": "향후 5회 실행 시간을 가져왔습니다",
  "timestamp": "2025-10-02T10:36:00"
}
```

---

## ❌ 실패 응답 샘플

### 1. 인증 실패 - 401 Unauthorized (RFC 7807 형식)

**Request:**
```bash
GET /api/v1/schedules
# Authorization 헤더 없음
```

**Response:**
```json
{
  "type": "https://bifai.app/problems/authentication",
  "title": "로그인이 필요해요",
  "status": 401,
  "detail": "로그인이 필요합니다. 다시 로그인해주세요.",
  "userAction": "다시 로그인해 주세요",
  "timestamp": "2025-10-02T01:23:11.417105098Z"
}
```

**특징:**
- RFC 7807 (Problem Details for HTTP APIs) 표준 형식
- `type`, `title`, `status`, `detail`, `userAction` 필드
- Spring Security가 자동 생성

---

### 2. 권한 없음 - 403 Forbidden

**Request:**
```bash
GET /api/v1/schedules/999
Authorization: Bearer eyJhbGc...
# 다른 사용자의 일정에 접근 시도
```

**Response:**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "timestamp": "2025-10-02T10:40:00",
  "error": {
    "code": "FORBIDDEN",
    "message": "다른 사용자의 일정에 접근할 수 없습니다",
    "userAction": "본인의 일정만 확인해 주세요"
  }
}
```

---

### 3. 일정 없음 - 404 Not Found

**Request:**
```bash
GET /api/v1/schedules/99999
Authorization: Bearer eyJhbGc...
# 존재하지 않는 일정 ID
```

**Response:**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "timestamp": "2025-10-02T10:41:00",
  "error": {
    "code": "NOT_FOUND",
    "message": "일정을 찾을 수 없습니다",
    "userAction": "일정 목록을 새로고침해 주세요"
  }
}
```

---

### 4. 유효성 검증 실패 - 400 Bad Request

**Request:**
```bash
POST /api/v1/schedules
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "title": "",  # 빈 문자열
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00"
}
```

**Response:**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "timestamp": "2025-10-02T10:42:00",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "일정 제목을 입력해 주세요",
    "userAction": "필수 항목을 모두 입력했는지 확인해 주세요"
  }
}
```

---

### 5. 잘못된 데이터 형식 - 400 Bad Request

**Request:**
```bash
POST /api/v1/schedules
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "title": "약 먹기",
  "scheduleType": "INVALID_TYPE",  # 잘못된 enum 값
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00"
}
```

**Response:**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "timestamp": "2025-10-02T10:43:00",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "유효하지 않은 일정 유형입니다",
    "userAction": "올바른 일정 유형을 선택해 주세요"
  }
}
```

---

### 6. 필수 필드 누락 - 400 Bad Request

**Request:**
```bash
POST /api/v1/schedules
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "title": "약 먹기"
  # scheduleType, recurrenceType, executionTime, startDate 누락
}
```

**Response:**
```json
{
  "success": false,
  "data": null,
  "message": null,
  "timestamp": "2025-10-02T10:44:00",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "일정 유형을 선택해 주세요",
    "userAction": "필수 항목을 모두 입력했는지 확인해 주세요"
  }
}
```

---

## 📊 응답 형식 요약

### 성공 응답 구조
```typescript
interface SuccessResponse<T> {
  success: true;
  data: T;              // 실제 데이터 (배열, 객체, null 가능)
  message: string;      // 사용자 친화적 메시지
  timestamp: string;    // LocalDateTime (YYYY-MM-DDTHH:mm:ss)
}
```

### 실패 응답 구조 (일반)
```typescript
interface ErrorResponse {
  success: false;
  data: null;
  message: null;
  timestamp: string;
  error: {
    code: string;       // ERROR_CODE (대문자)
    message: string;    // 에러 설명
    userAction: string; // 사용자가 할 행동
  }
}
```

### 실패 응답 구조 (401 인증)
```typescript
interface AuthErrorResponse {
  type: string;         // Problem type URI
  title: string;        // 간단한 제목
  status: number;       // HTTP 상태 코드
  detail: string;       // 상세 설명
  userAction: string;   // 사용자가 할 행동
  timestamp: string;    // ISO 8601 형식
}
```

---

## 🎯 프론트엔드 개발 팁

### 1. 응답 타입 체크
```typescript
if (response.success) {
  // 성공 - data 사용
  console.log(response.data);
  console.log(response.message);
} else {
  // 실패 - error 사용
  if (response.error) {
    alert(response.error.message);
    console.log(response.error.userAction);
  }
}
```

### 2. 401 인증 에러 처리
```typescript
if (error.response.status === 401) {
  // RFC 7807 형식
  const problemDetail = error.response.data;
  alert(problemDetail.title);  // "로그인이 필요해요"
  // 로그인 페이지로 리다이렉트
  router.push('/login');
}
```

### 3. Pagination 처리
```typescript
const response = await getSchedules({ page: 0, size: 10 });
if (response.success) {
  const { content, totalElements, totalPages } = response.data;
  console.log(`전체 ${totalElements}건 중 페이지 ${totalPages}`);
}
```

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-02
**용도**: 프론트엔드 개발 참고용
