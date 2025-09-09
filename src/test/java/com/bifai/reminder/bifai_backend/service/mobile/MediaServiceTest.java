package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.MediaFileRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

  @Mock
  private MediaFileRepository mediaFileRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private S3Client s3Client;
  
  @Mock
  private MultipartFile multipartFile;
  
  @Mock
  private User user;
  
  @Mock
  private MediaFile mediaFile;
  
  private MediaService mediaService;
  
  private final String BUCKET_NAME = "test-bucket";
  private final Long USER_ID = 1L;
  private final String FILE_NAME = "test-image.jpg";
  private final String CONTENT_TYPE = "image/jpeg";
  private final long FILE_SIZE = 1024L;
  
  @BeforeEach
  void setUp() {
    mediaService = new MediaService(
        mediaFileRepository,
        userRepository,
        s3Client,
        BUCKET_NAME
    );
  }
  
  @Test
  void uploadFile_Success() throws IOException {
    // Given
    byte[] fileContent = "test file content".getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
    
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.getOriginalFilename()).thenReturn(FILE_NAME);
    when(multipartFile.getContentType()).thenReturn(CONTENT_TYPE);
    when(multipartFile.getSize()).thenReturn(FILE_SIZE);
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getInputStream()).thenReturn(inputStream);
    
    PutObjectResponse putResponse = PutObjectResponse.builder()
        .eTag("test-etag")
        .build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(putResponse);
    
    when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> {
      MediaFile saved = invocation.getArgument(0);
      // Simulate ID assignment
      return saved;
    });
    
    // When
    MediaFile result = mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE);
    
    // Then
    assertNotNull(result);
    verify(userRepository).findById(USER_ID);
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    verify(mediaFileRepository).save(any(MediaFile.class));
  }
  
  @Test
  void uploadFile_UserNotFound_ThrowsException() {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE)
    );
    
    verify(userRepository).findById(USER_ID);
    verifyNoInteractions(s3Client);
    verifyNoInteractions(mediaFileRepository);
  }
  
  @Test
  void uploadFile_EmptyFile_ThrowsException() throws IOException {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.isEmpty()).thenReturn(true);
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE)
    );
  }
  
  @Test
  void uploadFile_FileSizeExceeded_ThrowsException() throws IOException {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getSize()).thenReturn(60 * 1024 * 1024L); // 60MB
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE)
    );
  }
  
  @Test
  void uploadFile_InvalidContentType_ThrowsException() throws IOException {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getSize()).thenReturn(FILE_SIZE);
    when(multipartFile.getContentType()).thenReturn("text/plain");
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE)
    );
  }
  
  @Test
  void deleteFile_Success() {
    // Given
    Long mediaFileId = 100L;
    when(mediaFile.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(USER_ID);
    when(mediaFile.getS3Key()).thenReturn("media/profile/1/test.jpg");
    when(mediaFileRepository.findById(mediaFileId)).thenReturn(Optional.of(mediaFile));
    
    DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().build();
    when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(deleteResponse);
    
    // When
    mediaService.deleteFile(USER_ID, mediaFileId);
    
    // Then
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    verify(mediaFileRepository).delete(mediaFile);
  }
  
  @Test
  void deleteFile_NotOwner_ThrowsException() {
    // Given
    Long mediaFileId = 100L;
    Long anotherUserId = 999L;
    when(mediaFile.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(anotherUserId);
    when(mediaFileRepository.findById(mediaFileId)).thenReturn(Optional.of(mediaFile));
    
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> 
        mediaService.deleteFile(USER_ID, mediaFileId)
    );
    
    verifyNoInteractions(s3Client);
    verify(mediaFileRepository, never()).delete(any());
  }
  
  @Test
  void getFileUrl_WithCloudFront() {
    // Given
    String s3Key = "media/test/file.jpg";
    String cloudFrontUrl = "https://cdn.example.com";
    
    // Use reflection to set cloudFrontUrl
    try {
      java.lang.reflect.Field field = MediaService.class.getDeclaredField("cloudFrontUrl");
      field.setAccessible(true);
      field.set(mediaService, cloudFrontUrl);
    } catch (Exception e) {
      fail("Failed to set cloudFrontUrl");
    }
    
    // When
    String url = mediaService.getFileUrl(s3Key);
    
    // Then
    assertEquals("https://cdn.example.com/media/test/file.jpg", url);
  }
  
  @Test
  void getFileUrl_WithoutCloudFront() {
    // Given
    String s3Key = "media/test/file.jpg";
    
    // When
    String url = mediaService.getFileUrl(s3Key);
    
    // Then
    assertTrue(url.contains("s3"));
    assertTrue(url.contains(BUCKET_NAME));
    assertTrue(url.contains(s3Key));
  }
  
  @Test
  void validateFile_ProfileType_AcceptsImage() throws IOException {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getSize()).thenReturn(FILE_SIZE);
    when(multipartFile.getContentType()).thenReturn("image/jpeg");
    when(multipartFile.getOriginalFilename()).thenReturn(FILE_NAME);
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    
    PutObjectResponse putResponse = PutObjectResponse.builder().eTag("test").build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(putResponse);
    when(mediaFileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    
    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.PROFILE)
    );
  }
  
  @Test
  void validateFile_EmergencyType_AcceptsMultipleFormats() throws IOException {
    // Given
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(multipartFile.isEmpty()).thenReturn(false);
    when(multipartFile.getSize()).thenReturn(FILE_SIZE);
    when(multipartFile.getOriginalFilename()).thenReturn("emergency.mp4");
    when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    
    PutObjectResponse putResponse = PutObjectResponse.builder().eTag("test").build();
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(putResponse);
    when(mediaFileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    
    // Test video
    when(multipartFile.getContentType()).thenReturn("video/mp4");
    assertDoesNotThrow(() -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.EMERGENCY)
    );
    
    // Test audio
    when(multipartFile.getContentType()).thenReturn("audio/mpeg");
    assertDoesNotThrow(() -> 
        mediaService.uploadFile(USER_ID, multipartFile, MediaFile.UploadType.EMERGENCY)
    );
  }
}