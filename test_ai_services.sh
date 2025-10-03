#!/bin/bash

# AI 서비스 통합 테스트 스크립트

echo "🧪 AI 서비스 통합 테스트 시작"
echo "========================================"

# 환경 변수 로드
if [ -f .env ]; then
  export $(cat .env | grep -v '^#' | xargs)
  echo "✅ .env 파일 로드 완료"
else
  echo "⚠️  .env 파일이 없습니다"
fi

echo ""
echo "📋 환경 변수 확인:"
echo "  GOOGLE_VISION_ENABLED: ${GOOGLE_VISION_ENABLED}"
echo "  FCM_ENABLED: ${FCM_ENABLED}"
echo "  OPENAI_API_KEY: ${OPENAI_API_KEY:0:20}..."
echo ""

# OpenAI 서비스 테스트
echo "1️⃣ OpenAI ChatClient 테스트"
echo "----------------------------------------"
./gradlew test --tests "*OpenAIChatClientIntegrationTest*" --quiet 2>&1 | grep -E "(OpenAI|✅|⚠️|PASSED|FAILED|SKIPPED)"
echo ""

# Google Vision 서비스 테스트
echo "2️⃣ Google Cloud Vision 테스트"
echo "----------------------------------------"
./gradlew test --tests "*GoogleVisionIntegrationTest*" --quiet 2>&1 | grep -E "(Vision|✅|⚠️|PASSED|FAILED|SKIPPED)"
echo ""

# FCM 서비스 테스트
echo "3️⃣ Firebase Cloud Messaging 테스트"
echo "----------------------------------------"
./gradlew test --tests "*FcmServiceIntegrationTest*" --quiet 2>&1 | grep -E "(FCM|✅|⚠️|PASSED|FAILED|SKIPPED)"
echo ""

echo "========================================"
echo "✅ AI 서비스 통합 테스트 완료"
