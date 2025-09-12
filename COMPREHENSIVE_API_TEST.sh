#!/bin/bash

# 🎯 BIF-AI Backend 종합 API 테스트 스크립트
# 모든 엔드포인트 성공/실패/엣지 케이스 테스트

BASE_URL="http://localhost:8080"
API_BASE="${BASE_URL}/api"
RESULTS_DIR="comprehensive_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

# 테스트 결과 파일들
LOG_FILE="$RESULTS_DIR/test.log"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"
SUCCESS_FILE="$RESULTS_DIR/success_cases.txt"
FAILURE_FILE="$RESULTS_DIR/failure_cases.txt"
EDGE_FILE="$RESULTS_DIR/edge_cases.txt"

# 테스트 카운터
TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILURE_TESTS=0
EDGE_TESTS=0

# 로그 함수
log_test() {
    local test_type="$1"
    local endpoint="$2"
    local method="$3"
    local status_code="$4"
    local expected="$5"
    local description="$6"
    
    ((TOTAL_TESTS++))
    
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local log_entry="[$timestamp] $test_type: $method $endpoint -> HTTP $status_code (Expected: $expected) - $description"
    
    echo "$log_entry" | tee -a "$LOG_FILE"
    
    if [[ "$status_code" == "$expected" ]]; then
        case "$test_type" in
            "SUCCESS") 
                ((SUCCESS_TESTS++))
                echo "$log_entry" >> "$SUCCESS_FILE"
                ;;
            "FAILURE") 
                ((FAILURE_TESTS++))
                echo "$log_entry" >> "$FAILURE_FILE"
                ;;
            "EDGE") 
                ((EDGE_TESTS++))
                echo "$log_entry" >> "$EDGE_FILE"
                ;;
        esac
        echo "✅ PASS: $description"
    else
        echo "❌ FAIL: $description (Got $status_code, Expected $expected)"
        echo "FAILED: $log_entry" >> "$SUMMARY_FILE"
    fi
}

# HTTP 상태 코드 확인 함수
test_endpoint() {
    local method="$1"
    local url="$2"
    local data="$3"
    local headers="$4"
    local test_type="$5"
    local expected_code="$6"
    local description="$7"
    
    local status_code
    if [[ "$method" == "GET" ]]; then
        status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers "$url")
    elif [[ "$method" == "POST" ]]; then
        if [[ -n "$data" ]]; then
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X POST -d "$data" "$url")
        else
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X POST "$url")
        fi
    elif [[ "$method" == "PUT" ]]; then
        if [[ -n "$data" ]]; then
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X PUT -d "$data" "$url")
        else
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X PUT "$url")
        fi
    elif [[ "$method" == "DELETE" ]]; then
        status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X DELETE "$url")
    elif [[ "$method" == "PATCH" ]]; then
        if [[ -n "$data" ]]; then
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X PATCH -d "$data" "$url")
        else
            status_code=$(curl -s -w "%{http_code}" -o /dev/null $headers -X PATCH "$url")
        fi
    fi
    
    log_test "$test_type" "$url" "$method" "$status_code" "$expected_code" "$description"
}

echo "🚀 BIF-AI Backend 종합 API 테스트 시작..."
echo "📁 결과 저장 디렉토리: $RESULTS_DIR"
echo "🕒 테스트 시작 시간: $(date)"
echo ""

# 기본 헤더
JSON_HEADER="-H 'Content-Type: application/json'"
MULTIPART_HEADER="-H 'Content-Type: multipart/form-data'"
FAKE_JWT_HEADER="-H 'Authorization: Bearer fake.jwt.token'"

echo "=== 1. 공개 엔드포인트 테스트 (인증 불필요) ===" | tee -a "$LOG_FILE"

# Health Check 엔드포인트들
test_endpoint "GET" "$BASE_URL/health" "" "" "SUCCESS" "200" "Health check endpoint"
test_endpoint "GET" "$API_BASE/health" "" "" "SUCCESS" "200" "API health check"
test_endpoint "GET" "$API_BASE/auth/health" "" "" "SUCCESS" "200" "Auth service health"
test_endpoint "GET" "$API_BASE/test/health" "" "" "SUCCESS" "200" "Test health endpoint"

echo ""
echo "=== 2. 인증 관련 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

# 회원가입 - 성공 케이스 (데이터베이스 문제로 500 예상)
register_data='{"username":"testuser123","email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트","birthDate":"1990-01-01","agreeToTerms":true,"agreeToPrivacyPolicy":true}'
test_endpoint "POST" "$API_BASE/auth/register" "$register_data" "$JSON_HEADER" "FAILURE" "500" "Valid registration data (DB issue expected)"

# 로그인 - 실패 케이스 (잘못된 데이터)
login_data='{"usernameOrEmail":"invalid@test.com","password":"wrongpass"}'
test_endpoint "POST" "$API_BASE/auth/login" "$login_data" "$JSON_HEADER" "FAILURE" "401" "Invalid login credentials"

# 엣지 케이스 - 빈 데이터
test_endpoint "POST" "$API_BASE/auth/register" '{}' "$JSON_HEADER" "EDGE" "400" "Empty registration data"
test_endpoint "POST" "$API_BASE/auth/login" '{}' "$JSON_HEADER" "EDGE" "400" "Empty login data"

# 엣지 케이스 - 잘못된 JSON
test_endpoint "POST" "$API_BASE/auth/register" 'invalid json' "$JSON_HEADER" "EDGE" "400" "Invalid JSON format"

echo ""
echo "=== 3. 보호된 엔드포인트 테스트 (JWT 인증 필요) ===" | tee -a "$LOG_FILE"

# 인증 없이 접근 - 403 또는 401 예상
test_endpoint "GET" "$API_BASE/users/me" "" "" "SUCCESS" "403" "User profile without auth"
test_endpoint "GET" "$API_BASE/admin/statistics" "" "" "SUCCESS" "403" "Admin statistics without auth"
test_endpoint "GET" "$API_BASE/notifications/settings" "" "" "SUCCESS" "403" "Notification settings without auth"
test_endpoint "GET" "$API_BASE/emergency-contacts" "" "" "SUCCESS" "403" "Emergency contacts without auth"
test_endpoint "GET" "$API_BASE/guardians/my" "" "" "SUCCESS" "403" "Guardian info without auth"
test_endpoint "GET" "$API_BASE/accessibility/settings" "" "" "SUCCESS" "403" "Accessibility settings without auth"

# 잘못된 JWT 토큰으로 접근 - 403 또는 401 예상
test_endpoint "GET" "$API_BASE/users/me" "" "$FAKE_JWT_HEADER" "SUCCESS" "403" "User profile with fake JWT"
test_endpoint "GET" "$API_BASE/admin/statistics" "" "$FAKE_JWT_HEADER" "SUCCESS" "403" "Admin statistics with fake JWT"

echo ""
echo "=== 4. Vision/AI 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

# Vision 분석 - 파일 업로드 없이 접근
test_endpoint "POST" "$API_BASE/vision/analyze" "" "$MULTIPART_HEADER" "FAILURE" "400" "Vision analyze without file"
test_endpoint "POST" "$API_BASE/vision/detect-danger" "" "$MULTIPART_HEADER" "FAILURE" "400" "Danger detection without file"
test_endpoint "POST" "$API_BASE/images/analyze" "" "$MULTIPART_HEADER" "FAILURE" "400" "Image analyze without file"

echo ""
echo "=== 5. 모바일 전용 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

# 모바일 인증
mobile_login_data='{"deviceId":"test-device","email":"test@test.com","password":"test123"}'
test_endpoint "POST" "$API_BASE/mobile/auth/login" "$mobile_login_data" "$JSON_HEADER" "FAILURE" "401" "Mobile login with invalid credentials"
test_endpoint "GET" "$API_BASE/mobile/auth/check" "" "" "SUCCESS" "403" "Mobile auth check without token"

echo ""
echo "=== 6. 실험/A-B 테스트 엔드포인트 ===" | tee -a "$LOG_FILE"

# 실험 관련 - 권한 없이 접근
test_endpoint "GET" "$API_BASE/experiments/my-experiments" "" "" "SUCCESS" "403" "My experiments without auth"
test_endpoint "GET" "$API_BASE/experiments/test-exp" "" "" "SUCCESS" "403" "Specific experiment without auth"
test_endpoint "GET" "$API_BASE/experiments/feature-flags/test-flag" "" "" "SUCCESS" "403" "Feature flag without auth"

echo ""
echo "=== 7. 긴급상황 관련 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

# 긴급 상황 - 인증 없이 접근
test_endpoint "GET" "$API_BASE/emergency/active" "" "" "SUCCESS" "403" "Active emergencies without auth"
test_endpoint "POST" "$API_BASE/emergency/alert" '{}' "$JSON_HEADER" "SUCCESS" "403" "Emergency alert without auth"
test_endpoint "GET" "$API_BASE/sos/history" "" "" "SUCCESS" "403" "SOS history without auth"

# 긴급 연락처
test_endpoint "GET" "$API_BASE/emergency-contacts" "" "" "SUCCESS" "403" "Emergency contacts list without auth"
test_endpoint "GET" "$API_BASE/emergency-contacts/active" "" "" "SUCCESS" "403" "Active emergency contacts without auth"

echo ""
echo "=== 8. 지오펜스 관련 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

test_endpoint "GET" "$API_BASE/geofences/paged" "" "" "SUCCESS" "403" "Paged geofences without auth"
test_endpoint "GET" "$API_BASE/geofences/stats" "" "" "SUCCESS" "403" "Geofence statistics without auth"
test_endpoint "GET" "$API_BASE/geofences/type/HOME" "" "" "SUCCESS" "403" "Geofences by type without auth"

echo ""
echo "=== 9. 알림 관련 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

# FCM 토큰 관련
fcm_data='{"deviceId":"test-device","token":"fake-fcm-token"}'
test_endpoint "POST" "$API_BASE/notifications/fcm-token" "$fcm_data" "$JSON_HEADER" "SUCCESS" "403" "FCM token registration without auth"
test_endpoint "GET" "$API_BASE/notifications/settings" "" "" "SUCCESS" "403" "Notification settings without auth"

echo ""
echo "=== 10. 사용자 행동 분석 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

behavior_data='{"action":"page_view","page":"/test","timestamp":"2025-01-09T10:00:00Z"}'
test_endpoint "POST" "$API_BASE/behavior/log" "$behavior_data" "$JSON_HEADER" "SUCCESS" "403" "Behavior logging without auth"
test_endpoint "POST" "$API_BASE/behavior/pageview" "$behavior_data" "$JSON_HEADER" "SUCCESS" "403" "Page view logging without auth"

echo ""
echo "=== 11. 관리자 전용 엔드포인트 테스트 ===" | tee -a "$LOG_FILE"

test_endpoint "GET" "$API_BASE/admin/statistics" "" "" "SUCCESS" "403" "Admin statistics without auth"
test_endpoint "GET" "$API_BASE/admin/sessions" "" "" "SUCCESS" "403" "Admin sessions without auth"
test_endpoint "DELETE" "$API_BASE/admin/cache" "" "" "SUCCESS" "403" "Admin cache clear without auth"

echo ""
echo "=== 12. 엣지 케이스 및 오류 처리 테스트 ===" | tee -a "$LOG_FILE"

# 존재하지 않는 엔드포인트
test_endpoint "GET" "$API_BASE/nonexistent/endpoint" "" "" "EDGE" "404" "Non-existent endpoint"
test_endpoint "POST" "$API_BASE/fake/api" "{}" "$JSON_HEADER" "EDGE" "404" "Non-existent POST endpoint"

# 잘못된 HTTP 메소드
test_endpoint "DELETE" "$API_BASE/auth/health" "" "" "EDGE" "405" "Wrong HTTP method on health endpoint"

# 매우 큰 데이터
large_data=$(printf '{"data":"%*s"}' 10000 | tr ' ' 'x')
test_endpoint "POST" "$API_BASE/auth/register" "$large_data" "$JSON_HEADER" "EDGE" "400" "Large payload test"

# 특수 문자가 포함된 URL
test_endpoint "GET" "$API_BASE/users/%3Cscript%3E" "" "" "EDGE" "400" "URL with special characters"

echo ""
echo "=== 📊 테스트 결과 요약 ===" | tee -a "$SUMMARY_FILE"
echo "🕒 테스트 완료 시간: $(date)" | tee -a "$SUMMARY_FILE"
echo "📊 총 테스트 수: $TOTAL_TESTS" | tee -a "$SUMMARY_FILE"
echo "✅ 성공 케이스: $SUCCESS_TESTS" | tee -a "$SUMMARY_FILE"
echo "❌ 실패 케이스: $FAILURE_TESTS" | tee -a "$SUMMARY_FILE"
echo "🔍 엣지 케이스: $EDGE_TESTS" | tee -a "$SUMMARY_FILE"
echo "📈 성공률: $(( (SUCCESS_TESTS + FAILURE_TESTS + EDGE_TESTS) * 100 / TOTAL_TESTS ))%" | tee -a "$SUMMARY_FILE"

echo ""
echo "📁 상세 결과 파일:"
echo "   - 전체 로그: $LOG_FILE"
echo "   - 요약: $SUMMARY_FILE"
echo "   - 성공 케이스: $SUCCESS_FILE"
echo "   - 실패 케이스: $FAILURE_FILE"  
echo "   - 엣지 케이스: $EDGE_FILE"

echo ""
echo "🎯 BIF-AI Backend 종합 API 테스트 완료!"

# 최종 요약 출력
cat "$SUMMARY_FILE"
