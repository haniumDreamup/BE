# 다음 통합 테스트 시나리오

## 기반: 완료된 초기 통합 테스트

✅ **완료된 테스트**:
- 서버 Health Check (200 OK)
- 인증 실패 시나리오 (401)
- 플러터-서버 네트워크 통신 검증
- 실시간 로그 모니터링 설정
- 보안 헤더 확인
- CORS 동작 확인

---

## 1단계: 완전한 OAuth2 로그인 플로우 테스트

### 1-1. 카카오 로그인

**사전 준비**:
```bash
# 환경변수 확인
ssh ubuntu@43.200.49.171 'docker exec bifai-backend env | grep OAUTH2_KAKAO'
```

**테스트 순서**:

1. **플러터 앱에서 카카오 로그인 버튼 클릭**
   - 예상: 카카오 OAuth2 인증 페이지로 리다이렉트
   - 확인: 플러터 로그에서 리다이렉트 URL 확인

2. **카카오 계정 로그인 및 동의**
   - 예상: Authorization Code 발급
   - 확인: URL callback 파라미터 확인

3. **서버에서 토큰 교환**
   - 예상: Access Token + Refresh Token 발급
   - 서버 로그 확인:
     ```bash
     ssh ubuntu@43.200.49.171 'docker logs -f bifai-backend | grep -i "oauth2\|token"'
     ```

4. **JWT 토큰 발급**
   - 예상: 서버에서 Custom JWT 토큰 생성
   - 플러터에서 토큰 저장 확인

5. **자동 로그인 상태 유지**
   - 예상: 플러터 앱에서 메인 화면으로 이동
   - 확인: 플러터 라우터 로그

**예상 서버 로그**:
```
[OAuth2Controller] 카카오 로그인 요청
[OAuth2Service] Authorization Code 수신: ABC123...
[OAuth2Service] 카카오 토큰 요청 성공
[OAuth2Service] 사용자 정보 조회: 홍길동 (kakao:123456789)
[JwtService] JWT 토큰 발급: userId=1
[AuthController] 로그인 성공: 홍길동
```

**예상 플러터 로그**:
```
[AUTH] 카카오 로그인 시작
[AUTH] Redirect URL: https://kauth.kakao.com/oauth/authorize?...
[AUTH] Callback 수신: code=ABC123...
[AUTH] 서버 JWT 토큰 수신
[STORAGE] JWT 토큰 저장 완료
[ROUTER] 인증 성공 - 메인 화면 이동
```

**검증 항목**:
- [ ] 카카오 OAuth2 리다이렉트 성공
- [ ] Authorization Code 발급
- [ ] 서버 토큰 교환 성공
- [ ] JWT 토큰 발급
- [ ] 플러터 앱 자동 로그인
- [ ] 에러 없이 메인 화면 진입

---

### 1-2. 네이버 로그인 (카카오와 유사)

**차이점**:
- Provider: `naver`
- User Attribute: `response` 객체 내부

**테스트 명령어**:
```bash
# 네이버 OAuth2 환경변수 확인
ssh ubuntu@43.200.49.171 'docker exec bifai-backend env | grep OAUTH2_NAVER'
```

---

## 2단계: 인증된 API 호출 테스트

### 2-1. 사용자 정보 조회

**API**: `GET /api/users/me`

**플러터 앱에서 실행**:
- 메인 화면 진입 후 자동 호출
- 프로필 화면에서 수동 호출

**cURL 테스트** (토큰 필요):
```bash
# 1. 로그인하여 토큰 획득 (플러터 앱에서)
# 2. 토큰을 복사하여 테스트

TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -H "Authorization: Bearer $TOKEN" \
     http://43.200.49.171:8080/api/users/me
```

**예상 응답**:
```json
{
  "s": true,
  "d": {
    "userId": 1,
    "username": "홍길동",
    "email": "hong@example.com",
    "profileImageUrl": "https://...",
    "role": "USER",
    "createdAt": "2025-01-15T10:30:00Z"
  },
  "t": [2025, 9, 30, 14, 30, 0, 0]
}
```

**서버 로그 확인**:
```bash
ssh ubuntu@43.200.49.171 'docker logs bifai-backend --tail 20 | grep "GET /api/users/me"'
```

---

### 2-2. 약 복용 일정 등록

**API**: `POST /api/medications`

**플러터 앱에서**:
1. 약 추가 버튼 클릭
2. 약 정보 입력:
   - 약 이름: "타이레놀"
   - 복용 시간: 08:00, 14:00, 20:00
   - 복용 기간: 2025-09-30 ~ 2025-10-07
3. 저장 버튼 클릭

**cURL 테스트**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -X POST http://43.200.49.171:8080/api/medications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "타이레놀",
    "dosage": "500mg",
    "times": ["08:00", "14:00", "20:00"],
    "startDate": "2025-09-30",
    "endDate": "2025-10-07",
    "notes": "식후 30분"
  }'
```

**예상 응답**:
```json
{
  "s": true,
  "d": {
    "medicationId": 123,
    "name": "타이레놀",
    "dosage": "500mg",
    "times": ["08:00", "14:00", "20:00"],
    "startDate": "2025-09-30",
    "endDate": "2025-10-07",
    "createdAt": "2025-09-30T14:35:00Z"
  },
  "m": "약 복용 일정이 등록되었어요",
  "t": [2025, 9, 30, 14, 35, 0, 0]
}
```

**플러터 로그**:
```
[MEDICATION] 약 등록 요청: 타이레놀
[API] POST /api/medications 성공
[UI] 약 목록 업데이트
[NOTIFICATION] 알림 예약: 08:00, 14:00, 20:00
```

**서버 로그**:
```
[MedicationController] 약 등록 요청: userId=1, name=타이레놀
[MedicationService] 약 정보 저장 완료: id=123
[NotificationService] 알림 스케줄 등록: 3개 시간대
```

---

### 2-3. 건강 데이터 기록

**API**: `POST /api/health/records`

**플러터 앱에서**:
1. 건강 기록 버튼 클릭
2. 데이터 입력:
   - 혈압: 120/80
   - 혈당: 95
   - 체온: 36.5
   - 메모: "아침 식사 후 측정"
3. 저장

**cURL 테스트**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -X POST http://43.200.49.171:8080/api/health/records \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bloodPressureSystolic": 120,
    "bloodPressureDiastolic": 80,
    "bloodSugar": 95,
    "temperature": 36.5,
    "notes": "아침 식사 후 측정",
    "recordedAt": "2025-09-30T09:00:00Z"
  }'
```

---

### 2-4. 보호자 연동

**API**: `POST /api/guardians/invite`

**플러터 앱에서**:
1. 보호자 추가 버튼
2. 보호자 휴대폰 번호 입력: 010-1234-5678
3. 초대 메시지 전송

**cURL 테스트**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -X POST http://43.200.49.171:8080/api/guardians/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "010-1234-5678",
    "relationship": "가족",
    "message": "보호자로 등록을 요청합니다"
  }'
```

**예상 응답**:
```json
{
  "s": true,
  "d": {
    "inviteId": 456,
    "phoneNumber": "010-1234-5678",
    "status": "PENDING",
    "expiresAt": "2025-10-07T14:40:00Z"
  },
  "m": "보호자 초대를 보냈어요",
  "t": [2025, 9, 30, 14, 40, 0, 0]
}
```

---

## 3단계: 에러 시나리오 테스트

### 3-1. 네트워크 단절 시뮬레이션

**방법 1: 서버 일시 중지**
```bash
ssh ubuntu@43.200.49.171 'docker-compose -f docker-compose.prod.yml stop'
```

**플러터 앱 동작 확인**:
- [ ] 네트워크 에러 감지
- [ ] 사용자 친화적 메시지 표시
- [ ] 오프라인 모드 전환 (가능한 경우)
- [ ] 재시도 버튼 제공

**서버 재시작**:
```bash
ssh ubuntu@43.200.49.171 'docker-compose -f docker-compose.prod.yml start'
```

**플러터 앱 복구 확인**:
- [ ] 자동 재연결
- [ ] 대기 중이던 요청 재전송
- [ ] 로그인 상태 유지

---

### 3-2. 토큰 만료

**방법**: JWT 토큰 TTL을 짧게 설정 (테스트 환경)

**application-prod.yml** (임시 변경):
```yaml
app:
  jwt:
    access-token-expiration-ms: 60000  # 1분
```

**플러터 앱 동작 확인**:
- [ ] 1분 후 API 호출 시 401 에러
- [ ] Refresh Token으로 자동 갱신 시도
- [ ] 갱신 실패 시 로그인 화면으로 이동

**서버 로그**:
```
[JwtFilter] JWT 토큰 만료: userId=1
[JwtService] Refresh Token 검증 시도
[JwtService] 새 Access Token 발급: userId=1
```

---

### 3-3. 잘못된 데이터 전송

**API**: `POST /api/medications` (잘못된 데이터)

**cURL 테스트**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# 1. 필수 필드 누락
curl -X POST http://43.200.49.171:8080/api/medications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": ""
  }'

# 2. 잘못된 시간 형식
curl -X POST http://43.200.49.171:8080/api/medications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "타이레놀",
    "times": ["25:00"]
  }'

# 3. 날짜 논리 오류 (종료일이 시작일보다 빠름)
curl -X POST http://43.200.49.171:8080/api/medications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "타이레놀",
    "startDate": "2025-10-01",
    "endDate": "2025-09-30"
  }'
```

**예상 응답**:
```json
{
  "type": "https://bifai.app/problems/validation",
  "title": "입력 정보를 확인해 주세요",
  "status": 400,
  "detail": "약 이름을 입력해 주세요",
  "userAction": "약 이름을 다시 입력해 주세요",
  "timestamp": "2025-09-30T14:45:00Z",
  "fields": {
    "name": "약 이름은 필수입니다"
  }
}
```

---

### 3-4. 서버 타임아웃

**방법**: 긴 처리 시간이 필요한 API 호출

**예**: 대용량 이미지 분석 (Google Vision API)

```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# 10MB 이미지 업로드
curl -X POST http://43.200.49.171:8080/api/vision/analyze \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@large_image.jpg" \
  --max-time 5
```

**플러터 앱 동작 확인**:
- [ ] 로딩 인디케이터 표시
- [ ] 타임아웃 시 적절한 메시지
- [ ] 재시도 옵션 제공

---

## 4단계: 성능 테스트

### 4-1. 응답 시간 측정

**Health Check**:
```bash
for i in {1..10}; do
  time curl -s http://43.200.49.171:8080/api/health > /dev/null
done
```

**인증 API**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

for i in {1..10}; do
  time curl -s -H "Authorization: Bearer $TOKEN" \
    http://43.200.49.171:8080/api/users/me > /dev/null
done
```

**목표**:
- Health Check: < 200ms
- 단순 조회 API: < 500ms
- 복잡한 조회 API: < 1s
- AI 분석 API: < 3s

---

### 4-2. 동시 요청 처리

**Apache Bench 사용**:
```bash
# Health Check - 100 요청, 10 동시 연결
ab -n 100 -c 10 http://43.200.49.171:8080/api/health

# 인증 API - 50 요청, 5 동시 연결
ab -n 50 -c 5 \
  -H "Authorization: Bearer $TOKEN" \
  http://43.200.49.171:8080/api/users/me
```

**서버 모니터링**:
```bash
# 실시간 로그
ssh ubuntu@43.200.49.171 'docker stats bifai-backend'

# CPU, 메모리 사용량
ssh ubuntu@43.200.49.171 'docker exec bifai-backend top -b -n 1'
```

**목표**:
- 100 동시 사용자 지원
- CPU 사용률 < 70%
- 메모리 사용률 < 80%
- 에러율 < 1%

---

### 4-3. 대용량 데이터 처리

**시나리오**: 한 달치 건강 데이터 조회

**API**: `GET /api/health/records?startDate=2025-09-01&endDate=2025-09-30`

```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

time curl -H "Authorization: Bearer $TOKEN" \
  "http://43.200.49.171:8080/api/health/records?startDate=2025-09-01&endDate=2025-09-30"
```

**서버 로그**:
```
[HealthController] 건강 데이터 조회: userId=1, range=30일
[HealthService] 데이터 개수: 90개 (하루 3회 × 30일)
[HealthService] 조회 완료: 450ms
```

**목표**:
- 1000개 데이터: < 1s
- 10000개 데이터: < 3s
- 페이지네이션 적용 시: < 500ms

---

## 5단계: 종단 간 사용자 시나리오

### 시나리오 1: 아침 약 복용 알림

**시간**: 08:00

**플로우**:
1. 앱에서 푸시 알림 수신
2. 알림 클릭하여 앱 열기
3. "복용 완료" 버튼 클릭
4. 서버에 복용 기록 전송
5. 음성 피드백: "약을 잘 드셨어요!"

**로그 확인**:
```bash
# 플러터 로그
[NOTIFICATION] 푸시 알림 수신: 타이레놀 복용 시간
[UI] 약 복용 화면 열기
[MEDICATION] 복용 완료 기록: medicationId=123

# 서버 로그
[MedicationController] 복용 기록: userId=1, medicationId=123, time=08:00
[MedicationService] 복용 완료 처리
[NotificationService] 다음 알림 예약: 14:00
```

---

### 시나리오 2: 응급 상황 알림

**트리거**: 낙상 감지 (가속도계 센서)

**플로우**:
1. 플러터 앱에서 낙상 감지
2. 10초 카운트다운 시작
3. 사용자 반응 없음
4. 자동으로 보호자에게 알림 전송
5. 서버에서 SMS + 푸시 알림 발송

**API 호출**:
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

curl -X POST http://43.200.49.171:8080/api/emergency/alert \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "FALL_DETECTION",
    "location": {
      "latitude": 37.5665,
      "longitude": 126.9780
    },
    "timestamp": "2025-09-30T14:50:00Z",
    "autoDetected": true
  }'
```

**예상 응답**:
```json
{
  "s": true,
  "d": {
    "alertId": 789,
    "status": "SENT",
    "notifiedGuardians": [
      {"name": "보호자1", "phone": "010-1234-5678", "notified": true},
      {"name": "보호자2", "phone": "010-8765-4321", "notified": true}
    ]
  },
  "m": "보호자에게 알림을 보냈어요",
  "t": [2025, 9, 30, 14, 50, 0, 0]
}
```

**서버 로그**:
```
[EmergencyController] 응급 알림: userId=1, type=FALL_DETECTION
[EmergencyService] 보호자 목록 조회: 2명
[NotificationService] SMS 전송: 010-1234-5678
[NotificationService] 푸시 알림: 010-8765-4321
[EmergencyService] 응급 알림 완료: alertId=789
```

---

## 테스트 체크리스트

### 기능 테스트
- [ ] OAuth2 로그인 (카카오, 네이버)
- [ ] JWT 토큰 발급 및 갱신
- [ ] 사용자 정보 조회
- [ ] 약 복용 일정 등록
- [ ] 건강 데이터 기록
- [ ] 보호자 연동
- [ ] 푸시 알림
- [ ] 응급 알림

### 에러 처리
- [ ] 네트워크 단절
- [ ] 토큰 만료
- [ ] 잘못된 입력
- [ ] 서버 타임아웃
- [ ] 권한 없음 (403)
- [ ] 리소스 없음 (404)

### 성능
- [ ] 응답 시간 < 목표치
- [ ] 동시 요청 처리 (100명)
- [ ] 대용량 데이터 조회
- [ ] 메모리 사용량 안정
- [ ] CPU 사용량 < 70%

### 보안
- [ ] JWT 토큰 검증
- [ ] HTTPS 적용 (추후)
- [ ] SQL Injection 방어
- [ ] XSS 방어
- [ ] CSRF 방어
- [ ] Rate Limiting

### 접근성
- [ ] 음성 피드백
- [ ] 큰 버튼 (48dp)
- [ ] 단순한 문구 (5학년 수준)
- [ ] 2단계 이하 네비게이션

---

## 테스트 환경 설정

### 플러터 앱 실행
```bash
cd /Users/ihojun/Desktop/FE
flutter run -d chrome --web-port=8081
```

### 서버 로그 모니터링
```bash
ssh ubuntu@43.200.49.171 'docker logs -f bifai-backend'
```

### 동시 로그 확인 (tmux 사용)
```bash
# 세션 생성
tmux new -s bifai-test

# 화면 분할
Ctrl+B %  # 수직 분할
Ctrl+B "  # 수평 분할

# 왼쪽: 서버 로그
ssh ubuntu@43.200.49.171 'docker logs -f bifai-backend'

# 오른쪽: 플러터 앱 실행
cd /Users/ihojun/Desktop/FE
flutter run -d chrome --web-port=8081

# 세션 종료
Ctrl+B d
```

---

## 테스트 결과 문서화

각 테스트 완료 후 다음을 기록:

1. **테스트 ID**: TEST-001, TEST-002, ...
2. **테스트 시나리오**: 간략한 설명
3. **예상 결과**: 무엇이 일어나야 하는가
4. **실제 결과**: 무엇이 일어났는가
5. **성공/실패**: ✅ / ❌
6. **로그 스크린샷**: 필요 시
7. **개선 사항**: 발견된 이슈
8. **다음 단계**: 추가 테스트 필요 사항

---

## 참고 자료

- [초기 통합 테스트 리포트](./INTEGRATION_TEST_REPORT.md)
- [배포 트러블슈팅](./DEPLOYMENT_TROUBLESHOOTING.md)
- [API 문서](../api-docs/) (예정)
- [플러터 앱 가이드](../../FE/docs/) (예정)