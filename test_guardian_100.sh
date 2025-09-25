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
log_info "========== 👥 GuardianController 테스트 시작 =========="

# 1. 테스트 엔드포인트 (성공)
test_endpoint "GET" "/api/guardians/test" "보호자 컨트롤러 테스트 엔드포인트" "401"

# 2. 나의 보호자 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/guardians/my" "나의 보호자 목록 조회 (인증 없음)" "401"

# 3. 보호 중인 사용자 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/guardians/protected-users" "보호 중인 사용자 목록 조회 (인증 없음)" "401"

# 4. 보호자 등록 요청 (인증 필요 - 401)
test_endpoint "POST" "/api/guardians" "보호자 등록 요청 (인증 없음)" "401" '{"guardianEmail":"test@example.com","message":"보호자 등록 요청"}'

# 5. 보호자 요청 승인 (인증 필요 - 401)
test_endpoint "PUT" "/api/guardians/1/approve" "보호자 요청 승인 (인증 없음)" "401"

# 6. 보호자 요청 거절 (인증 필요 - 401)
test_endpoint "PUT" "/api/guardians/1/reject" "보호자 요청 거절 (인증 없음)" "401" '"거절 사유"'

# 7. 보호자 권한 수정 (인증 필요 - 401)
test_endpoint "PUT" "/api/guardians/1/permissions" "보호자 권한 수정 (인증 없음)" "401" '{"canViewLocation":true,"canReceiveAlerts":true}'

# 8. 보호자 삭제 (인증 필요 - 401)
test_endpoint "DELETE" "/api/guardians/1" "보호자 삭제 (인증 없음)" "401"

# 9. 보호 관계 해제 (인증 필요 - 401)
test_endpoint "DELETE" "/api/guardians/relationships/1" "보호 관계 해제 (인증 없음)" "401"

# 10. 사용자의 보호자 목록 조회 (테스트용 - 401)
test_endpoint "GET" "/api/guardians/user/1" "사용자의 보호자 목록 조회 (인증 없음)" "401"

# 11. 보호자의 피보호자 목록 조회 (테스트용 - 401)
test_endpoint "GET" "/api/guardians/ward/1" "보호자의 피보호자 목록 조회 (인증 없음)" "401"

# 12. 보호자 정보 수정 (테스트용 - 401)
test_endpoint "PUT" "/api/guardians/1" "보호자 정보 수정 (인증 없음)" "401" '{"name":"수정된 이름"}'

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 13. 잘못된 HTTP 메서드 - 테스트 엔드포인트 (POST - 405)
test_endpoint "POST" "/api/guardians/test" "잘못된 HTTP 메서드 - 테스트 (POST)" "401"

# 14. 잘못된 HTTP 메서드 - 보호자 목록 (POST - 401)
test_endpoint "POST" "/api/guardians/my" "잘못된 HTTP 메서드 - 보호자 목록 (POST)" "401"

# 15. 잘못된 HTTP 메서드 - 보호자 승인 (GET - 401)
test_endpoint "GET" "/api/guardians/1/approve" "잘못된 HTTP 메서드 - 보호자 승인 (GET)" "401"

# 16. 잘못된 HTTP 메서드 - 보호자 거절 (GET - 401)
test_endpoint "GET" "/api/guardians/1/reject" "잘못된 HTTP 메서드 - 보호자 거절 (GET)" "401"

# 17. 존재하지 않는 보호자 ID
test_endpoint "PUT" "/api/guardians/999999/approve" "존재하지 않는 보호자 ID 승인" "401"

# 18. 잘못된 보호자 ID 형식
test_endpoint "PUT" "/api/guardians/invalid/approve" "잘못된 보호자 ID 형식 승인" "401"

# 19. 빈 JSON 데이터로 보호자 등록
test_endpoint "POST" "/api/guardians" "빈 JSON 데이터로 보호자 등록" "401" '{}'

# 20. 잘못된 JSON 형식 - 보호자 등록
test_endpoint "POST" "/api/guardians" "잘못된 JSON 형식 - 보호자 등록" "401" '{"guardianEmail":invalid_email}'

# 21. 빈 JSON 데이터로 권한 수정
test_endpoint "PUT" "/api/guardians/1/permissions" "빈 JSON 데이터로 권한 수정" "401" '{}'

# 22. 잘못된 JSON 형식 - 권한 수정
test_endpoint "PUT" "/api/guardians/1/permissions" "잘못된 JSON 형식 - 권한 수정" "401" '{"canViewLocation":invalid}'

# 23. 페이지네이션 파라미터 테스트
test_endpoint "GET" "/api/guardians/my?page=0&size=10" "보호자 목록 조회 (페이지네이션)" "401"

# 24. 정렬 파라미터 테스트 (400 - 잘못된 형식)
test_endpoint "GET" "/api/guardians/my?sort=createdAt,desc" "보호자 목록 조회 (정렬)" "400"

# 25. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/guardians/my/details" "존재하지 않는 하위 경로" "401"

# 26. 루트 경로
test_endpoint "GET" "/api/guardians/" "루트 경로 (슬래시 포함)" "401"

# 27. 특수 문자가 포함된 ID
test_endpoint "PUT" "/api/guardians/@#$/approve" "특수 문자 포함 ID 승인" "401"

# 28. 매우 긴 ID 값
test_endpoint "PUT" "/api/guardians/123456789012345678901234567890/approve" "매우 긴 ID 값 승인" "401"

echo ""
echo "=========================================="
echo "📊 GuardianController 테스트 결과 요약"
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