#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
echo "=== BIF-AI Backend 엔드포인트 종합 테스트 ==="
echo "서버: $BASE_URL"
echo "시작 시간: $(date)"
echo ""

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    
    echo "🔍 Testing: $method $endpoint"
    echo "설명: $description"
    
    response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" 2>/dev/null)
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo "✅ PASS: $status_code (예상: $expected_status)"
    else
        echo "❌ FAIL: $status_code (예상: $expected_status)"
    fi
    
    if [ ${#body} -gt 0 ] && [ ${#body} -lt 200 ]; then
        echo "응답: $body"
    elif [ ${#body} -gt 200 ]; then
        echo "응답: $(echo "$body" | head -c 200)..."
    fi
    echo ""
}

echo "=== 1. 공개 엔드포인트 (인증 불필요) ==="

# Health 체크
test_endpoint "GET" "/api/health" "200" "애플리케이션 헬스 체크"

# OAuth2 엔드포인트  
test_endpoint "GET" "/api/auth/oauth2/login-urls" "200" "OAuth2 로그인 URL 조회"

echo "=== 2. 인증 필요 엔드포인트 ==="

# WebSocket
test_endpoint "GET" "/ws" "403" "WebSocket 연결 (인증 필요)"

# 접근성 
test_endpoint "GET" "/api/accessibility/settings" "403" "접근성 설정 조회 (인증 필요)"

# 통계
test_endpoint "GET" "/api/statistics/geofence" "403" "지오펜스 통계 (인증 필요)"

# 실험
test_endpoint "GET" "/api/experiments" "403" "실험 목록 조회 (인증 필요)"

echo "=== 테스트 완료 ==="
echo "종료 시간: $(date)"
