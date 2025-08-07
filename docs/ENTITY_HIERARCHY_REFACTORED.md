# 엔티티 계층 구조 리팩토링 완료

## 새로운 구조

```
BaseTimeEntity (시간 정보만)
│   - createdAt
│   - updatedAt
│
└── BaseEntity (+ 작성자 정보)
    │   - createdBy
    │   - modifiedBy
    │
    └── SoftDeletableEntity (+ Soft Delete)
        - deleted
        - deletedAt
        - deletedBy
```

## 구조 개선 내용

### 1. BaseTimeEntity
- **용도**: 단순히 생성/수정 시간만 필요한 엔티티
- **사용 예시**: Notification, Schedule, HealthMetric, ActivityLog
- **특징**: 가장 가벼운 베이스 클래스

### 2. BaseEntity
- **용도**: 시간 + 작성자 정보가 필요한 엔티티
- **사용 예시**: 대부분의 주요 엔티티
- **특징**: JPA Auditing으로 자동 관리

### 3. SoftDeletableEntity
- **용도**: Soft Delete가 필요한 중요 엔티티
- **사용 예시**: User, Guardian, Device, Role
- **특징**: 데이터 복구 가능, 감사 추적

## 변경 사항

1. **중복 제거**
   - `/model/BaseEntity.java` 삭제 완료
   - 패키지 정리로 혼란 해결

2. **명확한 네이밍**
   - BaseEntity: 기본 audit 기능
   - SoftDeletableEntity: Soft Delete 추가

3. **인터페이스 분리**
   - SoftDeletable 인터페이스 생성
   - 기능별 관심사 분리

## 엔티티별 적용 가이드

### BaseTimeEntity만 사용
```java
@Entity
public class Notification extends BaseTimeEntity {
    // 알림은 생성/수정 시간만 필요
}
```

### BaseEntity 사용
```java
@Entity
public class Emergency extends BaseEntity {
    // 긴급상황은 누가 생성/수정했는지 추적 필요
}
```

### SoftDeletableEntity 사용
```java
@Entity
public class User extends SoftDeletableEntity {
    // 사용자는 삭제 후에도 데이터 유지 필요
}
```

## 장점

1. **명확성**: 각 계층의 역할이 분명
2. **유연성**: 필요한 기능만 선택 사용
3. **유지보수**: 기능별로 독립적 관리
4. **확장성**: 새로운 공통 기능 추가 용이

## 마이그레이션 체크리스트

- [x] model 패키지의 BaseEntity 삭제
- [x] BaseEntity에서 Soft Delete 기능 제거
- [x] SoftDeletable 인터페이스 생성
- [x] SoftDeletableEntity 클래스 생성
- [ ] 각 엔티티의 상속 관계 업데이트
- [ ] Repository에 @Where 절 추가 (Soft Delete 엔티티)
- [ ] 테스트 코드 수정

## 다음 단계

1. 각 엔티티를 새로운 계층 구조에 맞게 수정
2. Repository에 Soft Delete 필터링 추가
3. Service 레이어에서 삭제 로직 수정
4. 통합 테스트 실행

이제 더 깔끔하고 이해하기 쉬운 엔티티 구조가 되었습니다!