# Schedule API 명세서

## 🚀 배포 정보
- **Base URL**: `http://43.200.49.171:8080/api/v1/schedules` (HTTP, HTTPS 아님)
- **인증**: Bearer Token (JWT) 필수
- **버전**: v1.0
- **배포일**: 2025-10-02
- **서버 상태**: ✅ 정상 작동 중 (http://43.200.49.171:8080/api/health)

---

## 📋 목차
0. [인증 (Authentication)](#0-인증-authentication)
1. [CRUD 기본 API](#1-crud-기본-api)
2. [조회 필터 API](#2-조회-필터-api)
3. [상태 관리 API](#3-상태-관리-api)
4. [반복 일정 API](#4-반복-일정-api)
5. [데이터 모델](#5-데이터-모델)

---

## 0. 인증 (Authentication)

모든 Schedule API는 JWT 토큰 인증이 필요합니다.

### 0.1 로그인하여 토큰 발급받기

```http
POST /api/v1/auth/login
Content-Type: application/json
```

**Request Body:**
```json
{
  "usernameOrEmail": "user@example.com",
  "password": "your-password"
}
```

**필드 설명:**
- `usernameOrEmail`: 사용자명 또는 이메일 (필수)
- `password`: 비밀번호 (필수, 4~128자)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "message": "로그인 성공",
  "timestamp": "2025-10-02T10:30:00",
  
}
```

### 0.2 인증된 요청 방법

발급받은 토큰을 모든 Schedule API 요청의 `Authorization` 헤더에 포함:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**예시:**
```bash
curl -X GET "http://43.200.49.171:8080/api/v1/schedules/today" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json"
```

---

## 1. CRUD 기본 API

### 1.1 일정 생성
```http
POST /api/v1/schedules
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "약 먹기",
  "description": "혈압약 1알",
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00",
  "endDate": null,
  "selectedDays": null,
  "dayOfMonth": null,
  "intervalValue": 1,
  "priority": 3,
  "visualIndicator": "pill-icon",
  "reminderMinutesBefore": 10,
  "requiresConfirmation": true,
  "isActive": true
}
```

**Response (201 Created):**
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
    "visualIndicator": "pill-icon",
    "reminderMinutesBefore": 10,
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
  "timestamp": "2025-10-02T10:30:00",
  
}
```

**참고사항:**
- `timestamp`는 ISO 8601 형식의 배열로 반환됩니다: `[year, month, day, hour, minute, second, nano]`
- 성공 응답에서 `error` 필드는 항상 `null`입니다.

---

### 1.2 일정 상세 조회
```http
GET /api/v1/schedules/{scheduleId}
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "약 먹기",
    ...
  },
  "message": "일정 정보를 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 1.3 일정 목록 조회 (페이징)
```http
GET /api/v1/schedules?page=0&size=20
Authorization: Bearer {token}
```

**Query Parameters:**
- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 페이지 크기 (기본값: 20)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "약 먹기",
        ...
      },
      {
        "id": 2,
        "title": "산책하기",
        ...
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 45,
    "totalPages": 3,
    "last": false
  },
  "message": "일정 목록을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 1.4 일정 수정
```http
PUT /api/v1/schedules/{scheduleId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:** (1.1과 동일)

**Response (200 OK):**
```json
{
  "success": true,
  "data": { ... },
  "message": "일정이 수정되었습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 1.5 일정 삭제
```http
DELETE /api/v1/schedules/{scheduleId}
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "일정이 삭제되었습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

## 2. 조회 필터 API

### 2.1 오늘의 일정 조회
```http
GET /api/v1/schedules/today
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "약 먹기",
      "nextExecutionTime": "2025-10-02T09:00:00",
      ...
    },
    {
      "id": 5,
      "title": "점심 식사",
      "nextExecutionTime": "2025-10-02T12:00:00",
      ...
    }
  ],
  "message": "오늘의 일정 2건을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 2.2 다가오는 일정 조회
```http
GET /api/v1/schedules/upcoming?days=7
Authorization: Bearer {token}
```

**Query Parameters:**
- `days`: 조회할 일수 (기본값: 7)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    { "id": 1, "title": "약 먹기", ... },
    { "id": 2, "title": "병원 방문", ... }
  ],
  "message": "7일 이내 일정 15건을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 2.3 특정 날짜 일정 조회
```http
GET /api/v1/schedules/date?date=2025-10-05
Authorization: Bearer {token}
```

**Query Parameters:**
- `date`: 조회할 날짜 (YYYY-MM-DD 형식)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [ ... ],
  "message": "2025-10-05 일정 3건을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 2.4 기간별 일정 조회
```http
GET /api/v1/schedules/range?startDate=2025-10-01&endDate=2025-10-31
Authorization: Bearer {token}
```

**Query Parameters:**
- `startDate`: 시작 날짜 (YYYY-MM-DD)
- `endDate`: 종료 날짜 (YYYY-MM-DD)

**Response (200 OK):**
```json
{
  "success": true,
  "data": [ ... ],
  "message": "기간 내 일정 42건을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

## 3. 상태 관리 API

### 3.1 일정 완료 처리
```http
POST /api/v1/schedules/{scheduleId}/complete
Authorization: Bearer {token}
```

**설명:** 일정을 완료로 표시하고, 반복 일정인 경우 다음 실행 시간을 자동 계산합니다.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "lastExecutionTime": "2025-10-02T09:15:00",
    "nextExecutionTime": "2025-10-03T09:00:00",
    ...
  },
  "message": "일정이 완료 처리되었습니다",
  "timestamp": "2025-10-02T09:15:00Z"
}
```

---

### 3.2 일정 완료 취소
```http
POST /api/v1/schedules/{scheduleId}/uncomplete
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": { ... },
  "message": "일정 완료가 취소되었습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 3.3 일정 활성화
```http
PUT /api/v1/schedules/{scheduleId}/activate
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": { "isActive": true, ... },
  "message": "일정이 활성화되었습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 3.4 일정 비활성화
```http
PUT /api/v1/schedules/{scheduleId}/deactivate
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": { "isActive": false, ... },
  "message": "일정이 비활성화되었습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

## 4. 반복 일정 API

### 4.1 다음 실행 건너뛰기
```http
POST /api/v1/schedules/{scheduleId}/skip-next
Authorization: Bearer {token}
```

**설명:** 반복 일정의 다음 실행을 건너뛰고 그 다음 실행 시간을 계산합니다.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nextExecutionTime": "2025-10-04T09:00:00",
    ...
  },
  "message": "다음 실행이 건너뛰어졌습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 4.2 반복 일정 목록 조회
```http
GET /api/v1/schedules/{scheduleId}/occurrences?count=10
Authorization: Bearer {token}
```

**Query Parameters:**
- `count`: 조회할 실행 횟수 (기본값: 10)

**설명:** 반복 일정의 향후 실행 시간 목록을 조회합니다.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    "2025-10-02T09:00:00",
    "2025-10-03T09:00:00",
    "2025-10-04T09:00:00",
    "2025-10-05T09:00:00",
    "2025-10-06T09:00:00",
    "2025-10-07T09:00:00",
    "2025-10-08T09:00:00",
    "2025-10-09T09:00:00",
    "2025-10-10T09:00:00",
    "2025-10-11T09:00:00"
  ],
  "message": "향후 10회 실행 시간을 가져왔습니다",
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

## 5. 데이터 모델

### 5.1 ScheduleType (일정 유형)

| 값 | 설명 |
|---|---|
| `MEDICATION` | 약물 복용 |
| `MEAL` | 식사 |
| `EXERCISE` | 운동 |
| `APPOINTMENT` | 약속/병원 |
| `REMINDER` | 알림 |
| `THERAPY` | 치료 |
| `HYGIENE` | 위생 관리 |
| `SAFETY_CHECK` | 안전 확인 |
| `SOCIAL_ACTIVITY` | 사회 활동 |
| `PERSONAL_CARE` | 개인 관리 |

---

### 5.2 RecurrenceType (반복 패턴)

| 값 | 설명 | 사용 예시 |
|---|---|---|
| `ONCE` | 한 번만 | 병원 예약 |
| `DAILY` | 매일 | 약 복용 |
| `WEEKLY` | 매주 | 주간 운동 |
| `MONTHLY` | 매월 | 월간 검진 |
| `CUSTOM_DAYS` | 요일 선택 | 월/수/금 운동 |
| `INTERVAL_DAYS` | N일마다 | 3일마다 샤워 |
| `INTERVAL_WEEKS` | N주마다 | 2주마다 병원 |

---

### 5.3 요일 선택 (selectedDays)

**WEEKLY 또는 CUSTOM_DAYS 사용 시:**
```json
"selectedDays": ["MONDAY", "WEDNESDAY", "FRIDAY"]
```

**가능한 값:**
- `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

---

### 5.4 우선순위 (priority)

| 값 | 설명 | UI 표시 |
|---|---|---|
| 1 | 낮음 | 회색 |
| 2 | 보통 | 파란색 (기본값) |
| 3 | 높음 | 주황색 |
| 4 | 매우 높음 | 빨간색 |

---

### 5.5 필수/선택 필드

**필수 필드:**
- `title` (제목)
- `scheduleType` (일정 유형)
- `recurrenceType` (반복 패턴)
- `executionTime` (실행 시간)
- `startDate` (시작 날짜)

**선택 필드:**
- `description` (설명)
- `endDate` (종료 날짜) - null이면 무기한
- `selectedDays` (요일 선택) - WEEKLY/CUSTOM_DAYS에만 필요
- `dayOfMonth` (월 날짜) - MONTHLY에만 필요
- `intervalValue` (반복 간격) - 기본값: 1
- `priority` (우선순위) - 기본값: 2
- `visualIndicator` (시각적 표시)
- `reminderMinutesBefore` (미리 알림)
- `requiresConfirmation` (완료 확인 필요)
- `isActive` (활성화 상태) - 기본값: true

---

## 6. 에러 응답

### 6.1 인증 실패 (401)
```json
{
  "type": "https://bifai.app/problems/authentication",
  "title": "로그인이 필요해요",
  "status": 401,
  "detail": "로그인이 필요합니다. 다시 로그인해주세요.",
  "userAction": "다시 로그인해 주세요",
  "timestamp": "2025-10-02T01:19:46.866444298Z"
}
```

**참고:** 인증 에러는 RFC 7807 (Problem Details) 형식을 사용합니다.

---

### 6.2 권한 없음 (403)
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "다른 사용자의 일정에 접근할 수 없습니다",
    "userAction": "본인의 일정만 확인해 주세요"
  },
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 6.3 일정 없음 (404)
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "일정을 찾을 수 없습니다",
    "userAction": "일정 목록을 새로고침해 주세요"
  },
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

### 6.4 유효성 검증 실패 (400)
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "일정 제목을 입력해 주세요",
    "userAction": "필수 항목을 모두 입력했는지 확인해 주세요"
  },
  "timestamp": "2025-10-02T10:30:00Z"
}
```

---

## 7. 사용 예시

### 7.1 매일 아침 약 복용 일정
```json
{
  "title": "혈압약 먹기",
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00",
  "reminderMinutesBefore": 10,
  "requiresConfirmation": true,
  "priority": 4
}
```

---

### 7.2 월/수/금 운동 일정
```json
{
  "title": "산책하기",
  "scheduleType": "EXERCISE",
  "recurrenceType": "CUSTOM_DAYS",
  "executionTime": "07:00",
  "startDate": "2025-10-02T00:00:00",
  "selectedDays": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "reminderMinutesBefore": 30,
  "priority": 2
}
```

---

### 7.3 매월 15일 병원 예약
```json
{
  "title": "정기 검진",
  "scheduleType": "APPOINTMENT",
  "recurrenceType": "MONTHLY",
  "executionTime": "14:00",
  "startDate": "2025-10-15T00:00:00",
  "dayOfMonth": 15,
  "reminderMinutesBefore": 60,
  "requiresConfirmation": true,
  "priority": 3
}
```

---

### 7.4 3일마다 샤워
```json
{
  "title": "샤워하기",
  "scheduleType": "HYGIENE",
  "recurrenceType": "INTERVAL_DAYS",
  "executionTime": "20:00",
  "startDate": "2025-10-02T00:00:00",
  "intervalValue": 3,
  "reminderMinutesBefore": 15,
  "priority": 2
}
```

---

## 8. 주의사항

1. **인증 토큰**: 모든 API 호출 시 `Authorization: Bearer {token}` 헤더 필수
2. **시간 형식**:
   - `executionTime`: `HH:mm` (예: "09:00")
   - `startDate`, `endDate`: ISO 8601 (예: "2025-10-02T00:00:00")
3. **페이지네이션**: 기본 페이지 크기는 20, 최대 100까지 가능
4. **반복 패턴별 필수 필드**:
   - `WEEKLY`, `CUSTOM_DAYS` → `selectedDays` 필수
   - `MONTHLY` → `dayOfMonth` 필수
   - `INTERVAL_DAYS`, `INTERVAL_WEEKS` → `intervalValue` 필수
5. **다음 실행 시간**: 일정 생성/수정 시 자동 계산됨

---

## 9. Swagger 문서
배포 후 Swagger UI에서 대화형으로 API 테스트 가능:
```
http://43.200.49.171:8080/swagger-ui/index.html
```

**OpenAPI 3.0 스펙:**
```
http://43.200.49.171:8080/v3/api-docs
```

---

## 10. 빠른 시작 가이드 (Quick Start)

### Step 1: 로그인하여 토큰 받기
```bash
curl -X POST "http://43.200.49.171:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"your@email.com","password":"your-password"}'
```

### Step 2: 오늘의 일정 조회
```bash
curl -X GET "http://43.200.49.171:8080/api/v1/schedules/today" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Step 3: 새 일정 생성
```bash
curl -X POST "http://43.200.49.171:8080/api/v1/schedules" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "약 먹기",
    "scheduleType": "MEDICATION",
    "recurrenceType": "DAILY",
    "executionTime": "09:00",
    "startDate": "2025-10-02T00:00:00",
    "priority": 3,
    "requiresConfirmation": true
  }'
```

### Step 4: 일정 완료 처리
```bash
curl -X POST "http://43.200.49.171:8080/api/v1/schedules/1/complete" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 11. API 엔드포인트 요약

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api/v1/schedules` | 일정 생성 |
| GET | `/api/v1/schedules/{id}` | 일정 상세 조회 |
| GET | `/api/v1/schedules` | 일정 목록 조회 (페이징) |
| PUT | `/api/v1/schedules/{id}` | 일정 수정 |
| DELETE | `/api/v1/schedules/{id}` | 일정 삭제 |
| GET | `/api/v1/schedules/today` | 오늘의 일정 |
| GET | `/api/v1/schedules/upcoming` | 다가오는 일정 |
| GET | `/api/v1/schedules/date` | 특정 날짜 일정 |
| GET | `/api/v1/schedules/range` | 기간별 일정 |
| POST | `/api/v1/schedules/{id}/complete` | 일정 완료 |
| POST | `/api/v1/schedules/{id}/uncomplete` | 완료 취소 |
| PUT | `/api/v1/schedules/{id}/activate` | 일정 활성화 |
| PUT | `/api/v1/schedules/{id}/deactivate` | 일정 비활성화 |
| POST | `/api/v1/schedules/{id}/skip-next` | 다음 실행 건너뛰기 |
| GET | `/api/v1/schedules/{id}/occurrences` | 반복 일정 목록 |

---

**문서 버전**: 1.0.1
**최종 수정일**: 2025-10-02
**담당**: BIF-AI 개발팀
**검증 완료**: ✅ 실제 배포 서버에서 테스트 완료
