# 프로젝트 정리 권장사항

## 1. 삭제 가능한 파일들

### 테스트/임시 파일
- **SimpleTestController.java** ❌
  - main 메소드가 있는 테스트용 컨트롤러
  - 실제 애플리케이션과 충돌 가능
  - HealthController가 이미 존재

### 중복/미사용 Config
- **TestCacheConfig.java** ⚠️
  - test 프로파일 전용이지만 src/main에 위치
  - src/test로 이동 권장

### 과도한 프로파일 설정
- 현재 8개의 application-*.yml 파일 존재
  - application-minimal.yml ❌
  - application-simple.yml ❌ 
  - application-noauth.yml ❌
  - application-h2.yml (dev와 중복?)

## 2. 빈 디렉토리
- `/dto/request/` - 빈 디렉토리
- `/dto/response/` - BifApiResponse.java만 있음 (ApiResponse와 중복?)
- `/static/` - 빈 디렉토리
- `/templates/` - 빈 디렉토리

## 3. 중복 클래스/기능

### Response DTO 중복
- `ApiResponse.java` vs `BifApiResponse.java`
  - 동일한 목적의 클래스로 보임
  - 하나로 통합 필요

### 과도한 Config 클래스
- DatabaseConfig + HibernateConfig + JpaConfig
  - 3개로 분리할 필요가 있나?
  - DatabaseConfig에 통합 고려

### Health Check 중복
- HealthController
- DatabaseHealthIndicator
- RedisHealthIndicator
  - Spring Actuator 사용 시 중복

## 4. TODO 정리 필요
- 12개의 TODO 발견
- NotificationService: 실제 구현 필요
- AdminService: 미구현 기능 다수

## 5. 미사용 Repository
아래 Repository들은 Entity는 있지만 Service에서 사용 안 됨:
- ContentMetadataRepository
- AnalysisResultRepository
- ReminderTemplateRepository
- MedicationAdherenceRepository
- ConnectivityLogRepository
- BatteryHistoryRepository

## 6. 네이밍 일관성
- 패키지명: bifai_backend (언더스코어 사용)
- 일반적으로 Java 패키지는 bifaibackend 또는 bifai.backend 권장

## 정리 우선순위

### 즉시 삭제 (High Priority) 🔴
1. SimpleTestController.java
2. 빈 디렉토리들 (/request, /static, /templates)
3. 중복 프로파일 파일 (minimal, simple, noauth)

### 통합/리팩토링 (Medium Priority) 🟡
1. ApiResponse vs BifApiResponse 통합
2. Config 파일 통합 (Database 관련)
3. TestCacheConfig를 test 소스로 이동

### 검토 후 결정 (Low Priority) 🟢
1. 미사용 Repository/Entity 제거
2. TODO 항목 처리
3. 패키지명 변경

## 실행 스크립트

```bash
# 1. 백업 생성
tar -czf backup_$(date +%Y%m%d_%H%M%S).tar.gz .

# 2. 불필요한 파일 삭제
rm src/main/java/.../SimpleTestController.java
rm src/main/java/.../dto/response/BifApiResponse.java
rm -rf src/main/java/.../dto/request/
rm -rf src/main/resources/static/
rm -rf src/main/resources/templates/
rm src/main/resources/application-minimal.yml
rm src/main/resources/application-simple.yml
rm src/main/resources/application-noauth.yml

# 3. TestCacheConfig 이동
mkdir -p src/test/java/.../config/
mv src/main/java/.../config/TestCacheConfig.java src/test/java/.../config/
```

## 예상 효과
- 코드베이스 20% 감소
- 빌드 시간 단축
- 유지보수성 향상
- 개발자 혼란 감소

## 주의사항
- 삭제 전 반드시 백업
- 프로파일 파일은 사용 여부 재확인
- Repository는 향후 기능 구현 계획 확인 후 삭제