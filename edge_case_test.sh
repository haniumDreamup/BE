#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
echo "=== 엣지 케이스 및 에러 시나리오 테스트 ==="
echo ""

test_edge_case() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    
    echo "🔍 $method $endpoint"
    echo "   시나리오: $description"
    
    response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" 2>/dev/null)
    status_code=$(echo "$response" | tail -n1)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo "   ✅ PASS: $status_code"
    else
        echo "   ❌ FAIL: $status_code (예상: $expected_status)"
    fi
    echo ""
}

echo "=== 1. 존재하지 않는 엔드포인트 (404 예상) ==="
test_edge_case "GET" "/api/nonexistent" "404" "존재하지 않는 API 경로"
test_edge_case "GET" "/api/health/invalid" "404" "잘못된 헬스 체크 경로"
test_edge_case "GET" "/api/auth/invalid" "404" "잘못된 인증 경로"

echo "=== 2. 잘못된 HTTP 메서드 (405 예상) ==="
test_edge_case "DELETE" "/api/health" "405" "헬스 체크에 DELETE 메서드"
test_edge_case "PUT" "/api/auth/oauth2/login-urls" "405" "OAuth2 URL에 PUT 메서드"

echo "=== 3. 인증이 필요한 엔드포인트에 잘못된 토큰 (403 예상) ==="
echo "🔍 GET /api/accessibility/settings with invalid token"
echo "   시나리오: 잘못된 JWT 토큰으로 접근"
invalid_response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer invalid-token" "$BASE_URL/api/accessibility/settings" 2>/dev/null)
invalid_status=$(echo "$invalid_response" | tail -n1)
if [ "$invalid_status" = "403" ]; then
    echo "   ✅ PASS: $invalid_status"
else
    echo "   ❌ FAIL: $invalid_status (예상: 403)"
fi
echo ""

echo "=== 4. 필수 파라미터 누락 테스트 ==="
test_edge_case "GET" "/api/accessibility/screen-reader-hint" "400" "필수 파라미터 누락"
test_edge_case "GET" "/api/accessibility/screen-reader-hint?action=" "400" "빈 파라미터 값"

echo "=== 5. Content-Type 헤더 테스트 ==="
echo "🔍 POST /api/accessibility/voice-guidance without Content-Type"
echo "   시나리오: JSON 엔드포인트에 Content-Type 헤더 없음"
content_response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/accessibility/voice-guidance" -d '{}' 2>/dev/null)
content_status=$(echo "$content_response" | tail -n1)
if [ "$content_status" = "403" ] || [ "$content_status" = "415" ]; then
    echo "   ✅ PASS: $content_status (403=인증필요 또는 415=잘못된타입)"
else
    echo "   ❌ FAIL: $content_status"
fi
echo ""

echo "=== 6. 대소문자 구분 테스트 ==="
test_edge_case "GET" "/API/HEALTH" "404" "대문자 경로"
test_edge_case "GET" "/api/HEALTH" "404" "부분 대문자 경로"

echo "=== 7. 특수 문자 경로 테스트 ==="
test_edge_case "GET" "/api/health/../admin" "404" "경로 트래버설 시도"
test_edge_case "GET" "/api/health%2F" "404" "URL 인코딩된 경로"

echo "=== 테스트 완료 ==="
echo ""
echo "예상 결과:"
echo "- 404: 존재하지 않는 경로"
echo "- 405: 지원하지 않는 HTTP 메서드"
echo "- 403: 인증 실패"
echo "- 400: 잘못된 요청"
echo "- 415: 지원하지 않는 미디어 타입"
