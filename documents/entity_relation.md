# BIF-AI 리마인더 시스템 엔티티 관계도 및 테이블 정의서

## 1. 엔티티 관계도 (ERD)

```
+----------------+        +-------------------+        +-------------------+
|     User       |        |      Device       |        |    Schedule       |
+----------------+        +-------------------+        +-------------------+
| PK: id         |<------>| PK: id            |        | PK: id            |
| name           |        | user_id (FK)      |        | user_id (FK)      |
| email          |        | device_name       |        | title             |
| password       |        | device_type       |        | description       |
| phone          |        | status            |        | start_time        |
| birth_date     |        | battery_level     |        | end_time          |
| gender         |        | last_connected    |        | repeat_pattern    |
| profile_image  |        | firmware_version  |        | priority          |
| cognitive_level|        | settings          |        | status            |
| created_at     |        | created_at        |        | created_at        |
| updated_at     |        | updated_at        |        | updated_at        |
+----------------+        +-------------------+        +-------------------+
      |   |                        |                          |
      |   |                        |                          |
      |   |                        v                          |
      |   |              +-------------------+                |
      |   |              |   CapturedImage   |                |
      |   |              +-------------------+                |
      |   |              | PK: id            |                |
      |   |              | device_id (FK)    |                |
      |   |              | image_url         |                |
      |   |              | capture_time      |                |
      |   |              | processed         |                |
      |   |              | metadata          |                |
      |   |              | created_at        |                |
      |   |              | updated_at        |                |
      |   |              +-------------------+                |
      |   |                        |                          |
      |   |                        v                          v
      |   |              +-------------------+        +-------------------+
      |   |              |   AnalysisResult  |        |   Notification    |
      |   |              +-------------------+        +-------------------+
      |   |              | PK: id            |        | PK: id            |
      |   |              | image_id (FK)     |        | user_id (FK)      |
      |   |              | description       |<-------| schedule_id (FK)  |
      |   |              | recommendations   |        | analysis_id (FK)  |
      |   |              | context_data      |        | type              |
      |   |              | created_at        |        | title             |
      |   |              | updated_at        |        | message           |
      |   |              +-------------------+        | priority          |
      |   |                                           | status            |
      |   |                                           | scheduled_time    |
      |   |                                           | created_at        |
      |   |                                           | updated_at        |
      |   |                                           +-------------------+
      |   |
      |   v
+----------------+        +-------------------+
|   Guardian     |        |   ActivityLog     |
+----------------+        +-------------------+
| PK: id         |        | PK: id            |
| user_id (FK)   |<-------| user_id (FK)      |
| name           |        | device_id (FK)    |
| email          |        | activity_type     |
| phone          |        | description       |
| relation       |        | location          |
| created_at     |        | created_at        |
| updated_at     |        +-------------------+
+----------------+
```

## 2. 테이블 정의서

### 2.1 User (사용자)

| 필드명          | 데이터 타입   | NULL 허용 | 기본값     | 설명                                     |
|----------------|--------------|----------|-----------|------------------------------------------|
| id             | BIGINT       | No       | AUTO_INC  | 사용자 고유 식별자                        |
| name           | VARCHAR(100) | No       | NULL      | 사용자 이름                              |
| email          | VARCHAR(100) | Yes      | NULL      | 이메일 주소 (선택적)                      |
| password       | VARCHAR(255) | Yes      | NULL      | 해시된 비밀번호 (2단계에서 사용)          |
| phone          | VARCHAR(20)  | Yes      | NULL      | 전화번호                                |
| birth_date     | DATE         | Yes      | NULL      | 생년월일                                |
| gender         | CHAR(1)      | Yes      | NULL      | 성별 (M/F/O)                            |
| profile_image  | VARCHAR(255) | Yes      | NULL      | 프로필 이미지 URL                        |
| cognitive_level| TINYINT      | Yes      | NULL      | 인지 레벨 (사용자 특성에 따른 설정값)      |
| created_at     | TIMESTAMP    | No       | CURRENT   | 생성 일시                                |
| updated_at     | TIMESTAMP    | No       | CURRENT   | 수정 일시                                |

**인덱스**:
- PRIMARY KEY: id
- UNIQUE INDEX: email (email이 있는 경우)

**설명**:
- 시스템 사용자 정보를 저장합니다.
- BIF 사용자 및 보호자(Guardian)와 연결됩니다.
- 초기 MVP에서는 로그인/회원가입 기능이 없으므로 email, password 필드는 2단계에서 활성화될 예정입니다.

### 2.2 Device (디바이스)

| 필드명           | 데이터 타입   | NULL 허용 | 기본값     | 설명                                 |
|-----------------|--------------|----------|-----------|--------------------------------------|
| id              | BIGINT       | No       | AUTO_INC  | 디바이스 고유 식별자                   |
| user_id         | BIGINT       | No       | NULL      | 소유자 사용자 ID (FK: User.id)         |
| device_name     | VARCHAR(100) | No       | NULL      | 디바이스 이름                        |
| device_type     | VARCHAR(50)  | No       | 'CAMERA'  | 디바이스 유형                        |
| status          | VARCHAR(20)  | No       | 'INACTIVE'| 상태 (ACTIVE/INACTIVE/OFFLINE)       |
| battery_level   | TINYINT      | Yes      | NULL      | 배터리 잔량 (%)                      |
| last_connected  | TIMESTAMP    | Yes      | NULL      | 마지막 연결 시간                     |
| firmware_version| VARCHAR(20)  | Yes      | NULL      | 펌웨어 버전                         |
| settings        | JSON         | Yes      | NULL      | 디바이스 설정 (JSON 형식)            |
| created_at      | TIMESTAMP    | No       | CURRENT   | 생성 일시                            |
| updated_at      | TIMESTAMP    | No       | CURRENT   | 수정 일시                            |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: user_id REFERENCES User(id)
- INDEX: status, last_connected

**설명**:
- 카메라 디바이스 정보를 저장합니다.
- 사용자와 1:N 관계를 가집니다 (한 사용자가 여러 디바이스 보유 가능).
- settings 필드는 JSON 형태로 이미지 캡처 간격, 해상도 등의 설정을 저장합니다.

### 2.3 CapturedImage (캡처된 이미지)

| 필드명        | 데이터 타입   | NULL 허용 | 기본값     | 설명                                |
|--------------|--------------|----------|-----------|-----------------------------------|
| id           | BIGINT       | No       | AUTO_INC  | 이미지 고유 식별자                  |
| device_id    | BIGINT       | No       | NULL      | 캡처한 디바이스 ID (FK: Device.id)  |
| image_url    | VARCHAR(255) | No       | NULL      | S3에 저장된 이미지 URL              |
| capture_time | TIMESTAMP    | No       | CURRENT   | 이미지 캡처 시간                   |
| processed    | BOOLEAN      | No       | FALSE     | 처리 완료 여부                     |
| metadata     | JSON         | Yes      | NULL      | 추가 메타데이터 (위치, 센서 데이터 등)|
| created_at   | TIMESTAMP    | No       | CURRENT   | 생성 일시                          |
| updated_at   | TIMESTAMP    | No       | CURRENT   | 수정 일시                          |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: device_id REFERENCES Device(id)
- INDEX: capture_time, processed

**설명**:
- 디바이스에서 캡처된 이미지 정보를 저장합니다.
- 실제 이미지는 AWS S3에 저장되고 URL만 데이터베이스에 저장합니다.
- 메타데이터는 위치, 센서 데이터 등 추가 정보를 JSON 형태로 저장합니다.

### 2.4 AnalysisResult (분석 결과)

| 필드명           | 데이터 타입    | NULL 허용 | 기본값     | 설명                                |
|-----------------|---------------|----------|-----------|-----------------------------------|
| id              | BIGINT        | No       | AUTO_INC  | 분석 결과 고유 식별자               |
| image_id        | BIGINT        | No       | NULL      | 분석된 이미지 ID (FK: CapturedImage.id) |
| description     | TEXT          | No       | NULL      | 상황 설명                          |
| recommendations | TEXT          | Yes      | NULL      | 권장 사항                          |
| context_data    | JSON          | Yes      | NULL      | 추가 컨텍스트 데이터                |
| created_at      | TIMESTAMP     | No       | CURRENT   | 생성 일시                          |
| updated_at      | TIMESTAMP     | No       | CURRENT   | 수정 일시                          |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: image_id REFERENCES CapturedImage(id)
- FULLTEXT INDEX: description, recommendations

**설명**:
- LLM을 통해 분석된 이미지의 결과를 저장합니다.
- description은 상황에 대한 설명, recommendations는 권장 행동을 포함합니다.
- context_data는 분석 시 사용된 추가 정보(날씨, 시간대 등)를 JSON 형태로 저장합니다.

### 2.5 Schedule (일정)

| 필드명          | 데이터 타입   | NULL 허용 | 기본값     | 설명                               |
|----------------|--------------|----------|-----------|----------------------------------|
| id             | BIGINT       | No       | AUTO_INC  | 일정 고유 식별자                   |
| user_id        | BIGINT       | No       | NULL      | 소유자 사용자 ID (FK: User.id)     |
| title          | VARCHAR(200) | No       | NULL      | 일정 제목                        |
| description    | TEXT         | Yes      | NULL      | 일정 설명                        |
| start_time     | TIMESTAMP    | No       | NULL      | 시작 시간                        |
| end_time       | TIMESTAMP    | Yes      | NULL      | 종료 시간 (선택적)                |
| repeat_pattern | VARCHAR(50)  | Yes      | NULL      | 반복 패턴 (NONE, DAILY, WEEKLY 등)|
| priority       | TINYINT      | No       | 1         | 우선순위 (1-5)                   |
| status         | VARCHAR(20)  | No       | 'PENDING' | 상태 (PENDING, COMPLETED 등)     |
| created_at     | TIMESTAMP    | No       | CURRENT   | 생성 일시                        |
| updated_at     | TIMESTAMP    | No       | CURRENT   | 수정 일시                        |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: user_id REFERENCES User(id)
- INDEX: start_time, status, priority

**설명**:
- 사용자의 일정 정보를 저장합니다.
- 약속, 약 복용, 일상 활동 등 다양한 일정을 포함합니다.
- repeat_pattern은 일정 반복 주기를 나타냅니다 (NONE, DAILY, WEEKLY, MONTHLY, CUSTOM).

### 2.6 Notification (알림)

| 필드명          | 데이터 타입   | NULL 허용 | 기본값     | 설명                                   |
|----------------|--------------|----------|-----------|--------------------------------------|
| id             | BIGINT       | No       | AUTO_INC  | 알림 고유 식별자                       |
| user_id        | BIGINT       | No       | NULL      | 수신자 사용자 ID (FK: User.id)         |
| schedule_id    | BIGINT       | Yes      | NULL      | 관련 일정 ID (FK: Schedule.id)        |
| analysis_id    | BIGINT       | Yes      | NULL      | 관련 분석 결과 ID (FK: AnalysisResult.id) |
| type           | VARCHAR(50)  | No       | NULL      | 알림 유형 (SCHEDULE, ANALYSIS, EMERGENCY 등) |
| title          | VARCHAR(200) | No       | NULL      | 알림 제목                            |
| message        | TEXT         | No       | NULL      | 알림 내용                            |
| priority       | TINYINT      | No       | 1         | 우선순위 (1-5)                       |
| status         | VARCHAR(20)  | No       | 'PENDING' | 상태 (PENDING, DELIVERED, READ 등)   |
| scheduled_time | TIMESTAMP    | Yes      | NULL      | 예약 전송 시간                       |
| created_at     | TIMESTAMP    | No       | CURRENT   | 생성 일시                            |
| updated_at     | TIMESTAMP    | No       | CURRENT   | 수정 일시                            |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: user_id REFERENCES User(id)
- FOREIGN KEY: schedule_id REFERENCES Schedule(id)
- FOREIGN KEY: analysis_id REFERENCES AnalysisResult(id)
- INDEX: status, priority, scheduled_time

**설명**:
- 사용자에게 전송되는 알림 정보를 저장합니다.
- 일정 리마인더, 상황 인식 정보, 비상 알림 등 다양한 유형의 알림을 포함합니다.
- schedule_id와 analysis_id는 알림의 출처에 따라 선택적으로 설정됩니다.

### 2.7 Guardian (보호자)

| 필드명      | 데이터 타입   | NULL 허용 | 기본값     | 설명                             |
|------------|--------------|----------|-----------|--------------------------------|
| id         | BIGINT       | No       | AUTO_INC  | 보호자 고유 식별자                |
| user_id    | BIGINT       | No       | NULL      | 관련 사용자 ID (FK: User.id)     |
| name       | VARCHAR(100) | No       | NULL      | 보호자 이름                     |
| email      | VARCHAR(100) | Yes      | NULL      | 이메일 주소                     |
| phone      | VARCHAR(20)  | No       | NULL      | 전화번호                       |
| relation   | VARCHAR(50)  | Yes      | NULL      | 사용자와의 관계 (부모, 자녀 등)   |
| created_at | TIMESTAMP    | No       | CURRENT   | 생성 일시                       |
| updated_at | TIMESTAMP    | No       | CURRENT   | 수정 일시                       |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: user_id REFERENCES User(id)
- INDEX: phone

**설명**:
- BIF 사용자의 보호자 정보를 저장합니다.
- 한 사용자는 여러 보호자를 가질 수 있습니다.
- 비상 상황 발생 시 연락 및 상태 모니터링에 사용됩니다.

### 2.8 ActivityLog (활동 로그)

| 필드명         | 데이터 타입   | NULL 허용 | 기본값     | 설명                             |
|---------------|--------------|----------|-----------|--------------------------------|
| id            | BIGINT       | No       | AUTO_INC  | 로그 고유 식별자                 |
| user_id       | BIGINT       | No       | NULL      | 사용자 ID (FK: User.id)         |
| device_id     | BIGINT       | Yes      | NULL      | 디바이스 ID (FK: Device.id)     |
| activity_type | VARCHAR(50)  | No       | NULL      | 활동 유형 (LOGIN, CAPTURE, MOVEMENT 등) |
| description   | TEXT         | Yes      | NULL      | 활동 설명                       |
| location      | GEOMETRY     | Yes      | NULL      | 위치 정보 (GPS 좌표)             |
| created_at    | TIMESTAMP    | No       | CURRENT   | 생성 일시                       |

**인덱스**:
- PRIMARY KEY: id
- FOREIGN KEY: user_id REFERENCES User(id)
- FOREIGN KEY: device_id REFERENCES Device(id)
- INDEX: activity_type, created_at
- SPATIAL INDEX: location

**설명**:
- 사용자 및 디바이스 활동 기록을 저장합니다.
- 이동 경로, 앱 사용, 디바이스 상태 변경 등 다양한 활동을 추적합니다.
- 위치 기반 분석 및 비상 상황 추적에 활용됩니다.

## 3. 데이터베이스 설계 원칙

### 3.1 정규화
- 제3정규형(3NF)까지 정규화하여 데이터 중복을 최소화하고 무결성을 보장합니다.
- JSON 필드는 자주 변경되거나 구조가 유연한 데이터에만 제한적으로 사용합니다.

### 3.2 성능 최적화
- 자주 조회되는 필드에 적절한 인덱스를 설정합니다.
- 대용량 데이터(이미지, 음성)는 S3에 저장하고 URL만 데이터베이스에 저장합니다.
- 시계열 데이터(로그, 활동 기록)는 파티셔닝을 고려합니다.

### 3.3 확장성
- 사용자 증가에 따른 데이터베이스 확장을 고려한 설계입니다.
- UUID 사용, 샤딩 등의 기법을 필요에 따라 적용할 수 있도록 준비합니다.

### 3.4 보안
- 민감한 개인정보(비밀번호 등)는 해시 처리하여 저장합니다.
- 위치 데이터 등 프라이버시 관련 정보는 접근 제한을 엄격히 적용합니다.

## 4. 2단계 확장 고려사항

### 4.1 사용자 인증 및 권한 관리
- User 테이블에 role, auth_provider, refresh_token 등의 필드 추가
- UserRole, Permission 테이블 추가

### 4.2 데이터 분석 및 학습
- UserBehaviorPattern: 사용자 행동 패턴 학습 결과 저장
- FeedbackData: 사용자 피드백 및 시스템 개선을 위한 데이터 저장

### 4.3 소셜 및 커뮤니티 기능
- UserConnection: 사용자 간 연결 정보
- Community, Post, Comment: 커뮤니티 기능 관련 테이블

## 5. 참고 사항

### 5.1 한글 처리
- 모든 문자열 필드는 UTF-8 인코딩을 사용하여 한글을 포함한 다국어 지원을 보장합니다.
- 검색 및 정렬 시 한글 처리를 위한 인덱스 및 콜레이션 설정이 필요합니다.

### 5.2 시간대 처리
- 모든 시간 관련 필드는 UTC로 저장하고, 앱에서 사용자 시간대에 맞게 변환하여 표시합니다.
- 시간대 정보는 User 테이블의 설정에 포함하여 저장합니다.

### 5.3 초기 데이터
- 시스템 초기 구축 시 필요한 기본 데이터(시스템 설정, 기본 알림 유형 등)는 마이그레이션 스크립트를 통해 제공합니다. 