#!/bin/bash

# 모든 130개 엔드포인트를 빠르게 테스트하는 스크립트
BASE_URL="http://localhost:8080/api"

# 토큰 획득
ACCESS_TOKEN=$(curl -X POST $BASE_URL/auth/login -H "Content-Type: application/json" -d '{"usernameOrEmail": "testuser123", "password": "password123"}' -s | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

echo "=== 130개 엔드포인트 빠른 테스트 ==="
echo "토큰 획득: ${ACCESS_TOKEN:0:20}..."

# 모든 엔드포인트 리스트 (130개)
declare -a endpoints=(
    # Auth (5개)
    "POST:auth/register" "POST:auth/login" "POST:auth/refresh" "POST:auth/logout" "GET:auth/health"
    # Accessibility (13개)
    "POST:accessibility/voice-guidance" "POST:accessibility/aria-label" "GET:accessibility/screen-reader-hint" 
    "GET:accessibility/settings" "PUT:accessibility/settings" "POST:accessibility/settings/apply-profile"
    "GET:accessibility/color-schemes" "GET:accessibility/color-schemes/current" "GET:accessibility/simplified-navigation"
    "GET:accessibility/touch-targets" "POST:accessibility/simplify-text" "POST:accessibility/settings/sync" "GET:accessibility/statistics"
    # Admin (8개)  
    "GET:admin/statistics" "GET:admin/sessions" "DELETE:admin/sessions/1" "GET:admin/auth-logs"
    "GET:admin/settings" "PUT:admin/settings" "POST:admin/backup" "DELETE:admin/cache"
    # Emergency (6개)
    "POST:emergency/alert" "POST:emergency/fall-detection" "GET:emergency/status/1" 
    "GET:emergency/history/1" "GET:emergency/active" "PUT:emergency/1/resolve"
    # Emergency Contacts (12개)
    "POST:emergency-contacts" "PUT:emergency-contacts/1" "DELETE:emergency-contacts/1" "GET:emergency-contacts/1"
    "GET:emergency-contacts" "GET:emergency-contacts/active" "GET:emergency-contacts/available" "GET:emergency-contacts/medical"
    "POST:emergency-contacts/1/verify" "PATCH:emergency-contacts/1/toggle-active" "PUT:emergency-contacts/priorities" "POST:emergency-contacts/1/contact-record"
    # Experiments (16개)
    "POST:experiments" "GET:experiments" "GET:experiments/test-key" "PUT:experiments/test-key"
    "POST:experiments/test-key/start" "POST:experiments/test-key/pause" "POST:experiments/test-key/resume" "POST:experiments/test-key/complete"
    "POST:experiments/test-key/assign" "POST:experiments/test-key/convert" "GET:experiments/test-key/analysis" "GET:experiments/my-experiments"
    "GET:experiments/feature-flags/test-flag" "POST:experiments/test-key/opt-out" "POST:experiments/test-key/groups" "POST:experiments/test-key/variants"
    # Geofences (10개)
    "POST:geofences" "PUT:geofences/1" "DELETE:geofences/1" "GET:geofences/1"
    "GET:geofences" "GET:geofences/paged" "GET:geofences/type/HOME" "PATCH:geofences/1/toggle"
    "PUT:geofences/priorities" "GET:geofences/stats"
    # Guardian (8개)
    "GET:guardians/my" "GET:guardians/protected-users" "POST:guardians" "PUT:guardians/1/approve"
    "PUT:guardians/1/reject" "PUT:guardians/1/permissions" "DELETE:guardians/1" "DELETE:guardians/relationships/1"
    # Guardian Dashboard (3개)
    "GET:guardian/dashboard/daily-summary/1" "GET:guardian/dashboard/weekly-summary/1" "GET:guardian/dashboard/integrated/1"
    # Guardian Relationships (12개)
    "POST:guardian-relationships/invite" "POST:guardian-relationships/accept-invitation" "POST:guardian-relationships/reject-invitation"
    "PUT:guardian-relationships/1/permissions" "POST:guardian-relationships/1/suspend" "POST:guardian-relationships/1/reactivate"
    "DELETE:guardian-relationships/1" "GET:guardian-relationships/user/1" "GET:guardian-relationships/guardian/1"
    "GET:guardian-relationships/user/1/emergency-contacts" "GET:guardian-relationships/check-permission" "POST:guardian-relationships/update-activity"
    # Health (3개)
    "GET:health" "GET:health" "GET:api/health"
    # Images (3개)
    "POST:images/analyze" "GET:images/analysis/1" "POST:images/quick-analyze"
    # Notifications (7개)
    "POST:notifications/fcm-token" "DELETE:notifications/fcm-token/device1" "GET:notifications/settings"
    "PUT:notifications/settings" "POST:notifications/test" "POST:notifications/emergency" "POST:notifications/validate-token"
    # OAuth2 (1개)
    "GET:auth/oauth2/login-urls"
    # Pose (4개)
    "POST:pose/data" "POST:pose/data/batch" "GET:pose/fall-status/1" "POST:pose/fall-event/1/feedback"
    # SOS (4개)
    "POST:sos/trigger" "PUT:sos/1/cancel" "GET:sos/history" "POST:sos/quick"
    # Test (1개)
    "GET:test/health"
    # User Behavior (5개)
    "POST:behavior/log" "POST:behavior/batch" "POST:behavior/pageview" "POST:behavior/click" "POST:behavior/error"
    # Users (7개)
    "GET:users/me" "PUT:users/me" "GET:users/1" "GET:users" "PUT:users/1/deactivate" "PUT:users/1/activate" "PUT:users/1/roles"
    # Vision (2개)
    "POST:vision/analyze" "POST:vision/detect-danger"
)

# 결과 카운터
total=0
success=0
auth_error=0  
server_error=0
not_found=0

echo -e "\n테스트 중..."

for endpoint in "${endpoints[@]}"; do
    IFS=':' read -r method path <<< "$endpoint"
    total=$((total + 1))
    
    # 요청 실행
    if [[ "$method" == "GET" ]]; then
        response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE_URL/$path")
    else
        response=$(curl -s -w "%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -X "$method" "$BASE_URL/$path" -d '{}')
    fi
    
    # HTTP 상태 코드 추출 (마지막 3자리)
    http_code=$(echo "$response" | tail -c 4)
    
    # 상태별 분류
    case "$http_code" in
        200|201|202) success=$((success + 1)); echo -n "✅" ;;
        400|401|403) auth_error=$((auth_error + 1)); echo -n "🔐" ;;
        404) not_found=$((not_found + 1)); echo -n "❌" ;;
        500|502|503) server_error=$((server_error + 1)); echo -n "⚠️" ;;
        *) echo -n "❓" ;;
    esac
    
    # 진행률 표시
    if [ $((total % 10)) -eq 0 ]; then
        echo " ($total/130)"
    fi
done

echo -e "\n\n=== 테스트 결과 요약 ==="
echo "총 테스트: $total개"
echo "✅ 성공: $success개 ($(( success * 100 / total ))%)"
echo "🔐 인증 필요: $auth_error개 ($(( auth_error * 100 / total ))%)"  
echo "⚠️ 서버 오류: $server_error개 ($(( server_error * 100 / total ))%)"
echo "❌ 찾을 수 없음: $not_found개 ($(( not_found * 100 / total ))%)"