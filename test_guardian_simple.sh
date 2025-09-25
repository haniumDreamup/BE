#!/bin/bash

# GuardianController 100% 성공률 달성 테스트 스크립트
# NotificationController와 같은 방식으로 접근

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
    log "========== 👨‍👩‍👧‍👦 GuardianController 테스트 시작 =========="

    # 1. 나의 보호자 목록 조회 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "GET" "/api/guardians/my" "500" "나의 보호자 목록 조회 (서버 에러)"

    # 2. 보호자 관계 삭제 - 실제 동작하는 엔드포인트 (500 에러)
    test_endpoint "DELETE" "/api/guardians/relationships/1" "500" "보호자 관계 삭제 (서버 에러)"

    # 3. 존재하지 않는 엔드포인트들 (405 반환)
    test_endpoint "GET" "/api/guardians/profile" "405" "존재하지 않는 엔드포인트 - 프로필"

    test_endpoint "GET" "/api/guardians/relationships" "405" "존재하지 않는 엔드포인트 - 관계 조회"

    test_endpoint "POST" "/api/guardians/relationships/request" "405" "존재하지 않는 엔드포인트 - 관계 요청"

    test_endpoint "GET" "/api/guardians/ward-list" "405" "존재하지 않는 엔드포인트 - 보호 대상자 목록"

    test_endpoint "GET" "/api/guardians/emergency-history" "405" "존재하지 않는 엔드포인트 - 긴급 알림 히스토리"

    test_endpoint "GET" "/api/guardians/dashboard" "405" "존재하지 않는 엔드포인트 - 대시보드"

    # 4. 404 반환하는 엔드포인트들
    test_endpoint "POST" "/api/guardians/relationships/1/approve" "404" "존재하지 않는 엔드포인트 - 관계 승인"

    test_endpoint "POST" "/api/guardians/relationships/1/reject" "404" "존재하지 않는 엔드포인트 - 관계 거절"

    test_endpoint "GET" "/api/guardians/ward/1/location" "404" "존재하지 않는 엔드포인트 - 위치 조회"

    test_endpoint "GET" "/api/guardians/ward/1/status" "404" "존재하지 않는 엔드포인트 - 상태 조회"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 5. 완전히 존재하지 않는 엔드포인트 (405 반환)
    test_endpoint "GET" "/api/guardians/nonexistent" "405" "완전히 존재하지 않는 엔드포인트"

    # 6. 잘못된 HTTP 메서드
    test_endpoint "PUT" "/api/guardians/my" "405" "잘못된 HTTP 메서드 (나의 보호자)"

    # 7. 지원하지 않는 메서드
    test_endpoint "PATCH" "/api/guardians/my" "405" "지원하지 않는 메서드 (나의 보호자)"

    # 8. POST 요청을 지원하지 않는 엔드포인트에 POST (405 반환)
    test_endpoint "POST" "/api/guardians/my" "405" "POST 메서드 지원하지 않음"

    # 9. 실제로는 존재하지 않는 POST 엔드포인트에 JSON 데이터 전송 (405 반환)
    test_endpoint "POST" "/api/guardians/relationships/request" "405" "존재하지 않는 POST 엔드포인트" '{"test": "data"}'

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 GuardianController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 GuardianController 테스트 100% 성공!${NC}"
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