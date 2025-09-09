# Spring Boot 3 Testing Best Practices (2024)

## 베스트 프랙티스 조사 결과

### 1. 테스트 전략 계층 구조

**우선순위**:
1. **Unit Tests**: Spring Boot 객체가 영향을 받지 않는 경우 단위 테스트 사용
2. **Test Slices**: 필요한 부분만 로드하여 통합 테스트
3. **@SpringBootTest**: 더 큰 응용 프로그램 부분을 테스트할 때 사용

### 2. Spring Boot 3 필수 의존성

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <version>3.5.0</version>
</dependency>
```

### 3. 엔드포인트 테스트용 Test Slice 어노테이션

#### @WebMvcTest (컨트롤러 테스트)
- Controller와 HTTP 계층 간 통합 테스트
- Spring MVC 인프라만 자동 구성
- `@MockBean`으로 비즈니스 로직 모킹

```java
@WebMvcTest(UserController.class)
@ExtendWith(SpringExtension.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk());
    }
}
```

#### @SpringBootTest (전체 통합 테스트)
- 전체 애플리케이션 컨텍스트 초기화
- TestRestTemplate 자동 주입 가능

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
class IntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testEndToEnd() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 4. Spring Boot 3 + Java 21 현대적 설정

#### JUnit 5 마이그레이션
- `@RunWith` → `@ExtendWith` 
- `@ExtendWith(SpringExtension.class)` 사용

#### 현대적 테스트 구조
```java
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ModernIntegrationTest {
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 동적 속성 설정
    }
}
```

### 5. 데이터베이스 통합 테스트

#### Testcontainers 사용
```java
@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### 6. 성능 최적화

#### ApplicationContext 재사용
- 테스트 간 애플리케이션 컨텍스트 재사용으로 성능 향상
- `@DirtiesContext` 신중하게 사용

#### Test Slicing 활용
- `@WebMvcTest`, `@DataJpaTest` 등으로 필요한 부분만 테스트

### 7. 엔드포인트 검증 패턴

#### 성공 케이스
```java
@Test
void shouldReturnUser_WhenValidId() {
    // given
    Long userId = 1L;
    
    // when & then
    mockMvc.perform(get("/api/users/{id}", userId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(userId))
           .andExpect(jsonPath("$.success").value(true));
}
```

#### 실패 케이스
```java
@Test
void shouldReturn404_WhenUserNotFound() {
    // given
    Long nonExistentId = 999L;
    
    // when & then
    mockMvc.perform(get("/api/users/{id}", nonExistentId))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.success").value(false))
           .andExpect(jsonPath("$.error.code").exists());
}
```

#### 엣지 케이스
```java
@Test
void shouldReturn400_WhenInvalidInput() {
    // given
    String invalidJson = "{ \"name\": \"\" }";
    
    // when & then
    mockMvc.perform(post("/api/users")
           .contentType(MediaType.APPLICATION_JSON)
           .content(invalidJson))
           .andExpected(status().isBadRequest())
           .andExpect(jsonPath("$.error.message").exists());
}
```

### 8. 보안 테스트 통합

#### JWT 기반 인증 테스트
```java
@Test
@WithMockUser(roles = "USER")
void shouldAllowAccess_WhenAuthenticated() {
    mockMvc.perform(get("/api/protected-endpoint"))
           .andExpect(status().isOk());
}

@Test
void shouldDenyAccess_WhenNotAuthenticated() {
    mockMvc.perform(get("/api/protected-endpoint"))
           .andExpect(status().isUnauthorized());
}
```

### 9. 테스트 데이터 관리

#### @Sql 어노테이션 활용
```java
@Test
@Sql(scripts = "/data/test-users.sql")
void shouldFindUser_WhenDataExists() {
    // 테스트 로직
}
```

### 10. 주요 검증 포인트

#### 필수 검증 사항
1. **HTTP 상태 코드** 정확성
2. **응답 구조** (success, data, error 필드)
3. **에러 메시지** BIF 친화적 언어 (5학년 수준)
4. **인증/인가** 제대로 작동
5. **입력 검증** 적절한 에러 처리
6. **데이터 일관성** 실제 데이터베이스 상태

#### 성능 검증
- 3초 이내 응답 시간
- 100+ 동시 사용자 지원
- 메모리 사용량 모니터링

### 11. CI/CD 통합

#### Gradle 테스트 실행
```bash
# 모든 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests "*IntegrationTest"

# 빌드 제외 테스트
./gradlew build -x test
```

### 12. 모니터링 및 리포팅

#### 테스트 커버리지
- 최소 80% 단위 테스트 커버리지
- 모든 API 엔드포인트 통합 테스트

#### 지속적인 개선
- 테스트 실패 패턴 분석
- 성능 회귀 감지
- 보안 취약점 테스트