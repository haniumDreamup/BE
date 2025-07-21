package com.bifai.reminder.bifai_backend.security.aspect;

import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.annotation.GuardianCheck;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianCheckAspect 테스트")
class GuardianCheckAspectTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private GuardianRepository guardianRepository;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @Mock
    private SecurityContext securityContext;
    
    @InjectMocks
    private GuardianCheckAspect guardianCheckAspect;
    
    private User currentUser;
    private User targetUser;
    private Guardian approvedGuardian;
    private GuardianCheck guardianCheck;
    
    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .userId(1L)
                .username("guardian")
                .email("guardian@example.com")
                .isActive(true)
                .build();
        
        targetUser = User.builder()
                .userId(2L)
                .username("patient")
                .email("patient@example.com")
                .isActive(true)
                .build();
        
        approvedGuardian = Guardian.builder()
                .id(1L)
                .user(targetUser)
                .guardianUser(currentUser)
                .name("보호자")
                .relationshipType(Guardian.RelationshipType.PARENT)
                .approvalStatus(Guardian.ApprovalStatus.APPROVED)
                .isActive(true)
                .isPrimary(true)
                .canModifySettings(true)
                .canViewLocation(true)
                .canReceiveAlerts(true)
                .build();
        
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    @DisplayName("보호자 권한 검증 성공 - 승인된 보호자")
    void checkGuardianPermission_Success_ApprovedGuardian() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(false, false, "");
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(guardianRepository.findByUserIdAndGuardianUserId(2L, 1L))
                .thenReturn(Collections.singletonList(approvedGuardian));
        when(joinPoint.proceed()).thenReturn("success");
        
        // when
        Object result = guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck);
        
        // then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
    }
    
    @Test
    @DisplayName("보호자 권한 검증 성공 - 본인 접근")
    void checkGuardianPermission_Success_SelfAccess() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(true, false, ""); // allowSelf = true
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L}); // 본인 ID
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(joinPoint.proceed()).thenReturn("success");
        
        // when
        Object result = guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck);
        
        // then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
        verify(guardianRepository, never()).findByUserIdAndGuardianUserId(anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("보호자 권한 검증 실패 - 인증되지 않은 사용자")
    void checkGuardianPermission_Fail_NotAuthenticated() throws Throwable {
        // given
        when(securityContext.getAuthentication()).thenReturn(null);
        setupGuardianCheck(false, false, "");
        
        // when & then
        assertThatThrownBy(() -> guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증이 필요합니다");
    }
    
    @Test
    @DisplayName("보호자 권한 검증 실패 - 보호자 관계 없음")
    void checkGuardianPermission_Fail_NotGuardian() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(false, false, "");
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(guardianRepository.findByUserIdAndGuardianUserId(2L, 1L))
                .thenReturn(Collections.emptyList());
        
        // when & then
        assertThatThrownBy(() -> guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("해당 사용자의 보호자가 아닙니다");
    }
    
    @Test
    @DisplayName("보호자 권한 검증 실패 - 승인되지 않은 보호자")
    void checkGuardianPermission_Fail_NotApproved() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(false, false, "");
        
        Guardian pendingGuardian = Guardian.builder()
                .id(1L)
                .user(targetUser)
                .guardianUser(currentUser)
                .approvalStatus(Guardian.ApprovalStatus.PENDING)
                .isActive(true)
                .build();
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(guardianRepository.findByUserIdAndGuardianUserId(2L, 1L))
                .thenReturn(Collections.singletonList(pendingGuardian));
        
        // when & then
        assertThatThrownBy(() -> guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("승인된 보호자 관계가 아닙니다");
    }
    
    @Test
    @DisplayName("보호자 권한 검증 실패 - 주 보호자가 아님")
    void checkGuardianPermission_Fail_NotPrimaryGuardian() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(false, true, ""); // primaryOnly = true
        
        Guardian nonPrimaryGuardian = Guardian.builder()
                .id(1L)
                .user(targetUser)
                .guardianUser(currentUser)
                .approvalStatus(Guardian.ApprovalStatus.APPROVED)
                .isActive(true)
                .isPrimary(false) // 주 보호자가 아님
                .build();
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(guardianRepository.findByUserIdAndGuardianUserId(2L, 1L))
                .thenReturn(Collections.singletonList(nonPrimaryGuardian));
        
        // when & then
        assertThatThrownBy(() -> guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("주 보호자만 접근할 수 있습니다");
    }
    
    @Test
    @DisplayName("보호자 권한 검증 실패 - 특정 권한 없음")
    void checkGuardianPermission_Fail_NoSpecificPermission() throws Throwable {
        // given
        setupAuthentication();
        setupGuardianCheck(false, false, "canModifySettings");
        
        Guardian guardianWithoutPermission = Guardian.builder()
                .id(1L)
                .user(targetUser)
                .guardianUser(currentUser)
                .approvalStatus(Guardian.ApprovalStatus.APPROVED)
                .isActive(true)
                .canModifySettings(false) // 설정 수정 권한 없음
                .build();
        
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[]{2L});
        
        when(userRepository.findByUsername("guardian")).thenReturn(Optional.of(currentUser));
        when(guardianRepository.findByUserIdAndGuardianUserId(2L, 1L))
                .thenReturn(Collections.singletonList(guardianWithoutPermission));
        
        // when & then
        assertThatThrownBy(() -> guardianCheckAspect.checkGuardianPermission(joinPoint, guardianCheck))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("필요한 권한이 없습니다: canModifySettings");
    }
    
    private void setupAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "guardian", null, Collections.emptyList()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }
    
    private void setupGuardianCheck(boolean allowSelf, boolean primaryOnly, String requiredPermission) {
        guardianCheck = new GuardianCheck() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return GuardianCheck.class;
            }
            
            @Override
            public String userIdParam() {
                return "userId";
            }
            
            @Override
            public boolean allowSelf() {
                return allowSelf;
            }
            
            @Override
            public boolean primaryOnly() {
                return primaryOnly;
            }
            
            @Override
            public String requiredPermission() {
                return requiredPermission;
            }
        };
    }
    
    private Method getTestMethod() {
        try {
            return TestController.class.getMethod("testMethod", Long.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    // 테스트용 컨트롤러 클래스
    static class TestController {
        public String testMethod(Long userId) {
            return "success";
        }
    }
}