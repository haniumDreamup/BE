# JPA Entity Scan 문제 해결 가이드

## 문제 상황
`Not a managed type: class com.bifai.reminder.bifai_backend.entity.User` 오류 발생

## 문제 원인
1. JPA가 User 엔티티를 인식하지 못함
2. Spring Security 필터가 먼저 로드되면서 UserRepository가 필요한데, 이 시점에 Entity가 아직 스캔되지 않음
3. 순환 의존성 문제 가능성

## 해결 방법

### 1. @EntityScan 위치 확인
현재 `BifaiBackendApplication`에 다음과 같이 설정됨:
```java
@EntityScan(basePackages = "com.bifai.reminder.bifai_backend.entity")
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
```

### 2. Entity 클래스 확인
- `User.java`: `@Entity` 어노테이션 있음 ✓
- `BaseEntity.java`: `@MappedSuperclass` 있음 ✓
- `BaseTimeEntity.java`: `@MappedSuperclass` 있음 ✓

### 3. 가능한 해결책

#### 방법 1: JPA 설정 분리
JpaConfig에서 EntityScan을 제거하고 Application 클래스에서만 관리

#### 방법 2: Lazy Initialization
Spring Security 빈들을 Lazy로 초기화

#### 방법 3: 프로파일 분리
Security 없이 JPA만 테스트하는 프로파일 생성

## 현재 시도한 방법들
1. H2 데이터베이스 사용 - 여전히 동일한 문제
2. 다양한 프로파일 설정 - 동일한 문제
3. EntityScan 위치 변경 - 부분적 해결

## 권장 해결책
1. Spring Security를 임시로 비활성화하고 JPA 먼저 동작 확인
2. 동작 확인 후 Security 단계적 활성화
3. 필요시 Security 설정을 @Lazy로 변경