# 배포 트러블슈팅 문서

## 날짜: 2025-09-30

## 문제 요약
GitHub Actions를 통한 자동 배포가 지속적으로 실패하며 `Access denied for user 'admin'@'172.31.43.183' (using password: YES)` 에러 발생

## 초기 상황
- 수동 배포: 성공
- GitHub Actions 자동 배포: 실패
- 에러: MySQL 접근 거부

## 트러블슈팅 과정

### 1단계: Docker Compose 의존성 문제 (해결됨)
**문제**: GitHub Actions 워크플로우가 docker-compose 명령어를 사용했으나 EC2에 미설치
```bash
bash: line 29: docker-compose: command not found
```

**시도한 해결책**:
- `.github/workflows/deploy.yml`을 수정하여 개별 `docker run` 명령어로 변경

**결과**: docker-compose 에러는 해결되었으나 여전히 DB 접근 실패

---

### 2단계: GitHub Secrets 전달 문제 조사
**문제**: GitHub Secrets가 SSH를 통해 EC2로 제대로 전달되지 않는 것으로 의심

**시도한 해결책들**:

#### 2-1. Heredoc 방식 시도
```yaml
ssh -o StrictHostKeyChecking=no -i /tmp/ssh_key $USER_NAME@$HOSTNAME bash -s <<EOF
docker run -d --name bifai-backend \
  -e DB_PASSWORD='${{ secrets.DB_PASSWORD }}'
EOF
```
**결과**: 실패 - 변수 확장 타이밍 문제

#### 2-2. 환경변수 Export 방식
```yaml
ssh -o StrictHostKeyChecking=no -i /tmp/ssh_key $USER_NAME@$HOSTNAME "
export DB_PASSWORD='${DB_PASSWORD}' &&
docker run -d --name bifai-backend \
  -e DB_PASSWORD=\${DB_PASSWORD}
"
```
**결과**: 실패 - SSH를 통한 환경변수 전달 문제

#### 2-3. .env 파일 방식
EC2에 `.env.prod` 파일을 생성하고 `--env-file` 사용
```bash
docker run -d --name bifai-backend --env-file /home/ubuntu/.env.prod
```
**결과**: 실패 - 보안상 부적절하고 여전히 DB 접근 실패

---

### 3단계: RDS 연결 및 권한 검증
**문제**: 비밀번호와 권한 설정 확인 필요

**검증 과정**:

#### 3-1. MySQL CLI 직접 연결 테스트
```bash
mysql -h bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com \
  -u admin -p"BifaiDB2025!" -e "SELECT 1;"
```
**결과**: ✅ 성공 - 비밀번호와 네트워크 연결은 정상

#### 3-2. 사용자 권한 확인
```sql
SELECT User, Host FROM mysql.user WHERE User="admin";
-- admin | %

SHOW GRANTS FOR "admin"@"%";
-- GRANT USAGE ON *.* TO `admin`@`%`
-- GRANT `rds_superuser_role`@`%` TO `admin`@`%`
```
**결과**: 권한 설정 정상

#### 3-3. 인증 플러그인 확인
```sql
SELECT user, host, plugin FROM mysql.user WHERE user="admin";
-- admin | % | mysql_native_password
```
**결과**: `mysql_native_password` 사용 중 - Java와 호환 가능

---

### 4단계: Docker 네트워크 격리 문제 조사
**문제**: Docker 컨테이너 간 네트워크 통신 문제 의심

**시도한 해결책들**:

#### 4-1. Host 네트워크 모드
```bash
docker run -d --name bifai-backend --network host
```
**결과**: 실패 - 여전히 같은 에러

#### 4-2. Docker Compose 사용
```bash
docker-compose -f docker-compose.prod.yml up -d
```
**결과**: 실패 - 같은 DB 접근 에러

---

### 5단계: 환경변수 전달 검증
**문제**: Docker 컨테이너가 환경변수를 제대로 받았는지 확인

**검증**:
```bash
docker inspect bifai-backend | grep -A 20 "Env"
# DB_USER=admin ✓
# DB_PASSWORD=BifaiDB2025! ✓
# DB_HOST=bifai-db-prod... ✓
```

```bash
docker exec bifai-backend env | grep DB_
# DB_PASSWORD=BifaiDB2025! ✓
# DB_USER=admin ✓
```

**결과**: 환경변수는 정확히 전달되었으나 여전히 Spring Boot에서 접근 실패

---

### 6단계: 최종 원인 발견 - 비밀번호 특수문자 문제

**핵심 발견**:
- MySQL CLI: 비밀번호 `BifaiDB2025!` 성공
- Spring Boot: 비밀번호 `BifaiDB2025!` 실패
- 차이점: 특수문자 `!` 처리 방식

**가설**: Spring Boot가 환경변수에서 비밀번호를 읽을 때 특수문자 파싱 문제

**검증 테스트**:
```bash
# 1. RDS 비밀번호 변경 (특수문자 제거)
mysql -h ... -u admin -p"BifaiDB2025!" \
  -e "ALTER USER 'admin'@'%' IDENTIFIED BY 'BifaiDB2025Secure';"

# 2. 새 비밀번호로 배포
docker run -d --name bifai-backend \
  -e DB_PASSWORD=BifaiDB2025Secure \
  ...

# 3. Health Check
curl http://43.200.49.171:8080/api/health
```

**결과**: ✅ **성공!**

```json
{
  "s": true,
  "d": {
    "message": "Application is running",
    "status": "UP"
  }
}
```

---

## 근본 원인 분석

### 왜 특수문자가 문제였나?

1. **Shell 변수 확장 시점**:
   ```bash
   # Shell에서 ! 는 히스토리 확장 문자
   DB_PASSWORD=BifaiDB2025!
   # Bash가 이를 특별하게 처리함
   ```

2. **YAML 파싱**:
   ```yaml
   # Spring Boot application-prod.yml
   password: ${DB_PASSWORD:BifaiDB2025!}
   # ! 가 YAML 특수문자로 처리될 수 있음
   ```

3. **환경변수 이스케이핑**:
   - Docker run에서 `-e DB_PASSWORD=BifaiDB2025!`
   - Shell이 `!`를 히스토리 확장으로 해석
   - 실제 전달되는 값이 달라짐

### 왜 MySQL CLI는 성공했나?
```bash
mysql -u admin -p"BifaiDB2025!"
# -p 옵션에서는 따옴표로 감싸져 있어 Shell 확장 방지
# MySQL client가 직접 비밀번호 처리
```

---

## 최종 해결책

### 1. RDS 비밀번호 변경
```sql
ALTER USER 'admin'@'%' IDENTIFIED BY 'BifaiDB2025Secure';
```
- 특수문자 제거
- 영문자와 숫자만 사용

### 2. GitHub Secret 업데이트
```bash
echo "BifaiDB2025Secure" | gh secret set DB_PASSWORD
```

### 3. Docker Compose 기반 배포로 전환
**이유**:
- 네트워크 자동 설정
- 컨테이너 간 의존성 관리
- 환경변수 관리 간소화
- 재현 가능한 배포 환경

**docker-compose.prod.yml**:
```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: bifai-redis
    network_mode: host

  app:
    image: ${ECR_REGISTRY_URL}/hanium/bifai:latest
    container_name: bifai-backend
    network_mode: host
    environment:
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      # ... 기타 환경변수
    depends_on:
      redis:
        condition: service_healthy
```

### 4. GitHub Actions 워크플로우 수정
```yaml
- name: Transfer docker-compose file
  run: |
    scp -i /tmp/ssh_key docker-compose.prod.yml $USER@$HOST:/home/ubuntu/

- name: Deploy to EC2
  env:
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
    # ... 기타 secrets
  run: |
    ssh -i /tmp/ssh_key $USER@$HOST "
      cd /home/ubuntu &&
      export DB_PASSWORD=$DB_PASSWORD &&
      docker-compose -f docker-compose.prod.yml down &&
      docker-compose -f docker-compose.prod.yml up -d
    "
```

---

## 교훈 및 베스트 프랙티스

### 1. 비밀번호 정책
❌ **피해야 할 것**:
- Shell 특수문자: `!`, `$`, `` ` ``, `\`, `"`, `'`
- YAML 특수문자: `:`, `{`, `}`, `[`, `]`, `,`, `&`, `*`, `#`, `?`, `|`, `-`, `<`, `>`, `=`, `%`, `@`

✅ **권장사항**:
- 영문 대소문자
- 숫자
- 안전한 특수문자: `_`, `-` (단, 처음이나 끝에는 사용 안 함)
- 최소 16자 이상

### 2. 환경변수 전달 검증
```bash
# 항상 컨테이너 내부에서 실제 값 확인
docker exec container_name env | grep PASSWORD

# 또는
docker inspect container_name | grep -A 10 "Env"
```

### 3. 트러블슈팅 순서
1. ✅ 직접 연결 테스트 (MySQL CLI)
2. ✅ 권한 및 네트워크 확인
3. ✅ 환경변수 전달 검증
4. ✅ 로그 상세 분석
5. ✅ 비교 테스트 (작동하는 것 vs 실패하는 것)
6. ✅ 점진적 단순화 (변수 제거하며 테스트)

### 4. Docker Compose 사용 이점
- **일관성**: 로컬, 스테이징, 프로덕션 환경 동일
- **재현성**: 동일한 명령어로 배포
- **네트워크**: 자동 네트워크 구성
- **의존성**: 컨테이너 시작 순서 관리
- **버전 관리**: compose 파일을 Git으로 관리

### 5. GitHub Actions 시크릿 관리
```yaml
# ❌ 나쁜 예: 직접 문자열로 전달
-e DB_PASSWORD=${{ secrets.DB_PASSWORD }}

# ✅ 좋은 예: 환경변수로 먼저 설정
env:
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
run: |
  export DB_PASSWORD=$DB_PASSWORD
  docker-compose up -d
```

---

## 디버깅에 유용했던 명령어

### RDS 연결 테스트
```bash
# 간단한 쿼리로 연결 확인
mysql -h $RDS_HOST -u $USER -p"$PASSWORD" -e "SELECT 1;"

# 사용자 정보 확인
mysql -h $RDS_HOST -u $USER -p"$PASSWORD" \
  -e "SELECT user, host, plugin FROM mysql.user WHERE user='admin';"

# 권한 확인
mysql -h $RDS_HOST -u $USER -p"$PASSWORD" \
  -e "SHOW GRANTS FOR 'admin'@'%';"
```

### Docker 디버깅
```bash
# 컨테이너 환경변수 확인
docker exec container_name env | grep DB

# 컨테이너 상세 정보
docker inspect container_name | jq '.[] | .Config.Env'

# 실시간 로그
docker logs -f container_name

# 컨테이너 안으로 진입
docker exec -it container_name bash

# 네트워크 확인
docker network ls
docker network inspect bridge
```

### Spring Boot 디버깅
```bash
# 로그 레벨 올려서 상세 정보 확인
docker logs container_name 2>&1 | grep -i "hikari\|datasource\|mysql"

# 특정 에러 필터링
docker logs container_name 2>&1 | grep -i "error\|exception\|failed"

# 애플리케이션 시작 확인
docker logs container_name 2>&1 | grep "Started.*Application"
```

---

## 타임라인

| 시간 | 단계 | 상태 | 비고 |
|------|------|------|------|
| 10:20 | 초기 문제 발견 | ❌ | docker-compose not found |
| 10:30 | docker run 변경 | ❌ | Access denied 계속 발생 |
| 10:45 | Heredoc 시도 | ❌ | 변수 확장 실패 |
| 11:00 | Export 방식 | ❌ | SSH 전달 문제 |
| 11:15 | .env 파일 | ❌ | 여전히 실패 |
| 11:30 | RDS 연결 검증 | ✅ | MySQL CLI 성공 |
| 11:45 | 환경변수 검증 | ✅ | Docker에 정상 전달 확인 |
| 12:00 | 네트워크 격리 조사 | ❌ | host 모드도 실패 |
| 12:30 | Docker Compose 설치 | ✅ | EC2에 설치 완료 |
| 12:45 | 비밀번호 특수문자 의심 | 💡 | 가설 수립 |
| 13:00 | 비밀번호 변경 | ✅ | **문제 해결!** |
| 13:05 | 최종 배포 성공 | ✅ | Health Check 통과 |

**총 소요 시간**: 약 2시간 45분

---

## 예방 조치

### 1. 비밀번호 생성 스크립트
```bash
#!/bin/bash
# generate_safe_password.sh
# 안전한 비밀번호 생성 (특수문자 제외)

LENGTH=${1:-32}
PASSWORD=$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c $LENGTH)
echo "Generated password: $PASSWORD"
echo "Length: ${#PASSWORD}"
```

### 2. 환경변수 검증 스크립트
```bash
#!/bin/bash
# validate_env_vars.sh

REQUIRED_VARS=("DB_HOST" "DB_USER" "DB_PASSWORD" "JWT_SECRET")

for VAR in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!VAR}" ]; then
        echo "❌ $VAR is not set"
        exit 1
    fi

    # 특수문자 검사
    if [[ "${!VAR}" =~ [\!\$\`\\\"\'] ]]; then
        echo "⚠️  WARNING: $VAR contains shell special characters"
    fi
done

echo "✅ All environment variables validated"
```

### 3. Pre-deployment 체크리스트
```markdown
## 배포 전 체크리스트

- [ ] RDS 연결 테스트 (MySQL CLI)
- [ ] 비밀번호 특수문자 확인
- [ ] GitHub Secrets 업데이트 확인
- [ ] docker-compose.prod.yml 최신 버전 확인
- [ ] ECR 이미지 빌드 및 푸시 완료
- [ ] 로컬에서 docker-compose 테스트
- [ ] Health Check 엔드포인트 동작 확인
```

### 4. 모니터링 설정
```yaml
# docker-compose.prod.yml에 추가
services:
  app:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

---

## 참고 자료

### 공식 문서
- [MySQL Environment Variables](https://dev.mysql.com/doc/refman/8.0/en/environment-variables.html)
- [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [GitHub Actions Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

### 관련 이슈
- HikariCP connection pool timeout
- Docker network isolation
- GitHub Actions secret masking
- Shell special character escaping

---

## 결론

이번 트러블슈팅의 핵심 교훈:

1. **간단한 것부터 의심하라**: 특수문자 하나가 2시간 45분의 디버깅을 유발
2. **비교 테스트의 중요성**: MySQL CLI는 되는데 애플리케이션은 안 되는 차이점 분석
3. **환경변수 전달 경로 이해**: GitHub Actions → SSH → Shell → Docker → Spring Boot
4. **Docker Compose 표준화**: 일관된 배포 환경 구축
5. **문서화**: 다음 문제 발생 시 빠른 해결을 위한 지식 축적

**최종 상태**: ✅ GitHub Actions 자동 배포 성공