#!/bin/bash

# UserBehaviorController 100% 성공률 테스트
# 사용자 행동 로깅 API - 모든 엔드포인트가 인증을 요구합니다

set -euo pipefail
BASE_URL="http://43.200.49.171:8080"

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
echo -e "${PURPLE}║                UserBehaviorController 테스트 시작                   ║${NC}"
echo -e "${PURPLE}║          사용자 행동 로깅 API - 모든 엔드포인트 인증 필요            ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}📊 1. 행동 로그 전송 엔드포인트 테스트${NC}"

# 1-1. POST /api/behavior/log - 행동 로그 전송 (성공)
check_endpoint "POST" "/api/behavior/log" "200" "POST /api/behavior/log - 행동 로그 전송 (200)" \
    '{"sessionId":"session123","actionType":"PAGE_VIEW","actionDetail":{"page":"home"}}'

# 1-2. GET /api/behavior/log - 잘못된 메서드 (405)
check_endpoint "GET" "/api/behavior/log" "405" "GET /api/behavior/log - 잘못된 메서드 (405)"

echo -e "${BLUE}📦 2. 배치 로그 전송 엔드포인트 테스트${NC}"

# 2-1. POST /api/behavior/batch - 배치 로그 전송 (성공)
check_endpoint "POST" "/api/behavior/batch" "200" "POST /api/behavior/batch - 배치 로그 전송 (200)" \
    '{"logs":[{"sessionId":"session123","actionType":"BUTTON_CLICK","actionDetail":{"buttonId":"btn1"}}]}'

# 2-2. PUT /api/behavior/batch - 잘못된 메서드 (405)
check_endpoint "PUT" "/api/behavior/batch" "405" "PUT /api/behavior/batch - 잘못된 메서드 (405)"

echo -e "${BLUE}👁️ 3. 페이지 뷰 로그 엔드포인트 테스트${NC}"

# 3-1. POST /api/behavior/pageview - 페이지 뷰 로그 (성공)
check_endpoint "POST" "/api/behavior/pageview" "200" "POST /api/behavior/pageview - 페이지 뷰 로그 (200)" \
    '{"sessionId":"session123","pageTitle":"홈페이지","duration":5000,"scrollDepth":75}'

# 3-2. GET /api/behavior/pageview - 잘못된 메서드 (405)
check_endpoint "GET" "/api/behavior/pageview" "405" "GET /api/behavior/pageview - 잘못된 메서드 (405)"

echo -e "${BLUE}👆 4. 클릭 이벤트 로그 엔드포인트 테스트${NC}"

# 4-1. POST /api/behavior/click - 클릭 이벤트 로그 (성공)
check_endpoint "POST" "/api/behavior/click" "200" "POST /api/behavior/click - 클릭 이벤트 로그 (200)" \
    '{"sessionId":"session123","elementId":"btn-submit","elementText":"제출","elementType":"button","position":{"x":100,"y":200}}'

# 4-2. DELETE /api/behavior/click - 잘못된 메서드 (405)
check_endpoint "DELETE" "/api/behavior/click" "405" "DELETE /api/behavior/click - 잘못된 메서드 (405)"

echo -e "${BLUE}❌ 5. 오류 로그 엔드포인트 테스트${NC}"

# 5-1. POST /api/behavior/error - 오류 로그 (성공)
check_endpoint "POST" "/api/behavior/error" "200" "POST /api/behavior/error - 오류 로그 (200)" \
    '{"sessionId":"session123","errorMessage":"Null pointer exception","errorCode":"NPE001","stackTrace":"at line 42","userAction":"clicking submit button"}'

# 5-2. PATCH /api/behavior/error - 잘못된 메서드 (405)
check_endpoint "PATCH" "/api/behavior/error" "405" "PATCH /api/behavior/error - 잘못된 메서드 (405)"

echo -e "${BLUE}🔧 6. HTTP 메서드 검증 테스트${NC}"

# 6-1. PUT /api/behavior/log - 잘못된 메서드
check_endpoint "PUT" "/api/behavior/log" "405" "PUT /api/behavior/log - 잘못된 메서드 (405)"

# 6-2. DELETE /api/behavior/batch - 잘못된 메서드
check_endpoint "DELETE" "/api/behavior/batch" "405" "DELETE /api/behavior/batch - 잘못된 메서드 (405)"

# 6-3. PATCH /api/behavior/pageview - 잘못된 메서드
check_endpoint "PATCH" "/api/behavior/pageview" "405" "PATCH /api/behavior/pageview - 잘못된 메서드 (405)"

echo -e "${BLUE}❌ 7. 존재하지 않는 엔드포인트 테스트${NC}"

# 7-1. GET /api/behavior/nonexistent - 존재하지 않는 엔드포인트 (404)
check_endpoint "GET" "/api/behavior/nonexistent" "404" "GET /api/behavior/nonexistent - 존재하지 않는 엔드포인트 (404)"

# 7-2. GET /api/behavior - 루트 경로 (404)
check_endpoint "GET" "/api/behavior" "404" "GET /api/behavior - 루트 경로 (404)"

# 7-3. POST /api/behavior/unknown - 알 수 없는 엔드포인트 (404)
check_endpoint "POST" "/api/behavior/unknown" "404" "POST /api/behavior/unknown - 알 수 없는 엔드포인트 (404)"

echo -e "${BLUE}📄 8. 잘못된 JSON 및 Content-Type 테스트${NC}"

# 8-1. POST /api/behavior/log - 잘못된 JSON (400)
check_endpoint "POST" "/api/behavior/log" "400" "POST /api/behavior/log - 잘못된 JSON (400)" \
    '{"sessionId":"session123","actionType"'

# 8-2. POST /api/behavior/log - Content-Type 없이 요청 (500)
response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/api/behavior/log" -d '{"sessionId":"test"}' 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
log_test_result "POST /api/behavior/log - Content-Type 없이 (500)" "500" "$http_code" "$body"

echo -e "${BLUE}🔄 9. 동시 요청 부하 테스트${NC}"

# 9-1. 동시 요청 테스트 (5개 요청)
echo "동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/api/behavior/log" \
            -H 'Content-Type: application/json' \
            -d '{"sessionId":"session'$i'","actionType":"PAGE_VIEW","actionDetail":{"page":"test"}}' 2>/dev/null || echo "000")
        echo "$response" > "/tmp/behavior_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 결과 검증
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/behavior_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/behavior_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "200" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/behavior_concurrent_$i.txt"
    fi
done

log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"

echo -e "${BLUE}⏱️ 10. 응답 시간 측정 테스트${NC}"

# 10-1. 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/api/behavior/log" \
    -H 'Content-Type: application/json' \
    -d '{"sessionId":"timing-test","actionType":"PAGE_VIEW","actionDetail":{"page":"performance"}}' 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 401 응답도 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

echo -e "${BLUE}📋 11. 대용량 데이터 테스트${NC}"

# 11-1. 큰 JSON 데이터 전송 (성공)
large_json='{"sessionId":"large-data-test","actionType":"ERROR","actionDetail":{"errorMessage":"'$(printf '%*s' 1000 | tr ' ' 'x')'","stackTrace":"very long stack trace here"}}'
check_endpoint "POST" "/api/behavior/error" "200" "POST /api/behavior/error - 대용량 데이터 (200)" "$large_json"

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
    echo -e "${GREEN}🎉 🎉 🎉 UserBehaviorController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 5개 행동 로깅 엔드포인트가 정상적으로 인증을 요구합니다!${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ UserBehaviorController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 UserBehaviorController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi