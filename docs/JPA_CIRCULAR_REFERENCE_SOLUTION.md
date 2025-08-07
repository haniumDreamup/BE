# JPA 순환 참조 문제 해결 방안

## 문제 상황
Spring Boot 3.5.0에서 User와 Guardian 엔티티 간 양방향 연관관계로 인한 순환 참조 발생
- 오류: "Not a managed type: class com.bifai.reminder.bifai_backend.entity.User"

## 순환 참조 구조
```
User ↔ Guardian (양방향)
├── User.guardians → Guardian.user
└── User.guardianFor → Guardian.guardianUser
```

## 해결 방안

### 1. Lazy Loading 적용 (이미 적용됨)
```java
@ManyToOne(fetch = FetchType.LAZY)
```

### 2. @ToString 순환 참조 방지
```java
@ToString(exclude = {"user", "guardianUser"})
```

### 3. @JsonManagedReference/@JsonBackReference 사용
```java
// User.java
@JsonManagedReference("user-guardians")
@OneToMany(mappedBy = "user")
private List<Guardian> guardians;

// Guardian.java
@JsonBackReference("user-guardians")
@ManyToOne
private User user;
```

### 4. DTO 패턴 사용 (권장)
연관관계를 포함하지 않는 DTO를 생성하여 순환 참조 회피

### 5. 엔티티 초기화 순서 제어
```java
@DependsOn("entityManagerFactory")
@Configuration
public class JpaEntityScanConfig {
    // 설정
}
```

## 즉시 적용 가능한 해결책

### Guardian 엔티티 수정
```java
@Entity
@Table(name = "guardians")
@ToString(exclude = {"user", "guardianUser"})  // 순환 참조 방지
public class Guardian extends BaseEntity {
    // 기존 코드
}
```

### User 엔티티 수정
```java
@Entity
@Table(name = "users")
@ToString(exclude = {"guardians", "guardianFor", "devices", "schedules", 
                    "notifications", "userPreference", "locationHistories", 
                    "activities", "medications", "healthMetrics", "roles"})
public class User extends BaseEntity {
    // 기존 코드
}
```

### JpaConfig 업데이트
```java
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = "com.bifai.reminder.bifai_backend.repository",
    considerNestedRepositories = true
)
@EntityScan(
    basePackages = "com.bifai.reminder.bifai_backend.entity"
)
public class JpaConfig {
    // 기존 코드
}
```