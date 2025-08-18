# BIF-AI Mobile API Specification v1.0

## 개요
BIF-AI 모바일 애플리케이션을 위한 REST API 명세서입니다. 
경계선 지능 장애인(IQ 70-85)을 위한 특화된 모바일 인터페이스를 제공합니다.

### 기본 정보
- **Base URL**: `https://api.bifai.com/api/v1/mobile`
- **인증 방식**: JWT Bearer Token
- **Content-Type**: `application/json`
- **응답 압축**: Gzip 지원
- **API 버전**: v1

### 설계 원칙
1. **간단한 응답 구조**: 불필요한 중첩 최소화
2. **명확한 에러 메시지**: 5학년 수준의 한국어 설명
3. **최소 페이로드**: 모바일 데이터 사용량 최적화
4. **일관된 응답 형식**: 모든 API 동일한 구조

### 공통 응답 형식
```json
{
  "success": true,
  "data": {},
  "message": "작업이 완료되었어요",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 에러 응답 형식
```json
{
  "success": false,
  "error": {
    "code": "AUTH_001",
    "message": "다시 로그인해 주세요",
    "userAction": "앱을 다시 열어주세요"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 인증 API

### 1. 로그인
**POST** `/auth/login`

간단한 로그인 프로세스를 제공합니다.

#### Request
```json
{
  "username": "user@example.com",
  "password": "password123",
  "deviceId": "device-uuid-123",
  "deviceType": "ios" // ios, android
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "name": "홍길동",
      "profileImage": "https://cdn.bifai.com/profiles/user1.jpg",
      "cognitiveLevel": "MODERATE"
    },
    "expiresIn": 3600
  },
  "message": "로그인 성공!",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 토큰 갱신
**POST** `/auth/refresh`

#### Request
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. 로그아웃
**POST** `/auth/logout`

디바이스 토큰을 제거하고 푸시 알림을 비활성화합니다.

---

## 홈 화면 API

### 1. 홈 대시보드
**GET** `/home/dashboard`

오늘의 중요한 정보를 한눈에 보여줍니다.

#### Response
```json
{
  "success": true,
  "data": {
    "greeting": "좋은 아침이에요, 길동님!",
    "today": {
      "date": "2024년 1월 1일 월요일",
      "weather": "맑음, 10°C"
    },
    "summary": {
      "medicationsToTake": 3,
      "schedulesToday": 5,
      "nextEvent": {
        "title": "아침 약 먹기",
        "time": "08:00",
        "icon": "pill"
      }
    },
    "urgentAlerts": []
  },
  "message": "오늘 하루도 화이팅!",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 약물 관리 API

### 1. 오늘의 약물 목록
**GET** `/medications/today`

오늘 복용해야 할 약물을 시간순으로 표시합니다.

#### Response
```json
{
  "success": true,
  "data": {
    "medications": [
      {
        "id": 1,
        "name": "혈압약",
        "simpleDescription": "심장을 건강하게 해주는 약",
        "time": "08:00",
        "taken": false,
        "dosage": "1알",
        "color": "#FF6B6B",
        "icon": "heart",
        "image": "https://cdn.bifai.com/meds/med1.jpg",
        "important": true
      }
    ],
    "taken": 0,
    "remaining": 3,
    "nextMedication": {
      "name": "혈압약",
      "timeUntil": "30분 후"
    }
  },
  "message": "오늘 약 3개를 먹어야 해요",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 약물 복용 기록
**POST** `/medications/{id}/take`

#### Request
```json
{
  "takenAt": "2024-01-01T08:00:00Z",
  "note": "아침 식사 후 복용"
}
```

### 3. 약물 상세 정보
**GET** `/medications/{id}`

간단한 언어로 약물 정보를 제공합니다.

---

## 일정 관리 API

### 1. 오늘의 일정
**GET** `/schedules/today`

#### Query Parameters
- `simplified`: boolean (true = 간단 보기)

#### Response
```json
{
  "success": true,
  "data": {
    "schedules": [
      {
        "id": 1,
        "title": "병원 가기",
        "time": "14:00",
        "location": "서울 병원",
        "type": "APPOINTMENT",
        "color": "#4ECDC4",
        "icon": "hospital",
        "reminder": "30분 전",
        "completed": false
      }
    ],
    "totalCount": 5
  },
  "message": "오늘 일정이 5개 있어요",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 일정 완료 표시
**PUT** `/schedules/{id}/complete`

---

## 알림 API

### 1. 푸시 토큰 등록
**POST** `/notifications/register`

#### Request
```json
{
  "token": "fcm-token-string",
  "deviceType": "ios",
  "deviceId": "device-uuid"
}
```

### 2. 알림 설정
**GET** `/notifications/settings`
**PUT** `/notifications/settings`

#### Request (PUT)
```json
{
  "medication": true,
  "schedule": true,
  "emergency": true,
  "quietHours": {
    "enabled": true,
    "start": "22:00",
    "end": "08:00"
  }
}
```

### 3. 알림 히스토리
**GET** `/notifications/history`

#### Query Parameters
- `limit`: 개수 (기본: 20)
- `offset`: 시작 위치

---

## 미디어 업로드 API

### 1. 이미지 업로드 URL 생성
**POST** `/media/upload/prepare`

S3 직접 업로드를 위한 Presigned URL을 생성합니다.

#### Request
```json
{
  "fileName": "photo.jpg",
  "fileType": "image/jpeg",
  "fileSize": 2048000,
  "purpose": "profile" // profile, medication, activity
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://s3.amazonaws.com/bifai-uploads/...",
    "fileKey": "uploads/2024/01/uuid-photo.jpg",
    "expiresIn": 300
  },
  "message": "5분 안에 사진을 올려주세요",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 업로드 완료 확인
**POST** `/media/upload/complete`

#### Request
```json
{
  "fileKey": "uploads/2024/01/uuid-photo.jpg",
  "metadata": {
    "width": 1920,
    "height": 1080,
    "location": "서울"
  }
}
```

---

## 보호자 연동 API

### 1. 내 보호자 목록
**GET** `/guardians`

#### Response
```json
{
  "success": true,
  "data": {
    "guardians": [
      {
        "id": 1,
        "name": "김보호",
        "relationship": "가족",
        "phone": "010-****-5678",
        "profileImage": "https://cdn.bifai.com/profiles/guardian1.jpg",
        "isPrimary": true,
        "canViewLocation": true
      }
    ]
  },
  "message": "보호자 2명이 있어요",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 긴급 연락
**POST** `/guardians/emergency`

#### Request
```json
{
  "type": "HELP", // HELP, LOST, MEDICAL
  "location": {
    "latitude": 37.5665,
    "longitude": 126.9780
  },
  "message": "도움이 필요해요"
}
```

---

## 위치 공유 API

### 1. 현재 위치 업데이트
**POST** `/location/update`

#### Request
```json
{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "accuracy": 10,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 안전 구역 확인
**GET** `/location/safe-zones`

---

## 활동 기록 API

### 1. 최근 활동
**GET** `/activities/recent`

#### Response
```json
{
  "success": true,
  "data": {
    "activities": [
      {
        "id": 1,
        "type": "MEDICATION_TAKEN",
        "title": "아침 약 복용 완료",
        "timestamp": "2024-01-01T08:00:00Z",
        "icon": "check-circle",
        "color": "#51CF66"
      }
    ]
  },
  "message": "잘 하고 있어요!",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## 에러 코드

### 인증 관련 (AUTH_xxx)
- `AUTH_001`: 로그인 필요
- `AUTH_002`: 토큰 만료
- `AUTH_003`: 권한 없음

### 데이터 관련 (DATA_xxx)
- `DATA_001`: 찾을 수 없음
- `DATA_002`: 잘못된 입력
- `DATA_003`: 중복된 데이터

### 서버 관련 (SERVER_xxx)
- `SERVER_001`: 서버 오류
- `SERVER_002`: 서비스 점검 중

---

## 페이징

모든 목록 API는 다음 파라미터를 지원합니다:

- `page`: 페이지 번호 (1부터 시작)
- `size`: 페이지 크기 (기본: 20, 최대: 50)

### Response
```json
{
  "data": {
    "content": [],
    "page": {
      "current": 1,
      "total": 5,
      "size": 20,
      "totalElements": 100
    }
  }
}
```

---

## Rate Limiting

- 일반 API: 분당 60회
- 인증 API: 분당 10회
- 업로드 API: 분당 5회

초과 시 429 상태 코드와 함께 다음 응답:
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT",
    "message": "잠시 후 다시 시도해주세요",
    "userAction": "1분 후에 다시 해주세요"
  }
}
```

---

## 버전 관리

API 버전은 URL 경로에 포함됩니다:
- v1: `/api/v1/mobile/...` (현재)
- v2: `/api/v2/mobile/...` (예정)

구버전 지원 정책:
- 새 버전 출시 후 6개월간 구버전 지원
- 구버전 사용 시 응답 헤더에 경고 포함

---

## 보안 고려사항

1. **HTTPS 필수**: 모든 통신은 HTTPS로만
2. **토큰 만료**: Access Token 1시간, Refresh Token 7일
3. **디바이스 바인딩**: 토큰은 디바이스 ID와 연결
4. **민감정보 마스킹**: 전화번호, 주소 등 부분 마스킹
5. **요청 서명**: 중요 작업은 추가 서명 검증