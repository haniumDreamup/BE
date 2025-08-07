#!/bin/bash

# 테스트용 Docker 환경 관리 스크립트

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 함수: 성공 메시지
success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 함수: 에러 메시지
error() {
    echo -e "${RED}✗ $1${NC}"
}

# 함수: 정보 메시지
info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Docker Compose 파일 경로
COMPOSE_FILE="docker-compose.test.yml"

# 명령어 처리
case "$1" in
    start)
        info "테스트 환경 시작 중..."
        docker-compose -f $COMPOSE_FILE up -d
        
        # MySQL과 Redis가 준비될 때까지 대기
        info "서비스가 준비될 때까지 대기 중..."
        sleep 5
        
        # 헬스체크
        if docker-compose -f $COMPOSE_FILE ps | grep -q "healthy"; then
            success "테스트 환경이 준비되었습니다!"
            info "MySQL: localhost:3308"
            info "Redis: localhost:6381"
        else
            error "서비스 시작 실패"
            docker-compose -f $COMPOSE_FILE logs
        fi
        ;;
        
    stop)
        info "테스트 환경 종료 중..."
        docker-compose -f $COMPOSE_FILE down
        success "테스트 환경이 종료되었습니다."
        ;;
        
    restart)
        $0 stop
        $0 start
        ;;
        
    test)
        # Docker 환경이 실행 중인지 확인
        if ! docker-compose -f $COMPOSE_FILE ps | grep -q "Up"; then
            info "테스트 환경을 시작합니다..."
            $0 start
        fi
        
        info "테스트 실행 중..."
        ./gradlew clean test
        ;;
        
    status)
        docker-compose -f $COMPOSE_FILE ps
        ;;
        
    logs)
        docker-compose -f $COMPOSE_FILE logs -f
        ;;
        
    clean)
        info "테스트 환경 완전 제거 중..."
        docker-compose -f $COMPOSE_FILE down -v
        success "볼륨을 포함한 모든 리소스가 제거되었습니다."
        ;;
        
    *)
        echo "사용법: $0 {start|stop|restart|test|status|logs|clean}"
        echo ""
        echo "Commands:"
        echo "  start   - 테스트용 MySQL과 Redis 컨테이너 시작"
        echo "  stop    - 컨테이너 중지"
        echo "  restart - 컨테이너 재시작"
        echo "  test    - 컨테이너 시작 후 테스트 실행"
        echo "  status  - 컨테이너 상태 확인"
        echo "  logs    - 컨테이너 로그 보기"
        echo "  clean   - 컨테이너와 볼륨 완전 제거"
        exit 1
        ;;
esac