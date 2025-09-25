#!/bin/bash

# EmergencyController 테스트 스크립트
# 긴급상황 관리 관련 모든 엔드포인트의 성공/실패/엣지케이스 검증

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

    # 긴급상황 생성 성공 시 ID 추출
    if [[ "$endpoint" == "/api/v1/emergency/alert" && "$status_code" == "201" ]]; then
        if echo "$body" | grep -q "emergencyId\|id"; then
            EMERGENCY_ID=$(echo "$body" | grep -o '"emergencyId":[0-9]*\|"id":[0-9]*' | head -1 | cut -d':' -f2)
            log "생성된 긴급상황 ID: $EMERGENCY_ID"
        fi
    fi
}

# 전역 변수
ACCESS_TOKEN=""
USER_ACCESS_TOKEN=""
GUARDIAN_ACCESS_TOKEN=""
ADMIN_ACCESS_TOKEN=""
EMERGENCY_ID=""
TEST_USER_ID=""

# 테스트용 사용자 생성 및 로그인
setup_test_users() {
    log "========== 테스트 사용자 설정 =========="

    local test_username="testuser_$(date +%s)"
    local test_email="test_$(date +%s)@example.com"

    # 일반 사용자 생성
    local register_data="{
        \"username\": \"$test_username\",
        \"email\": \"$test_email\",
        \"password\": \"TestPassword123\",
        \"confirmPassword\": \"TestPassword123\",
        \"fullName\": \"테스트사용자\",
        \"agreeToTerms\": true,
        \"agreeToPrivacyPolicy\": true,
        \"agreeToMarketing\": false
    }"

    curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$register_data" \
        "$BASE_URL/api/v1/auth/register" > /dev/null

    # 로그인하여 토큰 획득
    local login_data="{
        \"usernameOrEmail\": \"$test_username\",
        \"password\": \"TestPassword123\"
    }"

    local login_response
    login_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$login_data" \
        "$BASE_URL/api/v1/auth/login")

    if echo "$login_response" | grep -q "accessToken"; then
        USER_ACCESS_TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        ACCESS_TOKEN="$USER_ACCESS_TOKEN"  # 기본 토큰으로 설정
        log "일반 사용자 토큰 획득 성공: ${USER_ACCESS_TOKEN:0:20}..."

        # 사용자 ID 추출
        if echo "$login_response" | grep -q "userId"; then
            TEST_USER_ID=$(echo "$login_response" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
            log "테스트 사용자 ID: $TEST_USER_ID"
        fi
    else
        log "일반 사용자 토큰 획득 실패"
    fi
}

test_create_emergency_alert() {
    log "========== 긴급상황 신고 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스 - 수동 긴급상황 신고
    local emergency_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"도움이 필요합니다\",
        \"severity\": \"HIGH\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$emergency_data" "$auth_header" "201" "수동 긴급상황 신고"

    # 성공 케이스 - 다른 유형의 긴급상황
    local medical_emergency_data="{
        \"type\": \"MEDICAL_EMERGENCY\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"의료진이 필요합니다\",
        \"severity\": \"CRITICAL\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$medical_emergency_data" "$auth_header" "201" "의료 긴급상황 신고"

    # 실패 케이스 - 인증 없음
    make_request "POST" "/api/v1/emergency/alert" "$emergency_data" "" "401" "인증 없이 긴급상황 신고"

    # 실패 케이스 - 잘못된 타입
    local invalid_type_data="{
        \"type\": \"INVALID_TYPE\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"도움이 필요합니다\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$invalid_type_data" "$auth_header" "400" "잘못된 긴급상황 타입"

    # 실패 케이스 - 잘못된 좌표
    local invalid_coords_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 200,
        \"longitude\": 200,
        \"description\": \"도움이 필요합니다\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$invalid_coords_data" "$auth_header" "400" "잘못된 좌표값"

    # 실패 케이스 - 필수 필드 누락
    local missing_field_data="{
        \"type\": \"MANUAL_ALERT\",
        \"description\": \"도움이 필요합니다\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$missing_field_data" "$auth_header" "400" "필수 필드 누락"

    # 실패 케이스 - 빈 데이터
    make_request "POST" "/api/v1/emergency/alert" "{}" "$auth_header" "400" "빈 긴급상황 데이터"
}

test_fall_detection() {
    log "========== 낙상 감지 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스 - 높은 신뢰도 낙상 감지
    local fall_detection_data="{
        \"confidence\": 0.95,
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"detectionTime\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",
        \"imageUrl\": \"http://example.com/fall-image.jpg\"
    }"

    make_request "POST" "/api/v1/emergency/fall-detection" "$fall_detection_data" "$auth_header" "201" "높은 신뢰도 낙상 감지"

    # 성공 케이스 - 중간 신뢰도 낙상 감지
    local medium_confidence_data="{
        \"confidence\": 0.75,
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"detectionTime\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
    }"

    make_request "POST" "/api/v1/emergency/fall-detection" "$medium_confidence_data" "$auth_header" "201" "중간 신뢰도 낙상 감지"

    # 실패 케이스 - 인증 없음
    make_request "POST" "/api/v1/emergency/fall-detection" "$fall_detection_data" "" "401" "인증 없이 낙상 감지"

    # 실패 케이스 - 잘못된 신뢰도 (범위 초과)
    local invalid_confidence_data="{
        \"confidence\": 1.5,
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"detectionTime\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
    }"

    make_request "POST" "/api/v1/emergency/fall-detection" "$invalid_confidence_data" "$auth_header" "400" "잘못된 신뢰도 값"

    # 실패 케이스 - 음수 신뢰도
    local negative_confidence_data="{
        \"confidence\": -0.1,
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"detectionTime\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\"
    }"

    make_request "POST" "/api/v1/emergency/fall-detection" "$negative_confidence_data" "$auth_header" "400" "음수 신뢰도 값"

    # 실패 케이스 - 잘못된 시간 형식
    local invalid_time_data="{
        \"confidence\": 0.95,
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"detectionTime\": \"invalid-date-format\"
    }"

    make_request "POST" "/api/v1/emergency/fall-detection" "$invalid_time_data" "$auth_header" "400" "잘못된 시간 형식"
}

test_get_emergency_status() {
    log "========== 긴급상황 상태 조회 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 먼저 긴급상황 생성 (테스트용)
    if [[ -z "$EMERGENCY_ID" ]]; then
        local test_emergency_data="{
            \"type\": \"MANUAL_ALERT\",
            \"latitude\": 37.5665,
            \"longitude\": 126.9780,
            \"description\": \"상태 조회 테스트용\",
            \"severity\": \"HIGH\"
        }"

        local response
        response=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: $auth_header" \
            -d "$test_emergency_data" \
            "$BASE_URL/api/v1/emergency/alert")

        if echo "$response" | grep -q "emergencyId\|id"; then
            EMERGENCY_ID=$(echo "$response" | grep -o '"emergencyId":[0-9]*\|"id":[0-9]*' | head -1 | cut -d':' -f2)
            log "테스트용 긴급상황 생성됨 ID: $EMERGENCY_ID"
        fi
    fi

    if [[ -n "$EMERGENCY_ID" ]]; then
        # 성공 케이스 - 본인의 긴급상황 조회
        make_request "GET" "/api/v1/emergency/status/$EMERGENCY_ID" "" "$auth_header" "200" "본인 긴급상황 상태 조회"
    fi

    # 실패 케이스 - 존재하지 않는 긴급상황
    make_request "GET" "/api/v1/emergency/status/99999" "" "$auth_header" "404" "존재하지 않는 긴급상황 조회"

    # 실패 케이스 - 인증 없음
    make_request "GET" "/api/v1/emergency/status/1" "" "" "401" "인증 없이 긴급상황 조회"

    # 실패 케이스 - 잘못된 ID 형식
    make_request "GET" "/api/v1/emergency/status/invalid-id" "" "$auth_header" "400" "잘못된 긴급상황 ID 형식"

    # 실패 케이스 - 음수 ID
    make_request "GET" "/api/v1/emergency/status/-1" "" "$auth_header" "400" "음수 긴급상황 ID"
}

test_get_user_emergency_history() {
    log "========== 사용자 긴급상황 이력 조회 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" || -z "$TEST_USER_ID" ]]; then
        log "ACCESS_TOKEN 또는 TEST_USER_ID가 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스 - 본인 이력 조회
    make_request "GET" "/api/v1/emergency/history/$TEST_USER_ID" "" "$auth_header" "200" "본인 긴급상황 이력 조회"

    # 성공 케이스 - 페이지네이션 파라미터
    make_request "GET" "/api/v1/emergency/history/$TEST_USER_ID?page=0&size=10" "" "$auth_header" "200" "페이지네이션 파라미터 포함 이력 조회"

    # 실패 케이스 - 다른 사용자 이력 조회 (권한 없음)
    make_request "GET" "/api/v1/emergency/history/999" "" "$auth_header" "403" "권한 없는 사용자 이력 조회"

    # 실패 케이스 - 인증 없음
    make_request "GET" "/api/v1/emergency/history/$TEST_USER_ID" "" "" "401" "인증 없이 이력 조회"

    # 실패 케이스 - 존재하지 않는 사용자
    make_request "GET" "/api/v1/emergency/history/99999" "" "$auth_header" "404" "존재하지 않는 사용자 이력 조회"

    # 실패 케이스 - 잘못된 사용자 ID 형식
    make_request "GET" "/api/v1/emergency/history/invalid-id" "" "$auth_header" "400" "잘못된 사용자 ID 형식"
}

test_get_active_emergencies() {
    log "========== 활성 긴급상황 목록 조회 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 실패 케이스 - 일반 사용자 접근 (보호자/관리자만 가능)
    make_request "GET" "/api/v1/emergency/active" "" "$auth_header" "403" "일반 사용자의 활성 긴급상황 조회"

    # 실패 케이스 - 인증 없음
    make_request "GET" "/api/v1/emergency/active" "" "" "401" "인증 없이 활성 긴급상황 조회"
}

test_resolve_emergency() {
    log "========== 긴급상황 해결 처리 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    if [[ -n "$EMERGENCY_ID" ]]; then
        # 실패 케이스 - 권한 없음 (보호자/관리자만 가능)
        make_request "PUT" "/api/v1/emergency/$EMERGENCY_ID/resolve?resolvedBy=테스트자&notes=테스트해결" "" "$auth_header" "403" "권한 없는 긴급상황 해결"
    fi

    # 실패 케이스 - 존재하지 않는 긴급상황
    make_request "PUT" "/api/v1/emergency/99999/resolve?resolvedBy=테스트자" "" "$auth_header" "404" "존재하지 않는 긴급상황 해결"

    # 실패 케이스 - 인증 없음
    make_request "PUT" "/api/v1/emergency/1/resolve?resolvedBy=테스트자" "" "" "401" "인증 없이 긴급상황 해결"

    # 실패 케이스 - 필수 파라미터 누락
    if [[ -n "$EMERGENCY_ID" ]]; then
        make_request "PUT" "/api/v1/emergency/$EMERGENCY_ID/resolve" "" "$auth_header" "400" "해결자 파라미터 누락"
    fi
}

test_edge_cases() {
    log "========== 엣지 케이스 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # SQL Injection 시도
    local sql_injection_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"'; DROP TABLE emergencies; --\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$sql_injection_data" "$auth_header" "400" "SQL Injection 시도"

    # XSS 시도
    local xss_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"<script>alert('xss')</script>\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$xss_data" "$auth_header" "400" "XSS 시도"

    # 매우 긴 설명
    local long_description=""
    for i in {1..1000}; do
        long_description+="매우 긴 설명입니다. "
    done

    local long_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 37.5665,
        \"longitude\": 126.9780,
        \"description\": \"$long_description\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$long_data" "$auth_header" "400" "매우 긴 설명"

    # 극한 좌표값
    local extreme_coords_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": 90.1,
        \"longitude\": 180.1,
        \"description\": \"극한 좌표 테스트\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$extreme_coords_data" "$auth_header" "400" "극한 좌표값"

    # null 값 테스트
    local null_data="{
        \"type\": \"MANUAL_ALERT\",
        \"latitude\": null,
        \"longitude\": null,
        \"description\": \"null 테스트\"
    }"

    make_request "POST" "/api/v1/emergency/alert" "$null_data" "$auth_header" "400" "null 값 포함"
}

print_summary() {
    echo ""
    echo "=========================================="
    echo "EmergencyController 테스트 결과 요약"
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
    log "========== EmergencyController 테스트 시작 =========="

    # 서버 상태 확인
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        log_error "서버가 실행되지 않았습니다. 서버를 먼저 실행해주세요."
        exit 1
    fi

    setup_test_users
    test_create_emergency_alert
    test_fall_detection
    test_get_emergency_status
    test_get_user_emergency_history
    test_get_active_emergencies
    test_resolve_emergency
    test_edge_cases

    print_summary
}

main "$@"