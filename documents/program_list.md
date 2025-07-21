# BIF-AI 시스템 프로그램 목록

## 1. 백엔드 애플리케이션 (Spring Boot)

### 1.1 메인 애플리케이션
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| BifaiBackendApplication | `src/main/java/com/bifai/reminder/bifai_backend/BifaiBackendApplication.java` | Spring Boot 메인 클래스 | 애플리케이션 시작점 |

### 1.2 설정 클래스 (Configuration)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| AppConfig | `src/main/java/.../config/AppConfig.java` | 애플리케이션 전역 설정 | Bean 등록, 공통 설정 |
| SecurityConfig | `src/main/java/.../config/SecurityConfig.java` | 보안 설정 | JWT, 인증/인가 |
| JpaConfig | `src/main/java/.../config/JpaConfig.java` | JPA 설정 | 데이터베이스 연결 |
| RedisConfig | `src/main/java/.../config/RedisConfig.java` | Redis 설정 | 캐시, 세션 관리 |

### 1.3 컨트롤러 클래스 (Controller)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| AuthController | `src/main/java/.../controller/AuthController.java` | 인증 관련 API | 로그인, 회원가입, 토큰 관리 |
| HealthController | `src/main/java/.../controller/HealthController.java` | 헬스체크 API | 시스템 상태 확인 |
| MedicationController | `src/main/java/.../controller/MedicationController.java` | 복약 관리 API | 약물 등록, 복용 기록 |
| ReminderController | `src/main/java/.../controller/ReminderController.java` | 알림 관리 API | 알림 생성, 조회, 수정 |
| EmergencyController | `src/main/java/.../controller/EmergencyController.java` | 응급상황 API | 응급 신고, 보호자 알림 |
| HealthMetricController | `src/main/java/.../controller/HealthMetricController.java` | 건강지표 API | 혈압, 혈당 등 기록 |
| GuardianController | `src/main/java/.../controller/GuardianController.java` | 보호자 관리 API | 보호자 등록, 권한 관리 |
| DashboardController | `src/main/java/.../controller/DashboardController.java` | 대시보드 API | 종합 현황 조회 |

### 1.4 서비스 클래스 (Service)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| AuthService | `src/main/java/.../service/AuthService.java` | 인증 서비스 | 사용자 인증 로직 |
| MedicationService | `src/main/java/.../service/MedicationService.java` | 복약 관리 서비스 | 약물 관리, 복용 추적 |
| ReminderService | `src/main/java/.../service/ReminderService.java` | 알림 서비스 | 알림 스케줄링, 전송 |
| NotificationService | `src/main/java/.../service/NotificationService.java` | 알림 전송 서비스 | 푸시, SMS, 이메일 |
| EmergencyService | `src/main/java/.../service/EmergencyService.java` | 응급상황 서비스 | 응급 감지, 대응 처리 |
| HealthAnalysisService | `src/main/java/.../service/HealthAnalysisService.java` | 건강 분석 서비스 | 건강 데이터 분석 |
| UserPatternService | `src/main/java/.../service/UserPatternService.java` | 사용자 패턴 서비스 | 행동 패턴 분석 |
| GuardianService | `src/main/java/.../service/GuardianService.java` | 보호자 서비스 | 보호자 관리, 권한 |
| ScheduleService | `src/main/java/.../service/ScheduleService.java` | 일정 관리 서비스 | 일정 등록, 알림 |
| LocationService | `src/main/java/.../service/LocationService.java` | 위치 서비스 | GPS 추적, 안전구역 |

### 1.5 데이터 액세스 클래스 (Repository)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| UserRepository | `src/main/java/.../repository/UserRepository.java` | 사용자 데이터 액세스 | 사용자 CRUD |
| MedicationRepository | `src/main/java/.../repository/MedicationRepository.java` | 약물 데이터 액세스 | 약물 정보 관리 |
| MedicationAdherenceRepository | `src/main/java/.../repository/MedicationAdherenceRepository.java` | 복약 순응도 데이터 | 복용 기록 관리 |
| ReminderRepository | `src/main/java/.../repository/ReminderRepository.java` | 알림 데이터 액세스 | 알림 정보 관리 |
| NotificationRepository | `src/main/java/.../repository/NotificationRepository.java` | 알림 전송 데이터 | 알림 전송 이력 |
| GuardianRepository | `src/main/java/.../repository/GuardianRepository.java` | 보호자 데이터 액세스 | 보호자 정보 관리 |
| HealthMetricRepository | `src/main/java/.../repository/HealthMetricRepository.java` | 건강지표 데이터 | 건강 측정값 관리 |
| ActivityLogRepository | `src/main/java/.../repository/ActivityLogRepository.java` | 활동 로그 데이터 | 사용자 활동 기록 |
| ScheduleRepository | `src/main/java/.../repository/ScheduleRepository.java` | 일정 데이터 액세스 | 일정 정보 관리 |
| AnalysisResultRepository | `src/main/java/.../repository/AnalysisResultRepository.java` | 분석 결과 데이터 | AI 분석 결과 저장 |

### 1.6 엔티티 클래스 (Entity)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| User | `src/main/java/.../entity/User.java` | 사용자 엔티티 | 사용자 정보 |
| Guardian | `src/main/java/.../entity/Guardian.java` | 보호자 엔티티 | 보호자 정보 |
| Medication | `src/main/java/.../entity/Medication.java` | 약물 엔티티 | 약물 정보 |
| MedicationAdherence | `src/main/java/.../entity/MedicationAdherence.java` | 복약 순응도 엔티티 | 복용 기록 |
| Reminder | `src/main/java/.../entity/Reminder.java` | 알림 엔티티 | 알림 정보 |
| Notification | `src/main/java/.../entity/Notification.java` | 알림 전송 엔티티 | 알림 전송 내역 |
| HealthMetric | `src/main/java/.../entity/HealthMetric.java` | 건강지표 엔티티 | 건강 측정값 |
| ActivityLog | `src/main/java/.../entity/ActivityLog.java` | 활동 로그 엔티티 | 사용자 활동 |
| Schedule | `src/main/java/.../entity/Schedule.java` | 일정 엔티티 | 일정 정보 |
| Device | `src/main/java/.../entity/Device.java` | 기기 엔티티 | 사용자 기기 정보 |
| Location | `src/main/java/.../entity/Location.java` | 위치 엔티티 | 위치 정보 |

### 1.7 DTO 클래스 (Data Transfer Object)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| ApiResponse | `src/main/java/.../dto/ApiResponse.java` | 공통 응답 DTO | 표준 API 응답 |
| LoginRequest | `src/main/java/.../dto/auth/LoginRequest.java` | 로그인 요청 DTO | 로그인 정보 |
| AuthResponse | `src/main/java/.../dto/auth/AuthResponse.java` | 인증 응답 DTO | 토큰 정보 |
| MedicationDto | `src/main/java/.../dto/MedicationDto.java` | 약물 DTO | 약물 정보 전송 |
| ReminderDto | `src/main/java/.../dto/ReminderDto.java` | 알림 DTO | 알림 정보 전송 |
| HealthMetricDto | `src/main/java/.../dto/HealthMetricDto.java` | 건강지표 DTO | 건강 데이터 전송 |
| EmergencyDto | `src/main/java/.../dto/EmergencyDto.java` | 응급상황 DTO | 응급 상황 정보 |
| DashboardDto | `src/main/java/.../dto/DashboardDto.java` | 대시보드 DTO | 종합 현황 정보 |

### 1.8 보안 클래스 (Security)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| JwtTokenProvider | `src/main/java/.../security/jwt/JwtTokenProvider.java` | JWT 토큰 제공자 | 토큰 생성/검증 |
| JwtAuthenticationFilter | `src/main/java/.../security/jwt/JwtAuthenticationFilter.java` | JWT 인증 필터 | 요청 인증 처리 |
| BifUserDetails | `src/main/java/.../security/userdetails/BifUserDetails.java` | 사용자 상세 정보 | Spring Security 사용자 |
| BifUserDetailsService | `src/main/java/.../security/userdetails/BifUserDetailsService.java` | 사용자 상세 서비스 | 사용자 로드 |

### 1.9 예외 처리 클래스 (Exception)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| GlobalExceptionHandler | `src/main/java/.../exception/GlobalExceptionHandler.java` | 전역 예외 처리자 | 공통 예외 처리 |
| ResourceNotFoundException | `src/main/java/.../exception/ResourceNotFoundException.java` | 리소스 없음 예외 | 404 에러 처리 |
| UnauthorizedException | `src/main/java/.../exception/UnauthorizedException.java` | 권한 없음 예외 | 401 에러 처리 |
| ValidationException | `src/main/java/.../exception/ValidationException.java` | 유효성 검사 예외 | 400 에러 처리 |

## 2. 유틸리티 및 헬퍼 클래스

### 2.1 유틸리티 클래스 (Utility)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| DateTimeUtil | `src/main/java/.../util/DateTimeUtil.java` | 날짜/시간 유틸리티 | 날짜 처리 공통 기능 |
| EncryptionUtil | `src/main/java/.../util/EncryptionUtil.java` | 암호화 유틸리티 | 데이터 암호화/복호화 |
| ValidationUtil | `src/main/java/.../util/ValidationUtil.java` | 유효성 검사 유틸리티 | 입력값 검증 |
| NotificationUtil | `src/main/java/.../util/NotificationUtil.java` | 알림 유틸리티 | 알림 템플릿, 형식 |
| LocationUtil | `src/main/java/.../util/LocationUtil.java` | 위치 유틸리티 | 거리 계산, 좌표 변환 |

### 2.2 상수 클래스 (Constants)
| 프로그램명 | 파일 경로 | 설명 | 담당 기능 |
|-----------|----------|------|-----------|
| ApiConstants | `src/main/java/.../constant/ApiConstants.java` | API 상수 | URL, 응답 코드 |
| SecurityConstants | `src/main/java/.../constant/SecurityConstants.java` | 보안 상수 | 토큰 만료시간, 키 |
| NotificationConstants | `src/main/java/.../constant/NotificationConstants.java` | 알림 상수 | 알림 타입, 템플릿 |
| HealthConstants | `src/main/java/.../constant/HealthConstants.java` | 건강 상수 | 정상 범위, 임계값 |

## 3. 설정 파일

### 3.1 애플리케이션 설정
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| application.yml | `src/main/resources/application.yml` | 기본 설정 | 공통 설정 |
| application-dev.yml | `src/main/resources/application-dev.yml` | 개발 환경 설정 | 개발용 DB, 로그 |
| application-prod.yml | `src/main/resources/application-prod.yml` | 운영 환경 설정 | 운영용 DB, 보안 |
| application-test.yml | `src/main/resources/application-test.yml` | 테스트 환경 설정 | 테스트용 DB |
| logback-spring.xml | `src/main/resources/logback-spring.xml` | 로깅 설정 | 로그 레벨, 형식 |

### 3.2 데이터베이스 마이그레이션
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| V1__Initial_Schema.sql | `src/main/resources/db/migration/V1__Initial_Schema.sql` | 초기 스키마 | 기본 테이블 생성 |
| V2__Performance_Optimization.sql | `src/main/resources/db/migration/V2__Performance_Optimization.sql` | 성능 최적화 | 인덱스, 제약조건 |
| V3__Add_Emergency_Features.sql | `src/main/resources/db/migration/V3__Add_Emergency_Features.sql` | 응급 기능 추가 | 응급 관련 테이블 |

### 3.3 빌드 설정
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| build.gradle | `build.gradle` | Gradle 빌드 설정 | 의존성, 빌드 태스크 |
| settings.gradle | `settings.gradle` | Gradle 프로젝트 설정 | 프로젝트 이름 |
| gradle.properties | `gradle.properties` | Gradle 속성 | JVM 옵션 |

## 4. 테스트 클래스

### 4.1 단위 테스트 (Unit Test)
| 프로그램명 | 파일 경로 | 설명 | 테스트 대상 |
|-----------|----------|------|------------|
| AuthServiceTest | `src/test/java/.../service/AuthServiceTest.java` | 인증 서비스 테스트 | AuthService |
| MedicationServiceTest | `src/test/java/.../service/MedicationServiceTest.java` | 복약 서비스 테스트 | MedicationService |
| ReminderServiceTest | `src/test/java/.../service/ReminderServiceTest.java` | 알림 서비스 테스트 | ReminderService |
| HealthAnalysisServiceTest | `src/test/java/.../service/HealthAnalysisServiceTest.java` | 건강 분석 서비스 테스트 | HealthAnalysisService |
| JwtTokenProviderTest | `src/test/java/.../security/jwt/JwtTokenProviderTest.java` | JWT 토큰 제공자 테스트 | JwtTokenProvider |

### 4.2 통합 테스트 (Integration Test)
| 프로그램명 | 파일 경로 | 설명 | 테스트 대상 |
|-----------|----------|------|------------|
| AuthControllerIntegrationTest | `src/test/java/.../controller/AuthControllerIntegrationTest.java` | 인증 API 통합 테스트 | AuthController |
| MedicationControllerIntegrationTest | `src/test/java/.../controller/MedicationControllerIntegrationTest.java` | 복약 API 통합 테스트 | MedicationController |
| DatabaseConfigTest | `src/test/java/.../config/DatabaseConfigTest.java` | 데이터베이스 설정 테스트 | JPA, DB 연결 |

### 4.3 성능 테스트 (Performance Test)
| 프로그램명 | 파일 경로 | 설명 | 테스트 대상 |
|-----------|----------|------|------------|
| NotificationPerformanceTest | `src/test/java/.../performance/NotificationPerformanceTest.java` | 알림 성능 테스트 | 대량 알림 처리 |
| DatabasePerformanceTest | `src/test/java/.../performance/DatabasePerformanceTest.java` | DB 성능 테스트 | 쿼리 성능 |

## 5. 배포 및 운영 스크립트

### 5.1 Docker 설정
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| Dockerfile | `Dockerfile` | Docker 이미지 빌드 | 컨테이너 설정 |
| docker-compose.yml | `docker-compose.yml` | Docker 컨테이너 구성 | 전체 시스템 실행 |
| docker-compose.dev.yml | `docker-compose.dev.yml` | 개발 환경 Docker | 개발용 구성 |

### 5.2 배포 스크립트
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| deploy.sh | `scripts/deploy.sh` | 배포 스크립트 | 자동 배포 |
| health-check.sh | `scripts/health-check.sh` | 헬스체크 스크립트 | 서비스 상태 확인 |
| backup.sh | `scripts/backup.sh` | 백업 스크립트 | 데이터 백업 |

### 5.3 모니터링 설정
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| prometheus.yml | `monitoring/prometheus.yml` | Prometheus 설정 | 메트릭 수집 |
| grafana-dashboard.json | `monitoring/grafana-dashboard.json` | Grafana 대시보드 | 시각화 |

## 6. 문서 파일

### 6.1 API 문서
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| api-docs.yml | `docs/api-docs.yml` | OpenAPI 스펙 | API 문서 |
| postman-collection.json | `docs/postman-collection.json` | Postman 컬렉션 | API 테스트 |

### 6.2 시스템 문서
| 파일명 | 파일 경로 | 설명 | 용도 |
|--------|----------|------|-----|
| README.md | `README.md` | 프로젝트 설명서 | 프로젝트 개요 |
| CONTRIBUTING.md | `docs/CONTRIBUTING.md` | 기여 가이드 | 개발 참여 방법 |
| CHANGELOG.md | `docs/CHANGELOG.md` | 변경 로그 | 버전별 변경사항 |

## 프로그램 통계 요약

| 구분 | 개수 | 설명 |
|------|------|------|
| 컨트롤러 | 8개 | REST API 엔드포인트 |
| 서비스 | 10개 | 비즈니스 로직 처리 |
| 리포지토리 | 10개 | 데이터 액세스 계층 |
| 엔티티 | 11개 | 데이터베이스 모델 |
| DTO | 8개 | 데이터 전송 객체 |
| 설정 클래스 | 4개 | 애플리케이션 구성 |
| 보안 클래스 | 4개 | 인증/인가 처리 |
| 유틸리티 | 9개 | 공통 기능 |
| 테스트 클래스 | 7개 | 단위/통합 테스트 |
| **총 프로그램 수** | **71개** | **전체 Java 클래스** |

## 프로그램 복잡도 분석

### 높은 복잡도 (High Complexity)
- **HealthAnalysisService**: 복잡한 건강 데이터 분석 로직
- **EmergencyService**: 다양한 응급상황 처리 로직
- **NotificationService**: 멀티채널 알림 처리

### 중간 복잡도 (Medium Complexity)
- **MedicationService**: 복약 관리 및 순응도 추적
- **UserPatternService**: 사용자 행동 패턴 분석
- **AuthService**: 인증 및 보안 처리

### 낮은 복잡도 (Low Complexity)
- **Controller 클래스들**: 요청/응답 처리
- **Repository 클래스들**: 단순 데이터 액세스
- **DTO 클래스들**: 데이터 전송 객체 