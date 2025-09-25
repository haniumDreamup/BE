# 컨트롤러 중복 기능 통합 계획

## 🎯 목표
BIF-AI 백엔드의 중복된 컨트롤러 기능을 정리하여 코드 중복을 제거하고 API 일관성을 향상시킵니다.

## 🔥 우선순위 1: Emergency/SOS 컨트롤러 통합

### 현재 상태
- **EmergencyController** (`/api/v1/emergency`)
  - 포괄적인 긴급상황 관리
  - 낙상 감지, 관리자 기능 포함
  - 복잡한 권한 관리

- **SosController** (`/api/sos`)
  - 원터치 SOS 특화
  - 단순한 사용자 인터페이스
  - BIF 사용자 친화적

### 통합 방식: **EmergencyController로 통합**

#### 이유:
1. **포괄성**: EmergencyController가 더 완전한 기능 제공
2. **확장성**: 향후 긴급상황 유형 확장에 유리
3. **관리효율**: 단일 컨트롤러로 관리 단순화

#### 마이그레이션 계획:

##### 1단계: SosController 기능을 EmergencyController로 이전
```java
// EmergencyController에 추가될 엔드포인트
@PostMapping("/sos/trigger")  // 원터치 SOS
@PostMapping("/sos/quick")    // 빠른 SOS (위치만)
@PutMapping("/sos/{emergencyId}/cancel") // SOS 취소
@GetMapping("/sos/history")   // 개인 SOS 이력
```

##### 2단계: 기존 SOS 엔드포인트 호환성 유지
- SosController를 @Deprecated 처리
- 기존 `/api/sos/*` 요청을 `/api/v1/emergency/*`로 리다이렉트

##### 3단계: 점진적 마이그레이션
- 클라이언트 앱에서 새 엔드포인트로 전환 후
- SosController 완전 제거

## ❌ AuthController vs OAuth2Controller 재분석 결과

### 중복 있음 - 통합 필요 (베스트 프랙티스)
**조사 결과**: Spring Boot OAuth2 베스트 프랙티스에 따르면 통합이 필요합니다.

#### 🔍 **중복되는 기능들**:
- **JWT 토큰 발급**: 소셜 로그인 후에도 동일한 JWT 생성 과정
- **사용자 세션 관리**: 인증 방식과 무관하게 동일한 사용자 상태 관리
- **권한 설정**: 모든 인증 방식에서 동일한 역할/권한 할당
- **인증 후 처리**: 로그인 성공 후 동일한 비즈니스 로직

#### 🎯 **올바른 아키텍처**:
- **AuthController**: 모든 인증 방식의 통합 관리
  - `/api/v1/auth/login` (기본 로그인)
  - `/api/v1/auth/oauth2/callback` (소셜 로그인 콜백)
  - 공통 JWT 발급 및 사용자 관리 로직

## 📊 우선순위 2: Guardian 컨트롤러 정리

### 현재 상태
- **GuardianController**: 보호자 기본 관리
- **GuardianRelationshipController**: 보호자-피보호자 관계 관리
- **GuardianDashboardController**: 보호자 대시보드

### 통합 방식: **기능별 분리 유지**
각각 명확한 책임이 있어 현재 구조 유지가 적절

## 🎯 우선순위 3: Health 엔드포인트 정리

### 중복 제거
- 여러 버전의 health check 엔드포인트 통합
- 단일 `/api/health` 엔드포인트로 표준화

## 📅 구현 일정

### Week 1: Emergency/SOS 통합
- [ ] EmergencyController에 SOS 기능 추가
- [ ] 테스트 케이스 작성 및 실행
- [ ] API 문서 업데이트

### Week 2: 호환성 및 테스트
- [ ] SosController 리다이렉트 구현
- [ ] 전체 통합 테스트
- [ ] 성능 검증

### Week 3: 최종 정리
- [ ] Health 엔드포인트 통합
- [ ] 불필요한 컨트롤러 제거
- [ ] 문서 최종 업데이트

## 📋 구현 시 고려사항

1. **하위 호환성**: 기존 클라이언트 앱 지원
2. **테스트 커버리지**: 통합 후에도 100% 유지
3. **BIF 사용자 경험**: 단순하고 직관적인 API 유지
4. **권한 관리**: 보안 요구사항 준수

## 📖 API 설계 원칙

1. **단순성**: BIF 사용자(IQ 70-85)를 위한 단순한 인터페이스
2. **일관성**: 전체 API에서 일관된 응답 형식
3. **안정성**: 긴급상황 처리의 높은 안정성 보장
4. **확장성**: 향후 기능 추가를 고려한 설계

## 🔧 기술적 세부사항

### 응답 형식 통일
```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 에러 처리 표준화
```json
{
  "success": false,
  "error": {
    "code": "USER_FRIENDLY_CODE",
    "message": "Simple explanation",
    "userAction": "What user can do"
  }
}
```