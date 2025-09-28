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
log_info "========== 🚨 EmergencyController 테스트 시작 =========="

# 1. 테스트 엔드포인트 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/test" "긴급상황 컨트롤러 테스트 엔드포인트 (인증 없음)" "401"

# 2. 긴급상황 신고 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/emergency/alert" "긴급상황 신고 (인증 없음)" "401" '{"type":"FALL","latitude":37.5665,"longitude":126.9780,"description":"테스트 긴급상황"}'

# 3. 낙상 감지 신고 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/emergency/fall-detection" "낙상 감지 신고 (인증 없음)" "401" '{"confidence":0.95,"latitude":37.5665,"longitude":126.9780}'

# 4. 긴급상황 상태 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/status/1" "긴급상황 상태 조회 (인증 없음)" "401"

# 5. 전체 긴급상황 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency" "전체 긴급상황 목록 조회 (인증 없음)" "401"

# 6. 긴급상황 신고 (테스트용 - 인증 필요)
test_endpoint "POST" "/api/v1/emergency/report" "긴급상황 신고 테스트용 (인증 없음)" "401"

# 7. 사용자별 긴급상황 이력 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/user/1/history" "사용자별 긴급상황 이력 조회 (인증 없음)" "401"

# 8. 긴급상황 이력 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/history/1" "긴급상황 이력 조회 (인증 없음)" "401"

# 9. 활성 긴급상황 목록 조회 (보호자/관리자 권한 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/active" "활성 긴급상황 목록 조회 (인증 없음)" "401"

# 10. 긴급상황 해결 처리 (인증 필요 - 401)
test_endpoint "PUT" "/api/v1/emergency/1/resolve" "긴급상황 해결 처리 (인증 없음)" "401" "" ""

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 11. 잘못된 HTTP 메서드 - 테스트 엔드포인트 (POST - 401, 보안이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/test" "잘못된 HTTP 메서드 - 테스트 (POST)" "401"

# 12. 잘못된 HTTP 메서드 - 긴급상황 상태 조회 (POST - 401)
test_endpoint "POST" "/api/v1/emergency/status/1" "잘못된 HTTP 메서드 - 상태 조회 (POST)" "401"

# 13. 잘못된 HTTP 메서드 - 전체 목록 (POST - 401)
test_endpoint "POST" "/api/v1/emergency" "잘못된 HTTP 메서드 - 전체 목록 (POST)" "401"

# 14. 잘못된 HTTP 메서드 - 해결 처리 (GET - 401)
test_endpoint "GET" "/api/v1/emergency/1/resolve" "잘못된 HTTP 메서드 - 해결 처리 (GET)" "401"

# 15. 존재하지 않는 긴급상황 ID
test_endpoint "GET" "/api/v1/emergency/status/999999" "존재하지 않는 긴급상황 ID" "401"

# 16. 잘못된 긴급상황 ID 형식
test_endpoint "GET" "/api/v1/emergency/status/invalid" "잘못된 긴급상황 ID 형식" "401"

# 17. 빈 JSON 데이터로 긴급상황 신고
test_endpoint "POST" "/api/v1/emergency/alert" "빈 JSON 데이터로 긴급상황 신고" "401" '{}'

# 18. 잘못된 JSON 형식 - 긴급상황 신고
test_endpoint "POST" "/api/v1/emergency/alert" "잘못된 JSON 형식 - 긴급상황 신고" "401" '{"type":invalid_json}'

# 19. 빈 JSON 데이터로 낙상 감지
test_endpoint "POST" "/api/v1/emergency/fall-detection" "빈 JSON 데이터로 낙상 감지" "401" '{}'

# 20. 잘못된 JSON 형식 - 낙상 감지
test_endpoint "POST" "/api/v1/emergency/fall-detection" "잘못된 JSON 형식 - 낙상 감지" "401" '{"confidence":invalid}'

# 21. 페이지네이션 파라미터 테스트
test_endpoint "GET" "/api/v1/emergency/history/1?page=0&size=10" "긴급상황 이력 조회 (페이지네이션)" "401"

# 22. 정렬 파라미터 테스트 (잘못된 형식이므로 400이 맞음)
test_endpoint "GET" "/api/v1/emergency/history/1?sort=createdAt,desc" "긴급상황 이력 조회 (정렬)" "400"

# 23. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/v1/emergency/status/1/details" "존재하지 않는 하위 경로" "401"

# 24. 루트 경로
test_endpoint "GET" "/api/v1/emergency/" "루트 경로 (슬래시 포함)" "401"

# 25. 해결 처리 쿼리 파라미터 테스트 (인증 필요)
test_endpoint "PUT" "/api/v1/emergency/1/resolve?resolvedBy=test&notes=test" "해결 처리 쿼리 파라미터 테스트" "401"

echo ""
echo "=========================================="
echo "📊 EmergencyController 테스트 결과 요약"
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