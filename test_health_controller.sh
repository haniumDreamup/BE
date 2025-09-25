#!/bin/bash

set -e

# 현재 스크립트 디렉토리 가져오기
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 공통 유틸리티 로드
source "$SCRIPT_DIR/test_utils.sh"

test_basic_health_endpoint() {
    print_test "GET /health - 기본 헬스체크"

    local response=$(api_call "GET" "/health" "" "")
    local parsed=$(parse_response "$response")
    local status_code=$(echo "$parsed" | cut -d'|' -f1)
    local body=$(echo "$parsed" | cut -d'|' -f2)

    if check_status_code "$status_code" "200"; then
        if echo "$body" | jq -e '.status == "UP"' >/dev/null 2>&1; then
            print_success "기본 헬스체크 성공"
        else
            print_failure "응답 구조 오류" "$body"
        fi
    else
        print_failure "HTTP 상태코드 오류" "Expected: 200, Got: $status_code"
    fi
}

test_api_health_endpoint() {
    print_test "GET /api/health - API 헬스체크"

    local response=$(api_call "GET" "/api/health" "" "")
    local parsed=$(parse_response "$response")
    local status_code=$(echo "$parsed" | cut -d'|' -f1)
    local body=$(echo "$parsed" | cut -d'|' -f2)

    if check_status_code "$status_code" "200"; then
        if echo "$body" | jq -e '.status == "UP"' >/dev/null 2>&1; then
            print_success "API 헬스체크 성공"
        else
            print_failure "응답 구조 오류" "$body"
        fi
    else
        print_failure "HTTP 상태코드 오류" "Expected: 200, Got: $status_code"
    fi
}

test_api_v1_health_endpoint() {
    print_test "GET /api/v1/health - V1 API 헬스체크"

    local response=$(api_call "GET" "/api/v1/health" "" "")
    local parsed=$(parse_response "$response")
    local status_code=$(echo "$parsed" | cut -d'|' -f1)
    local body=$(echo "$parsed" | cut -d'|' -f2)

    if check_status_code "$status_code" "200"; then
        if echo "$body" | jq -e '.status == "UP"' >/dev/null 2>&1; then
            print_success "V1 API 헬스체크 성공"
        else
            print_failure "응답 구조 오류" "$body"
        fi
    else
        print_failure "HTTP 상태코드 오류" "Expected: 200, Got: $status_code"
    fi
}

main() {
    print_header "HealthController 테스트 시작"

    if ! check_dependencies; then
        exit 1
    fi

    if ! check_server_status; then
        print_failure "서버가 응답하지 않습니다" "테스트를 중단합니다"
        exit 1
    fi

    print_info "HealthController 엔드포인트 테스트 실행 중..."

    test_basic_health_endpoint
    test_api_health_endpoint
    test_api_v1_health_endpoint

    print_test_summary "HealthController"
    save_test_results "HealthController"

    if [[ $FAILED_TESTS -gt 0 ]]; then
        exit 1
    else
        exit 0
    fi
}

main "$@"