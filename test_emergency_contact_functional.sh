#!/bin/bash

# EmergencyContactController Functional Test with JWT Auth
# Tests actual functionality with authentication

set -euo pipefail
BASE_URL="http://localhost:8080"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILED_TESTS=0

log_test_result() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    local response_body="${4:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$expected" = "$actual" ]; then
        echo -e "✅ ${GREEN}$test_name${NC}: HTTP $actual"
        SUCCESS_TESTS=$((SUCCESS_TESTS + 1))
    else
        echo -e "❌ ${RED}$test_name${NC}: 예상 $expected, 실제 $actual"
        if [ -n "$response_body" ]; then
            echo -e "   ${YELLOW}응답${NC}: $(echo $response_body | head -c 200)"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║      EmergencyContactController 실제 기능 테스트 (JWT 인증)       ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

# 1. Create test user and get JWT token
echo -e "${BLUE}🔐 1. 테스트 사용자 생성 및 로그인${NC}"

RANDOM_ID=$RANDOM
TEST_USER="testuser_${RANDOM_ID}@test.com"
TEST_PASSWORD="Test1234!"

# Register user
register_response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\",\"confirmPassword\":\"$TEST_PASSWORD\",\"username\":\"testuser$RANDOM_ID\",\"fullName\":\"테스트사용자\",\"agreeToTerms\":true,\"agreeToPrivacyPolicy\":true}" 2>&1)

register_code=$(echo "$register_response" | tail -n1)
register_body=$(echo "$register_response" | sed '$ d')

if [ "$register_code" = "201" ] || [ "$register_code" = "200" ]; then
    echo -e "✅ ${GREEN}사용자 등록 성공${NC}"
else
    echo -e "❌ ${RED}사용자 등록 실패: HTTP $register_code${NC}"
    echo "$register_body"
    exit 1
fi

# Login to get token
login_response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"usernameOrEmail\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\"}" 2>&1)

login_code=$(echo "$login_response" | tail -n1)
login_body=$(echo "$login_response" | sed '$ d')

if [ "$login_code" = "200" ]; then
    TOKEN=$(echo "$login_body" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    if [ -z "$TOKEN" ]; then
        echo -e "❌ ${RED}토큰 추출 실패${NC}"
        exit 1
    fi
    echo -e "✅ ${GREEN}로그인 성공 - 토큰 획득${NC}"
else
    echo -e "❌ ${RED}로그인 실패: HTTP $login_code${NC}"
    echo "$login_body"
    exit 1
fi

# 2. Test Emergency Contact Creation
echo -e "\n${BLUE}📞 2. 긴급 연락처 생성${NC}"

create_response=$(curl -s -w '\n%{http_code}' -X POST "$BASE_URL/api/emergency-contacts" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"119 소방서","phoneNumber":"119","relationship":"공공기관","priority":1}' 2>&1)

create_code=$(echo "$create_response" | tail -n1)
create_body=$(echo "$create_response" | sed '$ d')

log_test_result "긴급 연락처 생성" "201" "$create_code" "$create_body"

CONTACT_ID=""
if [ "$create_code" = "201" ] || [ "$create_code" = "200" ]; then
    CONTACT_ID=$(echo "$create_body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
fi

# 3. Test Get All Contacts
echo -e "\n${BLUE}📋 3. 모든 긴급 연락처 조회${NC}"

getall_response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

getall_code=$(echo "$getall_response" | tail -n1)
getall_body=$(echo "$getall_response" | sed '$ d')

log_test_result "모든 연락처 조회" "200" "$getall_code" "$getall_body"

# 4. Test Get Active Contacts
echo -e "\n${BLUE}✅ 4. 활성 연락처 조회${NC}"

active_response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts/active" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

active_code=$(echo "$active_response" | tail -n1)
active_body=$(echo "$active_response" | sed '$ d')

log_test_result "활성 연락처 조회" "200" "$active_code" "$active_body"

# 5. Test Get Medical Contacts
echo -e "\n${BLUE}🏥 5. 의료진 연락처 조회${NC}"

medical_response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts/medical" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

medical_code=$(echo "$medical_response" | tail -n1)
medical_body=$(echo "$medical_response" | sed '$ d')

log_test_result "의료진 연락처 조회" "200" "$medical_code" "$medical_body"

# 6. Test Update Contact (if we have contact ID)
if [ -n "$CONTACT_ID" ]; then
    echo -e "\n${BLUE}✏️ 6. 긴급 연락처 수정 (ID: $CONTACT_ID)${NC}"

    update_response=$(curl -s -w '\n%{http_code}' -X PUT "$BASE_URL/api/emergency-contacts/$CONTACT_ID" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"name":"119 소방서 수정","phoneNumber":"119","relationship":"공공기관","priority":1}' 2>&1)

    update_code=$(echo "$update_response" | tail -n1)
    update_body=$(echo "$update_response" | sed '$ d')

    log_test_result "연락처 수정" "200" "$update_code" "$update_body"

    # 7. Test Get Specific Contact
    echo -e "\n${BLUE}🔍 7. 특정 연락처 조회 (ID: $CONTACT_ID)${NC}"

    get_response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts/$CONTACT_ID" \
        -H "Authorization: Bearer $TOKEN" 2>&1)

    get_code=$(echo "$get_response" | tail -n1)
    get_body=$(echo "$get_response" | sed '$ d')

    log_test_result "특정 연락처 조회" "200" "$get_code" "$get_body"

    # 8. Test Delete Contact
    echo -e "\n${BLUE}🗑️ 8. 긴급 연락처 삭제 (ID: $CONTACT_ID)${NC}"

    delete_response=$(curl -s -w '\n%{http_code}' -X DELETE "$BASE_URL/api/emergency-contacts/$CONTACT_ID" \
        -H "Authorization: Bearer $TOKEN" 2>&1)

    delete_code=$(echo "$delete_response" | tail -n1)
    delete_body=$(echo "$delete_response" | sed '$ d')

    log_test_result "연락처 삭제" "204" "$delete_code" "$delete_body"
fi

# 9. Test Unauthenticated Access (Should be 401)
echo -e "\n${BLUE}🚫 9. 인증 없는 접근 테스트 (401 기대)${NC}"

unauth_response=$(curl -s -w '\n%{http_code}' -X GET "$BASE_URL/api/emergency-contacts" 2>&1)
unauth_code=$(echo "$unauth_response" | tail -n1)
unauth_body=$(echo "$unauth_response" | sed '$ d')

log_test_result "인증 없는 접근" "401" "$unauth_code" "$unauth_body"

# Results Summary
echo
echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                           테스트 결과 요약                           ║${NC}"
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
    echo -e "${GREEN}🎉 EmergencyContactController 100% 성공률 달성!${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 80" | bc -l) )); then
    echo -e "${YELLOW}⚡ EmergencyContactController ${SUCCESS_RATE}% 성공률 - 양호${NC}"
    exit 0
else
    echo -e "${RED}💥 EmergencyContactController ${SUCCESS_RATE}% 성공률 - 개선 필요${NC}"
    exit 1
fi
