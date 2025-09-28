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
log_info "========== 📍 GeofenceController 테스트 시작 =========="

# GeofenceController는 /api/geofences 경로이며 인증이 필요한 엔드포인트들이므로 401 예상

# 1. 지오펜스 생성 (인증 필요 - 401)
test_endpoint "POST" "/api/geofences" "지오펜스 생성 (인증 필요)" "401" '{"name":"집","description":"우리집","latitude":37.5665,"longitude":126.9780,"radius":100,"isActive":true}'

# 2. 사용자의 지오펜스 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/geofences" "사용자의 지오펜스 목록 조회 (인증 필요)" "401"

# 3. 특정 지오펜스 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/geofences/1" "특정 지오펜스 조회 (인증 필요)" "401"

# 4. 지오펜스 수정 (인증 필요 - 401)
test_endpoint "PUT" "/api/geofences/1" "지오펜스 수정 (인증 필요)" "401" '{"name":"회사","description":"직장","latitude":37.5665,"longitude":126.9780,"radius":200,"isActive":true}'

# 5. 지오펜스 삭제 (인증 필요 - 401)
test_endpoint "DELETE" "/api/geofences/1" "지오펜스 삭제 (인증 필요)" "401"

# 6. 지오펜스 활성화/비활성화 토글 (인증 필요 - 401, PATCH 메서드)
test_endpoint "PATCH" "/api/geofences/1/toggle" "지오펜스 활성화/비활성화 토글 (인증 필요)" "401"

# 7. 지오펜스 페이징 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/geofences/paged" "지오펜스 페이징 조회 (인증 필요)" "401"

# 8. 타입별 지오펜스 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/geofences/type/HOME" "타입별 지오펜스 조회 (인증 필요)" "401"

# 9. 우선순위 변경 (인증 필요 - 401)
test_endpoint "PUT" "/api/geofences/priorities" "우선순위 변경 (인증 필요)" "401" '[1,2,3]'

# 10. 지오펜스 통계 (인증 필요 - 401)
test_endpoint "GET" "/api/geofences/stats" "지오펜스 통계 (인증 필요)" "401"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 11. 잘못된 HTTP 메서드 - 지오펜스 목록 조회 (PUT - 인증이 먼저 체크됨 - 401)
test_endpoint "PUT" "/api/geofences" "잘못된 HTTP 메서드 - 지오펜스 목록 조회 (PUT)" "401"

# 12. 잘못된 HTTP 메서드 - 지오펜스 조회 (POST - 인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/geofences/1" "잘못된 HTTP 메서드 - 지오펜스 조회 (POST)" "401"

# 13. 잘못된 HTTP 메서드 - 토글 (GET - 인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/1/toggle" "잘못된 HTTP 메서드 - 토글 (GET)" "401"

# 14. 잘못된 HTTP 메서드 - 타입별 조회 (POST - 인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/geofences/type/HOME" "잘못된 HTTP 메서드 - 타입별 조회 (POST)" "401"

# 15. 존재하지 않는 지오펜스 ID (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/999999" "존재하지 않는 지오펜스 ID 조회" "401"

# 16. 잘못된 지오펜스 ID 형식 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/invalid" "잘못된 지오펜스 ID 형식 조회" "401"

# 17. 0 지오펜스 ID (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/0" "0 지오펜스 ID 조회" "401"

# 18. 네거티브 지오펜스 ID (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/-1" "네거티브 지오펜스 ID 조회" "401"

# 19. 빈 JSON 데이터로 지오펜스 생성 (인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/geofences" "빈 JSON 데이터로 지오펜스 생성" "401" "{}"

# 20. 잘못된 JSON 형식 - 지오펜스 생성 (인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/geofences" "잘못된 JSON 형식 - 지오펜스 생성" "401" "invalid json"

# 21. 잘못된 지오펜스 타입 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/type/INVALID" "잘못된 지오펜스 타입 조회" "401"

# 22. 페이지네이션 파라미터 테스트 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences?activeOnly=true" "지오펜스 목록 조회 (필터링)" "401"

# 23. 페이징 파라미터 테스트 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/paged?page=0&size=10" "지오펜스 페이징 조회 (파라미터)" "401"

# 24. 존재하지 않는 하위 경로 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/settings" "존재하지 않는 하위 경로" "401"

# 25. 루트 경로 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/" "루트 경로 (슬래시 포함)" "401"

# 26. 특수 문자가 포함된 ID (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/@#$" "특수 문자 포함 ID 조회" "401"

# 27. 매우 긴 ID 값 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/123456789012345678901234567890" "매우 긴 ID 값 조회" "401"

# 28. 빈 경로 파라미터 (400)
test_endpoint "GET" "/api/geofences//stats" "빈 경로 파라미터 - 통계 조회" "400"

# 29. 쿼리 파라미터 포함 테스트 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/geofences/1?detailed=true" "쿼리 파라미터 포함 조회" "401"

# 30. 빈 우선순위 배열 테스트 (인증이 먼저 체크됨 - 401)
test_endpoint "PUT" "/api/geofences/priorities" "빈 우선순위 배열 테스트" "401" "[]"

echo ""
echo "=========================================="
echo "📊 GeofenceController 테스트 결과 요약"
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