package com.bifai.reminder.bifai_backend.security.aspect;

import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.annotation.GuardianCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * 보호자 권한 검증 AOP
 * @GuardianCheck 어노테이션이 붙은 메서드의 보호자 권한을 검증
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GuardianCheckAspect {
    
    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    
    @Around("@annotation(guardianCheck)")
    public Object checkGuardianPermission(ProceedingJoinPoint joinPoint, GuardianCheck guardianCheck) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("인증이 필요합니다");
        }
        
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UnauthorizedException("현재 사용자를 찾을 수 없습니다"));
        
        // 대상 사용자 ID 추출
        Long targetUserId = extractUserId(joinPoint, guardianCheck.userIdParam());
        
        if (targetUserId == null) {
            throw new IllegalArgumentException("대상 사용자 ID를 찾을 수 없습니다");
        }
        
        // 본인 접근 허용 여부 확인
        if (guardianCheck.allowSelf() && currentUser.getUserId().equals(targetUserId)) {
            log.debug("본인 접근 허용: userId={}", currentUser.getUserId());
            return joinPoint.proceed();
        }
        
        // 보호자 관계 확인
        List<Guardian> guardianRelations = guardianRepository.findByUserIdAndGuardianUserId(
                targetUserId, currentUser.getUserId());
        
        if (guardianRelations.isEmpty()) {
            throw new UnauthorizedException("해당 사용자의 보호자가 아닙니다");
        }
        
        // 승인된 보호자인지 확인
        Guardian approvedGuardian = guardianRelations.stream()
                .filter(g -> g.getApprovalStatus() == Guardian.ApprovalStatus.APPROVED)
                .filter(g -> g.getIsActive())
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("승인된 보호자 관계가 아닙니다"));
        
        // 주 보호자만 접근 가능한 경우
        if (guardianCheck.primaryOnly() && !approvedGuardian.getIsPrimary()) {
            throw new UnauthorizedException("주 보호자만 접근할 수 있습니다");
        }
        
        // 특정 권한 확인
        if (!guardianCheck.requiredPermission().isEmpty()) {
            boolean hasPermission = checkSpecificPermission(approvedGuardian, guardianCheck.requiredPermission());
            if (!hasPermission) {
                throw new UnauthorizedException("필요한 권한이 없습니다: " + guardianCheck.requiredPermission());
            }
        }
        
        log.info("보호자 권한 검증 성공: guardianId={}, targetUserId={}", 
                currentUser.getUserId(), targetUserId);
        
        return joinPoint.proceed();
    }
    
    /**
     * 메서드 파라미터에서 사용자 ID 추출
     */
    private Long extractUserId(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            // @Param 어노테이션이나 파라미터 이름으로 확인
            if (parameter.getName().equals(paramName) || 
                (parameter.isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class) &&
                 parameter.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class).value().equals(paramName))) {
                if (args[i] instanceof Long) {
                    return (Long) args[i];
                } else if (args[i] instanceof String) {
                    try {
                        return Long.parseLong((String) args[i]);
                    } catch (NumberFormatException e) {
                        log.error("사용자 ID 파싱 실패: {}", args[i]);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 특정 권한 확인
     */
    private boolean checkSpecificPermission(Guardian guardian, String permission) {
        switch (permission) {
            case "canModifySettings":
                return guardian.getCanModifySettings();
            case "canViewLocation":
                return guardian.getCanViewLocation();
            case "canReceiveAlerts":
                return guardian.getCanReceiveAlerts();
            default:
                log.warn("알 수 없는 권한: {}", permission);
                return false;
        }
    }
}