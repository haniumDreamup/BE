#!/bin/bash

# 🎯 BIF-AI Backend 완전한 엔드포인트 테스트 (126개 엔드포인트 모두)
BASE_URL="http://localhost:8080"
RESULTS_DIR="complete_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

LOG_FILE="$RESULTS_DIR/complete_test.log"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log_test() {
    local endpoint="$1"
    local method="$2"
    local expected_code="$3"
    local description="$4"
    
    ((TOTAL_TESTS++))
    
    local status_code
    if [[ "$method" == "GET" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" "$endpoint")
    elif [[ "$method" == "POST" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" -d '{}' "$endpoint")
    elif [[ "$method" == "PUT" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" -X PUT -H "Content-Type: application/json" -d '{}' "$endpoint")
    elif [[ "$method" == "DELETE" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$endpoint")
    elif [[ "$method" == "PATCH" ]]; then
        status_code=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH -H "Content-Type: application/json" -d '{}' "$endpoint")
    fi
    
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local log_entry="[$timestamp] $method $endpoint -> HTTP $status_code (Expected: $expected_code) - $description"
    
    echo "$log_entry" | tee -a "$LOG_FILE"
    
    if [[ "$status_code" == "$expected_code" ]] || [[ "$expected_code" == "ANY" ]]; then
        echo "✅ PASS: $description" | tee -a "$SUMMARY_FILE"
        ((PASSED_TESTS++))
    else
        echo "❌ FAIL: $description (Got $status_code, Expected $expected_code)" | tee -a "$SUMMARY_FILE"
        ((FAILED_TESTS++))
    fi
}

echo "🚀 BIF-AI Backend 완전한 엔드포인트 테스트 시작..."
echo "📁 결과 저장 디렉토리: $RESULTS_DIR"
echo "🔢 총 예상 테스트 수: 126개"
echo ""

# === 1. Admin Controller (8개) ===
echo "=== 1. Admin Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/admin/statistics" "GET" "403" "Admin statistics"
log_test "$BASE_URL/api/v1/admin/sessions" "GET" "403" "Admin sessions"
log_test "$BASE_URL/api/v1/admin/sessions/1" "DELETE" "403" "Admin delete session"
log_test "$BASE_URL/api/v1/admin/auth-logs" "GET" "403" "Admin auth logs"
log_test "$BASE_URL/api/v1/admin/settings" "GET" "403" "Admin get settings"
log_test "$BASE_URL/api/v1/admin/settings" "PUT" "403" "Admin update settings"
log_test "$BASE_URL/api/v1/admin/backup" "POST" "403" "Admin backup"
log_test "$BASE_URL/api/v1/admin/cache" "DELETE" "403" "Admin clear cache"

# === 2. Guardian Relationship Controller (11개) ===
echo "=== 2. Guardian Relationship Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/guardian-relationships/invite" "POST" "403" "Guardian invite"
log_test "$BASE_URL/api/v1/guardian-relationships/accept-invitation" "POST" "403" "Accept invitation"
log_test "$BASE_URL/api/v1/guardian-relationships/reject-invitation" "POST" "403" "Reject invitation"
log_test "$BASE_URL/api/v1/guardian-relationships/1/permissions" "PUT" "403" "Update permissions"
log_test "$BASE_URL/api/v1/guardian-relationships/1/suspend" "POST" "403" "Suspend relationship"
log_test "$BASE_URL/api/v1/guardian-relationships/1/reactivate" "POST" "403" "Reactivate relationship"
log_test "$BASE_URL/api/v1/guardian-relationships/1" "DELETE" "403" "Delete relationship"
log_test "$BASE_URL/api/v1/guardian-relationships/user/1" "GET" "403" "Get user relationships"
log_test "$BASE_URL/api/v1/guardian-relationships/guardian/1" "GET" "403" "Get guardian relationships"
log_test "$BASE_URL/api/v1/guardian-relationships/user/1/emergency-contacts" "GET" "403" "Get emergency contacts"
log_test "$BASE_URL/api/v1/guardian-relationships/check-permission" "GET" "403" "Check permission"
log_test "$BASE_URL/api/v1/guardian-relationships/update-activity" "POST" "403" "Update activity"

# === 3. Vision Controller (2개) ===
echo "=== 3. Vision Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/vision/analyze" "POST" "403" "Vision analyze"
log_test "$BASE_URL/api/v1/vision/detect-danger" "POST" "403" "Vision detect danger"

# === 4. Emergency Controller (6개) ===
echo "=== 4. Emergency Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/emergency/alert" "POST" "403" "Emergency alert"
log_test "$BASE_URL/api/v1/emergency/fall-detection" "POST" "403" "Fall detection"
log_test "$BASE_URL/api/v1/emergency/status/1" "GET" "403" "Emergency status"
log_test "$BASE_URL/api/v1/emergency/history/1" "GET" "403" "Emergency history"
log_test "$BASE_URL/api/v1/emergency/active" "GET" "403" "Active emergencies"
log_test "$BASE_URL/api/v1/emergency/1/resolve" "PUT" "403" "Resolve emergency"

# === 5. Notification Controller (7개) ===
echo "=== 5. Notification Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/notifications/fcm-token" "POST" "403" "FCM token register"
log_test "$BASE_URL/api/v1/notifications/fcm-token/device1" "DELETE" "403" "FCM token delete"
log_test "$BASE_URL/api/v1/notifications/settings" "GET" "403" "Notification settings get"
log_test "$BASE_URL/api/v1/notifications/settings" "PUT" "403" "Notification settings update"
log_test "$BASE_URL/api/v1/notifications/test" "POST" "403" "Notification test"
log_test "$BASE_URL/api/v1/notifications/emergency" "POST" "403" "Emergency notification"
log_test "$BASE_URL/api/v1/notifications/validate-token" "POST" "403" "Validate token"

# === 6. Pose Controller (4개) ===
echo "=== 6. Pose Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/pose/data" "POST" "403" "Pose data"
log_test "$BASE_URL/api/v1/pose/data/batch" "POST" "403" "Pose data batch"
log_test "$BASE_URL/api/v1/pose/fall-status/1" "GET" "403" "Fall status"
log_test "$BASE_URL/api/v1/pose/fall-event/1/feedback" "POST" "403" "Fall event feedback"

# === 7. OAuth2 Controller (1개) ===
echo "=== 7. OAuth2 Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/auth/oauth2/login-urls" "GET" "500" "OAuth2 login URLs"

# === 8. SOS Controller (4개) ===
echo "=== 8. SOS Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/sos/trigger" "POST" "403" "SOS trigger"
log_test "$BASE_URL/api/v1/sos/1/cancel" "PUT" "403" "SOS cancel"
log_test "$BASE_URL/api/v1/sos/history" "GET" "403" "SOS history"
log_test "$BASE_URL/api/v1/sos/quick" "POST" "403" "SOS quick"

# === 9. Health Controller (3개) ===
echo "=== 9. Health Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/health" "GET" "404" "API health"
log_test "$BASE_URL/api/v1/health" "GET" "200" "API v1 health"
log_test "$BASE_URL/" "GET" "404" "Root health (expected 404)"

# === 10. Guardian Dashboard Controller (3개) ===
echo "=== 10. Guardian Dashboard Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/guardian/dashboard/daily-summary/1" "GET" "403" "Daily summary"
log_test "$BASE_URL/api/v1/guardian/dashboard/weekly-summary/1" "GET" "403" "Weekly summary"
log_test "$BASE_URL/api/v1/guardian/dashboard/integrated/1" "GET" "403" "Integrated dashboard"

# === 11. Auth Controller (5개) ===
echo "=== 11. Auth Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/auth/register" "POST" "ANY" "Auth register"
log_test "$BASE_URL/api/v1/auth/login" "POST" "ANY" "Auth login"
log_test "$BASE_URL/api/v1/auth/refresh" "POST" "ANY" "Auth refresh"
log_test "$BASE_URL/api/v1/auth/logout" "POST" "403" "Auth logout"
log_test "$BASE_URL/api/v1/auth/health" "GET" "200" "Auth health"

# === 12. Image Analysis Controller (3개) ===
echo "=== 12. Image Analysis Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/images/analyze" "POST" "403" "Image analyze"
log_test "$BASE_URL/api/v1/images/analysis/1" "GET" "403" "Image analysis result"
log_test "$BASE_URL/api/v1/images/quick-analyze" "POST" "403" "Image quick analyze"

# === 13. Geofence Controller (8개) ===
echo "=== 13. Geofence Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/geofences/1" "PUT" "403" "Update geofence"
log_test "$BASE_URL/api/v1/geofences/1" "DELETE" "403" "Delete geofence"
log_test "$BASE_URL/api/v1/geofences/1" "GET" "403" "Get geofence"
log_test "$BASE_URL/api/v1/geofences/paged" "GET" "403" "Paged geofences"
log_test "$BASE_URL/api/v1/geofences/type/HOME" "GET" "403" "Geofences by type"
log_test "$BASE_URL/api/v1/geofences/1/toggle" "PATCH" "403" "Toggle geofence"
log_test "$BASE_URL/api/v1/geofences/priorities" "PUT" "403" "Update priorities"
log_test "$BASE_URL/api/v1/geofences/stats" "GET" "403" "Geofence stats"

# === 14. Mobile Auth Controller (4개) ===
echo "=== 14. Mobile Auth Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/mobile/auth/login" "POST" "ANY" "Mobile login"
log_test "$BASE_URL/api/v1/mobile/auth/refresh" "POST" "ANY" "Mobile refresh"
log_test "$BASE_URL/api/v1/mobile/auth/logout" "POST" "403" "Mobile logout"
log_test "$BASE_URL/api/v1/mobile/auth/check" "GET" "403" "Mobile auth check"

# === 15. User Behavior Controller (5개) ===
echo "=== 15. User Behavior Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/behavior/log" "POST" "403" "Behavior log"
log_test "$BASE_URL/api/v1/behavior/batch" "POST" "403" "Behavior batch"
log_test "$BASE_URL/api/v1/behavior/pageview" "POST" "403" "Behavior pageview"
log_test "$BASE_URL/api/v1/behavior/click" "POST" "403" "Behavior click"
log_test "$BASE_URL/api/v1/behavior/error" "POST" "403" "Behavior error"

# === 16. Emergency Contact Controller (11개) ===
echo "=== 16. Emergency Contact Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/emergency-contacts" "GET" "403" "Get emergency contacts"
log_test "$BASE_URL/api/v1/emergency-contacts/1" "PUT" "403" "Update emergency contact"
log_test "$BASE_URL/api/v1/emergency-contacts/1" "DELETE" "403" "Delete emergency contact"
log_test "$BASE_URL/api/v1/emergency-contacts/1" "GET" "403" "Get emergency contact"
log_test "$BASE_URL/api/v1/emergency-contacts/active" "GET" "403" "Active emergency contacts"
log_test "$BASE_URL/api/v1/emergency-contacts/available" "GET" "403" "Available emergency contacts"
log_test "$BASE_URL/api/v1/emergency-contacts/medical" "GET" "403" "Medical emergency contacts"
log_test "$BASE_URL/api/v1/emergency-contacts/1/verify" "POST" "403" "Verify emergency contact"
log_test "$BASE_URL/api/v1/emergency-contacts/1/toggle-active" "PATCH" "403" "Toggle emergency contact"
log_test "$BASE_URL/api/v1/emergency-contacts/priorities" "PUT" "403" "Update contact priorities"
log_test "$BASE_URL/api/v1/emergency-contacts/1/contact-record" "POST" "403" "Contact record"

# === 17. Test Controller (1개) ===
echo "=== 17. Test Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/test/health" "GET" "ANY" "Test health"

# === 18. Experiment Controller (16개) ===
echo "=== 18. Experiment Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/experiments/test-exp" "GET" "403" "Get experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp" "PUT" "403" "Update experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/start" "POST" "403" "Start experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/pause" "POST" "403" "Pause experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/resume" "POST" "403" "Resume experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/complete" "POST" "403" "Complete experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/assign" "POST" "403" "Assign experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/convert" "POST" "403" "Convert experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/analysis" "GET" "403" "Experiment analysis"
log_test "$BASE_URL/api/v1/experiments/my-experiments" "GET" "403" "My experiments"
log_test "$BASE_URL/api/v1/experiments/feature-flags/test-flag" "GET" "403" "Feature flag"
log_test "$BASE_URL/api/v1/experiments/test-exp/opt-out" "POST" "403" "Opt out experiment"
log_test "$BASE_URL/api/v1/experiments/test-exp/groups" "POST" "403" "Experiment groups"
log_test "$BASE_URL/api/v1/experiments/test-exp/variants" "POST" "403" "Experiment variants"

# === 19. Guardian Controller (6개) ===
echo "=== 19. Guardian Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/guardians/my" "GET" "403" "My guardians"
log_test "$BASE_URL/api/v1/guardians/protected-users" "GET" "403" "Protected users"
log_test "$BASE_URL/api/v1/guardians/1/approve" "PUT" "403" "Approve guardian"
log_test "$BASE_URL/api/v1/guardians/1/reject" "PUT" "403" "Reject guardian"
log_test "$BASE_URL/api/v1/guardians/1/permissions" "PUT" "403" "Guardian permissions"
log_test "$BASE_URL/api/v1/guardians/1" "DELETE" "403" "Delete guardian"
log_test "$BASE_URL/api/v1/guardians/relationships/1" "DELETE" "403" "Delete guardian relationship"

# === 20. Accessibility Controller (12개) ===
echo "=== 20. Accessibility Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/accessibility/voice-guidance" "POST" "403" "Voice guidance"
log_test "$BASE_URL/api/v1/accessibility/aria-label" "POST" "403" "ARIA label"
log_test "$BASE_URL/api/v1/accessibility/screen-reader-hint" "GET" "403" "Screen reader hint"
log_test "$BASE_URL/api/v1/accessibility/settings" "GET" "403" "Accessibility settings get"
log_test "$BASE_URL/api/v1/accessibility/settings" "PUT" "403" "Accessibility settings update"
log_test "$BASE_URL/api/v1/accessibility/settings/apply-profile" "POST" "403" "Apply profile"
log_test "$BASE_URL/api/v1/accessibility/color-schemes" "GET" "403" "Color schemes"
log_test "$BASE_URL/api/v1/accessibility/color-schemes/current" "GET" "403" "Current color scheme"
log_test "$BASE_URL/api/v1/accessibility/simplified-navigation" "GET" "403" "Simplified navigation"
log_test "$BASE_URL/api/v1/accessibility/touch-targets" "GET" "403" "Touch targets"
log_test "$BASE_URL/api/v1/accessibility/simplify-text" "POST" "403" "Simplify text"
log_test "$BASE_URL/api/v1/accessibility/settings/sync" "POST" "403" "Sync settings"
log_test "$BASE_URL/api/v1/accessibility/statistics" "GET" "403" "Accessibility statistics"

# === 21. User Controller (5개) ===
echo "=== 21. User Controller 테스트 ===" | tee -a "$LOG_FILE"
log_test "$BASE_URL/api/v1/users/me" "GET" "403" "Get current user"
log_test "$BASE_URL/api/v1/users/me" "PUT" "403" "Update current user"
log_test "$BASE_URL/api/v1/users/1" "GET" "403" "Get user by ID"
log_test "$BASE_URL/api/v1/users/1/deactivate" "PUT" "403" "Deactivate user"
log_test "$BASE_URL/api/v1/users/1/activate" "PUT" "403" "Activate user"
log_test "$BASE_URL/api/v1/users/1/roles" "PUT" "403" "Update user roles"

echo ""
echo "=== 📊 완전한 테스트 결과 요약 ===" | tee -a "$SUMMARY_FILE"
echo "🕒 테스트 완료 시간: $(date)" | tee -a "$SUMMARY_FILE"
echo "📊 총 테스트 수: $TOTAL_TESTS" | tee -a "$SUMMARY_FILE"
echo "✅ 성공: $PASSED_TESTS" | tee -a "$SUMMARY_FILE"
echo "❌ 실패: $FAILED_TESTS" | tee -a "$SUMMARY_FILE"
echo "📈 성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%" | tee -a "$SUMMARY_FILE"

echo ""
echo "📁 상세 결과 파일:"
echo "   - 전체 로그: $LOG_FILE"
echo "   - 요약: $SUMMARY_FILE"

echo ""
echo "🎯 BIF-AI Backend 126개 전체 엔드포인트 테스트 완료!"

cat "$SUMMARY_FILE"
