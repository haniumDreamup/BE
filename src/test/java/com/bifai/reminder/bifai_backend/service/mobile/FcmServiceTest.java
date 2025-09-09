package com.bifai.reminder.bifai_backend.service.mobile;

import com.google.firebase.messaging.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

/**
 * FcmService 단위 테스트
 * Firebase Cloud Messaging 푸시 알림 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService 단위 테스트")
class FcmServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private FcmService fcmService;

    private final String testToken = "test-fcm-token";
    private final String testTitle = "테스트 제목";
    private final String testBody = "테스트 내용";
    private final String messageId = "test-message-id";

    @BeforeEach
    void setUp() {
        // Mock 설정은 각 테스트에서 개별적으로 수행
    }

    @Test
    @DisplayName("단일 알림 전송 성공")
    void sendNotification_Success() throws Exception {
        // Given
        Map<String, String> data = new HashMap<>();
        data.put("customKey", "customValue");

        when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

        // When
        boolean result = fcmService.sendNotification(
                testToken, testTitle, testBody, data, 
                FcmService.NotificationCategory.MEDICATION, 
                FcmService.Priority.HIGH
        );

        // Then
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("단일 알림 전송 실패 - Firebase 에러")
    void sendNotification_FirebaseError_Failure() throws Exception {
        // Given
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(exception.getMessage()).thenReturn("Internal error");
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        // When
        boolean result = fcmService.sendNotification(
                testToken, testTitle, testBody, null,
                FcmService.NotificationCategory.REMINDER,
                FcmService.Priority.NORMAL
        );

        // Then
        assertThat(result).isFalse();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("단일 알림 전송 실패 - 일반 예외")
    void sendNotification_GeneralException_Failure() throws Exception {
        // Given
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        boolean result = fcmService.sendNotification(
                testToken, testTitle, testBody, null,
                FcmService.NotificationCategory.SCHEDULE,
                FcmService.Priority.LOW
        );

        // Then
        assertThat(result).isFalse();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("배치 알림 전송 성공")
    void sendBatchNotification_Success() throws Exception {
        // Given
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        Map<String, String> data = new HashMap<>();

        BatchResponse batchResponse = mock(BatchResponse.class, withSettings().lenient());
        when(batchResponse.getSuccessCount()).thenReturn(3);
        when(batchResponse.getFailureCount()).thenReturn(0);

        when(firebaseMessaging.sendAll(anyList())).thenReturn(batchResponse);

        // When
        BatchResponse result = fcmService.sendBatchNotification(
                tokens, testTitle, testBody, data,
                FcmService.NotificationCategory.HEALTH
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailureCount()).isEqualTo(0);
        verify(firebaseMessaging).sendAll(anyList());
    }

    @Test
    @DisplayName("배치 알림 전송 - 부분 실패")
    void sendBatchNotification_PartialFailure() throws Exception {
        // Given
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);
        
        SendResponse failResponse = mock(SendResponse.class);
        when(failResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException failException = mock(FirebaseMessagingException.class);
        when(failException.getMessage()).thenReturn("Failed");
        when(failResponse.getException()).thenReturn(failException);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(Arrays.asList(
                successResponse, successResponse, failResponse
        ));

        when(firebaseMessaging.sendAll(anyList())).thenReturn(batchResponse);

        // When
        BatchResponse result = fcmService.sendBatchNotification(
                tokens, testTitle, testBody, null,
                FcmService.NotificationCategory.EMERGENCY
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("배치 알림 전송 실패 - 토큰 없음")
    void sendBatchNotification_EmptyTokens_Failure() {
        // Given
        List<String> emptyTokens = Collections.emptyList();

        // When
        BatchResponse result = fcmService.sendBatchNotification(
                emptyTokens, testTitle, testBody, null,
                FcmService.NotificationCategory.LOCATION
        );

        // Then
        assertThat(result).isNull();
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("비동기 알림 전송")
    void sendNotificationAsync_Success() throws Exception {
        // Given
        when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

        // When
        CompletableFuture<Boolean> future = fcmService.sendNotificationAsync(
                testToken, testTitle, testBody, FcmService.NotificationCategory.REMINDER
        );

        // Then
        Boolean result = future.get();
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("약물 복용 알림 전송 성공")
    void sendMedicationReminder_Success() throws Exception {
        // Given
        String medicationName = "타이레놀";
        String time = "오전 8시";
        when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

        // When
        boolean result = fcmService.sendMedicationReminder(testToken, medicationName, time);

        // Then
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("일정 알림 전송 성공")
    void sendScheduleReminder_Success() throws Exception {
        // Given
        String scheduleName = "병원 방문";
        String time = "오후 2시";
        when(firebaseMessaging.send(any(Message.class))).thenReturn(messageId);

        // When
        boolean result = fcmService.sendScheduleReminder(testToken, scheduleName, time);

        // Then
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("긴급 알림 전송 성공")
    void sendEmergencyAlert_Success() throws Exception {
        // Given
        List<String> guardianTokens = Arrays.asList("guardian1", "guardian2");
        String userName = "홍길동";
        String location = "서울시 강남구";

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(0);

        when(firebaseMessaging.sendAll(anyList())).thenReturn(batchResponse);

        // When
        BatchResponse result = fcmService.sendEmergencyAlert(guardianTokens, userName, location);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        verify(firebaseMessaging).sendAll(anyList());
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    void validateToken_Valid_Success() throws Exception {
        // Given
        when(firebaseMessaging.send(any(Message.class), eq(true))).thenReturn(messageId);

        // When
        boolean result = fcmService.validateToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(firebaseMessaging).send(any(Message.class), eq(true));
    }

    @Test
    @DisplayName("토큰 유효성 검증 실패")
    void validateToken_Invalid_Failure() throws Exception {
        // Given
        FirebaseMessagingException validationException = mock(FirebaseMessagingException.class, withSettings().lenient());
        when(validationException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(firebaseMessaging.send(any(Message.class), eq(true))).thenThrow(validationException);

        // When
        boolean result = fcmService.validateToken(testToken);

        // Then
        assertThat(result).isFalse();
        verify(firebaseMessaging).send(any(Message.class), eq(true));
    }

    @Test
    @DisplayName("토큰 유효성 검증 - null 토큰")
    void validateToken_NullToken_Failure() {
        // When
        boolean result = fcmService.validateToken(null);

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("토큰 유효성 검증 - 빈 토큰")
    void validateToken_EmptyToken_Failure() {
        // When
        boolean result = fcmService.validateToken("");

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    @DisplayName("알림 카테고리 제목 생성")
    void notificationCategory_GetTitle_Success() {
        // When & Then
        assertThat(FcmService.NotificationCategory.MEDICATION.getTitle())
                .isEqualTo("💊 약물");
        assertThat(FcmService.NotificationCategory.EMERGENCY.getTitle())
                .isEqualTo("🚨 긴급");
        assertThat(FcmService.NotificationCategory.SCHEDULE.getTitle())
                .isEqualTo("📅 일정");
    }

    @Test
    @DisplayName("Firebase 비초기화 상태에서 알림 전송")
    void sendNotification_FirebaseNotInitialized_Failure() {
        // Given - firebaseMessaging을 null로 설정
        FcmService fcmServiceWithNullFirebase = new FcmService(null);

        // When
        boolean result = fcmServiceWithNullFirebase.sendNotification(
                testToken, testTitle, testBody, null,
                FcmService.NotificationCategory.REMINDER,
                FcmService.Priority.NORMAL
        );

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Firebase 비초기화 상태에서 배치 알림 전송")
    void sendBatchNotification_FirebaseNotInitialized_Failure() {
        // Given
        FcmService fcmServiceWithNullFirebase = new FcmService(null);
        List<String> tokens = Arrays.asList("token1", "token2");

        // When
        BatchResponse result = fcmServiceWithNullFirebase.sendBatchNotification(
                tokens, testTitle, testBody, null,
                FcmService.NotificationCategory.HEALTH
        );

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Firebase 비초기화 상태에서 토큰 검증")
    void validateToken_FirebaseNotInitialized_Failure() {
        // Given
        FcmService fcmServiceWithNullFirebase = new FcmService(null);

        // When
        boolean result = fcmServiceWithNullFirebase.validateToken(testToken);

        // Then
        assertThat(result).isFalse();
    }
}