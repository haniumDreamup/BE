#!/bin/bash

# 모든 Controller에서 API 엔드포인트 추출
echo "🔍 모든 API 엔드포인트 추출 중..."

OUTPUT_FILE="ALL_ENDPOINTS_EXTRACTED.txt"
> "$OUTPUT_FILE"

echo "=== BIF-AI Backend 모든 API 엔드포인트 ===" >> "$OUTPUT_FILE"
echo "추출 시간: $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Controller 파일들에서 엔드포인트 추출
find src/main/java -name "*Controller*.java" | while read file; do
    controller_name=$(basename "$file" .java)
    echo "📁 Controller: $controller_name" >> "$OUTPUT_FILE"
    
    # RequestMapping, GetMapping, PostMapping, PutMapping, DeleteMapping, PatchMapping 찾기
    grep -n -E "(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)" "$file" | \
    grep -E "\"|'" | \
    sed 's/^[[:space:]]*//' | \
    while IFS= read -r line; do
        echo "   $line" >> "$OUTPUT_FILE"
    done
    
    echo "" >> "$OUTPUT_FILE"
done

echo "✅ 모든 엔드포인트가 $OUTPUT_FILE 파일에 추출되었습니다"
echo "📊 추출된 Controller 수: $(find src/main/java -name "*Controller*.java" | wc -l)"
echo "📈 추출된 엔드포인트 수: $(grep -c -E "(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)" src/main/java/com/bifai/reminder/bifai_backend/controller/*.java)"

cat "$OUTPUT_FILE"
