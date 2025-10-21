#!/bin/bash

# StatisticsController 올바른 HTTP 상태 코드 테스트 스크립트
# 수정된 SecurityConfig와 GlobalExceptionHandler로 테스트

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local data=$5
    local headers=$6

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    printf "${BLUE}테스트 $TOTAL_TESTS: $description${NC}\n"

    if [ -z "$headers" ]; then
        headers="Content-Type: application/json"
    fi

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint" -H "$headers")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" -H "$headers" -d "$data")
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL$endpoint" -H "$headers" -d "$data")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint" -H "$headers")
    elif [ "$method" = "PATCH" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL$endpoint" -H "$headers" -d "$data")
    fi

    # 응답에서 상태 코드 추출 (마지막 줄)
    status_code=$(echo "$response" | tail -n1)

    # 응답 본문 (상태 코드 제외)
    response_body=$(echo "$response" | sed '$d')

    printf "   요청: $method $BASE_URL$endpoint\n"
    printf "   예상 상태: $expected_status, 실제 상태: $status_code\n"

    if [ "$status_code" = "$expected_status" ]; then
        printf "   ${GREEN}✓ 통과${NC}\n"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        printf "   ${RED}✗ 실패${NC}\n"
        printf "   응답 본문: $response_body\n"
    fi

    printf "\n"
}

echo "========================================"
echo "StatisticsController 올바른 상태 코드 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. 지오펜스 통계 조회 테스트 (GET /api/statistics/geofence)
# =========================
echo "${YELLOW}=== 1. 지오펜스 통계 조회 테스트 ===${NC}"

# 1. 인증 없이 접근 (401 예상)
test_endpoint "GET" "/api/statistics/geofence" "401" "지오펜스 통계 조회 (인증 없음)"

# 2. 잘못된 인증으로 접근 (401 예상)
test_endpoint "GET" "/api/statistics/geofence" "401" "지오펜스 통계 조회 (잘못된 인증)" "" "Authorization: Bearer invalid-token"

# 3. 잘못된 날짜 형식 (400 예상)
test_endpoint "GET" "/api/statistics/geofence?startDate=invalid-date" "400" "지오펜스 통계 조회 (잘못된 날짜 형식)"

# 4. 잘못된 날짜 형식 2 (400 예상)
test_endpoint "GET" "/api/statistics/geofence?endDate=2024/01/01" "400" "지오펜스 통계 조회 (슬래시 날짜 형식)"

# =========================
# 2. 일일 활동 통계 조회 테스트 (GET /api/statistics/daily-activity)
# =========================
echo "${YELLOW}=== 2. 일일 활동 통계 조회 테스트 ===${NC}"

# 5. 인증 없이 접근 (401 예상)
test_endpoint "GET" "/api/statistics/daily-activity" "401" "일일 활동 통계 조회 (인증 없음)"

# 6. 잘못된 날짜 형식 (400 예상)
test_endpoint "GET" "/api/statistics/daily-activity?startDate=bad-date" "400" "일일 활동 통계 조회 (잘못된 날짜 형식)"

# =========================
# 3. 특정 날짜 일일 활동 통계 조회 테스트 (GET /api/statistics/daily-activity/single)
# =========================
echo "${YELLOW}=== 3. 특정 날짜 일일 활동 통계 조회 테스트 ===${NC}"

# 7. 인증 없이 접근 (401 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single" "401" "특정 날짜 활동 통계 조회 (인증 없음)"

# 8. 잘못된 날짜 형식 (400 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single?date=wrong-format" "400" "특정 날짜 활동 통계 조회 (잘못된 날짜 형식)"

# =========================
# 4. 안전 통계 조회 테스트 (GET /api/statistics/safety)
# =========================
echo "${YELLOW}=== 4. 안전 통계 조회 테스트 ===${NC}"

# 9. 인증 없이 접근 (401 예상)
test_endpoint "GET" "/api/statistics/safety" "401" "안전 통계 조회 (인증 없음)"

# 10. 잘못된 날짜 형식 (400 예상)
test_endpoint "GET" "/api/statistics/safety?startDate=invalid" "400" "안전 통계 조회 (잘못된 날짜 형식)"

# =========================
# 5. 전체 통계 요약 조회 테스트 (GET /api/statistics/summary)
# =========================
echo "${YELLOW}=== 5. 전체 통계 요약 조회 테스트 ===${NC}"

# 11. 인증 없이 접근 (401 예상)
test_endpoint "GET" "/api/statistics/summary" "401" "통계 요약 조회 (인증 없음)"

# 12. 잘못된 날짜 형식 (400 예상)
test_endpoint "GET" "/api/statistics/summary?endDate=bad-format" "400" "통계 요약 조회 (잘못된 날짜 형식)"

# =========================
# 6. 메서드 검증 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 6. 메서드 검증 및 엣지 케이스 테스트 ===${NC}"

# 13. POST 메서드로 통계 조회 시도 (405 예상)
test_endpoint "POST" "/api/statistics/geofence" "405" "지오펜스 통계 - POST 메서드 (메서드 불허용)"

# 14. PUT 메서드로 통계 조회 시도 (405 예상)
test_endpoint "PUT" "/api/statistics/daily-activity" "405" "일일 활동 통계 - PUT 메서드 (메서드 불허용)"

# 15. DELETE 메서드로 통계 조회 시도 (405 예상)
test_endpoint "DELETE" "/api/statistics/safety" "405" "안전 통계 - DELETE 메서드 (메서드 불허용)"

# 16. 존재하지 않는 엔드포인트 (404 예상)
test_endpoint "GET" "/api/statistics/nonexistent" "404" "존재하지 않는 엔드포인트"

# 17. 잘못된 경로 (404 예상)
test_endpoint "GET" "/api/statistics/geofence/extra/path" "404" "잘못된 경로 (추가 패스)"

# 18. 루트 경로 접근 (404 예상 - 루트에는 GET 매핑 없음)
test_endpoint "GET" "/api/statistics" "404" "루트 경로 접근"

# =========================
# 결과 요약
# =========================
echo "========================================"
echo "           테스트 결과 요약"
echo "========================================"
printf "총 테스트: ${BLUE}$TOTAL_TESTS${NC}\n"
printf "통과: ${GREEN}$PASSED_TESTS${NC}\n"
printf "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}\n"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    printf "\n${GREEN}🎉 모든 테스트 통과! (100%% 성공률)${NC}\n"
    printf "HTTP 상태 코드가 올바르게 반환됩니다!\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    exit 1
fi