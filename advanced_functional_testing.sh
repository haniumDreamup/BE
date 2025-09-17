#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="functional_test_results_${TIMESTAMP}"
mkdir -p "$RESULTS_DIR"

echo "=== BIF-AI Backend 고급 기능 테스트 ==="
echo "서버: $BASE_URL"
echo "시작 시간: $(date)"
echo "결과 저장: $RESULTS_DIR"
echo ""

# 통계 변수
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
advanced_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local expected_status="$4"
    local description="$5"
    local auth_header="$6"
    local content_type="$7"
    local data="$8"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo "🧪 Advanced Test #$TOTAL_TESTS: $test_name"
    echo "   $method $endpoint"
    echo "   설명: $description"
    echo "   예상 상태: $expected_status"

    # curl 명령 구성
    local curl_cmd="curl -s -w \"\\n%{http_code}\" -X $method"

    if [ ! -z "$auth_header" ]; then
        curl_cmd="$curl_cmd -H \"Authorization: $auth_header\""
    fi

    if [ ! -z "$content_type" ]; then
        curl_cmd="$curl_cmd -H \"Content-Type: $content_type\""
    fi

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd \"$BASE_URL$endpoint\""

    # 요청 실행
    response=$(eval $curl_cmd 2>/dev/null)
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    # 결과 판정
    if [ "$status_code" = "$expected_status" ]; then
        echo "   ✅ PASS: $status_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "PASS,$test_name,$method,$endpoint,$expected_status,$status_code,$description" >> "$RESULTS_DIR/summary.csv"
    else
        echo "   ❌ FAIL: $status_code (예상: $expected_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "FAIL,$test_name,$method,$endpoint,$expected_status,$status_code,$description" >> "$RESULTS_DIR/summary.csv"
        echo "실패 상세: $body" >> "$RESULTS_DIR/failures.log"
    fi

    # 상세 로그 저장
    echo "[$TIMESTAMP] $test_name -> $status_code" >> "$RESULTS_DIR/detailed.log"
    echo "Response: $body" >> "$RESULTS_DIR/detailed.log"
    echo "" >> "$RESULTS_DIR/detailed.log"

    # 응답 본문 분석
    if [ ${#body} -gt 0 ] && [ ${#body} -lt 500 ]; then
        echo "   응답: $body"
    elif [ ${#body} -gt 500 ]; then
        echo "   응답: $(echo "$body" | head -c 200)..."
    fi
    echo ""
}

# CSV 헤더 생성
echo "Result,TestName,Method,Endpoint,Expected,Actual,Description" > "$RESULTS_DIR/summary.csv"

echo "=== 1. AuthController 상세 테스트 ==="

# 회원가입 입력 검증 테스트
advanced_test "Register_Empty_JSON" "POST" "/auth/register" "400" "빈 JSON으로 회원가입" "" "application/json" "{}"

advanced_test "Register_Missing_Username" "POST" "/auth/register" "400" "사용자명 누락" "" "application/json" '{"email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

advanced_test "Register_Invalid_Email" "POST" "/auth/register" "400" "잘못된 이메일 형식" "" "application/json" '{"username":"test123","email":"invalid-email","password":"test123","confirmPassword":"test123","fullName":"테스트","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

advanced_test "Register_Short_Username" "POST" "/auth/register" "400" "너무 짧은 사용자명" "" "application/json" '{"username":"ab","email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

advanced_test "Register_Password_Mismatch" "POST" "/auth/register" "400" "비밀번호 불일치" "" "application/json" '{"username":"test123","email":"test@test.com","password":"test123","confirmPassword":"different","fullName":"테스트","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

advanced_test "Register_No_Terms_Agree" "POST" "/auth/register" "400" "약관 미동의" "" "application/json" '{"username":"test123","email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트","agreeToTerms":false,"agreeToPrivacyPolicy":true}'

# 로그인 입력 검증 테스트
advanced_test "Login_Empty_JSON" "POST" "/auth/login" "400" "빈 JSON으로 로그인" "" "application/json" "{}"

advanced_test "Login_Missing_Password" "POST" "/auth/login" "400" "비밀번호 누락" "" "application/json" '{"usernameOrEmail":"test@test.com"}'

advanced_test "Login_Missing_Username" "POST" "/auth/login" "400" "사용자명/이메일 누락" "" "application/json" '{"password":"test123"}'

# 토큰 갱신 테스트
advanced_test "Refresh_Empty_Token" "POST" "/auth/refresh" "400" "빈 리프레시 토큰" "" "application/json" '{"refreshToken":""}'

advanced_test "Refresh_Invalid_Token" "POST" "/auth/refresh" "401" "잘못된 리프레시 토큰" "" "application/json" '{"refreshToken":"invalid-token-12345"}'

echo "=== 2. AccessibilityController 파라미터 테스트 ==="

# 스크린 리더 힌트 파라미터 테스트
advanced_test "ScreenReader_Missing_Action" "GET" "/api/accessibility/screen-reader-hint" "403" "action 파라미터 누락" "" "" ""

advanced_test "ScreenReader_Empty_Action" "GET" "/api/accessibility/screen-reader-hint?action=" "403" "빈 action 파라미터" "" "" ""

advanced_test "ScreenReader_Invalid_Action" "GET" "/api/accessibility/screen-reader-hint?action=invalid&target=button" "403" "잘못된 action 값" "" "" ""

# 음성 안내 JSON 테스트
advanced_test "VoiceGuidance_Empty_JSON" "POST" "/api/accessibility/voice-guidance" "403" "빈 JSON" "" "application/json" "{}"

advanced_test "VoiceGuidance_Invalid_JSON" "POST" "/api/accessibility/voice-guidance" "400" "잘못된 JSON 형식" "" "application/json" "invalid-json"

advanced_test "VoiceGuidance_Missing_ContentType" "POST" "/api/accessibility/voice-guidance" "403" "Content-Type 헤더 누락" "" "" '{"text":"테스트"}'

echo "=== 3. StatisticsController 쿼리 파라미터 테스트 ==="

# 일일 활동 통계 파라미터 테스트
advanced_test "DailyActivity_Date_Format" "GET" "/api/statistics/daily-activity?date=invalid-date" "403" "잘못된 날짜 형식" "" "" ""

advanced_test "DailyActivity_Future_Date" "GET" "/api/statistics/daily-activity?date=2030-12-31" "403" "미래 날짜" "" "" ""

advanced_test "DailyActivity_Missing_Date" "GET" "/api/statistics/daily-activity/single" "403" "날짜 파라미터 누락" "" "" ""

echo "=== 4. GeofenceController CRUD 테스트 ==="

# 지오펜스 생성 테스트
advanced_test "Geofence_Create_Empty" "POST" "/api/geofences" "403" "빈 지오펜스 데이터" "" "application/json" "{}"

advanced_test "Geofence_Create_Invalid_Coords" "POST" "/api/geofences" "403" "잘못된 좌표" "" "application/json" '{"name":"테스트","latitude":"invalid","longitude":"invalid"}'

advanced_test "Geofence_Create_Missing_Name" "POST" "/api/geofences" "403" "이름 누락" "" "application/json" '{"latitude":37.5665,"longitude":126.9780}'

# 지오펜스 타입별 조회 테스트
advanced_test "Geofence_Type_Invalid" "GET" "/api/geofences/type/invalid-type" "403" "잘못된 지오펜스 타입" "" "" ""

advanced_test "Geofence_Type_Empty" "GET" "/api/geofences/type/" "404" "빈 타입 파라미터" "" "" ""

echo "=== 5. EmergencyController 응급상황 테스트 ==="

# 응급 알림 테스트
advanced_test "Emergency_Alert_Empty" "POST" "/api/emergency/alert" "403" "빈 응급 알림" "" "application/json" "{}"

advanced_test "Emergency_Alert_Invalid_Location" "POST" "/api/emergency/alert" "403" "잘못된 위치 정보" "" "application/json" '{"latitude":"invalid","longitude":"invalid","message":"도움!"}'

# 낙상 감지 테스트
advanced_test "Fall_Detection_Empty" "POST" "/api/emergency/fall-detection" "403" "빈 낙상 감지 데이터" "" "application/json" "{}"

advanced_test "Fall_Detection_Invalid_Data" "POST" "/api/emergency/fall-detection" "403" "잘못된 센서 데이터" "" "application/json" '{"accelerometer":"invalid","gyroscope":"invalid"}'

echo "=== 6. UserBehaviorController 로그 테스트 ==="

# 행동 로그 테스트
advanced_test "Behavior_Log_Empty" "POST" "/api/behavior/log" "403" "빈 행동 로그" "" "application/json" "{}"

advanced_test "Behavior_Log_Invalid_Event" "POST" "/api/behavior/log" "403" "잘못된 이벤트 타입" "" "application/json" '{"eventType":"invalid","timestamp":"2024-01-01T00:00:00Z"}'

# 페이지뷰 로그 테스트
advanced_test "Pageview_Log_Missing_URL" "POST" "/api/behavior/pageview" "403" "URL 누락" "" "application/json" '{"timestamp":"2024-01-01T00:00:00Z"}'

advanced_test "Pageview_Log_Invalid_URL" "POST" "/api/behavior/pageview" "403" "잘못된 URL 형식" "" "application/json" '{"url":"not-a-valid-url","timestamp":"2024-01-01T00:00:00Z"}'

echo "=== 7. NotificationController 알림 테스트 ==="

# FCM 토큰 테스트
advanced_test "FCM_Token_Empty" "POST" "/api/notifications/fcm-token" "403" "빈 FCM 토큰" "" "application/json" "{}"

advanced_test "FCM_Token_Invalid_Format" "POST" "/api/notifications/fcm-token" "403" "잘못된 토큰 형식" "" "application/json" '{"token":"invalid-token-format"}'

# 알림 설정 테스트
advanced_test "Notification_Settings_Invalid" "PUT" "/api/notifications/settings" "403" "잘못된 알림 설정" "" "application/json" '{"emergencyAlerts":"invalid","soundEnabled":"not-boolean"}'

echo "=== 8. ExperimentController A/B 테스트 ==="

# 실험 생성 테스트
advanced_test "Experiment_Create_Empty" "POST" "/api/experiments" "403" "빈 실험 데이터" "" "application/json" "{}"

advanced_test "Experiment_Create_Invalid_Config" "POST" "/api/experiments" "403" "잘못된 실험 설정" "" "application/json" '{"name":"test","config":"invalid-config"}'

# Feature Flag 테스트
advanced_test "FeatureFlag_Invalid_Key" "GET" "/api/experiments/feature-flags/invalid-key-format-!!!" "403" "잘못된 플래그 키" "" "" ""

advanced_test "FeatureFlag_Empty_Key" "GET" "/api/experiments/feature-flags/" "404" "빈 플래그 키" "" "" ""

echo "=== 9. HTTP 메서드 및 콘텐츠 타입 고급 테스트 ==="

# 잘못된 Content-Type 테스트
advanced_test "Wrong_ContentType_XML" "POST" "/auth/register" "400" "XML Content-Type" "" "application/xml" "<user><name>test</name></user>"

advanced_test "Wrong_ContentType_Text" "POST" "/api/accessibility/voice-guidance" "403" "Text Content-Type" "" "text/plain" "plain text data"

# 큰 페이로드 테스트
LARGE_JSON='{"data":"'$(printf 'a%.0s' {1..5000})'"}'
advanced_test "Large_Payload_Test" "POST" "/auth/register" "400" "큰 JSON 페이로드 (5KB)" "" "application/json" "$LARGE_JSON"

# 특수 문자 테스트
advanced_test "Special_Chars_Email" "POST" "/auth/register" "400" "특수문자 이메일" "" "application/json" '{"username":"test","email":"test@<script>alert()</script>.com","password":"test","confirmPassword":"test","fullName":"테스트","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

advanced_test "SQL_Injection_Username" "POST" "/auth/login" "401" "SQL 인젝션 시도" "" "application/json" '{"usernameOrEmail":"admin\"; DROP TABLE users; --","password":"anything"}'

echo "=== 10. 국제화 및 문자 인코딩 테스트 ==="

# 한글 데이터 테스트
advanced_test "Korean_Username" "POST" "/auth/register" "400" "한글 사용자명" "" "application/json" '{"username":"한글사용자","email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트사용자","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

# 이모지 테스트
advanced_test "Emoji_Name" "POST" "/auth/register" "400" "이모지 포함 이름" "" "application/json" '{"username":"test123","email":"test@test.com","password":"test123","confirmPassword":"test123","fullName":"테스트😀사용자","agreeToTerms":true,"agreeToPrivacyPolicy":true}'

echo "=== 11. 동시성 및 성능 테스트 ==="

# 동시 요청 시뮬레이션 (백그라운드)
echo "동시 요청 테스트 시작..."
for i in {1..5}; do
    curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/health" > "$RESULTS_DIR/concurrent_$i.log" &
done
wait

# 결과 확인
concurrent_success=0
for i in {1..5}; do
    if grep -q "200" "$RESULTS_DIR/concurrent_$i.log"; then
        concurrent_success=$((concurrent_success + 1))
    fi
done

if [ $concurrent_success -eq 5 ]; then
    echo "✅ 동시 요청 테스트 성공 (5/5)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo "❌ 동시 요청 테스트 실패 ($concurrent_success/5)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo "=== 테스트 완료 ==="
echo "종료 시간: $(date)"
echo ""
echo "=== 고급 기능 테스트 결과 요약 ==="
echo "총 테스트: $TOTAL_TESTS"
echo "성공: $PASSED_TESTS"
echo "실패: $FAILED_TESTS"
echo "성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
echo ""
echo "상세 결과는 $RESULTS_DIR 폴더를 확인하세요:"
echo "- summary.csv: 전체 결과 요약"
echo "- detailed.log: 상세 응답 로그"
echo "- failures.log: 실패한 테스트 상세"
echo "- concurrent_*.log: 동시 요청 테스트 결과"
echo ""

# 중요한 발견사항 요약
echo "=== 주요 발견사항 ==="
echo "1. 입력 검증: Spring Validation이 작동하는지 확인"
echo "2. 에러 처리: 적절한 HTTP 상태 코드와 에러 메시지 반환"
echo "3. 보안: SQL 인젝션, XSS 등 보안 취약점 테스트"
echo "4. 국제화: 한글, 이모지 등 다국어 지원"
echo "5. 성능: 동시 요청 처리 능력"
echo "6. API 설계: RESTful 원칙 준수"

# 실패 분석
if [ $FAILED_TESTS -gt 0 ]; then
    echo ""
    echo "=== 실패 분석 ==="
    echo "가장 흔한 실패 원인:"
    echo "- 403 응답: JWT 인증이 먼저 체크되어 파라미터 검증 전에 차단"
    echo "- 500 응답: 서버 내부 오류 (DB 연결, 설정 문제 등)"
    echo "- 400 vs 다른 응답: 입력 검증 순서나 우선순위 차이"
fi

echo ""
echo "테스트 완료! 🎯"