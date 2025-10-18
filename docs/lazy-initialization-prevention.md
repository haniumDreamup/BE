# LazyInitializationException 예방 Best Practices

## 문제 정의

JPA에서 `FetchType.LAZY`로 설정된 연관관계 필드에 접근할 때, Hibernate Session(트랜잭션)이 종료된 이후에 접근하면 `LazyInitializationException`이 발생합니다.

```java
// ❌ 문제 상황
public ScheduleResponse getSchedule(Long id) {
    Schedule schedule = scheduleRepository.findById(id).orElseThrow();
    // 트랜잭션 종료 후 DTO 변환 시도
    return ScheduleResponse.from(schedule); // selectedDays 접근 시 LazyInitializationException!
}
```

## Best Practice 해결 방법

### 1. Repository에서 @EntityGraph 사용 (가장 권장)

**장점:**
- 명시적으로 필요한 연관관계를 지정
- N+1 문제 완벽 해결
- 성능 최적화 가능

**단점:**
- Repository에 메서드 추가 필요

```java
// ✅ Repository
@EntityGraph(attributePaths = {"selectedDays"})
@Query("SELECT s FROM Schedule s WHERE s.id = :scheduleId")
Optional<Schedule> findByIdWithSelectedDays(@Param("scheduleId") Long scheduleId);

// ✅ Service
@Transactional(readOnly = true)
public ScheduleResponse getSchedule(Long scheduleId) {
    Schedule schedule = scheduleRepository.findByIdWithSelectedDays(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다"));
    return ScheduleResponse.from(schedule);
}
```

### 2. Service 메서드에 @Transactional 추가

**장점:**
- 간단한 구현
- 기존 코드 수정 최소화

**단점:**
- 트랜잭션 범위가 넓어져 성능 저하 가능
- 실수로 트랜잭션 누락 가능

```java
// ✅ Service
@Transactional(readOnly = true)
public ScheduleResponse getSchedule(Long scheduleId) {
    Schedule schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다"));
    // 트랜잭션 내부에서 selectedDays 초기화
    schedule.getSelectedDays().size(); // force initialization
    return ScheduleResponse.from(schedule);
}
```

### 3. DTO 변환 메서드를 트랜잭션 내부로 이동

**장점:**
- DTO 변환 로직 명확화
- 트랜잭션 범위 최적화

```java
// ✅ Service
@Transactional(readOnly = true)
public ScheduleResponse getSchedule(Long scheduleId) {
    return scheduleRepository.findById(scheduleId)
        .map(ScheduleResponse::from) // 트랜잭션 내부에서 변환
        .orElseThrow(() -> new ResourceNotFoundException("일정을 찾을 수 없습니다"));
}
```

## 프로젝트 적용 가이드

### 1단계: LazyInitializationException 발생 가능한 패턴 식별

다음 패턴들을 찾아서 수정:

1. **@Transactional 없이 컬렉션 접근**
   ```java
   // 검색 키워드: @OneToMany, @ManyToMany + .from( or .toDTO(
   ```

2. **트랜잭션 종료 후 DTO 변환**
   ```java
   Entity entity = repository.findById(id);
   return Response.from(entity); // ❌ 위험
   ```

### 2단계: 수정 우선순위

1. **High Priority**: 자주 호출되는 API 엔드포인트
   - `getSchedule()`, `getTodaySchedules()`
   - `getEmergency()`, `getActiveEmergencies()`
   - `getGuardiansByUserId()`

2. **Medium Priority**: 관리자/대시보드 API
   - 통계 조회
   - 이력 조회

3. **Low Priority**: 배치/스케줄러
   - 백그라운드 작업

### 3단계: 수정 가이드라인

```java
// Before (문제 있는 코드)
public SomeResponse getSomething(Long id) {
    SomeEntity entity = repository.findById(id).orElseThrow();
    return SomeResponse.from(entity); // LazyInitializationException 위험
}

// After (Option 1: @EntityGraph 사용 - 가장 권장)
// Repository
@EntityGraph(attributePaths = {"lazyCollection"})
Optional<SomeEntity> findByIdWithLazyCollection(Long id);

// Service
@Transactional(readOnly = true)
public SomeResponse getSomething(Long id) {
    SomeEntity entity = repository.findByIdWithLazyCollection(id).orElseThrow();
    return SomeResponse.from(entity);
}

// After (Option 2: @Transactional만 추가)
@Transactional(readOnly = true)
public SomeResponse getSomething(Long id) {
    return repository.findById(id)
        .map(SomeResponse::from)
        .orElseThrow();
}
```

## 주요 Entity별 주의사항

### Schedule Entity
- **Lazy 필드**: `selectedDays`, `user`
- **위험 메서드**: `getTodaySchedules()`, `getSchedule()`
- **해결**: `@EntityGraph(attributePaths = {"selectedDays"})`

### User Entity
- **Lazy 필드**: `guardians`, `schedules`, `devices`, 모든 컬렉션
- **위험 메서드**: 사용자 정보 조회 시 보호자 정보 함께 반환
- **해결**: 필요한 필드만 `@EntityGraph`로 페치

### Guardian Entity
- **Lazy 필드**: `user`, `guardianUser`
- **위험 메서드**: `findActiveGuardiansByUserId()`
- **해결**: `@EntityGraph(attributePaths = {"user", "guardianUser"})`

### Emergency Entity
- **Lazy 필드**: `user`
- **위험 메서드**: `getEmergencyStatus()`, `notifyGuardians()`
- **해결**: `@EntityGraph(attributePaths = {"user"})` 또는 `@Transactional`

## 테스트 가이드

### 1. LazyInitializationException 테스트

```java
@Test
void testLazyInitialization() {
    // Given
    Long scheduleId = 1L;

    // When
    ScheduleResponse response = scheduleService.getSchedule(scheduleId);

    // Then
    assertNotNull(response.getSelectedDays()); // LazyInitializationException 발생하면 안됨
}
```

### 2. N+1 문제 테스트

```java
@Test
void testNPlusOneQueryProblem() {
    // Given
    List<Long> userIds = Arrays.asList(1L, 2L, 3L);

    // When
    long startTime = System.currentTimeMillis();
    List<GuardianResponse> guardians = guardianService.getGuardiansByUserIds(userIds);
    long endTime = System.currentTimeMillis();

    // Then
    assertTrue(endTime - startTime < 500); // 500ms 이하여야 함
}
```

## 참고 자료

- [Hibernate Documentation - FetchType](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Spring Data JPA - @EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-graph)
- [Baeldung - LazyInitializationException](https://www.baeldung.com/hibernate-initialize-proxy-exception)
- [김영한 JPA 강의](https://www.inflearn.com/course/ORM-JPA-Basic)
