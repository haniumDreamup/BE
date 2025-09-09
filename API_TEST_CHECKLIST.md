# API 테스트 체크리스트

## 1. 헬스체크 & 기본
- [ ] GET `/actuator/health` - 서버 상태
- [ ] GET `/api/health` - API 헬스체크

## 2. 인증 (Auth) - 가장 먼저 테스트
- [ ] POST `/api/v1/auth/register` - 회원가입
  ```json
  {
    "username": "testuser",
    "email": "test@test.com",
    "password": "Test1234!",
    "fullName": "테스트유저"
  }
  ```
- [ ] POST `/api/v1/auth/login` - 로그인
  ```json
  {
    "username": "testuser",
    "password": "Test1234!"
  }
  ```
- [ ] POST `/api/v1/auth/refresh` - 토큰 갱신
- [ ] POST `/api/v1/auth/logout` - 로그아웃
- [ ] GET `/api/v1/auth/oauth2/login-urls` - OAuth2 URL 조회

## 3. 사용자 관리 (User)
- [ ] GET `/api/v1/users/profile` - 내 정보 조회
- [ ] PUT `/api/v1/users/profile` - 내 정보 수정
- [ ] POST `/api/v1/users/preferences` - 설정 저장
- [ ] GET `/api/v1/users/preferences` - 설정 조회

## 4. 일정 관리 (Schedule)
- [ ] POST `/api/v1/schedules` - 일정 생성
  ```json
  {
    "title": "병원 방문",
    "description": "정기 검진",
    "startTime": "2025-09-01T10:00:00",
    "location": "서울대병원"
  }
  ```
- [ ] GET `/api/v1/schedules` - 일정 목록
- [ ] GET `/api/v1/schedules/{id}` - 일정 상세
- [ ] PUT `/api/v1/schedules/{id}` - 일정 수정
- [ ] DELETE `/api/v1/schedules/{id}` - 일정 삭제

## 5. 약물 관리 (Medication)
- [ ] POST `/api/v1/medications` - 약물 등록
  ```json
  {
    "name": "혈압약",
    "dosage": "1정",
    "frequency": "하루 2번",
    "timeOfDay": "아침, 저녁"
  }
  ```
- [ ] GET `/api/v1/medications` - 약물 목록
- [ ] PUT `/api/v1/medications/{id}` - 약물 수정
- [ ] DELETE `/api/v1/medications/{id}` - 약물 삭제
- [ ] POST `/api/v1/medications/{id}/taken` - 복용 기록

## 6. 활동 로그 (Activity)
- [ ] POST `/api/v1/activities` - 활동 기록
- [ ] GET `/api/v1/activities` - 활동 목록
- [ ] GET `/api/v1/activities/stats` - 활동 통계

## 7. 긴급 기능 (Emergency)
- [ ] POST `/api/v1/emergency/contacts` - 긴급 연락처 등록
- [ ] GET `/api/v1/emergency/contacts` - 긴급 연락처 조회
- [ ] POST `/api/v1/emergency/alert` - 긴급 알림 발송

## 8. 보호자 기능 (Guardian)
- [ ] POST `/api/v1/guardian/request` - 보호자 요청
- [ ] GET `/api/v1/guardian/relationships` - 관계 조회
- [ ] GET `/api/v1/guardian/dashboard` - 보호자 대시보드

## 9. 지오펜스 (Geofence)
- [ ] POST `/api/v1/geofences` - 지오펜스 생성
  ```json
  {
    "name": "집",
    "latitude": 37.5665,
    "longitude": 126.9780,
    "radius": 100
  }
  ```
- [ ] GET `/api/v1/geofences` - 지오펜스 목록
- [ ] DELETE `/api/v1/geofences/{id}` - 지오펜스 삭제

## 10. 낙상 감지 (Fall Detection)
- [ ] POST `/api/v1/fall-detection/report` - 낙상 신고
- [ ] GET `/api/v1/fall-detection/history` - 낙상 이력

## 11. AI 기능 (AI)
- [ ] POST `/api/v1/ai/analyze-image` - 이미지 분석 (OpenAI API 필요)
- [ ] POST `/api/v1/ai/situational-guidance` - 상황 안내
- [ ] POST `/api/v1/ai/simplify-text` - 텍스트 간소화

## 12. WebSocket (실시간)
- [ ] CONNECT `/ws` - WebSocket 연결
- [ ] SUBSCRIBE `/topic/notifications` - 알림 구독
- [ ] SEND `/app/message` - 메시지 전송

---

## 테스트 도구

### Postman Collection 생성
```bash
# Swagger에서 Postman으로 가져오기
1. Swagger UI 접속
2. /v3/api-docs 접속
3. JSON 복사
4. Postman > Import > Raw Text
```

### cURL 테스트 스크립트
```bash
# 로그인 후 토큰 저장
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!"}' \
  | jq -r '.data.accessToken')

# 인증이 필요한 API 호출
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/users/profile
```

### 자동화 테스트
```bash
# Newman으로 Postman Collection 실행
npm install -g newman
newman run bifai-api-collection.json
```

---

## 체크 포인트

### 🟢 정상 동작 확인
- 200 OK 응답
- 응답 시간 < 500ms
- 정확한 데이터 반환

### 🟡 경고 사항
- 응답 시간 500ms ~ 1s
- 부분적 데이터 누락
- 캐시 미적용

### 🔴 문제 발생
- 500 에러
- 인증 실패
- 데이터베이스 연결 실패

---

## 트러블슈팅

### JWT 토큰 문제
```bash
# .env에서 JWT_SECRET 확인
# 64자 이상인지 체크
```

### 데이터베이스 연결 실패
```bash
# MySQL 컨테이너 상태
docker-compose ps mysql
docker-compose logs mysql
```

### Redis 연결 실패
```bash
# Redis 컨테이너 상태
docker-compose ps redis
docker-compose exec redis redis-cli ping
```

### 메모리 부족
```bash
# Docker 메모리 제한 증가
docker-compose down
# docker-compose.yml에서 mem_limit 수정
docker-compose up -d
```