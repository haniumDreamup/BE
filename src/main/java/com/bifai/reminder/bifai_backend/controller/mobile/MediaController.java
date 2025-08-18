package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.*;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadType;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.service.mobile.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 미디어 업로드 컨트롤러
 * 
 * 이미지/비디오 파일 업로드 및 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "미디어 파일 관리 API")
public class MediaController {
  
  private final MediaService mediaService;
  
  /**
   * Presigned URL 생성 (단일 업로드)
   */
  @PostMapping("/presigned-url")
  @Operation(summary = "업로드 URL 생성", description = "파일 업로드를 위한 임시 URL을 생성합니다")
  public ResponseEntity<MobileApiResponse<PresignedUrlResponse>> generatePresignedUrl(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody MediaUploadRequest request) {
    
    log.info("Presigned URL request: userId={}, fileName={}, fileType={}, fileSize={}", 
        userDetails.getUserId(), request.getFileName(), request.getFileType(), request.getFileSize());
    
    PresignedUrlResponse response;
    
    if (request.needsMultipartUpload()) {
      // 5MB 이상은 멀티파트 업로드
      response = mediaService.initiateMultipartUpload(userDetails.getUserId(), request);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, "큰 파일 업로드를 시작해요")
      );
    } else {
      // 5MB 미만은 단일 업로드
      response = mediaService.generatePresignedUrl(userDetails.getUserId(), request);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, "업로드 URL이 준비됐어요")
      );
    }
  }
  
  /**
   * 멀티파트 업로드 시작
   */
  @PostMapping("/multipart/init")
  @Operation(summary = "멀티파트 업로드 시작", description = "대용량 파일을 위한 멀티파트 업로드를 시작합니다")
  public ResponseEntity<MobileApiResponse<PresignedUrlResponse>> initiateMultipartUpload(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody MediaUploadRequest request) {
    
    log.info("Multipart upload init: userId={}, fileName={}, fileSize={}", 
        userDetails.getUserId(), request.getFileName(), request.getFileSize());
    
    PresignedUrlResponse response = mediaService.initiateMultipartUpload(
        userDetails.getUserId(), request);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(response, "큰 파일 업로드를 시작해요")
    );
  }
  
  /**
   * 업로드 완료 확인
   */
  @PostMapping("/{mediaId}/complete")
  @Operation(summary = "업로드 완료", description = "S3 업로드 완료 후 서버에 알립니다")
  public ResponseEntity<MobileApiResponse<MediaResponse>> completeUpload(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String mediaId,
      @RequestBody Map<String, String> request) {
    
    log.info("Upload complete: userId={}, mediaId={}", userDetails.getUserId(), mediaId);
    
    String uploadId = request.get("uploadId");
    String etag = request.get("etag");
    
    MediaResponse response = mediaService.completeUpload(
        userDetails.getUserId(), mediaId, uploadId, etag);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(response, "업로드가 완료됐어요")
    );
  }
  
  /**
   * 멀티파트 업로드 완료
   */
  @PostMapping("/multipart/{uploadId}/complete")
  @Operation(summary = "멀티파트 업로드 완료", description = "모든 파트 업로드 완료 후 파일을 조합합니다")
  public ResponseEntity<MobileApiResponse<MediaResponse>> completeMultipartUpload(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String uploadId,
      @RequestBody Map<String, Object> request) {
    
    log.info("Multipart upload complete: userId={}, uploadId={}", 
        userDetails.getUserId(), uploadId);
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> parts = (List<Map<String, Object>>) request.get("parts");
    
    MediaResponse response = mediaService.completeMultipartUpload(
        userDetails.getUserId(), uploadId, parts);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(response, "업로드가 완료됐어요")
    );
  }
  
  /**
   * 미디어 목록 조회
   */
  @GetMapping
  @Operation(summary = "미디어 목록 조회", description = "업로드한 미디어 파일 목록을 조회합니다")
  public ResponseEntity<MobileApiResponse<Page<MediaResponse>>> getMediaList(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Parameter(description = "업로드 타입") @RequestParam(required = false) UploadType type,
      @Parameter(description = "시작일 (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
      @Parameter(description = "종료일 (YYYY-MM-DD)") @RequestParam(required = false) String endDate,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    
    log.info("Get media list: userId={}, type={}", userDetails.getUserId(), type);
    
    Page<MediaResponse> mediaList = mediaService.getMediaList(
        userDetails.getUserId(), type, pageable);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(mediaList, "사진을 불러왔어요")
    );
  }
  
  /**
   * 미디어 상세 조회
   */
  @GetMapping("/{mediaId}")
  @Operation(summary = "미디어 상세 조회", description = "특정 미디어 파일의 상세 정보를 조회합니다")
  public ResponseEntity<MobileApiResponse<MediaResponse>> getMedia(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String mediaId) {
    
    log.info("Get media: userId={}, mediaId={}", userDetails.getUserId(), mediaId);
    
    // TODO: 단일 조회 서비스 메서드 구현
    
    return ResponseEntity.ok(
        MobileApiResponse.success(null, "파일 정보를 불러왔어요")
    );
  }
  
  /**
   * 미디어 삭제
   */
  @DeleteMapping("/{mediaId}")
  @Operation(summary = "미디어 삭제", description = "업로드한 미디어 파일을 삭제합니다")
  public ResponseEntity<MobileApiResponse<Void>> deleteMedia(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String mediaId) {
    
    log.info("Delete media: userId={}, mediaId={}", userDetails.getUserId(), mediaId);
    
    mediaService.deleteMedia(userDetails.getUserId(), mediaId);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(null, "삭제했어요")
    );
  }
  
  /**
   * 업로드 상태 확인
   */
  @GetMapping("/status/{uploadId}")
  @Operation(summary = "업로드 상태 확인", description = "진행중인 업로드의 상태를 확인합니다")
  public ResponseEntity<MobileApiResponse<Map<String, Object>>> getUploadStatus(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String uploadId) {
    
    log.info("Get upload status: userId={}, uploadId={}", userDetails.getUserId(), uploadId);
    
    // TODO: 업로드 상태 조회 구현
    Map<String, Object> status = Map.of(
        "uploadId", uploadId,
        "status", "UPLOADING",
        "progress", 75,
        "message", "업로드 중이에요"
    );
    
    return ResponseEntity.ok(
        MobileApiResponse.success(status, "업로드 상태를 확인했어요")
    );
  }
}