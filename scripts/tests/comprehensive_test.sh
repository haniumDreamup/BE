#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
echo "=== BIF-AI Backend 전체 엔드포인트 종합 테스트 ==="
echo "서버: $BASE_URL"
echo "시작 시간: $(date)"
echo ""

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    
    echo "🔍 $method $endpoint"
    echo "   설명: $description"
    
    response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" 2>/dev/null)
    status_code=$(echo "$response" | tail -n1)
    
    if [ "$status_code" = "$expected_status" ]; then
        echo "   ✅ PASS: $status_code"
    else
        echo "   ❌ FAIL: $status_code (예상: $expected_status)"
    fi
    echo ""
}

echo "=== 1. HealthController (공개) ==="
test_endpoint "GET" "/api/health" "200" "애플리케이션 헬스 체크"

echo "=== 2. OAuth2Controller (공개) ==="  
test_endpoint "GET" "/api/auth/oauth2/login-urls" "200" "OAuth2 로그인 URL 조회"

echo "=== 3. WebSocketController (인증 필요) ==="
# WebSocket은 HTTP로 직접 테스트하기 어려우므로 스킵

echo "=== 4. AccessibilityController (인증 필요) ==="
test_endpoint "POST" "/api/accessibility/voice-guidance" "403" "음성 안내 텍스트 생성"
test_endpoint "POST" "/api/accessibility/aria-label" "403" "ARIA 라벨 생성"
test_endpoint "GET" "/api/accessibility/screen-reader-hint?action=click&target=button" "403" "스크린 리더 힌트"
test_endpoint "GET" "/api/accessibility/settings" "403" "접근성 설정 조회"
test_endpoint "PUT" "/api/accessibility/settings" "403" "접근성 설정 업데이트"
test_endpoint "POST" "/api/accessibility/settings/apply-profile?profileType=basic" "403" "접근성 프로파일 적용"
test_endpoint "GET" "/api/accessibility/color-schemes" "403" "색상 스키마 목록"
test_endpoint "GET" "/api/accessibility/color-schemes/current" "403" "현재 색상 스키마"
test_endpoint "GET" "/api/accessibility/simplified-navigation" "403" "간소화 네비게이션"
test_endpoint "GET" "/api/accessibility/touch-targets" "403" "터치 타겟 정보"
test_endpoint "POST" "/api/accessibility/simplify-text" "403" "텍스트 간소화"
test_endpoint "POST" "/api/accessibility/settings/sync" "403" "설정 동기화"
test_endpoint "GET" "/api/accessibility/statistics" "403" "접근성 통계"

echo "=== 5. StatisticsController (인증 필요) ==="
test_endpoint "GET" "/api/statistics/geofence" "403" "지오펜스 통계"
test_endpoint "GET" "/api/statistics/daily-activity" "403" "일일 활동 통계 목록"
test_endpoint "GET" "/api/statistics/daily-activity/single" "403" "특정 날짜 활동 통계"
test_endpoint "GET" "/api/statistics/safety" "403" "안전 통계"
test_endpoint "GET" "/api/statistics/summary" "403" "전체 통계 요약"

echo "=== 6. ExperimentController (인증 필요) ==="
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

echo "=== 테스트 완료 ==="
echo "종료 시간: $(date)"
echo ""
echo "요약:"
echo "- 공개 엔드포인트: 200 OK 예상"
echo "- 보호된 엔드포인트: 403 Forbidden 예상"
