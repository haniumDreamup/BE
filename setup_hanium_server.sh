#!/bin/bash

# 한이음 EC2 서버 초기 설정 스크립트
echo "한이음 BIF-AI 프로젝트 서버 설정 시작..."

# 1. 필요한 디렉토리 생성
echo "1. 디렉토리 구조 생성..."
mkdir -p ~/bifai/{backend,uploads,logs,scripts}
mkdir -p ~/bifai/uploads/{images,videos,temp}

# 2. Java 프로젝트 설정 (sudo 권한 없을 경우)
if ! command -v java &> /dev/null; then
    echo "Java가 설치되어 있지 않습니다."
    echo "관리자에게 Java 17 설치를 요청하세요."
else
    echo "Java 버전: $(java -version 2>&1 | head -1)"
fi

# 3. MySQL 연결 테스트 (로컬 또는 원격)
echo "3. MySQL 연결 테스트..."
if command -v mysql &> /dev/null; then
    echo "MySQL 클라이언트 발견"
    # 로컬 MySQL 테스트
    mysql -u root -p -e "SELECT VERSION();" 2>/dev/null && echo "MySQL 연결 성공" || echo "MySQL 연결 실패"
else
    echo "MySQL 클라이언트가 설치되어 있지 않습니다."
fi

# 4. 포트 확인 (8080 사용 가능한지)
echo "4. 사용 가능한 포트 확인..."
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️ 포트 8080이 사용 중입니다."
    echo "다른 포트를 사용하거나 기존 프로세스를 종료하세요."
else
    echo "✅ 포트 8080 사용 가능"
fi

# 5. 시작 스크립트 생성
echo "5. 시작 스크립트 생성..."
cat > ~/bifai/scripts/start.sh << 'EOF'
#!/bin/bash
cd ~/bifai/backend
nohup java -jar bifai-backend.jar \
  --server.port=8080 \
  --spring.profiles.active=prod \
  > ~/bifai/logs/app.log 2>&1 &
echo $! > ~/bifai/app.pid
echo "BIF-AI 백엔드 시작됨. PID: $(cat ~/bifai/app.pid)"
EOF

cat > ~/bifai/scripts/stop.sh << 'EOF'
#!/bin/bash
if [ -f ~/bifai/app.pid ]; then
    kill $(cat ~/bifai/app.pid)
    rm ~/bifai/app.pid
    echo "BIF-AI 백엔드 종료됨"
else
    echo "실행 중인 프로세스 없음"
fi
EOF

chmod +x ~/bifai/scripts/*.sh

# 6. 환경 변수 파일 생성
echo "6. 환경 설정 파일 생성..."
cat > ~/bifai/backend/.env << 'EOF'
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=bifai_db
DB_USER=bifai_user
DB_PASSWORD=changeMe!

# File Upload
UPLOAD_DIR=/home/hanium_75/bifai/uploads
MAX_FILE_SIZE=10485760

# Server
SERVER_PORT=8080
SERVER_HOST=0.0.0.0
EOF

# 7. 방화벽 규칙 확인 (iptables)
echo "7. 방화벽 상태 확인..."
sudo iptables -L -n 2>/dev/null || echo "iptables 권한 없음 - 관리자에게 포트 개방 요청 필요"

# 8. 서버 정보 저장
echo "8. 서버 정보 저장..."
cat > ~/bifai/SERVER_INFO.txt << EOF
========================================
한이음 BIF-AI 프로젝트 서버 정보
========================================
생성일: $(date)
사용자: $(whoami)
홈 디렉토리: $HOME
작업 디렉토리: ~/bifai
Public IP: $(curl -s ifconfig.me)
Private IP: $(hostname -I | awk '{print $1}')

디렉토리 구조:
~/bifai/
  ├── backend/       # Spring Boot JAR 파일
  ├── uploads/       # 업로드된 파일
  │   ├── images/    
  │   └── videos/    
  ├── logs/          # 애플리케이션 로그
  └── scripts/       # 관리 스크립트
      ├── start.sh   # 서버 시작
      └── stop.sh    # 서버 종료

사용법:
1. JAR 파일 업로드: scp bifai-backend.jar hanium_75@etechcloud.co.kr:~/bifai/backend/
2. 서버 시작: ~/bifai/scripts/start.sh
3. 서버 종료: ~/bifai/scripts/stop.sh
4. 로그 확인: tail -f ~/bifai/logs/app.log
========================================
EOF

echo "✅ 설정 완료!"
echo "📄 서버 정보는 ~/bifai/SERVER_INFO.txt 파일을 확인하세요."
ls -la ~/bifai/