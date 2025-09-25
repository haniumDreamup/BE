#!/bin/bash

# AccessibilityController 직접 테스트
# 모든 접근성 관련 엔드포인트 검증

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

    ((TOTAL_TESTS++))

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

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
    sleep 0.3
}

main() {
    log "========== 📱 AccessibilityController 테스트 시작 =========="

    # 1. 음성 안내 텍스트 생성 (POST /api/v1/accessibility/voice-guidance)
    test_endpoint "POST" "/api/v1/accessibility/voice-guidance" "200" "음성 안내 텍스트 생성" \
        '{"context": "navigation", "params": {"action": "move", "target": "button"}, "language": "ko"}'

    # 2. ARIA 라벨 생성 (POST /api/v1/accessibility/aria-label)
    test_endpoint "POST" "/api/v1/accessibility/aria-label" "200" "ARIA 라벨 생성" \
        '{"elementType": "button", "elementName": "확인", "attributes": {"role": "primary"}}'

    # 3. 스크린 리더 힌트 조회 (GET /api/v1/accessibility/screen-reader-hint)
    test_endpoint "GET" "/api/v1/accessibility/screen-reader-hint?action=click&target=button" "200" "스크린 리더 힌트 조회"

    # 4. 접근성 설정 조회 (GET /api/v1/accessibility/settings)
    test_endpoint "GET" "/api/v1/accessibility/settings" "200" "접근성 설정 조회"

    # 5. 접근성 설정 업데이트 (PUT /api/v1/accessibility/settings)
    test_endpoint "PUT" "/api/v1/accessibility/settings" "200" "접근성 설정 업데이트" \
        '{"fontSize": "large", "highContrast": true, "voiceSpeed": "normal"}'

    # 6. 프로파일 적용 (POST /api/v1/accessibility/settings/apply-profile)
    test_endpoint "POST" "/api/v1/accessibility/settings/apply-profile?profileType=low_vision" "200" "접근성 프로파일 적용"

    # 7. 색상 스키마 목록 조회 (GET /api/v1/accessibility/color-schemes)
    test_endpoint "GET" "/api/v1/accessibility/color-schemes" "200" "색상 스키마 목록 조회"

    # 8. 현재 색상 스키마 조회 (GET /api/v1/accessibility/color-schemes/current)
    test_endpoint "GET" "/api/v1/accessibility/color-schemes/current" "200" "현재 색상 스키마 조회"

    # 9. 간소화된 네비게이션 조회 (GET /api/v1/accessibility/simplified-navigation)
    test_endpoint "GET" "/api/v1/accessibility/simplified-navigation" "200" "간소화 네비게이션 조회"

    # 10. 터치 타겟 정보 조회 (GET /api/v1/accessibility/touch-targets)
    test_endpoint "GET" "/api/v1/accessibility/touch-targets?deviceType=mobile" "200" "터치 타겟 정보 조회"

    # 11. 텍스트 간소화 (POST /api/v1/accessibility/simplify-text)
    test_endpoint "POST" "/api/v1/accessibility/simplify-text" "200" "텍스트 간소화" \
        '{"text": "복잡한 문장을 간단하게 만들어주세요", "targetLevel": "elementary"}'

    # 12. 설정 동기화 (POST /api/v1/accessibility/settings/sync)
    test_endpoint "POST" "/api/v1/accessibility/settings/sync" "200" "설정 동기화"

    # 13. 접근성 통계 조회 (GET /api/v1/accessibility/statistics)
    test_endpoint "GET" "/api/v1/accessibility/statistics" "200" "접근성 통계 조회"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 잘못된 HTTP 메서드
    test_endpoint "DELETE" "/api/v1/accessibility/voice-guidance" "405" "잘못된 메서드 - 음성 안내"

    # 존재하지 않는 엔드포인트
    test_endpoint "GET" "/api/v1/accessibility/nonexistent" "404" "존재하지 않는 엔드포인트"

    # 빈 데이터로 POST 요청
    test_endpoint "POST" "/api/v1/accessibility/voice-guidance" "400" "빈 데이터 POST 요청" "{}"

    # 잘못된 JSON 형식
    test_endpoint "POST" "/api/v1/accessibility/aria-label" "400" "잘못된 JSON 형식" "invalid json"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 AccessibilityController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"
    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 AccessibilityController 테스트 100% 성공!${NC}"
        else
            echo -e "${YELLOW}⚠️  일부 테스트가 실패했습니다.${NC}"
        fi
    fi
    echo "=========================================="

    return $FAILED_TESTS
}

main "$@"