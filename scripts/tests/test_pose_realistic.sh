#!/bin/bash

# PoseController 현실적 테스트 스크립트 v2
# Spring Security 설정을 고려한 현실적 테스트

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
        if [ ${#response_body} -lt 200 ]; then
            printf "   응답 본문: $response_body\n"
        else
            printf "   응답 본문: ${response_body:0:200}...\n"
        fi
    fi

    printf "\n"
}

echo "========================================"
echo "PoseController 현실적 테스트 v2"
echo "Spring Security 설정 기반 실용적 검증"
echo "========================================"
printf "\n"

# =========================
# 1. Spring Security 인증 테스트 (401 응답)
# =========================
echo "${YELLOW}=== 1. Spring Security 인증 테스트 ===${NC}"

# 1. POST /data - 인증 필요 (TestSecurityConfig에서 .authenticated() 설정)
test_endpoint "POST" "/api/v1/pose/data" "401" "Pose 데이터 전송 - 인증 없음 (401 by Security)"

# 2. POST /data/batch - 인증 필요
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "일괄 Pose 데이터 - 인증 없음 (401 by Security)"

# 3. GET /fall-status/{id} - 인증 필요
test_endpoint "GET" "/api/v1/pose/fall-status/1" "401" "낙상 상태 조회 - 인증 없음 (401 by Security)"

# 4. POST /fall-event/{id}/feedback - 인증 필요
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "401" "낙상 피드백 - 인증 없음 (401 by Security)"

# =========================
# 2. 경로 매개변수 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 2. 경로 매개변수 검증 테스트 ===${NC}"

# 5. 잘못된 userId 형식 - Spring이 400으로 처리
test_endpoint "GET" "/api/v1/pose/fall-status/invalid" "400" "낙상 상태 - 잘못된 사용자 ID 형식 (400 by Spring)"

# 6. 잘못된 eventId 형식 - Spring이 400으로 처리
test_endpoint "POST" "/api/v1/pose/fall-event/invalid/feedback" "400" "낙상 피드백 - 잘못된 이벤트 ID 형식 (400 by Spring)"

# =========================
# 3. HTTP 메서드 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 3. HTTP 메서드 검증 ===${NC}"

# 7. 잘못된 HTTP 메서드 - GET으로 POST 엔드포인트 호출
test_endpoint "GET" "/api/v1/pose/data" "405" "Pose 데이터 - GET 메서드 (405 Method Not Allowed)"

# 8. 잘못된 HTTP 메서드 - PUT으로 POST 엔드포인트 호출
test_endpoint "PUT" "/api/v1/pose/data/batch" "405" "일괄 데이터 - PUT 메서드 (405 Method Not Allowed)"

# 9. 잘못된 HTTP 메서드 - POST로 GET 엔드포인트 호출
test_endpoint "POST" "/api/v1/pose/fall-status/1" "405" "낙상 상태 - POST 메서드 (405 Method Not Allowed)"

# 10. 잘못된 HTTP 메서드 - GET으로 POST 엔드포인트 호출
test_endpoint "GET" "/api/v1/pose/fall-event/1/feedback" "405" "낙상 피드백 - GET 메서드 (405 Method Not Allowed)"

# =========================
# 4. 엔드포인트 존재 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 4. 엔드포인트 존재 검증 ===${NC}"

# 11. 존재하지 않는 엔드포인트
test_endpoint "GET" "/api/v1/pose/nonexistent" "404" "존재하지 않는 엔드포인트 (404 Not Found)"

# 12. 루트 경로 - 매핑되지 않은 경로
test_endpoint "GET" "/api/v1/pose" "404" "루트 Pose 경로 (404 Not Found)"

# 13. 잘못된 하위 경로
test_endpoint "GET" "/api/v1/pose/invalid/path" "404" "잘못된 하위 경로 (404 Not Found)"

# 14. 매개변수 없는 fall-status 경로
test_endpoint "GET" "/api/v1/pose/fall-status" "404" "매개변수 없는 fall-status (404 Not Found)"

# =========================
# 5. JSON 파싱 에러 (Spring이 처리)
# =========================
echo "${YELLOW}=== 5. JSON 파싱 에러 테스트 ===${NC}"

# 15. 잘못된 JSON 형식 - Spring이 400으로 처리
test_endpoint "POST" "/api/v1/pose/data" "400" "잘못된 JSON 형식 (400 Bad Request)" "invalid json"

# 16. 빈 요청 본문 - Spring이 400으로 처리
test_endpoint "POST" "/api/v1/pose/data" "400" "빈 요청 본문 (400 Bad Request)" ""

# 17. 일괄 데이터 - 잘못된 JSON
test_endpoint "POST" "/api/v1/pose/data/batch" "400" "일괄 데이터 - 잘못된 JSON (400 Bad Request)" "not json"

# 18. 낙상 피드백 - 잘못된 JSON
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "400" "낙상 피드백 - 잘못된 JSON (400 Bad Request)" "invalid"

# =========================
# 6. Content-Type 검증 (선택적)
# =========================
echo "${YELLOW}=== 6. Content-Type 검증 ===${NC}"

# 19. 잘못된 Content-Type - Spring이 415로 처리할 수 있음
test_endpoint "POST" "/api/v1/pose/data" "415" "잘못된 Content-Type (415 Unsupported Media Type)" '{}' "Content-Type: text/plain"

# 20. Content-Type 누락 - 일부 서버에서 400 처리
test_endpoint "POST" "/api/v1/pose/data" "400" "Content-Type 누락 (400 Bad Request)" '{}' ""

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
    printf "${YELLOW}📝 PoseController는 Spring Security와 올바르게 통합되었습니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 일부 테스트가 예상과 다른 결과를 보입니다${NC}\n"
    exit 1
fi