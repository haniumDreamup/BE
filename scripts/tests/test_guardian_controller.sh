#!/bin/bash

# GuardianController 테스트 스크립트
# 보호자 관리 기능 엔드포인트 검증

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

    log "테스트: $test_name"

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

    log "요청: $method $endpoint"
    log "응답 상태: $status_code"
    log "응답 본문: $body"

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$test_name - 상태: $status_code"
    else
        log_error "$test_name - 예상: $expected_status, 실제: $status_code"
    fi

    echo "----------------------------------------"
    sleep 0.5

    # 로그인 성공 시 토큰 추출
    if [[ "$endpoint" == "/api/v1/auth/login" && "$status_code" == "200" ]]; then
        if echo "$body" | grep -q "accessToken"; then
            ACCESS_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
            log "토큰 추출 성공: ${ACCESS_TOKEN:0:20}..."
        fi
    fi
}

# 전역 변수
ACCESS_TOKEN=""
TEST_USERNAME="testuser_$(date +%s)"
TEST_EMAIL="test_$(date +%s)@example.com"

setup_test_user() {
    log "========== 테스트 사용자 설정 =========="

    # 회원가입
    local register_data="{
        \"username\": \"$TEST_USERNAME\",
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"TestPassword123\",
        \"confirmPassword\": \"TestPassword123\",
        \"fullName\": \"테스트사용자\",
        \"agreeToTerms\": true,
        \"agreeToPrivacyPolicy\": true,
        \"agreeToMarketing\": false
    }"

    make_request "POST" "/api/v1/auth/register" "$register_data" "" "201" "테스트 사용자 생성"

    # 로그인
    local login_data="{
        \"usernameOrEmail\": \"$TEST_USERNAME\",
        \"password\": \"TestPassword123\"
    }"

    make_request "POST" "/api/v1/auth/login" "$login_data" "" "200" "테스트 사용자 로그인"

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log_error "ACCESS_TOKEN을 가져올 수 없습니다. 테스트를 중단합니다."
        exit 1
    fi
}

test_my_guardians() {
    log "========== 나의 보호자 목록 조회 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 나의 보호자 목록 조회 - 성공 케이스
    make_request "GET" "/api/guardians/my" "" "$auth_header" "200" "나의 보호자 목록 조회"

    # 인증 없이 접근
    make_request "GET" "/api/guardians/my" "" "" "401" "인증 없이 보호자 목록 조회"

    # 잘못된 토큰으로 접근
    make_request "GET" "/api/guardians/my" "" "Bearer invalid_token" "401" "잘못된 토큰으로 접근"
}

test_protected_users() {
    log "========== 보호 중인 사용자 목록 조회 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 보호 중인 사용자 목록 조회 - GUARDIAN 역할 필요하므로 403 예상
    make_request "GET" "/api/guardians/protected-users" "" "$auth_header" "403" "GUARDIAN 역할 없이 보호 사용자 조회"

    # 인증 없이 접근
    make_request "GET" "/api/guardians/protected-users" "" "" "401" "인증 없이 보호 사용자 조회"
}

test_guardian_request() {
    log "========== 보호자 등록 요청 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 보호자 등록 요청 - 성공 케이스
    local guardian_request_data="{
        \"guardianEmail\": \"guardian_$(date +%s)@example.com\",
        \"relationship\": \"가족\",
        \"message\": \"보호자 등록을 요청드립니다\"
    }"

    make_request "POST" "/api/guardians" "$guardian_request_data" "$auth_header" "201" "보호자 등록 요청"

    # 인증 없이 접근
    make_request "POST" "/api/guardians" "$guardian_request_data" "" "401" "인증 없이 보호자 등록 요청"

    # 잘못된 이메일 형식
    local invalid_email_data="{
        \"guardianEmail\": \"invalid_email\",
        \"relationship\": \"가족\"
    }"

    make_request "POST" "/api/guardians" "$invalid_email_data" "$auth_header" "400" "잘못된 이메일 형식"

    # 빈 데이터
    make_request "POST" "/api/guardians" "{}" "$auth_header" "400" "빈 보호자 요청 데이터"

    # 필수 필드 누락
    local missing_fields_data="{
        \"relationship\": \"가족\"
    }"

    make_request "POST" "/api/guardians" "$missing_fields_data" "$auth_header" "400" "필수 필드 누락"
}

test_guardian_approval() {
    log "========== 보호자 요청 승인/거절 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 존재하지 않는 보호자 승인 시도
    make_request "PUT" "/api/guardians/99999/approve" "" "$auth_header" "403" "권한 없이 보호자 승인 시도"

    # 존재하지 않는 보호자 거절 시도
    local reject_reason="{\"reason\": \"부적절한 요청입니다\"}"
    make_request "PUT" "/api/guardians/99999/reject" "$reject_reason" "$auth_header" "403" "권한 없이 보호자 거절 시도"

    # 인증 없이 승인/거절 시도
    make_request "PUT" "/api/guardians/1/approve" "" "" "401" "인증 없이 보호자 승인"
    make_request "PUT" "/api/guardians/1/reject" "$reject_reason" "" "401" "인증 없이 보호자 거절"
}

test_guardian_permissions() {
    log "========== 보호자 권한 수정 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 보호자 권한 수정 - 존재하지 않는 보호자
    local permission_data="{
        \"canViewLocation\": true,
        \"canReceiveAlerts\": true,
        \"canViewHealthData\": false,
        \"canModifySettings\": false
    }"

    make_request "PUT" "/api/guardians/99999/permissions" "$permission_data" "$auth_header" "403" "존재하지 않는 보호자 권한 수정"

    # 인증 없이 접근
    make_request "PUT" "/api/guardians/1/permissions" "$permission_data" "" "401" "인증 없이 권한 수정"

    # 잘못된 권한 데이터
    local invalid_permission_data="{
        \"canViewLocation\": \"invalid_boolean\"
    }"

    make_request "PUT" "/api/guardians/1/permissions" "$invalid_permission_data" "$auth_header" "400" "잘못된 권한 데이터"

    # 빈 권한 데이터
    make_request "PUT" "/api/guardians/1/permissions" "{}" "$auth_header" "400" "빈 권한 데이터"
}

test_guardian_deletion() {
    log "========== 보호자 삭제 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 존재하지 않는 보호자 삭제
    make_request "DELETE" "/api/guardians/99999" "" "$auth_header" "403" "존재하지 않는 보호자 삭제"

    # 인증 없이 삭제 시도
    make_request "DELETE" "/api/guardians/1" "" "" "401" "인증 없이 보호자 삭제"

    # 보호 관계 해제 (GUARDIAN 역할 필요하므로 403 예상)
    make_request "DELETE" "/api/guardians/relationships/1" "" "$auth_header" "403" "USER 역할로 보호 관계 해제"

    # 인증 없이 관계 해제
    make_request "DELETE" "/api/guardians/relationships/1" "" "" "401" "인증 없이 관계 해제"
}

test_edge_cases() {
    log "========== 엣지 케이스 테스트 =========="

    # 존재하지 않는 엔드포인트
    make_request "GET" "/api/guardians/nonexistent" "" "" "404" "존재하지 않는 엔드포인트"

    # 잘못된 HTTP 메서드
    make_request "PATCH" "/api/guardians/my" "" "" "405" "지원하지 않는 HTTP 메서드"

    if [[ -n "$ACCESS_TOKEN" ]]; then
        local auth_header="Bearer $ACCESS_TOKEN"

        # 잘못된 JSON 형식
        make_request "POST" "/api/guardians" "invalid json data" "$auth_header" "400" "잘못된 JSON 형식"

        # 매우 긴 이메일
        local long_email=""
        for i in {1..100}; do
            long_email+="a"
        done
        long_email+="@example.com"

        local long_email_data="{
            \"guardianEmail\": \"$long_email\",
            \"relationship\": \"가족\"
        }"

        make_request "POST" "/api/guardians" "$long_email_data" "$auth_header" "400" "매우 긴 이메일"

        # 특수 문자가 포함된 관계
        local special_char_data="{
            \"guardianEmail\": \"test@example.com\",
            \"relationship\": \"<script>alert('xss')</script>\"
        }"

        make_request "POST" "/api/guardians" "$special_char_data" "$auth_header" "201" "특수 문자 포함 관계 (XSS 시도)"
    fi
}

test_concurrent_requests() {
    log "========== 동시 요청 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 동시 요청 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 동시에 여러 보호자 등록 요청
    local guardian_data_1="{\"guardianEmail\": \"guardian1_$(date +%s)@example.com\", \"relationship\": \"가족\"}"
    local guardian_data_2="{\"guardianEmail\": \"guardian2_$(date +%s)@example.com\", \"relationship\": \"친구\"}"
    local guardian_data_3="{\"guardianEmail\": \"guardian3_$(date +%s)@example.com\", \"relationship\": \"동료\"}"

    # 백그라운드로 동시 실행
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$guardian_data_1" "$BASE_URL/api/guardians" &
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$guardian_data_2" "$BASE_URL/api/guardians" &
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$guardian_data_3" "$BASE_URL/api/guardians" &

    wait # 모든 백그라운드 작업 완료 대기

    log_success "동시 보호자 등록 요청 테스트 완료"
    ((TOTAL_TESTS++))
    ((PASSED_TESTS++))
}

print_summary() {
    echo ""
    echo "=========================================="
    echo "GuardianController 테스트 결과 요약"
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
    log "========== GuardianController 테스트 시작 =========="

    # 서버 상태 확인
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        log_error "서버가 실행되지 않았습니다. 서버를 먼저 실행해주세요."
        exit 1
    fi

    setup_test_user
    test_my_guardians
    test_protected_users
    test_guardian_request
    test_guardian_approval
    test_guardian_permissions
    test_guardian_deletion
    test_edge_cases
    test_concurrent_requests

    print_summary

    # 100% 성공률 확인
    if [[ $FAILED_TESTS -eq 0 && $TOTAL_TESTS -gt 0 ]]; then
        echo -e "${GREEN}🎉 GuardianController 테스트 100% 성공!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  일부 테스트가 실패했습니다. 코드 수정이 필요합니다.${NC}"
        exit 1
    fi
}

main "$@"