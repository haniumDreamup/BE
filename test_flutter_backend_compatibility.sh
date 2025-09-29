#!/bin/bash

echo "=== Flutter 백엔드 API 호환성 검증 ==="
echo "Flutter 앱에서 사용하는 API 엔드포인트와 파라미터가 백엔드와 호환되는지 검사합니다."
echo

# Flutter 프로젝트 경로 설정
FLUTTER_PATH="/Users/ihojun/Desktop/FE"
BACKEND_PATH="/Users/ihojun/Desktop/javaWorkSpace/BE"

echo "1. Flutter 앱의 API 엔드포인트 추출..."

# Flutter 앱에서 사용하는 API 엔드포인트 분석
echo "=== 인증 관련 API 엔드포인트 ==="
grep -r "api/v1/auth" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "=== 사용자 관련 API 엔드포인트 ==="
grep -r "api/v1/users" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "=== 긴급상황 관련 API 엔드포인트 ==="
grep -r "api/v1/emergency" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "=== 접근성 관련 API 엔드포인트 ==="
grep -r "api/v1/accessibility" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "=== 통계 관련 API 엔드포인트 ==="
grep -r "api/statistics" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "2. 백엔드 컨트롤러의 실제 엔드포인트 확인..."

# 백엔드 컨트롤러에서 정의된 엔드포인트 분석
echo "=== AuthController 엔드포인트 ==="
grep -r "@PostMapping\|@GetMapping\|@PutMapping\|@DeleteMapping" "$BACKEND_PATH/src/main/java" --include="*AuthController*.java" | grep -E "login|register|refresh"

echo
echo "=== UserController 엔드포인트 ==="
grep -r "@PostMapping\|@GetMapping\|@PutMapping\|@DeleteMapping" "$BACKEND_PATH/src/main/java" --include="*UserController*.java"

echo
echo "=== EmergencyController 엔드포인트 ==="
grep -r "@PostMapping\|@GetMapping\|@PutMapping\|@DeleteMapping" "$BACKEND_PATH/src/main/java" --include="*EmergencyController*.java"

echo
echo "=== AccessibilityController 엔드포인트 ==="
grep -r "@PostMapping\|@GetMapping\|@PutMapping\|@DeleteMapping" "$BACKEND_PATH/src/main/java" --include="*AccessibilityController*.java"

echo
echo "=== StatisticsController 엔드포인트 ==="
grep -r "@PostMapping\|@GetMapping\|@PutMapping\|@DeleteMapping" "$BACKEND_PATH/src/main/java" --include="*StatisticsController*.java"

echo
echo "3. Flutter API 호출 파라미터 분석..."

# Flutter에서 사용하는 JSON 파라미터 구조 분석
echo "=== 로그인 파라미터 구조 ==="
grep -A 10 -B 5 "username.*password" "$FLUTTER_PATH/lib" --include="*.dart"

echo
echo "=== 회원가입 파라미터 구조 ==="
grep -A 10 -B 5 "registerRequest\|RegisterRequest" "$FLUTTER_PATH/lib" --include="*.dart"

echo
echo "=== 긴급상황 파라미터 구조 ==="
grep -A 10 -B 5 "alertType\|latitude\|longitude" "$FLUTTER_PATH/lib" --include="*.dart"

echo
echo "4. 백엔드 DTO 구조 확인..."

# 백엔드 DTO 클래스의 필드 분석
echo "=== LoginRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*LoginRequest*.java" -exec grep -E "private.*String|private.*Boolean|private.*Long" {} \;

echo
echo "=== RegisterRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*RegisterRequest*.java" -exec grep -E "private.*String|private.*Boolean|private.*Long" {} \;

echo
echo "=== EmergencyAlert DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*Emergency*Request*.java" -o -name "*Alert*Request*.java" | head -3 | xargs -I {} grep -E "private.*String|private.*Boolean|private.*Double|private.*Long" {}

echo
echo "=== 검증 완료 ==="
echo "Flutter 앱과 백엔드 API 간의 호환성을 확인했습니다."
echo "불일치하는 부분이 있다면 위의 출력을 비교 분석하세요."