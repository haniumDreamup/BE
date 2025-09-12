#!/bin/bash

# BIF-AI Backend 포괄적 API 테스트 스크립트
# 모든 엔드포인트의 성공/실패 케이스를 체계적으로 테스트합니다
# 실행: ./API_TEST_COMPREHENSIVE.sh

# set -e 제거: 실패해도 계속 진행

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 기본 설정
BASE_URL="http://localhost:8080/api"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="test_results_$TIMESTAMP"
SUMMARY_FILE="$RESULTS_DIR/test_summary.md"
DETAIL_FILE="$RESULTS_DIR/test_details.json"

# 테스트 통계
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 요약 파일 초기화
cat > "$SUMMARY_FILE" << EOF
# BIF-AI Backend API 테스트 결과 요약
실행 시간: $(date)
Base URL: $BASE_URL

## 테스트 통계
EOF

# JSON 결과 파일 초기화
echo "[" > "$DETAIL_FILE"

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    local expected_status=$5
    local headers=${6:-""}
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${BLUE}[$TOTAL_TESTS]${NC} Testing: $method $endpoint"
    echo "  Description: $description"
    echo "  Expected Status: $expected_status"
    
    # curl 명령 구성
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"
    
    # 헤더 추가
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"
    if [ -n "$headers" ]; then
        curl_cmd="$curl_cmd $headers"
    fi
    
    # 데이터 추가 (POST, PUT, PATCH)
    if [ -n "$data" ] && [ "$method" != "GET" ] && [ "$method" != "DELETE" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi
    
    # URL 추가
    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"
    
    # 명령 실행
    local response=$(eval $curl_cmd 2>/dev/null || echo "CURL_ERROR")
    
    # 응답 파싱
    if [ "$response" == "CURL_ERROR" ]; then
        echo -e "  ${RED}✗ FAIL: Connection error${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
    
    local status_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | sed '$d')
    
    # JSON 결과 저장
    if [ $TOTAL_TESTS -gt 1 ]; then
        echo "," >> "$DETAIL_FILE"
    fi
    
    cat >> "$DETAIL_FILE" << EOF
{
  "test_number": $TOTAL_TESTS,
  "method": "$method",
  "endpoint": "$endpoint",
  "description": "$description",
  "expected_status": $expected_status,
  "actual_status": $status_code,
  "request_data": $(echo "$data" | jq -c . 2>/dev/null || echo "\"$data\""),
  "response_body": $(echo "$response_body" | jq -c . 2>/dev/null || echo "\"$response_body\""),
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF
    
    # 테스트 결과 판정
    if [ "$status_code" == "$expected_status" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} (Status: $status_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # 응답 검증 (성공 응답인 경우)
        if [ "$expected_status" -ge 200 ] && [ "$expected_status" -lt 300 ]; then
            if echo "$response_body" | grep -q '"success":true\|"s":true\|"status":"UP"' 2>/dev/null; then
                echo -e "  ${GREEN}✓ Response validation passed${NC}"
            elif echo "$response_body" | grep -q '"success":false\|"s":false' 2>/dev/null; then
                echo -e "  ${YELLOW}⚠ Warning: Success flag is false${NC}"
            fi
        fi
        
        # 에러 응답 검증 (실패 응답인 경우)
        if [ "$expected_status" -ge 400 ]; then
            if echo "$response_body" | grep -q '"error":\|"message":' 2>/dev/null; then
                echo -e "  ${GREEN}✓ Error message present${NC}"
            else
                echo -e "  ${YELLOW}⚠ Warning: No error message in response${NC}"
            fi
        fi
        
        return 0
    else
        echo -e "  ${RED}✗ FAIL${NC} (Expected: $expected_status, Got: $status_code)"
        echo "  Response: $(echo "$response_body" | jq -c . 2>/dev/null || echo "$response_body" | head -c 200)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# 테스트 섹션 시작 함수
start_section() {
    local section_name=$1
    echo ""
    echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW} $section_name${NC}"
    echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "### $section_name" >> "$SUMMARY_FILE"
    echo "" >> "$SUMMARY_FILE"
}

# 메인 테스트 실행
echo -e "${BLUE}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     BIF-AI Backend API 포괄적 테스트 시작           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

# ============================
# 1. Health & Test Endpoints
# ============================
start_section "1. Health Check & Test 엔드포인트"

test_endpoint "GET" "/health" "Health Check - 정상" "" 200
test_endpoint "GET" "/test/health" "Test Health - 정상" "" 200

# ============================
# 2. Authentication Endpoints
# ============================
start_section "2. 인증 (Authentication) 엔드포인트"

# 2.1 회원가입 테스트
echo -e "${BLUE}2.1 회원가입 (Register)${NC}"

# 실패 케이스: 필수 필드 누락
test_endpoint "POST" "/auth/register" \
    "회원가입 - 필수 필드 누락" \
    '{"username":"test"}' \
    400

# 실패 케이스: 잘못된 이메일 형식
test_endpoint "POST" "/auth/register" \
    "회원가입 - 잘못된 이메일 형식" \
    '{
        "username":"testuser1",
        "email":"invalid-email",
        "password":"Test1234!",
        "confirmPassword":"Test1234!",
        "fullName":"테스트 사용자",
        "agreeToTerms":true,
        "agreeToPrivacyPolicy":true
    }' \
    400

# 실패 케이스: 비밀번호 불일치
test_endpoint "POST" "/auth/register" \
    "회원가입 - 비밀번호 불일치" \
    '{
        "username":"testuser2",
        "email":"test2@example.com",
        "password":"Test1234!",
        "confirmPassword":"Different123!",
        "fullName":"테스트 사용자",
        "agreeToTerms":true,
        "agreeToPrivacyPolicy":true
    }' \
    400

# 실패 케이스: 약관 미동의
test_endpoint "POST" "/auth/register" \
    "회원가입 - 약관 미동의" \
    '{
        "username":"testuser3",
        "email":"test3@example.com",
        "password":"Test1234!",
        "confirmPassword":"Test1234!",
        "fullName":"테스트 사용자",
        "agreeToTerms":false,
        "agreeToPrivacyPolicy":false
    }' \
    400

# 성공 케이스: 정상 회원가입
RANDOM_NUM=$RANDOM
test_endpoint "POST" "/auth/register" \
    "회원가입 - 정상 (신규 사용자)" \
    '{
        "username":"testuser'$RANDOM_NUM'",
        "email":"test'$RANDOM_NUM'@example.com",
        "password":"Test1234!",
        "confirmPassword":"Test1234!",
        "fullName":"테스트 사용자",
        "birthDate":"1990-01-01",
        "agreeToTerms":true,
        "agreeToPrivacyPolicy":true
    }' \
    201

# 실패 케이스: 중복 사용자명
test_endpoint "POST" "/auth/register" \
    "회원가입 - 중복 사용자명" \
    '{
        "username":"testuser'$RANDOM_NUM'",
        "email":"another@example.com",
        "password":"Test1234!",
        "confirmPassword":"Test1234!",
        "fullName":"다른 사용자",
        "agreeToTerms":true,
        "agreeToPrivacyPolicy":true
    }' \
    409

# 2.2 로그인 테스트
echo -e "${BLUE}2.2 로그인 (Login)${NC}"

# 실패 케이스: 존재하지 않는 사용자
test_endpoint "POST" "/auth/login" \
    "로그인 - 존재하지 않는 사용자" \
    '{"username":"nonexistent","password":"Test1234!"}' \
    401

# 실패 케이스: 잘못된 비밀번호
test_endpoint "POST" "/auth/login" \
    "로그인 - 잘못된 비밀번호" \
    '{"username":"testuser'$RANDOM_NUM'","password":"WrongPassword!"}' \
    401

# 성공 케이스: 정상 로그인
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser'$RANDOM_NUM'","password":"Test1234!"}')

if echo "$LOGIN_RESPONSE" | grep -q "accessToken\|token"; then
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -oP '(?<="accessToken"|"token":")[^"]*' | head -1)
    echo -e "${GREEN}✓ 로그인 성공, 토큰 획득${NC}"
else
    TOKEN=""
    echo -e "${YELLOW}⚠ 로그인 실패, 토큰 없음${NC}"
fi

test_endpoint "POST" "/auth/login" \
    "로그인 - 정상" \
    '{"username":"testuser'$RANDOM_NUM'","password":"Test1234!"}' \
    200

# 2.3 토큰 갱신 테스트
echo -e "${BLUE}2.3 토큰 갱신 (Refresh Token)${NC}"

test_endpoint "POST" "/auth/refresh" \
    "토큰 갱신 - 토큰 없음" \
    '{}' \
    401

# ============================
# 3. User Management Endpoints
# ============================
start_section "3. 사용자 관리 (User Management) 엔드포인트"

# 인증 없이 접근
test_endpoint "GET" "/users/profile" \
    "프로필 조회 - 인증 없음" \
    "" \
    401

# 인증 헤더 포함 (토큰이 있는 경우)
if [ -n "$TOKEN" ]; then
    test_endpoint "GET" "/users/profile" \
        "프로필 조회 - 인증 포함" \
        "" \
        200 \
        "-H 'Authorization: Bearer $TOKEN'"
    
    test_endpoint "PUT" "/users/profile" \
        "프로필 수정 - 정상" \
        '{"fullName":"수정된 이름","phoneNumber":"010-1234-5678"}' \
        200 \
        "-H 'Authorization: Bearer $TOKEN'"
fi

# ============================
# 4. Emergency Endpoints
# ============================
start_section "4. 긴급상황 (Emergency) 엔드포인트"

test_endpoint "POST" "/emergency/report" \
    "긴급상황 신고 - 위치 정보 없음" \
    '{"type":"LOST","description":"길을 잃었습니다"}' \
    400

test_endpoint "POST" "/emergency/report" \
    "긴급상황 신고 - 정상" \
    '{
        "type":"LOST",
        "latitude":37.5665,
        "longitude":126.9780,
        "description":"길을 잃었습니다"
    }' \
    200

test_endpoint "GET" "/emergency/history" \
    "긴급상황 이력 조회" \
    "" \
    200

# ============================
# 5. Guardian Endpoints
# ============================
start_section "5. 보호자 관리 (Guardian) 엔드포인트"

test_endpoint "GET" "/guardians" \
    "보호자 목록 조회 - 인증 없음" \
    "" \
    401

if [ -n "$TOKEN" ]; then
    test_endpoint "GET" "/guardians" \
        "보호자 목록 조회 - 인증 포함" \
        "" \
        200 \
        "-H 'Authorization: Bearer $TOKEN'"
    
    test_endpoint "POST" "/guardians/invite" \
        "보호자 초대 - 이메일 누락" \
        '{"name":"보호자"}' \
        400 \
        "-H 'Authorization: Bearer $TOKEN'"
    
    test_endpoint "POST" "/guardians/invite" \
        "보호자 초대 - 정상" \
        '{"email":"guardian@example.com","name":"보호자"}' \
        200 \
        "-H 'Authorization: Bearer $TOKEN'"
fi

# ============================
# 6. Notification Endpoints
# ============================
start_section "6. 알림 (Notification) 엔드포인트"

test_endpoint "GET" "/notifications" \
    "알림 목록 조회" \
    "" \
    200

test_endpoint "GET" "/notifications/unread" \
    "읽지 않은 알림 조회" \
    "" \
    200

test_endpoint "PUT" "/notifications/1/read" \
    "알림 읽음 처리 - 존재하지 않는 알림" \
    "" \
    404

# ============================
# 7. Schedule/Reminder Endpoints
# ============================
start_section "7. 일정/리마인더 (Schedule/Reminder) 엔드포인트"

test_endpoint "GET" "/schedules" \
    "일정 목록 조회" \
    "" \
    200

test_endpoint "POST" "/schedules" \
    "일정 생성 - 필수 필드 누락" \
    '{"title":""}' \
    400

test_endpoint "POST" "/schedules" \
    "일정 생성 - 정상" \
    '{
        "title":"병원 방문",
        "description":"정기 검진",
        "startTime":"2025-09-10T10:00:00",
        "location":"서울대학교병원"
    }' \
    201

test_endpoint "GET" "/schedules/1" \
    "특정 일정 조회 - 존재하지 않는 ID" \
    "" \
    404

test_endpoint "DELETE" "/schedules/99999" \
    "일정 삭제 - 존재하지 않는 ID" \
    "" \
    404

# ============================
# 8. Image Analysis Endpoints
# ============================
start_section "8. 이미지 분석 (Image Analysis) 엔드포인트"

test_endpoint "POST" "/images/analyze" \
    "이미지 분석 - 이미지 없음" \
    '{}' \
    400

test_endpoint "GET" "/images/history" \
    "이미지 분석 이력" \
    "" \
    200

# ============================
# 9. Geofence Endpoints
# ============================
start_section "9. 지오펜스 (Geofence) 엔드포인트"

test_endpoint "GET" "/geofences" \
    "지오펜스 목록 조회" \
    "" \
    200

test_endpoint "POST" "/geofences" \
    "지오펜스 생성 - 잘못된 좌표" \
    '{
        "name":"집",
        "latitude":"invalid",
        "longitude":"invalid",
        "radius":100
    }' \
    400

test_endpoint "POST" "/geofences" \
    "지오펜스 생성 - 정상" \
    '{
        "name":"집",
        "latitude":37.5665,
        "longitude":126.9780,
        "radius":100
    }' \
    201

# ============================
# 10. SOS Endpoints
# ============================
start_section "10. SOS 엔드포인트"

test_endpoint "POST" "/sos/trigger" \
    "SOS 발동" \
    '{"latitude":37.5665,"longitude":126.9780}' \
    200

test_endpoint "GET" "/sos/status" \
    "SOS 상태 확인" \
    "" \
    200

# ============================
# 11. Accessibility Endpoints
# ============================
start_section "11. 접근성 (Accessibility) 엔드포인트"

test_endpoint "GET" "/accessibility/settings" \
    "접근성 설정 조회" \
    "" \
    200

test_endpoint "PUT" "/accessibility/settings" \
    "접근성 설정 변경" \
    '{
        "fontSize":"large",
        "highContrast":true,
        "voiceGuidance":true
    }' \
    200

# ============================
# 12. Admin Endpoints
# ============================
start_section "12. 관리자 (Admin) 엔드포인트"

test_endpoint "GET" "/admin/users" \
    "관리자 - 사용자 목록 (권한 없음)" \
    "" \
    403

test_endpoint "GET" "/admin/stats" \
    "관리자 - 통계 (권한 없음)" \
    "" \
    403

test_endpoint "GET" "/admin/logs" \
    "관리자 - 로그 조회 (권한 없음)" \
    "" \
    403

# ============================
# 13. OAuth2 Endpoints
# ============================
start_section "13. OAuth2 소셜 로그인 엔드포인트"

test_endpoint "GET" "/oauth2/authorization/kakao" \
    "카카오 OAuth2 인증 URL" \
    "" \
    302

test_endpoint "GET" "/oauth2/authorization/naver" \
    "네이버 OAuth2 인증 URL" \
    "" \
    302

test_endpoint "GET" "/oauth2/authorization/google" \
    "구글 OAuth2 인증 URL" \
    "" \
    302

# ============================
# 14. Vision API Endpoints
# ============================
start_section "14. Vision API 엔드포인트"

test_endpoint "POST" "/vision/analyze" \
    "비전 분석 - 이미지 URL 없음" \
    '{}' \
    400

test_endpoint "POST" "/vision/analyze" \
    "비전 분석 - 잘못된 URL" \
    '{"imageUrl":"not-a-url"}' \
    400

# ============================
# 15. User Behavior Endpoints
# ============================
start_section "15. 사용자 행동 분석 엔드포인트"

test_endpoint "GET" "/behavior/analysis" \
    "행동 분석 조회" \
    "" \
    200

test_endpoint "POST" "/behavior/log" \
    "행동 로그 기록" \
    '{
        "action":"BUTTON_CLICK",
        "target":"home_button",
        "timestamp":"2025-09-05T12:00:00"
    }' \
    201

# ============================
# 16. Experiment/AB Testing Endpoints
# ============================
start_section "16. A/B 테스트 엔드포인트"

test_endpoint "GET" "/experiments/active" \
    "활성 실험 조회" \
    "" \
    200

test_endpoint "POST" "/experiments/track" \
    "실험 이벤트 추적" \
    '{
        "experimentId":"exp_001",
        "variant":"A",
        "event":"click"
    }' \
    200

# ============================
# 17. Mobile Specific Endpoints
# ============================
start_section "17. 모바일 전용 엔드포인트"

test_endpoint "POST" "/mobile/auth/login" \
    "모바일 로그인" \
    '{"username":"testuser'$RANDOM_NUM'","password":"Test1234!"}' \
    200

test_endpoint "GET" "/mobile/config" \
    "모바일 설정 조회" \
    "" \
    200

# ============================
# 18. Pose Detection Endpoints
# ============================
start_section "18. 자세 감지 엔드포인트"

test_endpoint "POST" "/pose/analyze" \
    "자세 분석 - 데이터 없음" \
    '{}' \
    400

test_endpoint "GET" "/pose/history" \
    "자세 분석 이력" \
    "" \
    200

# ============================
# 19. Emergency Contact Endpoints
# ============================
start_section "19. 긴급 연락처 엔드포인트"

test_endpoint "GET" "/emergency-contacts" \
    "긴급 연락처 목록" \
    "" \
    200

test_endpoint "POST" "/emergency-contacts" \
    "긴급 연락처 추가 - 전화번호 없음" \
    '{"name":"응급실"}' \
    400

test_endpoint "POST" "/emergency-contacts" \
    "긴급 연락처 추가 - 정상" \
    '{
        "name":"응급실",
        "phoneNumber":"119",
        "relationship":"EMERGENCY"
    }' \
    201

# ============================
# 20. Guardian Dashboard Endpoints
# ============================
start_section "20. 보호자 대시보드 엔드포인트"

test_endpoint "GET" "/guardian-dashboard/overview" \
    "보호자 대시보드 개요" \
    "" \
    200

test_endpoint "GET" "/guardian-dashboard/alerts" \
    "보호자 알림" \
    "" \
    200

test_endpoint "GET" "/guardian-dashboard/location" \
    "피보호자 위치" \
    "" \
    200

# ============================
# 결과 요약
# ============================
echo "]" >> "$DETAIL_FILE"

echo ""
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW} 테스트 완료 - 결과 요약${NC}"
echo -e "${YELLOW}════════════════════════════════════════════════════════${NC}"
echo ""

# 요약 통계 계산
PASS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)

# 콘솔 출력
echo -e "${BLUE}총 테스트:${NC} $TOTAL_TESTS"
echo -e "${GREEN}성공:${NC} $PASSED_TESTS"
echo -e "${RED}실패:${NC} $FAILED_TESTS"
if [ $SKIPPED_TESTS -gt 0 ]; then
    echo -e "${YELLOW}건너뜀:${NC} $SKIPPED_TESTS"
fi
echo -e "${BLUE}성공률:${NC} $PASS_RATE%"
echo ""

# 요약 파일에 통계 추가
cat >> "$SUMMARY_FILE" << EOF

## 최종 통계
- **총 테스트:** $TOTAL_TESTS
- **성공:** $PASSED_TESTS
- **실패:** $FAILED_TESTS
- **성공률:** $PASS_RATE%

## 결과 파일
- 요약: $SUMMARY_FILE
- 상세: $DETAIL_FILE

## 테스트 완료 시간
$(date)
EOF

# 실패한 테스트가 있으면 경고
if [ $FAILED_TESTS -gt 0 ]; then
    echo -e "${RED}⚠️  경고: $FAILED_TESTS 개의 테스트가 실패했습니다!${NC}"
    echo ""
    echo "실패한 테스트 확인:"
    echo "cat $DETAIL_FILE | jq '.[] | select(.expected_status != .actual_status) | {test_number, endpoint, description}'"
else
    echo -e "${GREEN}✅ 모든 테스트가 성공했습니다!${NC}"
fi

echo ""
echo "결과 파일 위치:"
echo "  - 요약: $SUMMARY_FILE"
echo "  - 상세: $DETAIL_FILE"
echo ""
echo "상세 결과를 보려면:"
echo "  cat $SUMMARY_FILE"
echo "  cat $DETAIL_FILE | jq '.'"

exit 0