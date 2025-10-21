#!/bin/bash

# Flyway Repair 스크립트
# V2 실패 상태 복구

echo "=== Flyway Repair 실행 ==="
echo "V2 마이그레이션 실패 상태를 복구합니다"

DB_HOST=${DB_HOST:-bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com}
DB_PORT=${DB_PORT:-3306}
DB_NAME=${DB_NAME:-bifai_db}
DB_USER=${DB_USER:-admin}
DB_PASSWORD=${DB_PASSWORD}

if [ -z "$DB_PASSWORD" ]; then
  echo "❌ DB_PASSWORD 환경 변수가 필요합니다"
  exit 1
fi

echo "Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo "User: $DB_USER"

# Flyway repair 명령 실행
JWT_SECRET="prod-secret-key-for-testing-only-minimum-64-characters-hs512-algorithm-needs-to-be-longer-than-this-for-proper-security-super-long" \
DB_HOST=$DB_HOST \
DB_PORT=$DB_PORT \
DB_NAME=$DB_NAME \
DB_USER=$DB_USER \
DB_PASSWORD=$DB_PASSWORD \
./gradlew flywayRepair

if [ $? -eq 0 ]; then
  echo "✅ Flyway Repair 성공!"
  echo "이제 애플리케이션을 재시작하면 V23-V27 마이그레이션이 실행됩니다"
else
  echo "❌ Flyway Repair 실패"
  exit 1
fi
