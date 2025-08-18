# 테스트 실패 원인 분석

## WebSocket 및 OAuth2 테스트 실패 원인

### 1. 주요 원인: Redis 의존성 문제

**문제점:**
- PoseDataService가 RedisTemplate<String, Object>를 필수로 요구
- TestRedisConfiguration이 @Profile("test")로 설정되어 있지만 제대로 로드되지 않음
- Spring Boot의 자동 설정과 충돌 발생

**발생 이유:**
```java
// PoseDataService.java
@RequiredArgsConstructor
public class PoseDataService {
  private final RedisTemplate<String, Object> redisTemplate; // 필수 의존성
}
```

### 2. Spring Context 로딩 문제

**WebSocket 테스트:**
- @SpringBootTest가 전체 컨텍스트를 로드하려 하지만 Redis 설정 누락
- WebSocketController → WebSocketService → 다른 서비스들이 Redis 의존

**OAuth2 테스트:**
- CustomOAuth2UserService가 전체 Security 설정을 요구
- Security 설정이 다시 Redis(RefreshTokenService)를 요구

### 3. 설정 충돌

**현재 구조의 문제:**
1. `@Profile("test")`와 `@ActiveProfiles("test")`가 일치하지만
2. `@Import`로 명시적으로 추가해도 Bean 생성 우선순위 문제 발생
3. Mock Bean과 실제 Bean 간의 충돌

## 해결 방안

### 방안 1: @ConditionalOnMissingBean 활용
```java
@TestConfiguration
@Profile("test")
public class TestRedisConfig {
  @Bean
  @Primary
  @ConditionalOnMissingBean(RedisTemplate.class)
  public RedisTemplate<String, Object> redisTemplate() {
    return mock(RedisTemplate.class);
  }
}
```

### 방안 2: Service에 Optional 의존성 사용
```java
public class PoseDataService {
  private final Optional<RedisTemplate<String, Object>> redisTemplate;
  
  // Redis가 없으면 인메모리 캐시 사용
  private Map<String, Object> memoryCache = new HashMap<>();
}
```

### 방안 3: 테스트 프로파일 분리
```yaml
# application-test.yml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

# application-test-with-redis.yml  
spring:
  redis:
    host: localhost
    port: 6379
```

### 방안 4: 조건부 Bean 생성
```java
@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class PoseDataService {
  // Redis 사용 버전
}

@Service
@Profile("test")
@ConditionalOnProperty(name = "redis.enabled", havingValue = "false")
public class MockPoseDataService implements PoseDataServiceInterface {
  // Mock 버전
}
```

## 권장 해결책

**단기 (즉시 적용 가능):**
1. PoseDataService를 인터페이스로 분리
2. 테스트용 Mock 구현체 제공
3. @Primary로 우선순위 지정

**장기 (리팩토링 필요):**
1. Redis 의존성을 선택적으로 변경
2. 캐시 추상화 레이어 도입
3. 테스트 컨테이너 사용 (실제 Redis)