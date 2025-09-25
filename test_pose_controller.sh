#!/bin/bash

# PoseController 종합 테스트 스크립트
# 4개 낙상감지/포즈데이터 엔드포인트 테스트

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
        printf "   응답 본문: $response_body\n"
    fi

    printf "\n"
}

echo "========================================"
echo "PoseController 테스트 시작"
echo "4개 낙상감지/포즈데이터 엔드포인트 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. 포즈 데이터 전송 테스트
# =========================
echo "${YELLOW}=== 1. 포즈 데이터 전송 테스트 ===${NC}"

# 1. 인증 없이 포즈 데이터 전송 (401 예상)
pose_data='{"userId": 123, "timestamp": "2024-01-01T10:00:00Z", "landmarks": [{"x": 0.5, "y": 0.5, "z": 0.1, "visibility": 0.9}], "confidence": 0.85}'
test_endpoint "POST" "/api/v1/pose/data" "401" "포즈 데이터 전송 - 인증 없음 (401)" "$pose_data"

# 2. 잘못된 데이터 형식 (401 예상 - 인증 우선)
invalid_pose_data='{"userId": "invalid", "timestamp": "invalid", "landmarks": "invalid"}'
test_endpoint "POST" "/api/v1/pose/data" "401" "포즈 데이터 전송 - 잘못된 데이터 (401 인증 우선)" "$invalid_pose_data"

# 3. 필수 필드 누락 (401 예상 - 인증 우선)
incomplete_data='{"userId": 123}'
test_endpoint "POST" "/api/v1/pose/data" "401" "포즈 데이터 전송 - 필수 필드 누락 (401 인증 우선)" "$incomplete_data"

# =========================
# 2. 포즈 데이터 일괄 전송 테스트
# =========================
echo "${YELLOW}=== 2. 포즈 데이터 일괄 전송 테스트 ===${NC}"

# 4. 인증 없이 일괄 포즈 데이터 전송 (401 예상)
batch_pose_data='[{"userId": 123, "timestamp": "2024-01-01T10:00:00Z", "landmarks": [{"x": 0.5, "y": 0.5, "z": 0.1, "visibility": 0.9}], "confidence": 0.85}]'
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "일괄 포즈 데이터 전송 - 인증 없음 (401)" "$batch_pose_data"

# 5. 빈 배열 (401 예상 - 인증 우선)
empty_array='[]'
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "일괄 포즈 데이터 전송 - 빈 배열 (401 인증 우선)" "$empty_array"

# 6. 잘못된 배열 형식 (401 예상 - 인증 우선)
invalid_array='"not an array"'
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "일괄 포즈 데이터 전송 - 잘못된 배열 (401 인증 우선)" "$invalid_array"

# =========================
# 3. 낙상 상태 조회 테스트
# =========================
echo "${YELLOW}=== 3. 낙상 상태 조회 테스트 ===${NC}"

# 7. 인증 없이 낙상 상태 조회 (401 예상)
test_endpoint "GET" "/api/v1/pose/fall-status/123" "401" "낙상 상태 조회 - 인증 없음 (401)"

# 8. 잘못된 사용자 ID 형식 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose/fall-status/invalid" "401" "낙상 상태 조회 - 잘못된 ID 형식 (401 인증 우선)"

# 9. 음수 사용자 ID (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose/fall-status/-1" "401" "낙상 상태 조회 - 음수 ID (401 인증 우선)"

# 10. 매우 큰 사용자 ID (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose/fall-status/999999999" "401" "낙상 상태 조회 - 큰 ID (401 인증 우선)"

# =========================
# 4. 낙상 피드백 제출 테스트
# =========================
echo "${YELLOW}=== 4. 낙상 피드백 제출 테스트 ===${NC}"

# 11. 인증 없이 낙상 피드백 제출 (401 예상)
feedback_data='{"isFalsePositive": true, "userComment": "오탐지입니다"}'
test_endpoint "POST" "/api/v1/pose/fall-event/123/feedback" "401" "낙상 피드백 제출 - 인증 없음 (401)" "$feedback_data"

# 12. 잘못된 이벤트 ID 형식 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/pose/fall-event/invalid/feedback" "401" "낙상 피드백 - 잘못된 ID 형식 (401 인증 우선)" "$feedback_data"

# 13. 음수 이벤트 ID (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/pose/fall-event/-1/feedback" "401" "낙상 피드백 - 음수 ID (401 인증 우선)" "$feedback_data"

# 14. 잘못된 피드백 데이터 (401 예상 - 인증 우선)
invalid_feedback='{"invalid": "data"}'
test_endpoint "POST" "/api/v1/pose/fall-event/123/feedback" "401" "낙상 피드백 - 잘못된 데이터 (401 인증 우선)" "$invalid_feedback"

# =========================
# 5. HTTP 메서드 검증 테스트
# =========================
echo "${YELLOW}=== 5. HTTP 메서드 검증 테스트 ===${NC}"

# 15. 잘못된 HTTP 메서드 - GET으로 포즈 데이터 전송 시도 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose/data" "401" "포즈 데이터 전송 - GET 메서드 (401 인증 우선)"

# 16. 잘못된 HTTP 메서드 - DELETE로 포즈 데이터 전송 시도 (401 예상 - 인증 우선)
test_endpoint "DELETE" "/api/v1/pose/data" "401" "포즈 데이터 전송 - DELETE 메서드 (401 인증 우선)"

# 17. 잘못된 HTTP 메서드 - POST로 낙상 상태 조회 시도 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/pose/fall-status/123" "401" "낙상 상태 조회 - POST 메서드 (401 인증 우선)"

# 18. 잘못된 HTTP 메서드 - PUT으로 일괄 전송 시도 (401 예상 - 인증 우선)
test_endpoint "PUT" "/api/v1/pose/data/batch" "401" "일괄 포즈 데이터 - PUT 메서드 (401 인증 우선)" "$batch_pose_data"

# =========================
# 6. 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 6. 엣지 케이스 테스트 ===${NC}"

# 19. 존재하지 않는 엔드포인트 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose/nonexistent" "401" "존재하지 않는 포즈 엔드포인트 (401 인증 우선)"

# 20. 루트 포즈 경로 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/pose" "401" "루트 포즈 경로 (401 인증 우선)"

# 21. 빈 데이터로 포즈 전송 시도 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/pose/data" "401" "포즈 데이터 전송 - 빈 데이터 (401 인증 우선)" ""

# 22. 매우 큰 데이터 (401 예상 - 인증 우선)
large_data='{"userId": 123, "timestamp": "2024-01-01T10:00:00Z", "landmarks": ['
for i in {1..1000}; do
    large_data+="{\"x\": 0.5, \"y\": 0.5, \"z\": 0.1, \"visibility\": 0.9},"
done
large_data="${large_data%,}], \"confidence\": 0.85}"
test_endpoint "POST" "/api/v1/pose/data" "401" "포즈 데이터 전송 - 큰 데이터 (401 인증 우선)" "$large_data"

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
    printf "${YELLOW}📝 Pose API는 낙상감지를 위한 중요한 MediaPipe 연동 API입니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 Pose API는 낙상감지를 위한 중요한 MediaPipe 연동 API입니다${NC}\n"
    exit 1
fi