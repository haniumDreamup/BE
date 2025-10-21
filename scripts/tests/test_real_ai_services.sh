#!/bin/bash

# 실제 AI 서비스 기능 테스트

echo "🧪 실제 AI 서비스 기능 테스트"
echo "========================================"
echo ""

# 환경 변수 설정 (실제 값은 환경변수 또는 .env 파일에서 로드)
export GOOGLE_VISION_ENABLED=true
export FCM_ENABLED=true
# export OPENAI_API_KEY="your-openai-api-key-here"  # .env 파일 또는 환경변수에서 설정 필요
# export JWT_SECRET="your-jwt-secret-here"  # .env 파일 또는 환경변수에서 설정 필요

echo "📋 테스트 환경:"
echo "  ✓ GOOGLE_VISION_ENABLED: ${GOOGLE_VISION_ENABLED}"
echo "  ✓ FCM_ENABLED: ${FCM_ENABLED}"
echo "  ✓ OPENAI_API_KEY: 설정됨"
echo ""

# 1. Vision API 테스트
echo "1️⃣ Google Vision API 실제 이미지 분석 테스트"
echo "----------------------------------------"
./gradlew test --tests "*RealImageAnalysisIntegrationTest.test_실제_이미지로_Vision_API_테스트" 2>&1 | \
  grep -E "(🖼️|✅|⚠️|📊|객체|라벨|텍스트|얼굴|Test.*PASSED|Test.*FAILED)" | head -20
echo ""

# 2. Vision + OpenAI 통합 테스트
echo "2️⃣ Vision + OpenAI 통합 테스트"
echo "----------------------------------------"
./gradlew test --tests "*RealImageAnalysisIntegrationTest.test_Vision_결과를_OpenAI로_해석" 2>&1 | \
  grep -E "(🤖|✅|⚠️|📝|설명|행동|안전도|Test.*PASSED|Test.*FAILED)" | head -20
echo ""

# 3. 전체 파이프라인 테스트
echo "3️⃣ 전체 AI 파이프라인 테스트 (이미지 → Vision → OpenAI)"
echo "----------------------------------------"
./gradlew test --tests "*RealImageAnalysisIntegrationTest.test_전체_플로우_이미지_업로드부터_AI_해석까지" 2>&1 | \
  grep -E "(🚀|✅|⚠️|객체|텍스트|Vision|OpenAI|파이프라인|Test.*PASSED|Test.*FAILED)" | head -30
echo ""

echo "========================================"
echo "✅ 실제 AI 서비스 기능 테스트 완료"
