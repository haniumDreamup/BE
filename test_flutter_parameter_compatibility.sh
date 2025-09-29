#!/bin/bash

echo "=== Flutter 파라미터와 백엔드 DTO 세부 호환성 검증 ==="
echo "Flutter 앱에서 전송하는 실제 파라미터 구조와 백엔드 DTO 필드를 상세 비교합니다."
echo

# Flutter 프로젝트 경로 설정
FLUTTER_PATH="/Users/ihojun/Desktop/FE"
BACKEND_PATH="/Users/ihojun/Desktop/javaWorkSpace/BE"

echo "1. 인증 API 파라미터 비교..."
echo "=== Flutter 로그인 요청 파라미터 ==="
grep -A 20 -B 5 "loginWithCredentials\|loginRequest" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "'username'|'password'|'email'|'deviceId'|username:|password:|email:|deviceId:" | head -10

echo
echo "=== 백엔드 LoginRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*LoginRequest*.java" -exec cat {} \; | grep -E "private.*String|private.*Boolean" -A 1 -B 1

echo
echo "=== Flutter 회원가입 요청 파라미터 ==="
grep -A 30 -B 5 "registerWithEmail\|registerRequest\|registrationData" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "'username'|'email'|'password'|'fullName'|'confirmPassword'|'agree" | head -15

echo
echo "=== 백엔드 RegisterRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*RegisterRequest*.java" -exec cat {} \; | grep -E "private.*String|private.*Boolean" -A 1 -B 1

echo
echo "2. 긴급상황 API 파라미터 비교..."
echo "=== Flutter 긴급상황 요청 파라미터 ==="
grep -A 20 -B 5 "triggerEmergency\|emergencyAlert\|sendAlert" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "'alertType'|'latitude'|'longitude'|'description'|'message'" | head -10

echo
echo "=== 백엔드 EmergencyAlertRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*Emergency*Request*.java" -o -name "*Alert*Request*.java" | head -3 | xargs -I {} cat {} | grep -E "private.*String|private.*Double|private.*Boolean" -A 1 -B 1

echo
echo "3. 사용자 정보 API 파라미터 비교..."
echo "=== Flutter 사용자 업데이트 요청 파라미터 ==="
grep -A 20 -B 5 "updateProfile\|updateUser\|profileUpdate" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "'fullName'|'email'|'phone'|'address'|'dateOfBirth'" | head -10

echo
echo "=== 백엔드 UserUpdateRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*User*Request*.java" -o -name "*Update*Request*.java" | grep -v Test | head -3 | xargs -I {} cat {} 2>/dev/null | grep -E "private.*String|private.*Boolean|private.*LocalDate" -A 1 -B 1

echo
echo "4. 접근성 설정 API 파라미터 비교..."
echo "=== Flutter 접근성 설정 파라미터 ==="
grep -A 20 -B 5 "AccessibilitySettings\|updateAccessibility" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "'fontSize'|'highContrast'|'voiceGuidance'|'screenReader'" | head -10

echo
echo "=== 백엔드 AccessibilitySettingsRequest DTO ==="
find "$BACKEND_PATH/src/main/java" -name "*Accessibility*Request*.java" -o -name "*Settings*Request*.java" | grep -v Test | head -3 | xargs -I {} cat {} 2>/dev/null | grep -E "private.*String|private.*Boolean|private.*Integer" -A 1 -B 1

echo
echo "5. 잠재적 파라미터 불일치 분석..."

echo "=== DTO 필수 필드 vs Flutter 선택적 필드 확인 ==="
echo "백엔드 @NotNull/@NotEmpty 필드:"
find "$BACKEND_PATH/src/main/java" -name "*Request*.java" | xargs grep -l "@NotNull\|@NotEmpty" | head -5 | xargs -I {} grep -A 2 -B 1 "@NotNull\|@NotEmpty" {}

echo
echo "=== Flutter 에서 null 허용 필드 확인 ==="
grep -r "String?" "$FLUTTER_PATH/lib" --include="*.dart" | grep -E "username|email|password|fullName" | head -5

echo
echo "6. API 호출 방식 검증..."
echo "=== Flutter HTTP 요청 헤더 ==="
grep -A 10 -B 5 "Content-Type\|Authorization\|Accept" "$FLUTTER_PATH/lib" --include="*.dart" | head -10

echo
echo "=== 백엔드 Controller Accept/Produces 설정 ==="
find "$BACKEND_PATH/src/main/java" -name "*Controller*.java" | head -5 | xargs -I {} grep -A 2 -B 1 "@RequestMapping\|@PostMapping.*produces\|@GetMapping.*produces" {} | head -10

echo
echo "=== 검증 완료 ==="
echo "Flutter와 백엔드 간 파라미터 호환성 상세 분석이 완료되었습니다."
echo "위의 출력에서 데이터 타입, 필수/선택 필드, 필드명 등의 불일치를 확인하세요."