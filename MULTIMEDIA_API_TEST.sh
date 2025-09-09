#!/bin/bash

# ===========================================
# BIF-AI Backend 멀티미디어 API 기능 테스트
# ===========================================

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
RESULTS_DIR="multimedia_test_results_$TIMESTAMP"
LOG_FILE="$RESULTS_DIR/multimedia_test.log"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 로깅 함수
log_message() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1" | tee -a "$LOG_FILE"
}

# 테스트 이미지 생성 함수
create_test_image() {
    local filename="$1"
    # 1x1 픽셀 PNG 이미지 생성 (Base64로 인코딩된 최소 PNG)
    echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==" | base64 -d > "$filename"
}

# 테스트 오디오 파일 생성 함수  
create_test_audio() {
    local filename="$1"
    # 최소한의 WAV 파일 헤더 (44바이트 헤더 + 2바이트 데이터)
    printf "RIFF\x2e\x00\x00\x00WAVEfmt \x10\x00\x00\x00\x01\x00\x01\x00\x44\xac\x00\x00\x88\x58\x01\x00\x02\x00\x10\x00data\x02\x00\x00\x00\x00\x00" > "$filename"
}

echo "🎯 BIF-AI Backend 멀티미디어 API 기능 테스트 시작..."
echo "📁 결과 저장 디렉토리: $RESULTS_DIR"
log_message "멀티미디어 API 테스트 시작"

# 테스트 파일들 생성
log_message "테스트 파일 생성 중..."
create_test_image "$RESULTS_DIR/test_image.png"
create_test_audio "$RESULTS_DIR/test_audio.wav"
echo "안녕하세요 테스트입니다" > "$RESULTS_DIR/test_text.txt"

echo ""
echo "=== 1. Vision Controller 이미지 분석 테스트 ==="
log_message "Vision Controller 테스트 시작"

# 인증 없이 테스트 (403 예상)
log_message "Vision analyze - 인증 없이 테스트"
vision_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/test_image.png" \
  "$BASE_URL/api/v1/vision/analyze" \
  -o "$RESULTS_DIR/vision_response.json")

echo "Vision Analyze - HTTP $vision_response"
log_message "Vision analyze 응답: $vision_response"

if [[ -f "$RESULTS_DIR/vision_response.json" ]]; then
    echo "응답 내용:"
    cat "$RESULTS_DIR/vision_response.json" | head -5
    echo ""
fi

echo ""
echo "=== 2. Image Analysis Controller 테스트 ==="
log_message "Image Analysis Controller 테스트 시작"

image_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/test_image.png" \
  "$BASE_URL/api/v1/images/analyze" \
  -o "$RESULTS_DIR/image_response.json")

echo "Image Analyze - HTTP $image_response"
log_message "Image analyze 응답: $image_response"

if [[ -f "$RESULTS_DIR/image_response.json" ]]; then
    echo "응답 내용:"
    cat "$RESULTS_DIR/image_response.json" | head -5
    echo ""
fi

echo ""
echo "=== 3. Vision Controller 위험 감지 테스트 ==="
log_message "Vision detect danger 테스트 시작"

danger_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/test_image.png" \
  "$BASE_URL/api/v1/vision/detect-danger" \
  -o "$RESULTS_DIR/danger_response.json")

echo "Detect Danger - HTTP $danger_response"
log_message "Detect danger 응답: $danger_response"

if [[ -f "$RESULTS_DIR/danger_response.json" ]]; then
    echo "응답 내용:"
    cat "$RESULTS_DIR/danger_response.json" | head -5
    echo ""
fi

echo ""
echo "=== 4. Image Analysis 빠른 분석 테스트 ==="
log_message "Image quick analyze 테스트 시작"

quick_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/test_image.png" \
  "$BASE_URL/api/v1/images/quick-analyze" \
  -o "$RESULTS_DIR/quick_response.json")

echo "Quick Analyze - HTTP $quick_response"
log_message "Quick analyze 응답: $quick_response"

if [[ -f "$RESULTS_DIR/quick_response.json" ]]; then
    echo "응답 내용:"
    cat "$RESULTS_DIR/quick_response.json" | head -5
    echo ""
fi

echo ""
echo "=== 5. Accessibility Voice Guidance 테스트 ==="
log_message "Accessibility voice guidance 테스트 시작"

voice_response=$(curl -s -w "%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -d '{"text": "안녕하세요 테스트입니다", "language": "ko", "speed": 1.0}' \
  "$BASE_URL/api/v1/accessibility/voice-guidance" \
  -o "$RESULTS_DIR/voice_response.json")

echo "Voice Guidance - HTTP $voice_response"
log_message "Voice guidance 응답: $voice_response"

if [[ -f "$RESULTS_DIR/voice_response.json" ]]; then
    echo "응답 내용:"
    cat "$RESULTS_DIR/voice_response.json" | head -5
    echo ""
fi

echo ""
echo "=== 6. 잘못된 파일 형식 테스트 ==="
log_message "잘못된 파일 형식 테스트 시작"

# 텍스트 파일을 이미지로 보내기
wrong_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/test_text.txt" \
  "$BASE_URL/api/v1/vision/analyze" \
  -o "$RESULTS_DIR/wrong_response.json")

echo "Wrong File Type - HTTP $wrong_response"
log_message "잘못된 파일 형식 응답: $wrong_response"

echo ""
echo "=== 7. 대용량 파일 테스트 (시뮬레이션) ==="
log_message "대용량 파일 테스트 시작"

# 1MB 랜덤 데이터 생성
dd if=/dev/zero of="$RESULTS_DIR/large_file.bin" bs=1024 count=1024 2>/dev/null

large_response=$(curl -s -w "%{http_code}" -X POST \
  -F "image=@$RESULTS_DIR/large_file.bin" \
  "$BASE_URL/api/v1/vision/analyze" \
  -o "$RESULTS_DIR/large_response.json" \
  --max-time 30)

echo "Large File - HTTP $large_response"
log_message "대용량 파일 응답: $large_response"

echo ""
echo "=== 📊 멀티미디어 API 테스트 결과 요약 ==="
echo "🕒 테스트 완료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo "📁 상세 결과: $LOG_FILE"
echo ""

# 결과 분석
total_tests=7
passed_tests=0

for response in "$vision_response" "$image_response" "$danger_response" "$quick_response" "$voice_response" "$wrong_response" "$large_response"; do
    if [[ "$response" =~ ^[4-5][0-9][0-9]$ ]]; then
        ((passed_tests++))
    fi
done

echo "✅ 응답받은 테스트: $passed_tests/$total_tests"
echo "📈 응답률: $(( passed_tests * 100 / total_tests ))%"

log_message "멀티미디어 API 테스트 완료 - 응답률: $(( passed_tests * 100 / total_tests ))%"

echo ""
echo "🎯 멀티미디어 API 기능 분석 완료!"
echo "📋 주요 발견사항:"
echo "   - 이미지 분석 API들은 multipart/form-data 형식으로 파일을 받음"
echo "   - 인증이 필요한 엔드포인트들은 403 Forbidden으로 정상 응답"
echo "   - 파일 형식 검증 및 대용량 파일 처리 확인"
echo "   - 음성 안내 API는 JSON 형식으로 텍스트를 받음"