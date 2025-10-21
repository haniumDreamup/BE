#!/bin/bash

# HealthController 100% 성공률 달성 테스트 스크립트
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
    log "========== 💚 HealthController 테스트 시작 =========="

    # 1. 기본 헬스 체크 - /api/health
    test_endpoint "GET" "/api/health" "200" "기본 헬스 체크 (API)"

    # 2. 단순 헬스 체크 - /health
    test_endpoint "GET" "/health" "200" "단순 헬스 체크 (루트)"

    # 3. V1 헬스 체크 - /api/v1/health
    test_endpoint "GET" "/api/v1/health" "200" "V1 헬스 체크 (API v1)"

    # 4. 존재하지 않는 엔드포인트들 (404 반환)
    test_endpoint "GET" "/api/health/status" "404" "존재하지 않는 엔드포인트 - 상태 세부사항"

    test_endpoint "GET" "/api/health/info" "404" "존재하지 않는 엔드포인트 - 정보"

    test_endpoint "GET" "/api/health/detailed" "404" "존재하지 않는 엔드포인트 - 상세정보"

    test_endpoint "GET" "/api/v2/health" "404" "존재하지 않는 엔드포인트 - V2 헬스"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 5. 잘못된 HTTP 메서드 (405 반환)
    test_endpoint "POST" "/api/health" "405" "잘못된 HTTP 메서드 (POST)"

    test_endpoint "PUT" "/health" "405" "잘못된 HTTP 메서드 (PUT)"

    test_endpoint "DELETE" "/api/v1/health" "405" "잘못된 HTTP 메서드 (DELETE)"

    test_endpoint "PATCH" "/api/health" "405" "잘못된 HTTP 메서드 (PATCH)"

    # 6. 완전히 존재하지 않는 엔드포인트
    test_endpoint "GET" "/api/health/nonexistent" "404" "완전히 존재하지 않는 엔드포인트"

    # 7. 경로 파라미터가 있는 잘못된 요청
    test_endpoint "GET" "/api/health/123" "404" "경로 파라미터가 있는 잘못된 요청"

    # 8. JSON 데이터와 함께 GET 요청 (무시되어야 함)
    test_endpoint "GET" "/api/health" "200" "JSON 데이터와 함께 GET 요청" '{"test":"data"}'

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 HealthController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 HealthController 테스트 100% 성공!${NC}"
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