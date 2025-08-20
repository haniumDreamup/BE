#!/bin/bash

echo "========================================="
echo "한이음 AWS EC2 서버 환경 체크 스크립트"
echo "========================================="

echo -e "\n1. 시스템 정보:"
echo "-------------------"
uname -a
lsb_release -a 2>/dev/null || cat /etc/os-release

echo -e "\n2. 현재 사용자 및 권한:"
echo "-------------------"
whoami
id
sudo -l 2>/dev/null || echo "sudo 권한 없음"

echo -e "\n3. 디스크 사용량:"
echo "-------------------"
df -h

echo -e "\n4. 메모리 상태:"
echo "-------------------"
free -h

echo -e "\n5. 설치된 주요 소프트웨어:"
echo "-------------------"
echo -n "Java: "; java -version 2>&1 | head -1
echo -n "Python: "; python3 --version 2>/dev/null || echo "설치 안됨"
echo -n "Node.js: "; node --version 2>/dev/null || echo "설치 안됨"
echo -n "Docker: "; docker --version 2>/dev/null || echo "설치 안됨"
echo -n "MySQL: "; mysql --version 2>/dev/null || echo "설치 안됨"
echo -n "Nginx: "; nginx -v 2>&1 | head -1 || echo "설치 안됨"

echo -e "\n6. 네트워크 정보:"
echo "-------------------"
ip addr | grep inet | grep -v inet6
curl -s ifconfig.me && echo " (Public IP)"

echo -e "\n7. 열린 포트:"
echo "-------------------"
ss -tuln | grep LISTEN || netstat -tuln | grep LISTEN

echo -e "\n8. 실행 중인 주요 프로세스:"
echo "-------------------"
ps aux | grep -E "java|python|node|mysql|nginx|docker" | grep -v grep

echo -e "\n9. 홈 디렉토리 구조:"
echo "-------------------"
ls -la ~/

echo -e "\n10. 쓰기 가능한 디렉토리:"
echo "-------------------"
echo "/tmp: $([ -w /tmp ] && echo "쓰기 가능" || echo "쓰기 불가")"
echo "/home/hanium_75: $([ -w ~ ] && echo "쓰기 가능" || echo "쓰기 불가")"
echo "/var/www: $([ -w /var/www ] 2>/dev/null && echo "쓰기 가능" || echo "쓰기 불가 또는 없음")"
echo "/opt: $([ -w /opt ] && echo "쓰기 가능" || echo "쓰기 불가")"

echo -e "\n========================================="
echo "체크 완료!"
echo "========================================="