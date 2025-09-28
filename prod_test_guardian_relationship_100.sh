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
log_info "========== 🤝 GuardianRelationshipController 테스트 시작 =========="

# GuardianRelationshipController는 /api/guardian-relationships 경로이며 대부분 인증 필요 401 반환

# 1. 보호자 초대 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/invite" "보호자 초대 (인증 필요)" "401" '{"userId":1,"guardianEmail":"guardian@example.com","relationship":"PARENT"}'

# 2. 초대 수락 (public 엔드포인트 - 400: 파라미터 필수)
test_endpoint "POST" "/api/guardian-relationships/accept-invitation" "초대 수락 (파라미터 없음)" "400"

# 3. 초대 수락 (token과 guardianId 포함 - 400: 잘못된 토큰)
test_endpoint "POST" "/api/guardian-relationships/accept-invitation?token=invalid&guardianId=1" "초대 수락 (잘못된 토큰)" "400"

# 4. 초대 거부 (public 엔드포인트 - 400: 파라미터 필수)
test_endpoint "POST" "/api/guardian-relationships/reject-invitation" "초대 거부 (파라미터 없음)" "400"

# 5. 초대 거부 (token과 guardianId 포함 - 400: 잘못된 토큰)
test_endpoint "POST" "/api/guardian-relationships/reject-invitation?token=invalid&guardianId=1" "초대 거부 (잘못된 토큰)" "400"

# 6. 권한 수정 (인증 필요 - 401)
test_endpoint "PUT" "/api/guardian-relationships/1/permissions" "권한 수정 (인증 필요)" "401" '{"permissions":["VIEW_LOCATION","SEND_NOTIFICATIONS"]}'

# 7. 관계 일시 중지 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/1/suspend" "관계 일시 중지 (인증 필요)" "401"

# 8. 관계 재활성화 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/1/reactivate" "관계 재활성화 (인증 필요)" "401"

# 9. 관계 종료 (인증 필요 - 401)
test_endpoint "DELETE" "/api/guardian-relationships/1" "관계 종료 (인증 필요)" "401"

# 10. 사용자의 보호자 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/user/1" "사용자의 보호자 목록 조회 (인증 필요)" "401"

# 11. 보호자의 피보호자 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/guardian/1" "보호자의 피보호자 목록 조회 (인증 필요)" "401"

# 12. 긴급 연락 보호자 목록 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/user/1/emergency-contacts" "긴급 연락 보호자 목록 조회 (인증 필요)" "401"

# 13. 권한 확인 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/check-permission?guardianId=1&userId=1&permissionType=VIEW_LOCATION" "권한 확인 (인증 필요)" "401"

# 14. 활동 시간 업데이트 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/update-activity?guardianId=1&userId=1" "활동 시간 업데이트 (인증 필요)" "401"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 15. 잘못된 HTTP 메서드 - 보호자 초대 (GET - 인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/guardian-relationships/invite" "잘못된 HTTP 메서드 - 보호자 초대 (GET)" "401"

# 16. 잘못된 HTTP 메서드 - 초대 수락 (GET - 405)
test_endpoint "GET" "/api/guardian-relationships/accept-invitation" "잘못된 HTTP 메서드 - 초대 수락 (GET)" "405"

# 17. 잘못된 HTTP 메서드 - 권한 수정 (POST - 인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/guardian-relationships/1/permissions" "잘못된 HTTP 메서드 - 권한 수정 (POST)" "401"

# 18. 잘못된 관계 ID - 권한 수정 (인증이 먼저 체크됨 - 401)
test_endpoint "PUT" "/api/guardian-relationships/invalid/permissions" "잘못된 관계 ID - 권한 수정" "401"

# 19. 0 관계 ID - 일시 중지 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/0/suspend" "0 관계 ID - 일시 중지" "401"

# 20. 네거티브 관계 ID - 재활성화 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/-1/reactivate" "네거티브 관계 ID - 재활성화" "401"

# 21. 잘못된 사용자 ID 형식 - 보호자 목록 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/guardian-relationships/user/invalid" "잘못된 사용자 ID 형식 - 보호자 목록" "401"

# 22. 0 보호자 ID - 피보호자 목록 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/guardian/0" "0 보호자 ID - 피보호자 목록" "401"

# 23. 네거티브 사용자 ID - 긴급 연락처 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/user/-1/emergency-contacts" "네거티브 사용자 ID - 긴급 연락처" "401"

# 24. 누락된 파라미터 - 권한 확인 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/guardian-relationships/check-permission?guardianId=1" "누락된 파라미터 - 권한 확인" "401"

# 25. 잘못된 파라미터 값 - 활동 시간 업데이트 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/update-activity?guardianId=invalid&userId=1" "잘못된 파라미터 값 - 활동 시간 업데이트" "401"

# 26. 빈 경로 파라미터 - 권한 수정 (400)
test_endpoint "PUT" "/api/guardian-relationships//permissions" "빈 경로 파라미터 - 권한 수정" "400"

# 27. 존재하지 않는 하위 경로 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/guardian-relationships/nonexistent" "존재하지 않는 하위 경로" "401"

# 28. 루트 경로 (인증이 먼저 체크됨 - 401)
test_endpoint "GET" "/api/guardian-relationships/" "루트 경로" "401"

# 29. 잘못된 Content-Type - 보호자 초대 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/invite" "잘못된 Content-Type - 보호자 초대" "401" 'invalid json'

# 30. 특수 문자 포함 ID - 관계 종료 (인증이 먼저 체크됨 - 401)
test_endpoint "DELETE" "/api/guardian-relationships/@#$" "특수 문자 포함 ID - 관계 종료" "401"

# 31. 매우 긴 ID 값 - 일시 중지 (인증이 먼저 체크됨 - 401)
test_endpoint "POST" "/api/guardian-relationships/123456789012345678901234567890/suspend" "매우 긴 ID 값 - 일시 중지" "401"

# 32. 쿼리 파라미터 포함 - 보호자 목록 (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/user/1?activeOnly=false&extra=value" "쿼리 파라미터 포함 - 보호자 목록" "401"

# 33. 대소문자 잘못된 경로 (404)
test_endpoint "GET" "/api/Guardian-Relationships/user/1" "대소문자 잘못된 경로" "404"

# 34. 잘못된 API 버전 경로 (404)
test_endpoint "GET" "/api/v1/guardian-relationships/user/1" "잘못된 API 버전 경로" "404"

# 35. 빈 JSON 데이터 - 보호자 초대 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/invite" "빈 JSON 데이터 - 보호자 초대" "401" "{}"

# 36. 권한 확인 잘못된 permissionType (인증 필요 - 401)
test_endpoint "GET" "/api/guardian-relationships/check-permission?guardianId=1&userId=1&permissionType=INVALID" "권한 확인 잘못된 permissionType" "401"

# 37. 활동 시간 업데이트 네거티브 파라미터 (인증 필요 - 401)
test_endpoint "POST" "/api/guardian-relationships/update-activity?guardianId=-1&userId=-1" "활동 시간 업데이트 네거티브 파라미터" "401"

echo ""
echo "=========================================="
echo "📊 GuardianRelationshipController 테스트 결과 요약"
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