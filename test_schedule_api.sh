#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== Schedule API 테스트 ==="
echo ""

# 1. 인증 없이 접근 (401 예상)
echo "1. 인증 없이 일정 목록 조회 (401 예상)"
curl -s -X GET "$BASE_URL/api/v1/schedules" \
  -H "accept: application/json" | jq . || echo "Response received"
echo ""
echo ""

# 2. Health check로 서버 상태 확인
echo "2. 서버 Health Check"
curl -s -X GET "$BASE_URL/api/health" \
  -H "accept: application/json" | jq .
echo ""
echo ""

# 3. 로그인 시도 (토큰 발급)
echo "3. 로그인 시도 (테스트 계정)"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

echo "$LOGIN_RESPONSE" | jq . || echo "$LOGIN_RESPONSE"

# 토큰 추출 시도
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty' 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo ""
  echo "⚠️  로그인 실패 또는 토큰 없음"
  echo "실제 테스트 계정으로 로그인 후 토큰을 수동으로 입력하세요"
  echo ""
  echo "수동 테스트 예시:"
  echo "TOKEN='your-jwt-token-here'"
  echo "curl -X GET '$BASE_URL/api/v1/schedules' \\"
  echo "  -H 'Authorization: Bearer \$TOKEN' \\"
  echo "  -H 'accept: application/json'"
else
  echo ""
  echo "4. 인증된 사용자로 일정 목록 조회"
  curl -s -X GET "$BASE_URL/api/v1/schedules" \
    -H "Authorization: Bearer $TOKEN" \
    -H "accept: application/json" | jq .
fi

echo ""
echo "=== API 엔드포인트 확인 ==="
echo ""
echo "다음 엔드포인트들이 배포되었어야 합니다:"
echo ""
echo "✓ POST   /api/v1/schedules                    - 일정 생성"
echo "✓ GET    /api/v1/schedules/{id}               - 일정 조회"
echo "✓ GET    /api/v1/schedules                    - 일정 목록"
echo "✓ PUT    /api/v1/schedules/{id}               - 일정 수정"
echo "✓ DELETE /api/v1/schedules/{id}               - 일정 삭제"
echo "✓ GET    /api/v1/schedules/today              - 오늘 일정"
echo "✓ GET    /api/v1/schedules/upcoming           - 다가오는 일정"
echo "✓ GET    /api/v1/schedules/date               - 특정 날짜 일정"
echo "✓ GET    /api/v1/schedules/range              - 기간별 일정"
echo "✓ POST   /api/v1/schedules/{id}/complete      - 완료 처리"
echo "✓ POST   /api/v1/schedules/{id}/uncomplete    - 완료 취소"
echo "✓ PUT    /api/v1/schedules/{id}/activate      - 활성화"
echo "✓ PUT    /api/v1/schedules/{id}/deactivate    - 비활성화"
echo "✓ POST   /api/v1/schedules/{id}/skip-next     - 다음 실행 건너뛰기"
echo "✓ GET    /api/v1/schedules/{id}/occurrences   - 반복 일정 목록"
echo ""
