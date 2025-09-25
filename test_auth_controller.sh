#!/bin/bash

# AuthController 테스트 스크립트
# 인증 관련 모든 엔드포인트의 성공/실패/엣지케이스 검증

set -e

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
    ((FAILED_TESTS++))
}

make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth_header=$4
    local expected_status=$5
    local test_name=$6

    ((TOTAL_TESTS++))

    log "Testing: $test_name"

    local curl_cmd="curl -s -w '%{http_code}' -X $method"

    if [[ -n "$auth_header" ]]; then
        curl_cmd="$curl_cmd -H 'Authorization: $auth_header'"
    fi

    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    local response
    response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"

    log "Request: $method $endpoint"
    log "Response Status: $status_code"
    log "Response Body: $body"

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$test_name - Status: $status_code"
    else
        log_error "$test_name - Expected: $expected_status, Got: $status_code"
    fi

    echo "----------------------------------------"
    sleep 1

    # 로그인 성공 시 토큰 추출
    if [[ "$endpoint" == "/api/v1/auth/login" && "$status_code" == "200" ]]; then
        if echo "$body" | grep -q "accessToken"; then
            ACCESS_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
            REFRESH_TOKEN=$(echo "$body" | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
            log "토큰 추출 성공: ACCESS_TOKEN=${ACCESS_TOKEN:0:20}..."
        fi
    fi
}

# 전역 변수
ACCESS_TOKEN=""
REFRESH_TOKEN=""
TEST_USERNAME="testuser_$(date +%s)"
TEST_EMAIL="test_$(date +%s)@example.com"

test_auth_health() {
    log "========== Auth Health Check 테스트 =========="

    make_request "GET" "/api/v1/auth/health" "" "" "200" "인증 서비스 상태 확인"
}

test_register() {
    log "========== 회원가입 테스트 =========="

    # 성공 케이스
    local register_data="{
        \"username\": \"$TEST_USERNAME\",
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"TestPassword123!\",
        \"confirmPassword\": \"TestPassword123!\",
        \"fullName\": \"테스트사용자\",
        \"agreeToTerms\": true,
        \"agreeToPrivacyPolicy\": true,
        \"agreeToMarketing\": false
    }"

    make_request "POST" "/api/v1/auth/register" "$register_data" "" "201" "정상 회원가입"

    # 실패 케이스 - 중복 사용자명
    make_request "POST" "/api/v1/auth/register" "$register_data" "" "400" "중복 사용자명 회원가입"

    # 실패 케이스 - 잘못된 이메일 형식
    local invalid_email_data="{
        \"username\": \"testuser2\",
        \"email\": \"invalid-email-format\",
        \"password\": \"TestPassword123!\",
        \"name\": \"테스트사용자\"
    }"

    make_request "POST" "/api/v1/auth/register" "$invalid_email_data" "" "400" "잘못된 이메일 형식"

    # 실패 케이스 - 약한 비밀번호
    local weak_password_data="{
        \"username\": \"testuser3\",
        \"email\": \"test3@example.com\",
        \"password\": \"123\",
        \"name\": \"테스트사용자\"
    }"

    make_request "POST" "/api/v1/auth/register" "$weak_password_data" "" "400" "약한 비밀번호"

    # 실패 케이스 - 필수 필드 누락
    local missing_field_data="{
        \"username\": \"testuser4\",
        \"password\": \"TestPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/register" "$missing_field_data" "" "400" "필수 필드 누락"

    # 실패 케이스 - 빈 데이터
    make_request "POST" "/api/v1/auth/register" "{}" "" "400" "빈 회원가입 데이터"

    # 실패 케이스 - 잘못된 JSON 형식
    make_request "POST" "/api/v1/auth/register" "invalid json" "" "400" "잘못된 JSON 형식"
}

test_login() {
    log "========== 로그인 테스트 =========="

    # 성공 케이스 - 사용자명으로 로그인
    local login_data="{
        \"usernameOrEmail\": \"$TEST_USERNAME\",
        \"password\": \"TestPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/login" "$login_data" "" "200" "사용자명으로 로그인"

    # 성공 케이스 - 이메일로 로그인
    local email_login_data="{
        \"usernameOrEmail\": \"$TEST_EMAIL\",
        \"password\": \"TestPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/login" "$email_login_data" "" "200" "이메일로 로그인"

    # 실패 케이스 - 잘못된 비밀번호
    local wrong_password_data="{
        \"usernameOrEmail\": \"$TEST_USERNAME\",
        \"password\": \"WrongPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/login" "$wrong_password_data" "" "401" "잘못된 비밀번호"

    # 실패 케이스 - 존재하지 않는 사용자
    local nonexistent_user_data="{
        \"usernameOrEmail\": \"nonexistent_user_999\",
        \"password\": \"TestPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/login" "$nonexistent_user_data" "" "401" "존재하지 않는 사용자"

    # 실패 케이스 - 필수 필드 누락
    local missing_password_data="{
        \"usernameOrEmail\": \"$TEST_USERNAME\"
    }"

    make_request "POST" "/api/v1/auth/login" "$missing_password_data" "" "400" "비밀번호 누락"

    # 실패 케이스 - 빈 데이터
    make_request "POST" "/api/v1/auth/login" "{}" "" "400" "빈 로그인 데이터"
}

test_refresh_token() {
    log "========== 토큰 갱신 테스트 =========="

    if [[ -z "$REFRESH_TOKEN" ]]; then
        log "REFRESH_TOKEN이 없어 토큰 갱신 테스트를 건너뜁니다"
        return
    fi

    # 성공 케이스
    local refresh_data="{
        \"refreshToken\": \"$REFRESH_TOKEN\"
    }"

    make_request "POST" "/api/v1/auth/refresh" "$refresh_data" "" "200" "정상 토큰 갱신"

    # 실패 케이스 - 잘못된 리프레시 토큰
    local invalid_refresh_data="{
        \"refreshToken\": \"invalid_refresh_token_here\"
    }"

    make_request "POST" "/api/v1/auth/refresh" "$invalid_refresh_data" "" "401" "잘못된 리프레시 토큰"

    # 실패 케이스 - 빈 리프레시 토큰
    local empty_refresh_data="{
        \"refreshToken\": \"\"
    }"

    make_request "POST" "/api/v1/auth/refresh" "$empty_refresh_data" "" "400" "빈 리프레시 토큰"

    # 실패 케이스 - 필드 누락
    make_request "POST" "/api/v1/auth/refresh" "{}" "" "400" "리프레시 토큰 필드 누락"
}

test_logout() {
    log "========== 로그아웃 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 로그아웃 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스
    make_request "POST" "/api/v1/auth/logout" "" "$auth_header" "200" "정상 로그아웃"

    # 실패 케이스 - 인증 헤더 없음
    make_request "POST" "/api/v1/auth/logout" "" "" "401" "인증 헤더 없이 로그아웃"

    # 실패 케이스 - 잘못된 토큰
    local invalid_auth_header="Bearer invalid_token_here"
    make_request "POST" "/api/v1/auth/logout" "" "$invalid_auth_header" "401" "잘못된 토큰으로 로그아웃"
}

test_edge_cases() {
    log "========== 엣지 케이스 테스트 =========="

    # SQL Injection 시도
    local sql_injection_data="{
        \"usernameOrEmail\": \"'; DROP TABLE users; --\",
        \"password\": \"TestPassword123!\"
    }"

    make_request "POST" "/api/v1/auth/login" "$sql_injection_data" "" "401" "SQL Injection 시도"

    # XSS 시도
    local xss_data="{
        \"username\": \"<script>alert('xss')</script>\",
        \"email\": \"xss@example.com\",
        \"password\": \"TestPassword123!\",
        \"name\": \"<script>alert('xss')</script>\"
    }"

    make_request "POST" "/api/v1/auth/register" "$xss_data" "" "400" "XSS 시도"

    # 매우 긴 문자열
    local long_string=""
    for i in {1..1000}; do
        long_string+="a"
    done

    local long_data="{
        \"username\": \"$long_string\",
        \"email\": \"long@example.com\",
        \"password\": \"TestPassword123!\",
        \"name\": \"테스트사용자\"
    }"

    make_request "POST" "/api/v1/auth/register" "$long_data" "" "400" "매우 긴 사용자명"

    # 특수 문자 테스트
    local special_char_data="{
        \"username\": \"test@#$%^&*()\",
        \"email\": \"special@example.com\",
        \"password\": \"TestPassword123!\",
        \"name\": \"테스트사용자\"
    }"

    make_request "POST" "/api/v1/auth/register" "$special_char_data" "" "400" "특수 문자 사용자명"
}

print_summary() {
    echo ""
    echo "=========================================="
    echo "AuthController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        echo "성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
    fi
    echo "=========================================="
}

main() {
    log "========== AuthController 테스트 시작 =========="

    # 서버 상태 확인
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        log_error "서버가 실행되지 않았습니다. 서버를 먼저 실행해주세요."
        exit 1
    fi

    test_auth_health
    test_register
    test_login
    test_refresh_token
    test_logout
    test_edge_cases

    print_summary
}

main "$@"