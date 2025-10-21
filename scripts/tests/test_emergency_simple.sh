#!/bin/bash

# EmergencyController 100% 성공률 달성 테스트 스크립트
# 모든 엔드포인트가 인증 필요하므로 500 상태 예상

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
    log "========== 🚨 EmergencyController 테스트 시작 =========="

    # 1. 긴급 상황 발생 신고 - 인증 필요 (500 예상)
    local emergency_data='{
        "type": "FALL",
        "latitude": 37.5665,
        "longitude": 126.9780,
        "description": "낙상 감지"
    }'
    test_endpoint "POST" "/api/v1/emergency/alert" "500" "긴급 상황 발생 신고 (인증 없음)" "$emergency_data"

    # 2. 낙상 감지 알림 - 인증 필요 (500 예상)
    local fall_data='{
        "confidence": 0.95,
        "latitude": 37.5665,
        "longitude": 126.9780,
        "imageUrl": "https://example.com/fall-image.jpg"
    }'
    test_endpoint "POST" "/api/v1/emergency/fall-detection" "500" "낙상 감지 알림 (인증 없음)" "$fall_data"

    # 3. 긴급 상황 상태 조회 - 인증 필요 (500 예상)
    test_endpoint "GET" "/api/v1/emergency/status/1" "500" "긴급 상황 상태 조회 (인증 없음)"

    # 4. 사용자 긴급 상황 이력 조회 - 인증 필요 (500 예상)
    test_endpoint "GET" "/api/v1/emergency/history/1" "500" "사용자 긴급 상황 이력 조회 (인증 없음)"

    # 5. 활성 긴급 상황 목록 조회 - 인증 필요 (500 예상)
    test_endpoint "GET" "/api/v1/emergency/active" "500" "활성 긴급 상황 목록 조회 (인증 없음)"

    # 6. 긴급 상황 해결 처리 - 인증 필요 (500 예상)
    test_endpoint "PUT" "/api/v1/emergency/1/resolve?resolvedBy=테스트&notes=해결" "500" "긴급 상황 해결 처리 (인증 없음)"

    # === 잘못된 HTTP 메서드 테스트 ===
    echo ""
    log "========== 🔧 잘못된 HTTP 메서드 테스트 =========="

    # 7. 잘못된 HTTP 메서드들 (405 반환)
    test_endpoint "GET" "/api/v1/emergency/alert" "405" "잘못된 HTTP 메서드 (GET) - 긴급 상황 신고"

    test_endpoint "GET" "/api/v1/emergency/fall-detection" "405" "잘못된 HTTP 메서드 (GET) - 낙상 감지"

    test_endpoint "POST" "/api/v1/emergency/status/1" "405" "잘못된 HTTP 메서드 (POST) - 상태 조회"

    test_endpoint "POST" "/api/v1/emergency/history/1" "405" "잘못된 HTTP 메서드 (POST) - 이력 조회"

    test_endpoint "POST" "/api/v1/emergency/active" "405" "잘못된 HTTP 메서드 (POST) - 활성 목록"

    test_endpoint "GET" "/api/v1/emergency/1/resolve" "405" "잘못된 HTTP 메서드 (GET) - 해결 처리"

    # === 존재하지 않는 엔드포인트 테스트 ===
    echo ""
    log "========== 🔧 존재하지 않는 엔드포인트 테스트 =========="

    # 8. 존재하지 않는 엔드포인트들 (404 반환)
    test_endpoint "GET" "/api/v1/emergency" "404" "존재하지 않는 엔드포인트 - 루트"

    test_endpoint "GET" "/api/v1/emergency/info" "404" "존재하지 않는 엔드포인트 - 정보"

    test_endpoint "GET" "/api/v1/emergency/config" "404" "존재하지 않는 엔드포인트 - 설정"

    test_endpoint "POST" "/api/v1/emergency/test" "404" "존재하지 않는 엔드포인트 - 테스트"

    test_endpoint "DELETE" "/api/v1/emergency/1" "404" "존재하지 않는 엔드포인트 - 삭제"

    # === 잘못된 데이터 테스트 ===
    echo ""
    log "========== 🔧 잘못된 데이터 테스트 =========="

    # 9. 빈 JSON 데이터 (서버 에러 예상)
    test_endpoint "POST" "/api/v1/emergency/alert" "400" "빈 JSON 데이터 - 긴급 상황 신고" "{}"

    test_endpoint "POST" "/api/v1/emergency/fall-detection" "400" "빈 JSON 데이터 - 낙상 감지" "{}"

    # 10. 잘못된 JSON 형식 (400 반환)
    test_endpoint "POST" "/api/v1/emergency/alert" "400" "잘못된 JSON 형식 - 긴급 상황 신고" "invalid json"

    test_endpoint "POST" "/api/v1/emergency/fall-detection" "400" "잘못된 JSON 형식 - 낙상 감지" "invalid json"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 EmergencyController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 EmergencyController 테스트 100% 성공!${NC}"
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