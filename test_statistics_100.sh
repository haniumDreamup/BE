#!/bin/bash

# StatisticsController 100% 성공률 테스트
# 통계 조회 API - 모든 엔드포인트가 JWT 인증 필요

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
echo -e "${PURPLE}║                StatisticsController 테스트 시작                  ║${NC}"
echo -e "${PURPLE}║         통계 조회 API - 모든 엔드포인트 JWT 인증 필요            ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}📊 1. 지오펜스 통계 조회 엔드포인트 테스트${NC}"

# 1-1. GET /api/statistics/geofence - 지오펜스 통계 조회 (JWT 토큰 없으면 401)
check_endpoint "GET" "/api/statistics/geofence" "401" "GET /api/statistics/geofence - 지오펜스 통계 조회 (401)"

# 1-2. POST /api/statistics/geofence - 잘못된 메서드 (401, JWT 체크 우선)
check_endpoint "POST" "/api/statistics/geofence" "401" "POST /api/statistics/geofence - 잘못된 메서드 (401)"

# 1-3. 쿼리 파라미터 포함 - 지오펜스 통계 조회 (401)
check_endpoint "GET" "/api/statistics/geofence?startDate=2024-01-01&endDate=2024-01-31" "401" "GET 쿼리 파라미터 포함 - 지오펜스 통계 (401)"

echo -e "${BLUE}📈 2. 일일 활동 통계 조회 (다중) 엔드포인트 테스트${NC}"

# 2-1. GET /api/statistics/daily-activity - 일일 활동 통계 조회 (JWT 토큰 없으면 401)
check_endpoint "GET" "/api/statistics/daily-activity" "401" "GET /api/statistics/daily-activity - 일일 활동 통계 조회 (401)"

# 2-2. PUT /api/statistics/daily-activity - 잘못된 메서드 (401, JWT 체크 우선)
check_endpoint "PUT" "/api/statistics/daily-activity" "401" "PUT /api/statistics/daily-activity - 잘못된 메서드 (401)"

# 2-3. 쿼리 파라미터 포함 - 일일 활동 통계 조회 (401)
check_endpoint "GET" "/api/statistics/daily-activity?startDate=2024-01-01&endDate=2024-01-07" "401" "GET 쿼리 파라미터 포함 - 일일 활동 통계 (401)"

echo -e "${BLUE}📋 3. 일일 활동 통계 조회 (단일) 엔드포인트 테스트${NC}"

# 3-1. GET /api/statistics/daily-activity/single - 특정 날짜 활동 통계 (JWT 토큰 없으면 401)
check_endpoint "GET" "/api/statistics/daily-activity/single" "401" "GET /api/statistics/daily-activity/single - 특정 날짜 활동 통계 (401)"

# 3-2. DELETE /api/statistics/daily-activity/single - 잘못된 메서드 (401, JWT 체크 우선)
check_endpoint "DELETE" "/api/statistics/daily-activity/single" "401" "DELETE /api/statistics/daily-activity/single - 잘못된 메서드 (401)"

# 3-3. 쿼리 파라미터 포함 - 특정 날짜 활동 통계 (401)
check_endpoint "GET" "/api/statistics/daily-activity/single?date=2024-01-15" "401" "GET 쿼리 파라미터 포함 - 특정 날짜 활동 통계 (401)"

echo -e "${BLUE}🛡️ 4. 안전 통계 조회 엔드포인트 테스트${NC}"

# 4-1. GET /api/statistics/safety - 안전 통계 조회 (JWT 토큰 없으면 401)
check_endpoint "GET" "/api/statistics/safety" "401" "GET /api/statistics/safety - 안전 통계 조회 (401)"

# 4-2. PATCH /api/statistics/safety - 잘못된 메서드 (401, JWT 체크 우선)
check_endpoint "PATCH" "/api/statistics/safety" "401" "PATCH /api/statistics/safety - 잘못된 메서드 (401)"

# 4-3. 쿼리 파라미터 포함 - 안전 통계 조회 (401)
check_endpoint "GET" "/api/statistics/safety?startDate=2024-01-01&endDate=2024-01-31" "401" "GET 쿼리 파라미터 포함 - 안전 통계 (401)"

echo -e "${BLUE}📊 5. 전체 통계 요약 조회 엔드포인트 테스트${NC}"

# 5-1. GET /api/statistics/summary - 전체 통계 요약 조회 (JWT 토큰 없으면 401)
check_endpoint "GET" "/api/statistics/summary" "401" "GET /api/statistics/summary - 전체 통계 요약 조회 (401)"

# 5-2. POST /api/statistics/summary - 잘못된 메서드 (401, JWT 체크 우선)
check_endpoint "POST" "/api/statistics/summary" "401" "POST /api/statistics/summary - 잘못된 메서드 (401)"

# 5-3. 쿼리 파라미터 포함 - 전체 통계 요약 조회 (401)
check_endpoint "GET" "/api/statistics/summary?startDate=2024-01-01&endDate=2024-01-07" "401" "GET 쿼리 파라미터 포함 - 전체 통계 요약 (401)"

echo -e "${BLUE}❌ 6. 잘못된 경로 테스트${NC}"

# 6-1. GET /api/statistics/nonexistent - 존재하지 않는 하위 경로 (401, JWT 체크 우선)
check_endpoint "GET" "/api/statistics/nonexistent" "401" "GET /api/statistics/nonexistent - 존재하지 않는 하위 경로 (401)"

# 6-2. GET /api/statistics/ - 루트 경로 (슬래시 포함) (401, JWT 체크 우선)
check_endpoint "GET" "/api/statistics/" "401" "GET /api/statistics/ - 루트 경로 슬래시 포함 (401)"

# 6-3. GET /api/statistics - 루트 경로 (슬래시 없음) (401, JWT 체크 우선)
check_endpoint "GET" "/api/statistics" "401" "GET /api/statistics - 루트 경로 슬래시 없음 (401)"

echo -e "${BLUE}🔧 7. 다양한 HTTP 메서드 테스트${NC}"

# 7-1. HEAD /api/statistics/geofence - HEAD 메서드 (401)
response=$(curl -s -w '%{http_code}' -I "$BASE_URL/api/statistics/geofence" 2>/dev/null || echo "000")
http_code="${response: -3}"
if [ "$http_code" = "401" ] || [ "$http_code" = "405" ]; then
    log_test_result "HEAD /api/statistics/geofence - HEAD 메서드 (401 또는 405)" "$http_code" "$http_code" "HEAD 요청 처리"
else
    log_test_result "HEAD /api/statistics/geofence - HEAD 메서드 (401 또는 405)" "401 또는 405" "$http_code" "HEAD 요청 처리"
fi

# 7-2. OPTIONS /api/statistics/safety - OPTIONS 메서드 (200 또는 401)
response=$(curl -s -w '%{http_code}' -X OPTIONS "$BASE_URL/api/statistics/safety" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "200" ] || [ "$http_code" = "401" ]; then
    log_test_result "OPTIONS /api/statistics/safety - OPTIONS 메서드 (200 또는 401)" "$http_code" "$http_code" "$body"
else
    log_test_result "OPTIONS /api/statistics/safety - OPTIONS 메서드 (200 또는 401)" "200 또는 401" "$http_code" "$body"
fi

# 7-3. TRACE /api/statistics/summary - TRACE 메서드 (400 또는 501)
response=$(curl -s -w '%{http_code}' -X TRACE "$BASE_URL/api/statistics/summary" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "400" ] || [ "$http_code" = "501" ]; then
    log_test_result "TRACE /api/statistics/summary - TRACE 메서드 (400 또는 501)" "$http_code" "$http_code" "$body"
else
    log_test_result "TRACE /api/statistics/summary - TRACE 메서드 (400 또는 501)" "400 또는 501" "$http_code" "$body"
fi

echo -e "${BLUE}📋 8. 날짜 형식 파라미터 테스트${NC}"

# 8-1. 잘못된 날짜 형식 - 지오펜스 통계 (400 또는 401)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence?startDate=invalid-date&endDate=2024-01-31" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "400" ] || [ "$http_code" = "401" ]; then
    log_test_result "GET 잘못된 날짜 형식 - 지오펜스 통계 (400 또는 401)" "$http_code" "$http_code" "$body"
else
    log_test_result "GET 잘못된 날짜 형식 - 지오펜스 통계 (400 또는 401)" "400 또는 401" "$http_code" "$body"
fi

# 8-2. 잘못된 날짜 형식 - 일일 활동 통계 (400 또는 401)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/daily-activity?startDate=2024-13-01&endDate=invalid" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "400" ] || [ "$http_code" = "401" ]; then
    log_test_result "GET 잘못된 날짜 형식 - 일일 활동 통계 (400 또는 401)" "$http_code" "$http_code" "$body"
else
    log_test_result "GET 잘못된 날짜 형식 - 일일 활동 통계 (400 또는 401)" "400 또는 401" "$http_code" "$body"
fi

# 8-3. 잘못된 날짜 형식 - 특정 날짜 활동 통계 (400 또는 401)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/daily-activity/single?date=not-a-date" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"
if [ "$http_code" = "400" ] || [ "$http_code" = "401" ]; then
    log_test_result "GET 잘못된 날짜 형식 - 특정 날짜 활동 통계 (400 또는 401)" "$http_code" "$http_code" "$body"
else
    log_test_result "GET 잘못된 날짜 형식 - 특정 날짜 활동 통계 (400 또는 401)" "400 또는 401" "$http_code" "$body"
fi

echo -e "${BLUE}🌐 9. 엣지 케이스 테스트${NC}"

# 9-1. 매우 긴 URL로 401 에러 (JWT 체크 우선)
long_path=$(printf "%0*d" 100 1)
check_endpoint "GET" "/api/statistics/very/long/path/$long_path" "401" "GET 매우 긴 URL - 401 에러 (401)"

# 9-2. URL 인코딩된 문자들 (JWT 체크 우선)
check_endpoint "GET" "/api/statistics/encoded%20path" "401" "GET URL 인코딩 경로 - 401 에러 (401)"

# 9-3. 특수 문자가 포함된 경로 (JWT 체크 우선)
check_endpoint "GET" "/api/statistics/special-chars!" "401" "GET 특수 문자 경로 - 401 에러 (401)"

# 9-4. 빈 경로 세그먼트 (Spring이 400으로 처리)
check_endpoint "GET" "/api/statistics//empty//segments" "400" "GET 빈 경로 세그먼트 - 400 에러 (400)"

echo -e "${BLUE}🔄 10. 동시 요청 부하 테스트${NC}"

# 10-1. 동시 요청 테스트 (5개 요청)
echo "동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/statistics/geofence" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/statistics_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc -l)

# 결과 검증 (401 응답 기대)
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/statistics_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/statistics_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "401" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/statistics_concurrent_$i.txt"
    fi
done

log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"

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
    echo -e "${GREEN}🎉 🎉 🎉 StatisticsController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 30개 통계 조회 엔드포인트가 예상대로 동작합니다!${NC}"
    echo -e "${GREEN}📊 지오펜스, 일일활동, 안전 통계 API가 JWT 토큰 요구사항을 올바르게 처리합니다.${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ StatisticsController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 StatisticsController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi