# Spring Boot 3.5 RestTemplate 및 Apache HttpClient 의존성 문제 조사 보고서

## 문제 요약
Spring Boot 3.5에서 RestTemplate 사용 시 `NoClassDefFoundError: org/apache/hc/client5/http/ssl/TlsSocketStrategy` 오류가 발생하는 이유와 해결 방법

## 근본 원인 분석

### 1. Spring Boot 3.x의 변경사항
- Spring Boot 3.x부터 Apache HttpComponents 5.x를 기본 HTTP 클라이언트로 사용
- RestTemplate 자동 구성 시 `HttpComponentsClientHttpRequestFactory` 사용
- TLS/SSL 연결 설정을 위해 `TlsSocketStrategy` 클래스 필요

### 2. 의존성이 필요한 이유
- **RestTemplate 자동 구성**: Spring Boot가 RestTemplate을 감지하면 자동으로 HTTP 클라이언트를 구성
- **TLS/SSL 지원**: HTTPS 연결을 위해 TlsSocketStrategy가 필수
- **Connection Pooling**: 성능 최적화를 위한 연결 풀링 기능 제공

### 3. 테스트 환경에서의 문제
- 프로덕션 코드의 `HttpClientConfig.java`가 Apache HttpClient를 사용
- 테스트 환경에서도 동일한 의존성 필요
- Spring Boot 3.5의 자동 구성이 테스트에서도 활성화

## 업계 베스트 프랙티스

### 1. Context7 조사 결과
- **공식 Spring Boot 문서**: HTTP 클라이언트 팩토리를 명시적으로 설정 권장
  ```yaml
  spring:
    http:
      client:
        factory: http-components  # 또는 jetty, simple, jdk
  ```

- **의존성 관리**: Apache HttpClient 5 의존성 명시적 추가
  ```gradle
  implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'
  implementation 'org.apache.httpcomponents.core5:httpcore5:5.2.4'
  ```

### 2. 웹 검색 결과
- **Spring Boot 3.4+ 변경사항**: 
  - HTTP 클라이언트 자동 선택 우선순위 변경
  - Apache HttpComponents > Jetty > Reactor > JDK > Simple 순서

- **일반적인 해결 방법**:
  1. 필수 의존성 추가
  2. 테스트용 HTTP 클라이언트 구성 분리
  3. 자동 구성 비활성화 옵션 사용

## 권장 해결 방안

### 옵션 1: 의존성 추가 (권장)
**장점**: 
- 프로덕션과 동일한 환경에서 테스트
- Connection pooling 등 고급 기능 사용 가능
- Spring Boot의 자동 구성 활용

**단점**:
- 추가 의존성 필요
- 테스트 실행 시간 약간 증가

### 옵션 2: 테스트용 간단한 HTTP 클라이언트 사용
```yaml
spring:
  http:
    client:
      factory: simple  # 또는 jdk
```

**장점**:
- 의존성 최소화
- 테스트 실행 속도 향상

**단점**:
- 프로덕션과 다른 환경
- 고급 기능 미지원

### 옵션 3: 테스트에서 HTTP 클라이언트 Mock
```java
@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate(new SimpleClientHttpRequestFactory());
    }
}
```

**장점**:
- 완전한 제어 가능
- 테스트 격리 향상

**단점**:
- 추가 설정 필요
- 실제 HTTP 동작과 차이 가능

## 최종 권장사항

프로젝트의 요구사항을 고려하여:

1. **통합 테스트의 정확성이 중요한 경우**: 옵션 1 (의존성 추가)
2. **테스트 속도와 간단함이 중요한 경우**: 옵션 2 (Simple/JDK 클라이언트)
3. **단위 테스트 위주인 경우**: 옵션 3 (Mock 사용)

현재 프로젝트는 100+ 동시 사용자 지원이 목표이므로, 프로덕션과 동일한 환경에서 테스트하는 것이 중요합니다. 따라서 **옵션 1 (Apache HttpClient 의존성 추가)**을 권장합니다.

## 구현 계획

1. Apache HttpClient 5 의존성 확인 및 추가
2. 테스트 구성에서 HTTP 클라이언트 팩토리 명시적 설정
3. 필요 시 테스트별 커스텀 구성 추가
4. 모든 테스트 통과 확인