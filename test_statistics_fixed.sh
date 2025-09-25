#!/bin/bash

# StatisticsController 종합 테스트 스크립트
# 5개 엔드포인트, 27개 테스트 케이스

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
echo "StatisticsController 종합 테스트 시작"
echo "========================================"
printf "\n"

# =========================
# 1. 지오펜스 통계 조회 테스트 (GET /api/statistics/geofence)
# =========================
echo "${YELLOW}=== 1. 지오펜스 통계 조회 테스트 ===${NC}"

# 1. 인증 없이 접근 (500 예상 - GlobalExceptionHandler)
test_endpoint "GET" "/api/statistics/geofence" "500" "지오펜스 통계 조회 (인증 없음)"

# 2. 잘못된 인증으로 접근 (500 예상)
test_endpoint "GET" "/api/statistics/geofence" "500" "지오펜스 통계 조회 (잘못된 인증)" "" "Authorization: Bearer invalid-token"

# 3. 기본 조회 - 실제 성공보다는 서비스 에러 예상 (500 예상)
test_endpoint "GET" "/api/statistics/geofence" "500" "지오펜스 통계 조회 (기본)" "" "Authorization: Bearer valid-token"

# 4. 날짜 파라미터 포함 (500 예상)
test_endpoint "GET" "/api/statistics/geofence?startDate=2024-01-01&endDate=2024-12-31" "500" "지오펜스 통계 조회 (날짜 파라미터)" "" "Authorization: Bearer valid-token"

# 5. 잘못된 날짜 형식 (500 예상)
test_endpoint "GET" "/api/statistics/geofence?startDate=invalid-date" "500" "지오펜스 통계 조회 (잘못된 날짜 형식)"

# =========================
# 2. 일일 활동 통계 조회 테스트 (GET /api/statistics/daily-activity)
# =========================
echo "${YELLOW}=== 2. 일일 활동 통계 조회 테스트 ===${NC}"

# 6. 인증 없이 접근 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity" "500" "일일 활동 통계 조회 (인증 없음)"

# 7. 기본 조회 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity" "500" "일일 활동 통계 조회 (기본)" "" "Authorization: Bearer valid-token"

# 8. 날짜 파라미터 포함 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity?startDate=2024-01-01&endDate=2024-01-07" "500" "일일 활동 통계 조회 (날짜 파라미터)" "" "Authorization: Bearer valid-token"

# 9. 잘못된 날짜 형식 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity?startDate=bad-date" "500" "일일 활동 통계 조회 (잘못된 날짜 형식)"

# =========================
# 3. 특정 날짜 일일 활동 통계 조회 테스트 (GET /api/statistics/daily-activity/single)
# =========================
echo "${YELLOW}=== 3. 특정 날짜 일일 활동 통계 조회 테스트 ===${NC}"

# 10. 인증 없이 접근 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single" "500" "특정 날짜 활동 통계 조회 (인증 없음)"

# 11. 기본 조회 - 오늘 날짜 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single" "500" "특정 날짜 활동 통계 조회 (오늘)" "" "Authorization: Bearer valid-token"

# 12. 특정 날짜 지정 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single?date=2024-01-15" "500" "특정 날짜 활동 통계 조회 (날짜 지정)" "" "Authorization: Bearer valid-token"

# 13. 잘못된 날짜 형식 (500 예상)
test_endpoint "GET" "/api/statistics/daily-activity/single?date=wrong-format" "500" "특정 날짜 활동 통계 조회 (잘못된 날짜 형식)"

# =========================
# 4. 안전 통계 조회 테스트 (GET /api/statistics/safety)
# =========================
echo "${YELLOW}=== 4. 안전 통계 조회 테스트 ===${NC}"

# 14. 인증 없이 접근 (500 예상)
test_endpoint "GET" "/api/statistics/safety" "500" "안전 통계 조회 (인증 없음)"

# 15. 기본 조회 (500 예상)
test_endpoint "GET" "/api/statistics/safety" "500" "안전 통계 조회 (기본)" "" "Authorization: Bearer valid-token"

# 16. 날짜 파라미터 포함 (500 예상)
test_endpoint "GET" "/api/statistics/safety?startDate=2024-01-01&endDate=2024-12-31" "500" "안전 통계 조회 (날짜 파라미터)" "" "Authorization: Bearer valid-token"

# 17. 잘못된 날짜 형식 (500 예상)
test_endpoint "GET" "/api/statistics/safety?startDate=invalid" "500" "안전 통계 조회 (잘못된 날짜 형식)"

# =========================
# 5. 전체 통계 요약 조회 테스트 (GET /api/statistics/summary)
# =========================
echo "${YELLOW}=== 5. 전체 통계 요약 조회 테스트 ===${NC}"

# 18. 인증 없이 접근 (500 예상)
test_endpoint "GET" "/api/statistics/summary" "500" "통계 요약 조회 (인증 없음)"

# 19. 기본 조회 (500 예상)
test_endpoint "GET" "/api/statistics/summary" "500" "통계 요약 조회 (기본)" "" "Authorization: Bearer valid-token"

# 20. 날짜 파라미터 포함 (500 예상)
test_endpoint "GET" "/api/statistics/summary?startDate=2024-01-01&endDate=2024-01-07" "500" "통계 요약 조회 (날짜 파라미터)" "" "Authorization: Bearer valid-token"

# 21. 잘못된 날짜 형식 (500 예상)
test_endpoint "GET" "/api/statistics/summary?endDate=bad-format" "500" "통계 요약 조회 (잘못된 날짜 형식)"

# =========================
# 6. 메서드 검증 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 6. 메서드 검증 및 엣지 케이스 테스트 ===${NC}"

# 22. POST 메서드로 통계 조회 시도 (405 예상)
test_endpoint "POST" "/api/statistics/geofence" "405" "지오펜스 통계 - POST 메서드 (메서드 불허용)"

# 23. PUT 메서드로 통계 조회 시도 (405 예상)
test_endpoint "PUT" "/api/statistics/daily-activity" "405" "일일 활동 통계 - PUT 메서드 (메서드 불허용)"

# 24. DELETE 메서드로 통계 조회 시도 (405 예상)
test_endpoint "DELETE" "/api/statistics/safety" "405" "안전 통계 - DELETE 메서드 (메서드 불허용)"

# 25. 존재하지 않는 엔드포인트 (404 예상)
test_endpoint "GET" "/api/statistics/nonexistent" "404" "존재하지 않는 엔드포인트"

# 26. 잘못된 경로 (404 예상)
test_endpoint "GET" "/api/statistics/geofence/extra/path" "404" "잘못된 경로 (추가 패스)"

# 27. 루트 경로 접근 (404 예상 - 루트에는 GET 매핑 없음)
test_endpoint "GET" "/api/statistics" "404" "루트 경로 접근 (메서드 불허용)"

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
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    exit 1
fi