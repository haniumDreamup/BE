#!/bin/bash

# 프론트엔드-백엔드 API 통합 테스트
# 실제 JWT 인증으로 프론트엔드가 사용하는 모든 API 테스트

set -euo pipefail
BASE_URL="http://43.200.49.171:8080"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILED_TESTS=0

log_test() {
    local name="$1"
    local expected="$2"
    local actual="$3"
    local body="${4:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$expected" = "$actual" ]; then
        echo -e "✅ ${GREEN}$name${NC}: HTTP $actual"
        SUCCESS_TESTS=$((SUCCESS_TESTS + 1))
    else
        echo -e "❌ ${RED}$name${NC}: 예상 $expected, 실제 $actual"
        if [ -n "$body" ]; then
            echo -e "   ${YELLOW}응답${NC}: $(echo $body | head -c 300)"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║           프론트엔드-백엔드 API 통합 테스트 (JWT 인증)            ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

# 1. 사용자 등록 및 로그인
echo -e "${BLUE}🔐 1. 사용자 인증${NC}"

RANDOM_ID=$RANDOM
TEST_USER="integration_test_${RANDOM_ID}@test.com"
TEST_PASSWORD="Test1234!"

register_response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\",\"confirmPassword\":\"$TEST_PASSWORD\",\"username\":\"integtest$RANDOM_ID\",\"fullName\":\"통합테스트\",\"agreeToTerms\":true,\"agreeToPrivacyPolicy\":true}")

register_code=$(echo "$register_response" | tail -n1)
if [ "$register_code" != "201" ] && [ "$register_code" != "200" ]; then
    echo -e "${RED}사용자 등록 실패: $register_code${NC}"
    exit 1
fi
echo -e "✅ ${GREEN}사용자 등록 성공${NC}"

login_response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usernameOrEmail\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\"}")

login_code=$(echo "$login_response" | tail -n1)
login_body=$(echo "$login_response" | sed '$ d')

if [ "$login_code" != "200" ]; then
    echo -e "${RED}로그인 실패: $login_code${NC}"
    exit 1
fi

TOKEN=$(echo "$login_body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
    echo -e "${RED}토큰 추출 실패${NC}"
    exit 1
fi
echo -e "✅ ${GREEN}로그인 성공 - JWT 토큰 획득${NC}"
echo

# 2. UserService API 테스트
echo -e "${BLUE}👤 2. UserService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/v1/users/me - 내 정보 조회" "200" "$code" "$body"

# 3. EmergencyContactService API 테스트
echo -e "\n${BLUE}📞 3. EmergencyContactService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/emergency-contacts" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"119 소방서","phoneNumber":"119","relationship":"공공기관","priority":1}')
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "POST /api/emergency-contacts - 긴급 연락처 생성" "201" "$code" "$body"

CONTACT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/emergency-contacts - 연락처 목록" "200" "$code" "$body"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts/active" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/emergency-contacts/active - 활성 연락처" "200" "$code" "$body"

# 4. GuardianService API 테스트
echo -e "\n${BLUE}👨‍👩‍👧 4. GuardianService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/guardians/my" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/guardians/my - 내 보호자 목록" "200" "$code" "$body"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/guardians/protected-users" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/guardians/protected-users - 보호 대상자" "200" "$code" "$body"

# 5. StatisticsService API 테스트
echo -e "\n${BLUE}📊 5. StatisticsService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/statistics/safety" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/statistics/safety - 안전 통계" "200" "$code" "$body"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/statistics/daily-activity" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/statistics/daily-activity - 일일 활동" "200" "$code" "$body"

# 6. AccessibilityService API 테스트
echo -e "\n${BLUE}♿ 6. AccessibilityService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/v1/accessibility/settings" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/v1/accessibility/settings - 접근성 설정" "200" "$code" "$body"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/v1/accessibility/color-schemes" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/v1/accessibility/color-schemes - 색상 구성표" "200" "$code" "$body"

# 7. NotificationService API 테스트
echo -e "\n${BLUE}🔔 7. NotificationService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/notifications/settings" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/notifications/settings - 알림 설정" "200" "$code" "$body"

# 8. GeofenceService API 테스트
echo -e "\n${BLUE}🗺️  8. GeofenceService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/geofences" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/geofences - 지오펜스 목록" "200" "$code" "$body"

# 9. EmergencyService API 테스트
echo -e "\n${BLUE}🚨 9. EmergencyService API${NC}"

response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/v1/emergency/active" \
    -H "Authorization: Bearer $TOKEN")
code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$ d')
log_test "GET /api/v1/emergency/active - 활성 긴급상황" "200" "$code" "$body"

# Cleanup: Delete created contact
if [ -n "$CONTACT_ID" ]; then
    curl -s -X DELETE "$BASE_URL/api/emergency-contacts/$CONTACT_ID" \
        -H "Authorization: Bearer $TOKEN" > /dev/null
fi

# 최종 결과
echo
echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                          테스트 결과 요약                            ║${NC}"
echo -e "${PURPLE}║ 총 테스트: ${TOTAL_TESTS}개${NC}"
echo -e "${PURPLE}║ 성공: ${SUCCESS_TESTS}개${NC}"
echo -e "${PURPLE}║ 실패: ${FAILED_TESTS}개${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=1; $SUCCESS_TESTS * 100 / $TOTAL_TESTS" | bc -l)
    echo -e "${PURPLE}║ 성공률: ${SUCCESS_RATE}%${NC}"
else
    SUCCESS_RATE="0"
    echo -e "${PURPLE}║ 성공률: 0%${NC}"
fi

echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"

if [ "$SUCCESS_RATE" = "100.0" ]; then
    echo -e "${GREEN}🎉 프론트엔드-백엔드 API 통합 테스트 100% 성공!${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 80" | bc -l) )); then
    echo -e "${YELLOW}⚡ API 통합 테스트 ${SUCCESS_RATE}% 성공${NC}"
    exit 0
else
    echo -e "${RED}💥 API 통합 테스트 ${SUCCESS_RATE}% - 개선 필요${NC}"
    exit 1
fi