#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 서버 URL
BASE_URL="http://localhost:8080"

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
log_info "========== 🚨 EmergencyController SOS 테스트 시작 =========="

# EmergencyController SOS 엔드포인트는 테스트 엔드포인트가 없으므로 모든 엔드포인트에 인증 필요

# 1. SOS 발동 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "SOS 발동 (인증 없음)" "401" '{"latitude":37.5665,"longitude":126.9780,"emergencyType":"PANIC","message":"도움 필요","notifyAllContacts":true,"shareLocation":true}'

# 2. SOS 취소 (인증 필요 - 401)
test_endpoint "PUT" "/api/v1/emergency/sos/1/cancel" "SOS 취소 (인증 없음)" "401"

# 3. SOS 이력 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/v1/emergency/sos/history" "SOS 이력 조회 (인증 없음)" "401"

# 4. 빠른 SOS (인증 필요 - 401)
test_endpoint "POST" "/api/v1/emergency/sos/quick" "빠른 SOS (인증 없음)" "401" "" ""

# 5. 빠른 SOS 쿼리 파라미터 (인증 필요 - 401)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665&longitude=126.9780" "빠른 SOS 쿼리 파라미터 (인증 없음)" "401"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 6. 잘못된 HTTP 메서드 - SOS 발동 (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/v1/emergency/sos/trigger" "잘못된 HTTP 메서드 - SOS 발동 (GET)" "401"

# 7. 잘못된 HTTP 메서드 - SOS 취소 (POST - 401, 인증이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/sos/1/cancel" "잘못된 HTTP 메서드 - SOS 취소 (POST)" "401"

# 8. 잘못된 HTTP 메서드 - SOS 이력 조회 (POST - 401)
test_endpoint "POST" "/api/v1/emergency/sos/history" "잘못된 HTTP 메서드 - SOS 이력 조회 (POST)" "401"

# 9. 잘못된 HTTP 메서드 - 빠른 SOS (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/v1/emergency/sos/quick" "잘못된 HTTP 메서드 - 빠른 SOS (GET)" "401"

# 10. 존재하지 않는 긴급상황 ID - SOS 취소
test_endpoint "PUT" "/api/v1/emergency/sos/999999/cancel" "존재하지 않는 긴급상황 ID - SOS 취소" "401"

# 11. 잘못된 긴급상황 ID 형식 - SOS 취소
test_endpoint "PUT" "/api/v1/emergency/sos/invalid/cancel" "잘못된 긴급상황 ID 형식 - SOS 취소" "401"

# 12. 빈 JSON 데이터로 SOS 발동
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "빈 JSON 데이터로 SOS 발동" "401" '{}'

# 13. 잘못된 JSON 형식 - SOS 발동
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "잘못된 JSON 형식 - SOS 발동" "401" '{"latitude":invalid_json}'

# 14. 필수 파라미터 누락 - 빠른 SOS (위도만) (인증이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665" "필수 파라미터 누락 - 빠른 SOS (위도만)" "401"

# 15. 필수 파라미터 누락 - 빠른 SOS (경도만) (인증이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/sos/quick?longitude=126.9780" "필수 파라미터 누락 - 빠른 SOS (경도만)" "401"

# 16. 잘못된 위도 값 - 빠른 SOS (인증이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=invalid&longitude=126.9780" "잘못된 위도 값 - 빠른 SOS" "401"

# 17. 잘못된 경도 값 - 빠른 SOS (인증이 먼저 체크됨)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665&longitude=invalid" "잘못된 경도 값 - 빠른 SOS" "401"

# 18. 범위 초과 위도 값 - 빠른 SOS
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=91.0&longitude=126.9780" "범위 초과 위도 값 - 빠른 SOS" "401"

# 19. 범위 초과 경도 값 - 빠른 SOS
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665&longitude=181.0" "범위 초과 경도 값 - 빠른 SOS" "401"

# 20. 페이지네이션 파라미터 테스트 - SOS 이력 조회
test_endpoint "GET" "/api/v1/emergency/sos/history?page=0&size=10" "SOS 이력 조회 (페이지네이션)" "401"

# 21. 정렬 파라미터 테스트 - SOS 이력 조회
test_endpoint "GET" "/api/v1/emergency/sos/history?sort=createdAt,desc" "SOS 이력 조회 (정렬)" "400"

# 22. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/v1/emergency/sos/trigger/status" "존재하지 않는 하위 경로" "401"

# 23. 루트 경로
test_endpoint "GET" "/api/v1/emergency/sos/" "루트 경로 (슬래시 포함)" "401"

# 24. 특수 문자가 포함된 ID
test_endpoint "PUT" "/api/v1/emergency/sos/@#$/cancel" "특수 문자 포함 ID - SOS 취소" "401"

# 25. 매우 긴 ID 값
test_endpoint "PUT" "/api/v1/emergency/sos/123456789012345678901234567890/cancel" "매우 긴 ID 값 - SOS 취소" "401"

# 26. 빈 경로 파라미터 (경로 파싱 오류)
test_endpoint "PUT" "/api/v1/emergency/sos//cancel" "빈 경로 파라미터 - SOS 취소" "400"

# 27. 네거티브 ID 값
test_endpoint "PUT" "/api/v1/emergency/sos/-1/cancel" "네거티브 ID 값 - SOS 취소" "401"

# 28. 0 ID 값
test_endpoint "PUT" "/api/v1/emergency/sos/0/cancel" "0 ID 값 - SOS 취소" "401"

# 29. 쿼리 파라미터 조합 테스트 - SOS 이력 조회
test_endpoint "GET" "/api/v1/emergency/sos/history?startDate=2024-01-01&endDate=2024-01-31&type=PANIC" "SOS 이력 조회 (쿼리 파라미터 조합)" "401"

# 30. JSON 유효성 검증 - 잘못된 emergencyType
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "잘못된 emergencyType - SOS 발동" "401" '{"latitude":37.5665,"longitude":126.9780,"emergencyType":"INVALID_TYPE","message":"도움 필요"}'

echo ""
echo "=========================================="
echo "📊 EmergencyController SOS 테스트 결과 요약"
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