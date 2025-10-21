#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_DIR="verification_results_${TIMESTAMP}"
mkdir -p "$RESULTS_DIR"

echo "=== BIF-AI Backend 전체 엔드포인트 완전 검증 ==="
echo "서버: $BASE_URL"
echo "시작 시간: $(date)"
echo "결과 저장: $RESULTS_DIR"
echo ""

# 통계 변수
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local auth_header=$5
    local content_type=$6
    local data=$7

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo "🔍 Test #$TOTAL_TESTS: $method $endpoint"
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
        echo "PASS,$method,$endpoint,$expected_status,$status_code,$description" >> "$RESULTS_DIR/summary.csv"
    else
        echo "   ❌ FAIL: $status_code (예상: $expected_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "FAIL,$method,$endpoint,$expected_status,$status_code,$description" >> "$RESULTS_DIR/summary.csv"
        echo "실패 상세: $body" >> "$RESULTS_DIR/failures.log"
    fi

    # 상세 로그 저장
    echo "[$TIMESTAMP] $method $endpoint -> $status_code" >> "$RESULTS_DIR/detailed.log"
    echo "Response: $body" >> "$RESULTS_DIR/detailed.log"
    echo "" >> "$RESULTS_DIR/detailed.log"

    echo ""
}

# CSV 헤더 생성
echo "Result,Method,Endpoint,Expected,Actual,Description" > "$RESULTS_DIR/summary.csv"

echo "=== 1. 공개 엔드포인트 (인증 불필요) ==="

# HealthController
test_endpoint "GET" "/api/health" "200" "애플리케이션 헬스 체크"
test_endpoint "GET" "/health" "200" "대체 헬스 체크 경로"

# OAuth2Controller
test_endpoint "GET" "/api/auth/oauth2/login-urls" "200" "OAuth2 로그인 URL 조회"

# AuthController 공개 엔드포인트
test_endpoint "POST" "/auth/register" "400" "회원가입 (빈 데이터)" "" "application/json" "{}"
test_endpoint "POST" "/auth/login" "400" "로그인 (빈 데이터)" "" "application/json" "{}"
test_endpoint "GET" "/auth/health" "200" "인증 헬스 체크"

# TestController
test_endpoint "GET" "/api/test/health" "200" "테스트 헬스 체크"

echo "=== 2. 인증 필요 엔드포인트 (403 예상) ==="

# AccessibilityController
test_endpoint "POST" "/api/accessibility/voice-guidance" "403" "음성 안내 생성"
test_endpoint "POST" "/api/accessibility/aria-label" "403" "ARIA 라벨 생성"
test_endpoint "GET" "/api/accessibility/screen-reader-hint" "403" "스크린 리더 힌트"
test_endpoint "GET" "/api/accessibility/settings" "403" "접근성 설정 조회"
test_endpoint "PUT" "/api/accessibility/settings" "403" "접근성 설정 업데이트"
test_endpoint "POST" "/api/accessibility/settings/apply-profile" "403" "접근성 프로파일 적용"
test_endpoint "GET" "/api/accessibility/color-schemes" "403" "색상 스키마 목록"
test_endpoint "GET" "/api/accessibility/color-schemes/current" "403" "현재 색상 스키마"
test_endpoint "GET" "/api/accessibility/simplified-navigation" "403" "간소화 네비게이션"
test_endpoint "GET" "/api/accessibility/touch-targets" "403" "터치 타겟 정보"
test_endpoint "POST" "/api/accessibility/simplify-text" "403" "텍스트 간소화"
test_endpoint "POST" "/api/accessibility/settings/sync" "403" "설정 동기화"
test_endpoint "GET" "/api/accessibility/statistics" "403" "접근성 통계"

# StatisticsController
test_endpoint "GET" "/api/statistics/geofence" "403" "지오펜스 통계"
test_endpoint "GET" "/api/statistics/daily-activity" "403" "일일 활동 통계"
test_endpoint "GET" "/api/statistics/daily-activity/single" "403" "특정 날짜 활동 통계"
test_endpoint "GET" "/api/statistics/safety" "403" "안전 통계"
test_endpoint "GET" "/api/statistics/summary" "403" "전체 통계 요약"

# ExperimentController
test_endpoint "POST" "/api/experiments" "403" "실험 생성"
test_endpoint "GET" "/api/experiments" "403" "실험 목록 조회"
test_endpoint "GET" "/api/experiments/test-exp" "403" "실험 상세 조회"
test_endpoint "PUT" "/api/experiments/test-exp" "403" "실험 수정"
test_endpoint "POST" "/api/experiments/test-exp/start" "403" "실험 시작"
test_endpoint "POST" "/api/experiments/test-exp/pause" "403" "실험 일시 중지"
test_endpoint "POST" "/api/experiments/test-exp/resume" "403" "실험 재개"
test_endpoint "POST" "/api/experiments/test-exp/complete" "403" "실험 종료"
test_endpoint "POST" "/api/experiments/test-exp/assign" "403" "사용자 실험 할당"
test_endpoint "POST" "/api/experiments/test-exp/convert" "403" "전환 기록"
test_endpoint "GET" "/api/experiments/test-exp/analysis" "403" "실험 분석"
test_endpoint "GET" "/api/experiments/my-experiments" "403" "내 실험 목록"
test_endpoint "GET" "/api/experiments/feature-flags/test-flag" "403" "Feature Flag 조회"
test_endpoint "POST" "/api/experiments/test-exp/opt-out" "403" "실험 제외"
test_endpoint "POST" "/api/experiments/test-exp/groups" "403" "테스트 그룹 설정"
test_endpoint "POST" "/api/experiments/test-exp/variants" "403" "변형 설정"

# UserController
test_endpoint "GET" "/api/users/me" "403" "내 정보 조회"
test_endpoint "PUT" "/api/users/me" "403" "내 정보 수정"
test_endpoint "GET" "/api/users/123" "403" "사용자 정보 조회"
test_endpoint "GET" "/api/users" "403" "사용자 목록 조회"
test_endpoint "PUT" "/api/users/123/deactivate" "403" "사용자 비활성화"
test_endpoint "PUT" "/api/users/123/activate" "403" "사용자 활성화"
test_endpoint "PUT" "/api/users/123/roles" "403" "사용자 권한 수정"

# GuardianController
test_endpoint "GET" "/api/guardians/my" "403" "내 보호자 조회"
test_endpoint "GET" "/api/guardians/protected-users" "403" "보호 대상자 조회"
test_endpoint "POST" "/api/guardians" "403" "보호자 등록"
test_endpoint "PUT" "/api/guardians/123/approve" "403" "보호자 승인"
test_endpoint "PUT" "/api/guardians/123/reject" "403" "보호자 거부"
test_endpoint "PUT" "/api/guardians/123/permissions" "403" "보호자 권한 수정"
test_endpoint "DELETE" "/api/guardians/123" "403" "보호자 삭제"
test_endpoint "DELETE" "/api/guardians/relationships/123" "403" "보호자 관계 삭제"

# GuardianRelationshipController
test_endpoint "POST" "/api/guardian-relationships/invite" "403" "보호자 초대"
test_endpoint "POST" "/api/guardian-relationships/accept-invitation" "403" "초대 수락"
test_endpoint "POST" "/api/guardian-relationships/reject-invitation" "403" "초대 거부"
test_endpoint "PUT" "/api/guardian-relationships/123/permissions" "403" "관계 권한 수정"
test_endpoint "POST" "/api/guardian-relationships/123/suspend" "403" "관계 일시 중단"
test_endpoint "POST" "/api/guardian-relationships/123/reactivate" "403" "관계 재활성화"
test_endpoint "DELETE" "/api/guardian-relationships/123" "403" "관계 삭제"
test_endpoint "GET" "/api/guardian-relationships/user/123" "403" "사용자별 관계 조회"
test_endpoint "GET" "/api/guardian-relationships/guardian/123" "403" "보호자별 관계 조회"
test_endpoint "GET" "/api/guardian-relationships/user/123/emergency-contacts" "403" "응급 연락처 조회"
test_endpoint "GET" "/api/guardian-relationships/check-permission" "403" "권한 확인"
test_endpoint "POST" "/api/guardian-relationships/update-activity" "403" "활동 업데이트"

# GuardianDashboardController
test_endpoint "GET" "/api/guardian/dashboard/daily-summary/123" "403" "일일 요약"
test_endpoint "GET" "/api/guardian/dashboard/weekly-summary/123" "403" "주간 요약"
test_endpoint "GET" "/api/guardian/dashboard/integrated/123" "403" "통합 대시보드"

# EmergencyController
test_endpoint "POST" "/api/emergency/alert" "403" "응급 알림"
test_endpoint "POST" "/api/emergency/fall-detection" "403" "낙상 감지"
test_endpoint "GET" "/api/emergency/status/123" "403" "응급 상황 상태"
test_endpoint "GET" "/api/emergency/history/123" "403" "응급 상황 이력"
test_endpoint "GET" "/api/emergency/active" "403" "활성 응급 상황"
test_endpoint "PUT" "/api/emergency/123/resolve" "403" "응급 상황 해결"

# EmergencyContactController
test_endpoint "POST" "/api/emergency-contacts" "403" "응급 연락처 생성"
test_endpoint "PUT" "/api/emergency-contacts/123" "403" "응급 연락처 수정"
test_endpoint "DELETE" "/api/emergency-contacts/123" "403" "응급 연락처 삭제"
test_endpoint "GET" "/api/emergency-contacts/123" "403" "응급 연락처 조회"
test_endpoint "GET" "/api/emergency-contacts" "403" "응급 연락처 목록"
test_endpoint "GET" "/api/emergency-contacts/active" "403" "활성 응급 연락처"
test_endpoint "GET" "/api/emergency-contacts/available" "403" "사용 가능 연락처"
test_endpoint "GET" "/api/emergency-contacts/medical" "403" "의료진 연락처"
test_endpoint "POST" "/api/emergency-contacts/123/verify" "403" "연락처 검증"
test_endpoint "PATCH" "/api/emergency-contacts/123/toggle-active" "403" "활성 상태 토글"
test_endpoint "PUT" "/api/emergency-contacts/priorities" "403" "우선순위 설정"
test_endpoint "POST" "/api/emergency-contacts/123/contact-record" "403" "연락 기록"

# SosController
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "403" "SOS 발생"
test_endpoint "PUT" "/api/v1/emergency/sos/123/cancel" "403" "SOS 취소"
test_endpoint "GET" "/api/v1/emergency/sos/history" "403" "SOS 이력"
test_endpoint "POST" "/api/v1/emergency/sos/quick" "403" "빠른 SOS"

# VisionController
test_endpoint "POST" "/api/vision/analyze" "403" "이미지 분석"
test_endpoint "POST" "/api/vision/detect-danger" "403" "위험 감지"

# ImageAnalysisController
test_endpoint "POST" "/api/images/analyze" "403" "이미지 분석"
test_endpoint "GET" "/api/images/analysis/123" "403" "분석 결과 조회"
test_endpoint "POST" "/api/images/quick-analyze" "403" "빠른 분석"

# GeofenceController
test_endpoint "POST" "/api/geofences" "403" "지오펜스 생성"
test_endpoint "PUT" "/api/geofences/123" "403" "지오펜스 수정"
test_endpoint "DELETE" "/api/geofences/123" "403" "지오펜스 삭제"
test_endpoint "GET" "/api/geofences/123" "403" "지오펜스 조회"
test_endpoint "GET" "/api/geofences" "403" "지오펜스 목록"
test_endpoint "GET" "/api/geofences/paged" "403" "페이징 지오펜스 목록"
test_endpoint "GET" "/api/geofences/type/safe" "403" "타입별 지오펜스"
test_endpoint "PATCH" "/api/geofences/123/toggle" "403" "지오펜스 토글"
test_endpoint "PUT" "/api/geofences/priorities" "403" "지오펜스 우선순위"
test_endpoint "GET" "/api/geofences/stats" "403" "지오펜스 통계"

# NotificationController
test_endpoint "POST" "/api/notifications/fcm-token" "403" "FCM 토큰 등록"
test_endpoint "DELETE" "/api/notifications/fcm-token/device123" "403" "FCM 토큰 삭제"
test_endpoint "GET" "/api/notifications/settings" "403" "알림 설정 조회"
test_endpoint "PUT" "/api/notifications/settings" "403" "알림 설정 수정"
test_endpoint "POST" "/api/notifications/test" "403" "테스트 알림"
test_endpoint "POST" "/api/notifications/emergency" "403" "응급 알림"
test_endpoint "POST" "/api/notifications/validate-token" "403" "토큰 검증"

# PoseController
test_endpoint "POST" "/api/pose/data" "403" "포즈 데이터 저장"
test_endpoint "POST" "/api/pose/data/batch" "403" "포즈 데이터 일괄 저장"
test_endpoint "GET" "/api/pose/fall-status/123" "403" "낙상 상태 조회"
test_endpoint "POST" "/api/pose/fall-event/123/feedback" "403" "낙상 이벤트 피드백"

# UserBehaviorController
test_endpoint "POST" "/api/behavior/log" "403" "행동 로그"
test_endpoint "POST" "/api/behavior/batch" "403" "행동 일괄 로그"
test_endpoint "POST" "/api/behavior/pageview" "403" "페이지뷰 로그"
test_endpoint "POST" "/api/behavior/click" "403" "클릭 로그"
test_endpoint "POST" "/api/behavior/error" "403" "에러 로그"

# AdminController
test_endpoint "GET" "/api/admin/statistics" "403" "관리자 통계"
test_endpoint "GET" "/api/admin/sessions" "403" "세션 목록"
test_endpoint "DELETE" "/api/admin/sessions/123" "403" "세션 삭제"
test_endpoint "GET" "/api/admin/auth-logs" "403" "인증 로그"
test_endpoint "GET" "/api/admin/settings" "403" "관리자 설정"
test_endpoint "PUT" "/api/admin/settings" "403" "관리자 설정 수정"
test_endpoint "POST" "/api/admin/backup" "403" "백업 생성"
test_endpoint "DELETE" "/api/admin/cache" "403" "캐시 삭제"

echo "=== 3. 잘못된 토큰으로 인증 테스트 ==="

# 유효하지 않은 JWT 토큰으로 테스트
INVALID_TOKEN="Bearer invalid-jwt-token-12345"

test_endpoint "GET" "/api/users/me" "403" "잘못된 토큰으로 내 정보 조회" "$INVALID_TOKEN"
test_endpoint "GET" "/api/guardians/my" "403" "잘못된 토큰으로 보호자 조회" "$INVALID_TOKEN"
test_endpoint "GET" "/api/accessibility/settings" "403" "잘못된 토큰으로 접근성 설정" "$INVALID_TOKEN"

echo "=== 4. 에러 시나리오 테스트 ==="

# 존재하지 않는 엔드포인트
test_endpoint "GET" "/api/nonexistent" "404" "존재하지 않는 API 경로"
test_endpoint "GET" "/api/health/invalid" "404" "잘못된 헬스 체크 경로"
test_endpoint "GET" "/api/auth/invalid" "404" "잘못된 인증 경로"

# 잘못된 HTTP 메서드
test_endpoint "DELETE" "/api/health" "405" "헬스 체크에 DELETE 메서드"
test_endpoint "PUT" "/api/auth/oauth2/login-urls" "405" "OAuth2 URL에 PUT 메서드"
test_endpoint "PATCH" "/api/test/health" "405" "테스트 헬스체크에 PATCH 메서드"

# 필수 파라미터 누락
test_endpoint "GET" "/api/accessibility/screen-reader-hint" "400" "필수 파라미터 누락"
test_endpoint "GET" "/api/accessibility/screen-reader-hint?action=" "400" "빈 파라미터 값"

# Content-Type 헤더 테스트
test_endpoint "POST" "/api/accessibility/voice-guidance" "403" "JSON 엔드포인트에 Content-Type 없음" "" "" "{\"text\":\"test\"}"

# 대소문자 구분 테스트
test_endpoint "GET" "/API/HEALTH" "404" "대문자 경로"
test_endpoint "GET" "/api/HEALTH" "404" "부분 대문자 경로"

# 특수 문자 경로 테스트
test_endpoint "GET" "/api/health/../admin" "404" "경로 트래버설 시도"
test_endpoint "GET" "/api/health%2F" "404" "URL 인코딩된 경로"

# WebSocket 엔드포인트 (HTTP로 접근 시 에러)
test_endpoint "GET" "/ws" "403" "WebSocket 엔드포인트에 HTTP GET"

echo "=== 5. Content-Type 및 데이터 형식 테스트 ==="

# JSON 데이터가 필요한 엔드포인트에 잘못된 데이터
test_endpoint "POST" "/auth/register" "400" "회원가입에 잘못된 JSON" "" "application/json" "invalid-json"
test_endpoint "POST" "/auth/login" "400" "로그인에 XML 데이터" "" "application/xml" "<user><name>test</name></user>"

# 큰 데이터 테스트
LARGE_JSON='{"data":"'$(printf 'a%.0s' {1..10000})'"}'
test_endpoint "POST" "/auth/register" "400" "큰 JSON 데이터" "" "application/json" "$LARGE_JSON"

echo "=== 6. 파라미터 유효성 테스트 ==="

# 잘못된 ID 형식
test_endpoint "GET" "/api/users/abc" "403" "문자열 사용자 ID"
test_endpoint "GET" "/api/geofences/xyz" "403" "문자열 지오펜스 ID"
test_endpoint "GET" "/api/emergency/status/invalid" "403" "잘못된 응급상황 ID"

# 음수 ID
test_endpoint "GET" "/api/users/-1" "403" "음수 사용자 ID"
test_endpoint "GET" "/api/geofences/-123" "403" "음수 지오펜스 ID"

# 매우 큰 ID
test_endpoint "GET" "/api/users/999999999999" "403" "매우 큰 사용자 ID"

echo "=== 테스트 완료 ==="
echo "종료 시간: $(date)"
echo ""
echo "=== 결과 요약 ==="
echo "총 테스트: $TOTAL_TESTS"
echo "성공: $PASSED_TESTS"
echo "실패: $FAILED_TESTS"
echo "성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
echo ""
echo "상세 결과는 $RESULTS_DIR 폴더를 확인하세요:"
echo "- summary.csv: 전체 결과 요약"
echo "- detailed.log: 상세 응답 로그"
echo "- failures.log: 실패한 테스트 상세"
echo ""

# 실패한 테스트만 표시
if [ $FAILED_TESTS -gt 0 ]; then
    echo "=== 실패한 테스트 목록 ==="
    grep "FAIL" "$RESULTS_DIR/summary.csv" | while IFS=',' read -r result method endpoint expected actual description; do
        echo "❌ $method $endpoint (예상: $expected, 실제: $actual) - $description"
    done
    echo ""
fi

# 예상 결과 설명
echo "=== 예상 결과 해석 ==="
echo "- 200: 성공적인 응답 (공개 엔드포인트)"
echo "- 400: 잘못된 요청 (필수 파라미터 누락, 잘못된 데이터 형식)"
echo "- 403: 인증 실패 (JWT 토큰 없음 또는 잘못됨)"
echo "- 404: 존재하지 않는 경로"
echo "- 405: 지원하지 않는 HTTP 메서드"
echo "- 415: 지원하지 않는 미디어 타입"