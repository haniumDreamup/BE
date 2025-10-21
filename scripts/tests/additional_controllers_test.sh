#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
LOG_FILE="/tmp/additional_backend_test.log"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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
echo "║        추가 컨트롤러 테스트 (프론트엔드에서 사용하는 6개)             ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# 1. EmergencyContactController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   1️⃣  EmergencyContactController (/api/emergency-contacts)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "EmergencyContactController" "긴급 연락처 생성 - 인증 없음" "POST" "/api/emergency-contacts" '{"name":"응급실","phoneNumber":"119"}' "401"
test_endpoint "EmergencyContactController" "긴급 연락처 목록 - 인증 없음" "GET" "/api/emergency-contacts" "" "401"
test_endpoint "EmergencyContactController" "활성 연락처 조회 - 인증 없음" "GET" "/api/emergency-contacts/active" "" "401"
test_endpoint "EmergencyContactController" "의료진 연락처 조회 - 인증 없음" "GET" "/api/emergency-contacts/medical" "" "401"

# 2. GuardianRelationshipController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   2️⃣  GuardianRelationshipController (/api/guardian-relationships)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "GuardianRelationshipController" "보호자 초대 - 인증 없음" "POST" "/api/guardian-relationships/invite" '{"guardianEmail":"test@test.com"}' "401"
test_endpoint "GuardianRelationshipController" "초대 수락 - 공개 엔드포인트" "POST" "/api/guardian-relationships/accept-invitation" '{"token":"test"}' "400"
test_endpoint "GuardianRelationshipController" "초대 거부 - 공개 엔드포인트" "POST" "/api/guardian-relationships/reject-invitation" '{"token":"test"}' "400"
test_endpoint "GuardianRelationshipController" "권한 확인 - 인증 없음" "GET" "/api/guardian-relationships/check-permission" "" "401"

# 3. GuardianDashboardController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   3️⃣  GuardianDashboardController (/api/guardian/dashboard)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "GuardianDashboardController" "일일 요약 - 인증 없음" "GET" "/api/guardian/dashboard/daily-summary/1" "" "401"
test_endpoint "GuardianDashboardController" "주간 요약 - 인증 없음" "GET" "/api/guardian/dashboard/weekly-summary/1" "" "401"
test_endpoint "GuardianDashboardController" "통합 대시보드 - 인증 없음" "GET" "/api/guardian/dashboard/integrated/1" "" "401"

# 4. UserBehaviorController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   4️⃣  UserBehaviorController (/api/behavior)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "UserBehaviorController" "행동 로그 - 인증 없음" "POST" "/api/behavior/log" '{"action":"click"}' "401"
test_endpoint "UserBehaviorController" "배치 로그 - 인증 없음" "POST" "/api/behavior/batch" '[]' "401"
test_endpoint "UserBehaviorController" "페이지뷰 로그 - 인증 없음" "POST" "/api/behavior/pageview" '{"page":"home"}' "401"
test_endpoint "UserBehaviorController" "클릭 로그 - 인증 없음" "POST" "/api/behavior/click" '{"element":"button"}' "401"

# 5. AdminController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   5️⃣  AdminController (/api/admin)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "AdminController" "시스템 통계 - 인증 없음" "GET" "/api/admin/statistics" "" "401"
test_endpoint "AdminController" "활성 세션 - 인증 없음" "GET" "/api/admin/sessions" "" "401"
test_endpoint "AdminController" "시스템 설정 - 인증 없음" "GET" "/api/admin/settings" "" "401"
test_endpoint "AdminController" "캐시 초기화 - 인증 없음" "DELETE" "/api/admin/cache" "" "401"

# 6. ImageAnalysisController 테스트
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   6️⃣  ImageAnalysisController (/api/images)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

test_endpoint "ImageAnalysisController" "이미지 분석 - 인증 없음" "POST" "/api/images/analyze" '{}' "401"
test_endpoint "ImageAnalysisController" "빠른 분석 - 인증 없음" "POST" "/api/images/quick-analyze" '{}' "401"
test_endpoint "ImageAnalysisController" "위험 감지 - 인증 없음" "POST" "/api/images/detect-danger" '{}' "401"

# 최종 결과
echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                      추가 컨트롤러 테스트 결과                       ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${BLUE}총 테스트:${NC} $TOTAL"
echo -e "${GREEN}성공:${NC} $PASSED"
echo -e "${RED}실패:${NC} $FAILED"
echo -e "${YELLOW}성공률:${NC} $((PASSED * 100 / TOTAL))%"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 추가 컨트롤러 테스트 통과!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  $FAILED 개 테스트 실패 - 상세 내역 확인 필요${NC}"
    exit 1
fi