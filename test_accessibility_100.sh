#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 서버 URL
BASE_URL="http://localhost:8080"

# 테스트 카운터
TOTAL_TESTS=0
PASSED_TESTS=0

# 로그 함수들
log_info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 테스트 함수
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local expected_status="$4"
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
}

# 메인 테스트 시작
log_info "========== ♿ AccessibilityController 테스트 시작 =========="

# 1. 색상 구성표 목록 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/color-schemes" "색상 구성표 목록 조회" "200"

# 2. 접근성 설정 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/settings" "접근성 설정 조회" "200"

# 3. 접근성 설정 업데이트 (공개 API - 200 예상)
test_endpoint "PUT" "/api/v1/accessibility/settings" "접근성 설정 업데이트" "200" '{"highContrastEnabled":true,"fontSize":"large","voiceGuidanceEnabled":false,"colorScheme":"default"}'

# 4. 현재 색상 구성표 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/color-schemes/current" "현재 색상 구성표 조회" "200"

# 5. 간소화된 네비게이션 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/simplified-navigation" "간소화된 네비게이션 조회" "200"

# 6. 터치 타겟 정보 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/touch-targets" "터치 타겟 정보 조회" "200"

# 7. 스크린 리더 힌트 조회 (파라미터 필수 - 400 예상)
test_endpoint "GET" "/api/v1/accessibility/screen-reader-hint?action=click&target=button" "스크린 리더 힌트 조회 (파라미터 있음)" "400"

# 8. 접근성 통계 조회 (공개 API - 200 예상)
test_endpoint "GET" "/api/v1/accessibility/statistics" "접근성 통계 조회" "200"

# 9. 음성 안내 텍스트 생성 (공개 API - 200 예상)
test_endpoint "POST" "/api/v1/accessibility/voice-guidance" "음성 안내 텍스트 생성" "200" '{"context":"button_click","language":"ko","params":{}}'

# 10. ARIA 라벨 생성 (공개 API - 200 예상)
test_endpoint "POST" "/api/v1/accessibility/aria-label" "ARIA 라벨 생성" "200" '{"elementType":"button","elementName":"submit","attributes":{}}'

# 11. 텍스트 간소화 (공개 API - 200 예상)
test_endpoint "POST" "/api/v1/accessibility/simplify-text" "텍스트 간소화" "200" '{"text":"복잡한 텍스트 내용입니다","targetLevel":"grade5"}'

# 12. 프로파일 적용 (공개 API - 200 예상)
test_endpoint "POST" "/api/v1/accessibility/settings/apply-profile?profileType=high_contrast" "접근성 프로파일 적용" "200"

# 13. 설정 동기화 (공개 API - 200 예상)
test_endpoint "POST" "/api/v1/accessibility/settings/sync" "설정 동기화" "200"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 14. 잘못된 HTTP 메서드 - 색상 구성표 조회 (POST - 405)
test_endpoint "POST" "/api/v1/accessibility/color-schemes" "잘못된 HTTP 메서드 - 색상 구성표 조회 (POST)" "405"

# 15. 잘못된 HTTP 메서드 - 설정 조회 (POST - 405)
test_endpoint "POST" "/api/v1/accessibility/settings" "잘못된 HTTP 메서드 - 설정 조회 (POST)" "405"

# 16. 잘못된 HTTP 메서드 - 터치 타겟 정보 (DELETE - 405)
test_endpoint "DELETE" "/api/v1/accessibility/touch-targets" "잘못된 HTTP 메서드 - 터치 타겟 정보 (DELETE)" "405"

# 17. 잘못된 HTTP 메서드 - 통계 조회 (PUT - 405)
test_endpoint "PUT" "/api/v1/accessibility/statistics" "잘못된 HTTP 메서드 - 통계 조회 (PUT)" "405"

# 18. 존재하지 않는 엔드포인트 - 루트 (404)
test_endpoint "GET" "/api/v1/accessibility" "존재하지 않는 엔드포인트 - 루트" "404"

# 19. 존재하지 않는 엔드포인트 - 테마 (404)
test_endpoint "GET" "/api/v1/accessibility/themes" "존재하지 않는 엔드포인트 - 테마" "404"

# 20. 존재하지 않는 엔드포인트 - 설정 (404)
test_endpoint "GET" "/api/v1/accessibility/config" "존재하지 않는 엔드포인트 - 설정" "404"

# 21. 존재하지 않는 엔드포인트 - 음성 설정 (404)
test_endpoint "GET" "/api/v1/accessibility/voice-guidance/settings" "존재하지 않는 엔드포인트 - 음성 설정" "404"

# 22. 빈 JSON 데이터 - 설정 업데이트 (200 예상)
test_endpoint "PUT" "/api/v1/accessibility/settings" "빈 JSON 데이터 - 설정 업데이트" "200" "{}"

# 23. 잘못된 JSON 형식 - 설정 업데이트 (400)
test_endpoint "PUT" "/api/v1/accessibility/settings" "잘못된 JSON 형식 - 설정 업데이트" "400" "invalid json"

# 24. 잘못된 JSON 형식 - 음성 안내 (400)
test_endpoint "POST" "/api/v1/accessibility/voice-guidance" "잘못된 JSON 형식 - 음성 안내" "400" "invalid json"

# 25. 필수 파라미터 누락 - 스크린 리더 힌트 (400)
test_endpoint "GET" "/api/v1/accessibility/screen-reader-hint?action=" "필수 파라미터 누락 - 스크린 리더 힌트" "400"

# 26. 모든 파라미터 누락 - 스크린 리더 힌트 (400)
test_endpoint "GET" "/api/v1/accessibility/screen-reader-hint" "모든 파라미터 누락 - 스크린 리더 힌트" "400"

# 27. 특수 문자 포함 경로 (404)
test_endpoint "GET" "/api/v1/accessibility/special@#$" "특수 문자 포함 경로" "404"

# 28. 매우 긴 경로 (404)
test_endpoint "GET" "/api/v1/accessibility/very/long/path/that/should/not/exist/anywhere" "매우 긴 경로" "404"

# 29. 빈 경로 세그먼트 (400)
test_endpoint "GET" "/api/v1/accessibility//empty//path" "빈 경로 세그먼트" "400"

# 30. OPTIONS 메서드 테스트 (200 또는 404)
test_endpoint "OPTIONS" "/api/v1/accessibility/settings" "OPTIONS 메서드 테스트" "200"

echo ""
echo "=========================================="
echo "📊 AccessibilityController 테스트 결과 요약"
echo "=========================================="
echo "총 테스트: $TOTAL_TESTS"
echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
echo -e "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}"

# 성공률 계산
success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo "성공률: $success_rate%"

if [[ $success_rate -eq 100 ]]; then
    echo -e "${GREEN}🎉  100% 성공! 모든 테스트 통과${NC}"
elif [[ $success_rate -ge 80 ]]; then
    echo -e "${YELLOW}⚠️   양호: $success_rate% 성공률${NC}"
else
    echo -e "${RED}❌  개선 필요: $success_rate% 성공률${NC}"
fi

echo "=========================================="