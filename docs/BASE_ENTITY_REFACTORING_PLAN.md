# BaseEntity 중복 문제 및 리팩토링 계획

## 현재 문제점

### 1. 중복된 BaseEntity
- `/entity/BaseEntity.java` - Soft Delete 기능 포함
- `/model/BaseEntity.java` - 기본 audit 필드만 포함
- 같은 이름으로 다른 패키지에 존재 → 혼란 유발

### 2. 계층 구조의 문제
```
현재:
BaseTimeEntity (createdAt, updatedAt)
    └── BaseEntity (+ createdBy, modifiedBy, deleted)

문제:
- 일부 엔티티는 BaseEntity 사용
- 일부 엔티티는 BaseTimeEntity만 사용
- model 패키지의 BaseEntity는 사용되지 않음
```

### 3. 불일치하는 사용 패턴
- **BaseEntity 사용 (10개)**: User, Guardian, Role, Device 등 - Soft Delete 필요
- **BaseTimeEntity 사용 (11개)**: Notification, Schedule, HealthMetric 등 - Soft Delete 불필요

## 리팩토링 방안

### 방안 1: 3단계 계층 구조 (추천) ✅
```java
// 1. 시간 정보만
@MappedSuperclass
public abstract class BaseTimeEntity {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// 2. 시간 + 작성자 정보
@MappedSuperclass
public abstract class AuditableEntity extends BaseTimeEntity {
    private String createdBy;
    private String modifiedBy;
}

// 3. 전체 기능 (Soft Delete 포함)
@MappedSuperclass
public abstract class DeletableEntity extends AuditableEntity {
    private Boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
```

### 방안 2: 인터페이스 기반 접근
```java
public interface Timestamped {
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}

public interface Auditable extends Timestamped {
    String getCreatedBy();
    String getModifiedBy();
}

public interface SoftDeletable {
    boolean isDeleted();
    void softDelete(String deletedBy);
}
```

## 구체적인 실행 계획

### 1단계: 중복 제거
```bash
# model 패키지의 BaseEntity 삭제
rm /model/BaseEntity.java
```

### 2단계: 계층 구조 재정의
```java
// BaseTimeEntity.java - 그대로 유지
@MappedSuperclass
public abstract class BaseTimeEntity {
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}

// AuditableEntity.java - 새로 생성
@MappedSuperclass
public abstract class AuditableEntity extends BaseTimeEntity {
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String modifiedBy;
}

// BaseEntity.java → DeletableEntity.java로 이름 변경
@MappedSuperclass
public abstract class DeletableEntity extends AuditableEntity {
    private Boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
    
    public void softDelete(String deletedBy) { ... }
}
```

### 3단계: 엔티티별 적용

#### Soft Delete가 필요한 엔티티 (DeletableEntity 상속)
- User, Guardian - 중요 데이터, 삭제 시 기록 필요
- Device, Role - 변경 이력 추적 필요
- LocationHistory, BatteryHistory - 감사 목적

#### Audit 정보만 필요한 엔티티 (AuditableEntity 상속)
- Emergency - 긴급 상황은 삭제보다 상태 변경
- Geofence, Location - 위치 데이터는 새로 생성

#### 시간 정보만 필요한 엔티티 (BaseTimeEntity 상속)
- Notification, Schedule - 임시 데이터
- HealthMetric - 시계열 데이터
- ActivityLog - 로그는 삭제하지 않음

## 장점

1. **명확한 책임 분리**: 각 계층이 하나의 기능만 담당
2. **유연성**: 필요한 기능만 선택적으로 사용
3. **이해하기 쉬움**: 이름만으로 기능 파악 가능
4. **확장성**: 새로운 공통 기능 추가 용이

## 마이그레이션 체크리스트

- [ ] model 패키지의 BaseEntity 삭제
- [ ] AuditableEntity 클래스 생성
- [ ] BaseEntity → DeletableEntity 이름 변경
- [ ] 각 엔티티의 상속 관계 수정
- [ ] 테스트 코드 수정
- [ ] JPA Auditing 설정 확인

## 주의사항

1. **데이터베이스 스키마 변경 없음**: 컬럼명은 그대로 유지
2. **기존 코드 호환성**: 메소드 시그니처 유지
3. **점진적 마이그레이션**: 한 번에 모든 엔티티 수정하지 않음

## 예상 결과

```
BaseTimeEntity (11개 엔티티)
    ├── AuditableEntity (5개 엔티티)
    │       └── DeletableEntity (10개 엔티티)
    └── (직접 상속 엔티티들)
```

이렇게 하면 각 엔티티가 필요한 기능만 갖게 되어 더 깔끔하고 유지보수하기 쉬운 구조가 됩니다.