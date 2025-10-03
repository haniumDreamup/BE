# BIF-AI Reminder Backend

## 프로젝트 개요
BIF-AI Reminder는 경계선 지능(Borderline Intellectual Functioning, BIF) 대상자들을 위한 상황 인지 보조 시스템입니다. 이 백엔드 시스템은 AI 기반의 인지 보조 플랫폼을 구동합니다.

## 주요 기능
- 🧠 **인지 보조**: 실시간 이미지 분석 및 상황 안내
- 📅 **일정 관리**: 패턴 학습 기반 지능형 리마인더
- 🚨 **안전 모니터링**: 낙상 감지, 긴급 알림, GPS 추적
- 🗺️ **내비게이션 지원**: 실내외 단순화된 길찾기
- 💬 **사회적 상호작용**: 감정 인식 및 대화 보조

## 기술 스택
- **Backend**: Spring Boot 3.5.0
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Cloud**: AWS (EC2, RDS, S3)
- **AI**: OpenAI API
- **Build**: Gradle

## 시작하기

### 사전 요구사항
- Java 17+
- MySQL 8.0+
- Redis
- AWS 계정
- OpenAI API 키

### 설치
```bash
# 저장소 클론
git clone [repository-url]
cd BE

# 환경 변수 설정
cp .env.example .env
# .env 파일을 편집하여 필요한 값 설정

# 의존성 설치
./gradlew build
```

### 실행
```bash
# 개발 서버 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

## 프로젝트 구조
```
BE/
├── bifai-backend/          # Spring Boot 애플리케이션
├── documents/              # 시스템 설계 문서
├── scripts/                # 스크립트 및 PRD
├── .taskmaster/           # 작업 관리 시스템
└── CLAUDE.md              # Claude AI를 위한 컨텍스트
```

## API 문서
API 문서는 애플리케이션 실행 후 다음 주소에서 확인 가능합니다:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## AI 서비스 설정
Mock 데이터를 실제 AI 서비스로 연동하는 방법은 다음 문서를 참고하세요:
- **[AI 서비스 실제 연동 가이드](docs/AI_SERVICES_SETUP.md)**
  - Google Cloud Vision API 설정
  - OpenAI ChatGPT API 설정
  - Firebase Cloud Messaging 설정
  - 환경 변수 구성
  - 비용 관리 및 보안 권장 사항

## 작업 관리
```bash
# 모든 작업 보기
npx task-master list

# 다음 작업 확인
npx task-master next

# 작업 상태 업데이트
npx task-master set-status --id=<id> --status=in-progress
```

## 기여하기
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 라이선스
이 프로젝트는 MIT 라이선스 하에 있습니다.

## 팀 정보
- **이호준**: 팀 리더, 백엔드 개발, LLM 통합
- **신동범**: 안드로이드 앱 개발, LLM 지원
- **나현, 이유민**: 디바이스 개발

## 문의
프로젝트 관련 문의사항은 이슈 트래커를 이용해 주세요.

## CI/CD 상태
- ✅ GitHub Actions 파이프라인 설정 완료
- ✅ EC2 자동 배포 설정 완료
- ✅ ECR Docker 이미지 레지스트리 연동
- ✅ AWS 자격증명 설정 완료
- ✅ RDS VPC 연결 문제 해결 완료
