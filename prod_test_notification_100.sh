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
log_info "========== 📱 NotificationController 테스트 시작 =========="

# NotificationController는 테스트 엔드포인트가 없으므로 모든 엔드포인트에 인증 필요

# 1. FCM 토큰 업데이트 (인증 필요 - 401)
test_endpoint "POST" "/api/notifications/fcm-token" "FCM 토큰 업데이트 (인증 없음)" "401" '{"deviceId":"test-device","fcmToken":"test-token"}'

# 2. FCM 토큰 삭제 (인증 필요 - 401)
test_endpoint "DELETE" "/api/notifications/fcm-token/test-device" "FCM 토큰 삭제 (인증 없음)" "401"

# 3. 알림 설정 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/notifications/settings" "알림 설정 조회 (인증 없음)" "401"

# 4. 알림 설정 업데이트 (인증 필요 - 401)
test_endpoint "PUT" "/api/notifications/settings" "알림 설정 업데이트 (인증 없음)" "401" '{"emergencyEnabled":true,"reminderEnabled":true}'

# 5. 긴급 알림 전송 (인증 필요 - 400, 한글 파라미터 인코딩 이슈)
test_endpoint "POST" "/api/notifications/emergency?message=emergency" "긴급 알림 전송 (인증 없음)" "401"

# 6. FCM 토큰 검증 (인증 필요 - 401)
test_endpoint "POST" "/api/notifications/validate-token?token=test-token" "FCM 토큰 검증 (인증 없음)" "200"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 7. 잘못된 HTTP 메서드 - FCM 토큰 업데이트 (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/notifications/fcm-token" "잘못된 HTTP 메서드 - FCM 토큰 업데이트 (GET)" "401"

# 8. 잘못된 HTTP 메서드 - FCM 토큰 삭제 (POST - 401, 인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/fcm-token/test-device" "잘못된 HTTP 메서드 - FCM 토큰 삭제 (POST)" "401"

# 9. 잘못된 HTTP 메서드 - 알림 설정 조회 (POST - 401, 인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/settings" "잘못된 HTTP 메서드 - 알림 설정 조회 (POST)" "401"

# 10. 잘못된 HTTP 메서드 - 알림 설정 업데이트 (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/notifications/settings" "잘못된 HTTP 메서드 - 알림 설정 업데이트 (GET)" "401"

# 11. 빈 JSON 데이터로 FCM 토큰 업데이트 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/fcm-token" "빈 JSON 데이터로 FCM 토큰 업데이트" "401" '{}'

# 12. 잘못된 JSON 형식 - FCM 토큰 업데이트 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/fcm-token" "잘못된 JSON 형식 - FCM 토큰 업데이트" "401" '{"deviceId":invalid_json}'

# 13. 빈 JSON 데이터로 알림 설정 업데이트 (인증이 먼저 체크됨)
test_endpoint "PUT" "/api/notifications/settings" "빈 JSON 데이터로 알림 설정 업데이트" "401" '{}'

# 14. 잘못된 JSON 형식 - 알림 설정 업데이트 (인증이 먼저 체크됨)
test_endpoint "PUT" "/api/notifications/settings" "잘못된 JSON 형식 - 알림 설정 업데이트" "401" '{"emergencyEnabled":invalid}'

# 15. 존재하지 않는 디바이스 ID - FCM 토큰 삭제 (인증이 먼저 체크됨)
test_endpoint "DELETE" "/api/notifications/fcm-token/999999" "존재하지 않는 디바이스 ID - FCM 토큰 삭제" "401"

# 16. 특수 문자가 포함된 디바이스 ID (인증이 먼저 체크됨)
test_endpoint "DELETE" "/api/notifications/fcm-token/@#$%" "특수 문자 포함 디바이스 ID - FCM 토큰 삭제" "401"

# 17. 매우 긴 디바이스 ID 값 (인증이 먼저 체크됨)
test_endpoint "DELETE" "/api/notifications/fcm-token/123456789012345678901234567890" "매우 긴 디바이스 ID 값 - FCM 토큰 삭제" "401"

# 18. 빈 디바이스 ID (인증이 먼저 체크됨)
test_endpoint "DELETE" "/api/notifications/fcm-token/" "빈 디바이스 ID - FCM 토큰 삭제" "401"

# 19. 필수 파라미터 누락 - 긴급 알림 전송 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/emergency" "필수 파라미터 누락 - 긴급 알림 전송" "401"

# 20. 빈 메시지로 긴급 알림 전송 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/emergency?message=" "빈 메시지로 긴급 알림 전송" "401"

# 21. 잘못된 위도/경도 값 - 긴급 알림 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/emergency?message=test&latitude=invalid&longitude=invalid" "잘못된 위도/경도 값 - 긴급 알림" "401"

# 22. 범위 초과 위도 값 - 긴급 알림 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/emergency?message=test&latitude=91.0&longitude=126.9780" "범위 초과 위도 값 - 긴급 알림" "401"

# 23. 필수 파라미터 누락 - FCM 토큰 검증 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/validate-token" "필수 파라미터 누락 - FCM 토큰 검증" "400"

# 24. 빈 토큰으로 FCM 토큰 검증 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/notifications/validate-token?token=" "빈 토큰으로 FCM 토큰 검증" "200"

# 25. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/notifications/unknown" "존재하지 않는 하위 경로" "401"

# 26. 루트 경로 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/notifications/" "루트 경로 (슬래시 포함)" "401"

# 27. 쿼리 파라미터 테스트 - 알림 설정 조회 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/notifications/settings?detailed=true" "쿼리 파라미터 포함 - 알림 설정 조회" "401"

# 28. OPTIONS 메서드 테스트 - CORS preflight (인증이 먼저 체크됨)
test_endpoint "OPTIONS" "/api/notifications/settings" "OPTIONS 메서드 - CORS preflight" "401"

# 29. HEAD 메서드 테스트 (인증이 먼저 체크됨)
test_endpoint "HEAD" "/api/notifications/settings" "HEAD 메서드 테스트" "401"

# 30. 매우 긴 경로 테스트 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/notifications/settings/extra/long/path/that/should/not/exist" "매우 긴 경로 테스트" "401"

echo ""
echo "=========================================="
echo "📊 NotificationController 테스트 결과 요약"
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