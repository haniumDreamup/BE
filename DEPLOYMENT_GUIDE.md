# BIF-AI Backend 배포 가이드

## 목차
1. [사전 준비](#사전-준비)
2. [로컬 배포](#로컬-배포)
3. [프로덕션 배포](#프로덕션-배포)
4. [AWS 배포](#aws-배포)
5. [모니터링](#모니터링)
6. [문제 해결](#문제-해결)

## 사전 준비

### 필수 요구사항
- Java 17+
- Docker & Docker Compose
- MySQL 8.0+
- Redis
- 최소 2GB RAM (권장 4GB+)

### 환경 설정

1. **환경 변수 설정**
```bash
# .env.example을 복사하여 .env 생성
cp .env.example .env

# .env 파일 편집하여 실제 값 입력
vim .env
```

2. **필수 환경 변수**
- `DB_PASSWORD`: MySQL 비밀번호
- `JWT_SECRET`: 최소 64자 이상의 보안 키
- `OPENAI_API_KEY`: OpenAI API 키 (선택사항)

## 로컬 배포

### 1. Docker Compose 사용 (권장)

```bash
# 전체 스택 실행 (MySQL + Redis + App)
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 서비스 중지
docker-compose down

# 데이터 포함 완전 삭제
docker-compose down -v
```

### 2. 로컬 개발 환경

```bash
# MySQL과 Redis는 Docker로 실행
docker-compose up -d mysql redis

# Spring Boot 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar build/libs/bifai-backend-*.jar --spring.profiles.active=dev
```

### 3. 데이터베이스 초기화

```bash
# Flyway 마이그레이션 실행
./gradlew flywayMigrate

# 또는 Docker 컨테이너에서 직접 실행
docker exec -it bifai-mysql mysql -u root -p
```

## 프로덕션 배포

### 1. 빌드 및 이미지 생성

```bash
# 프로덕션용 Docker 이미지 빌드
docker build -t bifai-backend:latest .

# 이미지 태그
docker tag bifai-backend:latest your-registry/bifai-backend:v1.0.0

# 레지스트리 푸시
docker push your-registry/bifai-backend:v1.0.0
```

### 2. 프로덕션 환경 변수 설정

```bash
# 프로덕션용 .env 파일 생성
cat > .env.prod <<EOF
DB_HOST=your-rds-endpoint.amazonaws.com
DB_PASSWORD=secure_password_here
REDIS_HOST=your-elasticache-endpoint.amazonaws.com
JWT_SECRET=your_production_jwt_secret_minimum_64_chars
OPENAI_API_KEY=sk-your-production-key
EOF
```

### 3. 프로덕션 실행

```bash
# 프로덕션 프로파일로 실행
docker run -d \
  --name bifai-app \
  --env-file .env.prod \
  -p 8080:8080 \
  -v /var/log/bifai:/var/log/bifai \
  -v /var/bifai/files:/var/bifai/files \
  bifai-backend:latest
```

## AWS 배포

### 1. AWS 리소스 준비

#### RDS (MySQL)
```bash
# RDS 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier bifai-mysql \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --allocated-storage 20 \
  --master-username admin \
  --master-user-password YourSecurePassword
```

#### ElastiCache (Redis)
```bash
# Redis 클러스터 생성
aws elasticache create-cache-cluster \
  --cache-cluster-id bifai-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1
```

#### EC2 인스턴스
```bash
# EC2 인스턴스 시작 후
ssh ec2-user@your-ec2-ip

# Docker 설치
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo usermod -a -G docker ec2-user

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. ECS/Fargate 배포 (선택사항)

```bash
# Task Definition 생성
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json

# 서비스 생성
aws ecs create-service \
  --cluster bifai-cluster \
  --service-name bifai-backend \
  --task-definition bifai-backend:1 \
  --desired-count 2 \
  --launch-type FARGATE
```

### 3. Application Load Balancer 설정

```bash
# ALB 생성
aws elbv2 create-load-balancer \
  --name bifai-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx

# Target Group 생성
aws elbv2 create-target-group \
  --name bifai-targets \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxx \
  --health-check-path /actuator/health
```

## 모니터링

### 1. 헬스 체크

```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 상세 메트릭
curl http://localhost:8080/actuator/metrics
```

### 2. 로그 확인

```bash
# Docker 로그
docker logs -f bifai-app

# 파일 로그
tail -f /var/log/bifai/application.log
```

### 3. CloudWatch 설정

```json
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/bifai/application.log",
            "log_group_name": "/aws/ec2/bifai",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  }
}
```

## 문제 해결

### 일반적인 문제들

#### 1. 데이터베이스 연결 실패
```bash
# MySQL 연결 테스트
mysql -h localhost -u bifai_user -p

# 연결 설정 확인
echo "SHOW VARIABLES LIKE 'max_connections';" | mysql -u root -p
```

#### 2. 메모리 부족
```bash
# JVM 힙 메모리 증가
java -Xms1024m -Xmx2048m -jar app.jar

# Docker 메모리 제한
docker run -m 2g bifai-backend
```

#### 3. 포트 충돌
```bash
# 사용 중인 포트 확인
sudo lsof -i :8080

# 다른 포트로 실행
SERVER_PORT=8081 java -jar app.jar
```

### 롤백 절차

```bash
# 이전 버전으로 롤백
docker stop bifai-app
docker run -d --name bifai-app your-registry/bifai-backend:previous-version

# 데이터베이스 롤백 (Flyway)
./gradlew flywayUndo
```

## 보안 체크리스트

- [ ] 프로덕션 환경 변수 설정 완료
- [ ] JWT Secret 64자 이상 설정
- [ ] 데이터베이스 비밀번호 변경
- [ ] HTTPS 인증서 설치
- [ ] 방화벽 규칙 설정
- [ ] 백업 정책 수립
- [ ] 모니터링 알람 설정

## 성능 최적화

### JVM 튜닝
```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

### 데이터베이스 인덱스
```sql
-- 자주 조회되는 컬럼에 인덱스 추가
CREATE INDEX idx_user_login ON users(username, password_hash);
CREATE INDEX idx_activity_user_date ON activity_logs(user_id, activity_date);
```

### Redis 캐싱
```yaml
# application-prod.yml
spring.cache.redis.time-to-live: 3600000  # 1시간
```

## 지원

문제 발생 시:
1. 로그 확인: `/var/log/bifai/application.log`
2. GitHub Issues: [프로젝트 저장소]/issues
3. 문서 참조: `/docs` 디렉토리

---
마지막 업데이트: 2025-08-26