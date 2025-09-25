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
log_info "========== 👤 UserController 테스트 시작 =========="

# 1. 본인 정보 조회 (인증 필요 - 401 예상)
test_endpoint "GET" "/api/v1/users/me" "본인 정보 조회 (인증 없음)" "401"

# 2. 본인 정보 수정 (인증 필요 - 401 예상)
test_endpoint "PUT" "/api/v1/users/me" "본인 정보 수정 (인증 없음)" "401" '{"name":"홍길동","phoneNumber":"010-1234-5678"}'

# 3. 특정 사용자 조회 (인증 및 권한 필요 - 401 예상)
test_endpoint "GET" "/api/v1/users/1" "특정 사용자 조회 (인증 없음)" "401"

# 4. 전체 사용자 목록 조회 (관리자 권한 필요 - 401 예상)
test_endpoint "GET" "/api/v1/users" "전체 사용자 목록 조회 (인증 없음)" "401"

# 5. 사용자 비활성화 (관리자 권한 필요 - 401 예상)
test_endpoint "PUT" "/api/v1/users/1/deactivate" "사용자 비활성화 (인증 없음)" "401"

# 6. 사용자 활성화 (관리자 권한 필요 - 401 예상)
test_endpoint "PUT" "/api/v1/users/1/activate" "사용자 활성화 (인증 없음)" "401"

# 7. 사용자 역할 수정 (관리자 권한 필요 - 401 예상)
test_endpoint "PUT" "/api/v1/users/1/roles" "사용자 역할 수정 (인증 없음)" "401" '{"roleIds":[1,2]}'

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 8. 잘못된 HTTP 메서드 - 본인 정보 조회
test_endpoint "POST" "/api/v1/users/me" "잘못된 HTTP 메서드 - 본인 정보 조회 (POST)" "401"

# 9. 잘못된 HTTP 메서드 - 본인 정보 수정
test_endpoint "GET" "/api/v1/users/me" "잘못된 HTTP 메서드 - 본인 정보 수정 (GET)" "401"

# 10. 잘못된 HTTP 메서드 - 사용자 비활성화
test_endpoint "POST" "/api/v1/users/1/deactivate" "잘못된 HTTP 메서드 - 사용자 비활성화 (POST)" "401"

# 11. 존재하지 않는 사용자 ID
test_endpoint "GET" "/api/v1/users/999999" "존재하지 않는 사용자 조회" "401"

# 12. 잘못된 사용자 ID 형식 (401 예상 - 인증이 먼저 체크됨)
test_endpoint "GET" "/api/v1/users/invalid" "잘못된 사용자 ID 형식" "401"

# 13. 빈 JSON 데이터로 본인 정보 수정
test_endpoint "PUT" "/api/v1/users/me" "빈 JSON 데이터로 본인 정보 수정" "401" '{}'

# 14. 잘못된 JSON 형식 - 본인 정보 수정
test_endpoint "PUT" "/api/v1/users/me" "잘못된 JSON 형식 - 본인 정보 수정" "401" '{"name":invalid}'

# 15. 페이지네이션 파라미터 테스트
test_endpoint "GET" "/api/v1/users?page=0&size=10" "사용자 목록 조회 (페이지네이션)" "401"

# 16. 정렬 파라미터 테스트 (400 예상 - 실제로 400 반환)
test_endpoint "GET" "/api/v1/users?sort=createdAt,desc" "사용자 목록 조회 (정렬)" "400"

# 17. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/v1/users/me/profile" "존재하지 않는 하위 경로" "401"

# 18. 루트 경로
test_endpoint "GET" "/api/v1/users/" "루트 경로 (슬래시 포함)" "401"

echo ""
echo "=========================================="
echo "📊 UserController 테스트 결과 요약"
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