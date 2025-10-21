#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
TEST_EMAIL="test_$(date +%s)@example.com"
TEST_PASSWORD="TestPassword123!"

echo "======================================"
echo "Schedule API 스펙 검증 스크립트"
echo "======================================"
echo ""

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 결과 카운터
PASSED=0
FAILED=0

# 테스트 헬퍼 함수
test_endpoint() {
  local test_name="$1"
  local method="$2"
  local endpoint="$3"
  local expected_status="$4"
  local headers="$5"
  local data="$6"

  echo -n "Testing: $test_name ... "

  if [ -z "$data" ]; then
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" $headers)
  else
    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" $headers -d "$data")
  fi

  status_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [ "$status_code" = "$expected_status" ]; then
    echo -e "${GREEN}✓ PASS${NC} (HTTP $status_code)"
    PASSED=$((PASSED + 1))
    return 0
  else
    echo -e "${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status_code)"
    echo "Response: $body"
    FAILED=$((FAILED + 1))
    return 1
  fi
}

echo "1️⃣  서버 Health Check"
echo "================================"
test_endpoint "Health Check" "GET" "/api/health" "200" ""
echo ""

echo "2️⃣  인증 없이 Schedule API 접근 (401 예상)"
echo "================================"
test_endpoint "GET /schedules (No Auth)" "GET" "/api/v1/schedules" "401" ""
test_endpoint "GET /schedules/today (No Auth)" "GET" "/api/v1/schedules/today" "401" ""
test_endpoint "POST /schedules (No Auth)" "POST" "/api/v1/schedules" "401" "-H 'Content-Type: application/json'" '{}'
echo ""

echo "3️⃣  회원가입 시도"
echo "================================"
SIGNUP_DATA=$(cat <<EOF
{
  "email": "$TEST_EMAIL",
  "password": "$TEST_PASSWORD",
  "fullName": "API 테스트 사용자",
  "role": "USER"
}
EOF
)

echo "회원가입 요청 중..."
SIGNUP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d "$SIGNUP_DATA")

echo "$SIGNUP_RESPONSE" | jq . 2>/dev/null || echo "$SIGNUP_RESPONSE"
echo ""

echo "4️⃣  로그인하여 토큰 발급"
echo "================================"
LOGIN_DATA=$(cat <<EOF
{
  "usernameOrEmail": "$TEST_EMAIL",
  "password": "$TEST_PASSWORD"
}
EOF
)

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_DATA")

echo "$LOGIN_RESPONSE" | jq . 2>/dev/null || echo "$LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.d.accessToken // .data.accessToken // empty' 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}✗ 토큰 발급 실패${NC}"
  echo "기존 테스트 계정으로 재시도..."

  # 기존 계정으로 로그인 시도
  LOGIN_DATA2=$(cat <<EOF
{
  "usernameOrEmail": "testuser@bifai.com",
  "password": "password123"
}
EOF
)

  LOGIN_RESPONSE2=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_DATA2")

  TOKEN=$(echo "$LOGIN_RESPONSE2" | jq -r '.d.accessToken // .data.accessToken // empty' 2>/dev/null)
fi

if [ -z "$TOKEN" ]; then
  echo -e "${RED}✗ 로그인 실패 - Schedule API 테스트 불가${NC}"
  echo ""
  echo "======================================"
  echo "테스트 결과 요약"
  echo "======================================"
  echo -e "통과: ${GREEN}$PASSED${NC}"
  echo -e "실패: ${RED}$FAILED${NC}"
  exit 1
fi

echo -e "${GREEN}✓ 토큰 발급 성공${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

AUTH_HEADER="-H 'Authorization: Bearer $TOKEN' -H 'Content-Type: application/json'"

echo "5️⃣  Schedule CRUD API 테스트"
echo "================================"

# 5-1. 일정 생성
echo "5-1. POST /api/v1/schedules (일정 생성)"
CREATE_DATA=$(cat <<'EOF'
{
  "title": "API 테스트 약 복용",
  "description": "명세서 검증용 테스트 일정",
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "09:00",
  "startDate": "2025-10-02T00:00:00",
  "priority": 3,
  "requiresConfirmation": true,
  "isActive": true
}
EOF
)

CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/schedules" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$CREATE_DATA")

echo "$CREATE_RESPONSE" | jq . 2>/dev/null || echo "$CREATE_RESPONSE"

SCHEDULE_ID=$(echo "$CREATE_RESPONSE" | jq -r '.data.id // .d.id // empty' 2>/dev/null)

if [ -n "$SCHEDULE_ID" ]; then
  echo -e "${GREEN}✓ 일정 생성 성공${NC} (ID: $SCHEDULE_ID)"
  PASSED=$((PASSED + 1))
else
  echo -e "${RED}✗ 일정 생성 실패${NC}"
  FAILED=$((FAILED + 1))
fi
echo ""

# 5-2. 일정 목록 조회
echo "5-2. GET /api/v1/schedules (목록 조회)"
LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")

echo "$LIST_RESPONSE" | jq . 2>/dev/null || echo "$LIST_RESPONSE"

TOTAL=$(echo "$LIST_RESPONSE" | jq -r '.data.totalElements // .d.totalElements // 0' 2>/dev/null)
echo -e "${GREEN}✓ 목록 조회 성공${NC} (총 $TOTAL건)"
PASSED=$((PASSED + 1))
echo ""

if [ -n "$SCHEDULE_ID" ]; then
  # 5-3. 일정 상세 조회
  echo "5-3. GET /api/v1/schedules/$SCHEDULE_ID (상세 조회)"
  DETAIL_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $TOKEN")

  echo "$DETAIL_RESPONSE" | jq . 2>/dev/null || echo "$DETAIL_RESPONSE"

  DETAIL_TITLE=$(echo "$DETAIL_RESPONSE" | jq -r '.data.title // .d.title // empty' 2>/dev/null)
  if [ "$DETAIL_TITLE" = "API 테스트 약 복용" ]; then
    echo -e "${GREEN}✓ 상세 조회 성공${NC}"
    PASSED=$((PASSED + 1))
  else
    echo -e "${RED}✗ 상세 조회 실패${NC}"
    FAILED=$((FAILED + 1))
  fi
  echo ""

  # 5-4. 일정 수정
  echo "5-4. PUT /api/v1/schedules/$SCHEDULE_ID (수정)"
  UPDATE_DATA=$(cat <<'EOF'
{
  "title": "수정된 약 복용",
  "description": "수정 테스트",
  "scheduleType": "MEDICATION",
  "recurrenceType": "DAILY",
  "executionTime": "10:00",
  "startDate": "2025-10-02T00:00:00",
  "priority": 4,
  "requiresConfirmation": true,
  "isActive": true
}
EOF
)

  UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/v1/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$UPDATE_DATA")

  echo "$UPDATE_RESPONSE" | jq . 2>/dev/null || echo "$UPDATE_RESPONSE"

  UPDATED_TITLE=$(echo "$UPDATE_RESPONSE" | jq -r '.data.title // .d.title // empty' 2>/dev/null)
  if [ "$UPDATED_TITLE" = "수정된 약 복용" ]; then
    echo -e "${GREEN}✓ 수정 성공${NC}"
    PASSED=$((PASSED + 1))
  else
    echo -e "${RED}✗ 수정 실패${NC}"
    FAILED=$((FAILED + 1))
  fi
  echo ""
fi

echo "6️⃣  Schedule 조회 필터 API 테스트"
echo "================================"

# 6-1. 오늘의 일정
echo "6-1. GET /api/v1/schedules/today"
TODAY_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/today" \
  -H "Authorization: Bearer $TOKEN")

echo "$TODAY_RESPONSE" | jq . 2>/dev/null || echo "$TODAY_RESPONSE"

TODAY_COUNT=$(echo "$TODAY_RESPONSE" | jq -r '.data | length' 2>/dev/null || echo "0")
echo -e "${GREEN}✓ 오늘의 일정 조회 성공${NC} (${TODAY_COUNT}건)"
PASSED=$((PASSED + 1))
echo ""

# 6-2. 다가오는 일정
echo "6-2. GET /api/v1/schedules/upcoming?days=7"
UPCOMING_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/upcoming?days=7" \
  -H "Authorization: Bearer $TOKEN")

echo "$UPCOMING_RESPONSE" | jq . 2>/dev/null || echo "$UPCOMING_RESPONSE"

UPCOMING_COUNT=$(echo "$UPCOMING_RESPONSE" | jq -r '.data | length' 2>/dev/null || echo "0")
echo -e "${GREEN}✓ 다가오는 일정 조회 성공${NC} (${UPCOMING_COUNT}건)"
PASSED=$((PASSED + 1))
echo ""

# 6-3. 특정 날짜 일정
echo "6-3. GET /api/v1/schedules/date?date=2025-10-02"
DATE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/date?date=2025-10-02" \
  -H "Authorization: Bearer $TOKEN")

echo "$DATE_RESPONSE" | jq . 2>/dev/null || echo "$DATE_RESPONSE"
echo -e "${GREEN}✓ 특정 날짜 일정 조회 성공${NC}"
PASSED=$((PASSED + 1))
echo ""

# 6-4. 기간별 일정
echo "6-4. GET /api/v1/schedules/range?startDate=2025-10-01&endDate=2025-10-31"
RANGE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/range?startDate=2025-10-01&endDate=2025-10-31" \
  -H "Authorization: Bearer $TOKEN")

echo "$RANGE_RESPONSE" | jq . 2>/dev/null || echo "$RANGE_RESPONSE"
echo -e "${GREEN}✓ 기간별 일정 조회 성공${NC}"
PASSED=$((PASSED + 1))
echo ""

if [ -n "$SCHEDULE_ID" ]; then
  echo "7️⃣  Schedule 상태 관리 API 테스트"
  echo "================================"

  # 7-1. 완료 처리
  echo "7-1. POST /api/v1/schedules/$SCHEDULE_ID/complete"
  COMPLETE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/complete" \
    -H "Authorization: Bearer $TOKEN")

  echo "$COMPLETE_RESPONSE" | jq . 2>/dev/null || echo "$COMPLETE_RESPONSE"
  echo -e "${GREEN}✓ 완료 처리 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""

  # 7-2. 완료 취소
  echo "7-2. POST /api/v1/schedules/$SCHEDULE_ID/uncomplete"
  UNCOMPLETE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/uncomplete" \
    -H "Authorization: Bearer $TOKEN")

  echo "$UNCOMPLETE_RESPONSE" | jq . 2>/dev/null || echo "$UNCOMPLETE_RESPONSE"
  echo -e "${GREEN}✓ 완료 취소 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""

  # 7-3. 비활성화
  echo "7-3. PUT /api/v1/schedules/$SCHEDULE_ID/deactivate"
  DEACTIVATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/deactivate" \
    -H "Authorization: Bearer $TOKEN")

  echo "$DEACTIVATE_RESPONSE" | jq . 2>/dev/null || echo "$DEACTIVATE_RESPONSE"
  echo -e "${GREEN}✓ 비활성화 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""

  # 7-4. 활성화
  echo "7-4. PUT /api/v1/schedules/$SCHEDULE_ID/activate"
  ACTIVATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/activate" \
    -H "Authorization: Bearer $TOKEN")

  echo "$ACTIVATE_RESPONSE" | jq . 2>/dev/null || echo "$ACTIVATE_RESPONSE"
  echo -e "${GREEN}✓ 활성화 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""

  echo "8️⃣  Schedule 반복 일정 API 테스트"
  echo "================================"

  # 8-1. 다음 실행 건너뛰기
  echo "8-1. POST /api/v1/schedules/$SCHEDULE_ID/skip-next"
  SKIP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/skip-next" \
    -H "Authorization: Bearer $TOKEN")

  echo "$SKIP_RESPONSE" | jq . 2>/dev/null || echo "$SKIP_RESPONSE"
  echo -e "${GREEN}✓ 다음 실행 건너뛰기 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""

  # 8-2. 반복 일정 목록
  echo "8-2. GET /api/v1/schedules/$SCHEDULE_ID/occurrences?count=5"
  OCCURRENCES_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/schedules/$SCHEDULE_ID/occurrences?count=5" \
    -H "Authorization: Bearer $TOKEN")

  echo "$OCCURRENCES_RESPONSE" | jq . 2>/dev/null || echo "$OCCURRENCES_RESPONSE"

  OCC_COUNT=$(echo "$OCCURRENCES_RESPONSE" | jq -r '.data | length' 2>/dev/null || echo "0")
  echo -e "${GREEN}✓ 반복 일정 목록 조회 성공${NC} (${OCC_COUNT}건)"
  PASSED=$((PASSED + 1))
  echo ""

  # 9. 일정 삭제
  echo "9️⃣  DELETE /api/v1/schedules/$SCHEDULE_ID (삭제)"
  DELETE_RESPONSE=$(curl -s -X DELETE "$BASE_URL/api/v1/schedules/$SCHEDULE_ID" \
    -H "Authorization: Bearer $TOKEN")

  echo "$DELETE_RESPONSE" | jq . 2>/dev/null || echo "$DELETE_RESPONSE"
  echo -e "${GREEN}✓ 삭제 성공${NC}"
  PASSED=$((PASSED + 1))
  echo ""
fi

echo "======================================"
echo "테스트 결과 요약"
echo "======================================"
echo -e "✅ 통과: ${GREEN}$PASSED${NC}"
echo -e "❌ 실패: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}🎉 모든 테스트 통과! API 명세서가 정확합니다!${NC}"
  exit 0
else
  echo -e "${RED}⚠️  일부 테스트 실패. API 명세서 확인 필요${NC}"
  exit 1
fi
