# YOLOv8 Integration Research for BIF-AI Backend

## 개요
BIF-AI 백엔드에 YOLOv8 객체 인식 기능을 통합하기 위한 베스트 프랙티스 연구 문서입니다.

## YOLOv8과 Spring Boot 통합 방안

### 1. 아키텍처 옵션

#### Option 1: Python 마이크로서비스 방식 (권장)
- **장점**:
  - YOLOv8의 네이티브 Python 환경 활용
  - 독립적인 스케일링 가능
  - ML 모델 업데이트가 메인 서비스에 영향 없음
  - GPU 리소스 효율적 관리
- **단점**:
  - 추가 서비스 관리 필요
  - 네트워크 레이턴시

#### Option 2: Deep Java Library (DJL) 사용
- **장점**:
  - Java 네이티브 솔루션
  - 단일 애플리케이션으로 관리
- **단점**:
  - YOLOv8 지원 제한적
  - 성능 오버헤드

#### Option 3: JNI/JNA를 통한 직접 통합
- **장점**:
  - 높은 성능
- **단점**:
  - 복잡한 구현
  - 플랫폼 의존성

### 2. 권장 아키텍처: Python 마이크로서비스

```
┌─────────────────┐     REST API      ┌──────────────────┐
│                 │ ←─────────────────→│                  │
│  Spring Boot    │                    │  Python Service  │
│  Backend        │                    │  (FastAPI)       │
│                 │                    │  - YOLOv8        │
└─────────────────┘                    └──────────────────┘
         ↓                                      ↓
    ┌─────────┐                           ┌─────────┐
    │ MySQL   │                           │ Redis   │
    └─────────┘                           └─────────┘
```

### 3. Python 서비스 구현 예시

```python
# app.py
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from ultralytics import YOLO
import cv2
import numpy as np
import base64
from typing import List, Dict, Any
import io
from PIL import Image
import logging

app = FastAPI()
logger = logging.getLogger(__name__)

# YOLOv8 모델 로드
model = YOLO('yolov8n.pt')  # nano 버전 사용 (경량화)

class ObjectDetectionService:
    def __init__(self, confidence_threshold: float = 0.5):
        self.confidence_threshold = confidence_threshold
        self.model = model
        
    async def detect_objects(self, image: np.ndarray) -> List[Dict[str, Any]]:
        """객체 감지 수행"""
        try:
            results = self.model(image, conf=self.confidence_threshold)
            
            detections = []
            for r in results:
                boxes = r.boxes
                if boxes is not None:
                    for box in boxes:
                        detection = {
                            "class_id": int(box.cls),
                            "class_name": self.model.names[int(box.cls)],
                            "confidence": float(box.conf),
                            "bbox": {
                                "x1": float(box.xyxy[0][0]),
                                "y1": float(box.xyxy[0][1]),
                                "x2": float(box.xyxy[0][2]),
                                "y2": float(box.xyxy[0][3])
                            }
                        }
                        detections.append(detection)
            
            return detections
        except Exception as e:
            logger.error(f"Object detection error: {str(e)}")
            raise

detection_service = ObjectDetectionService()

@app.post("/api/v1/detect")
async def detect_objects(file: UploadFile = File(...)):
    """이미지에서 객체 감지"""
    if not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드 가능합니다")
    
    try:
        # 이미지 읽기
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        # 객체 감지
        detections = await detection_service.detect_objects(image)
        
        # BIF 사용자를 위한 간단한 설명 생성
        simplified_result = simplify_for_bif_user(detections)
        
        return JSONResponse(content={
            "success": True,
            "detections": detections,
            "simple_description": simplified_result,
            "count": len(detections)
        })
        
    except Exception as e:
        logger.error(f"Detection error: {str(e)}")
        raise HTTPException(status_code=500, detail="객체 인식 중 오류가 발생했습니다")

def simplify_for_bif_user(detections: List[Dict]) -> str:
    """BIF 사용자를 위한 간단한 설명 생성"""
    if not detections:
        return "아무것도 발견하지 못했어요."
    
    # 객체별 카운트
    object_counts = {}
    for det in detections:
        name = det["class_name"]
        object_counts[name] = object_counts.get(name, 0) + 1
    
    # 간단한 문장으로 변환
    descriptions = []
    for obj, count in object_counts.items():
        if count == 1:
            descriptions.append(f"{obj} 1개")
        else:
            descriptions.append(f"{obj} {count}개")
    
    return "발견한 것: " + ", ".join(descriptions)

@app.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    return {"status": "healthy", "model": "YOLOv8"}
```

### 4. Spring Boot 통합 코드

```java
// YOLOv8Service.java
@Service
@Slf4j
@RequiredArgsConstructor
public class YOLOv8Service {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${yolov8.service.url:http://localhost:8001}")
    private String yolov8ServiceUrl;
    
    @Value("${yolov8.service.timeout:30000}")
    private int timeout;
    
    public ObjectDetectionResult detectObjects(MultipartFile imageFile) {
        try {
            // 이미지 검증
            validateImage(imageFile);
            
            // HTTP 요청 준비
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(convertToFile(imageFile)));
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);
            
            // YOLOv8 서비스 호출
            ResponseEntity<String> response = restTemplate.exchange(
                yolov8ServiceUrl + "/api/v1/detect",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            // 응답 파싱
            return objectMapper.readValue(response.getBody(), ObjectDetectionResult.class);
            
        } catch (Exception e) {
            log.error("YOLOv8 객체 감지 실패", e);
            throw new RuntimeException("객체 인식 서비스 오류", e);
        }
    }
    
    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 허용됩니다");
        }
        
        // 파일 크기 제한 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
        }
    }
}

// ObjectDetectionResult.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectDetectionResult {
    private boolean success;
    private List<Detection> detections;
    private String simpleDescription;
    private int count;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detection {
        private int classId;
        private String className;
        private double confidence;
        private BoundingBox bbox;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private double x1;
        private double y1;
        private double x2;
        private double y2;
    }
}

// ObjectDetectionController.java
@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vision API", description = "시각 인식 관련 API")
public class ObjectDetectionController {
    
    private final YOLOv8Service yolov8Service;
    private final SituationAnalysisService situationAnalysisService;
    
    @PostMapping("/detect")
    @Operation(summary = "객체 인식", description = "이미지에서 객체를 인식합니다")
    public ResponseEntity<BifApiResponse<ObjectDetectionResult>> detectObjects(
            @RequestParam("image") MultipartFile image) {
        
        log.info("객체 인식 요청 - 파일명: {}, 크기: {}", 
                image.getOriginalFilename(), image.getSize());
        
        ObjectDetectionResult result = yolov8Service.detectObjects(image);
        
        // BIF 사용자를 위한 상황 분석
        String situationAnalysis = situationAnalysisService.analyzeScene(result);
        result.setSimpleDescription(situationAnalysis);
        
        return ResponseEntity.ok(BifApiResponse.success(result));
    }
}
```

### 5. 성능 최적화 전략

#### 5.1 캐싱
```java
@Cacheable(value = "objectDetection", key = "#imageHash")
public ObjectDetectionResult detectObjectsCached(String imageHash, MultipartFile image) {
    return detectObjects(image);
}
```

#### 5.2 비동기 처리
```java
@Async
public CompletableFuture<ObjectDetectionResult> detectObjectsAsync(MultipartFile image) {
    return CompletableFuture.completedFuture(detectObjects(image));
}
```

#### 5.3 배치 처리
```java
public List<ObjectDetectionResult> detectObjectsBatch(List<MultipartFile> images) {
    return images.parallelStream()
        .map(this::detectObjects)
        .collect(Collectors.toList());
}
```

### 6. 모니터링 및 로깅

```java
@Component
@Slf4j
public class YOLOv8MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter detectionCounter;
    private final Timer detectionTimer;
    
    public YOLOv8MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.detectionCounter = Counter.builder("yolov8.detections")
            .description("Total number of object detections")
            .register(meterRegistry);
        this.detectionTimer = Timer.builder("yolov8.detection.time")
            .description("Object detection execution time")
            .register(meterRegistry);
    }
    
    public void recordDetection(long duration, int objectCount) {
        detectionCounter.increment();
        detectionTimer.record(duration, TimeUnit.MILLISECONDS);
        meterRegistry.gauge("yolov8.objects.detected", objectCount);
    }
}
```

### 7. 보안 고려사항

1. **이미지 검증**
   - 파일 형식 검증
   - 파일 크기 제한
   - 악성 코드 스캔

2. **API 보안**
   - Rate limiting
   - API 키 인증
   - HTTPS 통신

3. **데이터 프라이버시**
   - 임시 파일 즉시 삭제
   - 민감한 정보 마스킹
   - 로그에 개인정보 미포함

### 8. 에러 처리

```java
@ControllerAdvice
public class YOLOv8ExceptionHandler {
    
    @ExceptionHandler(YOLOv8ServiceException.class)
    public ResponseEntity<BifApiResponse<?>> handleYOLOv8Exception(
            YOLOv8ServiceException e) {
        log.error("YOLOv8 서비스 오류", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(BifApiResponse.error(
                "VISION_SERVICE_ERROR",
                "시각 인식 서비스를 사용할 수 없습니다",
                "잠시 후 다시 시도해주세요"
            ));
    }
}
```

### 9. 배포 고려사항

#### Docker Compose 설정
```yaml
version: '3.8'
services:
  spring-boot:
    build: .
    ports:
      - "8080:8080"
    environment:
      - YOLOV8_SERVICE_URL=http://yolov8:8001
    depends_on:
      - yolov8
      
  yolov8:
    build: ./yolov8-service
    ports:
      - "8001:8001"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

### 10. 테스트 전략

```java
@SpringBootTest
@AutoConfigureMockMvc
class ObjectDetectionIntegrationTest {
    
    @MockBean
    private YOLOv8Service yolov8Service;
    
    @Test
    void testObjectDetection() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", 
            "test image content".getBytes()
        );
        
        ObjectDetectionResult mockResult = new ObjectDetectionResult();
        mockResult.setSuccess(true);
        mockResult.setCount(2);
        mockResult.setSimpleDescription("사람 1명, 자동차 1대를 발견했어요");
        
        when(yolov8Service.detectObjects(any())).thenReturn(mockResult);
        
        // When & Then
        mockMvc.perform(multipart("/api/v1/vision/detect")
                .file(image))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.count").value(2));
    }
}
```

## 결론

YOLOv8을 Spring Boot와 통합하는 가장 효과적인 방법은 Python 마이크로서비스를 별도로 구축하고 REST API로 통신하는 것입니다. 이 방식은 각 기술 스택의 장점을 최대한 활용하면서도 유지보수와 확장성을 보장합니다.

BIF 사용자를 위해서는 복잡한 객체 인식 결과를 간단하고 이해하기 쉬운 형태로 변환하는 것이 중요합니다.