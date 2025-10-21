#!/bin/bash

# 모든 컨트롤러 간단 테스트 스크립트
# 인증 없이 각 엔드포인트의 기본 동작 확인

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" -H "Content-Type: application/json")
    status_code=$(echo "$response" | tail -n1)

    if [ "$status_code" = "$expected_status" ]; then
        printf "${GREEN}✓${NC} %-50s [%s %s] -> %s\n" "$description" "$method" "$endpoint" "$status_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        printf "${RED}✗${NC} %-50s [%s %s] -> 예상:%s 실제:%s\n" "$description" "$method" "$endpoint" "$expected_status" "$status_code"
    fi
}

echo "=============================================="
echo "모든 컨트롤러 간단 테스트"
echo "=============================================="
echo

# HealthController (100% 성공)
echo "${YELLOW}=== HealthController ===${NC}"
test_endpoint "GET" "/api/health" "200" "헬스체크"
test_endpoint "GET" "/api/health/liveness" "200" "라이브니스 체크"
test_endpoint "GET" "/api/health/readiness" "200" "레디니스 체크"
echo

# TestController (100% 성공)
echo "${YELLOW}=== TestController ===${NC}"
test_endpoint "GET" "/api/test/health" "200" "테스트 헬스체크"
test_endpoint "POST" "/api/test/echo" "200" "에코 테스트"
echo

# UserController (70% 성공)
echo "${YELLOW}=== UserController ===${NC}"
test_endpoint "GET" "/api/v1/users/me" "401" "현재 사용자 조회 - 인증 없음"
test_endpoint "GET" "/api/v1/users/1" "401" "특정 사용자 조회 - 인증 없음"
test_endpoint "GET" "/api/v1/users" "401" "전체 사용자 목록 - 인증 없음"
test_endpoint "PUT" "/api/v1/users/me" "401" "사용자 정보 수정 - 인증 없음"
test_endpoint "GET" "/api/v1/users/invalid" "400" "잘못된 사용자 ID"
echo

# EmergencyController (85% 성공)
echo "${YELLOW}=== EmergencyController ===${NC}"
test_endpoint "POST" "/api/v1/emergency/report" "401" "긴급상황 신고 - 인증 없음"
test_endpoint "GET" "/api/v1/emergency/status/1" "401" "긴급상황 상태 조회 - 인증 없음"
test_endpoint "GET" "/api/v1/emergency/user/1/history" "401" "긴급상황 이력 - 인증 없음"
test_endpoint "GET" "/api/v1/emergency/active" "401" "활성 긴급상황 목록 - 인증 없음"
test_endpoint "PUT" "/api/v1/emergency/1/resolve" "401" "긴급상황 해결 - 인증 없음"
echo

# GuardianController (92% 성공)
echo "${YELLOW}=== GuardianController ===${NC}"
test_endpoint "GET" "/api/guardians/user/1" "401" "사용자의 보호자 목록 - 인증 없음"
test_endpoint "GET" "/api/guardians/ward/1" "401" "보호자의 피보호자 목록 - 인증 없음"
test_endpoint "POST" "/api/guardians" "401" "보호자 추가 - 인증 없음"
test_endpoint "PUT" "/api/guardians/1" "401" "보호자 정보 수정 - 인증 없음"
test_endpoint "DELETE" "/api/guardians/1" "401" "보호자 삭제 - 인증 없음"
echo

# StatisticsController (22% 성공)
echo "${YELLOW}=== StatisticsController ===${NC}"
test_endpoint "GET" "/api/statistics/user/1/dashboard" "401" "대시보드 통계 - 인증 없음"
test_endpoint "GET" "/api/statistics/guardian/1/overview" "401" "보호자 통계 개요 - 인증 없음"
test_endpoint "GET" "/api/statistics/safety-score/1" "401" "안전 점수 - 인증 없음"
test_endpoint "GET" "/api/statistics/location-heatmap/1" "401" "위치 히트맵 - 인증 없음"
test_endpoint "GET" "/api/statistics/emergency-statistics/1" "401" "긴급상황 통계 - 인증 없음"
echo

# AccessibilityController (100% 성공)
echo "${YELLOW}=== AccessibilityController ===${NC}"
test_endpoint "GET" "/api/v1/accessibility/contrast" "200" "대비 설정 조회"
test_endpoint "GET" "/api/v1/accessibility/font-size" "200" "글꼴 크기 조회"
test_endpoint "POST" "/api/v1/accessibility/contrast" "200" "대비 설정 변경"
test_endpoint "POST" "/api/v1/accessibility/font-size" "200" "글꼴 크기 변경"
echo

# SosController (100% 성공)
echo "${YELLOW}=== SosController ===${NC}"
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - 인증 없음"
test_endpoint "POST" "/api/v1/emergency/sos/quick" "400" "빠른 SOS - 파라미터 없음"
test_endpoint "GET" "/api/v1/emergency/sos/history" "401" "SOS 이력 - 인증 없음"
test_endpoint "PUT" "/api/v1/emergency/sos/1/cancel" "401" "SOS 취소 - 인증 없음"
echo

# PoseController (75% 성공)
echo "${YELLOW}=== PoseController ===${NC}"
test_endpoint "POST" "/api/v1/pose/data" "401" "Pose 데이터 전송 - 인증 없음"
test_endpoint "POST" "/api/v1/pose/data/batch" "401" "Pose 일괄 전송 - 인증 없음"
test_endpoint "GET" "/api/v1/pose/fall-status/1" "401" "낙상 상태 조회 - 인증 없음"
test_endpoint "POST" "/api/v1/pose/fall-event/1/feedback" "401" "낙상 피드백 - 인증 없음"
echo

# ImageAnalysisController (50% 성공)
echo "${YELLOW}=== ImageAnalysisController ===${NC}"
test_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 인증 없음"
test_endpoint "POST" "/api/images/quick-analyze" "401" "빠른 분석 - 인증 없음"
test_endpoint "GET" "/api/images/analysis/1" "401" "분석 결과 조회 - 인증 없음"
echo

# GuardianRelationshipController (30% 성공)
echo "${YELLOW}=== GuardianRelationshipController ===${NC}"
test_endpoint "POST" "/api/guardian-relationships/invite" "401" "보호자 초대 - 인증 없음"
test_endpoint "POST" "/api/guardian-relationships/accept-invitation" "400" "초대 수락 - 파라미터 없음"
test_endpoint "POST" "/api/guardian-relationships/reject-invitation" "400" "초대 거부 - 파라미터 없음"
test_endpoint "GET" "/api/guardian-relationships/user/1" "401" "사용자 보호자 목록 - 인증 없음"
test_endpoint "DELETE" "/api/guardian-relationships/1" "401" "관계 종료 - 인증 없음"
echo

# 결과 요약
echo "=============================================="
echo "           테스트 결과 요약"
echo "=============================================="
printf "총 테스트: ${BLUE}$TOTAL_TESTS${NC}\n"
printf "통과: ${GREEN}$PASSED_TESTS${NC}\n"
printf "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}\n"
success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
printf "성공률: ${YELLOW}$success_rate%%${NC}\n"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    printf "\n${GREEN}🎉 모든 테스트 통과!${NC}\n"
    exit 0
else
    printf "\n${YELLOW}⚠️ 일부 테스트가 실패했습니다${NC}\n"
    exit 1
fi