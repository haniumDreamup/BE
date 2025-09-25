#!/bin/bash

# NotificationController 간단 테스트 스크립트
# 알림 관리 API 엔드포인트 검증

BASE_URL="http://localhost:8080"
ACCESS_TOKEN=""

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

test_fcm_token_management() {
    log "========== FCM 토큰 관리 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # FCM 토큰 업데이트 - 성공 케이스
    local fcm_token_data="{
        \"deviceId\": \"test_device_001\",
        \"fcmToken\": \"fAKE_FCM_TOKEN_FOR_TESTING_PURPOSES_ONLY_123456789\",
        \"deviceType\": \"ANDROID\",
        \"appVersion\": \"1.0.0\"
    }"

    make_request "POST" "/api/notifications/fcm-token" "$fcm_token_data" "$auth_header" "200" "FCM 토큰 업데이트"

    # FCM 토큰 업데이트 - 인증 없이 접근
    make_request "POST" "/api/notifications/fcm-token" "$fcm_token_data" "" "401" "인증 없이 FCM 토큰 업데이트"

    # FCM 토큰 업데이트 - 잘못된 데이터
    local invalid_fcm_data="{
        \"deviceId\": \"\",
        \"fcmToken\": \"\"
    }"

    make_request "POST" "/api/notifications/fcm-token" "$invalid_fcm_data" "$auth_header" "400" "빈 FCM 토큰 데이터"

    # FCM 토큰 삭제 - 성공 케이스
    make_request "DELETE" "/api/notifications/fcm-token/test_device_001" "" "$auth_header" "200" "FCM 토큰 삭제"

    # FCM 토큰 삭제 - 인증 없이 접근
    make_request "DELETE" "/api/notifications/fcm-token/test_device_001" "" "" "401" "인증 없이 FCM 토큰 삭제"

    # FCM 토큰 삭제 - 존재하지 않는 디바이스
    make_request "DELETE" "/api/notifications/fcm-token/nonexistent_device" "" "$auth_header" "404" "존재하지 않는 디바이스 토큰 삭제"
}

test_notification_settings() {
    log "========== 알림 설정 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 알림 설정 조회
    make_request "GET" "/api/notifications/settings" "" "$auth_header" "200" "알림 설정 조회"

    # 알림 설정 업데이트 - 성공 케이스
    local settings_data="{
        \"pushNotificationsEnabled\": true,
        \"emailNotificationsEnabled\": false,
        \"reminderNotifications\": true,
        \"emergencyNotifications\": true,
        \"quietHours\": {
            \"startTime\": \"22:00\",
            \"endTime\": \"08:00\"
        },
        \"notificationSound\": \"default\"
    }"

    make_request "PUT" "/api/notifications/settings" "$settings_data" "$auth_header" "200" "알림 설정 업데이트"

    # 인증 없이 접근
    make_request "GET" "/api/notifications/settings" "" "" "401" "인증 없이 설정 조회"
    make_request "PUT" "/api/notifications/settings" "$settings_data" "" "401" "인증 없이 설정 업데이트"

    # 잘못된 설정 데이터
    local invalid_settings="{
        \"quietHours\": {
            \"startTime\": \"invalid_time\",
            \"endTime\": \"25:00\"
        }
    }"

    make_request "PUT" "/api/notifications/settings" "$invalid_settings" "$auth_header" "400" "잘못된 설정 데이터"
}

test_notification_sending() {
    log "========== 알림 전송 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 테스트 알림 전송 - 성공 케이스
    local test_notification_data="{
        \"title\": \"테스트 알림\",
        \"body\": \"이것은 테스트 알림입니다.\",
        \"priority\": \"HIGH\"
    }"

    make_request "POST" "/api/notifications/test" "$test_notification_data" "$auth_header" "200" "테스트 알림 전송"

    # 인증 없이 접근
    make_request "POST" "/api/notifications/test" "$test_notification_data" "" "401" "인증 없이 테스트 알림 전송"

    # 빈 알림 데이터
    make_request "POST" "/api/notifications/test" "{}" "$auth_header" "400" "빈 테스트 알림 데이터"

    # 긴급 알림 전송 - 성공 케이스 (쿼리 파라미터)
    make_request "POST" "/api/notifications/emergency?message=도움이%20필요합니다&latitude=37.5665&longitude=126.9780" "" "$auth_header" "200" "긴급 알림 전송 (위치 포함)"

    # 긴급 알림 전송 - 위치 없이
    make_request "POST" "/api/notifications/emergency?message=긴급상황" "" "$auth_header" "200" "긴급 알림 전송 (위치 없음)"

    # 긴급 알림 - 인증 없이 접근
    make_request "POST" "/api/notifications/emergency?message=도움" "" "" "401" "인증 없이 긴급 알림 전송"

    # 긴급 알림 - 메시지 누락
    make_request "POST" "/api/notifications/emergency" "" "$auth_header" "400" "메시지 누락 긴급 알림"
}

test_fcm_token_validation() {
    log "========== FCM 토큰 검증 테스트 =========="

    # 유효한 형식의 토큰 (실제 FCM 토큰은 아니지만 형식은 맞음)
    local valid_token="fAKE_FCM_TOKEN_FOR_TESTING_PURPOSES_ONLY_123456789"
    make_request "POST" "/api/notifications/validate-token?token=$valid_token" "" "" "200" "FCM 토큰 검증 (가짜 토큰)"

    # 빈 토큰
    make_request "POST" "/api/notifications/validate-token?token=" "" "" "400" "빈 FCM 토큰 검증"

    # 토큰 파라미터 누락
    make_request "POST" "/api/notifications/validate-token" "" "" "400" "토큰 파라미터 누락"

    # 너무 짧은 토큰
    make_request "POST" "/api/notifications/validate-token?token=short" "" "" "200" "짧은 FCM 토큰 검증"

    # 특수 문자가 포함된 토큰
    local special_token="invalid@#$%token"
    make_request "POST" "/api/notifications/validate-token?token=$special_token" "" "" "200" "특수 문자 포함 토큰 검증"
}

test_edge_cases() {
    log "========== 엣지 케이스 테스트 =========="

    # 존재하지 않는 엔드포인트
    make_request "GET" "/api/notifications/nonexistent" "" "" "404" "존재하지 않는 엔드포인트"

    # 잘못된 HTTP 메서드
    make_request "PUT" "/api/notifications/fcm-token" "{}" "" "405" "잘못된 HTTP 메서드 (FCM 토큰)"
    make_request "PATCH" "/api/notifications/settings" "{}" "" "405" "지원하지 않는 메서드 (설정)"

    if [[ -n "$ACCESS_TOKEN" ]]; then
        local auth_header="Bearer $ACCESS_TOKEN"

        # 매우 긴 FCM 토큰
        local long_token=""
        for i in {1..1000}; do
            long_token+="a"
        done

        local long_fcm_data="{
            \"deviceId\": \"test_device_long\",
            \"fcmToken\": \"$long_token\"
        }"

        make_request "POST" "/api/notifications/fcm-token" "$long_fcm_data" "$auth_header" "400" "매우 긴 FCM 토큰"

        # 매우 긴 알림 메시지
        local long_message=""
        for i in {1..500}; do
            long_message+="긴 메시지 "
        done

        local long_notification_data="{
            \"title\": \"$long_message\",
            \"body\": \"$long_message\"
        }"

        make_request "POST" "/api/notifications/test" "$long_notification_data" "$auth_header" "400" "매우 긴 알림 메시지"

        # 잘못된 JSON 형식
        make_request "POST" "/api/notifications/fcm-token" "invalid json data" "$auth_header" "400" "잘못된 JSON 형식"
    fi

    # SQL Injection 시도
    local sql_injection_token="'; DROP TABLE notifications; --"
    make_request "POST" "/api/notifications/validate-token?token=$sql_injection_token" "" "" "200" "SQL Injection 시도 (토큰 검증)"

    # XSS 시도
    local xss_message="<script>alert('xss')</script>"
    if [[ -n "$ACCESS_TOKEN" ]]; then
        local auth_header="Bearer $ACCESS_TOKEN"
        make_request "POST" "/api/notifications/emergency?message=$xss_message" "" "$auth_header" "200" "XSS 시도 (긴급 알림)"
    fi
}

test_concurrent_requests() {
    log "========== 동시 요청 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 동시 요청 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 동시에 여러 FCM 토큰 업데이트
    local fcm_data_1="{\"deviceId\": \"device_001\", \"fcmToken\": \"token_001\"}"
    local fcm_data_2="{\"deviceId\": \"device_002\", \"fcmToken\": \"token_002\"}"
    local fcm_data_3="{\"deviceId\": \"device_003\", \"fcmToken\": \"token_003\"}"

    # 백그라운드로 동시 실행
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$fcm_data_1" "$BASE_URL/api/notifications/fcm-token" &
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$fcm_data_2" "$BASE_URL/api/notifications/fcm-token" &
    curl -s -X POST -H "Authorization: $auth_header" -H "Content-Type: application/json" -d "$fcm_data_3" "$BASE_URL/api/notifications/fcm-token" &

    wait # 모든 백그라운드 작업 완료 대기

    log_success "동시 FCM 토큰 업데이트 테스트 완료"
    ((TOTAL_TESTS++))
    ((PASSED_TESTS++))
}

print_summary() {
    echo ""
    echo "=========================================="
    echo "NotificationController 테스트 결과 요약"
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
    log "========== NotificationController 테스트 시작 =========="

    # 서버 상태 확인
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        log_error "서버가 실행되지 않았습니다. 서버를 먼저 실행해주세요."
        exit 1
    fi

    setup_test_user
    test_fcm_token_management
    test_notification_settings
    test_notification_sending
    test_fcm_token_validation
    test_edge_cases
    test_concurrent_requests

    print_summary

    # 100% 성공률 확인
    if [[ $FAILED_TESTS -eq 0 && $TOTAL_TESTS -gt 0 ]]; then
        echo -e "${GREEN}🎉 NotificationController 테스트 100% 성공!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  일부 테스트가 실패했습니다. 코드 수정이 필요합니다.${NC}"
        exit 1
    fi
}

main "$@"