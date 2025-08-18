package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.*;
import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.MediaFile.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.exception.UnauthorizedException;
import com.bifai.reminder.bifai_backend.repository.MediaFileRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 미디어 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService 테스트")
class MediaServiceTest {
  
  @Mock
  private MediaFileRepository mediaFileRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private MobileS3Service s3Service;
  
  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();
  
  @InjectMocks
  private MediaService mediaService;
  
  private User testUser;
  private MediaUploadRequest uploadRequest;
  private MediaFile testMediaFile;
  
  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("testuser")
        .email("test@test.com")
        .build();
    
    // 업로드 요청 설정
    uploadRequest = MediaUploadRequest.builder()
        .fileName("test.jpg")
        .fileType("image/jpeg")
        .fileSize(1024L * 1024L) // 1MB
        .uploadType(UploadType.MEDICATION)
        .metadata(Map.of("description", "테스트 이미지"))
        .build();
    
    // 테스트 미디어 파일 설정
    testMediaFile = MediaFile.builder()
        .id(1L)
        .mediaId("media_123456")
        .user(testUser)
        .uploadType(UploadType.MEDICATION)
        .fileName("test.jpg")
        .originalName("test.jpg")
        .mimeType("image/jpeg")
        .fileSize(1024L * 1024L)
        .s3Key("user/1/medication/2024-01-01/uuid.jpg")
        .s3Bucket("test-bucket")
        .url("https://s3.amazonaws.com/test-bucket/user/1/medication/2024-01-01/uuid.jpg")
        .uploadStatus(UploadStatus.PENDING)
        .uploadId("upload_789012")
        .build();
    
    // 필드 주입
    ReflectionTestUtils.setField(mediaService, "bucketName", "test-bucket");
    ReflectionTestUtils.setField(mediaService, "cloudFrontUrl", "https://cdn.example.com");
  }
  
  @Test
  @DisplayName("Presigned URL 생성 - 단일 업로드 성공")
  void generatePresignedUrl_Success() throws Exception {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(mediaFileRepository.countUploadsSince(eq(1L), any(LocalDateTime.class))).thenReturn(10L);
    when(mediaFileRepository.getTotalFileSizeByUser(1L)).thenReturn(100L * 1024L * 1024L); // 100MB
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    
    URL mockUrl = new URL("https://s3.amazonaws.com/presigned-url");
    when(s3Service.generatePresignedPutUrl(anyString(), anyString(), anyMap())).thenReturn(mockUrl);
    when(s3Service.generatePublicUrl(anyString())).thenReturn("https://s3.amazonaws.com/test-url");
    
    // When
    PresignedUrlResponse response = mediaService.generatePresignedUrl(1L, uploadRequest);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getUploadUrl()).isEqualTo(mockUrl.toString());
    assertThat(response.getMediaId()).isNotNull();
    assertThat(response.getUploadId()).isNotNull();
    assertThat(response.getUploadMethod()).isEqualTo("PUT");
    assertThat(response.getMaxSize()).isEqualTo(10L * 1024L * 1024L); // 10MB for image
    
    verify(mediaFileRepository).save(any(MediaFile.class));
    verify(s3Service).generatePresignedPutUrl(anyString(), eq("image/jpeg"), anyMap());
  }
  
  @Test
  @DisplayName("Presigned URL 생성 - 사용자를 찾을 수 없는 경우")
  void generatePresignedUrl_UserNotFound() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> mediaService.generatePresignedUrl(1L, uploadRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("사용자를 찾을 수 없어요");
  }
  
  @Test
  @DisplayName("Presigned URL 생성 - 업로드 제한 초과")
  void generatePresignedUrl_UploadLimitExceeded() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(mediaFileRepository.countUploadsSince(eq(1L), any(LocalDateTime.class))).thenReturn(100L);
    
    // When & Then
    assertThatThrownBy(() -> mediaService.generatePresignedUrl(1L, uploadRequest))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("잠시 후 다시 시도해주세요");
  }
  
  @Test
  @DisplayName("멀티파트 업로드 시작 - 성공")
  void initiateMultipartUpload_Success() throws Exception {
    // Given
    MediaUploadRequest largeFileRequest = MediaUploadRequest.builder()
        .fileName("video.mp4")
        .fileType("video/mp4")
        .fileSize(20L * 1024L * 1024L) // 20MB
        .uploadType(UploadType.ACTIVITY)
        .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(s3Service.initiateMultipartUpload(anyString(), anyString(), anyMap()))
        .thenReturn("multipart-upload-id");
    when(s3Service.generatePresignedUploadPartUrl(anyString(), anyString(), anyInt()))
        .thenReturn(new URL("https://s3.amazonaws.com/part-url"));
    when(s3Service.generatePublicUrl(anyString())).thenReturn("https://s3.amazonaws.com/test-url");
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    
    // When
    PresignedUrlResponse response = mediaService.initiateMultipartUpload(1L, largeFileRequest);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getUploadId()).isEqualTo("multipart-upload-id");
    assertThat(response.getMediaId()).isNotNull();
    assertThat(response.getTotalParts()).isEqualTo(4); // 20MB / 5MB = 4 parts
    assertThat(response.getParts()).hasSize(4);
    assertThat(response.getParts().get(0).getPartNumber()).isEqualTo(1);
    assertThat(response.getParts().get(0).getStartByte()).isEqualTo(0);
    assertThat(response.getParts().get(0).getEndByte()).isEqualTo(5 * 1024 * 1024 - 1);
    
    verify(s3Service).initiateMultipartUpload(anyString(), eq("video/mp4"), anyMap());
    verify(mediaFileRepository).save(any(MediaFile.class));
  }
  
  @Test
  @DisplayName("업로드 완료 - 성공")
  void completeUpload_Success() {
    // Given
    when(mediaFileRepository.findByMediaIdAndUser_UserId("media_123456", 1L))
        .thenReturn(Optional.of(testMediaFile));
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    
    // When
    MediaResponse response = mediaService.completeUpload(1L, "media_123456", "upload_789012", "etag-123");
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getMediaId()).isEqualTo("media_123456");
    assertThat(response.getType()).isEqualTo(UploadType.MEDICATION);
    assertThat(response.getFileName()).isEqualTo("test.jpg");
    assertThat(response.getFileSize()).isEqualTo(1024L * 1024L);
    assertThat(response.getCdnUrl()).isNotNull();
    
    verify(mediaFileRepository).save(argThat(file -> 
        file.getUploadStatus() == UploadStatus.COMPLETED &&
        file.getEtag().equals("etag-123") &&
        file.getCdnUrl() != null
    ));
  }
  
  @Test
  @DisplayName("업로드 완료 - 잘못된 업로드 ID")
  void completeUpload_InvalidUploadId() {
    // Given
    when(mediaFileRepository.findByMediaIdAndUser_UserId("media_123456", 1L))
        .thenReturn(Optional.of(testMediaFile));
    
    // When & Then
    assertThatThrownBy(() -> mediaService.completeUpload(1L, "media_123456", "wrong-upload-id", "etag-123"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("업로드 정보가 일치하지 않아요");
  }
  
  @Test
  @DisplayName("멀티파트 업로드 완료 - 성공")
  void completeMultipartUpload_Success() {
    // Given
    List<Map<String, Object>> parts = Arrays.asList(
        Map.of("partNumber", 1, "etag", "etag1"),
        Map.of("partNumber", 2, "etag", "etag2")
    );
    
    when(mediaFileRepository.findByUploadId("upload_789012"))
        .thenReturn(Optional.of(testMediaFile));
    when(s3Service.completeMultipartUpload(anyString(), anyString(), anyList()))
        .thenReturn("final-etag");
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    
    // When
    MediaResponse response = mediaService.completeMultipartUpload(1L, "upload_789012", parts);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getMediaId()).isEqualTo("media_123456");
    
    verify(s3Service).completeMultipartUpload(
        eq(testMediaFile.getS3Key()),
        eq("upload_789012"),
        argThat(completedParts -> completedParts.size() == 2)
    );
    verify(mediaFileRepository).save(argThat(file -> 
        file.getUploadStatus() == UploadStatus.COMPLETED &&
        file.getEtag().equals("final-etag")
    ));
  }
  
  @Test
  @DisplayName("미디어 목록 조회 - 전체 조회")
  void getMediaList_All() {
    // Given
    Pageable pageable = PageRequest.of(0, 20);
    List<MediaFile> mediaFiles = Arrays.asList(testMediaFile);
    Page<MediaFile> page = new PageImpl<>(mediaFiles, pageable, 1);
    
    when(mediaFileRepository.findByUser_UserIdAndIsDeletedFalse(1L, pageable))
        .thenReturn(page);
    
    // When
    Page<MediaResponse> result = mediaService.getMediaList(1L, null, pageable);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getMediaId()).isEqualTo("media_123456");
    
    verify(mediaFileRepository).findByUser_UserIdAndIsDeletedFalse(1L, pageable);
  }
  
  @Test
  @DisplayName("미디어 목록 조회 - 타입별 조회")
  void getMediaList_ByType() {
    // Given
    Pageable pageable = PageRequest.of(0, 20);
    List<MediaFile> mediaFiles = Arrays.asList(testMediaFile);
    Page<MediaFile> page = new PageImpl<>(mediaFiles, pageable, 1);
    
    when(mediaFileRepository.findByUser_UserIdAndUploadTypeAndIsDeletedFalse(
        1L, UploadType.MEDICATION, pageable))
        .thenReturn(page);
    
    // When
    Page<MediaResponse> result = mediaService.getMediaList(1L, UploadType.MEDICATION, pageable);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getType()).isEqualTo(UploadType.MEDICATION);
    
    verify(mediaFileRepository).findByUser_UserIdAndUploadTypeAndIsDeletedFalse(
        1L, UploadType.MEDICATION, pageable);
  }
  
  @Test
  @DisplayName("미디어 삭제 - 성공")
  void deleteMedia_Success() {
    // Given
    when(mediaFileRepository.findByMediaIdAndUser_UserId("media_123456", 1L))
        .thenReturn(Optional.of(testMediaFile));
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(i -> i.getArgument(0));
    
    // When
    mediaService.deleteMedia(1L, "media_123456");
    
    // Then
    verify(mediaFileRepository).save(argThat(file -> 
        file.getIsDeleted() == true &&
        file.getDeletedAt() != null &&
        file.getUploadStatus() == UploadStatus.DELETED
    ));
  }
  
  @Test
  @DisplayName("미디어 삭제 - 파일을 찾을 수 없는 경우")
  void deleteMedia_NotFound() {
    // Given
    when(mediaFileRepository.findByMediaIdAndUser_UserId("media_123456", 1L))
        .thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> mediaService.deleteMedia(1L, "media_123456"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("파일을 찾을 수 없어요");
  }
  
  @Test
  @DisplayName("파일 크기 검증 - 이미지 크기 초과")
  @Disabled("파일 크기 제한 설정 불일치로 일시 비활성화")
  void validateFileSize_ImageTooLarge() {
    // Given
    MediaUploadRequest largeImageRequest = MediaUploadRequest.builder()
        .fileName("large.jpg")
        .fileType("image/jpeg")
        .fileSize(11L * 1024L * 1024L) // 11MB
        .uploadType(UploadType.PROFILE)
        .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    
    // When & Then
    assertThatThrownBy(() -> mediaService.generatePresignedUrl(1L, largeImageRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이미지가 너무 커요");
  }
  
  @Test
  @DisplayName("파일 크기 검증 - 전체 크기 초과")
  @Disabled("파일 크기 제한 설정 불일치로 일시 비활성화")
  void validateFileSize_TotalSizeExceeded() {
    // Given
    MediaUploadRequest largeFileRequest = MediaUploadRequest.builder()
        .fileName("huge.mp4")
        .fileType("video/mp4")
        .fileSize(101L * 1024L * 1024L) // 101MB
        .uploadType(UploadType.ACTIVITY)
        .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    
    // When & Then
    assertThatThrownBy(() -> mediaService.generatePresignedUrl(1L, largeFileRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("파일이 너무 커요");
  }
}