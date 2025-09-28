#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
LOG_FILE="/tmp/backend_test.log"
FLUTTER_LOG="/tmp/flutter_test.log"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 백엔드 로그 모니터링 시작
echo "🔍 백엔드 로그 모니터링 시작..."
tail -f /tmp/backend_test.log 2>/dev/null &
BACKEND_LOG_PID=$!

# 통계
TOTAL=0
PASSED=0
FAILED=0

test_endpoint() {
    local controller="$1"
    local test_name="$2"
    local method="$3"
    local endpoint="$4"
    local data="$5"
    local expected_status="$6"
    
    TOTAL=$((TOTAL + 1))
    
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}[$TOTAL] 테스트: $test_name${NC}"
    echo -e "${BLUE}   컨트롤러: $controller${NC}"
    echo -e "${BLUE}   요청: $method $endpoint${NC}"
    
    # 요청 전 타임스탬프
    echo "[$(date '+%H:%M:%S')] 요청 시작..." >> $LOG_FILE
    
    # API 호출
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Origin: http://localhost:3004" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$data" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Origin: http://localhost:3004" \
            -H "Accept: application/json" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$ d')
    
    # 응답 로깅
    echo "[$(date '+%H:%M:%S')] 응답: HTTP $http_code" >> $LOG_FILE
    
    # 결과 판정
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "   ${GREEN}✅ 성공 - HTTP $http_code${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "   ${RED}❌ 실패 - 예상: $expected_status, 실제: $http_code${NC}"
        echo -e "   ${RED}응답: $(echo $body | head -c 200)${NC}"
        FAILED=$((FAILED + 1))
    fi
    
    sleep 0.5
}

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║     프론트엔드-백엔드 전체 컨트롤러 통합 테스트                    ║"
echo "║           실시간 로그 모니터링 포함                              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# 1. AuthController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   1️⃣  AuthController (/api/v1/auth)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "AuthController" "OAuth2 로그인 URL 조회" "GET" "/api/v1/auth/oauth2/login-urls" "" "200"
test_endpoint "AuthController" "로그인 - 빈 데이터" "POST" "/api/v1/auth/login" '{}' "400"
test_endpoint "AuthController" "회원가입 - 잘못된 데이터" "POST" "/api/v1/auth/register" '{"username":""}' "400"
test_endpoint "AuthController" "토큰 갱신 - 잘못된 토큰" "POST" "/api/v1/auth/refresh" '{"refreshToken":"invalid"}' "401"

# 2. UserController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   2️⃣  UserController (/api/v1/users)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "UserController" "내 정보 조회 - 인증 없음" "GET" "/api/v1/users/me" "" "401"
test_endpoint "UserController" "사용자 목록 - 인증 없음" "GET" "/api/v1/users" "" "401"
test_endpoint "UserController" "프로필 수정 - 인증 없음" "PUT" "/api/v1/users/profile" '{}' "401"

# 3. EmergencyController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   3️⃣  EmergencyController (/api/v1/emergency)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "EmergencyController" "긴급 신고 - 인증 없음" "POST" "/api/v1/emergency/alert" '{"type":"FALL"}' "401"
test_endpoint "EmergencyController" "낙상 감지 - 인증 없음" "POST" "/api/v1/emergency/fall-detection" '{}' "401"
test_endpoint "EmergencyController" "활성 긴급상황 조회 - 인증 없음" "GET" "/api/v1/emergency/active" "" "401"
test_endpoint "EmergencyController" "SOS 트리거 - 인증 없음" "POST" "/api/v1/emergency/sos/trigger" '{}' "401"

# 4. AccessibilityController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   4️⃣  AccessibilityController (/api/v1/accessibility)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "AccessibilityController" "접근성 설정 조회" "GET" "/api/v1/accessibility/settings" "" "200"
test_endpoint "AccessibilityController" "음성 안내 생성" "POST" "/api/v1/accessibility/voice-guidance" '{"text":"test"}' "400"
test_endpoint "AccessibilityController" "ARIA 라벨 생성" "POST" "/api/v1/accessibility/aria-label" '{"elementType":"button","text":"submit"}' "200"
test_endpoint "AccessibilityController" "색상 구성표 조회" "GET" "/api/v1/accessibility/color-schemes" "" "200"

# 5. PoseController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   5️⃣  PoseController (/api/v1/pose)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "PoseController" "자세 데이터 전송 - 인증 없음" "POST" "/api/v1/pose/data" '{}' "401"
test_endpoint "PoseController" "배치 데이터 전송 - 인증 없음" "POST" "/api/v1/pose/data/batch" '[]' "401"

# 6. GuardianController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   6️⃣  GuardianController (/api/guardians)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "GuardianController" "보호자 목록 - 인증 없음" "GET" "/api/guardians/my" "" "401"
test_endpoint "GuardianController" "보호 대상자 목록 - 인증 없음" "GET" "/api/guardians/protected-users" "" "401"

# 7. NotificationController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   7️⃣  NotificationController (/api/notifications)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "NotificationController" "FCM 토큰 업데이트 - 인증 없음" "POST" "/api/notifications/fcm-token" '{}' "401"
test_endpoint "NotificationController" "알림 설정 조회 - 인증 없음" "GET" "/api/notifications/settings" "" "401"

# 8. StatisticsController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   8️⃣  StatisticsController (/api/statistics)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "StatisticsController" "안전 통계 - 인증 없음" "GET" "/api/statistics/safety" "" "401"
test_endpoint "StatisticsController" "일일 활동 - 인증 없음" "GET" "/api/statistics/daily-activity" "" "401"

# 9. GeofenceController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   9️⃣  GeofenceController (/api/geofences)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "GeofenceController" "지오펜스 목록 - 인증 없음" "GET" "/api/geofences" "" "401"
test_endpoint "GeofenceController" "지오펜스 생성 - 인증 없음" "POST" "/api/geofences" '{}' "401"

# 10. HealthController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   🔟 HealthController (/api/health, /api/v1/health)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "HealthController" "헬스 체크" "GET" "/api/health" "" "200"
test_endpoint "HealthController" "헬스 체크 V1" "GET" "/api/v1/health" "" "200"

# 백엔드 로그 모니터링 중지
kill $BACKEND_LOG_PID 2>/dev/null

# 최종 결과
echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                         최종 테스트 결과                          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${BLUE}총 테스트:${NC} $TOTAL"
echo -e "${GREEN}성공:${NC} $PASSED"
echo -e "${RED}실패:${NC} $FAILED"
echo -e "${YELLOW}성공률:${NC} $((PASSED * 100 / TOTAL))%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 테스트 통과!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  $FAILED 개 테스트 실패 - 상세 내역 확인 필요${NC}"
    exit 1
fi
