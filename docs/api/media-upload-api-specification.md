# 미디어 업로드 API 명세서

## 개요
BIF 사용자를 위한 이미지/비디오 업로드 API입니다. S3 Presigned URL을 사용하여 안전하고 효율적인 파일 업로드를 지원합니다.

## 설계 원칙
- 간단한 업로드 프로세스 (2단계)
- 명확한 에러 메시지
- 썸네일 자동 생성
- 대용량 파일 지원 (멀티파트 업로드)

## API 엔드포인트

### 1. Presigned URL 생성
**POST** `/api/v1/mobile/media/presigned-url`

클라이언트가 파일을 업로드하기 위한 임시 URL을 받습니다.

#### Request
```json
{
  "fileName": "photo.jpg",
  "fileType": "image/jpeg", 
  "fileSize": 2048576,
  "uploadType": "PROFILE", // PROFILE, MEDICATION, ACTIVITY, DOCUMENT
  "metadata": {
    "description": "약 사진"
  }
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://s3.amazonaws.com/bifai-media/...",
    "uploadId": "upload_123456",
    "mediaId": "media_789012",
    "expiresAt": "2024-01-01T10:00:00Z",
    "maxSize": 10485760,
    "uploadMethod": "PUT", // PUT or POST
    "headers": {
      "Content-Type": "image/jpeg"
    }
  },
  "message": "업로드 URL이 준비됐어요"
}
```

### 2. 업로드 완료 확인
**POST** `/api/v1/mobile/media/{mediaId}/complete`

S3 업로드 완료 후 서버에 알립니다.

#### Request
```json
{
  "uploadId": "upload_123456",
  "etag": "\"9bb58f26192e4ba00f01e2e7b136bbd8\""
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "mediaId": "media_789012",
    "url": "https://cdn.bifai.com/media/789012.jpg",
    "thumbnailUrl": "https://cdn.bifai.com/media/789012_thumb.jpg",
    "fileSize": 2048576,
    "mimeType": "image/jpeg",
    "width": 1920,
    "height": 1080,
    "duration": null, // 비디오인 경우 초 단위
    "processedAt": "2024-01-01T09:15:00Z"
  },
  "message": "업로드가 완료됐어요"
}
```

### 3. 멀티파트 업로드 시작
**POST** `/api/v1/mobile/media/multipart/init`

5MB 이상 파일용 멀티파트 업로드 시작

#### Request
```json
{
  "fileName": "video.mp4",
  "fileType": "video/mp4",
  "fileSize": 52428800, // 50MB
  "uploadType": "ACTIVITY"
}
```

#### Response
```json
{
  "success": true,
  "data": {
    "uploadId": "multipart_123",
    "mediaId": "media_456",
    "partSize": 5242880, // 5MB
    "totalParts": 10,
    "parts": [
      {
        "partNumber": 1,
        "uploadUrl": "https://s3.amazonaws.com/...",
        "startByte": 0,
        "endByte": 5242879
      }
      // ... 나머지 파트들
    ]
  },
  "message": "큰 파일 업로드를 시작해요"
}
```

### 4. 멀티파트 업로드 완료
**POST** `/api/v1/mobile/media/multipart/{uploadId}/complete`

#### Request
```json
{
  "parts": [
    {
      "partNumber": 1,
      "etag": "\"abc123\""
    },
    {
      "partNumber": 2,
      "etag": "\"def456\""
    }
  ]
}
```

### 5. 미디어 목록 조회
**GET** `/api/v1/mobile/media`

#### Query Parameters
- `type`: PROFILE, MEDICATION, ACTIVITY, DOCUMENT
- `page`: 페이지 번호 (기본: 0)
- `size`: 페이지 크기 (기본: 20)
- `startDate`: 시작일 (YYYY-MM-DD)
- `endDate`: 종료일 (YYYY-MM-DD)

#### Response
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "mediaId": "media_123",
        "type": "MEDICATION",
        "url": "https://cdn.bifai.com/media/123.jpg",
        "thumbnailUrl": "https://cdn.bifai.com/media/123_thumb.jpg",
        "fileName": "약물사진.jpg",
        "fileSize": 1024000,
        "mimeType": "image/jpeg",
        "uploadedAt": "2024-01-01T09:00:00Z",
        "metadata": {
          "description": "아침 약"
        }
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "currentPage": 0,
    "hasNext": true
  },
  "message": "사진을 불러왔어요"
}
```

### 6. 미디어 삭제
**DELETE** `/api/v1/mobile/media/{mediaId}`

#### Response
```json
{
  "success": true,
  "message": "삭제했어요"
}
```

## 에러 응답

### 파일 크기 초과
```json
{
  "success": false,
  "error": {
    "code": "FILE_TOO_LARGE",
    "message": "파일이 너무 커요",
    "userAction": "10MB보다 작은 파일을 선택해주세요"
  }
}
```

### 지원하지 않는 파일 형식
```json
{
  "success": false,
  "error": {
    "code": "UNSUPPORTED_FILE_TYPE",
    "message": "이 파일은 사용할 수 없어요",
    "userAction": "JPG, PNG, MP4 파일만 가능해요"
  }
}
```

### 업로드 만료
```json
{
  "success": false,
  "error": {
    "code": "UPLOAD_EXPIRED",
    "message": "업로드 시간이 지났어요",
    "userAction": "다시 시도해주세요"
  }
}
```

## 파일 제한사항

### 이미지
- 지원 형식: JPEG, PNG, GIF, WEBP
- 최대 크기: 10MB
- 최대 해상도: 4096x4096
- 자동 썸네일: 200x200, 400x400

### 비디오
- 지원 형식: MP4, MOV, AVI
- 최대 크기: 100MB
- 최대 길이: 5분
- 자동 썸네일: 첫 프레임 추출

## 보안 고려사항
1. Presigned URL은 15분 후 만료
2. 파일 내용 검증 (악성코드 스캔)
3. 사용자별 업로드 제한 (시간당 100개)
4. IP 기반 제한 (분당 20개)

## 클라이언트 구현 예제

### 1단계: Presigned URL 요청
```javascript
const response = await fetch('/api/v1/mobile/media/presigned-url', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    fileName: file.name,
    fileType: file.type,
    fileSize: file.size,
    uploadType: 'MEDICATION'
  })
});

const { data } = await response.json();
```

### 2단계: S3 직접 업로드
```javascript
await fetch(data.uploadUrl, {
  method: data.uploadMethod,
  headers: data.headers,
  body: file
});
```

### 3단계: 업로드 완료 확인
```javascript
await fetch(`/api/v1/mobile/media/${data.mediaId}/complete`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    uploadId: data.uploadId
  })
});
```

## 처리 흐름도
```
사용자 → 앱 → [Presigned URL 요청] → 백엔드
                    ↓
백엔드 → [URL 생성] → S3
                    ↓
백엔드 → [URL 응답] → 앱
                    ↓
앱 → [파일 업로드] → S3
                    ↓
앱 → [완료 알림] → 백엔드
                    ↓
백엔드 → [메타데이터 저장] → DB
        [썸네일 생성] → S3
                    ↓
백엔드 → [최종 응답] → 앱
```