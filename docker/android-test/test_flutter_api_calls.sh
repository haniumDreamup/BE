#!/bin/bash

echo "=== Flutter API 호출 테스트 ==="
echo "프로덕션 서버: http://43.200.49.171:8080"
echo "Flutter 앱: http://localhost:3008"
echo

# 1. 회원가입 테스트 데이터 생성
echo "1. 회원가입 API 테스트..."
REGISTER_RESPONSE=$(curl -s -X POST "http://43.200.49.171:8080/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "flutter_test_user_'$(date +%s)'",
    "password": "TestPassword123!",
    "confirmPassword": "TestPassword123!",
    "email": "flutter.test.'$(date +%s)'@example.com",
    "fullName": "플러터 테스트 사용자",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
  }')

echo "회원가입 응답: $REGISTER_RESPONSE"

# Extract username from response if successful
USERNAME=$(echo $REGISTER_RESPONSE | grep -o '"username":"[^"]*"' | cut -d'"' -f4)

if [ -n "$USERNAME" ]; then
    echo "회원가입 성공 - 사용자명: $USERNAME"

    # 2. 로그인 테스트
    echo "2. 로그인 API 테스트..."
    LOGIN_RESPONSE=$(curl -s -X POST "http://43.200.49.171:8080/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "username": "'$USERNAME'",
        "password": "TestPassword123!"
      }')

    echo "로그인 응답: $LOGIN_RESPONSE"

    # Extract token
    TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

    if [ -n "$TOKEN" ]; then
        echo "로그인 성공 - 토큰 획득됨"

        # 3. 인증이 필요한 API 테스트
        echo "3. 사용자 정보 조회 API 테스트..."
        USER_INFO_RESPONSE=$(curl -s -X GET "http://43.200.49.171:8080/api/v1/users/me" \
          -H "Authorization: Bearer $TOKEN")

        echo "사용자 정보 응답: $USER_INFO_RESPONSE"

        # 4. 긴급상황 알림 테스트
        echo "4. 긴급상황 API 테스트..."
        EMERGENCY_RESPONSE=$(curl -s -X POST "http://43.200.49.171:8080/api/v1/emergency/alert" \
          -H "Authorization: Bearer $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{
            "alertType": "FALL_DETECTION",
            "latitude": 37.5665,
            "longitude": 126.9780,
            "description": "Flutter API 테스트용 긴급 알림"
          }')

        echo "긴급상황 응답: $EMERGENCY_RESPONSE"

        # 5. 접근성 설정 조회 테스트
        echo "5. 접근성 설정 조회 API 테스트..."
        ACCESSIBILITY_RESPONSE=$(curl -s -X GET "http://43.200.49.171:8080/api/v1/accessibility/settings" \
          -H "Authorization: Bearer $TOKEN")

        echo "접근성 설정 응답: $ACCESSIBILITY_RESPONSE"

        # 6. 통계 조회 테스트
        echo "6. 통계 조회 API 테스트..."
        STATS_RESPONSE=$(curl -s -X GET "http://43.200.49.171:8080/api/statistics/summary" \
          -H "Authorization: Bearer $TOKEN")

        echo "통계 조회 응답: $STATS_RESPONSE"

    else
        echo "❌ 로그인 실패 - 토큰을 얻을 수 없습니다"
        exit 1
    fi
else
    echo "❌ 회원가입 실패"
    exit 1
fi

echo
echo "=== 테스트 완료 ==="
echo "모든 주요 엔드포인트가 정상적으로 응답하는지 확인하세요."
echo "Flutter 앱에서 동일한 경로로 API 호출이 이루어져야 합니다."