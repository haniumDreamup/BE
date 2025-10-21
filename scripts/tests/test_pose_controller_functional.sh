#!/bin/bash

# PoseController 기능 테스트 스크립트
# 4개 낙상감지/포즈데이터 엔드포인트의 실제 기능 테스트

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

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

    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')

    printf "   요청: $method $BASE_URL$endpoint\n"
    printf "   예상 상태: $expected_status, 실제 상태: $status_code\n"

    if [ "$status_code" = "$expected_status" ]; then
        printf "   ${GREEN}✓ 통과${NC}\n"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ -n "$response_body" ] && [ "$response_body" != "null" ]; then
            printf "   응답: $response_body\n"
        fi
    else
        printf "   ${RED}✗ 실패${NC}\n"
        printf "   응답 본문: $response_body\n"
    fi

    printf "\n"
}

echo "========================================"
echo "PoseController 기능 테스트 시작"
echo "4개 낙상감지/포즈데이터 엔드포인트 실제 기능 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. 포즈 데이터 전송 성공 테스트 (POST /api/v1/pose/data)
# =========================
echo "${YELLOW}=== 1. 포즈 데이터 전송 기능 테스트 ===${NC}"

# 1. 정상적인 포즈 데이터 전송 (실제 기능 테스트)
valid_pose_data='{
  "userId": 1,
  "timestamp": "2024-01-01T10:00:00Z",
  "landmarks": [
    {"x": 0.5, "y": 0.5, "z": 0.1, "visibility": 0.9},
    {"x": 0.6, "y": 0.4, "z": 0.2, "visibility": 0.8}
  ],
  "confidence": 0.85
}'
test_endpoint "POST" "/api/v1/pose/data" "200" "정상 포즈 데이터 전송 (200 성공)" "$valid_pose_data"

# 2. 잘못된 데이터 형식 (400 검증)
invalid_pose_data='{"userId": "invalid", "timestamp": "invalid", "landmarks": "not_array"}'
test_endpoint "POST" "/api/v1/pose/data" "400" "잘못된 포즈 데이터 형식 (400 검증 에러)" "$invalid_pose_data"

# 3. 필수 필드 누락 (400 검증)
incomplete_data='{"userId": 1}'
test_endpoint "POST" "/api/v1/pose/data" "400" "필수 필드 누락 (400 검증 에러)" "$incomplete_data"

# 4. 빈 landmarks 배열
empty_landmarks_data='{"userId": 1, "timestamp": "2024-01-01T10:00:00Z", "landmarks": [], "confidence": 0.85}'
test_endpoint "POST" "/api/v1/pose/data" "400" "빈 landmarks 배열 (400)" "$empty_landmarks_data"

# =========================
# 2. 일괄 포즈 데이터 전송 기능 테스트 (POST /api/v1/pose/data/batch)
# =========================
echo "${YELLOW}=== 2. 일괄 포즈 데이터 전송 기능 테스트 ===${NC}"

# 5. 정상적인 일괄 포즈 데이터 전송
batch_pose_data='[
  {
    "userId": 1,
    "timestamp": "2024-01-01T10:00:00Z",
    "landmarks": [{"x": 0.5, "y": 0.5, "z": 0.1, "visibility": 0.9}],
    "confidence": 0.85
  },
  {
    "userId": 1,
    "timestamp": "2024-01-01T10:00:01Z",
    "landmarks": [{"x": 0.6, "y": 0.4, "z": 0.2, "visibility": 0.8}],
    "confidence": 0.90
  }
]'
test_endpoint "POST" "/api/v1/pose/data/batch" "200" "정상 일괄 포즈 데이터 전송 (200)" "$batch_pose_data"

# 6. 빈 배열 (400 검증)
empty_array='[]'
test_endpoint "POST" "/api/v1/pose/data/batch" "400" "빈 배열 전송 (400)" "$empty_array"

# 7. 잘못된 배열 형식
invalid_array='"not an array"'
test_endpoint "POST" "/api/v1/pose/data/batch" "400" "잘못된 배열 형식 (400)" "$invalid_array"

# =========================
# 3. 낙상 상태 조회 기능 테스트 (GET /api/v1/pose/fall-status/{userId})
# =========================
echo "${YELLOW}=== 3. 낙상 상태 조회 기능 테스트 ===${NC}"

# 8. 정상적인 낙상 상태 조회
test_endpoint "GET" "/api/v1/pose/fall-status/1" "200" "정상 낙상 상태 조회 (200)"

# 9. 존재하지 않는 사용자 ID
test_endpoint "GET" "/api/v1/pose/fall-status/999999" "404" "존재하지 않는 사용자 ID (404)"

# 10. 잘못된 사용자 ID 형식
test_endpoint "GET" "/api/v1/pose/fall-status/invalid" "400" "잘못된 사용자 ID 형식 (400)"

# 11. 음수 사용자 ID
test_endpoint "GET" "/api/v1/pose/fall-status/-1" "400" "음수 사용자 ID (400)"

# =========================
# 4. 낙상 피드백 제출 기능 테스트 (POST /api/v1/pose/fall-event/{eventId}/feedback)
# =========================
echo "${YELLOW}=== 4. 낙상 피드백 제출 기능 테스트 ===${NC}"

# 12. 정상적인 낙상 피드백 제출
feedback_data='{"isFalsePositive": true, "userComment": "실제로는 넘어지지 않았습니다"}'
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "200" "정상 낙상 피드백 제출 (200)" "$feedback_data"

# 13. 존재하지 않는 이벤트 ID
test_endpoint "POST" "/api/v1/pose/fall-event/999999/feedback" "404" "존재하지 않는 이벤트 ID (404)" "$feedback_data"

# 14. 잘못된 이벤트 ID 형식
test_endpoint "POST" "/api/v1/pose/fall-event/invalid/feedback" "400" "잘못된 이벤트 ID 형식 (400)" "$feedback_data"

# 15. 잘못된 피드백 데이터
invalid_feedback='{"invalid": "data", "notBoolean": "true"}'
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "400" "잘못된 피드백 데이터 (400)" "$invalid_feedback"

# =========================
# 5. HTTP 메서드 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 5. HTTP 메서드 및 엣지 케이스 테스트 ===${NC}"

# 16. 잘못된 HTTP 메서드 - GET으로 포즈 데이터 전송 시도
test_endpoint "GET" "/api/v1/pose/data" "405" "포즈 데이터 전송 - GET 메서드 (405 Method Not Allowed)"

# 17. 잘못된 HTTP 메서드 - DELETE로 포즈 데이터 전송 시도
test_endpoint "DELETE" "/api/v1/pose/data" "405" "포즈 데이터 전송 - DELETE 메서드 (405)"

# 18. 잘못된 HTTP 메서드 - POST로 낙상 상태 조회 시도
test_endpoint "POST" "/api/v1/pose/fall-status/1" "405" "낙상 상태 조회 - POST 메서드 (405)"

# 19. 존재하지 않는 엔드포인트
test_endpoint "GET" "/api/v1/pose/nonexistent" "404" "존재하지 않는 포즈 엔드포인트 (404)"

# 20. 매우 큰 데이터 처리 테스트
large_data='{"userId": 1, "timestamp": "2024-01-01T10:00:00Z", "landmarks": ['
for i in {1..100}; do
    large_data+="{\"x\": 0.5, \"y\": 0.5, \"z\": 0.1, \"visibility\": 0.9},"
done
large_data="${large_data%,}], \"confidence\": 0.85}"
test_endpoint "POST" "/api/v1/pose/data" "200" "대용량 포즈 데이터 처리 (200)" "$large_data"

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
    printf "${YELLOW}📝 Pose API 기능이 정상적으로 작동합니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 일부 Pose API 기능에 문제가 있습니다${NC}\n"
    exit 1
fi