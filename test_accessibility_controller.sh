#!/bin/bash

# AccessibilityController 테스트 스크립트
# 접근성 기능 엔드포인트 검증

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

test_voice_guidance() {
    log "========== 음성 안내 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스
    local voice_data="{
        \"context\": \"navigation\",
        \"params\": {
            \"location\": \"home\",
            \"action\": \"guide\"
        },
        \"language\": \"ko\"
    }"

    make_request "POST" "/api/v1/accessibility/voice-guidance" "$voice_data" "$auth_header" "200" "음성 안내 생성"

    # 인증 없이 접근
    make_request "POST" "/api/v1/accessibility/voice-guidance" "$voice_data" "" "401" "인증 없이 음성 안내 요청"

    # 잘못된 데이터
    make_request "POST" "/api/v1/accessibility/voice-guidance" "{}" "$auth_header" "400" "빈 음성 안내 데이터"
}

test_aria_label() {
    log "========== ARIA 라벨 테스트 =========="

    # 성공 케이스 (인증 불필요)
    local aria_data="{
        \"elementType\": \"button\",
        \"elementName\": \"홈으로 가기\",
        \"attributes\": {
            \"role\": \"button\",
            \"state\": \"enabled\"
        }
    }"

    make_request "POST" "/api/v1/accessibility/aria-label" "$aria_data" "" "200" "ARIA 라벨 생성"

    # 잘못된 데이터
    make_request "POST" "/api/v1/accessibility/aria-label" "{}" "" "400" "빈 ARIA 라벨 데이터"

    # 잘못된 JSON
    make_request "POST" "/api/v1/accessibility/aria-label" "invalid json" "" "400" "잘못된 JSON 형식"
}

test_screen_reader_hint() {
    log "========== 스크린 리더 힌트 테스트 =========="

    # 성공 케이스
    make_request "GET" "/api/v1/accessibility/screen-reader-hint?action=navigate&target=menu" "" "" "200" "스크린 리더 힌트 조회"

    # 파라미터 누락
    make_request "GET" "/api/v1/accessibility/screen-reader-hint?action=navigate" "" "" "400" "타겟 파라미터 누락"
    make_request "GET" "/api/v1/accessibility/screen-reader-hint?target=menu" "" "" "400" "액션 파라미터 누락"
    make_request "GET" "/api/v1/accessibility/screen-reader-hint" "" "" "400" "모든 파라미터 누락"
}

test_accessibility_settings() {
    log "========== 접근성 설정 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 설정 조회
    make_request "GET" "/api/v1/accessibility/settings" "" "$auth_header" "200" "접근성 설정 조회"

    # 설정 업데이트
    local settings_data="{
        \"fontSize\": \"large\",
        \"highContrast\": true,
        \"voiceGuidance\": true,
        \"colorScheme\": \"dark\"
    }"

    make_request "PUT" "/api/v1/accessibility/settings" "$settings_data" "$auth_header" "200" "접근성 설정 업데이트"

    # 인증 없이 접근
    make_request "GET" "/api/v1/accessibility/settings" "" "" "401" "인증 없이 설정 조회"
    make_request "PUT" "/api/v1/accessibility/settings" "$settings_data" "" "401" "인증 없이 설정 업데이트"
}

test_accessibility_profiles() {
    log "========== 접근성 프로파일 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 프로파일 적용
    make_request "POST" "/api/v1/accessibility/settings/apply-profile?profileType=visual_impairment" "" "$auth_header" "200" "시각 장애 프로파일 적용"
    make_request "POST" "/api/v1/accessibility/settings/apply-profile?profileType=motor_impairment" "" "$auth_header" "200" "운동 장애 프로파일 적용"

    # 잘못된 프로파일 타입
    make_request "POST" "/api/v1/accessibility/settings/apply-profile?profileType=invalid_profile" "" "$auth_header" "400" "잘못된 프로파일 타입"

    # 파라미터 누락
    make_request "POST" "/api/v1/accessibility/settings/apply-profile" "" "$auth_header" "400" "프로파일 타입 누락"
}

test_color_schemes() {
    log "========== 색상 스키마 테스트 =========="

    # 색상 스키마 목록 조회 (인증 불필요)
    make_request "GET" "/api/v1/accessibility/color-schemes" "" "" "200" "색상 스키마 목록 조회"

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 현재 색상 스키마 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 현재 색상 스키마 조회
    make_request "GET" "/api/v1/accessibility/color-schemes/current" "" "$auth_header" "200" "현재 색상 스키마 조회"

    # 인증 없이 현재 스키마 조회
    make_request "GET" "/api/v1/accessibility/color-schemes/current" "" "" "401" "인증 없이 현재 스키마 조회"
}

test_navigation_and_touch() {
    log "========== 네비게이션 및 터치 타겟 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 간소화 네비게이션
    make_request "GET" "/api/v1/accessibility/simplified-navigation" "" "$auth_header" "200" "간소화 네비게이션 조회"

    # 터치 타겟 정보
    make_request "GET" "/api/v1/accessibility/touch-targets" "" "$auth_header" "200" "터치 타겟 정보 조회"
    make_request "GET" "/api/v1/accessibility/touch-targets?deviceType=mobile" "" "$auth_header" "200" "모바일 터치 타겟 정보"
    make_request "GET" "/api/v1/accessibility/touch-targets?deviceType=tablet" "" "$auth_header" "200" "태블릿 터치 타겟 정보"

    # 인증 없이 접근
    make_request "GET" "/api/v1/accessibility/simplified-navigation" "" "" "401" "인증 없이 네비게이션 조회"
    make_request "GET" "/api/v1/accessibility/touch-targets" "" "" "401" "인증 없이 터치 타겟 조회"
}

test_text_simplification() {
    log "========== 텍스트 간소화 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 성공 케이스
    local text_data="{
        \"text\": \"이것은 복잡하고 어려운 문장입니다. 간단하게 만들어주세요.\",
        \"targetLevel\": \"elementary\"
    }"

    make_request "POST" "/api/v1/accessibility/simplify-text" "$text_data" "$auth_header" "200" "텍스트 간소화"

    # 잘못된 데이터
    make_request "POST" "/api/v1/accessibility/simplify-text" "{}" "$auth_header" "400" "빈 텍스트 데이터"

    # 인증 없이 접근
    make_request "POST" "/api/v1/accessibility/simplify-text" "$text_data" "" "401" "인증 없이 텍스트 간소화"
}

test_sync_and_statistics() {
    log "========== 동기화 및 통계 테스트 =========="

    if [[ -z "$ACCESS_TOKEN" ]]; then
        log "ACCESS_TOKEN이 없어 테스트를 건너뜁니다"
        return
    fi

    local auth_header="Bearer $ACCESS_TOKEN"

    # 설정 동기화
    make_request "POST" "/api/v1/accessibility/settings/sync" "" "$auth_header" "200" "설정 동기화"

    # 접근성 통계 (인증 불필요 또는 관리자용)
    make_request "GET" "/api/v1/accessibility/statistics" "" "" "200" "접근성 통계 조회"

    # 인증 없이 동기화 시도
    make_request "POST" "/api/v1/accessibility/settings/sync" "" "" "401" "인증 없이 설정 동기화"
}

test_edge_cases() {
    log "========== 엣지 케이스 테스트 =========="

    # 존재하지 않는 엔드포인트
    make_request "GET" "/api/v1/accessibility/nonexistent" "" "" "404" "존재하지 않는 엔드포인트"
    make_request "POST" "/api/v1/accessibility/invalid-endpoint" "{}" "" "404" "잘못된 엔드포인트"

    # 잘못된 HTTP 메서드
    make_request "PUT" "/api/v1/accessibility/color-schemes" "{}" "" "405" "잘못된 HTTP 메서드"
    make_request "DELETE" "/api/v1/accessibility/statistics" "" "" "405" "지원하지 않는 메서드"

    if [[ -n "$ACCESS_TOKEN" ]]; then
        local auth_header="Bearer $ACCESS_TOKEN"

        # 매우 긴 텍스트
        local long_text=""
        for i in {1..1000}; do
            long_text+="매우 긴 텍스트 "
        done

        local long_data="{
            \"text\": \"$long_text\",
            \"targetLevel\": \"elementary\"
        }"

        make_request "POST" "/api/v1/accessibility/simplify-text" "$long_data" "$auth_header" "400" "매우 긴 텍스트 처리"
    fi
}

print_summary() {
    echo ""
    echo "=========================================="
    echo "AccessibilityController 테스트 결과 요약"
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
    log "========== AccessibilityController 테스트 시작 =========="

    # 서버 상태 확인
    if ! curl -s "$BASE_URL/health" > /dev/null; then
        log_error "서버가 실행되지 않았습니다. 서버를 먼저 실행해주세요."
        exit 1
    fi

    setup_test_user
    test_voice_guidance
    test_aria_label
    test_screen_reader_hint
    test_accessibility_settings
    test_accessibility_profiles
    test_color_schemes
    test_navigation_and_touch
    test_text_simplification
    test_sync_and_statistics
    test_edge_cases

    print_summary

    # 100% 성공률 확인
    if [[ $FAILED_TESTS -eq 0 && $TOTAL_TESTS -gt 0 ]]; then
        echo -e "${GREEN}🎉 AccessibilityController 테스트 100% 성공!${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️  일부 테스트가 실패했습니다. 코드 수정이 필요합니다.${NC}"
        exit 1
    fi
}

main "$@"