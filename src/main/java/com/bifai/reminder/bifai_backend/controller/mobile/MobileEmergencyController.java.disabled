package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.EmergencyCallRequest;
import com.bifai.reminder.bifai_backend.dto.mobile.EmergencyContactResponse;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.service.mobile.MobileEmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 모바일 긴급상황 API 컨트롤러
 * 
 * BIF 사용자를 위한 간단한 긴급상황 대응 기능을 제공합니다.
 * 긴급 연락, 보호자 연락처, 위치 공유 등을 포함합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/emergency")
@RequiredArgsConstructor
@Tag(name = "Mobile Emergency", description = "모바일 긴급상황 대응 API")
@SecurityRequirement(name = "bearerAuth")
public class MobileEmergencyController {
  
  private final MobileEmergencyService mobileEmergencyService;
  
  /**
   * 긴급 상황 신고
   * 
   * @param request 긴급 상황 신고 요청
   * @param userDetails 인증된 사용자 정보
   * @return 신고 결과
   */
  @PostMapping("/call")
  @Operation(
      summary = "긴급 상황 신고",
      description = "긴급 상황 발생 시 보호자와 관련 기관에 자동으로 연락합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "긴급 신고 접수 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요"
      ),
      @ApiResponse(
          responseCode = "500",
          description = "긴급 신고 처리 실패"
      )
  })
  public ResponseEntity<MobileApiResponse<Void>> emergencyCall(
      @Valid @RequestBody EmergencyCallRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.warn("긴급 상황 신고: user={}, type={}, location={}", 
        userDetails.getUsername(), request.getEmergencyType(), request.getLocation());
    
    try {
      mobileEmergencyService.handleEmergencyCall(userDetails.getUsername(), request);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(
              null, 
              "도움을 요청했어요. 곧 연락이 갈 거예요"
          )
      );
      
    } catch (Exception e) {
      log.error("긴급 상황 처리 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "EMG_001",
              "긴급 신고를 처리할 수 없어요",
              "직접 112나 119에 전화해주세요"
          )
      );
    }
  }
  
  /**
   * 보호자 연락처 목록 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 보호자 연락처 목록
   */
  @GetMapping("/contacts")
  @Operation(
      summary = "보호자 연락처 목록",
      description = "등록된 보호자 및 긴급 연락처 목록을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "연락처 목록 조회 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요"
      )
  })
  public ResponseEntity<MobileApiResponse<List<EmergencyContactResponse>>> getEmergencyContacts(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("긴급 연락처 목록 조회: user={}", userDetails.getUsername());
    
    try {
      List<EmergencyContactResponse> contacts = 
          mobileEmergencyService.getEmergencyContacts(userDetails.getUsername());
      
      String message = contacts.isEmpty() ? 
          "등록된 연락처가 없어요" : 
          String.format("등록된 연락처가 %d개 있어요", contacts.size());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(contacts, message)
      );
      
    } catch (Exception e) {
      log.error("긴급 연락처 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "EMG_002",
              "연락처를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 보호자에게 안전 알림 전송
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 알림 전송 결과
   */
  @PostMapping("/safety-check")
  @Operation(
      summary = "안전 알림 전송",
      description = "보호자에게 현재 안전한 상태임을 알려주는 메시지를 전송합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "안전 알림 전송 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요"
      )
  })
  public ResponseEntity<MobileApiResponse<Void>> sendSafetyCheck(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("안전 알림 전송: user={}", userDetails.getUsername());
    
    try {
      mobileEmergencyService.sendSafetyCheck(userDetails.getUsername());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(
              null, 
              "보호자에게 안전 알림을 보냈어요"
          )
      );
      
    } catch (Exception e) {
      log.error("안전 알림 전송 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "EMG_003",
              "안전 알림을 보낼 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 위치 공유
   * 
   * @param latitude 위도
   * @param longitude 경도
   * @param userDetails 인증된 사용자 정보
   * @return 위치 공유 결과
   */
  @PostMapping("/share-location")
  @Operation(
      summary = "위치 공유",
      description = "현재 위치를 보호자와 공유합니다."
  )
  public ResponseEntity<MobileApiResponse<Void>> shareLocation(
      @Parameter(description = "위도", example = "37.5665")
      @RequestParam Double latitude,
      @Parameter(description = "경도", example = "126.9780") 
      @RequestParam Double longitude,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("위치 공유: user={}, lat={}, lng={}", 
        userDetails.getUsername(), latitude, longitude);
    
    try {
      mobileEmergencyService.shareLocation(userDetails.getUsername(), latitude, longitude);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(
              null, 
              "위치를 보호자와 공유했어요"
          )
      );
      
    } catch (Exception e) {
      log.error("위치 공유 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "EMG_004",
              "위치를 공유할 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
}