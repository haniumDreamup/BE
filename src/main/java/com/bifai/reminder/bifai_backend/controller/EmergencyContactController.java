package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.emergencycontact.ContactAvailabilityResponse;
import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactRequest;
import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactResponse;
import com.bifai.reminder.bifai_backend.dto.response.BifApiResponse;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.service.EmergencyContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 긴급 연락처 관리 컨트롤러
 * 보호자 및 의료진 연락처 CRUD API
 */
@RestController
@RequestMapping("/api/emergency-contacts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Emergency Contacts", description = "긴급 연락처 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class EmergencyContactController {

  private final EmergencyContactService emergencyContactService;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 긴급 연락처 추가
   */
  @PostMapping
  @Operation(summary = "긴급 연락처 추가", description = "새로운 긴급 연락처를 등록합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "연락처 등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "409", description = "중복된 연락처")
  })
  public ResponseEntity<BifApiResponse<EmergencyContactResponse>> createEmergencyContact(
      @Valid @RequestBody EmergencyContactRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Creating emergency contact for user: {}", userId);
    
    EmergencyContactResponse response = emergencyContactService.createEmergencyContact(userId, request);
    
    return ResponseEntity.status(HttpStatus.CREATED).body(
        BifApiResponse.success(response, "긴급 연락처가 등록되었어요")
    );
  }

  /**
   * 긴급 연락처 수정
   */
  @PutMapping("/{contactId}")
  @Operation(summary = "긴급 연락처 수정", description = "기존 긴급 연락처 정보를 수정합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "404", description = "연락처를 찾을 수 없음")
  })
  public ResponseEntity<BifApiResponse<EmergencyContactResponse>> updateEmergencyContact(
      @PathVariable Long contactId,
      @Valid @RequestBody EmergencyContactRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Updating emergency contact {} for user: {}", contactId, userId);
    
    EmergencyContactResponse response = emergencyContactService.updateEmergencyContact(userId, contactId, request);
    
    return ResponseEntity.ok(
        BifApiResponse.success(response, "연락처 정보가 수정되었어요")
    );
  }

  /**
   * 긴급 연락처 삭제
   */
  @DeleteMapping("/{contactId}")
  @Operation(summary = "긴급 연락처 삭제", description = "긴급 연락처를 삭제합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "연락처를 찾을 수 없음")
  })
  public ResponseEntity<BifApiResponse<Void>> deleteEmergencyContact(
      @PathVariable Long contactId,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Deleting emergency contact {} for user: {}", contactId, userId);
    
    emergencyContactService.deleteEmergencyContact(userId, contactId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(null, "연락처가 삭제되었어요")
    );
  }

  /**
   * 긴급 연락처 단일 조회
   */
  @GetMapping("/{contactId}")
  @Operation(summary = "긴급 연락처 상세 조회", description = "특정 긴급 연락처의 상세 정보를 조회합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "연락처를 찾을 수 없음")
  })
  public ResponseEntity<BifApiResponse<EmergencyContactResponse>> getEmergencyContact(
      @PathVariable Long contactId,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Getting emergency contact {} for user: {}", contactId, userId);
    
    EmergencyContactResponse response = emergencyContactService.getEmergencyContact(userId, contactId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(response, "연락처 정보를 가져왔어요")
    );
  }

  /**
   * 사용자의 모든 긴급 연락처 조회
   */
  @GetMapping
  @Operation(summary = "긴급 연락처 목록 조회", description = "사용자의 모든 긴급 연락처를 조회합니다")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  public ResponseEntity<BifApiResponse<List<EmergencyContactResponse>>> getUserEmergencyContacts(
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Getting all emergency contacts for user: {}", userId);
    
    List<EmergencyContactResponse> contacts = emergencyContactService.getUserEmergencyContacts(userId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(contacts, 
            String.format("%d개의 연락처를 찾았어요", contacts.size()))
    );
  }

  /**
   * 활성화된 연락처만 조회
   */
  @GetMapping("/active")
  @Operation(summary = "활성 연락처 조회", description = "활성화된 긴급 연락처만 조회합니다")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  public ResponseEntity<BifApiResponse<List<EmergencyContactResponse>>> getActiveContacts(
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Getting active emergency contacts for user: {}", userId);
    
    List<EmergencyContactResponse> contacts = emergencyContactService.getActiveContacts(userId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(contacts, 
            String.format("%d개의 활성 연락처가 있어요", contacts.size()))
    );
  }

  /**
   * 현재 연락 가능한 연락처 조회
   */
  @GetMapping("/available")
  @Operation(summary = "연락 가능한 연락처 조회", 
             description = "현재 시간에 연락 가능한 긴급 연락처를 조회합니다")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  public ResponseEntity<BifApiResponse<List<ContactAvailabilityResponse>>> getAvailableContacts(
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Getting available emergency contacts for user: {}", userId);
    
    List<ContactAvailabilityResponse> contacts = emergencyContactService.getAvailableContacts(userId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(contacts, 
            String.format("지금 %d명에게 연락할 수 있어요", contacts.size()))
    );
  }

  /**
   * 의료진 연락처 조회
   */
  @GetMapping("/medical")
  @Operation(summary = "의료진 연락처 조회", description = "의료진 긴급 연락처만 조회합니다")
  @ApiResponse(responseCode = "200", description = "조회 성공")
  public ResponseEntity<BifApiResponse<List<EmergencyContactResponse>>> getMedicalContacts(
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Getting medical emergency contacts for user: {}", userId);
    
    List<EmergencyContactResponse> contacts = emergencyContactService.getMedicalContacts(userId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(contacts, 
            String.format("%d명의 의료진 연락처가 있어요", contacts.size()))
    );
  }

  /**
   * 연락처 검증
   */
  @PostMapping("/{contactId}/verify")
  @Operation(summary = "연락처 검증", description = "검증 코드를 사용하여 연락처를 인증합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "검증 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 검증 코드")
  })
  public ResponseEntity<BifApiResponse<EmergencyContactResponse>> verifyContact(
      @PathVariable Long contactId,
      @RequestParam String verificationCode,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Verifying emergency contact {} for user: {}", contactId, userId);
    
    EmergencyContactResponse response = emergencyContactService.verifyContact(userId, contactId, verificationCode);
    
    return ResponseEntity.ok(
        BifApiResponse.success(response, "연락처가 인증되었어요")
    );
  }

  /**
   * 연락처 활성화/비활성화 토글
   */
  @PatchMapping("/{contactId}/toggle-active")
  @Operation(summary = "연락처 활성화 토글", description = "연락처의 활성화 상태를 전환합니다")
  @ApiResponse(responseCode = "200", description = "상태 변경 성공")
  public ResponseEntity<BifApiResponse<EmergencyContactResponse>> toggleContactActive(
      @PathVariable Long contactId,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Toggling active status for contact {} of user: {}", contactId, userId);
    
    EmergencyContactResponse response = emergencyContactService.toggleContactActive(userId, contactId);
    
    String message = response.getIsActive() ? 
        "연락처가 활성화되었어요" : "연락처가 비활성화되었어요";
    
    return ResponseEntity.ok(
        BifApiResponse.success(response, message)
    );
  }

  /**
   * 우선순위 변경
   */
  @PutMapping("/priorities")
  @Operation(summary = "연락처 우선순위 변경", 
             description = "연락처들의 우선순위를 일괄 변경합니다")
  @ApiResponse(responseCode = "200", description = "우선순위 변경 성공")
  public ResponseEntity<BifApiResponse<Void>> updateContactPriorities(
      @RequestBody List<Long> contactIds,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Updating contact priorities for user: {}", userId);
    
    emergencyContactService.updateContactPriorities(userId, contactIds);
    
    return ResponseEntity.ok(
        BifApiResponse.success(null, "우선순위가 변경되었어요")
    );
  }

  /**
   * 연락 기록 업데이트
   */
  @PostMapping("/{contactId}/contact-record")
  @Operation(summary = "연락 기록 업데이트", 
             description = "연락 시도 결과를 기록합니다")
  @ApiResponse(responseCode = "200", description = "기록 업데이트 성공")
  public ResponseEntity<BifApiResponse<Void>> updateContactRecord(
      @PathVariable Long contactId,
      @RequestParam boolean responded,
      @RequestParam(defaultValue = "0") long responseTimeMinutes,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromToken(httpRequest);
    log.info("Updating contact record for contact {} of user: {}", contactId, userId);
    
    emergencyContactService.updateContactRecord(userId, contactId, responded, responseTimeMinutes);
    
    return ResponseEntity.ok(
        BifApiResponse.success(null, "연락 기록이 업데이트되었어요")
    );
  }

  /**
   * JWT 토큰에서 사용자 ID 추출
   */
  private Long getUserIdFromToken(HttpServletRequest request) {
    String token = jwtTokenProvider.resolveToken(request);
    return jwtTokenProvider.getUserId(token);
  }
}