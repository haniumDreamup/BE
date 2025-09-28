#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 서버 URL
BASE_URL="http://43.200.49.171:8080"

# 테스트 카운터
TOTAL_TESTS=0
PASSED_TESTS=0

# 로그 함수들
log_info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 테스트 함수
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local expected_status="$4"
    local data="$5"
    local auth_header="$6"

    ((TOTAL_TESTS++))

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    if [[ -n "$auth_header" ]]; then
        curl_cmd="$curl_cmd -H 'Authorization: $auth_header'"
    fi

    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    local response
    response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"

    echo "🔍 테스트: $description"
    echo "📤 요청: $method $endpoint"
    echo "📊 응답상태: $status_code"
    echo "📄 응답내용: ${body:0:200}..."

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$description - 상태: $status_code"
    else
        log_error "$description - 예상: $expected_status, 실제: $status_code"
    fi

    echo "----------------------------------------"
}

# 메인 테스트 시작
log_info "========== 🤸 PoseController 테스트 시작 =========="

# PoseController는 테스트 엔드포인트가 없으므로 모든 엔드포인트에 인증 필요

# 1. Pose 데이터 전송 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/pose/data" "Pose 데이터 전송 (인증 없음)" "401" '{"userId":1,"timestamp":"2024-01-01T10:00:00","landmarks":[{"x":0.5,"y":0.5,"z":0.0,"visibility":0.9}],"frameWidth":640,"frameHeight":480}'

# 2. Pose 데이터 일괄 전송 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/pose/data/batch" "Pose 데이터 일괄 전송 (인증 없음)" "401" '[{"userId":1,"timestamp":"2024-01-01T10:00:00","landmarks":[{"x":0.5,"y":0.5,"z":0.0,"visibility":0.9}],"frameWidth":640,"frameHeight":480}]'

# 3. 낙상 감지 상태 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/pose/fall-status/1" "낙상 감지 상태 조회 (인증 없음)" "401"

# 4. 낙상 이벤트 피드백 제출 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "낙상 이벤트 피드백 제출 (인증 없음)" "401" '{"isFalsePositive":true,"userComment":"실제로는 낙상이 아니었습니다"}'

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 5. 잘못된 HTTP 메서드 - Pose 데이터 전송 (GET - 401)
test_endpoint "GET" "/api/v1/pose/data" "잘못된 HTTP 메서드 - Pose 데이터 전송 (GET)" "401"

# 6. 잘못된 HTTP 메서드 - Pose 데이터 일괄 전송 (GET - 401)
test_endpoint "GET" "/api/v1/pose/data/batch" "잘못된 HTTP 메서드 - Pose 데이터 일괄 전송 (GET)" "401"

# 7. 잘못된 HTTP 메서드 - 낙상 상태 조회 (POST - 401)
test_endpoint "POST" "/api/v1/pose/fall-status/1" "잘못된 HTTP 메서드 - 낙상 상태 조회 (POST)" "401"

# 8. 잘못된 HTTP 메서드 - 낙상 피드백 (GET - 401)
test_endpoint "GET" "/api/v1/pose/fall-event/1/feedback" "잘못된 HTTP 메서드 - 낙상 피드백 (GET)" "401"

# 9. 존재하지 않는 사용자 ID - 낙상 상태 조회 (인증이 먼저 체크됨 - 500)
test_endpoint "GET" "/api/v1/pose/fall-status/999999" "존재하지 않는 사용자 ID - 낙상 상태 조회" "401"

# 10. 잘못된 사용자 ID 형식 - 낙상 상태 조회 (400)
test_endpoint "GET" "/api/v1/pose/fall-status/invalid" "잘못된 사용자 ID 형식 - 낙상 상태 조회" "401"

# 11. 0 사용자 ID - 낙상 상태 조회 (인증이 먼저 체크됨 - 500)
test_endpoint "GET" "/api/v1/pose/fall-status/0" "0 사용자 ID - 낙상 상태 조회" "401"

# 12. 네거티브 사용자 ID - 낙상 상태 조회 (인증이 먼저 체크됨 - 500)
test_endpoint "GET" "/api/v1/pose/fall-status/-1" "네거티브 사용자 ID - 낙상 상태 조회" "401"

# 13. 존재하지 않는 이벤트 ID - 낙상 피드백 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/fall-event/999999/feedback" "존재하지 않는 이벤트 ID - 낙상 피드백" "401" '{"isFalsePositive":false}'

# 14. 잘못된 이벤트 ID 형식 - 낙상 피드백 (400)
test_endpoint "POST" "/api/v1/pose/fall-event/invalid/feedback" "잘못된 이벤트 ID 형식 - 낙상 피드백" "401" '{"isFalsePositive":false}'

# 15. 0 이벤트 ID - 낙상 피드백 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/fall-event/0/feedback" "0 이벤트 ID - 낙상 피드백" "401" '{"isFalsePositive":false}'

# 16. 빈 JSON 데이터로 Pose 데이터 전송 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/data" "빈 JSON 데이터로 Pose 데이터 전송" "401" '{}'

# 17. 잘못된 JSON 형식 - Pose 데이터 전송 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/data" "잘못된 JSON 형식 - Pose 데이터 전송" "401" '{"userId":invalid_json}'

# 18. 빈 배열로 Pose 데이터 일괄 전송 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/data/batch" "빈 배열로 Pose 데이터 일괄 전송" "401" '[]'

# 19. 잘못된 JSON 형식 - Pose 데이터 일괄 전송 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/data/batch" "잘못된 JSON 형식 - Pose 데이터 일괄 전송" "401" '[{"userId":invalid}]'

# 20. 빈 JSON 데이터로 낙상 피드백 제출 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "빈 JSON 데이터로 낙상 피드백 제출" "401" '{}'

# 21. 잘못된 JSON 형식 - 낙상 피드백 제출 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "잘못된 JSON 형식 - 낙상 피드백 제출" "401" '{"isFalsePositive":invalid}'

# 22. 존재하지 않는 하위 경로 (404)
test_endpoint "GET" "/api/v1/pose/data/status" "존재하지 않는 하위 경로" "401"

# 23. 루트 경로 (404)
test_endpoint "GET" "/api/v1/pose/" "루트 경로 (슬래시 포함)" "401"

# 24. 특수 문자가 포함된 사용자 ID (400)
test_endpoint "GET" "/api/v1/pose/fall-status/@#$" "특수 문자 포함 사용자 ID - 낙상 상태" "401"

# 25. 매우 긴 사용자 ID 값 (인증이 먼저 체크됨 - 500)
test_endpoint "GET" "/api/v1/pose/fall-status/123456789012345678901234567890" "매우 긴 사용자 ID 값 - 낙상 상태" "401"

# 26. 특수 문자가 포함된 이벤트 ID (400)
test_endpoint "POST" "/api/v1/pose/fall-event/@#$/feedback" "특수 문자 포함 이벤트 ID - 낙상 피드백" "401" '{"isFalsePositive":false}'

# 27. 매우 긴 이벤트 ID 값 (인증이 먼저 체크됨 - 500)
test_endpoint "POST" "/api/v1/pose/fall-event/123456789012345678901234567890/feedback" "매우 긴 이벤트 ID 값 - 낙상 피드백" "401" '{"isFalsePositive":false}'

# 28. 빈 경로 파라미터 - 낙상 상태 조회 (404)
test_endpoint "GET" "/api/v1/pose/fall-status/" "빈 경로 파라미터 - 낙상 상태 조회" "401"

# 29. 빈 경로 파라미터 - 낙상 피드백 (400)
test_endpoint "POST" "/api/v1/pose/fall-event//feedback" "빈 경로 파라미터 - 낙상 피드백" "400"

# 30. 쿼리 파라미터 테스트 (인증이 먼저 체크됨 - 500)
test_endpoint "GET" "/api/v1/pose/fall-status/1?detailed=true" "쿼리 파라미터 포함 - 낙상 상태 조회" "401"

echo ""
echo "=========================================="
echo "📊 PoseController 테스트 결과 요약"
echo "=========================================="
echo "총 테스트: $TOTAL_TESTS"
echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
echo -e "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}"

# 성공률 계산
success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo "성공률: $success_rate%"

if [[ $success_rate -eq 100 ]]; then
    echo -e "${GREEN}🎉  100% 성공! 모든 테스트 통과${NC}"
elif [[ $success_rate -ge 80 ]]; then
    echo -e "${YELLOW}⚠️   양호: $success_rate% 성공률${NC}"
else
    echo -e "${RED}❌  개선 필요: $success_rate% 성공률${NC}"
fi

echo "=========================================="