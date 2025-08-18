# Spring Boot JPA 테스트 베스트 프랙티스 연구

## 문제 분석

### 1. Foreign Key 제약조건 위반 원인
- **근본 원인**: 테스트에서 자식 엔티티(Device)를 저장할 때 부모 엔티티(User)가 먼저 저장되지 않음
- **H2 특성**: H2는 MySQL과 달리 FK 제약조건을 더 엄격하게 검사
- **트랜잭션 롤백**: 각 테스트 후 롤백되어 데이터가 유지되지 않음

### 2. Bean 의존성 문제
- JWT Secret Key 길이 부족 (64바이트 미만)
- Redis 의존성 없이 실행 시 Bean 생성 실패
- 테스트 프로파일 설정 불일치

## 베스트 프랙티스 (Context7 & Web 조사 결과)

### 1. 테스트 데이터 설정 전략

#### 1.1 TestEntityManager 사용 (권장)
```java
@DataJpaTest
class RepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @BeforeEach
    void setUp() {
        // 부모 엔티티 먼저 저장
        User user = entityManager.persistAndFlush(createUser());
        // 자식 엔티티 저장
        Device device = createDevice(user);
        entityManager.persistAndFlush(device);
    }
}
```

#### 1.2 @Sql 스크립트 사용
```java
@Sql(scripts = "/test-data.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", 
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
```

#### 1.3 @DirtiesContext 사용
```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

### 2. H2 데이터베이스 설정

#### 2.1 최적 설정
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE
    
  jpa:
    defer-datasource-initialization: true  # 중요!
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        globally_quoted_identifiers: true
```

#### 2.2 FK 제약조건 비활성화 (테스트용)
```sql
SET REFERENTIAL_INTEGRITY FALSE;
-- 테스트 데이터 삽입
SET REFERENTIAL_INTEGRITY TRUE;
```

### 3. 트랜잭션 관리

#### 3.1 트랜잭션 롤백 비활성화
```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Rollback(false)
```

#### 3.2 TestTransaction 사용
```java
@Test
void testWithManualTransaction() {
    TestTransaction.flagForCommit();
    // 테스트 코드
    TestTransaction.end();
}
```

### 4. 테스트 격리

#### 4.1 독립적인 테스트 데이터
```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderedTest {
    @Test
    @Order(1)
    void createParentData() { }
    
    @Test
    @Order(2)
    void createChildData() { }
}
```

#### 4.2 TestContainers 사용 (실제 DB)
```java
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RealDatabaseTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
}
```

### 5. Mock 설정 최적화

#### 5.1 공통 Mock 설정
```java
@TestConfiguration
public class TestMockConfig {
    @Bean
    @Primary
    public RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
}
```

#### 5.2 @MockBean vs @Mock
- `@MockBean`: Spring 컨텍스트의 Bean을 Mock으로 대체
- `@Mock`: 단순 Mock 객체 생성

## 권장 솔루션

### 1. TestDataBuilder 패턴
```java
public class TestDataBuilder {
    public static User createUserWithDefaults() {
        return User.builder()
            .username("test_" + UUID.randomUUID())
            .email("test@example.com")
            .passwordHash("hashed")
            .isActive(true)
            .build();
    }
}
```

### 2. 계층적 데이터 설정
```java
@DataJpaTest
class RepositoryTest {
    @PersistenceContext
    private EntityManager em;
    
    @BeforeEach
    void setUp() {
        // 1. Role 생성
        Role role = createRole();
        em.persist(role);
        
        // 2. User 생성 (Role 참조)
        User user = createUser(role);
        em.persist(user);
        
        // 3. Device 생성 (User 참조)
        Device device = createDevice(user);
        em.persist(device);
        
        em.flush(); // 강제 동기화
        em.clear(); // 1차 캐시 비우기
    }
}
```

### 3. 프로파일 전략
```yaml
# application-test.yml
spring:
  profiles:
    active: test
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

## 구현 우선순위

1. **즉시 적용**
   - TestEntityManager 사용
   - defer-datasource-initialization 설정
   - JWT Secret 64바이트 이상 설정

2. **단계적 개선**
   - TestDataBuilder 클래스 생성
   - @Sql 스크립트 작성
   - 공통 Mock 설정 클래스

3. **장기 개선**
   - TestContainers 도입
   - 통합 테스트 분리
   - CI/CD 파이프라인 최적화

## 참고 자료
- Spring Boot 공식 문서: Testing
- Baeldung: Spring Boot @DataJpaTest
- DZone: Solving Foreign Key Problems in DBUnit