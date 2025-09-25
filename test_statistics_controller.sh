#!/bin/bash

# StatisticsController 테스트 스크립트
# 통계 API 엔드포인트 검증

BASE_URL="http://localhost:8080"
ACCESS_TOKEN=""

# 이미 생성된 사용자로 로그인
echo "=== 로그인 테스트 ==="
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail": "testuser_1758549671", "password": "TestPassword123"}')

echo "로그인 응답: $LOGIN_RESPONSE"

# 토큰 추출
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
echo "추출된 토큰: ${ACCESS_TOKEN:0:30}..."

if [[ -z "$ACCESS_TOKEN" ]]; then
    echo "❌ 토큰 추출 실패"
    exit 1
fi

echo ""
echo "=== StatisticsController 테스트 시작 ==="

# 1. 지오펜스 통계 조회
echo "1. 지오펜스 통계 조회"
GEOFENCE_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${GEOFENCE_RESPONSE: -3}"
RESPONSE_BODY="${GEOFENCE_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 2. 지오펜스 통계 조회 (날짜 파라미터 포함)
echo "2. 지오펜스 통계 조회 (날짜 파라미터)"
GEOFENCE_DATE_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence?startDate=2025-09-01&endDate=2025-09-22" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${GEOFENCE_DATE_RESPONSE: -3}"
RESPONSE_BODY="${GEOFENCE_DATE_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 3. 일일 활동 통계 조회 (여러 날짜)
echo "3. 일일 활동 통계 조회 (여러 날짜)"
DAILY_ACTIVITY_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/daily-activity" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${DAILY_ACTIVITY_RESPONSE: -3}"
RESPONSE_BODY="${DAILY_ACTIVITY_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 4. 일일 활동 통계 조회 (단일 날짜)
echo "4. 일일 활동 통계 조회 (단일 날짜)"
SINGLE_ACTIVITY_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/daily-activity/single?date=2025-09-22" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${SINGLE_ACTIVITY_RESPONSE: -3}"
RESPONSE_BODY="${SINGLE_ACTIVITY_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 5. 안전 통계 조회
echo "5. 안전 통계 조회"
SAFETY_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/safety" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${SAFETY_RESPONSE: -3}"
RESPONSE_BODY="${SAFETY_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 6. 통계 요약 조회
echo "6. 통계 요약 조회"
SUMMARY_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/summary" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${SUMMARY_RESPONSE: -3}"
RESPONSE_BODY="${SUMMARY_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 7. 인증 없이 접근 테스트
echo "7. 인증 없이 통계 조회"
NO_AUTH_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence")

STATUS_CODE="${NO_AUTH_RESPONSE: -3}"
RESPONSE_BODY="${NO_AUTH_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

# 8. 잘못된 날짜 형식 테스트
echo "8. 잘못된 날짜 형식 테스트"
INVALID_DATE_RESPONSE=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence?startDate=invalid-date" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

STATUS_CODE="${INVALID_DATE_RESPONSE: -3}"
RESPONSE_BODY="${INVALID_DATE_RESPONSE%???}"

echo "상태: $STATUS_CODE"
echo "응답: $RESPONSE_BODY"
echo ""

echo "=== StatisticsController 핵심 기능 테스트 완료 ==="