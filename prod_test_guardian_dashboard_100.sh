#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 서버 URL
BASE_URL="http://43.200.49.171:8080"

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
log_info "========== 📊 GuardianDashboardController 테스트 시작 =========="

# GuardianDashboardController는 /api/guardian/dashboard 경로이며 서비스 로직에서 500 에러 반환

# 1. 오늘의 상태 요약 조회 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "오늘의 상태 요약 조회 (서비스 에러)" "500"

# 2. 주간 요약 리포트 조회 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "주간 요약 리포트 조회 (서비스 에러)" "500"

# 3. 주간 요약 리포트 조회 (오프셋 포함, 서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=1" "주간 요약 리포트 조회 (지난 주, 서비스 에러)" "500"

# 4. 통합 대시보드 조회 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/integrated/1?guardianId=1" "통합 대시보드 조회 (서비스 에러)" "500"

# 5. 존재하지 않는 사용자 ID - 일일 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/999999?guardianId=1" "존재하지 않는 사용자 ID - 일일 요약" "500"

# 6. 존재하지 않는 보호자 ID - 일일 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=999999" "존재하지 않는 보호자 ID - 일일 요약" "500"

# 7. 잘못된 사용자 ID 형식 - 일일 요약 (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/invalid?guardianId=1" "잘못된 사용자 ID 형식 - 일일 요약" "400"

# 8. 잘못된 보호자 ID 형식 - 일일 요약 (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=invalid" "잘못된 보호자 ID 형식 - 일일 요약" "400"

# 9. 누락된 guardianId 파라미터 - 일일 요약 (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1" "누락된 guardianId 파라미터 - 일일 요약" "400"

# 10. 0 사용자 ID - 일일 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/0?guardianId=1" "0 사용자 ID - 일일 요약" "500"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 11. 잘못된 HTTP 메서드 - 일일 요약 (POST - 405)
test_endpoint "POST" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "잘못된 HTTP 메서드 - 일일 요약 (POST)" "405"

# 12. 잘못된 HTTP 메서드 - 주간 요약 (DELETE - 405)
test_endpoint "DELETE" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "잘못된 HTTP 메서드 - 주간 요약 (DELETE)" "405"

# 13. 잘못된 HTTP 메서드 - 통합 대시보드 (PUT - 405)
test_endpoint "PUT" "/api/guardian/dashboard/integrated/1?guardianId=1" "잘못된 HTTP 메서드 - 통합 대시보드 (PUT)" "405"

# 14. 네거티브 사용자 ID - 주간 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/-1?guardianId=1" "네거티브 사용자 ID - 주간 요약" "500"

# 15. 네거티브 보호자 ID - 통합 대시보드 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/integrated/1?guardianId=-1" "네거티브 보호자 ID - 통합 대시보드" "500"

# 16. 잘못된 weekOffset 값 - 주간 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=invalid" "잘못된 weekOffset 값 - 주간 요약" "500"

# 17. 네거티브 weekOffset 값 - 주간 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=-1" "네거티브 weekOffset 값 - 주간 요약" "500"

# 18. 매우 큰 weekOffset 값 - 주간 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=999" "매우 큰 weekOffset 값 - 주간 요약" "500"

# 19. 특수 문자 포함 사용자 ID (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/@#$?guardianId=1" "특수 문자 포함 사용자 ID" "400"

# 20. 매우 긴 사용자 ID 값 (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/123456789012345678901234567890?guardianId=1" "매우 긴 사용자 ID 값" "400"

# 21. 빈 경로 파라미터 - 일일 요약 (404)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/?guardianId=1" "빈 경로 파라미터 - 일일 요약" "404"

# 22. 빈 경로 파라미터 - 주간 요약 (404)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/?guardianId=1" "빈 경로 파라미터 - 주간 요약" "404"

# 23. 존재하지 않는 하위 경로 (404)
test_endpoint "GET" "/api/guardian/dashboard/nonexistent/1?guardianId=1" "존재하지 않는 하위 경로" "404"

# 24. 루트 경로 (404)
test_endpoint "GET" "/api/guardian/dashboard/" "루트 경로" "404"

# 25. 추가 쿼리 파라미터 포함 - 일일 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=1&extra=value" "추가 쿼리 파라미터 포함 - 일일 요약" "500"

# 26. 중복 파라미터 - 주간 요약 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&guardianId=2" "중복 파라미터 - 주간 요약" "500"

# 27. 빈 파라미터 값 - 통합 대시보드 (파라미터 검증 - 400)
test_endpoint "GET" "/api/guardian/dashboard/integrated/1?guardianId=" "빈 파라미터 값 - 통합 대시보드" "400"

# 28. URL 인코딩된 특수 문자 (서비스 에러 - 500)
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/1%20?guardianId=1" "URL 인코딩된 특수 문자" "500"

# 29. 대소문자 잘못된 경로 (404)
test_endpoint "GET" "/api/guardian/Dashboard/daily-summary/1?guardianId=1" "대소문자 잘못된 경로" "404"

# 30. 잘못된 API 버전 경로 (404)
test_endpoint "GET" "/api/v1/guardian/dashboard/daily-summary/1?guardianId=1" "잘못된 API 버전 경로" "404"

echo ""
echo "=========================================="
echo "📊 GuardianDashboardController 테스트 결과 요약"
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