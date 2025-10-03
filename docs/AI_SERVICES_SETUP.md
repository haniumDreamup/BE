# AI 서비스 실제 연동 가이드

BIF-AI Backend의 Mock 데이터를 실제 AI 서비스로 전환하는 설정 가이드입니다.

## 🔧 수정 사항

### 1. ImageAnalysisService - Google Vision API 연동 ✅

**변경 내용:**
- Mock 데이터 제거
- 실제 GoogleVisionService 호출로 변경
- MultipartFile을 직접 Vision API에 전달
- 폴백 메커니즘 추가

**주요 변경:**
```java
// Before: Mock 데이터
private List<Map<String, Object>> detectObjects(String imageUrl) {
  return List.of(Map.of("label", "사람", ...)); // 하드코딩
}

// After: 실제 API 호출
private void processImageAsync(..., MultipartFile imageFile) {
  var visionResult = googleVisionService.analyzeImage(imageFile);
  objects = convertVisionObjectsToMap(visionResult.getObjects());
}
```

### 2. OpenAI ChatClient 설정 추가 ✅

**새 파일:** `OpenAIConfig.java`

Spring AI를 사용한 ChatClient Bean 생성:
- OpenAiApi 클라이언트
- OpenAiChatModel
- ChatClient 빈 자동 주입

---

## 🚀 실제 서비스 활성화 방법

### 1️⃣ Google Cloud Vision API 설정

#### 필수 작업:

1. **Google Cloud Console에서 프로젝트 생성**
   - https://console.cloud.google.com/
   - 새 프로젝트 생성 또는 기존 프로젝트 선택

2. **Vision API 활성화**
   ```
   Google Cloud Console → APIs & Services → Library
   → "Cloud Vision API" 검색 → 활성화
   ```

3. **서비스 계정 생성 및 키 다운로드**
   ```
   IAM & Admin → Service Accounts → Create Service Account
   → 역할: "Cloud Vision AI 사용자" 추가
   → 키 생성 (JSON) → 다운로드
   ```

4. **키 파일 배치**
   ```bash
   # 방법 1: 프로젝트 리소스에 추가
   cp ~/Downloads/your-service-account-key.json \
      src/main/resources/google-cloud-credentials.json

   # 방법 2: 환경 변수로 설정
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/key.json"
   ```

5. **application.yml 설정**
   ```yaml
   google:
     cloud:
       vision:
         enabled: true  # false → true로 변경
         max-results: 10
         confidence-threshold: 0.7
       credentials:
         path: /google-cloud-credentials.json  # 또는 절대 경로
       project-id: your-project-id
   ```

6. **환경 변수 설정**
   ```bash
   export GOOGLE_VISION_ENABLED=true
   export GOOGLE_CLOUD_CREDENTIALS_PATH=/google-cloud-credentials.json
   export GOOGLE_CLOUD_PROJECT_ID=your-project-id
   ```

---

### 2️⃣ OpenAI API 설정

#### 필수 작업:

1. **OpenAI API 키 발급**
   - https://platform.openai.com/api-keys
   - "Create new secret key" 클릭
   - 생성된 키 복사 (한 번만 표시됨!)

2. **환경 변수 설정**
   ```bash
   export OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxx
   export OPENAI_MODEL=gpt-4o-mini  # 또는 gpt-4, gpt-3.5-turbo
   export OPENAI_TEMPERATURE=0.7
   export OPENAI_MAX_TOKENS=500
   ```

3. **application.yml 확인**
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY}  # 환경 변수 사용
         chat:
           options:
             model: ${OPENAI_MODEL:gpt-4o-mini}
             temperature: ${OPENAI_TEMPERATURE:0.7}
             max-tokens: ${OPENAI_MAX_TOKENS:500}
   ```

4. **비용 제한 설정 (중요!)**
   ```
   OpenAI Platform → Settings → Billing → Usage limits
   → "Hard limit" 설정 (예: $10/month)
   ```

---

### 3️⃣ Firebase Cloud Messaging (FCM) 설정

#### 필수 작업:

1. **Firebase 프로젝트 생성**
   - https://console.firebase.google.com/
   - "프로젝트 추가" 클릭

2. **서비스 계정 키 다운로드**
   ```
   프로젝트 설정 → 서비스 계정
   → "새 비공개 키 생성" → JSON 다운로드
   ```

3. **키 파일 배치**
   ```bash
   cp ~/Downloads/firebase-adminsdk-xxxxx.json \
      src/main/resources/firebase-service-account.json
   ```

4. **application.yml 설정**
   ```yaml
   fcm:
     enabled: true  # false → true로 변경
     project-id: your-firebase-project-id
     credentials-path: firebase-service-account.json
   ```

5. **환경 변수 설정**
   ```bash
   export FCM_ENABLED=true
   export FCM_PROJECT_ID=your-firebase-project-id
   export FCM_CREDENTIALS_PATH=firebase-service-account.json
   ```

---

## 📋 전체 환경 변수 체크리스트

### 필수 환경 변수

```bash
# Google Cloud Vision
export GOOGLE_VISION_ENABLED=true
export GOOGLE_CLOUD_CREDENTIALS_PATH=/path/to/google-cloud-credentials.json
export GOOGLE_CLOUD_PROJECT_ID=your-gcp-project-id

# OpenAI
export OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxx
export OPENAI_MODEL=gpt-4o-mini
export OPENAI_TEMPERATURE=0.7
export OPENAI_MAX_TOKENS=500

# Firebase Cloud Messaging
export FCM_ENABLED=true
export FCM_PROJECT_ID=your-firebase-project-id
export FCM_CREDENTIALS_PATH=firebase-service-account.json

# AWS S3 (이미지 저장용)
export AWS_ACCESS_KEY=your-access-key
export AWS_SECRET_KEY=your-secret-key
export S3_BUCKET_NAME=bifai-media
export AWS_REGION=ap-northeast-2

# JWT Secret
export JWT_SECRET=your-super-long-secret-key-minimum-64-characters
```

### .env 파일 예시

프로젝트 루트에 `.env` 파일 생성:

```bash
# .env
GOOGLE_VISION_ENABLED=true
GOOGLE_CLOUD_CREDENTIALS_PATH=/app/config/google-cloud-credentials.json
GOOGLE_CLOUD_PROJECT_ID=bifai-project-123456

OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxx
OPENAI_MODEL=gpt-4o-mini
OPENAI_TEMPERATURE=0.7
OPENAI_MAX_TOKENS=500

FCM_ENABLED=true
FCM_PROJECT_ID=bifai-reminder
FCM_CREDENTIALS_PATH=/app/config/firebase-service-account.json

AWS_ACCESS_KEY=AKIAXXXXXXXXXXXXX
AWS_SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
S3_BUCKET_NAME=bifai-media
AWS_REGION=ap-northeast-2

JWT_SECRET=prod-secret-key-minimum-64-characters-hs512
```

---

## 🧪 테스트 방법

### 1. Google Vision API 테스트

```bash
curl -X POST http://localhost:8080/api/v1/vision/analyze \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@/path/to/test-image.jpg" \
  -F "analysisType=ON_DEMAND"
```

**기대 결과:**
```json
{
  "success": true,
  "data": {
    "objects": [
      {"label": "사람", "confidence": 0.95},
      {"label": "자동차", "confidence": 0.88}
    ],
    "text": "출구",
    "simpleDescription": "발견한 것: 사람, 자동차\n글자가 있어요: 출구"
  }
}
```

### 2. OpenAI ChatClient 테스트

```bash
curl -X POST http://localhost:8080/api/v1/ai/interpret \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "objects": [{"label": "사람", "confidence": 0.95}],
    "extractedText": "출구",
    "userQuestion": "이게 뭐예요?"
  }'
```

**기대 결과:**
```json
{
  "success": true,
  "data": {
    "description": "사람이 보이고 '출구'라는 글자가 있어요.",
    "action": "출구 방향으로 가면 나갈 수 있어요.",
    "safety": "SAFE"
  }
}
```

### 3. FCM 푸시 알림 테스트

```bash
curl -X POST http://localhost:8080/api/v1/notifications/test \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fcmToken": "YOUR_DEVICE_FCM_TOKEN",
    "title": "테스트 알림",
    "body": "FCM이 정상 작동합니다"
  }'
```

---

## 🔍 문제 해결

### Google Vision API 오류

**오류:** `Vision API 클라이언트 초기화 실패`
```
원인: 인증 파일 경로가 잘못됨
해결:
1. 파일 경로 확인: ls -la src/main/resources/google-cloud-credentials.json
2. 환경 변수 확인: echo $GOOGLE_CLOUD_CREDENTIALS_PATH
3. 로그 확인: tail -f logs/bifai-backend.log | grep Vision
```

**오류:** `Permission denied`
```
원인: 서비스 계정 권한 부족
해결:
1. Google Cloud Console → IAM
2. 서비스 계정에 "Cloud Vision AI 사용자" 역할 추가
```

### OpenAI API 오류

**오류:** `ChatClient가 null입니다`
```
원인: API 키가 설정되지 않음
해결:
1. 환경 변수 확인: echo $OPENAI_API_KEY
2. application.yml 확인
3. 로그 확인: grep "ChatClient" logs/bifai-backend.log
```

**오류:** `Rate limit exceeded`
```
원인: OpenAI API 호출 제한 초과
해결:
1. OpenAI Platform에서 사용량 확인
2. 요청 빈도 조절 (RateLimiter 적용)
3. 더 높은 Tier로 업그레이드
```

### FCM 오류

**오류:** `Firebase 앱 초기화 실패`
```
원인: 인증 파일 형식 오류
해결:
1. JSON 파일 유효성 검증: cat firebase-service-account.json | jq
2. 프로젝트 ID 확인
3. 파일 권한 확인: chmod 644 firebase-service-account.json
```

---

## 💰 비용 관리

### Google Cloud Vision

- **무료 한도:** 월 1,000건
- **초과 비용:**
  - Label Detection: $1.50 / 1,000건
  - Text Detection: $1.50 / 1,000건
  - Object Localization: $1.50 / 1,000건

**예상 비용 (월 10,000건 처리):**
- 10,000건 × $1.50 / 1,000 = **$15/month**

### OpenAI API

- **gpt-4o-mini:**
  - Input: $0.150 / 1M tokens
  - Output: $0.600 / 1M tokens
- **gpt-3.5-turbo:**
  - Input: $0.50 / 1M tokens
  - Output: $1.50 / 1M tokens

**예상 비용 (월 10,000 요청, 평균 500 tokens):**
- 10,000 × 500 = 5M tokens
- gpt-4o-mini: 5M × ($0.150 + $0.600) / 1M = **$3.75/month**
- gpt-3.5-turbo: 5M × ($0.50 + $1.50) / 1M = **$10/month**

### Firebase Cloud Messaging

- **무료:** 무제한 알림 전송 (단, Firebase 프로젝트는 무료)

**총 예상 비용:**
- Google Vision: ~$15/month
- OpenAI: ~$4/month (gpt-4o-mini 사용 시)
- FCM: $0
- **합계: ~$19/month**

---

## 🔐 보안 권장 사항

### 1. API 키 관리

```bash
# ❌ 절대 하지 마세요
application.yml에 API 키 하드코딩
GitHub에 인증 파일 커밋

# ✅ 권장 사항
.gitignore에 인증 파일 추가
환경 변수 사용
AWS Secrets Manager / Google Secret Manager 사용
```

### 2. .gitignore 설정

```gitignore
# API Keys & Credentials
*.json
!package.json
!tsconfig.json
.env
.env.local
.env.*.local

# Google Cloud
google-cloud-credentials.json
*-service-account.json

# Firebase
firebase-service-account.json
firebase-adminsdk-*.json

# AWS
.aws/credentials
```

### 3. 프로덕션 배포

```bash
# Docker Secrets 사용
docker secret create google_credentials google-cloud-credentials.json
docker secret create firebase_credentials firebase-service-account.json

# Kubernetes Secrets
kubectl create secret generic ai-credentials \
  --from-file=google-cloud-credentials.json \
  --from-file=firebase-service-account.json \
  --from-literal=openai-api-key=$OPENAI_API_KEY
```

---

## ✅ 완료 체크리스트

- [ ] Google Cloud Vision API 활성화
- [ ] Vision API 서비스 계정 생성 및 키 다운로드
- [ ] `google-cloud-credentials.json` 파일 배치
- [ ] `GOOGLE_VISION_ENABLED=true` 설정
- [ ] OpenAI API 키 발급
- [ ] `OPENAI_API_KEY` 환경 변수 설정
- [ ] Firebase 프로젝트 생성
- [ ] `firebase-service-account.json` 파일 배치
- [ ] `FCM_ENABLED=true` 설정
- [ ] AWS S3 버킷 생성 및 인증 설정
- [ ] 모든 환경 변수 `.env` 파일에 추가
- [ ] 테스트 실행 (Vision, OpenAI, FCM)
- [ ] 로그 확인하여 에러 없는지 검증
- [ ] 비용 제한 설정 (OpenAI, Google Cloud)

---

## 📚 참고 문서

- [Google Cloud Vision API](https://cloud.google.com/vision/docs)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
