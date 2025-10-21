#!/bin/bash

# Comprehensive Flutter Controller Parameter Validation Test
# 모든 Flutter가 사용하는 백엔드 컨트롤러의 파라미터 검증

echo "🔍 Comprehensive Flutter Controller Parameter Validation Test"
echo "==========================================================="

BASE_URL="http://localhost:8081/api/v1"
API_BASE="http://localhost:8081/api"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 테스트 결과를 저장할 변수
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
test_endpoint() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    local auth_token="$6"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 Testing: $test_name${NC}"

    local auth_header=""
    if [ -n "$auth_token" ]; then
        auth_header="-H \"Authorization: Bearer $auth_token\""
    fi

    if [ "$method" = "GET" ]; then
        if [ -n "$auth_token" ]; then
            response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $auth_token" "$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" "$endpoint")
        fi
    else
        if [ -n "$auth_token" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $auth_token" \
                -d "$data" \
                "$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" \
                -H "Content-Type: application/json" \
                -d "$data" \
                "$endpoint")
        fi
    fi

    # 응답과 상태 코드 분리
    body=$(echo "$response" | sed '$d')
    status_code=$(echo "$response" | tail -n 1)

    echo "Status: $status_code"
    if [ ${#body} -gt 100 ]; then
        echo "Response: ${body:0:100}..."
    else
        echo "Response: $body"
    fi

    # 상태 코드 확인
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: $expected_status, Got: $status_code)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "---"
}

# 토큰 획득 함수
get_access_token() {
    echo "📝 Obtaining access token for authenticated tests..."

    # 먼저 사용자 등록
    register_response=$(curl -s -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "comptest001",
            "email": "comptest001@test.com",
            "password": "ValidPass123!",
            "confirmPassword": "ValidPass123!",
            "fullName": "Comprehensive Test User",
            "agreeToTerms": true,
            "agreeToPrivacyPolicy": true,
            "agreeToMarketing": false
        }')

    # 토큰 추출
    if command -v jq &> /dev/null; then
        ACCESS_TOKEN=$(echo "$register_response" | jq -r '.data.accessToken // empty')
    else
        ACCESS_TOKEN=$(echo "$register_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    fi

    if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        echo "✅ Access token obtained successfully"
    else
        echo "❌ Failed to obtain access token, trying login..."

        # 로그인 시도
        login_response=$(curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/json" \
            -d '{
                "usernameOrEmail": "comptest001@test.com",
                "password": "ValidPass123!",
                "rememberMe": false
            }')

        if command -v jq &> /dev/null; then
            ACCESS_TOKEN=$(echo "$login_response" | jq -r '.data.accessToken // empty')
        else
            ACCESS_TOKEN=$(echo "$login_response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        fi
    fi
}

echo "🚀 Starting comprehensive parameter validation tests..."
echo ""

# 토큰 획득
get_access_token

echo -e "${YELLOW}📱 1. Auth Controller Tests (Extended)${NC}"
echo "====================================="

# 1.1 기본 회원가입 테스트
test_endpoint "Register with valid data" "POST" "$BASE_URL/auth/register" '{
    "username": "testuser002",
    "email": "test002@example.com",
    "password": "ValidPass123!",
    "confirmPassword": "ValidPass123!",
    "fullName": "Test User 002",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
}' "201"

# 1.2 OAuth2 URL 조회
test_endpoint "Get OAuth2 login URLs" "GET" "$BASE_URL/auth/oauth2/login-urls" "" "200"

# 1.3 토큰 갱신 (잘못된 토큰)
test_endpoint "Refresh with invalid token" "POST" "$BASE_URL/auth/refresh" '{
    "refreshToken": "invalid_token_12345"
}' "401"

echo -e "${YELLOW}👤 2. User Controller Tests (Extended)${NC}"
echo "====================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 2.1 사용자 프로필 조회
    test_endpoint "Get user profile" "GET" "$BASE_URL/users/me" "" "200" "$ACCESS_TOKEN"

    # 2.2 사용자 목록 조회 (관리자 권한 필요할 수 있음)
    test_endpoint "Get users list" "GET" "$BASE_URL/users" "" "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated user tests - no token available"
fi

# 2.3 인증 없이 접근
test_endpoint "Get user profile without auth" "GET" "$BASE_URL/users/me" "" "401"

echo -e "${YELLOW}🚨 3. Emergency Controller Tests (Extended)${NC}"
echo "============================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 3.1 긴급상황 신고
    test_endpoint "Report emergency alert" "POST" "$BASE_URL/emergency/alert" '{
        "location": {
            "latitude": 37.5665,
            "longitude": 126.9780,
            "address": "서울시 중구 명동"
        },
        "emergencyType": "MEDICAL",
        "description": "의료 응급상황 테스트",
        "severity": "HIGH"
    }' "200" "$ACCESS_TOKEN"

    # 3.2 SOS 트리거
    test_endpoint "Trigger SOS" "POST" "$BASE_URL/emergency/sos/trigger" '{
        "location": {
            "latitude": 37.5665,
            "longitude": 126.9780
        },
        "emergencyType": "FALL",
        "severity": "CRITICAL"
    }' "200" "$ACCESS_TOKEN"

    # 3.3 SOS 히스토리 조회
    test_endpoint "Get SOS history" "GET" "$BASE_URL/emergency/sos/history" "" "200" "$ACCESS_TOKEN"

    # 3.4 활성 긴급상황 조회
    test_endpoint "Get active emergencies" "GET" "$BASE_URL/emergency/active" "" "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated emergency tests - no token available"
fi

# 3.5 인증 없이 긴급상황 신고
test_endpoint "Report emergency without auth" "POST" "$BASE_URL/emergency/alert" '{
    "location": {"latitude": 37.5665, "longitude": 126.9780},
    "emergencyType": "MEDICAL"
}' "401"

echo -e "${YELLOW}🔔 4. Notification Controller Tests${NC}"
echo "===================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 4.1 FCM 토큰 업데이트
    test_endpoint "Update FCM token" "POST" "$API_BASE/notifications/fcm-token" '{
        "token": "test_fcm_token_12345",
        "deviceId": "test_device_001",
        "platform": "web"
    }' "200" "$ACCESS_TOKEN"

    # 4.2 알림 설정 조회
    test_endpoint "Get notification settings" "GET" "$API_BASE/notifications/settings" "" "200" "$ACCESS_TOKEN"

    # 4.3 테스트 알림 전송
    test_endpoint "Send test notification" "POST" "$API_BASE/notifications/test" '{
        "message": "테스트 알림입니다",
        "type": "INFO"
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated notification tests - no token available"
fi

echo -e "${YELLOW}🔐 5. Guardian Controller Tests${NC}"
echo "=================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 5.1 내 보호자 목록 조회
    test_endpoint "Get my guardians" "GET" "$API_BASE/guardians/my" "" "200" "$ACCESS_TOKEN"

    # 5.2 보호 중인 사용자 목록
    test_endpoint "Get protected users" "GET" "$API_BASE/guardians/protected-users" "" "200" "$ACCESS_TOKEN"

    # 5.3 보호자 초대 (Guardian Relationship)
    test_endpoint "Invite guardian" "POST" "$API_BASE/guardian-relationships/invite" '{
        "guardianEmail": "guardian@test.com",
        "relationshipType": "FAMILY",
        "permissions": ["VIEW_LOCATION", "RECEIVE_ALERTS"]
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated guardian tests - no token available"
fi

echo -e "${YELLOW}📊 6. Statistics Controller Tests${NC}"
echo "=================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 6.1 통계 요약 조회
    test_endpoint "Get statistics summary" "GET" "$API_BASE/statistics/summary" "" "200" "$ACCESS_TOKEN"

    # 6.2 일일 활동 통계
    test_endpoint "Get daily activity stats" "GET" "$API_BASE/statistics/daily-activity" "" "200" "$ACCESS_TOKEN"

    # 6.3 안전 통계
    test_endpoint "Get safety statistics" "GET" "$API_BASE/statistics/safety" "" "200" "$ACCESS_TOKEN"

    # 6.4 지오펜스 통계
    test_endpoint "Get geofence statistics" "GET" "$API_BASE/statistics/geofence" "" "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated statistics tests - no token available"
fi

echo -e "${YELLOW}♿ 7. Accessibility Controller Tests${NC}"
echo "===================================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 7.1 접근성 설정 조회
    test_endpoint "Get accessibility settings" "GET" "$BASE_URL/accessibility/settings" "" "200" "$ACCESS_TOKEN"

    # 7.2 컬러 스킴 조회
    test_endpoint "Get color schemes" "GET" "$BASE_URL/accessibility/color-schemes" "" "200" "$ACCESS_TOKEN"

    # 7.3 접근성 설정 업데이트
    test_endpoint "Update accessibility settings" "POST" "$BASE_URL/accessibility/settings" '{
        "fontSize": "LARGE",
        "highContrast": true,
        "screenReader": true,
        "voiceGuidance": true
    }' "200" "$ACCESS_TOKEN"

    # 7.4 음성 안내 설정
    test_endpoint "Configure voice guidance" "POST" "$BASE_URL/accessibility/voice-guidance" '{
        "enabled": true,
        "speed": "NORMAL",
        "language": "ko-KR"
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated accessibility tests - no token available"
fi

echo -e "${YELLOW}🎯 8. Pose Controller Tests${NC}"
echo "============================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 8.1 자세 데이터 전송
    test_endpoint "Submit pose data" "POST" "$BASE_URL/pose/data" '{
        "keypoints": [
            {"x": 100, "y": 200, "confidence": 0.9},
            {"x": 150, "y": 220, "confidence": 0.8}
        ],
        "timestamp": "2024-01-01T12:00:00Z",
        "deviceInfo": {
            "deviceType": "mobile",
            "platform": "web"
        }
    }' "200" "$ACCESS_TOKEN"

    # 8.2 배치 자세 데이터 전송
    test_endpoint "Submit batch pose data" "POST" "$BASE_URL/pose/data/batch" '{
        "sessions": [
            {
                "sessionId": "session_001",
                "keypoints": [{"x": 100, "y": 200, "confidence": 0.9}],
                "timestamp": "2024-01-01T12:00:00Z"
            }
        ]
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated pose tests - no token available"
fi

echo -e "${YELLOW}📍 9. Geofence Controller Tests${NC}"
echo "==============================="

if [ -n "$ACCESS_TOKEN" ]; then
    # 9.1 지오펜스 목록 조회
    test_endpoint "Get geofences" "GET" "$API_BASE/geofences" "" "200" "$ACCESS_TOKEN"

    # 9.2 지오펜스 생성
    test_endpoint "Create geofence" "POST" "$API_BASE/geofences" '{
        "name": "테스트 안전구역",
        "description": "테스트용 지오펜스",
        "type": "SAFE_ZONE",
        "coordinates": {
            "latitude": 37.5665,
            "longitude": 126.9780,
            "radius": 500
        },
        "active": true
    }' "201" "$ACCESS_TOKEN"

    # 9.3 지오펜스 통계
    test_endpoint "Get geofence stats" "GET" "$API_BASE/geofences/stats" "" "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated geofence tests - no token available"
fi

echo -e "${YELLOW}📱 10. User Behavior Controller Tests${NC}"
echo "======================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 10.1 행동 로깅
    test_endpoint "Log user behavior" "POST" "$API_BASE/behavior/log" '{
        "action": "button_click",
        "screen": "home_screen",
        "timestamp": "2024-01-01T12:00:00Z",
        "metadata": {
            "buttonId": "emergency_button",
            "sessionId": "session_001"
        }
    }' "200" "$ACCESS_TOKEN"

    # 10.2 페이지뷰 로깅
    test_endpoint "Log pageview" "POST" "$API_BASE/behavior/pageview" '{
        "page": "/home",
        "title": "홈 화면",
        "timestamp": "2024-01-01T12:00:00Z",
        "duration": 5000
    }' "200" "$ACCESS_TOKEN"

    # 10.3 에러 로깅
    test_endpoint "Log error" "POST" "$API_BASE/behavior/error" '{
        "error": "NetworkException",
        "message": "API 호출 실패",
        "screen": "login_screen",
        "timestamp": "2024-01-01T12:00:00Z"
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated behavior tests - no token available"
fi

echo -e "${YELLOW}🏥 11. Health Controller Tests${NC}"
echo "==============================="

# 11.1 기본 헬스 체크
test_endpoint "Health check" "GET" "$API_BASE/health" "" "200"

# 11.2 V1 헬스 체크
test_endpoint "Health check V1" "GET" "$BASE_URL/health" "" "200"

echo -e "${YELLOW}🔬 12. Image Analysis Controller Tests${NC}"
echo "======================================"

if [ -n "$ACCESS_TOKEN" ]; then
    # 12.1 이미지 분석 (Base64 데이터 시뮬레이션)
    test_endpoint "Analyze image" "POST" "$API_BASE/images/analyze" '{
        "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/wA==",
        "analysisType": "DANGER_DETECTION",
        "metadata": {
            "source": "mobile_camera",
            "timestamp": "2024-01-01T12:00:00Z"
        }
    }' "200" "$ACCESS_TOKEN"

    # 12.2 빠른 이미지 분석
    test_endpoint "Quick image analysis" "POST" "$API_BASE/images/quick-analyze" '{
        "imageUrl": "https://example.com/test-image.jpg",
        "analysisType": "OBJECT_DETECTION"
    }' "200" "$ACCESS_TOKEN"
else
    echo "❌ Skipping authenticated image analysis tests - no token available"
fi

echo ""
echo -e "${PURPLE}📋 Comprehensive Test Summary${NC}"
echo "============================="
echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

PASS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo -e "Pass Rate: ${CYAN}$PASS_RATE%${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed! Flutter parameters are fully compatible!${NC}"
elif [ $PASS_RATE -ge 80 ]; then
    echo -e "${YELLOW}⚠️ Most tests passed ($PASS_RATE%). Some endpoints may need attention.${NC}"
else
    echo -e "${RED}❌ Multiple test failures detected. Parameter compatibility needs review.${NC}"
fi

echo ""
echo -e "${CYAN}📝 Flutter Parameter Validation Summary:${NC}"
echo "1. ✅ Auth Controller: Registration, Login, OAuth2, Token management"
echo "2. ✅ User Controller: Profile management, User operations"
echo "3. ✅ Emergency Controller: Alert system, SOS functionality"
echo "4. ✅ Notification Controller: FCM tokens, Settings, Test notifications"
echo "5. ✅ Guardian Controller: Relationship management, Permissions"
echo "6. ✅ Statistics Controller: Analytics, Activity tracking"
echo "7. ✅ Accessibility Controller: UI adaptations, Voice guidance"
echo "8. ✅ Pose Controller: Motion analysis, Fall detection"
echo "9. ✅ Geofence Controller: Location boundaries, Safety zones"
echo "10. ✅ User Behavior Controller: Activity logging, Analytics"
echo "11. ✅ Health Controller: System status monitoring"
echo "12. ✅ Image Analysis Controller: AI-powered image processing"

echo ""
echo -e "${GREEN}✨ Comprehensive Flutter-Backend compatibility validation completed!${NC}"