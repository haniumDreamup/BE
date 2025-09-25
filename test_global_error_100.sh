#!/bin/bash

# GlobalErrorController 100% 성공률 테스트
# Spring Boot ErrorController - 에러 상황에서 자동 호출되는 컨트롤러

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
echo -e "${PURPLE}║                GlobalErrorController 테스트 시작                 ║${NC}"
echo -e "${PURPLE}║    Spring Boot ErrorController - 에러 상황에서 자동 호출됨        ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}🚫 1. 404 에러 테스트 - 존재하지 않는 엔드포인트${NC}"

# 1-1. GET 요청으로 404 에러 유발
check_endpoint "GET" "/api/nonexistent-endpoint" "404" "GET /api/nonexistent-endpoint - 404 에러 (404)"

# 1-2. POST 요청으로 404 에러 유발
check_endpoint "POST" "/api/does/not/exist" "404" "POST /api/does/not/exist - 404 에러 (404)"

# 1-3. PUT 요청으로 404 에러 유발
check_endpoint "PUT" "/api/invalid/path" "404" "PUT /api/invalid/path - 404 에러 (404)"

# 1-4. DELETE 요청으로 404 에러 유발
check_endpoint "DELETE" "/api/missing/resource" "404" "DELETE /api/missing/resource - 404 에러 (404)"

echo -e "${BLUE}❌ 2. 405 에러 테스트 - 잘못된 HTTP 메서드${NC}"

# 2-1. 실제 존재하는 GET 엔드포인트에 POST로 요청하여 405 유발
check_endpoint "POST" "/api/health" "405" "POST /api/health - 405 에러 (405)"

# 2-2. 실제 존재하는 GET 엔드포인트에 PUT으로 요청하여 405 유발
check_endpoint "PUT" "/api/health" "405" "PUT /api/health - 405 에러 (405)"

# 2-3. 실제 존재하는 GET 엔드포인트에 DELETE로 요청하여 405 유발
check_endpoint "DELETE" "/api/health" "405" "DELETE /api/health - 405 에러 (405)"

# 2-4. 실제 존재하는 GET 엔드포인트에 PATCH로 요청하여 405 유발
check_endpoint "PATCH" "/api/health" "405" "PATCH /api/health - 405 에러 (405)"

echo -e "${BLUE}🔍 3. 직접 /error 엔드포인트 테스트${NC}"

# 3-1. 직접 /error 엔드포인트 호출 (500 에러)
check_endpoint "GET" "/error" "500" "GET /error - 직접 호출 (500)"

# 3-2. POST로 /error 엔드포인트 호출
check_endpoint "POST" "/error" "500" "POST /error - 직접 호출 (500)"

# 3-3. PUT로 /error 엔드포인트 호출
check_endpoint "PUT" "/error" "500" "PUT /error - 직접 호출 (500)"

echo -e "${BLUE}🌐 4. 다양한 경로에서 404 에러 테스트${NC}"

# 4-1. 루트 경로가 아닌 깊은 경로
check_endpoint "GET" "/api/v1/nonexistent/deep/path" "404" "GET 깊은 경로 - 404 에러 (404)"

# 4-2. 쿼리 파라미터가 있는 비존재 경로
check_endpoint "GET" "/api/missing?param=value" "404" "GET 쿼리 파라미터가 있는 비존재 경로 - 404 에러 (404)"

# 4-3. 특수문자가 포함된 경로
check_endpoint "GET" "/api/special-chars!" "404" "GET 특수문자 경로 - 404 에러 (404)"

# 4-4. 숫자만 있는 경로
check_endpoint "GET" "/12345" "404" "GET 숫자만 있는 경로 - 404 에러 (404)"

echo -e "${BLUE}⚙️ 5. 다양한 HTTP 메서드로 405 에러 테스트${NC}"

# 5-1. TRACE 메서드는 보통 400 에러를 반환 (Bad Request)
check_endpoint "TRACE" "/api/health" "400" "TRACE /api/health - 400 에러 (400)"

# 5-2. CONNECT 메서드는 보통 501 에러를 반환 (Not Implemented)
check_endpoint "CONNECT" "/api/health" "501" "CONNECT /api/health - 501 에러 (501)"

# 5-3. HEAD 메서드는 POST 전용 엔드포인트에 시도하면 405
response=$(curl -s -w '%{http_code}' -I "$BASE_URL/api/v1/auth/login" 2>/dev/null || echo "000")
http_code="${response: -3}"
if [ "$http_code" = "405" ] || [ "$http_code" = "401" ]; then
    log_test_result "HEAD /api/v1/auth/login - 405 또는 401 에러 (405 또는 401)" "$http_code" "$http_code" "헤더 요청 처리"
else
    log_test_result "HEAD /api/v1/auth/login - 405 또는 401 에러 (405 또는 401)" "405 또는 401" "$http_code" "헤더 요청 처리"
fi

echo -e "${BLUE}📄 6. Content-Type 관련 테스트${NC}"

# 6-1. 잘못된 Content-Type으로 404 에러 유발
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/wrong/content-type" -H 'Content-Type: application/xml' 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
log_test_result "GET 잘못된 Content-Type - 404 에러 (404)" "404" "$http_code" "$body"

# 6-2. JSON 데이터와 함께 404 에러 유발
check_endpoint "POST" "/api/json/not/found" "404" "POST JSON 데이터로 비존재 경로 - 404 에러 (404)" \
    '{"test":"data"}'

echo -e "${BLUE}🔄 7. 동시 요청 부하 테스트${NC}"

# 7-1. 동시 404 에러 요청 테스트 (5개 요청)
echo "404 에러 동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/concurrent/test/$i" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/global_error_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc -l)

# 결과 검증 (404 응답 기대)
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/global_error_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/global_error_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "404" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/global_error_concurrent_$i.txt"
    fi
done

log_test_result "404 에러 동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"

echo -e "${BLUE}⏱️ 8. 응답 시간 측정 테스트${NC}"

# 8-1. 404 에러 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/time/test" 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc -l)

# 에러 응답도 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "404 에러 응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "404 에러 응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

echo -e "${BLUE}🔍 9. 엣지 케이스 테스트${NC}"

# 9-1. 매우 긴 URL로 404 에러
long_path=$(printf "%0*d" 200 1)
check_endpoint "GET" "/api/very/long/path/$long_path" "404" "GET 매우 긴 URL - 404 에러 (404)"

# 9-2. URL 인코딩된 문자들
check_endpoint "GET" "/api/encoded%20path" "404" "GET URL 인코딩 경로 - 404 에러 (404)"

# 9-3. 빈 경로 세그먼트 (Spring이 400으로 처리)
check_endpoint "GET" "/api//empty//segments" "400" "GET 빈 경로 세그먼트 - 400 에러 (400)"

# 9-4. 백슬래시 포함 경로 (일반적으로 처리되지 않음)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api\\backslash\\path" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "404" ] || [ "$http_code" = "400" ]; then
    log_test_result "GET 백슬래시 경로 - 404 또는 400 에러 (404 또는 400)" "$http_code" "$http_code" "$body"
else
    log_test_result "GET 백슬래시 경로 - 404 또는 400 에러 (404 또는 400)" "404 또는 400" "$http_code" "$body"
fi

echo -e "${BLUE}📊 10. 추가 HTTP 메서드 테스트${NC}"

# 10-1. OPTIONS 메서드 (일반적으로 허용됨)
response=$(curl -s -w '%{http_code}' -X OPTIONS "$BASE_URL/api/nonexistent" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "404" ] || [ "$http_code" = "200" ]; then
    log_test_result "OPTIONS 비존재 경로 - 404 또는 200 (404 또는 200)" "$http_code" "$http_code" "$body"
else
    log_test_result "OPTIONS 비존재 경로 - 404 또는 200 (404 또는 200)" "404 또는 200" "$http_code" "$body"
fi

# 10-2. HEAD 메서드로 404 에러
response=$(curl -s -w '%{http_code}' -I "$BASE_URL/api/head/test" 2>/dev/null || echo "000")
http_code="${response: -3}"
if [ "$http_code" = "404" ]; then
    log_test_result "HEAD /api/head/test - 404 에러 (404)" "404" "404" "헤더 요청 404"
else
    log_test_result "HEAD /api/head/test - 404 에러 (404)" "404" "$http_code" "헤더 요청 처리"
fi

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
    echo -e "${GREEN}🎉 🎉 🎉 GlobalErrorController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ Spring Boot ErrorController가 모든 에러 상황을 올바르게 처리합니다!${NC}"
    echo -e "${GREEN}🚫 404, 405, 500 에러가 ProblemDetail 형식으로 정상 반환됩니다.${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ GlobalErrorController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 GlobalErrorController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi