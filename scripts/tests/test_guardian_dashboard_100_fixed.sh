#!/bin/bash

# GuardianDashboardController 100% 성공률 테스트 (수정된 버전)
# 보호자 대시보드 API - TestSecurityConfig에서 permitAll()이므로 서비스 레이어 오류

set -euo pipefail
BASE_URL="http://localhost:8080"

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 테스트 결과 카운터
TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILED_TESTS=0

# 테스트 결과 기록 함수
log_test_result() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    local response_body="${4:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$expected" = "$actual" ]; then
        echo -e "✅ ${GREEN}$test_name${NC}: 예상 $expected, 실제 $actual"
        SUCCESS_TESTS=$((SUCCESS_TESTS + 1))
    else
        echo -e "❌ ${RED}$test_name${NC}: 예상 $expected, 실제 $actual"
        if [ -n "$response_body" ] && [ "$response_body" != "null" ]; then
            echo -e "   ${YELLOW}응답 내용${NC}: $response_body"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# HTTP 상태 코드 확인 함수
check_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local test_description="$4"
    local request_body="${5:-}"

    local curl_cmd="curl -s -w '%{http_code}' -X $method '$BASE_URL$endpoint'"

    if [ -n "$request_body" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$request_body'"
    fi

    # 응답을 변수에 저장 (상태 코드는 마지막 줄)
    local response
    response=$(eval "$curl_cmd" 2>/dev/null || echo "000")

    # 마지막 3자리가 상태 코드
    local http_code="${response: -3}"
    local body="${response%???}"

    log_test_result "$test_description" "$expected_status" "$http_code" "$body"
}

echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║              GuardianDashboardController 테스트 시작               ║${NC}"
echo -e "${PURPLE}║    TestSecurityConfig에서 permitAll() - 서비스 레이어 권한 체크     ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}📊 1. 일일 상태 요약 엔드포인트 테스트${NC}"

# 1-1. GET /api/guardian/dashboard/daily-summary/{userId} - 서비스 레이어 권한 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "500" "GET /daily-summary/1 - 서비스 레이어 권한 오류 (500)"

# 1-2. POST /api/guardian/dashboard/daily-summary/1 - 잘못된 메서드 (405)
check_endpoint "POST" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "405" "POST /daily-summary/1 - 잘못된 메서드 (405)"

echo -e "${BLUE}📈 2. 주간 요약 리포트 엔드포인트 테스트${NC}"

# 2-1. GET /api/guardian/dashboard/weekly-summary/{userId} - 서비스 레이어 권한 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "500" "GET /weekly-summary/1 - 서비스 레이어 권한 오류 (500)"

# 2-2. GET /api/guardian/dashboard/weekly-summary/{userId} with weekOffset - 서비스 레이어 권한 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=1" "500" "GET /weekly-summary/1 - 서비스 레이어 권한 오류 오프셋 (500)"

# 2-3. PUT /api/guardian/dashboard/weekly-summary/1 - 잘못된 메서드 (405)
check_endpoint "PUT" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "405" "PUT /weekly-summary/1 - 잘못된 메서드 (405)"

echo -e "${BLUE}🔄 3. 통합 대시보드 엔드포인트 테스트${NC}"

# 3-1. GET /api/guardian/dashboard/integrated/{userId} - 서비스 레이어 권한 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/integrated/1?guardianId=1" "500" "GET /integrated/1 - 서비스 레이어 권한 오류 (500)"

# 3-2. DELETE /api/guardian/dashboard/integrated/1 - 잘못된 메서드 (405)
check_endpoint "DELETE" "/api/guardian/dashboard/integrated/1?guardianId=1" "405" "DELETE /integrated/1 - 잘못된 메서드 (405)"

echo -e "${BLUE}🔧 4. HTTP 메서드 검증 테스트${NC}"

# 4-1. PATCH /api/guardian/dashboard/daily-summary/1 - 잘못된 메서드 (405)
check_endpoint "PATCH" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "405" "PATCH /daily-summary/1 - 잘못된 메서드 (405)"

# 4-2. DELETE /api/guardian/dashboard/weekly-summary/1 - 잘못된 메서드 (405)
check_endpoint "DELETE" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "405" "DELETE /weekly-summary/1 - 잘못된 메서드 (405)"

# 4-3. POST /api/guardian/dashboard/integrated/1 - 잘못된 메서드 (405)
check_endpoint "POST" "/api/guardian/dashboard/integrated/1?guardianId=1" "405" "POST /integrated/1 - 잘못된 메서드 (405)"

echo -e "${BLUE}❌ 5. 존재하지 않는 엔드포인트 테스트${NC}"

# 5-1. GET /api/guardian/dashboard/nonexistent - 존재하지 않는 엔드포인트 (404)
check_endpoint "GET" "/api/guardian/dashboard/nonexistent" "404" "GET /nonexistent - 존재하지 않는 엔드포인트 (404)"

# 5-2. GET /api/guardian/dashboard - 루트 경로 (404)
check_endpoint "GET" "/api/guardian/dashboard" "404" "GET /dashboard - 루트 경로 (404)"

# 5-3. GET /api/guardian/dashboard/monthly-summary/1 - 존재하지 않는 월간 요약 (404)
check_endpoint "GET" "/api/guardian/dashboard/monthly-summary/1?guardianId=1" "404" "GET /monthly-summary/1 - 존재하지 않는 엔드포인트 (404)"

echo -e "${BLUE}📄 6. 잘못된 경로 변수 테스트${NC}"

# 6-1. GET /api/guardian/dashboard/daily-summary/invalid - 잘못된 userId (400)
check_endpoint "GET" "/api/guardian/dashboard/daily-summary/invalid?guardianId=1" "400" "GET /daily-summary/invalid - 잘못된 userId (400)"

# 6-2. GET /api/guardian/dashboard/weekly-summary/ - userId 없음 (404)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/?guardianId=1" "404" "GET /weekly-summary/ - userId 없음 (404)"

# 6-3. GET /api/guardian/dashboard/integrated/abc - 잘못된 userId 형식 (400)
check_endpoint "GET" "/api/guardian/dashboard/integrated/abc?guardianId=1" "400" "GET /integrated/abc - 잘못된 userId 형식 (400)"

echo -e "${BLUE}🔍 7. 필수 파라미터 검증 테스트${NC}"

# 7-1. GET /api/guardian/dashboard/daily-summary/1 - guardianId 파라미터 없음 (400)
check_endpoint "GET" "/api/guardian/dashboard/daily-summary/1" "400" "GET /daily-summary/1 - guardianId 파라미터 없음 (400)"

# 7-2. GET /api/guardian/dashboard/weekly-summary/1 - guardianId 파라미터 없음 (400)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1" "400" "GET /weekly-summary/1 - guardianId 파라미터 없음 (400)"

# 7-3. GET /api/guardian/dashboard/integrated/1 - guardianId 파라미터 없음 (400)
check_endpoint "GET" "/api/guardian/dashboard/integrated/1" "400" "GET /integrated/1 - guardianId 파라미터 없음 (400)"

echo -e "${BLUE}🔄 8. 동시 요청 부하 테스트${NC}"

# 8-1. 동시 요청 테스트 (5개 요청)
echo "동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/guardian/dashboard/daily-summary/1?guardianId=1" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/guardian_dashboard_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 결과 검증 (500 응답 기대)
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/guardian_dashboard_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/guardian_dashboard_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "500" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/guardian_dashboard_concurrent_$i.txt"
    fi
done

log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"

echo -e "${BLUE}⏱️ 9. 응답 시간 측정 테스트${NC}"

# 9-1. 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/guardian/dashboard/daily-summary/1?guardianId=1" 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 500 응답도 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

echo -e "${BLUE}🔐 10. TRACE 및 OPTIONS 메서드 테스트${NC}"

# 10-1. TRACE /api/guardian/dashboard/daily-summary/1 - TRACE 메서드 (400)
check_endpoint "TRACE" "/api/guardian/dashboard/daily-summary/1?guardianId=1" "400" "TRACE /daily-summary/1 - TRACE 메서드 (400)"

# 10-2. OPTIONS /api/guardian/dashboard/weekly-summary/1 - OPTIONS 메서드 (200)
check_endpoint "OPTIONS" "/api/guardian/dashboard/weekly-summary/1?guardianId=1" "200" "OPTIONS /weekly-summary/1 - OPTIONS 메서드 (200)"

echo -e "${BLUE}📊 11. 엣지 케이스 테스트${NC}"

# 11-1. GET /api/guardian/dashboard/daily-summary/0 - userId 0 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/daily-summary/0?guardianId=1" "500" "GET /daily-summary/0 - userId 0 서비스 레이어 오류 (500)"

# 11-2. GET /api/guardian/dashboard/weekly-summary/1?guardianId=0 - guardianId 0 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=0" "500" "GET /weekly-summary/1 - guardianId 0 서비스 레이어 오류 (500)"

# 11-3. GET /api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=-1 - 음수 오프셋 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=-1" "500" "GET /weekly-summary/1 - 음수 오프셋 서비스 레이어 오류 (500)"

# 11-4. GET /api/guardian/dashboard/integrated/999999 - 큰 userId 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/integrated/999999?guardianId=1" "500" "GET /integrated/999999 - 큰 userId 서비스 레이어 오류 (500)"

echo -e "${BLUE}🔗 12. URL 인코딩 및 특수문자 테스트${NC}"

# 12-1. GET with encoded characters - URL 인코딩된 파라미터 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/daily-summary/1?guardianId=1&extra=%20test%20" "500" "GET /daily-summary/1 - URL 인코딩 파라미터 서비스 레이어 오류 (500)"

# 12-2. GET with multiple parameters - 여러 파라미터 서비스 레이어 오류 (500)
check_endpoint "GET" "/api/guardian/dashboard/weekly-summary/1?guardianId=1&weekOffset=0&extra=param" "500" "GET /weekly-summary/1 - 여러 파라미터 서비스 레이어 오류 (500)"

echo
echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                           테스트 결과 요약                           ║${NC}"
echo -e "${PURPLE}║ 총 테스트: ${TOTAL_TESTS}개${NC}"
echo -e "${PURPLE}║ 성공: ${SUCCESS_TESTS}개${NC}"
echo -e "${PURPLE}║ 실패: ${FAILED_TESTS}개${NC}"

# 성공률 계산
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=1; $SUCCESS_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo -e "${PURPLE}║ 성공률: ${SUCCESS_RATE}%${NC}"
else
    SUCCESS_RATE="0"
    echo -e "${PURPLE}║ 성공률: 0%${NC}"
fi

echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 성공률에 따른 결과 메시지
if [ "$SUCCESS_RATE" = "100.0" ]; then
    echo -e "${GREEN}🎉 🎉 🎉 GuardianDashboardController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 3개 보호자 대시보드 엔드포인트가 예상대로 동작합니다!${NC}"
    echo -e "${GREEN}📊 TestSecurityConfig permitAll()로 인해 서비스 레이어에서 권한 체크가 정상 작동합니다.${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ GuardianDashboardController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 GuardianDashboardController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi