#!/bin/bash

echo "🚀 BIF-AI 앱 실행 스크립트"
echo "================================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 백엔드 서버 실행 함수
start_backend() {
    echo -e "${YELLOW}📦 백엔드 서버 시작 중...${NC}"
    cd /Users/ihojun/Desktop/javaWorkSpace/BE
    
    # 백엔드가 이미 실행 중인지 확인
    if lsof -i:8080 > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 백엔드 서버가 이미 실행 중입니다${NC}"
    else
        echo "백엔드 서버를 시작합니다..."
        ./gradlew bootRun &
        BACKEND_PID=$!
        
        # 서버가 시작될 때까지 대기
        echo "서버가 시작되기를 기다리는 중..."
        sleep 10
        
        # 서버 상태 확인
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 백엔드 서버가 성공적으로 시작되었습니다${NC}"
        else
            echo -e "${RED}❌ 백엔드 서버 시작 실패${NC}"
            exit 1
        fi
    fi
}

# Flutter 앱 실행 함수
start_flutter() {
    echo -e "${YELLOW}📱 Flutter 앱 시작 중...${NC}"
    cd /Users/ihojun/Desktop/javaWorkSpace/BE/bifai_app
    
    # 패키지 업데이트
    echo "Flutter 패키지 업데이트 중..."
    flutter pub get
    
    # 사용 가능한 디바이스 확인
    echo -e "${YELLOW}사용 가능한 디바이스:${NC}"
    flutter devices
    
    echo ""
    echo "실행할 플랫폼을 선택하세요:"
    echo "1) iOS Simulator"
    echo "2) Android Emulator"
    echo "3) Chrome (Web)"
    echo "4) 모든 플랫폼"
    read -p "선택 (1-4): " choice
    
    case $choice in
        1)
            echo -e "${GREEN}iOS Simulator에서 실행 중...${NC}"
            flutter run -d ios
            ;;
        2)
            echo -e "${GREEN}Android Emulator에서 실행 중...${NC}"
            flutter run -d android
            ;;
        3)
            echo -e "${GREEN}Chrome에서 실행 중...${NC}"
            flutter run -d chrome --web-port 3000
            ;;
        4)
            echo -e "${GREEN}모든 플랫폼에서 실행 중...${NC}"
            flutter run -d all
            ;;
        *)
            echo -e "${RED}잘못된 선택입니다${NC}"
            exit 1
            ;;
    esac
}

# 종료 핸들러
cleanup() {
    echo -e "\n${YELLOW}🛑 종료 중...${NC}"
    
    # 백엔드 서버 종료
    if [ ! -z "$BACKEND_PID" ]; then
        echo "백엔드 서버 종료 중..."
        kill $BACKEND_PID 2>/dev/null
    fi
    
    # 8080 포트 사용 프로세스 종료
    if lsof -i:8080 > /dev/null 2>&1; then
        echo "8080 포트 프로세스 종료 중..."
        kill $(lsof -t -i:8080) 2>/dev/null
    fi
    
    echo -e "${GREEN}✅ 정상적으로 종료되었습니다${NC}"
    exit 0
}

# 시그널 트랩 설정
trap cleanup INT TERM

# 메인 실행
echo "BIF-AI 개발 환경을 시작합니다..."
echo ""

# 백엔드 시작
start_backend

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}백엔드 서버: http://localhost:8080${NC}"
echo -e "${GREEN}Swagger UI: http://localhost:8080/swagger-ui.html${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# Flutter 앱 시작
start_flutter

# 프로세스 유지
wait