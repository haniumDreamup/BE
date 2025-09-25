#!/bin/bash

# PoseController 현실적 테스트 스크립트
# MediaPipe Pose API의 특성을 고려한 실용적 테스트

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
echo "PoseController 현실적 테스트 시작"
echo "MediaPipe Pose API 특성을 고려한 실용적 검증"
echo "========================================"
printf "\n"

# =========================
# 1. 인증 테스트 (예상 401/403)
# =========================
echo "${YELLOW}=== 1. 인증 테스트 ===${NC}"

# 1. Pose 데이터 전송 - 인증 없음
test_endpoint "POST" "/api/v1/pose/data" "401" "Pose 데이터 전송 - 인증 없음 (401)"

# 2. 일괄 Pose 데이터 전송 - 인증 없음
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "일괄 Pose 데이터 - 인증 없음 (401)"

# 3. 낙상 상태 조회 - 인증 없음
test_endpoint "GET" "/api/v1/pose/fall-status/1" "401" "낙상 상태 조회 - 인증 없음 (401)"

# 4. 낙상 피드백 제출 - 인증 없음
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "401" "낙상 피드백 - 인증 없음 (401)"

# =========================
# 2. 데이터 검증 테스트
# =========================
echo "${YELLOW}=== 2. 데이터 검증 테스트 ===${NC}"

# 5. Pose 데이터 - 빈 데이터
test_endpoint "POST" "/api/v1/pose/data" "400" "Pose 데이터 - 빈 데이터 (400)" ""

# 6. Pose 데이터 - 잘못된 JSON
test_endpoint "POST" "/api/v1/pose/data" "400" "Pose 데이터 - 잘못된 JSON (400)" "invalid json"

# 7. Pose 데이터 - 필수 필드 누락
incomplete_data='{"userId": 1}'
test_endpoint "POST" "/api/v1/pose/data" "400" "Pose 데이터 - 필수 필드 누락 (400)" "$incomplete_data"

# 8. 일괄 데이터 - 빈 배열
test_endpoint "POST" "/api/v1/pose/data/batch" "400" "일괄 데이터 - 빈 배열 (400)" "[]"

# 9. 일괄 데이터 - 잘못된 형식
test_endpoint "POST" "/api/v1/pose/data/batch" "400" "일괄 데이터 - 잘못된 형식 (400)" "not an array"

# =========================
# 3. 파라미터 검증 테스트
# =========================
echo "${YELLOW}=== 3. 파라미터 검증 테스트 ===${NC}"

# 10. 낙상 상태 조회 - 잘못된 사용자 ID
test_endpoint "GET" "/api/v1/pose/fall-status/invalid" "400" "낙상 상태 - 잘못된 사용자 ID (400)"

# 11. 낙상 상태 조회 - 음수 사용자 ID
test_endpoint "GET" "/api/v1/pose/fall-status/-1" "401" "낙상 상태 - 음수 사용자 ID (401)"

# 12. 낙상 상태 조회 - 매우 큰 사용자 ID
test_endpoint "GET" "/api/v1/pose/fall-status/999999999" "401" "낙상 상태 - 큰 사용자 ID (401)"

# 13. 낙상 피드백 - 잘못된 이벤트 ID
test_endpoint "POST" "/api/v1/pose/fall-event/invalid/feedback" "400" "낙상 피드백 - 잘못된 이벤트 ID (400)"

# 14. 낙상 피드백 - 음수 이벤트 ID
test_endpoint "POST" "/api/v1/pose/fall-event/-1/feedback" "401" "낙상 피드백 - 음수 이벤트 ID (401)"

# =========================
# 4. HTTP 메서드 검증
# =========================
echo "${YELLOW}=== 4. HTTP 메서드 검증 ===${NC}"

# 15. Pose 데이터 - GET 메서드 (잘못된 메서드)
test_endpoint "GET" "/api/v1/pose/data" "405" "Pose 데이터 - GET 메서드 (405)"

# 16. 일괄 데이터 - PUT 메서드 (잘못된 메서드)
test_endpoint "PUT" "/api/v1/pose/data/batch" "405" "일괄 데이터 - PUT 메서드 (405)"

# 17. 낙상 상태 조회 - POST 메서드 (잘못된 메서드)
test_endpoint "POST" "/api/v1/pose/fall-status/1" "405" "낙상 상태 - POST 메서드 (405)"

# 18. 낙상 피드백 - GET 메서드 (잘못된 메서드)
test_endpoint "GET" "/api/v1/pose/fall-event/1/feedback" "405" "낙상 피드백 - GET 메서드 (405)"

# =========================
# 5. 엔드포인트 존재 검증
# =========================
echo "${YELLOW}=== 5. 엔드포인트 존재 검증 ===${NC}"

# 19. 존재하지 않는 엔드포인트
test_endpoint "GET" "/api/v1/pose/nonexistent" "404" "존재하지 않는 Pose 엔드포인트 (404)"

# 20. 루트 Pose 경로
test_endpoint "GET" "/api/v1/pose" "404" "루트 Pose 경로 (404)"

# 21. 잘못된 하위 경로
test_endpoint "GET" "/api/v1/pose/invalid/path" "404" "잘못된 하위 Pose 경로 (404)"

# 22. 잘못된 fall-status 경로
test_endpoint "GET" "/api/v1/pose/fall-status" "404" "잘못된 fall-status 경로 (404)"

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
    printf "${YELLOW}📝 Pose API는 MediaPipe 연동을 위한 중요한 AI API입니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 Pose API는 MediaPipe 연동을 위한 중요한 AI API입니다${NC}\n"
    exit 1
fi