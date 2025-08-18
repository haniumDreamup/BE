# Spring Boot 3.5 Testing Best Practices and Solutions

## Common ApplicationContext Load Failures and Solutions

### 1. Root Causes of ApplicationContext Load Failures

The "Failed to load ApplicationContext" error is a wrapper error that typically masks underlying issues:

#### Common Causes:
- **Dependency Version Conflicts**: Incompatible library versions (e.g., Jackson 2.12 vs 2.14+ requirements)
- **Missing Bean Dependencies**: NoSuchBeanDefinitionException when Spring cannot find required beans
- **Multiple Bean Conflicts**: NoUniqueBeanDefinitionException when multiple beans of same type exist
- **Configuration Issues**: Missing @Configuration classes or incorrect component scanning
- **Test Annotation Problems**: Incorrect use of test slice annotations

#### Solutions:
```java
// Ensure proper test configuration
@SpringBootTest(
    classes = BifaiBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource("classpath:application-test.yml")
class ApplicationContextTest {
    // Test implementation
}

// For web layer testing only
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class ControllerTest {
    // Controller-specific tests
}
```

### 2. Spring Security 6 Test Configuration

#### Key Changes in Spring Boot 3.5:
- `@MockBean` is deprecated (removed in 4.0), use `@MockitoBean` instead
- Spring Security 6 auto-configuration changes
- New `@TestBean` annotation available (Spring 6.2+)

#### Proper Security Test Configuration:
```java
@WebMvcTest
@Import(TestSecurityConfig.class)
class SecurityControllerTest {
    
    @MockitoBean  // Use instead of @MockBean
    private UserService userService;
    
    @TestConfiguration
    static class TestSecurityConfig {
        
        @Bean
        @Primary
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
        }
    }
}

// Alternative approach using @TestBean (Spring 6.2+)
@SpringBootTest
class IntegrationTest {
    
    @TestBean  // New annotation for bean overriding
    UserService userService = Mockito.mock(UserService.class);
}
```

## @SpringBootTest vs @WebMvcTest Best Practices

### When to Use Each Annotation

#### @SpringBootTest
**Use for:**
- Integration testing across multiple layers
- Full application context testing
- End-to-end workflow validation
- Testing with real external dependencies

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource("classpath:application-test.yml")
class IntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    // Full application testing
}
```

#### @WebMvcTest
**Use for:**
- Isolated controller testing
- Fast unit tests with mocked dependencies
- Web layer-specific validation

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private UserService userService;
    
    // Controller-only testing with mocked services
}
```

### Performance Considerations
- `@WebMvcTest` is significantly faster (loads only web layer)
- `@SpringBootTest` loads full application context (slower but comprehensive)
- Use `@WebMvcTest` for unit tests, `@SpringBootTest` for integration tests

## WebSocket Test Configuration Issues

### Common WebSocket Testing Problems:

1. **@EnableWebSocket not loaded in tests**
2. **Server container compatibility (Jetty vs Tomcat/Undertow)**
3. **Missing WebSocket auto-configuration**

### Solutions:

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {WebSocketConfig.class, TestConfig.class}
)
@EnableAutoConfiguration  // Critical for WebSocket container bootstrapping
class WebSocketIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    private final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(1);
    
    @BeforeEach
    void setup() throws Exception {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        stompSession = stompClient.connect(
            "ws://localhost:" + port + "/websocket", 
            new StompSessionHandlerAdapter() {}
        ).get(5, SECONDS);
    }
    
    @Test
    void testWebSocketCommunication() throws Exception {
        stompSession.subscribe("/topic/messages", new DefaultStompFrameHandler());
        stompSession.send("/app/message", "Test message");
        
        String result = blockingQueue.poll(5, SECONDS);
        assertThat(result).isEqualTo("Test message");
    }
}

@TestConfiguration
@EnableWebSocket
static class TestConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TestWebSocketHandler(), "/websocket")
                .setAllowedOrigins("*");
    }
}
```

## Migration from Spring Boot 2.x to 3.5

### Critical Migration Points:

#### 1. Namespace Changes
```java
// Before (Spring Boot 2.x)
import javax.validation.Valid;
import javax.persistence.Entity;

// After (Spring Boot 3.5)
import jakarta.validation.Valid;
import jakarta.persistence.Entity;
```

#### 2. Testing Dependencies Update
```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mockito:mockito-core:5.3.1' // Updated version
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'org.testcontainers:localstack'
}
```

#### 3. TestRestTemplate Changes
```java
// TestRestTemplate now follows same redirect settings as RestTemplate
@Autowired
private TestRestTemplate restTemplate;

// Use withRedirects for custom redirect behavior
TestRestTemplate customTemplate = restTemplate.withRedirects(TestRestTemplate.HttpClientOption.ENABLE_REDIRECTS);
```

#### 4. Bean Name Changes
```java
// Before: TaskExecutor with 'taskExecutor' name
@Autowired
@Qualifier("taskExecutor")
private TaskExecutor taskExecutor;

// After: Only 'applicationTaskExecutor' name available
@Autowired
@Qualifier("applicationTaskExecutor")
private TaskExecutor applicationTaskExecutor;
```

## Mocking External Dependencies (Redis, S3)

### Redis Testing Strategies

#### Option 1: Testcontainers (Recommended)
```java
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {
    
    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void testRedisOperations() {
        redisTemplate.opsForValue().set("test:key", "value");
        String result = (String) redisTemplate.opsForValue().get("test:key");
        assertThat(result).isEqualTo("value");
    }
}
```

#### Option 2: Embedded Redis
```java
@TestConfiguration
public class EmbeddedRedisConfig {
    
    private RedisServer redisServer;
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 6370)
        );
    }
    
    @PostConstruct
    public void startRedis() {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }
    
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
```

#### Option 3: Mock Redis Operations
```java
@SpringBootTest
class RedisServiceTest {
    
    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockitoBean
    private ValueOperations<String, Object> valueOperations;
    
    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testCacheOperations() {
        when(valueOperations.get("test:key")).thenReturn("cached:value");
        
        String result = cacheService.getValue("test:key");
        
        assertThat(result).isEqualTo("cached:value");
        verify(valueOperations).get("test:key");
    }
}
```

### S3 Testing with LocalStack

#### Testcontainers + LocalStack Configuration
```java
@SpringBootTest
@Testcontainers
class S3IntegrationTest {
    
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEBUG", "1");
    
    @TestConfiguration
    static class TestConfig {
        
        @Bean
        @Primary
        public S3Client s3Client() {
            return S3Client.builder()
                    .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")))
                    .region(Region.US_EAST_1)
                    .forcePathStyle(true)
                    .build();
        }
    }
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private S3Client s3Client;
    
    @BeforeEach
    void setup() {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket("test-bucket")
                .build());
    }
    
    @Test
    void testS3Operations() {
        // Test S3 operations with LocalStack
        String key = "test-file.txt";
        String content = "Test content";
        
        s3Service.uploadFile("test-bucket", key, content.getBytes());
        
        byte[] downloaded = s3Service.downloadFile("test-bucket", key);
        assertThat(new String(downloaded)).isEqualTo(content);
    }
}
```

### Mock S3 Operations
```java
@SpringBootTest
class S3ServiceTest {
    
    @MockitoBean
    private S3Client s3Client;
    
    @Autowired
    private S3Service s3Service;
    
    @Test
    void testFileUpload() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        
        assertDoesNotThrow(() -> s3Service.uploadFile("bucket", "key", new byte[0]));
        
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
```

## Test Configuration Best Practices

### 1. Use @TestConfiguration for Test-Specific Beans
```java
@TestConfiguration
public class TestDatabaseConfig {
    
    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .addScript("test-data.sql")
                .build();
    }
}
```

### 2. Profile-Based Test Configuration
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  data:
    redis:
      host: localhost
      port: 6370
  
  cloud:
    aws:
      s3:
        endpoint: http://localhost:4566
        
logging:
  level:
    com.bifai.reminder: DEBUG
    org.springframework.security: DEBUG
```

### 3. Test Slicing Strategy
```java
// Repository layer testing
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    // JPA-specific tests
}

// Service layer testing
@SpringBootTest(classes = {UserService.class, TestConfig.class})
class UserServiceTest {
    // Service-specific tests with mocked dependencies
}

// Web layer testing
@WebMvcTest(UserController.class)
class UserControllerTest {
    // Controller-specific tests
}

// Full integration testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIntegrationTest {
    // End-to-end tests
}
```

## Troubleshooting Checklist

### ApplicationContext Load Failures:
1. ✅ Check dependency versions for compatibility
2. ✅ Verify all required beans are properly configured
3. ✅ Ensure test annotations are correctly applied
4. ✅ Check for circular dependencies
5. ✅ Validate component scanning configuration

### Security Configuration Issues:
1. ✅ Replace `@MockBean` with `@MockitoBean`
2. ✅ Use `@TestConfiguration` for security overrides
3. ✅ Import security configurations in test classes
4. ✅ Verify authentication/authorization test setup

### WebSocket Testing Issues:
1. ✅ Add `@EnableAutoConfiguration` to test classes
2. ✅ Use compatible server container (Tomcat/Undertow over Jetty)
3. ✅ Include WebSocket configurations in test context
4. ✅ Use `BlockingQueue` for asynchronous response handling

### External Dependency Mocking:
1. ✅ Use Testcontainers for integration testing
2. ✅ Configure LocalStack for AWS services
3. ✅ Use embedded servers for lightweight testing
4. ✅ Mock external clients with `@MockitoBean`

## Performance Optimization

### Test Execution Speed:
- Use `@WebMvcTest` for isolated controller testing
- Leverage `@DataJpaTest` for repository testing
- Implement test slicing to avoid full context loading
- Use embedded databases and in-memory storage
- Cache application contexts across test classes

### Resource Management:
- Use `@DirtiesContext` sparingly (slows down tests)
- Share test containers across test classes
- Properly clean up external resources
- Use test profiles to minimize loaded components

This document provides comprehensive solutions for common Spring Boot 3.5 testing issues, focusing on ApplicationContext load failures, Security configuration conflicts, and proper external dependency mocking strategies.