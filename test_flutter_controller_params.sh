#!/bin/bash

# Flutter Controller Parameter Validation Test
# 플러터에서 각 컨트롤러별 엔드포인트로 보내는 파라미터 검증

echo "🔍 Flutter Controller Parameter Validation Test"
echo "=============================================="

BASE_URL="http://localhost:8081/api/v1"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 테스트 결과를 저장할 변수
TOTAL_TESTS=0
PASSED_TESTS=0

# 테스트 함수
test_endpoint() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: $test_name${NC}"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi

    # 응답과 상태 코드 분리
    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    echo "Status: $status_code"
    echo "Response: $body"

    # 상태 코드 확인
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: $expected_status, Got: $status_code)${NC}"
    fi
    echo "---"
}

echo -e "${YELLOW}📱 1. Auth Controller Tests${NC}"
echo "================================"

# 1.1 회원가입 - 정상 케이스
test_endpoint "Register with valid data" "POST" "/auth/register" '{
    "username": "fluttertest001",
    "email": "flutter001@test.com",
    "password": "ValidPass123!",
    "confirmPassword": "ValidPass123!",
    "fullName": "Flutter Test User",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
}' "201"

# 1.2 회원가입 - 잘못된 username (2글자)
test_endpoint "Register with invalid username (too short)" "POST" "/auth/register" '{
    "username": "ab",
    "email": "flutter002@test.com",
    "password": "ValidPass123!",
    "confirmPassword": "ValidPass123!",
    "fullName": "Flutter Test User",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true
}' "400"

# 1.3 회원가입 - 잘못된 이메일 형식
test_endpoint "Register with invalid email format" "POST" "/auth/register" '{
    "username": "fluttertest003",
    "email": "invalid-email-format",
    "password": "ValidPass123!",
    "confirmPassword": "ValidPass123!",
    "fullName": "Flutter Test User",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true
}' "400"

# 1.4 회원가입 - 비밀번호 불일치
test_endpoint "Register with password mismatch" "POST" "/auth/register" '{
    "username": "fluttertest004",
    "email": "flutter004@test.com",
    "password": "ValidPass123!",
    "confirmPassword": "DifferentPass456!",
    "fullName": "Flutter Test User",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true
}' "400"

# 1.5 로그인 - 정상 케이스
test_endpoint "Login with valid credentials" "POST" "/auth/login" '{
    "usernameOrEmail": "fluttertest001",
    "password": "ValidPass123!",
    "rememberMe": false
}' "200"

# 1.6 로그인 - 잘못된 자격증명
test_endpoint "Login with invalid credentials" "POST" "/auth/login" '{
    "usernameOrEmail": "nonexistent@test.com",
    "password": "WrongPassword123!",
    "rememberMe": false
}' "401"

# 1.7 OAuth2 로그인 URL 조회
test_endpoint "Get OAuth2 login URLs" "GET" "/auth/oauth2/login-urls" "" "200"

echo -e "${YELLOW}🏥 2. Health Controller Tests${NC}"
echo "================================"

# 2.1 헬스 체크
test_endpoint "Health check" "GET" "/health" "" "200"

echo -e "${YELLOW}👤 3. User Controller Tests${NC}"
echo "================================"

# 사용자 정보 조회를 위한 토큰 얻기 (임시)
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "usernameOrEmail": "fluttertest001",
        "password": "ValidPass123!",
        "rememberMe": false
    }')

# 토큰 추출 (jq가 있다면 사용, 없으면 간단한 grep 사용)
if command -v jq &> /dev/null; then
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken // empty')
else
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi

if [ -n "$ACCESS_TOKEN" ]; then
    echo "📝 Obtained access token for authenticated tests"

    # 3.1 내 정보 조회
    response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$BASE_URL/users/me")

    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: Get my user info${NC}"
    echo "Status: $status_code"
    echo "Response: $body"

    if [ "$status_code" = "200" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: 200, Got: $status_code)${NC}"
    fi
    echo "---"
else
    echo -e "${RED}❌ Failed to obtain access token for authenticated tests${NC}"
fi

# 3.2 인증 없이 사용자 정보 조회 시도
test_endpoint "Get user info without auth" "GET" "/users/me" "" "401"

echo -e "${YELLOW}🚨 4. Emergency Controller Tests${NC}"
echo "================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 4.1 긴급 상황 신고
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "location": {
                "latitude": 37.5665,
                "longitude": 126.9780,
                "address": "서울시 중구 명동"
            },
            "emergencyType": "MEDICAL",
            "description": "Flutter 테스트 긴급상황",
            "severity": "HIGH"
        }' \
        "$BASE_URL/emergency/report")

    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: Report emergency${NC}"
    echo "Status: $status_code"
    echo "Response: $body"

    if [ "$status_code" = "200" ] || [ "$status_code" = "201" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: 200/201, Got: $status_code)${NC}"
    fi
    echo "---"
fi

# 4.2 인증 없이 긴급상황 신고 시도
test_endpoint "Report emergency without auth" "POST" "/emergency/report" '{
    "location": {
        "latitude": 37.5665,
        "longitude": 126.9780
    },
    "emergencyType": "MEDICAL",
    "description": "Unauthorized test"
}' "401"

echo -e "${YELLOW}📊 5. Statistics Controller Tests${NC}"
echo "================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 5.1 통계 조회
    response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$BASE_URL/statistics/summary")

    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: Get statistics summary${NC}"
    echo "Status: $status_code"
    echo "Response: $body"

    if [ "$status_code" = "200" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: 200, Got: $status_code)${NC}"
    fi
    echo "---"
fi

echo -e "${YELLOW}🔔 6. Notification Controller Tests${NC}"
echo "================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 6.1 알림 설정 조회
    response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$BASE_URL/notifications/settings")

    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: Get notification settings${NC}"
    echo "Status: $status_code"
    echo "Response: $body"

    if [ "$status_code" = "200" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: 200, Got: $status_code)${NC}"
    fi
    echo "---"
fi

echo -e "${YELLOW}🔐 7. Guardian Controller Tests${NC}"
echo "================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 7.1 보호자 목록 조회
    response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$BASE_URL/guardians")

    body=$(echo "$response" | head -n -1)
    status_code=$(echo "$response" | tail -n 1)

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: Get guardians list${NC}"
    echo "Status: $status_code"
    echo "Response: $body"

    if [ "$status_code" = "200" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: 200, Got: $status_code)${NC}"
    fi
    echo "---"
fi

echo ""
echo -e "${YELLOW}📋 Test Summary${NC}"
echo "=============="
echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
else
    echo -e "${YELLOW}⚠️  Some tests failed. Check the details above.${NC}"
fi

echo ""
echo -e "${BLUE}📝 Flutter Parameter Validation Notes:${NC}"
echo "1. Username: 3-50자, 영문/숫자/밑줄만 허용"
echo "2. Email: 유효한 이메일 형식 필요"
echo "3. Password: 최소 8자, 대소문자/숫자/특수문자 포함"
echo "4. 모든 인증이 필요한 엔드포인트는 Bearer 토큰 필요"
echo "5. 긴급상황 신고시 location 객체 필수"
echo "6. 회원가입시 agreeToTerms, agreeToPrivacyPolicy 필수"