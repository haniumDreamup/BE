# OpenAI Spring Boot Integration Best Practices

## 1. 의존성 추가

### Gradle
```gradle
implementation("com.openai:openai-java:2.12.0")
```

### Maven
```xml
<dependency>
  <groupId>com.openai</groupId>
  <artifactId>openai-java</artifactId>
  <version>2.12.0</version>
</dependency>
```

## 2. 클라이언트 설정 Best Practices

### 환경 변수 기반 설정
```java
// 환경 변수: OPENAI_API_KEY, OPENAI_ORG_ID, OPENAI_PROJECT_ID
OpenAIClient client = OpenAIOkHttpClient.fromEnv();
```

### Spring Bean 설정
```java
@Configuration
public class OpenAIConfig {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${openai.api.max-retries:3}")
    private int maxRetries;
    
    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .maxRetries(maxRetries)
            .streamHandlerExecutor(Executors.newFixedThreadPool(4))
            .responseValidation(true)
            .build();
    }
    
    @Bean
    public OpenAIClientAsync openAIClientAsync() {
        return OpenAIOkHttpClientAsync.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .maxRetries(maxRetries)
            .streamHandlerExecutor(Executors.newFixedThreadPool(4))
            .build();
    }
}
```

## 3. 에러 처리 전략

### 재시도 및 타임아웃 설정
```java
@Service
public class OpenAIService {
    
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 30000; // 30초
    
    public String generateResponse(String prompt) {
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4_1)
                .addUserMessage(prompt)
                .maxCompletionTokens(500)
                .temperature(0.7)
                .build();
                
            return client.chat().completions()
                .create(params, RequestOptions.builder()
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .build())
                .choices().get(0).message().content().orElse("");
                
        } catch (OpenAIException e) {
            log.error("OpenAI API error: {}", e.getMessage());
            throw new ServiceException("AI 서비스 일시 오류");
        }
    }
}
```

## 4. 프롬프트 템플릿 관리

### BIF 사용자를 위한 프롬프트 최적화
```java
@Component
public class BIFPromptTemplates {
    
    public static final String SIMPLIFY_RESPONSE = """
        다음 내용을 초등학교 5학년 수준으로 쉽게 설명해주세요:
        - 짧고 간단한 문장 사용
        - 어려운 용어는 쉬운 말로 바꾸기
        - 중요한 내용은 번호로 정리
        - 긍정적이고 격려하는 톤 사용
        
        내용: %s
        """;
    
    public static final String SITUATION_ANALYSIS = """
        다음 상황을 분석하고 BIF 사용자가 이해하기 쉽게 설명해주세요:
        
        상황: %s
        사용자 정보: %s
        
        다음 형식으로 답변:
        1. 지금 상황: (한 문장으로)
        2. 해야 할 일: (단계별로)
        3. 주의사항: (중요한 것만)
        """;
}
```

## 5. 스트리밍 응답 처리

### 동기 스트리밍
```java
@Service
public class StreamingService {
    
    public void streamResponse(String prompt, Consumer<String> onChunk) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_4_1)
            .addUserMessage(prompt)
            .stream(true)
            .build();
            
        try (StreamResponse<ChatCompletionChunk> stream = 
                client.chat().completions().createStreaming(params)) {
            stream.stream()
                .flatMap(chunk -> chunk.choices().stream())
                .flatMap(choice -> choice.delta().content().stream())
                .forEach(onChunk);
        }
    }
}
```

### 비동기 스트리밍 (WebFlux)
```java
@Service
public class ReactiveOpenAIService {
    
    public Flux<String> streamResponseReactive(String prompt) {
        return Flux.create(sink -> {
            CompletableFuture<Void> future = clientAsync.chat()
                .completions()
                .createStreaming(params)
                .subscribe(chunk -> {
                    chunk.choices().stream()
                        .flatMap(choice -> choice.delta().content().stream())
                        .forEach(sink::next);
                })
                .onCompleteFuture();
                
            future.whenComplete((result, error) -> {
                if (error != null) {
                    sink.error(error);
                } else {
                    sink.complete();
                }
            });
        });
    }
}
```

## 6. 캐싱 전략

### Spring Cache 통합
```java
@Service
@Slf4j
public class CachedOpenAIService {
    
    @Cacheable(value = "openai-responses", 
               key = "#prompt.hashCode()", 
               condition = "#prompt.length() < 100")
    public String getCachedResponse(String prompt) {
        return generateResponse(prompt);
    }
    
    @CacheEvict(value = "openai-responses", allEntries = true)
    @Scheduled(fixedDelay = 3600000) // 1시간마다
    public void evictCache() {
        log.info("OpenAI 응답 캐시 초기화");
    }
}
```

## 7. 사용량 모니터링

### API 사용량 추적
```java
@Component
@Slf4j
public class OpenAIUsageMonitor {
    
    private final MeterRegistry meterRegistry;
    private final AtomicLong totalTokens = new AtomicLong();
    
    @EventListener
    public void handleApiCall(OpenAIApiCallEvent event) {
        meterRegistry.counter("openai.api.calls", 
            "model", event.getModel(),
            "status", event.getStatus())
            .increment();
            
        meterRegistry.gauge("openai.tokens.total", totalTokens);
        
        if (event.getUsage() != null) {
            totalTokens.addAndGet(event.getUsage().getTotalTokens());
        }
    }
}
```

## 8. 보안 고려사항

### API 키 관리
```yaml
# application.yml
openai:
  api:
    key: ${OPENAI_API_KEY:}
    organization: ${OPENAI_ORG_ID:}
    project: ${OPENAI_PROJECT_ID:}
```

### 요청 검증
```java
@Component
public class OpenAIRequestValidator {
    
    private static final int MAX_PROMPT_LENGTH = 4000;
    private static final Pattern SAFE_CONTENT = Pattern.compile("^[\\p{L}\\p{N}\\s.,!?가-힣]+$");
    
    public void validateRequest(String prompt) {
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new ValidationException("요청이 너무 깁니다");
        }
        
        if (!SAFE_CONTENT.matcher(prompt).matches()) {
            throw new ValidationException("허용되지 않은 문자가 포함되어 있습니다");
        }
    }
}
```

## 9. 통합 테스트

### MockServer 사용
```java
@SpringBootTest
@AutoConfigureMockMvc
public class OpenAIIntegrationTest {
    
    @MockBean
    private OpenAIClient openAIClient;
    
    @Test
    public void testChatCompletion() {
        // Given
        ChatCompletion mockResponse = ChatCompletion.builder()
            .addChoice(ChatCompletion.Choice.builder()
                .message(ChatCompletionMessage.builder()
                    .content("테스트 응답")
                    .build())
                .build())
            .build();
            
        when(openAIClient.chat().completions().create(any()))
            .thenReturn(mockResponse);
            
        // When & Then
        // 테스트 로직
    }
}
```

## 10. 성능 최적화

### 연결 풀 설정
```java
@Configuration
public class OpenAIPerformanceConfig {
    
    @Bean
    public OkHttpClient customOkHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(
                10, // maxIdleConnections
                5, // keepAliveDuration
                TimeUnit.MINUTES
            ))
            .build();
    }
}
```

## 11. BIF 특화 기능

### 응답 단순화 서비스
```java
@Service
public class BIFResponseSimplifier {
    
    private static final Map<String, String> SIMPLE_WORDS = Map.of(
        "구매", "사기",
        "판매", "팔기",
        "예약", "미리 정하기",
        "취소", "안 하기"
    );
    
    public String simplifyResponse(String original) {
        String simplified = original;
        
        // 어려운 단어 치환
        for (Map.Entry<String, String> entry : SIMPLE_WORDS.entrySet()) {
            simplified = simplified.replace(entry.getKey(), entry.getValue());
        }
        
        // 문장 길이 제한
        String[] sentences = simplified.split("\\. ");
        return Arrays.stream(sentences)
            .map(s -> s.length() > 50 ? s.substring(0, 47) + "..." : s)
            .collect(Collectors.joining(". "));
    }
}
```

## 참고사항

1. **API 키 보안**: 절대 코드에 직접 하드코딩하지 말고 환경변수 사용
2. **비용 관리**: 토큰 사용량 모니터링 및 일일 한도 설정
3. **응답 시간**: BIF 사용자를 위해 3초 이내 응답 목표
4. **에러 메시지**: 기술적 오류도 사용자 친화적 메시지로 변환
5. **캐싱**: 자주 묻는 질문은 캐싱하여 응답 속도 향상