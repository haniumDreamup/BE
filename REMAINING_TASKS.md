# BIF-AI Backend 남은 작업 목록

## 🚨 1. 컴파일 에러 수정 (최우선)

### MediaFileRepository에 추가 필요
```java
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    List<MediaFile> findByUserUserIdAndUploadTypeOrderByCreatedAtDesc(Long userId, UploadType uploadType);
    List<MediaFile> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
```

### GeofenceRepository에 추가 필요
```java
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {
    List<Geofence> findByUserUserIdAndIsActive(Long userId, boolean isActive);
}
```

### LocationHistoryRepository에 추가 필요
```java
public interface LocationHistoryRepository extends JpaRepository<LocationHistory, Long> {
    List<LocationHistory> findByUserUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(
        Long userId, LocalDateTime start, LocalDateTime end);
}
```

### StatisticsService 메서드 시그니처 수정
- 현재: `getGeofenceStatistics(Long userId, int days)`
- 필요: `getGeofenceStatistics(Long userId, LocalDate start, LocalDate end)`

## 📋 2. 데이터베이스 마이그레이션

### Flyway 마이그레이션 추가
```sql
-- V2__Add_audit_columns.sql
ALTER TABLE users ADD COLUMN created_by BIGINT;
ALTER TABLE users ADD COLUMN updated_by BIGINT;
ALTER TABLE reminders ADD COLUMN created_by BIGINT;
ALTER TABLE reminders ADD COLUMN updated_by BIGINT;
-- 다른 테이블들도 동일하게...
```

## 🧪 3. 테스트 실행 및 검증

### 단위 테스트
- [ ] JwtAuthUtilsTest 실행
- [ ] MediaServiceTest 실행
- [ ] StatisticsServiceTest 작성 및 실행

### 통합 테스트
- [ ] AuthenticationIntegrationTest 실행
- [ ] API 엔드포인트 테스트
- [ ] S3 연동 테스트 (LocalStack)

## 🔐 4. 환경 설정 완료

### application-dev.yml 검증
- [ ] JWT 설정 확인
- [ ] AWS S3 설정 확인
- [ ] 데이터베이스 연결 확인

### application-prod.yml 준비
- [ ] 실제 AWS 자격증명 설정
- [ ] 프로덕션 데이터베이스 설정
- [ ] 보안 키 외부화 (AWS Secrets Manager)

## 🚀 5. 배포 준비

### Docker 설정
- [ ] Dockerfile 최적화
- [ ] docker-compose.yml 프로덕션 버전
- [ ] 헬스체크 설정

### CI/CD 파이프라인
- [ ] GitHub Actions 설정
- [ ] 자동 테스트 실행
- [ ] AWS ECR/ECS 배포

## 📊 6. 모니터링 및 로깅

### 로깅 설정
- [ ] Logback 설정 최적화
- [ ] CloudWatch 연동
- [ ] 에러 알림 설정

### 모니터링
- [ ] Spring Actuator 설정
- [ ] Prometheus/Grafana 연동
- [ ] 성능 메트릭 수집

## ✅ 7. 완료된 작업 (검증 필요)

### Phase 1: 인증 시스템 ✅
- JWT 토큰 user_id 클레임
- JwtAuthUtils 유틸리티
- 컨트롤러 보안 수정

### Phase 2: AWS S3 ✅
- AwsS3Config 설정
- MediaService 구현
- ImageAnalysisService 통합

### Phase 3: 운영 품질 ✅
- StatisticsService 구현
- StatisticsController API
- JPA Auditing 설정

## 🎯 8. 추가 개선 사항

### 성능 최적화
- [ ] 데이터베이스 인덱스 최적화
- [ ] 캐싱 전략 구현 (Redis)
- [ ] API 응답 시간 개선

### 보안 강화
- [ ] Rate Limiting 구현
- [ ] API Key 관리
- [ ] CORS 설정 검증

### 문서화
- [ ] API 문서 자동화 (Swagger/OpenAPI)
- [ ] 개발자 가이드 작성
- [ ] 운영 매뉴얼 작성

## 📅 우선순위 및 일정

### 즉시 (오늘)
1. Repository 인터페이스 메서드 추가
2. 컴파일 에러 해결
3. 기본 테스트 실행

### 단기 (1-2일)
1. 데이터베이스 마이그레이션
2. 통합 테스트 완료
3. 개발 환경 완전 검증

### 중기 (3-5일)
1. 프로덕션 환경 설정
2. Docker/CI/CD 설정
3. 모니터링 구축

### 장기 (1주 이후)
1. 성능 최적화
2. 추가 보안 강화
3. 완전한 문서화

## 🔍 현재 상태 요약

**구현 완료**: 핵심 기능 100% 구현
- JWT 인증 시스템 ✅
- AWS S3 파일 업로드 ✅
- 통계 서비스 ✅
- JPA Auditing ✅

**컴파일 가능**: ❌ (Repository 메서드 누락)
**테스트 가능**: ❌ (컴파일 에러로 인해 불가)
**배포 가능**: ❌ (테스트 미완료)

## 💡 다음 단계 추천

**바로 해야 할 일:**
1. Repository 인터페이스에 누락된 메서드 추가
2. `./gradlew build` 성공 확인
3. `./gradlew test` 실행 및 검증

이 세 가지만 완료하면 기본적인 기능은 모두 작동할 것입니다!