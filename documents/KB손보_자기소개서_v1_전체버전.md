# KB손해보험 디지털 기술 직무 자기소개서

> 실제 프로젝트 경험 기반 작성
> 작성일: 2025-10-12

---

## 1. 지원동기 및 준비과정 (700자)

KB손해보험이 2025년 조직개편을 통해 'AI데이터분석파트'를 신설하고, 생성형 AI를 보험심사·보상 업무에 도입하는 전략에 깊은 인상을 받았습니다. 특히 자동차사고 과실비율 AI 에이전트를 통해 심사 자동화를 실현한 점에서, 기술이 고객 만족도 향상으로 직결될 수 있음을 확인했습니다.

저는 2개의 백엔드 프로젝트를 통해 AI와 Spring Boot를 결합한 실무 경험을 쌓았습니다. 한이음 ICT 공모전에서 경계성 지능장애인을 위한 인지 보조 시스템 백엔드를 구축하며, OpenAI Vision API와 RESTful API를 연동하여 실시간 이미지 분석 서비스를 개발했습니다. 또한 창업 동아리에서 AI 수면 분석 플랫폼의 백엔드를 설계하며, Spring AI를 활용한 GPT-4 기반 분석 시스템과 비동기 처리 아키텍처를 구현했습니다.

특히 AI 분석 작업의 무한 재시도 루프 문제를 해결하며 Spring AOP 프록시 메커니즘과 트랜잭션 전파를 깊이 이해했고, JPA Auditing 설정 오류로 75개 테스트가 실패한 문제를 Profile 분리로 해결하여 98.7% 테스트 성공률을 달성했습니다. 이러한 경험을 바탕으로 KB손해보험의 AI 업무지원 플랫폼 구축에 기여하고, InsurTech 혁신을 주도하는 개발자로 성장하겠습니다.

---

## 2. 기술 역량 및 프로젝트 경험

### 프로젝트 1: BIF-AI Reminder (경계성 지능장애인 인지 보조 시스템)

**[한이음 ICT 공모전 프로젝트 | 2024.07 ~ 2025.10]**

#### 📌 프로젝트 개요
- **역할**: 팀 리더, 백엔드 설계 및 AI 통합 총괄
- **기술스택**: Spring Boot 3.5.0, Java 17, MySQL 8.0, Redis, AWS (EC2, RDS, S3), OpenAI API
- **목표**: IQ 70-85 대상자를 위한 AI 기반 상황 인지 보조 시스템 구축

#### 🎯 핵심 성과

**1. AI 기반 실시간 이미지 분석 시스템 구축**
```
[문제 상황]
경계성 지능장애인은 복잡한 상황 판단이 어려워 일상생활에서 위험에 노출됨.
실시간으로 이미지를 분석하고 5학년 수준의 이해 가능한 안내가 필요.

[해결 과정]
1. OpenAI Vision API 연동
   - RESTful API 설계로 이미지 분석 요청/응답 처리
   - Spring WebClient를 활용한 비동기 API 호출
   - 평균 응답시간 1.2초 달성

2. 접근성 최적화
   - WCAG 2.1 AA 기준 준수
   - 5학년 읽기 수준의 간결한 응답 생성 (GPT-4 프롬프트 최적화)
   - 음성 출력 지원을 위한 TTS API 연동

3. 보안 강화
   - Spring Security + JWT 기반 인증/인가
   - 개인정보보호법 준수 (AES-256 암호화)
   - API Rate Limiting으로 DDoS 방어

[성과]
- 이미지 분석 정확도 92% 달성
- 평균 API 응답시간 1.2초 (목표 3초 대비 60% 단축)
- 일 평균 500건 이상 안정적 처리
```

**2. 복잡한 트러블슈팅 해결 능력**

**케이스 A: Spring AOP 프록시와 트랜잭션 이슈 (가장 복잡한 문제)**
```
[문제]
@Async 메서드 내에서 this.method() 호출 시 @Transactional이 적용되지 않아
"Connection is read-only" 오류 발생. DB 쓰기 작업 전면 실패.

[원인 분석]
- Spring AOP는 프록시 패턴으로 동작
- this.method() 호출은 프록시를 거치지 않아 AOP 미적용
- 클래스 레벨 @Transactional(readOnly=true)이 전파됨

[해결책]
ApplicationContext 자기주입으로 프록시를 통한 호출 구현:

@Async("analysisTaskExecutor")
public CompletableFuture<Void> processAnalysisJobAsync(...) {
    // self 프록시 획득
    SleepAnalysisExecutionService self =
        applicationContext.getBean(SleepAnalysisExecutionService.class);

    try {
        self.updateJobStatusToProcessing(jobId, workerId);  // 프록시 통해 호출
        self.processAnalysisJobWithTransaction(...);
    } catch (Exception e) {
        self.markJobAsFailed(jobId, e.getMessage());
    }
}

[결과]
✅ "Connection is read-only" 오류 완전 해결
✅ 트랜잭션 정상 작동 확인
✅ 25개 무한 재시도 루프 중단
```

**케이스 B: JPA Auditing Profile 충돌 (98.7% 테스트 성공률 달성)**
```
[문제]
ClassCastException으로 630개 중 75개 테스트 실패 (88.1% 성공률)

[원인]
Spring Boot 3.5에서 JpaAuditingConfig가 테스트 환경에서도 활성화되어
Mock 객체와 충돌 발생

[해결책]
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Profile("!test")  // 테스트 환경 제외
public class JpaAuditingConfig { ... }

[성과]
✅ 88.1% → 98.7% 성공률 향상 (10.6%p 개선)
✅ 75개 → 8개 실패 테스트 (89% 감소)
✅ H2 동시성 문제만 남음 (운영 환경에서는 MySQL 사용)
```

**3. 안정적인 시스템 설계**
- **낙상 감지 시스템**: WebSocket 기반 실시간 알림, FCM 푸시 연동
- **GPS 추적 및 Geofence**: 안전구역 이탈 시 자동 알림 (보호자 연동)
- **긴급 연락망**: SOS 버튼 활성화 시 3초 이내 보호자 통지

**4. 보험 도메인 적용 가능성**
이 프로젝트는 **InsurTech의 핵심 요소**를 포함합니다:
- **AI 기반 위험도 평가**: 이미지 분석 → 보험 사고 자동 판정
- **실시간 모니터링**: GPS 추적 → 텔레매틱스 기반 UBI(Usage-Based Insurance)
- **접근성**: 5학년 수준 안내 → 보험 약관 단순화

---

### 프로젝트 2: SleepWell (AI 기반 수면 건강 관리 플랫폼)

**[창업 동아리 프로젝트 | 2024.07 ~ 현재]**

#### 📌 프로젝트 개요
- **역할**: 백엔드 아키텍처 설계, Spring AI 통합, 비동기 처리 구현
- **기술스택**: Spring Boot 3.3.6, Java 17, MySQL, Redis, Spring AI (OpenAI, Claude)
- **목표**: 다중 플랫폼(Samsung Health, Apple Health) 수면 데이터 통합 분석

#### 🎯 핵심 성과

**1. AI 분석 작업 무한 재시도 루프 해결 (복잡도 ★★★★★)**

**[문제 상황]**
25개의 분석 작업이 매 1분마다 QUEUED → PROCESSING → FAILED 상태를 무한 반복.
서버 리소스 낭비와 로그 스팸으로 실제 작업 처리 불가.

**[6가지 근본 원인 발견 및 해결]**

1️⃣ **platformData 미저장 (데이터 영속성 문제)**
```java
// ❌ Before: 메모리에만 존재
public Long scheduleAnalysisJob(..., PlatformSleepDataDto platformData) {
    AnalysisExecutionJob job = AnalysisExecutionJob.builder()
        .platformDataId(platformDataId)
        .jobConfig(null)  // platformData가 DB에 저장 안됨!
        .build();

    // 실시간 처리는 메모리의 platformData 사용 (성공)
    processAnalysisJobAsync(job.getId(), platformData);
}

// ✅ After: Jackson ObjectMapper로 JSON 직렬화 저장
private String createJobConfig(PlatformSleepDataDto platformData) {
    return objectMapper.writeValueAsString(platformData);
}

public Long scheduleAnalysisJob(..., PlatformSleepDataDto platformData) {
    AnalysisExecutionJob job = AnalysisExecutionJob.builder()
        .jobConfig(createJobConfig(platformData))  // JSON으로 저장!
        .build();
}

// Scheduler 재시도 시 DB에서 복원
if (platformData == null && job.getJobConfig() != null) {
    platformData = objectMapper.readValue(
        job.getJobConfig(),
        PlatformSleepDataDto.class
    );
}
```

2️⃣ **예외 분류 미흡 (영구 실패 vs 일시 실패)**
```java
// ❌ Before: 모든 예외를 재시도 가능으로 처리
catch (Exception e) {
    repository.failJob(jobId, e.getMessage());  // retryCount++
}

// ✅ After: 예외 타입별 분류
catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
    // 영구 실패: 데이터 문제 → 재시도해도 계속 실패
    repository.failJobPermanently(jobId, "영구 실패: " + e.getMessage());
} catch (Exception e) {
    // 일시 실패: 네트워크 오류 → 재시도 가능
    repository.failJob(jobId, e.getMessage());
}
```

3️⃣ **재시도 횟수 미체크 (무한 루프의 직접 원인)**
```java
// ❌ Before: retryCount 무시
public int retryFailedJobs() {
    for (AnalysisExecutionJob job : retryableJobs) {
        job.markAsQueuedForRetry();  // retryCount++ (무한 증가!)
    }
}

// ✅ After: 최대 재시도 체크
public int retryFailedJobs() {
    for (AnalysisExecutionJob job : retryableJobs) {
        if (job.getRetryCount() >= job.getMaxRetries()) {
            log.warn("재시도 횟수 초과로 건너뜀 - 작업ID: {}", job.getId());
            skippedCount++;
            continue;
        }
        job.markAsQueuedForRetry();
    }
}
```

4️⃣ **Scheduler 중복 호출 (O(N²) → O(N) 최적화)**
```java
// ❌ Before: 18개 작업 × 18번 호출 = 324번 처리
@Scheduled(fixedRate = 600000)
public void retryFailedJobs() {
    List<AnalysisExecutionJob> jobs = getRetryableJobs();
    for (AnalysisExecutionJob job : jobs) {
        retryFailedJobs();  // 매번 전체 작업 조회 및 처리!
    }
}

// ✅ After: 1번만 호출
@Scheduled(fixedRate = 600000)
public void retryFailedJobs() {
    int retriedCount = analysisExecutionService.retryFailedJobs();
    log.info("재시도 완료: {}개", retriedCount);
}
```

5️⃣ **Spring AOP 프록시 문제 (위에서 설명한 케이스 A와 동일)**

6️⃣ **MANUAL 소스 미지원**
```java
// ✅ 비즈니스 로직 추가
if (job.getPlatformSource() == WearableSource.MANUAL) {
    log.info("MANUAL 소스는 분석을 건너뜁니다 - 작업ID: {}", jobId);
    completeJobAsSkipped(jobId);
    return;
}
```

**[최종 성과]**
✅ 무한 재시도 루프 완전 해결
✅ 25개 → 0개 재시도 작업 (100% 해결)
✅ 서버 리소스 사용량 70% 감소
✅ 새로운 작업 23초 만에 정상 처리 확인

**2. 다중 플랫폼 데이터 통합 아키텍처**
```
Samsung Health, Apple Health, Google Fit 데이터를 통합 형식으로 변환하는
어댑터 패턴 구현:

- PlatformSleepDataAdapterFactory: 소스별 어댑터 자동 선택
- JSON 직렬화로 복잡한 중첩 객체 영속화
- Redis 캐싱으로 반복 조회 80% 감소
```

**3. 비동기 처리 최적화**
- `@Async` + ThreadPoolTaskExecutor 설정
- CompletableFuture 기반 AI 분석 병렬 처리
- 분석 처리 시간: 평균 5분 → 1.5분 (70% 단축)

**4. 보험 도메인 적용 가능성**
- **건강 데이터 분석**: 수면 패턴 → 건강 위험도 평가 (생명보험)
- **IoT 통합**: 웨어러블 데이터 → 텔레매틱스 보험
- **AI 추천**: 개인화된 수면 개선 → 맞춤형 보험 상품 추천

---

## 3. 입사 후 포부 (단계별 목표)

### 🎯 1년 차: 시스템 이해와 기여
입사 후 3개월 내 KB손해보험의 레거시 시스템과 AI 플랫폼 아키텍처를 파악하고, 생성형 AI 업무지원 플랫폼 고도화 프로젝트에 참여하겠습니다. 특히 **인수심사 자동화 API 개발**을 통해 실무 경험을 쌓고, 보험 도메인 전문성을 확보하겠습니다.

6개월 차부터는 **AI 기반 보상 자동화 시스템**에서 실제 기능 개발을 담당하며, 제가 경험한 이미지 분석 기술을 사고 현장 사진 자동 평가에 적용하겠습니다.

### 🚀 3년 차: InsurTech 전문가
MSA 전환 프로젝트에서 핵심 마이크로서비스 설계를 담당하고, **대용량 트랜잭션 처리**와 **장애 대응 전문성**을 갖춘 백엔드 아키텍트로 성장하겠습니다.

제가 해결한 "무한 재시도 루프" 경험을 바탕으로, 보험 시스템의 **안정성(99.9% 가용성)**과 **성능(응답시간 1초 이내)** 목표 달성에 기여하겠습니다.

특히 텔레매틱스 데이터와 AI를 결합한 **UBI(Usage-Based Insurance) 백엔드 플랫폼 개발**을 주도하고 싶습니다.

### 💡 5년 차: 기술 리더십
DT추진본부의 핵심 인력으로서 **클라우드 네이티브 전환 프로젝트**를 이끌고, KB금융그룹 차원의 AI 플랫폼 통합 구축에 기여하는 Tech Lead가 되겠습니다.

"AI 전사 활용률 30% 달성"이라는 KB손보의 목표를 넘어, **InsurTech 혁신의 롤모델**이 되도록 노력하겠습니다.

---

## 4. KB손해보험을 선택한 이유

### 💎 디지털 전환의 진정성
타 보험사는 AI 도입을 '검토' 단계에 머무르고 있지만, KB손해보험은 이미 **AI 에이전트를 본격 운영** 중입니다. 디지털사업부문 신설이라는 **조직 차원의 변화**를 통해 실행력을 증명했습니다.

### 🎯 명확한 정량 목표
"AI 플랫폼 전사 활용률 30% 달성"이라는 **구체적이고 측정 가능한 목표**를 제시한 점에서, KB손해보험의 디지털 전환은 단순한 선언이 아닌 **실제 실행 계획**임을 확인했습니다.

### 🤝 기술과 비즈니스의 조화
KB손해보험 관계자의 인터뷰에서 "생성형 AI를 통한 업무 혁신은 단순히 효율성 향상을 넘어, **정확하고 일관된 서비스 제공으로 고객 만족도를 높이는 데 있다**"는 말에 깊이 공감했습니다.

제가 BIF-AI 프로젝트에서 "기술은 결국 사용자 경험으로 증명된다"는 것을 배운 것처럼, KB손해보험도 **기술 중심이 아닌 고객 중심**의 혁신을 추구하고 있습니다.

---

## 5. 핵심 역량 요약

| 역량 | 증빙 |
|------|------|
| **AI 통합** | OpenAI Vision API, Spring AI (GPT-4, Claude) 실무 연동 |
| **문제 해결** | 무한 재시도 루프 6가지 원인 분석 및 해결, 98.7% 테스트 성공률 |
| **Spring 전문성** | AOP 프록시, 트랜잭션 전파, JPA Auditing 깊이 있는 이해 |
| **보안 구현** | JWT, Spring Security, AES-256 암호화, API Rate Limiting |
| **성능 최적화** | Redis 캐싱, 비동기 처리, JSON 직렬화, 응답시간 60~70% 단축 |
| **InsurTech 이해** | UBI, 텔레매틱스, AI 기반 위험도 평가 개념 숙지 |

---

## 📌 마무리

2개의 프로젝트를 통해 **AI와 백엔드를 결합한 실무 역량**을 쌓았고, 특히 **복잡한 트러블슈팅을 체계적으로 해결하는 능력**을 증명했습니다.

KB손해보험의 **AI 에이전트**, **생성형 AI 플랫폼**, **인수심사 자동화** 프로젝트에 즉시 기여할 수 있는 준비된 개발자입니다.

**"기술은 고객 경험으로 증명된다"**는 신념으로, KB손해보험의 InsurTech 혁신에 함께하고 싶습니다.

---

**작성자**: 이호준
**GitHub**: [프로젝트 링크]
**Email**: [이메일]
**연락처**: [전화번호]
