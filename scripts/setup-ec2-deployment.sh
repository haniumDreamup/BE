#!/bin/bash

# BIF-AI Backend EC2 배포 설정 스크립트
set -e

REGION="ap-northeast-2"
REPOSITORY_NAME="bifai-backend"

echo "🚀 BIF-AI Backend EC2 배포 환경 설정을 시작합니다..."

# 1. ECR 리포지토리 생성
echo "📦 ECR 리포지토리 생성 중..."
aws ecr describe-repositories --repository-names $REPOSITORY_NAME --region $REGION 2>/dev/null || \
aws ecr create-repository \
    --repository-name $REPOSITORY_NAME \
    --region $REGION \
    --image-scanning-configuration scanOnPush=true

# 계정 ID 가져오기
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --region $REGION)

echo ""
echo "✅ ECR 리포지토리 설정이 완료되었습니다!"
echo ""
echo "📋 설정 정보:"
echo "  - AWS Region: $REGION"
echo "  - Account ID: $ACCOUNT_ID"
echo "  - ECR Repository: $REPOSITORY_NAME"
echo "  - ECR URI: $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPOSITORY_NAME"
echo ""
echo "🔧 GitHub Secrets에 다음 값들을 추가하세요:"
echo ""
echo "Required Secrets:"
echo "  - AWS_ACCESS_KEY_ID: [AWS Access Key]"
echo "  - AWS_SECRET_ACCESS_KEY: [AWS Secret Key]"
echo "  - AWS_ACCOUNT_ID: $ACCOUNT_ID"
echo ""
echo "EC2 Connection Secrets:"
echo "  - EC2_HOST: [EC2 인스턴스 Public IP]"
echo "  - EC2_USER: ubuntu (또는 ec2-user)"
echo "  - EC2_PRIVATE_KEY: [SSH Private Key 내용]"
echo ""
echo "Application Secrets:"
echo "  - DB_USER: bifai_user"
echo "  - DB_PASSWORD: [RDS 비밀번호]"
echo "  - JWT_SECRET: [JWT 시크릿 키]"
echo "  - OPENAI_API_KEY: [OpenAI API 키]"
echo ""
echo "💡 EC2 인스턴스 준비사항:"
echo "1. Docker 설치:"
echo "   sudo apt-get update"
echo "   sudo apt-get install -y docker.io"
echo "   sudo systemctl start docker"
echo "   sudo systemctl enable docker"
echo "   sudo usermod -aG docker \$USER"
echo ""
echo "2. AWS CLI 설치:"
echo "   curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\""
echo "   unzip awscliv2.zip"
echo "   sudo ./aws/install"
echo ""
echo "3. AWS CLI 설정:"
echo "   aws configure"
echo ""
echo "4. 방화벽 설정 (포트 8080 오픈):"
echo "   sudo ufw allow 8080"
echo ""
echo "🚀 모든 설정 완료 후 GitHub에 Push하면 자동 배포됩니다!"