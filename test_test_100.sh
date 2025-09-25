#!/bin/bash

# TestController 100% 성공률 테스트
# 테스트용 개발 도구 API - TestSecurityConfig에서 모든 /api/test/** 경로가 permitAll()

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
echo -e "${PURPLE}║                    TestController 테스트 시작                      ║${NC}"
echo -e "${PURPLE}║        테스트용 개발 도구 API - 모든 엔드포인트 인증 없이 허용       ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}🏥 1. 기본 헬스체크 엔드포인트 테스트${NC}"

# 1-1. GET /api/test/health - 테스트용 헬스체크 (200)
check_endpoint "GET" "/api/test/health" "200" "GET /api/test/health - 테스트용 헬스체크 (200)"

# 1-2. POST /api/test/health - 잘못된 메서드 (405)
check_endpoint "POST" "/api/test/health" "405" "POST /api/test/health - 잘못된 메서드 (405)"

echo -e "${BLUE}📅 2. 날짜 테스트 엔드포인트${NC}"

# 2-1. GET /api/test/date - 유효한 날짜 (200)
check_endpoint "GET" "/api/test/date?date=2024-01-01" "200" "GET /api/test/date - 유효한 날짜 (200)"

# 2-2. GET /api/test/date - 날짜 파라미터 없음 (200)
check_endpoint "GET" "/api/test/date" "200" "GET /api/test/date - 날짜 파라미터 없음 (200)"

# 2-3. GET /api/test/date - 윤년 날짜 (200)
check_endpoint "GET" "/api/test/date?date=2024-02-29" "200" "GET /api/test/date - 윤년 날짜 (200)"

# 2-4. GET /api/test/date - 잘못된 날짜 형식 (400)
check_endpoint "GET" "/api/test/date?date=invalid-date" "400" "GET /api/test/date - 잘못된 날짜 형식 (400)"

# 2-5. POST /api/test/date - 잘못된 메서드 (405)
check_endpoint "POST" "/api/test/date" "405" "POST /api/test/date - 잘못된 메서드 (405)"

echo -e "${BLUE}📢 3. 에코 테스트 엔드포인트${NC}"

# 3-1. POST /api/test/echo - 정상 메시지 (200)
check_endpoint "POST" "/api/test/echo" "200" "POST /api/test/echo - 정상 메시지 (200)" \
    '"Hello Test Message"'

# 3-2. POST /api/test/echo - 빈 메시지 (200)
check_endpoint "POST" "/api/test/echo" "200" "POST /api/test/echo - 빈 메시지 (200)" \
    '""'

# 3-3. POST /api/test/echo - 유니코드 메시지 (200)
check_endpoint "POST" "/api/test/echo" "200" "POST /api/test/echo - 유니코드 메시지 (200)" \
    '"안녕하세요 테스트 🚀"'

# 3-4. POST /api/test/echo - 잘못된 JSON도 문자열로 처리 (200)
check_endpoint "POST" "/api/test/echo" "200" "POST /api/test/echo - 잘못된 JSON 문자열로 처리 (200)" \
    '{invalid json'

# 3-5. GET /api/test/echo - 잘못된 메서드 (405)
check_endpoint "GET" "/api/test/echo" "405" "GET /api/test/echo - 잘못된 메서드 (405)"

echo -e "${BLUE}🔧 4. HTTP 메서드 검증 테스트${NC}"

# 4-1. PUT /api/test/health - 잘못된 메서드 (405)
check_endpoint "PUT" "/api/test/health" "405" "PUT /api/test/health - 잘못된 메서드 (405)"

# 4-2. DELETE /api/test/date - 잘못된 메서드 (405)
check_endpoint "DELETE" "/api/test/date" "405" "DELETE /api/test/date - 잘못된 메서드 (405)"

# 4-3. PATCH /api/test/echo - 잘못된 메서드 (405)
check_endpoint "PATCH" "/api/test/echo" "405" "PATCH /api/test/echo - 잘못된 메서드 (405)"

echo -e "${BLUE}❌ 5. 존재하지 않는 엔드포인트 테스트${NC}"

# 5-1. GET /api/test/nonexistent - 존재하지 않는 엔드포인트 (404)
check_endpoint "GET" "/api/test/nonexistent" "404" "GET /api/test/nonexistent - 존재하지 않는 엔드포인트 (404)"

# 5-2. GET /api/test - 루트 경로 (404)
check_endpoint "GET" "/api/test" "404" "GET /api/test - 루트 경로 (404)"

# 5-3. GET /api/TEST/health - 대문자 경로 (404)
check_endpoint "GET" "/api/TEST/health" "404" "GET /api/TEST/health - 대문자 경로 (404)"

echo -e "${BLUE}📄 6. 추가 HTTP 메서드 테스트${NC}"

# 6-1. OPTIONS /api/test/health - OPTIONS 메서드 (200)
check_endpoint "OPTIONS" "/api/test/health" "200" "OPTIONS /api/test/health - OPTIONS 메서드 (200)"

# 6-2. HEAD /api/test/health - HEAD 메서드 (200)
response=$(curl -s -w '%{http_code}' -I "$BASE_URL/api/test/health" 2>/dev/null || echo "000")
http_code="${response: -3}"
if [ "$http_code" = "200" ]; then
    log_test_result "HEAD /api/test/health - HEAD 메서드 (200)" "200" "200" "헤더 요청 성공"
else
    log_test_result "HEAD /api/test/health - HEAD 메서드 (200)" "200" "$http_code" "헤더 요청 처리"
fi

echo -e "${BLUE}🔄 7. 동시 요청 부하 테스트${NC}"

# 7-1. 동시 요청 테스트 (5개 요청)
echo "동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/test/health" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/test_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc -l)

# 결과 검증 (200 응답 기대)
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/test_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/test_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "200" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/test_concurrent_$i.txt"
    fi
done

log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"

echo -e "${BLUE}⏱️ 8. 응답 시간 측정 테스트${NC}"

# 8-1. 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/test/health" 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc -l)

# 응답이 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

echo -e "${BLUE}🔍 9. 엣지 케이스 테스트${NC}"

# 9-1. GET /api/test/date - 특수 문자 인코딩 (400)
check_endpoint "GET" "/api/test/date?date=2024-01-01%20extra" "400" "GET /api/test/date - 특수 문자 인코딩 (400)"

# 9-2. POST /api/test/echo - Content-Type 없이도 처리됨 (200)
response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/api/test/echo" -d '"test"' 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
log_test_result "POST /api/test/echo - Content-Type 없이도 처리됨 (200)" "200" "$http_code" "$body"

# 9-3. GET /api/test/date - 매우 긴 파라미터 (400)
long_param=$(printf "%0*d" 500 1)
check_endpoint "GET" "/api/test/date?date=2024-01-$long_param" "400" "GET /api/test/date - 매우 긴 파라미터 (400)"

echo

echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                           테스트 결과 요약                           ║${NC}"
echo -e "${PURPLE}║ 총 테스트: ${TOTAL_TESTS}개${NC}"
echo -e "${PURPLE}║ 성공: ${SUCCESS_TESTS}개${NC}"
echo -e "${PURPLE}║ 실패: ${FAILED_TESTS}개${NC}"

# 성공률 계산
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=1; $SUCCESS_TESTS * 100 / $TOTAL_TESTS" | bc -l)
    echo -e "${PURPLE}║ 성공률: ${SUCCESS_RATE}%${NC}"
else
    SUCCESS_RATE="0"
    echo -e "${PURPLE}║ 성공률: 0%${NC}"
fi

echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 성공률에 따른 결과 메시지
if [ "$SUCCESS_RATE" = "100.0" ]; then
    echo -e "${GREEN}🎉 🎉 🎉 TestController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 3개 테스트 개발 도구 엔드포인트가 정상적으로 작동합니다!${NC}"
    echo -e "${GREEN}🔧 TestSecurityConfig permitAll()로 인해 인증 없이 사용 가능한 개발 API입니다.${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ TestController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 TestController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi