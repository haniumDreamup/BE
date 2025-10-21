#!/bin/bash

echo "╔════════════════════════════════════════════════════════════╗"
echo "║   프론트엔드-백엔드 통합 테스트                                ║"
echo "║   CORS, 인증, 에러 처리, 엣지 케이스 검증                       ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

BASE_URL="http://localhost:8080"

TOTAL=0
PASSED=0

test_api() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    local origin="${6:-http://localhost:3004}"
    
    TOTAL=$((TOTAL + 1))
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Origin: $origin" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Origin: $origin" \
            -H "Accept: application/json" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$ d')
    
    # CORS 헤더 확인
    cors_check=$(curl -s -I -X OPTIONS "$BASE_URL$endpoint" \
        -H "Origin: $origin" \
        -H "Access-Control-Request-Method: $method" | grep -i "access-control-allow-origin")
    
    if [ "$http_code" = "$expected_status" ]; then
        if [ -n "$cors_check" ]; then
            echo "✅ [$TOTAL] $name - HTTP $http_code (CORS ✓)"
            PASSED=$((PASSED + 1))
        else
            echo "⚠️  [$TOTAL] $name - HTTP $http_code (CORS ✗)"
        fi
    else
        echo "❌ [$TOTAL] $name - 예상: $expected_status, 실제: $http_code"
    fi
}

echo "📋 1. CORS 검증"
test_api "CORS - Health API" "GET" "/api/v1/health" "" "401" "http://localhost:3004"
test_api "CORS - Different Port" "GET" "/api/v1/health" "" "401" "http://localhost:3005"
test_api "CORS - Port 3006" "GET" "/api/v1/health" "" "401" "http://localhost:3006"

echo ""
echo "🔐 2. 인증 엔드포인트"
test_api "OAuth2 Login URLs" "GET" "/api/v1/auth/oauth2/login-urls" "" "200"
test_api "Login - Empty Data" "POST" "/api/v1/auth/login" '{}' "400"
test_api "Register - Invalid" "POST" "/api/v1/auth/register" '{"username":""}' "400"

echo ""
echo "🚨 3. 에러 처리 검증"
test_api "Protected - No Auth" "GET" "/api/v1/users/me" "" "401"
test_api "Emergency - No Auth" "POST" "/api/v1/emergency/alert" '{}' "401"
test_api "Not Found" "GET" "/api/v1/nonexistent" "" "404"

echo ""
echo "⚡ 4. 엣지 케이스"
test_api "Invalid JSON" "POST" "/api/v1/auth/login" 'invalid json' "400"
test_api "Large Payload" "POST" "/api/v1/auth/register" "$(python3 -c 'print("{\"data\":\"" + "A"*10000 + "\"}")')" "400"
test_api "Special Characters" "GET" "/api/v1/test/@#$" "" "404"

echo ""
echo "📱 5. Flutter 엔드포인트 호환성"
test_api "Emergency Alert" "POST" "/api/v1/emergency/alert" '{"type":"FALL","location":{"latitude":37.5,"longitude":127.0}}' "401"
test_api "Accessibility" "GET" "/api/v1/accessibility/settings" "" "200"
test_api "Pose Data" "POST" "/api/v1/pose/data" '{}' "401"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                        테스트 결과                            ║"
echo "║ 총 테스트: $TOTAL"
echo "║ 성공: $PASSED"
echo "║ 성공률: $((PASSED * 100 / TOTAL))%"
echo "╚════════════════════════════════════════════════════════════╝"

if [ $PASSED -eq $TOTAL ]; then
    echo "🎉 모든 테스트 통과! 프론트엔드-백엔드 통합 OK"
    exit 0
else
    echo "⚠️  일부 테스트 실패"
    exit 1
fi
