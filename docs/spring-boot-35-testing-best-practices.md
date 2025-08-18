# Spring Boot 3.5 Testing Best Practices

## 주요 변경사항

### 1. @MockBean → @MockitoBean 마이그레이션
- Spring Boot 3.2+부터 @MockBean이 deprecated
- Spring Boot 3.5에서는 @MockitoBean 사용 권장
- 패키지: `org.springframework.test.context.bean.override.mockito.MockitoBean`

### 2. @WebMvcTest 베스트 프랙티스
```java
// 기존 (Spring Boot 3.1 이하)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockBean
    private UserService userService;
}

// 변경 (Spring Boot 3.5)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockitoBean  // 새로운 어노테이션
    private UserService userService;
}
```

### 3. 장점
- 더 빠른 테스트 실행 (Spring Context 재로딩 불필요)
- JUnit 5 및 Mockito와 더 나은 통합
- 다른 테스트에 영향 없음 (글로벌 모킹 제거)

## 우리 프로젝트 현황

### 문제점 분석
1. **SecurityConfig 충돌**: `@Profile("!simple")` 때문에 테스트에서 로드 안됨
2. **@MockBean 사용**: 6개 테스트 파일에서 deprecated 어노테이션 사용
3. **WebSocket 테스트 실패**: 인증 문제로 29/45 실패
4. **Controller 테스트 실패**: MockMvc와 Security 설정 충돌

### 해결 전략

#### Phase 1: Controller 테스트 수정
1. @MockBean → @MockitoBean 교체
2. Security 설정 단순화
3. @WithMockUser 또는 단위 테스트로 전환

#### Phase 2: WebSocket 테스트 수정
1. WebSocket 인증 처리 개선
2. 비동기 테스트를 동기식으로 변경
3. MockitoBean 적용

#### Phase 3: 통합 테스트 개선
1. TestContainers 활용
2. @SpringBootTest 최적화
3. 프로파일 기반 테스트 분리

## 구체적 수정 사항

### BaseControllerTest.java
```java
// 변경 전
import org.springframework.boot.test.mock.mockito.MockBean;

// 변경 후
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```

### SimpleWebSocketTest.java
```java
// 변경 전
@MockBean
private WebSocketService webSocketService;

// 변경 후
@MockitoBean
private WebSocketService webSocketService;
```

## 테스트 실행 전략

### 단위 테스트 (빠름)
```bash
./gradlew test --tests "*UnitTest"
```

### 통합 테스트 (느림)
```bash
./gradlew test --tests "*IntegrationTest"
```

### 전체 테스트
```bash
./gradlew test
```