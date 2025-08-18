# Redis 테스트 전략 가이드

## 현재 선택: Mocking 우선

### 결정 이유
1. **즉시 해결 가능**: 현재 실패 중인 75개 테스트 빠른 수정
2. **개발 속도**: 테스트 실행 시간 50초 유지
3. **CI/CD 단순성**: Docker 의존성 없이 실행
4. **점진적 개선 가능**: 나중에 TestContainers 추가 용이

## 구현 방법

### 1. 기본 Mock 설정 (현재 적용)
```yaml
# application-test.yml
redis:
  mock:
    enabled: true  # Mock 사용
```

### 2. 테스트 실행
```bash
# 단위 테스트 (Mock Redis)
./gradlew test

# 통합 테스트 (향후 Docker Redis)
./gradlew test -Dspring.profiles.active=test-integration
```

## 향후 TestContainers 도입 시점

### 도입이 필요한 경우
- Redis 트랜잭션 테스트
- TTL/Expire 동작 검증
- Pub/Sub 기능 테스트
- 성능 테스트

### 도입 방법
```groovy
// build.gradle
testImplementation 'org.testcontainers:testcontainers:1.19.0'
testImplementation 'org.testcontainers:junit-jupiter:1.19.0'
testImplementation 'com.redis:testcontainers-redis:1.7.0'
```

```java
@TestContainers
@SpringBootTest
class RedisIntegrationTest {
  @Container
  static RedisContainer redis = new RedisContainer("redis:7-alpine");
}
```

## 테스트 분류

### Mock 사용 (빠름)
- ✅ Controller 테스트
- ✅ Service 단위 테스트
- ✅ WebSocket 메시지 테스트
- ✅ CI/CD 파이프라인

### Docker Redis 사용 (정확)
- ✅ 캐시 만료 테스트
- ✅ 동시성 테스트
- ✅ 성능 테스트
- ✅ 배포 전 통합 테스트

## 성능 비교

| 방식 | 테스트 시간 | 정확도 | 환경 요구사항 |
|------|------------|--------|--------------|
| Mock | ~50초 | 80% | 없음 |
| TestContainers | ~90초 | 100% | Docker |
| Embedded Redis | ~60초 | 90% | 없음 (deprecated) |

## 결론

**현재는 Mock을 사용하여 테스트 안정화에 집중**하고, 
프로젝트가 성숙한 후 **중요 기능에 대해서만 TestContainers를 선택적으로 도입**하는 것이 최적입니다.