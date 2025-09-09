package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileMedicationResponse;
import com.bifai.reminder.bifai_backend.dto.mobile.MedicationTakeRequest;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.service.mobile.MobileMedicationService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 모바일 약물 관리 API 컨트롤러
 * 
 * BIF 사용자를 위한 간단한 약물 복용 관리 기능을 제공합니다.
 * 오늘의 약물, 복용 체크, 복용 기록 등을 포함합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/medications")
@RequiredArgsConstructor
@Tag(name = "Mobile Medication", description = "모바일 약물 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class MobileMedicationController {
  
  private final MobileMedicationService mobileMedicationService;
  
  /**
   * 오늘의 약물 목록 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 오늘 복용해야 할 약물 목록
   */
  @GetMapping("/today")
  @Operation(
      summary = "오늘의 약물 목록",
      description = "오늘 복용해야 할 약물 목록과 복용 상태를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "약물 목록 조회 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요"
      )
  })
  public ResponseEntity<MobileApiResponse<List<MobileMedicationResponse>>> getTodayMedications(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("오늘의 약물 목록 조회: user={}", userDetails.getUsername());
    
    try {
      List<MobileMedicationResponse> medications = 
          mobileMedicationService.getTodayMedications(userDetails.getUsername());
      
      String message = medications.isEmpty() ? 
          "오늘 복용할 약이 없어요" : 
          String.format("오늘 복용할 약이 %d개 있어요", medications.size());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(medications, message)
      );
      
    } catch (Exception e) {
      log.error("약물 목록 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "MED_001",
              "약물 정보를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 특정 날짜의 약물 목록 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @param date 조회할 날짜
   * @return 해당 날짜의 약물 목록
   */
  @GetMapping
  @Operation(
      summary = "특정 날짜 약물 목록",
      description = "지정한 날짜의 약물 목록과 복용 기록을 조회합니다."
  )
  public ResponseEntity<MobileApiResponse<List<MobileMedicationResponse>>> getMedicationsByDate(
      @AuthenticationPrincipal UserDetails userDetails,
      @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", example = "2024-01-15")
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
    
    log.info("약물 목록 조회: user={}, date={}", userDetails.getUsername(), date);
    
    try {
      List<MobileMedicationResponse> medications = 
          mobileMedicationService.getMedicationsByDate(userDetails.getUsername(), date);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(medications, "약물 정보를 불러왔어요")
      );
      
    } catch (Exception e) {
      log.error("약물 목록 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "MED_002",
              "약물 정보를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 약물 복용 체크
   * 
   * @param medicationId 약물 ID
   * @param request 복용 체크 요청
   * @param userDetails 인증된 사용자 정보
   * @return 복용 체크 결과
   */
  @PostMapping("/{medicationId}/take")
  @Operation(
      summary = "약물 복용 체크",
      description = "약물 복용을 체크하고 기록합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "복용 체크 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "약물을 찾을 수 없음"
      )
  })
  public ResponseEntity<MobileApiResponse<MobileMedicationResponse>> takeMedication(
      @Parameter(description = "약물 ID", example = "1")
      @PathVariable Long medicationId,
      @Valid @RequestBody MedicationTakeRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("약물 복용 체크: user={}, medicationId={}, taken={}", 
        userDetails.getUsername(), medicationId, request.isTaken());
    
    try {
      MobileMedicationResponse response = mobileMedicationService.takeMedication(
          userDetails.getUsername(), medicationId, request);
      
      String message = request.isTaken() ? 
          "복용 완료했어요! 잘하고 있어요 👏" : 
          "복용 체크를 취소했어요";
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, message)
      );
      
    } catch (Exception e) {
      log.error("약물 복용 체크 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "MED_003",
              "복용 체크를 할 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 약물 상세 정보 조회
   * 
   * @param medicationId 약물 ID
   * @param userDetails 인증된 사용자 정보
   * @return 약물 상세 정보
   */
  @GetMapping("/{medicationId}")
  @Operation(
      summary = "약물 상세 정보",
      description = "특정 약물의 상세 정보를 조회합니다."
  )
  public ResponseEntity<MobileApiResponse<MobileMedicationResponse>> getMedicationDetail(
      @Parameter(description = "약물 ID", example = "1")
      @PathVariable Long medicationId,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("약물 상세 조회: user={}, medicationId={}", 
        userDetails.getUsername(), medicationId);
    
    try {
      MobileMedicationResponse medication = mobileMedicationService.getMedicationDetail(
          userDetails.getUsername(), medicationId);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(medication, "약물 정보를 불러왔어요")
      );
      
    } catch (Exception e) {
      log.error("약물 상세 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(404).body(
          MobileApiResponse.error(
              "MED_004",
              "약물 정보를 찾을 수 없어요",
              "다른 약물을 확인해보세요"
          )
      );
    }
  }
}