# LazyInitializationException 방지 체크리스트

## 개발 시 체크리스트

### Entity 작성 시
- [ ] `@OneToMany`, `@ManyToMany` 관계는 기본적으로 `FetchType.LAZY` 사용
- [ ] `@ManyToOne`, `@OneToOne` 관계도 `FetchType.LAZY` 명시 (성능 최적화)
- [ ] 양방향 연관관계 설정 시 `@ToString(exclude = {...})` 추가하여 순환 참조 방지
- [ ] `@JsonIgnore` 추가하여 직렬화 시 순환 참조 방지

```java
// ✅ Good
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
@JsonIgnore
@ToString.Exclude
private List<Schedule> schedules;

// ❌ Bad
@OneToMany(mappedBy = "user", fetch = FetchType.EAGER) // EAGER는 성능 저하
private List<Schedule> schedules;
```

### Repository 작성 시
- [ ] 연관관계를 함께 조회해야 하는 경우 `@EntityGraph` 사용
- [ ] `@Query`로 JPQL 작성 시 필요한 연관관계는 `JOIN FETCH` 명시
- [ ] N+1 문제가 발생할 가능성이 있는 메서드는 `@EntityGraph` 필수

```java
// ✅ Good - @EntityGraph 사용
@EntityGraph(attributePaths = {"user", "guardianUser"})
@Query("SELECT g FROM Guardian g WHERE g.user.userId = :userId")
List<Guardian> findByUserId(@Param("userId") Long userId);

// ✅ Good - JOIN FETCH 사용
@Query("SELECT s FROM Schedule s JOIN FETCH s.selectedDays WHERE s.id = :id")
Optional<Schedule> findByIdWithSelectedDays(@Param("id") Long id);

// ❌ Bad - Lazy 필드를 페치하지 않음
List<Guardian> findByUserId(Long userId);
```

### Service 작성 시
- [ ] 조회 메서드는 반드시 `@Transactional(readOnly = true)` 추가
- [ ] DTO 변환 로직을 트랜잭션 범위 내에서 실행
- [ ] 컬렉션 접근이 필요한 경우 트랜잭션 내부에서 초기화
- [ ] private 메서드도 Lazy 필드 접근 시 `@Transactional` 추가

```java
// ✅ Good
@Transactional(readOnly = true)
public ScheduleResponse getSchedule(Long id) {
    return scheduleRepository.findByIdWithSelectedDays(id)
        .map(ScheduleResponse::from) // 트랜잭션 내부에서 변환
        .orElseThrow();
}

// ❌ Bad - 트랜잭션 없음
public ScheduleResponse getSchedule(Long id) {
    Schedule schedule = scheduleRepository.findById(id).orElseThrow();
    return ScheduleResponse.from(schedule); // LazyInitializationException!
}

// ❌ Bad - 트랜잭션 외부에서 변환
@Transactional(readOnly = true)
public Schedule getSchedule(Long id) {
    return scheduleRepository.findById(id).orElseThrow();
}
// Controller에서 ScheduleResponse.from(schedule) 호출 시 예외!
```

### DTO 작성 시
- [ ] `from()` 메서드에서 Lazy 필드 접근 시 주의
- [ ] Builder 패턴 사용 시 null 체크 추가
- [ ] 컬렉션 변환 시 stream 연산 사용

```java
// ✅ Good - Lazy 필드 안전하게 접근
public static ScheduleResponse from(Schedule schedule) {
    return ScheduleResponse.builder()
        .id(schedule.getId())
        .title(schedule.getTitle())
        // selectedDays는 트랜잭션 내부에서 초기화되어야 함
        .selectedDays(schedule.getSelectedDays() != null
            ? schedule.getSelectedDays().stream()
                .map(SelectedDay::getDayOfWeek)
                .collect(Collectors.toList())
            : Collections.emptyList())
        .build();
}
```

### Controller 작성 시
- [ ] Service에서 Entity를 직접 반환하지 않도록 확인
- [ ] DTO 변환은 Service 계층에서 처리
- [ ] ResponseEntity 생성 전에 모든 데이터가 초기화되었는지 확인

```java
// ✅ Good - Service에서 DTO 반환
@GetMapping("/{id}")
public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable Long id) {
    ScheduleResponse response = scheduleService.getSchedule(id);
    return ResponseEntity.ok(response);
}

// ❌ Bad - Entity 직접 반환
@GetMapping("/{id}")
public ResponseEntity<Schedule> getSchedule(@PathVariable Long id) {
    Schedule schedule = scheduleService.getSchedule(id);
    return ResponseEntity.ok(schedule); // LazyInitializationException!
}
```

## 코드 리뷰 체크리스트

### Pull Request 작성자
- [ ] 새로운 Repository 메서드에 `@EntityGraph` 또는 `JOIN FETCH` 적용 확인
- [ ] 새로운 Service 메서드에 `@Transactional` 적용 확인
- [ ] DTO 변환 로직이 트랜잭션 내부에 있는지 확인
- [ ] 테스트 코드에서 LazyInitializationException 발생하지 않는지 확인

### 리뷰어
- [ ] Repository 메서드가 연관관계를 적절히 페치하는지 확인
- [ ] Service 메서드에 적절한 트랜잭션 범위가 설정되었는지 확인
- [ ] DTO 변환 시점이 트랜잭션 내부인지 확인
- [ ] N+1 쿼리 문제가 발생할 가능성이 있는지 확인

## 테스트 체크리스트

### 단위 테스트
- [ ] Repository 테스트에서 `@DataJpaTest` 사용
- [ ] Service 테스트에서 `@SpringBootTest` 또는 `@Transactional` 사용
- [ ] Lazy 필드 접근 시 예외가 발생하지 않는지 검증

```java
@Test
void shouldNotThrowLazyInitializationException() {
    // Given
    Long scheduleId = 1L;

    // When
    ScheduleResponse response = scheduleService.getSchedule(scheduleId);

    // Then - selectedDays 접근 시 예외 발생하지 않음
    assertNotNull(response.getSelectedDays());
    assertDoesNotThrow(() -> response.getSelectedDays().size());
}
```

### 통합 테스트
- [ ] API 엔드포인트 호출 시 LazyInitializationException 발생하지 않는지 확인
- [ ] 트랜잭션 경계를 넘어 Entity 사용 시 테스트
- [ ] 대용량 데이터 조회 시 성능 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleControllerIntegrationTest {

    @Test
    void shouldReturnScheduleWithSelectedDays() throws Exception {
        // Given
        Long scheduleId = 1L;

        // When & Then
        mockMvc.perform(get("/api/v1/schedules/{id}", scheduleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedDays").isArray())
            .andExpect(jsonPath("$.selectedDays").isNotEmpty());
    }
}
```

### 성능 테스트
- [ ] N+1 쿼리 문제 확인
- [ ] 대량 데이터 조회 시 응답 시간 측정
- [ ] 쿼리 실행 횟수 모니터링

```java
@Test
void shouldNotHaveNPlusOneProblem() {
    // Given
    createMultipleSchedules(100);

    // When
    long startTime = System.currentTimeMillis();
    List<ScheduleResponse> schedules = scheduleService.getAllSchedules();
    schedules.forEach(s -> s.getSelectedDays().size()); // 모든 selectedDays 접근
    long endTime = System.currentTimeMillis();

    // Then - 응답 시간이 500ms 이하
    assertTrue(endTime - startTime < 500);
    // 쿼리 실행 횟수 확인 (Spring Boot Actuator 또는 p6spy 사용)
}
```

## 자주 발생하는 실수

### 1. 트랜잭션 외부에서 DTO 변환
```java
// ❌ Bad
@Transactional(readOnly = true)
public Schedule getSchedule(Long id) {
    return scheduleRepository.findById(id).orElseThrow();
}
// Controller에서 ScheduleResponse.from(schedule) 호출 시 예외!
```

### 2. @EntityGraph 없이 연관관계 조회
```java
// ❌ Bad
List<Guardian> findByUserId(Long userId);
// Guardian의 user, guardianUser에 접근 시 LazyInitializationException!
```

### 3. Stream 연산에서 Lazy 필드 접근
```java
// ❌ Bad
public List<String> getGuardianEmails(Long userId) {
    List<Guardian> guardians = guardianRepository.findByUserId(userId);
    return guardians.stream()
        .map(g -> g.getGuardianUser().getEmail()) // LazyInitializationException!
        .collect(Collectors.toList());
}
```

### 4. private 메서드에 트랜잭션 누락
```java
// ❌ Bad
private void notifyGuardians(Emergency emergency) {
    List<Guardian> guardians = guardianRepository.findActiveGuardiansByUserId(...);
    guardians.forEach(g -> sendNotification(g.getGuardianUser())); // LazyInitializationException!
}
```

## 모니터링 및 디버깅

### 1. 로그 설정
```yaml
# application-dev.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.transaction: DEBUG
```

### 2. p6spy 설정 (쿼리 모니터링)
```yaml
# spy.properties
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime) | %(executionTime) ms | %(category) | %(sqlSingleLine)
```

### 3. 디버깅 팁
- `PersistenceUnitUtil.isLoaded(entity, "field")` - 필드 초기화 여부 확인
- Hibernate 로그에서 `LazyInitializationException` 검색
- 트랜잭션 경계 확인: `@Transactional` 어노테이션 위치 점검

## 참고 자료

- [lazy-initialization-prevention.md](./lazy-initialization-prevention.md) - Best Practice 가이드
- [lazy-initialization-fix-report.md](./lazy-initialization-fix-report.md) - 수정 보고서
- [Hibernate Documentation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
