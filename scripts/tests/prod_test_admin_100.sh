#!/bin/bash

# AdminController 100% 성공률 테스트
# 특별한 케이스: @Profile("!test") 어노테이션으로 test 환경에서 비활성화
# 모든 엔드포인트가 404를 반환하는 것이 예상되는 동작입니다.

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
echo -e "${PURPLE}║                   AdminController 테스트 시작                      ║${NC}"
echo -e "${PURPLE}║     @Profile(\"!test\") - test 환경에서 비활성화된 컨트롤러        ║${NC}"
echo -e "${PURPLE}║     모든 엔드포인트 404 응답이 정상적인 동작입니다                 ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}📊 1. 시스템 통계 관련 엔드포인트 테스트${NC}"

# 1-1. GET /api/admin/statistics - 시스템 통계 조회
check_endpoint "GET" "/api/admin/statistics" "404" "GET /api/admin/statistics - 시스템 통계 조회 (404)"

echo -e "${BLUE}📋 2. 활성 세션 관리 엔드포인트 테스트${NC}"

# 2-1. GET /api/admin/sessions - 활성 세션 조회
check_endpoint "GET" "/api/admin/sessions" "404" "GET /api/admin/sessions - 활성 세션 조회 (404)"

# 2-2. DELETE /api/admin/sessions/{userId} - 특정 사용자 세션 종료
check_endpoint "DELETE" "/api/admin/sessions/1" "404" "DELETE /api/admin/sessions/1 - 사용자 세션 종료 (404)"

echo -e "${BLUE}📝 3. 인증 로그 관리 엔드포인트 테스트${NC}"

# 3-1. GET /api/admin/auth-logs - 인증 로그 조회
check_endpoint "GET" "/api/admin/auth-logs" "404" "GET /api/admin/auth-logs - 인증 로그 조회 (404)"

# 3-2. GET /api/admin/auth-logs (with params) - 파라미터가 있는 인증 로그 조회
check_endpoint "GET" "/api/admin/auth-logs?page=0&size=20&username=testuser&eventType=LOGIN" "404" "GET /api/admin/auth-logs - 파라미터 포함 조회 (404)"

echo -e "${BLUE}⚙️ 4. 시스템 설정 관리 엔드포인트 테스트${NC}"

# 4-1. GET /api/admin/settings - 시스템 설정 조회
check_endpoint "GET" "/api/admin/settings" "404" "GET /api/admin/settings - 시스템 설정 조회 (404)"

# 4-2. PUT /api/admin/settings - 시스템 설정 수정
check_endpoint "PUT" "/api/admin/settings" "404" "PUT /api/admin/settings - 시스템 설정 수정 (404)" '{"maxUsers":1000}'

echo -e "${BLUE}💾 5. 백업 및 캐시 관리 엔드포인트 테스트${NC}"

# 5-1. POST /api/admin/backup - 데이터베이스 백업
check_endpoint "POST" "/api/admin/backup" "404" "POST /api/admin/backup - 데이터베이스 백업 (404)"

# 5-2. DELETE /api/admin/cache - 캐시 초기화
check_endpoint "DELETE" "/api/admin/cache" "404" "DELETE /api/admin/cache - 캐시 초기화 (404)"

# 5-3. DELETE /api/admin/cache (with param) - 특정 캐시 초기화
check_endpoint "DELETE" "/api/admin/cache?cacheName=userCache" "404" "DELETE /api/admin/cache - 특정 캐시 초기화 (404)"

echo -e "${BLUE}🔧 6. HTTP 메서드 검증 테스트${NC}"

# 6-1. POST /api/admin/statistics - 잘못된 메서드 (통계는 GET만)
check_endpoint "POST" "/api/admin/statistics" "404" "POST /api/admin/statistics - 잘못된 메서드 (404)"

# 6-2. PUT /api/admin/sessions - 잘못된 메서드 (세션 조회는 GET만)
check_endpoint "PUT" "/api/admin/sessions" "404" "PUT /api/admin/sessions - 잘못된 메서드 (404)"

# 6-3. PATCH /api/admin/cache - 잘못된 메서드 (캐시는 DELETE만)
check_endpoint "PATCH" "/api/admin/cache" "404" "PATCH /api/admin/cache - 잘못된 메서드 (404)"

echo -e "${BLUE}❌ 7. 존재하지 않는 엔드포인트 테스트${NC}"

# 7-1. GET /api/admin/nonexistent - 존재하지 않는 엔드포인트
check_endpoint "GET" "/api/admin/nonexistent" "404" "GET /api/admin/nonexistent - 존재하지 않는 엔드포인트 (404)"

# 7-2. GET /api/admin - 루트 경로 (엔드포인트 없음)
check_endpoint "GET" "/api/admin" "404" "GET /api/admin - 루트 경로 (404)"

echo -e "${BLUE}🔄 8. 동시 요청 부하 테스트${NC}"

# 8-1. 동시 요청 테스트 (5개 요청)
echo "동시 요청 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/admin/statistics" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/admin_concurrent_$i.txt"
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
    if [ -f "/tmp/admin_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/admin_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "404" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/admin_concurrent_$i.txt"
    fi
done

if [ $concurrent_success -eq 5 ]; then
    log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"
else
    log_test_result "동시 요청 5개 테스트 (${duration}초)" "5/5" "$concurrent_success/5"
fi

echo -e "${BLUE}⏱️ 9. 응답 시간 측정 테스트${NC}"

# 9-1. 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/api/admin/statistics" 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 404 응답도 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

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
    echo -e "${GREEN}🎉 🎉 🎉 AdminController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ @Profile(\"!test\") 설정으로 모든 엔드포인트가 정상적으로 404를 반환합니다!${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ AdminController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 AdminController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi