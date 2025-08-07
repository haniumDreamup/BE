# FallDetectionService 테스트 실패 원인 분석

## 실패한 테스트들
1. **detectFall_RealFallScenario** - 실제 낙상 시나리오
2. **detectFall_PatternBased** - 패턴 기반 낙상 감지 
3. **detectFall_SuddenAngleChange** - 급격한 각도 변화 감지
4. **detectFall_PreventDuplicate** - 중복 낙상 감지 방지

## 원인 분석

### 1. 테스트 데이터의 문제점

#### a. 프레임 순서 문제
```java
// 테스트 코드 (FallDetectionServiceEnhancedTest.java)
for (int i = 149; i >= 0; i--) {  // 역순으로 추가
```
- 테스트 데이터가 역순으로 추가되어 있지만, 실제 시간 순서가 맞지 않음
- `timestamp`가 올바르게 설정되지 않아 속도 계산이 잘못됨

#### b. 필수 필드 누락
```java
// 테스트에서 생성한 PoseData
PoseData frame = PoseData.builder()
    .centerY(centerY)
    .velocityY(velocityY)  // 이 값은 서비스에서 계산되는데 테스트에서 설정
    .motionScore(motionScore)  // 이 값도 서비스에서 계산됨
```
- `velocityY`와 `motionScore`는 서비스 내부에서 계산되는 값
- 테스트에서 미리 설정하면 안 됨

### 2. 서비스 로직의 문제점

#### a. 속도 계산 로직
```java
// FallDetectionService.java
float deltaY = currentFrame.getCenterY() - previousFrame.getCenterY();
```
- 프레임 간 시간 차이가 0일 때 처리가 부족
- 음수 값(상승)과 양수 값(하강)의 구분이 명확하지 않음

#### b. 낙상 판단 조건이 너무 엄격
```java
// 모든 조건을 만족해야 낙상으로 판단
- 급격한 하강 (velocityY > 0.2f)
- 낮은 위치 (centerY > 0.7f)  
- 수평 자세 (isHorizontal = true)
- 움직임 없음 (motionScore < 0.01f)
- 최소 신뢰도 (confidence >= 0.75f)
```

#### c. 프레임 데이터 조회 순서
```java
// 가장 최근 프레임과 비교
PoseData previousFrame = recentFrames.get(0);
```
- `recentFrames`가 시간 역순으로 정렬되어 있다고 가정
- 하지만 실제로는 순서가 보장되지 않을 수 있음

### 3. 구체적인 실패 원인

#### detectFall_RealFallScenario 실패
- `velocityY` 계산이 0.25f로 설정되었지만, 실제 계산 시 다른 값
- `isHorizontal` 판단 시 랜드마크 visibility 체크에서 실패
- 전체 confidence 점수가 0.75 미만

#### detectFall_PatternBased 실패  
- 패턴 감지 로직에서 프레임 순서 문제
- `motionScore`가 null인 경우 처리 안 됨

#### detectFall_PreventDuplicate 실패
- Mockito의 불필요한 stubbing 오류
- 테스트 설정과 실제 호출이 일치하지 않음

## 해결 계획

### 1단계: 테스트 데이터 수정
- [ ] 프레임 시간 순서 정정
- [ ] 서비스에서 계산되는 필드 제거
- [ ] 랜드마크 데이터 개선

### 2단계: 서비스 로직 개선  
- [ ] 속도 계산 로직 개선
- [ ] 낙상 판단 조건 완화
- [ ] null 체크 및 예외 처리 강화

### 3단계: 테스트 코드 정리
- [ ] Mock 설정 최적화
- [ ] 실제 서비스 동작과 일치하도록 수정
- [ ] 디버그 로그 추가

### 4단계: 통합 테스트
- [ ] 단위 테스트 통과 확인
- [ ] 실제 시나리오 테스트
- [ ] 성능 테스트