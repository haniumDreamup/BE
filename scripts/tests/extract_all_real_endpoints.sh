#!/bin/bash

# 모든 실제 엔드포인트를 추출하는 스크립트
# RequestMapping, GetMapping, PostMapping 등을 모두 찾아서 실제 경로 생성

echo "🔍 모든 컨트롤러에서 실제 엔드포인트 추출 중..."

# 결과 파일
OUTPUT_FILE="all_real_endpoints.txt"
> "$OUTPUT_FILE"

# 컨트롤러 디렉토리
CONTROLLER_DIR="src/main/java/com/bifai/reminder/bifai_backend/controller"

# 모든 컨트롤러 파일 찾기
find "$CONTROLLER_DIR" -name "*.java" | while read -r file; do
    echo "📁 처리 중: $file"

    # 클래스 레벨 @RequestMapping 추출
    class_path=$(grep -n "@RequestMapping" "$file" | head -1 | sed 's/.*@RequestMapping[^"]*"\([^"]*\)".*/\1/')
    if [ -z "$class_path" ]; then
        class_path=""
    fi

    # 각 메서드의 매핑 추출
    grep -n -A5 -B1 "@.*Mapping" "$file" | while IFS= read -r line; do
        if [[ $line == *"@GetMapping"* ]]; then
            method="GET"
            path=$(echo "$line" | sed 's/.*@GetMapping[^"]*"\([^"]*\)".*/\1/')
        elif [[ $line == *"@PostMapping"* ]]; then
            method="POST"
            path=$(echo "$line" | sed 's/.*@PostMapping[^"]*"\([^"]*\)".*/\1/')
        elif [[ $line == *"@PutMapping"* ]]; then
            method="PUT"
            path=$(echo "$line" | sed 's/.*@PutMapping[^"]*"\([^"]*\)".*/\1/')
        elif [[ $line == *"@DeleteMapping"* ]]; then
            method="DELETE"
            path=$(echo "$line" | sed 's/.*@DeleteMapping[^"]*"\([^"]*\)".*/\1/')
        elif [[ $line == *"@PatchMapping"* ]]; then
            method="PATCH"
            path=$(echo "$line" | sed 's/.*@PatchMapping[^"]*"\([^"]*\)".*/\1/')
        elif [[ $line == *"@RequestMapping"* ]] && [[ $line != *"class"* ]]; then
            # RequestMapping의 method 추출
            if [[ $line == *"RequestMethod.GET"* ]]; then
                method="GET"
            elif [[ $line == *"RequestMethod.POST"* ]]; then
                method="POST"
            elif [[ $line == *"RequestMethod.PUT"* ]]; then
                method="PUT"
            elif [[ $line == *"RequestMethod.DELETE"* ]]; then
                method="DELETE"
            elif [[ $line == *"RequestMethod.PATCH"* ]]; then
                method="PATCH"
            else
                method="GET"  # 기본값
            fi
            path=$(echo "$line" | sed 's/.*@RequestMapping[^"]*"\([^"]*\)".*/\1/')
        else
            continue
        fi

        # 경로가 추출되었다면
        if [ -n "$path" ] && [ "$path" != "$line" ]; then
            # 전체 경로 생성
            if [ -n "$class_path" ]; then
                full_path="${class_path}${path}"
            else
                full_path="$path"
            fi

            # 중복 슬래시 제거
            full_path=$(echo "$full_path" | sed 's|//|/|g')

            echo "$method|$full_path" >> "$OUTPUT_FILE"
        fi
    done
done

# 중복 제거 및 정렬
sort -u "$OUTPUT_FILE" -o "$OUTPUT_FILE"

echo "✅ 추출 완료: $OUTPUT_FILE"
echo "📊 총 엔드포인트 수: $(wc -l < "$OUTPUT_FILE")"

# 결과 미리보기
echo ""
echo "🔍 추출된 엔드포인트 미리보기 (처음 20개):"
head -20 "$OUTPUT_FILE"

if [ $(wc -l < "$OUTPUT_FILE") -gt 20 ]; then
    echo "..."
    echo "더 많은 엔드포인트가 있습니다. 전체 목록은 $OUTPUT_FILE을 확인하세요."
fi