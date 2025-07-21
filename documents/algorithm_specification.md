# BIF-AI 리마인더 시스템 알고리즘 명세서

## 1. 카메라 영상 분석 알고리즘

### 1.1 실시간 상황 인식 알고리즘
```
알고리즘: RealTimeContextAnalyzer
입력: 카메라영상(videoFrame), 사용자프로필(userProfile), 현재시간(currentTime)
출력: 상황분석결과(contextAnalysis)

BEGIN
  // 영상 전처리
  processedFrame = PREPROCESS_FRAME(videoFrame)
  objects = DETECT_OBJECTS(processedFrame)
  text = EXTRACT_TEXT(processedFrame)
  
  // 상황 분류
  contextType = CLASSIFY_CONTEXT(objects, text, currentTime)
  
  SWITCH contextType:
    CASE "COOKING":
      cookingItems = FILTER_OBJECTS(objects, "COOKING_RELATED")
      safetyRisks = CHECK_COOKING_SAFETY(cookingItems)
      
      IF safetyRisks.gasStove == "ON" AND safetyRisks.userPresent == FALSE:
        guidance = "가스레인지가 켜져 있어요. 끄는 것을 잊지 마세요."
        priority = "HIGH"
      ELSE:
        guidance = GENERATE_COOKING_GUIDANCE(cookingItems)
        priority = "NORMAL"
      END IF
      
    CASE "MEDICATION":
      medicationItems = FILTER_OBJECTS(objects, "MEDICATION")
      pillCount = COUNT_PILLS(medicationItems)
      
      IF pillCount > 0:
        guidance = "약이 보여요. 복용 시간을 확인해보세요."
        priority = "MEDIUM"
      END IF
      
    CASE "TRANSPORTATION":
      transportInfo = EXTRACT_TRANSPORT_INFO(text, objects)
      
      IF transportInfo.busNumber != NULL:
        guidance = "버스 " + transportInfo.busNumber + "번이 보여요."
        priority = "HIGH"
      ELSE IF transportInfo.subwayLine != NULL:
        guidance = transportInfo.subwayLine + " 지하철역이에요."
        priority = "HIGH"
      END IF
      
    CASE "SHOPPING":
      products = FILTER_OBJECTS(objects, "PRODUCTS")
      priceText = EXTRACT_PRICE_INFO(text)
      
      guidance = GENERATE_SHOPPING_GUIDANCE(products, priceText)
      priority = "NORMAL"
      
    CASE "SOCIAL":
      people = FILTER_OBJECTS(objects, "PERSON")
      
      IF people.count > 0:
        guidance = "사람들이 주변에 있어요. 도움이 필요하면 말씀해보세요."
        priority = "LOW"
      END IF
      
    DEFAULT:
      guidance = "주변 상황을 지켜보고 있어요."
      priority = "LOW"
      
  END SWITCH
  
  contextAnalysis = CREATE_CONTEXT_ANALYSIS(
    type: contextType,
    objects: objects,
    guidance: guidance,
    priority: priority,
    timestamp: currentTime,
    confidence: CALCULATE_CONFIDENCE(objects, text)
  )
  
  RETURN contextAnalysis
END
```

### 1.2 LLM 기반 상황 해석 알고리즘
```
알고리즘: LLMContextInterpreter
입력: 상황분석결과(contextAnalysis), 사용자질문(userQuestion)
출력: AI응답(aiResponse)

BEGIN
  // 프롬프트 구성
  systemPrompt = "당신은 경계선 지적 기능을 가진 성인을 돕는 AI 도우미입니다. 
                 간단하고 명확한 언어로 도움을 주세요."
  
  contextPrompt = "현재 상황: " + contextAnalysis.type + 
                 "\n감지된 물체: " + JOIN(contextAnalysis.objects, ", ") +
                 "\n시간: " + contextAnalysis.timestamp
  
  userPrompt = "사용자 질문: " + userQuestion
  
  // LLM API 호출
  llmRequest = CREATE_LLM_REQUEST(
    model: "gpt-4",
    messages: [
      {role: "system", content: systemPrompt},
      {role: "user", content: contextPrompt + "\n" + userPrompt}
    ],
    maxTokens: 150,
    temperature: 0.3
  )
  
  llmResponse = CALL_LLM_API(llmRequest)
  
  // 응답 후처리
  aiResponse = POSTPROCESS_RESPONSE(llmResponse.content)
  
  // 안전성 검사
  IF CONTAINS_UNSAFE_CONTENT(aiResponse):
    aiResponse = "죄송해요. 다시 질문해주세요."
  END IF
  
  // 간단한 언어로 변환
  aiResponse = SIMPLIFY_LANGUAGE(aiResponse)
  
  RETURN aiResponse
END
```

## 2. 응급상황 감지 및 대응 알고리즘

### 2.1 응급상황 분류 알고리즘
```
알고리즘: EmergencyClassifier
입력: 사용자입력(userInput), 카메라데이터(cameraData), 위치정보(locationData)
출력: 응급등급(emergencyLevel)

BEGIN
  emergencyLevel = NONE
  
  // 사용자 직접 입력 분석
  IF userInput.type == "EMERGENCY_BUTTON":
    emergencyLevel = CRITICAL
  ELSE IF userInput.contains(["심장", "가슴", "숨", "의식"]):
    emergencyLevel = CRITICAL
  ELSE IF userInput.contains(["다침", "넘어짐", "출혈"]):
    emergencyLevel = HIGH
  ELSE IF userInput.contains(["어지러움", "메스꺼움", "길을 잃음"]):
    emergencyLevel = MEDIUM
  END IF
  
  // 카메라 데이터 분석
  IF cameraData.fallDetected == TRUE:
    emergencyLevel = MAX(emergencyLevel, HIGH)
  END IF
  
  IF cameraData.unconsciousDetected == TRUE:
    emergencyLevel = MAX(emergencyLevel, CRITICAL)
  END IF
  
  // 위치 기반 위험 상황 감지
  IF locationData.inDangerousArea == TRUE:
    emergencyLevel = MAX(emergencyLevel, MEDIUM)
  END IF
  
  IF locationData.stationaryTooLong == TRUE:
    emergencyLevel = MAX(emergencyLevel, MEDIUM)
  END IF
  
  RETURN emergencyLevel
END
```

### 2.2 응급 대응 프로토콜 알고리즘
```
알고리즘: EmergencyResponseProtocol
입력: 응급등급(emergencyLevel), 사용자위치(location), 사용자정보(userInfo)
출력: 대응결과(responseResult)

BEGIN
  currentTime = GET_CURRENT_TIME()
  responseActions = []
  
  SWITCH emergencyLevel:
    CASE CRITICAL:
      // 119 자동 신고
      emergencyCall = MAKE_EMERGENCY_CALL(
        number: "119",
        location: location,
        userInfo: userInfo,
        medicalHistory: userInfo.medicalHistory
      )
      responseActions.ADD(emergencyCall)
      
      // 즉시 보호자 알림
      FOR EACH guardian IN userInfo.guardians:
        guardianNotification = SEND_NOTIFICATION(
          recipient: guardian,
          type: "CRITICAL_EMERGENCY",
          location: location,
          timestamp: currentTime,
          callId: emergencyCall.id
        )
        responseActions.ADD(guardianNotification)
      END FOR
      
    CASE HIGH:
      // 보호자 우선 알림
      primaryGuardian = GET_PRIMARY_GUARDIAN(userInfo)
      guardianNotification = SEND_NOTIFICATION(
        recipient: primaryGuardian,
        type: "HIGH_EMERGENCY",
        location: location,
        timestamp: currentTime,
        options: ["CALL_119", "COME_TO_LOCATION", "CALL_USER"]
      )
      responseActions.ADD(guardianNotification)
      
      // 30초 후 119 신고 준비
      delayedEmergencyCall = SCHEDULE_DELAYED_ACTION(
        delay: 30_SECONDS,
        action: "EMERGENCY_CALL_119",
        cancelable: TRUE
      )
      responseActions.ADD(delayedEmergencyCall)
      
    CASE MEDIUM:
      // 상담 서비스 연결
      consultationCall = CONNECT_TO_CONSULTATION(
        userInfo: userInfo,
        symptoms: userInput.symptoms
      )
      responseActions.ADD(consultationCall)
      
      // 보호자에게 상황 공유
      guardianNotification = SEND_NOTIFICATION(
        recipient: userInfo.guardians,
        type: "HEALTH_CONCERN",
        details: userInput.symptoms,
        timestamp: currentTime
      )
      responseActions.ADD(guardianNotification)
      
  END SWITCH
  
  // 모든 대응 기록
  emergencyRecord = CREATE_EMERGENCY_RECORD(
    userId: userInfo.id,
    level: emergencyLevel,
    location: location,
    timestamp: currentTime,
    actions: responseActions,
    resolved: FALSE
  )
  
  SAVE_EMERGENCY_RECORD(emergencyRecord)
  RETURN responseActions
END
```

## 3. 길찾기 네비게이션 알고리즘

### 3.1 경로 탐색 알고리즘
```
알고리즘: NavigationPathFinder
입력: 출발지(origin), 목적지(destination), 사용자선호도(preferences)
출력: 최적경로(optimalPath)

BEGIN
  // 대중교통 경로 검색
  publicTransitRoutes = SEARCH_PUBLIC_TRANSIT(origin, destination)
  walkingRoute = SEARCH_WALKING_ROUTE(origin, destination)
  
  routes = []
  
  FOR EACH route IN publicTransitRoutes:
    // BIF 사용자를 위한 경로 평가
    complexity = CALCULATE_ROUTE_COMPLEXITY(route)
    
    routeScore = 0
    
    // 환승 횟수 페널티 (환승이 적을수록 좋음)
    transferPenalty = route.transferCount * 20
    routeScore -= transferPenalty
    
    // 소요 시간 (짧을수록 좋음)
    timePenalty = route.duration / 60 * 5
    routeScore -= timePenalty
    
    // 복잡도 페널티 (단순할수록 좋음)
    complexityPenalty = complexity * 15
    routeScore -= complexityPenalty
    
    // 접근성 보너스 (엘리베이터, 에스컬레이터 있는 역)
    accessibilityBonus = COUNT_ACCESSIBLE_STATIONS(route) * 10
    routeScore += accessibilityBonus
    
    routeWithScore = ADD_SCORE(route, routeScore)
    routes.ADD(routeWithScore)
  END FOR
  
  // 도보 경로 평가
  IF walkingRoute.distance <= preferences.maxWalkingDistance:
    walkingScore = 100 - (walkingRoute.distance / 100 * 10)
    walkingRouteWithScore = ADD_SCORE(walkingRoute, walkingScore)
    routes.ADD(walkingRouteWithScore)
  END IF
  
  // 최고 점수 경로 선택
  optimalPath = MAX_SCORE(routes)
  
  RETURN optimalPath
END
```

### 3.2 단계별 안내 생성 알고리즘
```
알고리즘: StepByStepGuideGenerator
입력: 최적경로(optimalPath), 현재위치(currentLocation)
출력: 단계별안내(stepGuide)

BEGIN
  steps = []
  currentStep = 0
  
  FOR EACH segment IN optimalPath.segments:
    SWITCH segment.type:
      CASE "WALK":
        walkSteps = GENERATE_WALKING_STEPS(segment)
        FOR EACH walkStep IN walkSteps:
          step = CREATE_STEP(
            type: "WALK",
            instruction: walkStep.instruction,
            distance: walkStep.distance,
            landmark: walkStep.landmark,
            estimatedTime: walkStep.estimatedTime,
            voiceGuide: GENERATE_VOICE_GUIDE(walkStep.instruction)
          )
          steps.ADD(step)
        END FOR
        
      CASE "BUS":
        busStep = CREATE_STEP(
          type: "BUS",
          instruction: segment.busNumber + "번 버스를 타세요",
          busNumber: segment.busNumber,
          busStop: segment.boardingStop,
          destination: segment.alightingStop,
          estimatedTime: segment.duration,
          voiceGuide: segment.busNumber + "번 버스를 타고 " + 
                     segment.alightingStop + "에서 내리세요"
        )
        steps.ADD(busStep)
        
      CASE "SUBWAY":
        subwayStep = CREATE_STEP(
          type: "SUBWAY",
          instruction: segment.line + " " + segment.direction + " 방향 지하철을 타세요",
          line: segment.line,
          direction: segment.direction,
          boardingStation: segment.boardingStation,
          alightingStation: segment.alightingStation,
          estimatedTime: segment.duration,
          voiceGuide: segment.line + " " + segment.direction + " 방향 지하철을 타고 " +
                     segment.alightingStation + "에서 내리세요"
        )
        steps.ADD(subwayStep)
        
    END SWITCH
  END FOR
  
  stepGuide = CREATE_STEP_GUIDE(
    steps: steps,
    totalTime: optimalPath.totalDuration,
    totalDistance: optimalPath.totalDistance,
    currentStep: 0,
    completed: FALSE
  )
  
  RETURN stepGuide
END
```
        
      ## 4. 일정 관리 알고리즘

### 4.1 일정 우선순위 결정 알고리즘
```
알고리즘: SchedulePriorityCalculator
입력: 일정목록(schedules), 현재시간(currentTime), 사용자상태(userState)
출력: 우선순위정렬일정(prioritizedSchedules)

BEGIN
  prioritizedSchedules = []
  
  FOR EACH schedule IN schedules:
    priority = 0
    
    // 시간 기반 우선순위
    timeUntilSchedule = schedule.scheduledTime - currentTime
    
    IF timeUntilSchedule <= 30_MINUTES:
      priority += 50
    ELSE IF timeUntilSchedule <= 2_HOURS:
      priority += 30
    ELSE IF timeUntilSchedule <= 24_HOURS:
      priority += 10
    END IF
    
    // 일정 유형별 우선순위
    SWITCH schedule.type:
      CASE "MEDICAL":
        priority += 40
      CASE "WORK":
        priority += 30
      CASE "PERSONAL":
        priority += 20
      CASE "LEISURE":
        priority += 10
    END SWITCH
    
    // 반복 일정 보너스
    IF schedule.isRecurring == TRUE:
      priority += 15
    END IF
    
    // 사용자 상태 고려
    IF userState.stressLevel == "HIGH":
      IF schedule.type == "LEISURE":
        priority += 20
      END IF
    END IF
    
    scheduleWithPriority = ADD_PRIORITY(schedule, priority)
    prioritizedSchedules.ADD(scheduleWithPriority)
  END FOR
  
  // 우선순위 순으로 정렬
  prioritizedSchedules = SORT_BY_PRIORITY(prioritizedSchedules)
  
  RETURN prioritizedSchedules
END
```

### 4.2 일정 리마인더 알고리즘
```
알고리즘: ScheduleReminderManager
입력: 우선순위정렬일정(prioritizedSchedules), 현재시간(currentTime)
출력: 리마인더목록(reminders)

BEGIN
  reminders = []
  
  FOR EACH schedule IN prioritizedSchedules:
    timeUntilSchedule = schedule.scheduledTime - currentTime
    
    // 리마인더 시점 결정
    reminderTimes = []
    
    IF schedule.priority >= 70:
      // 높은 우선순위: 1시간 전, 30분 전, 10분 전
      reminderTimes = [60_MINUTES, 30_MINUTES, 10_MINUTES]
    ELSE IF schedule.priority >= 40:
      // 중간 우선순위: 30분 전, 10분 전
      reminderTimes = [30_MINUTES, 10_MINUTES]
    ELSE:
      // 낮은 우선순위: 10분 전
      reminderTimes = [10_MINUTES]
    END IF
    
    FOR EACH reminderTime IN reminderTimes:
      IF timeUntilSchedule <= reminderTime AND timeUntilSchedule > 0:
        reminderMessage = GENERATE_REMINDER_MESSAGE(schedule, reminderTime)
        
        reminder = CREATE_REMINDER(
          scheduleId: schedule.id,
          message: reminderMessage,
          type: DETERMINE_REMINDER_TYPE(schedule.priority),
          scheduledTime: schedule.scheduledTime,
          reminderTime: reminderTime
        )
        
        reminders.ADD(reminder)
      END IF
    END FOR
  END FOR
  
  RETURN reminders
END
```

## 5. 낙상 감지 알고리즘

### 5.1 카메라 기반 낙상 감지 알고리즘
```
알고리즘: CameraFallDetector
입력: 카메라영상시퀀스(videoSequence), 사용자프로필(userProfile)
출력: 낙상감지결과(fallDetectionResult)

BEGIN
  fallDetected = FALSE
  confidence = 0
  
  // 연속 프레임 분석
  FOR i = 0 TO videoSequence.length - 2:
    currentFrame = videoSequence[i]
    nextFrame = videoSequence[i + 1]
    
    // 사용자 포즈 추출
    currentPose = EXTRACT_POSE(currentFrame)
    nextPose = EXTRACT_POSE(nextFrame)
    
    IF currentPose != NULL AND nextPose != NULL:
      // 수직 위치 변화 계산
      verticalChange = currentPose.centerY - nextPose.centerY
      
      // 급격한 하강 감지
      IF verticalChange > FALL_THRESHOLD:
        // 추가 검증
        bodyOrientation = CALCULATE_BODY_ORIENTATION(nextPose)
        
        IF bodyOrientation == "HORIZONTAL":
          // 바닥에 누워있는 상태 확인
          stationaryFrames = COUNT_STATIONARY_FRAMES(videoSequence, i + 1)
          
          IF stationaryFrames >= MIN_STATIONARY_FRAMES:
            fallDetected = TRUE
            confidence = CALCULATE_CONFIDENCE(verticalChange, bodyOrientation, stationaryFrames)
            break
          END IF
        END IF
      END IF
    END IF
  END FOR
  
  fallDetectionResult = CREATE_FALL_DETECTION_RESULT(
    detected: fallDetected,
    confidence: confidence,
    timestamp: GET_CURRENT_TIME(),
    location: GET_CURRENT_LOCATION()
  )
  
  RETURN fallDetectionResult
END
```

## 6. 음성 인식 및 처리 알고리즘

### 6.1 BIF 사용자 특화 음성 인식 알고리즘
```
알고리즘: BIFSpeechRecognizer
입력: 음성데이터(audioData), 사용자프로필(userProfile)
출력: 인식결과(recognitionResult)

BEGIN
  // 음성 전처리
  cleanAudio = NOISE_REDUCTION(audioData)
  normalizedAudio = NORMALIZE_VOLUME(cleanAudio)
  
  // 기본 음성 인식
  rawText = SPEECH_TO_TEXT(normalizedAudio)
  
  // BIF 사용자 특화 후처리
  correctedText = CORRECT_COMMON_ERRORS(rawText, userProfile.speechPatterns)
  
  // 의도 분석
  intent = ANALYZE_INTENT(correctedText)
  
  SWITCH intent.type:
    CASE "EMERGENCY":
      confidence = 0.95
      urgency = "HIGH"
      
    CASE "NAVIGATION":
      destination = EXTRACT_DESTINATION(correctedText)
      confidence = CALCULATE_DESTINATION_CONFIDENCE(destination)
      urgency = "MEDIUM"
      
    CASE "QUESTION":
      questionType = CLASSIFY_QUESTION(correctedText)
      confidence = 0.8
      urgency = "LOW"
      
    CASE "SCHEDULE":
      scheduleAction = EXTRACT_SCHEDULE_ACTION(correctedText)
      confidence = 0.85
      urgency = "MEDIUM"
      
    DEFAULT:
      confidence = 0.6
      urgency = "LOW"
      
  END SWITCH
  
  recognitionResult = CREATE_RECOGNITION_RESULT(
    originalText: rawText,
    correctedText: correctedText,
    intent: intent,
    confidence: confidence,
    urgency: urgency,
    timestamp: GET_CURRENT_TIME()
  )
  
  RETURN recognitionResult
END
```

## 7. 사용자 행동 패턴 학습 알고리즘

### 7.1 일상 패턴 분석 알고리즘
```
알고리즘: DailyPatternAnalyzer
입력: 사용자활동기록(activityLog), 분석기간(analysisPeriod)
출력: 행동패턴(behaviorPatterns)

BEGIN
  patterns = []
  
  // 시간대별 활동 분석
  FOR hour = 0 TO 23:
    hourlyActivities = FILTER_BY_HOUR(activityLog, hour)
    
    IF hourlyActivities.count >= MIN_ACTIVITY_COUNT:
      commonActivity = FIND_MOST_COMMON_ACTIVITY(hourlyActivities)
      frequency = CALCULATE_FREQUENCY(commonActivity, hourlyActivities)
      
      IF frequency >= PATTERN_THRESHOLD:
        pattern = CREATE_PATTERN(
          type: "HOURLY",
          time: hour,
          activity: commonActivity,
          frequency: frequency,
          confidence: CALCULATE_CONFIDENCE(frequency)
        )
        patterns.ADD(pattern)
      END IF
    END IF
  END FOR
  
  // 요일별 패턴 분석
  FOR day = 0 TO 6:
    dailyActivities = FILTER_BY_DAY(activityLog, day)
    routines = EXTRACT_ROUTINES(dailyActivities)
    
    FOR EACH routine IN routines:
      IF routine.consistency >= ROUTINE_THRESHOLD:
        pattern = CREATE_PATTERN(
          type: "DAILY",
          day: day,
          routine: routine,
          frequency: routine.consistency,
          confidence: CALCULATE_CONFIDENCE(routine.consistency)
        )
        patterns.ADD(pattern)
      END IF
    END FOR
  END FOR
  
  // 이상 행동 감지
  anomalies = DETECT_ANOMALIES(activityLog, patterns)
  
  behaviorPatterns = CREATE_BEHAVIOR_PATTERNS(
    patterns: patterns,
    anomalies: anomalies,
    analysisDate: GET_CURRENT_DATE(),
    analysisPeriod: analysisPeriod
  )
  
  RETURN behaviorPatterns
END
```

## 성능 및 최적화 고려사항

### 실시간 처리 요구사항
- 카메라 영상 분석: 30fps 실시간 처리
- 음성 인식: 3초 이내 응답
- 응급상황 감지: 10초 이내 대응

### 메모리 최적화
- 영상 데이터 압축 및 임시 저장
- 불필요한 데이터 자동 삭제
- 캐시 메모리 효율적 활용

### 배터리 효율성
- AI 처리 빈도 조절
- 백그라운드 처리 최적화
- 하드웨어 가속 활용

### 정확도 및 성능 목표
- 상황 인식 정확도: 90% 이상
- 음성 인식 정확도: 95% 이상
- 낙상 감지 정확도: 90% 이상
- 응급상황 분류 정확도: 98% 이상
- 경로 탐색 만족도: 85% 이상

### 시스템 안정성
- 24시간 연속 운영 가능
- 99.9% 가용성 보장
- 자동 복구 메커니즘
- 실시간 모니터링 및 알림
