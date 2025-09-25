#!/bin/bash

# =============================================================================
# BIF-AI Backend 테스트 공통 유틸리티
# =============================================================================
# 모든 컨트롤러 테스트에서 사용하는 공통 함수들
#
# 사용법: source test_utils.sh
# =============================================================================

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 전역 변수
BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULTS=()

# =============================================================================
# 출력 함수들
# =============================================================================

print_header() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

print_test() {
    echo -e "${YELLOW}[TEST] $1${NC}"
    ((TOTAL_TESTS++))
}

print_success() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    ((PASSED_TESTS++))
    TEST_RESULTS+=("PASS: $1")
}

print_failure() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    echo -e "${RED}   Response: $2${NC}"
    ((FAILED_TESTS++))
    TEST_RESULTS+=("FAIL: $1 - $2")
}

print_info() {
    echo -e "${PURPLE}[INFO] $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}[WARN] $1${NC}"
}

# =============================================================================
# HTTP 및 JSON 유틸리티 함수들
# =============================================================================

# JSON 응답 검증 함수
check_response_structure() {
    local response="$1"
    local expected_success="$2"

    if echo "$response" | jq -e . >/dev/null 2>&1; then
        local success=$(echo "$response" | jq -r '.success // empty')
        local timestamp=$(echo "$response" | jq -r '.timestamp // empty')

        if [[ "$success" == "$expected_success" ]] && [[ -n "$timestamp" ]]; then
            return 0
        fi
    fi
    return 1
}

# HTTP 상태 코드 검증
check_status_code() {
    local actual="$1"
    local expected="$2"

    if [[ "$actual" == "$expected" ]]; then
        return 0
    fi
    return 1
}

# API 호출 함수 (공통)
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local headers="$4"

    local curl_cmd="curl -s -w 'STATUS_CODE:%{http_code}' -X $method"

    if [[ -n "$headers" ]]; then
        curl_cmd="$curl_cmd $headers"
    fi

    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data' -H 'Content-Type: application/json'"
    fi

    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"

    eval $curl_cmd
}

# 토큰 추출 함수
extract_token() {
    local response="$1"
    echo "$response" | jq -r '.data.accessToken // empty'
}

# HTTP 응답에서 상태 코드와 바디 분리
parse_response() {
    local response="$1"
    local status_code=$(echo "$response" | grep -o 'STATUS_CODE:[0-9]*' | cut -d: -f2)
    local body=$(echo "$response" | sed 's/STATUS_CODE:[0-9]*$//')

    echo "$status_code|$body"
}

# =============================================================================
# 서버 상태 확인
# =============================================================================

check_server_status() {
    print_info "서버 상태 확인 중..."

    local response=$(api_call "GET" "/health" "" "")
    local parsed=$(parse_response "$response")
    local status_code=$(echo "$parsed" | cut -d'|' -f1)
    local body=$(echo "$parsed" | cut -d'|' -f2)

    if check_status_code "$status_code" "200"; then
        print_info "서버가 정상 작동 중입니다"
        return 0
    else
        print_failure "서버 접속 실패" "Status: $status_code"
        return 1
    fi
}

# =============================================================================
# 인증 관련 유틸리티
# =============================================================================

# 테스트용 사용자 생성
create_test_user() {
    local prefix="$1"
    local timestamp=$(date +%s)

    local register_data='{
        "username": "'$prefix'user'$timestamp'",
        "email": "'$prefix'user'$timestamp'@example.com",
        "password": "TestPassword123!",
        "name": "'$prefix'테스트사용자'$timestamp'",
        "phoneNumber": "010-1234-5678"
    }'

    local response=$(api_call "POST" "/api/v1/auth/register" "$register_data" "")
    local parsed=$(parse_response "$response")
    local status_code=$(echo "$parsed" | cut -d'|' -f1)
    local body=$(echo "$parsed" | cut -d'|' -f2)

    if check_status_code "$status_code" "201"; then
        local token=$(extract_token "$body")
        echo "$token"
        return 0
    else
        echo ""
        return 1
    fi
}

# =============================================================================
# 테스트 결과 출력
# =============================================================================

print_test_summary() {
    local controller_name="$1"

    echo
    print_header "$controller_name 테스트 결과"
    echo -e "${BLUE}총 테스트: $TOTAL_TESTS${NC}"
    echo -e "${GREEN}성공: $PASSED_TESTS${NC}"
    echo -e "${RED}실패: $FAILED_TESTS${NC}"

    if [[ $FAILED_TESTS -eq 0 ]]; then
        echo -e "${GREEN}🎉 모든 테스트가 성공했습니다!${NC}"
    else
        echo -e "${RED}⚠️  일부 테스트가 실패했습니다.${NC}"
        echo
        echo -e "${YELLOW}실패한 테스트 목록:${NC}"
        for result in "${TEST_RESULTS[@]}"; do
            if [[ $result == FAIL* ]]; then
                echo -e "${RED}- $result${NC}"
            fi
        done
    fi

    # 성공률 계산
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc 2>/dev/null || echo "0")
        echo -e "${PURPLE}테스트 성공률: $success_rate%${NC}"
    fi

    echo
}

# 로그 파일 생성
save_test_results() {
    local controller_name="$1"
    local log_file="${controller_name}_test_$(date +%Y%m%d_%H%M%S).log"

    {
        echo "BIF-AI Backend $controller_name Test Results"
        echo "Generated at: $(date)"
        echo "Total Tests: $TOTAL_TESTS"
        echo "Passed: $PASSED_TESTS"
        echo "Failed: $FAILED_TESTS"
        if [[ $TOTAL_TESTS -gt 0 ]]; then
            local success_rate=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc 2>/dev/null || echo "0")
            echo "Success Rate: $success_rate%"
        fi
        echo
        echo "Test Results:"
        for result in "${TEST_RESULTS[@]}"; do
            echo "- $result"
        done
    } > "$log_file"

    echo -e "${BLUE}테스트 결과가 $log_file 파일에 저장되었습니다.${NC}"
}

# =============================================================================
# 초기화
# =============================================================================

# jq 설치 확인
check_dependencies() {
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}jq가 설치되어 있지 않습니다. 설치 후 다시 실행해주세요.${NC}"
        echo "macOS: brew install jq"
        echo "Ubuntu: sudo apt-get install jq"
        return 1
    fi

    if ! command -v bc &> /dev/null; then
        echo -e "${RED}bc가 설치되어 있지 않습니다. 설치 후 다시 실행해주세요.${NC}"
        echo "macOS: brew install bc"
        echo "Ubuntu: sudo apt-get install bc"
        return 1
    fi

    return 0
}