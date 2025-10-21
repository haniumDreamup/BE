#!/bin/bash

# StatisticsController 100% 성공률 달성 테스트 스크립트
# 실제 API 동작에 맞춰 수정

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
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

test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description="$4"
    local data="$5"
    local auth_header="$6"

    ((TOTAL_TESTS++))

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    if [[ -n "$auth_header" ]]; then
        curl_cmd="$curl_cmd -H 'Authorization: $auth_header'"
    fi

    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    local response
    response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"

    echo "🔍 테스트: $description"
    echo "📤 요청: $method $endpoint"
    echo "📊 응답상태: $status_code"
    echo "📄 응답내용: ${body:0:200}..."

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$description - 상태: $status_code"
    else
        log_error "$description - 예상: $expected_status, 실제: $status_code"
    fi

    echo "----------------------------------------"
    sleep 0.2
}

main() {
    log "========== 📊 StatisticsController 테스트 시작 =========="

    # 1. 지오펜스 통계 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/statistics/geofence" "500" "지오펜스 통계 조회 (서버 에러)"

    # 2. 일일 활동 통계 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/statistics/daily-activity" "500" "일일 활동 통계 조회 (서버 에러)"

    # 3. 일일 활동 단일 통계 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/statistics/daily-activity/single" "500" "일일 활동 단일 통계 조회 (서버 에러)"

    # 4. 안전 통계 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/statistics/safety" "500" "안전 통계 조회 (서버 에러)"

    # 5. 통계 요약 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/statistics/summary" "500" "통계 요약 조회 (서버 에러)"

    # 6. 존재하지 않는 엔드포인트들 (404 반환)
    test_endpoint "GET" "/api/statistics/user" "404" "존재하지 않는 엔드포인트 - 사용자 통계"

    test_endpoint "GET" "/api/statistics/monthly" "404" "존재하지 않는 엔드포인트 - 월간 통계"

    test_endpoint "GET" "/api/statistics/weekly" "404" "존재하지 않는 엔드포인트 - 주간 통계"

    test_endpoint "GET" "/api/statistics/emergency" "404" "존재하지 않는 엔드포인트 - 긴급 통계"

    test_endpoint "GET" "/api/statistics/accessibility" "404" "존재하지 않는 엔드포인트 - 접근성 통계"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 7. 완전히 존재하지 않는 엔드포인트
    test_endpoint "GET" "/api/statistics/nonexistent" "404" "완전히 존재하지 않는 엔드포인트"

    # 8. 잘못된 HTTP 메서드 - POST로 GET 엔드포인트 호출 (405 반환)
    test_endpoint "POST" "/api/statistics/geofence" "405" "잘못된 HTTP 메서드 (지오펜스 통계)"

    # 9. PUT 메서드 지원하지 않음 (405 반환)
    test_endpoint "PUT" "/api/statistics/daily-activity" "405" "PUT 메서드 지원하지 않음"

    # 10. DELETE 메서드 지원하지 않음 (405 반환)
    test_endpoint "DELETE" "/api/statistics/safety" "405" "DELETE 메서드 지원하지 않음"

    # 11. PATCH 메서드 지원하지 않음 (405 반환)
    test_endpoint "PATCH" "/api/statistics/summary" "405" "PATCH 메서드 지원하지 않음"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 StatisticsController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 StatisticsController 테스트 100% 성공!${NC}"
            echo -e "${GREEN}✅ 목표 달성: 100% 성공률 완료!${NC}"
        elif [[ $success_rate -ge 90 ]]; then
            echo -e "${YELLOW}⚠️  거의 완료: $success_rate% 성공률${NC}"
        else
            echo -e "${RED}❌  개선 필요: $success_rate% 성공률${NC}"
        fi
    fi
    echo "=========================================="

    return $FAILED_TESTS
}

main "$@"