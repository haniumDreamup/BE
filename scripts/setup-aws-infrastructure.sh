#!/bin/bash

# BIF-AI Backend AWS 인프라 설정 스크립트
set -e

# 변수 설정
REGION="ap-northeast-2"
CLUSTER_NAME="bifai-cluster"
SERVICE_NAME="bifai-backend-service"
REPOSITORY_NAME="bifai-backend"
VPC_NAME="bifai-vpc"
SUBNET_NAME="bifai-subnet"
SECURITY_GROUP_NAME="bifai-sg"
LOG_GROUP_NAME="/ecs/bifai-backend"

echo "🚀 BIF-AI Backend AWS 인프라 설정을 시작합니다..."

# 1. ECR 리포지토리 생성
echo "📦 ECR 리포지토리 생성 중..."
aws ecr describe-repositories --repository-names $REPOSITORY_NAME --region $REGION 2>/dev/null || \
aws ecr create-repository \
    --repository-name $REPOSITORY_NAME \
    --region $REGION \
    --image-scanning-configuration scanOnPush=true

# 2. CloudWatch 로그 그룹 생성
echo "📝 CloudWatch 로그 그룹 생성 중..."
aws logs describe-log-groups --log-group-name-prefix $LOG_GROUP_NAME --region $REGION | grep -q $LOG_GROUP_NAME || \
aws logs create-log-group \
    --log-group-name $LOG_GROUP_NAME \
    --region $REGION

# 3. ECS 클러스터 생성
echo "🌐 ECS 클러스터 생성 중..."
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $REGION 2>/dev/null | grep -q "ACTIVE" || \
aws ecs create-cluster \
    --cluster-name $CLUSTER_NAME \
    --capacity-providers FARGATE FARGATE_SPOT \
    --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1 \
    --region $REGION

# 4. VPC 및 서브넷 정보 가져오기
echo "🔍 기본 VPC 정보 조회 중..."
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=is-default,Values=true" --query "Vpcs[0].VpcId" --output text --region $REGION)
SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[*].SubnetId" --output text --region $REGION)

echo "VPC ID: $VPC_ID"
echo "Subnet IDs: $SUBNET_IDS"

# 5. 보안 그룹 생성
echo "🔒 보안 그룹 생성 중..."
SG_ID=$(aws ec2 describe-security-groups --filters "Name=group-name,Values=$SECURITY_GROUP_NAME" --query "SecurityGroups[0].GroupId" --output text --region $REGION 2>/dev/null || echo "None")

if [ "$SG_ID" = "None" ]; then
    SG_ID=$(aws ec2 create-security-group \
        --group-name $SECURITY_GROUP_NAME \
        --description "Security group for BIF-AI Backend" \
        --vpc-id $VPC_ID \
        --query "GroupId" \
        --output text \
        --region $REGION)
        
    # 인바운드 규칙 추가
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 8080 \
        --cidr 0.0.0.0/0 \
        --region $REGION
        
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region $REGION
        
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 443 \
        --cidr 0.0.0.0/0 \
        --region $REGION
fi

echo "Security Group ID: $SG_ID"

# 6. IAM 역할 생성 (실행 역할)
echo "👤 IAM 실행 역할 생성 중..."
aws iam get-role --role-name ecsTaskExecutionRole --region $REGION 2>/dev/null || \
aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "ecs-tasks.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }' \
    --region $REGION

# 실행 역할에 정책 연결
aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy \
    --region $REGION

# 7. IAM 태스크 역할 생성
echo "👤 IAM 태스크 역할 생성 중..."
aws iam get-role --role-name ecsTaskRole --region $REGION 2>/dev/null || \
aws iam create-role \
    --role-name ecsTaskRole \
    --assume-role-policy-document '{
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {
                    "Service": "ecs-tasks.amazonaws.com"
                },
                "Action": "sts:AssumeRole"
            }
        ]
    }' \
    --region $REGION

# 태스크 역할에 S3, Secrets Manager 권한 추가
aws iam attach-role-policy \
    --role-name ecsTaskRole \
    --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess \
    --region $REGION

aws iam attach-role-policy \
    --role-name ecsTaskRole \
    --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite \
    --region $REGION

# 8. Application Load Balancer 생성 (선택사항)
echo "⚖️ Application Load Balancer 생성 중..."
ALB_ARN=$(aws elbv2 describe-load-balancers --names "bifai-alb" --query "LoadBalancers[0].LoadBalancerArn" --output text --region $REGION 2>/dev/null || echo "None")

if [ "$ALB_ARN" = "None" ]; then
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name bifai-alb \
        --subnets $SUBNET_IDS \
        --security-groups $SG_ID \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --query "LoadBalancers[0].LoadBalancerArn" \
        --output text \
        --region $REGION)
        
    # 타겟 그룹 생성
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name bifai-tg \
        --protocol HTTP \
        --port 8080 \
        --vpc-id $VPC_ID \
        --target-type ip \
        --health-check-path /api/health \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --query "TargetGroups[0].TargetGroupArn" \
        --output text \
        --region $REGION)
        
    # 리스너 생성
    aws elbv2 create-listener \
        --load-balancer-arn $ALB_ARN \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn=$TARGET_GROUP_ARN \
        --region $REGION
fi

echo "ALB ARN: $ALB_ARN"

# 계정 ID 가져오기
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text --region $REGION)

# 출력 정보
echo ""
echo "✅ AWS 인프라 설정이 완료되었습니다!"
echo ""
echo "📋 설정 정보:"
echo "  - AWS Region: $REGION"
echo "  - Account ID: $ACCOUNT_ID"
echo "  - ECS Cluster: $CLUSTER_NAME"
echo "  - ECR Repository: $REPOSITORY_NAME"
echo "  - VPC ID: $VPC_ID"
echo "  - Security Group ID: $SG_ID"
echo "  - ALB ARN: $ALB_ARN"
echo ""
echo "🔧 다음 단계:"
echo "1. GitHub Secrets에 다음 값들을 추가하세요:"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo "   - AWS_ACCOUNT_ID: $ACCOUNT_ID"
echo ""
echo "2. task-definition.json 파일의 ACCOUNT_ID를 실제 계정 ID로 변경하세요"
echo "3. AWS Secrets Manager에 다음 시크릿들을 생성하세요:"
echo "   - bifai/db/username"
echo "   - bifai/db/password"
echo "   - bifai/jwt/secret"
echo "   - bifai/openai/api-key"
echo "   - bifai/google/project-id"
echo ""
echo "🚀 이제 GitHub Actions가 자동으로 빌드하고 배포할 준비가 되었습니다!"