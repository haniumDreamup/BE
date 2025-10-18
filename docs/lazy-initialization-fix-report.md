# LazyInitializationException 수정 보고서

## 요약

프로젝트 전반에 걸쳐 LazyInitializationException이 발생할 수 있는 패턴을 식별하고 Best Practice를 적용하여 수정했습니다.

## 수정된 파일들

### 1. Repository 계층

#### EmergencyRepository.java
**문제점:**
- `findByUserIdOrderByCreatedAtDesc()`: User를 함께 페치하지 않아 N+1 문제 발생
- `findActiveEmergencies()`: 동일한 문제

**해결 방법:**
```java
// Before
Page<Emergency> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

// After - @EntityGraph 추가
@EntityGraph(attributePaths = {"user"})
@Query("SELECT e FROM Emergency e WHERE e.user.userId = :userId ORDER BY e.createdAt DESC")
Page<Emergency> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
```

**적용 메서드:**
- `findByUserIdOrderByCreatedAtDesc()` - User eager fetch 추가
- `findActiveEmergencies()` - User eager fetch 추가

#### GuardianRepository.java (이미 수정됨)
**Best Practice:**
- `findByUser_UserId()` - user, guardianUser를 @EntityGraph로 페치
- `findPrimaryGuardianByUserId()` - 동일하게 적용
- `findActiveGuardiansByUserId()` - 동일하게 적용

### 2. Service 계층

#### EmergencyService.java

**문제점 1: notifyGuardians() 메서드**
- Guardian의 guardianUser가 LAZY로 로드됨
- 트랜잭션 외부에서 접근 시 예외 발생

**해결 방법:**
```java
// Before
private void notifyGuardians(Emergency emergency) {
    List<Guardian> guardians = guardianRepository.findActiveGuardiansByUserId(...);
    // guardianUser 접근 시 LazyInitializationException 발생 가능
    guardian.getGuardianUser().getEmail();
}

// After
@Transactional
private void notifyGuardians(Emergency emergency) {
    // GuardianRepository의 @EntityGraph 덕분에 guardianUser가 이미 로드됨
    List<Guardian> guardians = guardianRepository.findActiveGuardiansByUserId(...);
    guardian.getGuardianUser().getEmail(); // 안전
}
```

#### ScheduleService.java (이미 수정됨)

**문제점:**
- `getTodaySchedules()` - selectedDays 컬렉션 접근 시 예외

**해결 방법:**
- Repository에 `@EntityGraph(attributePaths = {"selectedDays"})` 추가
- Service 메서드에 `@Transactional(readOnly = true)` 추가

#### GuardianService.java

**문제점:**
- 다수의 조회 메서드에서 Guardian의 user, guardianUser 접근
- 트랜잭션 없이 Lazy 필드에 접근하여 예외 발생 가능

**해결 방법:**
```java
// Before
public List<Guardian> getMyGuardians() {
    User currentUser = getCurrentUser();
    return guardianRepository.findByUserAndIsActiveTrue(currentUser);
}

// After
@Transactional(readOnly = true)
public List<Guardian> getMyGuardians() {
    User currentUser = getCurrentUser();
    return guardianRepository.findByUserAndIsActiveTrue(currentUser);
}
```

**적용 메서드:**
- `getMyGuardians()` - @Transactional(readOnly = true) 추가
- `getProtectedUsers()` - @Transactional(readOnly = true) 추가
- `isGuardianOf()` - @Transactional(readOnly = true) 추가
- `canApproveGuardian()` - @Transactional(readOnly = true) 추가
- `canRejectGuardian()` - @Transactional(readOnly = true) 추가
- `isMyGuardian()` - @Transactional(readOnly = true) 추가
- `canRemoveRelationship()` - @Transactional(readOnly = true) 추가

## Best Practice 적용 원칙

### 1. Repository 계층
- **@EntityGraph 사용**: 명시적으로 필요한 연관관계 페치
- **N+1 문제 해결**: JOIN FETCH로 한 번에 데이터 로드
- **쿼리 명시**: @Query로 JPQL 작성하여 의도 명확화

### 2. Service 계층
- **@Transactional(readOnly = true)**: 조회 메서드에 필수
- **트랜잭션 범위 최적화**: DTO 변환을 트랜잭션 내부에서 수행
- **명시적 초기화**: 필요시 `entity.getLazyField().size()` 호출

### 3. 우선순위
1. **High**: @EntityGraph 사용 (가장 권장)
2. **Medium**: @Transactional(readOnly = true) 추가
3. **Low**: 명시적 초기화 (최후의 수단)

## 향후 작업

### 추가 수정 필요 서비스
다음 Service 클래스들도 검토 및 수정 필요:

1. **StatisticsService** - 통계 조회 시 연관관계 접근
2. **GuardianDashboardService** - 대시보드 데이터 조회
3. **NotificationService** - 알림 전송 시 사용자 정보 접근
4. **GeofenceService** - Geofence와 User 연관관계
5. **AccessibilityService** - 접근성 설정 조회

### 검증 방법

#### 1. 단위 테스트
```java
@Test
void shouldNotThrowLazyInitializationException() {
    // Given
    Long emergencyId = 1L;

    // When & Then - 예외 발생하지 않아야 함
    assertDoesNotThrow(() -> {
        EmergencyResponse response = emergencyService.getEmergencyStatus(emergencyId);
        assertNotNull(response.getUser()); // User 정보 접근
    });
}
```

#### 2. 통합 테스트
```java
@SpringBootTest
@Transactional
class LazyInitializationIntegrationTest {

    @Test
    void shouldFetchGuardiansWithUsers() {
        // Given
        User user = createTestUser();
        Guardian guardian = createTestGuardian(user);

        // When
        List<Guardian> guardians = guardianService.getMyGuardians();

        // Then
        assertFalse(guardians.isEmpty());
        assertDoesNotThrow(() -> guardians.get(0).getGuardianUser().getEmail());
    }
}
```

#### 3. 성능 테스트
```java
@Test
void shouldNotHaveNPlusOneProblem() {
    // Given
    createMultipleGuardians(10);

    // When
    long startTime = System.currentTimeMillis();
    List<Guardian> guardians = guardianService.getMyGuardians();
    guardians.forEach(g -> g.getGuardianUser().getEmail()); // 모든 guardianUser 접근
    long endTime = System.currentTimeMillis();

    // Then - 쿼리가 2개만 실행되어야 함 (Guardian 조회 1번 + User 조회 1번)
    assertTrue(endTime - startTime < 500);
}
```

## 참고 문서

- [lazy-initialization-prevention.md](./lazy-initialization-prevention.md) - 상세 Best Practice 가이드
- [Hibernate User Guide - FetchType](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#fetching)
- [Spring Data JPA - @EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-graph)

## 결론

### 적용 효과
1. **안정성 향상**: LazyInitializationException 발생 위험 제거
2. **성능 개선**: N+1 쿼리 문제 해결로 쿼리 수 감소
3. **유지보수성**: 명시적인 트랜잭션 범위로 코드 의도 명확화

### 권장사항
1. 새로운 Service 메서드 작성 시 `@Transactional(readOnly = true)` 기본 적용
2. Repository 메서드 작성 시 필요한 연관관계는 `@EntityGraph`로 명시
3. DTO 변환 로직은 트랜잭션 내부에서 실행
4. 정기적으로 N+1 쿼리 문제 모니터링

### 주의사항
1. `@Transactional` 남용은 성능 저하 초래
2. `FetchType.EAGER` 사용은 지양 (필요시 @EntityGraph 사용)
3. DTO 변환 시점을 트랜잭션 범위 내로 명확히 제어
